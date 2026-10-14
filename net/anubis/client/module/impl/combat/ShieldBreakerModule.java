package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.impl.combat.melee.SlotReturn;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10707;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3966;
import net.minecraft.class_640;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class ShieldBreakerModule extends ClientModule {
   private static final String MODE_PASSIVE = "Passive";
   private static final String MODE_AUTO = "Auto";
   private static final int REARM_TICKS = 10;
   private static final int MAX_LEAD_TICKS = 4;
   private final ModeSetting mode = (ModeSetting)this.addSetting("mode", new ModeSetting("Mode", "Passive", new String[]{"Passive", "Auto"}));
   private final BooleanSetting switchBack = (BooleanSetting)this.addSetting("switch-back", new BooleanSetting("Switch Back", true));
   private final SlotReturn slots = new SlotReturn(this);
   private int ticks;
   private int rearmTick;

   public ShieldBreakerModule() {
      super("shield-breaker", "Shield Breaker", "Swaps to an axe for any hit into a raised shield. Auto also makes the hit.", ModuleCategory.COMBAT);
   }

   protected void onEnable() {
      this.ticks = 0;
      this.rearmTick = 0;
   }

   protected void onDisable() {
      this.slots.drop(class_310.method_1551(), this.switchBack.get());
   }

   public void beforeAttack(class_746 player, class_1297 target) {
      if (target instanceof class_1657 other) {
         if (!MeleeAttacks.disablesShields(player.method_6047())) {
            if (!MeleeAttacks.shieldBroken(other) && blocksSoon(player, other)) {
               int slot = bestAxe(player);
               if (slot >= 0 && this.slots.take(player, slot)) {
                  MeleeAttacks.noteShieldBroken(other);
               }

               return;
            }

            return;
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent event) {
      ++this.ticks;
      this.slots.tick(class_310.method_1551(), this.switchBack.get());
   }

   @EventHandler
   public void onAutoTick(TickEvent event) {
      if (this.mode.is("Auto")) {
         this.tryHit(class_310.method_1551());
      }

   }

   private void tryHit(class_310 mc) {
      if (this.ticks >= this.rearmTick && MeleeAttacks.canAct(mc)) {
         if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
            class_746 player = mc.field_1724;
            if (!MeleeAttacks.actedThisTick(player) && !ServerRotation.pending() && !MeleeAttacks.clickPending(mc)) {
               class_3966 hit = MeleeAttacks.crosshairHit(mc);
               if (hit != null) {
                  class_1297 var5 = hit.method_17782();
                  if (var5 instanceof class_1657) {
                     class_1657 target = (class_1657)var5;
                     if (MeleeAttacks.isEnemy(player, target)) {
                        if (!MeleeAttacks.shieldBroken(target) && blocksSoon(player, target)) {
                           if (!MeleeAttacks.disablesShields(player.method_6047()) && bestAxe(player) < 0) {
                              return;
                           }

                           if (MeleeAttacks.attack(mc, hit)) {
                              this.rearmTick = this.ticks + 10;
                              MeleeAttacks.claimTick(mc);
                           }

                           return;
                        }

                        return;
                     }
                  }
               }

            }
         }
      }
   }

   private static boolean blocksSoon(class_746 player, class_1657 target) {
      if (!target.method_6115()) {
         return false;
      } else {
         class_1799 shield = target.method_6030();
         class_10707 blocks = (class_10707)shield.method_58694(class_9334.field_56396);
         return blocks != null && target.method_6048() + leadTicks(player) >= blocks.method_67197() ? MeleeAttacks.shieldBlocks(target, player, shield) : false;
      }
   }

   private static int leadTicks(class_746 player) {
      class_640 info = player.field_3944.method_2871(player.method_5667());
      return info == null ? 0 : class_3532.method_15340(Math.round((float)info.method_2959() / 50.0F), 0, 4);
   }

   static int bestAxe(class_746 player) {
      class_1661 inventory = player.method_31548();
      int best = -1;
      double bestDamage = Double.NEGATIVE_INFINITY;

      for(int slot = 0; slot < class_1661.method_7368(); ++slot) {
         class_1799 stack = inventory.method_5438(slot);
         if (MeleeAttacks.disablesShields(stack) && !stack.method_63692()) {
            double damage = MeleeAttacks.attackDamage(stack);
            if (damage > bestDamage) {
               best = slot;
               bestDamage = damage;
            }
         }
      }

      return best;
   }
}
