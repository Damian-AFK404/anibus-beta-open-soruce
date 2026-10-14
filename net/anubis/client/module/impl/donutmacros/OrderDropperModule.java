package net.anubis.client.module.impl.donutmacros;

import java.util.List;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_746;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class OrderDropperModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final int ITEM_SLOTS = 45;
   private static final int DEFAULT_COLLECT_BUTTON = 13;
   private static final int DEFAULT_DROP_BUTTON = 52;
   private static final int DEFAULT_NEXT_BUTTON = 53;
   private static final int MAX_EMPTY_FLIPS = 2;
   private static final long NEXT_PAGE_SETTLE_MS = 250L;
   private static final long OPEN_TIMEOUT_MS = 5000L;
   private static final long PAGE_TIMEOUT_MS = 4000L;
   private final NumberSetting orderSlot = (NumberSetting)this.addSetting("order-slot", (new NumberSetting("Order Slot (0 = first)", 0.0F, 0.0F, 44.0F)).step(1.0F));
   private final NumberSetting pages = (NumberSetting)this.addSetting("pages", (new NumberSetting("Pages To Drop", 1.0F, 1.0F, 100.0F)).step(1.0F));
   private final NumberSetting delay = (NumberSetting)this.addSetting("delay", (new NumberSetting("Drop Delay (s)", 0.5F, 0.25F, 5.0F)).step(0.25F));
   private final MacroWorker worker = new MacroWorker("Anubis Order Dropper");
   private volatile boolean running;

   public OrderDropperModule() {
      super("order-dropper", "Order Dropper", "Opens one of your orders, collects it and drops the chosen number of pages, then turns off.", ModuleCategory.DONUT_MACROS);
      this.usesNotifications();
   }

   public boolean persistEnabled() {
      return false;
   }

   protected void onEnable() {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.method_1562() != null) {
         int slot = this.orderSlot.getInt();
         int wanted = this.pages.getInt();
         long delayMs = (long)(this.delay.get() * 1000.0F);
         this.running = true;
         this.worker.start(() -> {
            try {
               this.drop(new Gui(this.worker), slot, wanted, delayMs);
            } catch (AuctionHouse.Problem problem) {
               this.say(ModuleChat.body().warn(problem.getMessage()).text("."));
            } finally {
               this.finish(mc);
            }

         }, (failure) -> {
            LOGGER.error("[Anubis] Order Dropper stopped", failure);
            this.say(ModuleChat.body().danger("Stopped").text(": " + String.valueOf(failure)));
            this.finish(mc);
         });
      } else {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      }
   }

   private void finish(class_310 mc) {
      mc.execute(() -> {
         if (this.running && this.isEnabled()) {
            this.setEnabled(false);
         }

      });
   }

   protected void onDisable() {
      boolean wasRunning = this.running;
      this.running = false;
      this.worker.stop();
      class_746 player = class_310.method_1551().field_1724;
      if (wasRunning && player != null && player.field_7512 != player.field_7498) {
         player.method_7346();
      }

   }

   private void drop(Gui gui, int slot, int wanted, long delayMs) {
      String orderName = this.openCollect(gui, slot);
      int dropped = 0;
      int items = 0;

      for(int page = 0; page < wanted; ++page) {
         if (!(Boolean)this.worker.call(Gui::containerOpenNow)) {
            orderName = this.openCollect(gui, slot);
         }

         int[] state = (int[])this.worker.call(OrderDropperModule::pageState);

         for(int flips = 0; state[0] == 0 && state[2] >= 0 && flips < 2; ++flips) {
            String beforeNext = gui.fingerprint();
            gui.click(state[2], 0, class_1713.field_7790);
            gui.waitChange(beforeNext, 4000L);
            this.worker.sleep(250L);
            state = (int[])this.worker.call(OrderDropperModule::pageState);
         }

         if (state[0] == 0) {
            this.say(ModuleChat.body().text("The order is empty."));
            break;
         }

         if (state[1] < 0) {
            throw new AuctionHouse.Problem("No Drop button in the collect menu");
         }

         String before = gui.fingerprint();
         gui.click(state[1], 0, class_1713.field_7790);
         ++dropped;
         items += state[0];
         if (page < wanted - 1) {
            gui.waitChange(before, 4000L);
            this.worker.sleep(delayMs);
         }
      }

      this.worker.sleep(300L);
      gui.closeAll();
      this.say(ModuleChat.body().good("Dropped " + dropped + (dropped == 1 ? " page" : " pages")).text(" (").value(items).text(" items) from ").name(orderName).text("."));
   }

   private String openCollect(Gui gui, int slot) {
      if (!OrdersNav.openYourOrders(this.worker, gui, this::say)) {
         throw new AuctionHouse.Problem("Could not open Your Orders");
      } else {
         class_1799 order = (class_1799)this.worker.call(() -> Gui.slotNow(slot));
         if (OrdersNav.isPane(order)) {
            throw new AuctionHouse.Problem("There is no order in slot " + slot);
         } else {
            String name = order.method_7964().getString();
            String before = gui.fingerprint();
            gui.click(slot, 0, class_1713.field_7790);
            if (!this.worker.waitFor(() -> Gui.containerOpenNow() && !OrdersNav.isYourOrdersNow() && !Gui.fingerprintNow().equals(before), 5000L)) {
               throw new AuctionHouse.Problem("The order in slot " + slot + " did not open");
            } else {
               this.worker.sleep(300L);
               int collect = (Integer)this.worker.call(() -> {
                  List<class_1799> stacks = Gui.containerNow();

                  for(int i = 0; i < stacks.size(); ++i) {
                     if (!((class_1799)stacks.get(i)).method_7960() && Gui.nameOf((class_1799)stacks.get(i)).contains("collect")) {
                        return i;
                     }
                  }

                  return stacks.size() > 13 && !((class_1799)stacks.get(13)).method_7960() ? 13 : -1;
               });
               if (collect < 0) {
                  throw new AuctionHouse.Problem("No Collect button in the order");
               } else {
                  String beforeCollect = gui.fingerprint();
                  gui.click(collect, 0, class_1713.field_7790);
                  if (!this.worker.waitFor(() -> Gui.containerOpenNow() && !Gui.fingerprintNow().equals(beforeCollect) && dropButton(Gui.containerNow()) >= 0, 5000L)) {
                     throw new AuctionHouse.Problem("The collect menu did not open");
                  } else {
                     this.worker.sleep(300L);
                     return name;
                  }
               }
            }
         }
      }
   }

   private static int[] pageState() {
      List<class_1799> stacks = Gui.containerNow();
      int count = 0;

      for(int i = 0; i < 45 && i < stacks.size(); ++i) {
         count += ((class_1799)stacks.get(i)).method_7947();
      }

      return new int[]{count, dropButton(stacks), nextButton(stacks)};
   }

   private static int nextButton(List<class_1799> stacks) {
      for(int i = 45; i < stacks.size(); ++i) {
         class_1799 stack = (class_1799)stacks.get(i);
         if (!stack.method_7960() && Gui.nameOf(stack).contains("next")) {
            return i;
         }
      }

      return stacks.size() > 53 && ((class_1799)stacks.get(53)).method_31574(class_1802.field_8107) ? 53 : -1;
   }

   private static int dropButton(List<class_1799> stacks) {
      for(int i = 45; i < stacks.size(); ++i) {
         class_1799 stack = (class_1799)stacks.get(i);
         if (!stack.method_7960() && (stack.method_31574(class_1802.field_8878) || Gui.nameOf(stack).equals("drop"))) {
            return i;
         }
      }

      return stacks.size() > 52 && ((class_1799)stacks.get(52)).method_31574(class_1802.field_8878) ? 52 : -1;
   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, (ModuleChat.Body)body);
   }
}
