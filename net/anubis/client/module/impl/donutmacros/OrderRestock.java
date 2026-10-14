package net.anubis.client.module.impl.donutmacros;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.anubis.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;

@Environment(EnvType.CLIENT)
final class OrderRestock {
   private static final int ORDER_SLOTS = 45;
   private static final int DEFAULT_COLLECT_BUTTON = 13;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long DIALOG_TIMEOUT_MS = 8000L;
   private static final long PICK_TIMEOUT_MS = 5000L;
   private final OrderAhBot bot;
   private final MacroWorker worker;
   private final Gui gui;
   private final Product product;
   private long nextCheckAt;

   OrderRestock(OrderAhBot bot) {
      this.bot = bot;
      this.worker = bot.worker;
      this.gui = bot.gui;
      this.product = bot.product;
   }

   private void say(ModuleChat.Body body) {
      this.bot.say(body);
   }

   private static AuctionHouse.Problem fail(String message) {
      return new AuctionHouse.Problem(message);
   }

   boolean run() {
      long now = System.currentTimeMillis();
      if (now < this.nextCheckAt) {
         return false;
      } else {
         this.nextCheckAt = now + this.bot.config.orderCheckMs();
         if (!this.openYourOrders()) {
            return false;
         } else {
            int own = (Integer)this.worker.call(() -> Gui.findNow((stack) -> stack.method_31574(this.product.item()), 45));
            if (own >= 0) {
               return this.collect(own) > 0;
            } else {
               this.gui.closeAll();
               long price = this.orderPrice();
               if (price <= 0L) {
                  return false;
               } else {
                  this.create(price);
                  return false;
               }
            }
         }
      }
   }

   private boolean openYourOrders() {
      return OrdersNav.openYourOrders(this.worker, this.gui, this::say);
   }

   private boolean isYourOrdersNow() {
      return OrdersNav.isYourOrdersNow();
   }

   private static boolean isNewOrderPane(class_1799 stack) {
      return OrdersNav.isNewOrderPane(stack);
   }

   private int collect(int ownSlot) {
      String before = this.gui.fingerprint();
      this.gui.click(ownSlot, 0, class_1713.field_7790);
      boolean opened = this.worker.waitFor(() -> Gui.containerOpenNow() && !this.isYourOrdersNow() && !Gui.fingerprintNow().equals(before), 2000L);
      if (!opened) {
         this.say(ModuleChat.body().warn("Your order did not open."));
         this.gui.closeAll();
         return 0;
      } else {
         this.worker.sleep(1000L);
         int[] controls = (int[])this.worker.call(() -> {
            List<class_1799> stacks = Gui.containerNow();
            int collectAt = -1;

            for(int i = 0; i < stacks.size(); ++i) {
               class_1799 stack = (class_1799)stacks.get(i);
               if (!stack.method_7960()) {
                  String name = Gui.nameOf(stack);
                  if (stack.method_31574(class_1802.field_8353) || name.contains("cancel")) {
                     return new int[]{1, -1};
                  }

                  if (collectAt < 0 && name.contains("collect")) {
                     collectAt = i;
                  }
               }
            }

            if (collectAt < 0 && stacks.size() > 13 && !((class_1799)stacks.get(13)).method_7960()) {
               collectAt = 13;
            }

            return new int[]{0, collectAt};
         });
         if (controls[0] == 1) {
            this.say(ModuleChat.body().text("Your ").name(this.product.displayName()).text(" order is still filling."));
            this.gui.closeAll();
            return 0;
         } else if (controls[1] < 0) {
            this.say(ModuleChat.body().warn("No collect button in your order."));
            this.gui.closeAll();
            return 0;
         } else {
            String beforeCollect = this.gui.fingerprint();
            this.gui.click(controls[1], 0, class_1713.field_7790);
            this.worker.sleep(250L);
            this.gui.waitChange(beforeCollect, 2000L);
            this.worker.sleep(150L);
            int collected = 0;

            for(int pass = 0; pass < 6 && (Boolean)this.worker.call(Gui::containerOpenNow) && !this.bot.inventoryCapReached(); ++pass) {
               boolean clicked = false;
               List<class_1799> stacks = this.gui.container();

               for(int i = 0; i < stacks.size(); ++i) {
                  class_1799 stack = (class_1799)stacks.get(i);
                  if (!stack.method_7960() && stack.method_31574(this.product.item())) {
                     if (this.bot.inventoryCapReached()) {
                        break;
                     }

                     collected += stack.method_7947();
                     this.gui.click(i, 0, class_1713.field_7794);
                     clicked = true;
                     this.worker.sleep(40L);
                  }
               }

               if (!clicked) {
                  break;
               }

               this.worker.ticks(1);
            }

            this.gui.closeAll();
            if (collected > 0) {
               this.say(ModuleChat.body().good("Collected " + collected + "x").text(" ").name(this.product.displayName()).text(" from your order."));
            }

            return collected;
         }
      }
   }

   private long orderPrice() {
      long price;
      if (!this.product.beatTopOrder()) {
         price = this.product.fixedPrice();
      } else {
         double top = (double)-1.0F;

         for(int attempt = 1; attempt <= 3 && top <= (double)0.0F; ++attempt) {
            this.gui.closeAll();
            this.worker.sleep(attempt == 1 ? 800L : 1400L);
            Gui var10000 = this.gui;
            String var10001 = this.product.search();
            var10000.command("order " + var10001);
            top = this.waitTopOrder(attempt == 3 ? 7000L : 4500L);
         }

         this.gui.closeAll();
         if (top <= (double)0.0F) {
            this.say(ModuleChat.body().warn("No order price found").text(" for ").name(this.product.describe()).text(this.product.enchants().isEmpty() ? ", trying later." : ". Use a Fixed order price for enchanted items."));
            return -1L;
         }

         price = (long)Math.floor(top) + this.product.bidIncrement();
         this.say(ModuleChat.body().text("Top order is $").value(ServerText.shortMoney(top)).text(", ordering at $").value(ServerText.shortMoney((double)price)).text(" each."));
      }

      if (this.product.maxPrice() > 0L && price > this.product.maxPrice()) {
         this.say(ModuleChat.body().text("Capped at your max price $").value(ServerText.shortMoney((double)this.product.maxPrice())).text("."));
         price = this.product.maxPrice();
      }

      return price;
   }

   private double waitTopOrder(long timeoutMs) {
      long end = System.currentTimeMillis() + timeoutMs;

      while(System.currentTimeMillis() < end) {
         double price = (Double)this.worker.call(() -> {
            if (!Gui.containerOpenNow()) {
               return (double)-1.0F;
            } else {
               for(class_1799 stack : Gui.containerNow()) {
                  if (!stack.method_7960() && stack.method_31574(this.product.item()) && (this.product.enchants().isEmpty() || this.product.matches(stack))) {
                     return ServerText.orderPrice(stack);
                  }
               }

               return (double)-1.0F;
            }
         });
         if (price > (double)0.0F) {
            return price;
         }

         this.worker.sleep(50L);
      }

      return (double)-1.0F;
   }

   private void create(long price) {
      if (this.openYourOrders()) {
         int pane = (Integer)this.worker.call(() -> Gui.findNow(OrderRestock::isNewOrderPane, 45));
         if (pane < 0) {
            this.say(ModuleChat.body().warn("No free order slot").text(" (no New Order button)."));
            this.gui.closeAll();
         } else {
            this.gui.click(pane, 0, class_1713.field_7790);
            Dialogs.View view = this.waitDialog((v) -> v.titled("item"), 10000L);
            if (view == null) {
               throw fail("Choose Item did not open");
            } else {
               if (!view.textInputs().isEmpty()) {
                  Dialogs.Button search = view.labelled("search");
                  view = this.press(view, search, Map.of((String)view.textInputs().get(0), this.product.search()));
               }

               Dialogs.Button item = view.find((button) -> this.product.nameMatches(button.label(), false));
               if (item == null) {
                  item = this.closestLoose(view);
               }

               if (item == null) {
                  String var15 = this.product.displayName();
                  throw fail(var15 + " is not in the search results, " + view.describe());
               } else {
                  view = this.press(view, item, Map.of());
                  this.checkItem(view);
                  if (view.titled("enchant")) {
                     view = this.pickEnchants(view);
                  } else if (!this.product.enchants().isEmpty()) {
                     String var10000 = this.product.displayName();
                     throw fail("No enchantment menu for " + var10000 + ", " + view.describe());
                  }

                  if (!view.titled("how many") && !view.titled("amount")) {
                     throw fail("Expected the amount menu, got " + view.describe());
                  } else {
                     view = this.press(view, view.labelled("next", "continue"), textFor(view, Integer.toString(this.product.orderAmount())));
                     if (!view.titled("price")) {
                        throw fail("Expected the price menu, got " + view.describe());
                     } else {
                        view = this.press(view, view.labelled("review order", "review", "next"), textFor(view, ServerText.exactMoney(price)));
                        if (!view.titled("review")) {
                           throw fail("Expected the review menu, got " + view.describe());
                        } else {
                           this.checkItem(view);
                           String problem = this.checkReview(view, price);
                           if (problem != null) {
                              Dialogs.Button cancel = view.labelled("cancel");
                              if (cancel != null) {
                                 this.worker.call(() -> Dialogs.press(Gui.mc(), view, cancel, Map.of()));
                              }

                              throw fail("Order review did not match (" + problem + "), cancelled");
                           } else {
                              long mark = System.currentTimeMillis();
                              Dialogs.Button createButton = view.labelled("create order", "create", "confirm");
                              if (createButton == null) {
                                 throw fail("No Create Order button, " + view.describe());
                              } else if (!(Boolean)this.worker.call(() -> Dialogs.press(Gui.mc(), view, createButton, Map.of()))) {
                                 throw fail("The review menu closed");
                              } else {
                                 this.worker.waitFor(() -> this.bot.orderedAt >= mark || Dialogs.current(Gui.mc()) == null || Dialogs.current(Gui.mc()).dialog() != view.dialog(), 8000L);
                                 this.worker.sleep(300L);
                                 this.gui.closeAll();
                                 this.say(ModuleChat.body().good("Ordered " + this.product.orderAmount() + "x").text(" ").name(this.product.describe()).text(" at $").value(ServerText.shortMoney((double)price)).text(" each."));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Dialogs.Button closestLoose(Dialogs.View view) {
      Dialogs.Button best = null;

      for(Dialogs.Button button : view.buttons()) {
         if (this.product.nameMatches(button.label(), true) && (best == null || button.key().length() < best.key().length())) {
            best = button;
         }
      }

      return best;
   }

   private void checkItem(Dialogs.View view) {
      if (!view.items().isEmpty()) {
         for(class_1799 shown : view.items()) {
            if (shown.method_31574(this.product.item())) {
               return;
            }
         }

         String picked = ((class_1799)view.items().get(0)).method_7964().getString();
         this.worker.run(() -> Dialogs.close(Gui.mc()));
         throw fail("Picked " + picked + " instead of " + this.product.displayName() + ", cancelled");
      }
   }

   private Dialogs.View pickEnchants(Dialogs.View view) {
      if (this.product.enchants().isEmpty()) {
         return this.press(view, view.labelled("skip enchantments", "skip"), Map.of());
      } else {
         for(Product.Enchant wanted : this.product.enchants()) {
            Dialogs.Button level = levelButton(view, wanted);
            if (level == null) {
               String var10000 = wanted.label();
               throw fail(var10000 + " is not offered, " + view.describe());
            }

            if (!level.green()) {
               if (!(Boolean)this.worker.call(() -> Dialogs.press(Gui.mc(), view, level, Map.of()))) {
                  throw fail("The menu changed before picking " + wanted.label());
               }

               Dialogs.View next = this.waitDialog((v) -> v.dialog() != view.dialog(), 5000L);
               if (next == null) {
                  next = (Dialogs.View)this.worker.call(() -> Dialogs.current(Gui.mc()));
                  if (next == null) {
                     throw fail("The enchantment menu closed after picking " + wanted.label());
                  }
               }

               view = next;
               Dialogs.Button again = levelButton(next, wanted);
               if (again != null && !again.green()) {
                  this.say(ModuleChat.body().warn("Could not confirm ").text(wanted.label()).text(" was picked, continuing."));
               }
            }
         }

         return this.press(view, view.labelled("continue", "confirm", "done", "next"), Map.of());
      }
   }

   static Dialogs.Button levelButton(Dialogs.View view, Product.Enchant wanted) {
      String header = "";

      for(Dialogs.Button button : view.buttons()) {
         int level = ServerText.roman(button.label());
         if (level > 0) {
            if (header.equals(wanted.key()) && level == wanted.level()) {
               return button;
            }
         } else if (!button.label().isBlank()) {
            header = ServerText.key(ServerText.withoutRoman(button.label()));
         }
      }

      return null;
   }

   private String checkReview(Dialogs.View view, long price) {
      for(String line : view.body()) {
         if (line.startsWith("amount")) {
            List<Double> values = ServerText.moneyAll(line);
            if (!values.isEmpty() && Math.abs((Double)values.get(0) - (double)this.product.orderAmount()) >= ServerText.displayStep(line)) {
               return line;
            }
         }
      }

      for(String line : view.body()) {
         if (line.startsWith("price")) {
            List<Double> values = ServerText.moneyAll(line);
            if (!values.isEmpty()) {
               double shown = (Double)values.get(0);
               if (Math.abs(shown - (double)price) > ServerText.displayStep(line)) {
                  return line;
               }
            }
         }
      }

      return null;
   }

   private static Map<String, String> textFor(Dialogs.View view, String value) {
      return view.textInputs().isEmpty() ? Map.of() : Map.of((String)view.textInputs().get(0), value);
   }

   private Dialogs.View press(Dialogs.View view, Dialogs.Button button, Map<String, String> text) {
      return this.press(view, button, text, 8000L);
   }

   private Dialogs.View press(Dialogs.View view, Dialogs.Button button, Map<String, String> text, long timeoutMs) {
      if (button == null) {
         throw fail("Button missing, " + view.describe());
      } else if (!(Boolean)this.worker.call(() -> Dialogs.press(Gui.mc(), view, button, text))) {
         throw fail("The menu changed before clicking " + button.label());
      } else {
         Dialogs.View next = this.waitDialog((v) -> v.dialog() != view.dialog(), timeoutMs);
         if (next == null) {
            throw fail("No answer after clicking '" + button.label() + "'");
         } else {
            return next;
         }
      }
   }

   private Dialogs.View waitDialog(Predicate<Dialogs.View> test, long timeoutMs) {
      long end = System.currentTimeMillis() + timeoutMs;

      while(System.currentTimeMillis() < end) {
         Dialogs.View view = (Dialogs.View)this.worker.call(() -> Dialogs.current(Gui.mc()));
         if (view != null && test.test(view)) {
            return view;
         }

         this.worker.sleep(50L);
      }

      return null;
   }
}
