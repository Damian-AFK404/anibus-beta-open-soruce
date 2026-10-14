package net.anubis.client.module.impl.donutmacros;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.setting.StringListSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_465;
import net.minecraft.class_634;
import net.minecraft.class_746;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
final class SellRoutine {
   private static final int INVENTORY_SLOTS = 36;
   private static final int CONFIRM_SLOT = 53;
   private static final int MAX_CONFIRMS = 3;
   private static final int STRIKES_TO_SKIP = 2;
   private static final int MAX_RESENDS = 3;
   private static final long CHECK_EVERY_MS = 500L;
   private static final long COMMAND_COOLDOWN_MS = 4000L;
   private static final long OPEN_TIMEOUT_MS = 10000L;
   private static final long STRAY_MENU_MS = 5000L;
   private static final long FAIL_COOLDOWN_MS = 30000L;
   private static final long CONFIRM_COOLDOWN_MS = 900L;
   private static final long POST_CONFIRM_MS = 250L;
   private static final long SKIP_MS = 600000L;
   private final ClientModule owner;
   private final StringListSetting neverSell;
   private final BiPredicate<Integer, class_1799> keep;
   private final Runnable onCommand;
   private State state;
   private long nextCheckAt;
   private long lastCommandAt;
   private long deadline;
   private boolean rejected;
   private long retryAt;
   private int resends;
   private int menuId;
   private int confirms;
   private final Map<class_1792, Integer> offered;
   private final Map<class_1792, Integer> strikes;
   private final Map<class_1792, Long> skipUntil;

   SellRoutine(ClientModule owner, StringListSetting neverSell, BiPredicate<Integer, class_1799> keep, Runnable onCommand) {
      this.state = SellRoutine.State.IDLE;
      this.menuId = -1;
      this.offered = new LinkedHashMap();
      this.strikes = new HashMap();
      this.skipUntil = new HashMap();
      this.owner = owner;
      this.neverSell = neverSell;
      this.keep = keep;
      this.onCommand = onCommand;
   }

   void reset() {
      this.state = SellRoutine.State.IDLE;
      this.nextCheckAt = 0L;
      this.lastCommandAt = 0L;
      this.rejected = false;
      this.menuId = -1;
      this.offered.clear();
      this.strikes.clear();
      this.skipUntil.clear();
   }

   boolean active() {
      return this.state != SellRoutine.State.IDLE;
   }

   void commandRejected(long retryAt) {
      if (this.state == SellRoutine.State.OPENING) {
         this.rejected = true;
         this.retryAt = retryAt;
      }
   }

   void abort(class_310 mc) {
      if (this.state != SellRoutine.State.IDLE) {
         class_746 player = mc.field_1724;
         if (player != null && player.field_7512 != player.field_7498 && player.field_7512.field_7763 == this.menuId) {
            player.method_7346();
         }

         this.state = SellRoutine.State.IDLE;
         this.menuId = -1;
         this.offered.clear();
         this.nextCheckAt = System.currentTimeMillis() + 500L;
      }
   }

   boolean step(class_310 mc, class_746 player, class_634 connection, long now, boolean canCommand) {
      boolean var10000;
      switch (this.state.ordinal()) {
         case 0 -> var10000 = this.stepIdle(mc, player, connection, now, canCommand);
         case 1 -> var10000 = this.stepOpening(mc, player, connection, now);
         case 2 -> var10000 = this.stepMoving(mc, player, now);
         case 3 -> var10000 = this.stepConfirming(mc, player, now);
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private boolean stepIdle(class_310 mc, class_746 player, class_634 connection, long now, boolean canCommand) {
      if (now - this.lastCommandAt < 5000L && sellMenuOpen(mc, player)) {
         player.method_7346();
         return true;
      } else if (now < this.nextCheckAt) {
         return false;
      } else {
         this.nextCheckAt = now + 500L;
         if (mc.field_1755 == null && canCommand && now - this.lastCommandAt >= 4000L) {
            if (!this.hasSellable(player.method_31548(), now)) {
               return false;
            } else {
               this.resends = 0;
               this.send(connection, now);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private void send(class_634 connection, long now) {
      connection.method_45730("sell");
      this.lastCommandAt = now;
      this.onCommand.run();
      this.deadline = now + 10000L;
      this.rejected = false;
      this.state = SellRoutine.State.OPENING;
   }

   private boolean stepOpening(class_310 mc, class_746 player, class_634 connection, long now) {
      if (sellMenuOpen(mc, player)) {
         this.menuId = player.field_7512.field_7763;
         this.state = SellRoutine.State.MOVING;
         return true;
      } else if (this.rejected && now >= this.retryAt && this.resends < 3) {
         ++this.resends;
         this.send(connection, now);
         return true;
      } else if (now < this.deadline) {
         return true;
      } else {
         this.state = SellRoutine.State.IDLE;
         ModuleChat.send(this.owner, ModuleChat.body().warn("The sell menu did not open").text(", trying again in 30s."));
         this.nextCheckAt = now + 30000L;
         return false;
      }
   }

   private boolean stepMoving(class_310 mc, class_746 player, long now) {
      class_1703 menu = player.field_7512;
      if (!this.ours(player)) {
         return this.finish((Map)null, now);
      } else {
         int moved = 0;

         for(class_1735 slot : menu.field_7761) {
            if (slot.field_7871 instanceof class_1661 && slot.method_7681()) {
               int index = slot.method_34266();
               if (index < 36 && this.sellable(index, slot.method_7677(), now)) {
                  mc.field_1761.method_2906(menu.field_7763, slot.field_7874, 0, class_1713.field_7794, player);
                  ++moved;
               }
            }
         }

         if (moved == 0) {
            return this.close(player, (Map)null, now);
         } else {
            this.offered.clear();
            this.offered.putAll(waitingItems(menu));
            this.confirms = 0;
            this.deadline = now;
            this.state = SellRoutine.State.CONFIRMING;
            return true;
         }
      }
   }

   private boolean stepConfirming(class_310 mc, class_746 player, long now) {
      if (!this.ours(player)) {
         return this.finish((Map)null, now);
      } else if (now < this.deadline) {
         return true;
      } else {
         class_1703 menu = player.field_7512;
         if (this.confirms < 3 && itemsWaiting(menu)) {
            mc.field_1761.method_2906(menu.field_7763, 53, 0, class_1713.field_7790, player);
            ++this.confirms;
            this.deadline = now + (this.confirms == 1 ? 250L : 900L);
            return true;
         } else {
            return this.close(player, waitingItems(menu), now);
         }
      }
   }

   private boolean close(class_746 player, Map<class_1792, Integer> left, long now) {
      if (this.ours(player)) {
         player.method_7346();
      }

      return this.finish(left, now);
   }

   private boolean finish(Map<class_1792, Integer> left, long now) {
      if (left != null) {
         this.settle(left, now);
      }

      this.offered.clear();
      this.menuId = -1;
      this.state = SellRoutine.State.IDLE;
      this.nextCheckAt = now + 500L;
      return false;
   }

   private void settle(Map<class_1792, Integer> left, long now) {
      int sold = 0;

      for(Map.Entry<class_1792, Integer> entry : this.offered.entrySet()) {
         class_1792 item = (class_1792)entry.getKey();
         int remaining = (Integer)left.getOrDefault(item, 0);
         sold += Math.max(0, (Integer)entry.getValue() - remaining);
         if (remaining <= 0) {
            this.strikes.remove(item);
         } else if ((Integer)this.strikes.merge(item, 1, Integer::sum) >= 2) {
            this.strikes.remove(item);
            this.skipUntil.put(item, now + 600000L);
            ModuleChat.send(this.owner, ModuleChat.body().warn("Could not sell ").name(item.method_63680().getString()).text(", skipping it for 10 minutes."));
         }
      }

      if (sold > 0) {
         ModuleChat.send(this.owner, ModuleChat.body().text("Sold ").value(sold).text(sold == 1 ? " item." : " items."));
      }

   }

   private boolean ours(class_746 player) {
      return player.field_7512 != player.field_7498 && player.field_7512.field_7763 == this.menuId;
   }

   private boolean hasSellable(class_1661 inventory, long now) {
      for(int index = 0; index < 36; ++index) {
         class_1799 stack = inventory.method_5438(index);
         if (!stack.method_7960() && this.sellable(index, stack, now)) {
            return true;
         }
      }

      return false;
   }

   private boolean sellable(int index, class_1799 stack, long now) {
      return !this.skipped(stack.method_7909(), now) && !this.keep.test(index, stack) && !this.blacklisted(stack);
   }

   private boolean skipped(class_1792 item, long now) {
      Long until = (Long)this.skipUntil.get(item);
      if (until == null) {
         return false;
      } else if (now < until) {
         return true;
      } else {
         this.skipUntil.remove(item);
         return false;
      }
   }

   private boolean blacklisted(class_1799 stack) {
      return neverSell(stack, this.neverSell.get());
   }

   static boolean neverSell(class_1799 stack, List<String> entries) {
      String id = class_7923.field_41178.method_10221(stack.method_7909()).toString().toLowerCase(Locale.ROOT);
      String shortName = id.substring(id.indexOf(58) + 1);

      for(String raw : entries) {
         String entry = raw.trim().toLowerCase(Locale.ROOT);
         if (!entry.isEmpty()) {
            if (id.equals(entry) || shortName.equals(entry)) {
               return true;
            }

            if (entry.indexOf(58) < 0 && id.contains(entry)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean itemsWaiting(class_1703 menu) {
      for(class_1735 slot : menu.field_7761) {
         if (!(slot.field_7871 instanceof class_1661) && slot.field_7874 != 53 && slot.method_7681()) {
            return true;
         }
      }

      return false;
   }

   private static Map<class_1792, Integer> waitingItems(class_1703 menu) {
      Map<class_1792, Integer> items = new LinkedHashMap();

      for(class_1735 slot : menu.field_7761) {
         if (!(slot.field_7871 instanceof class_1661) && slot.field_7874 != 53 && slot.method_7681()) {
            items.merge(slot.method_7677().method_7909(), slot.method_7677().method_7947(), Integer::sum);
         }
      }

      return items;
   }

   private static boolean sellMenuOpen(class_310 mc, class_746 player) {
      class_1703 menu = player.field_7512;
      return menu != player.field_7498 && mc.field_1755 instanceof class_465 && containerSlots(menu) > 53;
   }

   private static int containerSlots(class_1703 menu) {
      int count = 0;

      for(class_1735 slot : menu.field_7761) {
         if (!(slot.field_7871 instanceof class_1661)) {
            ++count;
         }
      }

      return count;
   }

   @Environment(EnvType.CLIENT)
   private static enum State {
      IDLE,
      OPENING,
      MOVING,
      CONFIRMING;

      // $FF: synthetic method
      private static State[] $values() {
         return new State[]{IDLE, OPENING, MOVING, CONFIRMING};
      }
   }
}
