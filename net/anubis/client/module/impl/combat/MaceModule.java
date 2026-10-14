package net.anubis.client.module.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Objects;
import java.util.Optional;
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
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.InputControl;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_1893;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3966;
import net.minecraft.class_5134;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_746;
import net.minecraft.class_9362;

@Environment(EnvType.CLIENT)
public final class MaceModule extends ClientModule {
   private static final String PICK_DAMAGE = "Damage";
   private static final String PICK_WIND_BURST = "Wind Burst";
   private static final String SMASH_DIRECT = "Direct";
   private static final String SMASH_DELAYED = "Delayed";
   private static final double REACH_MARGIN = 0.35;
   private static final float BREACH_GAIN = 1.15F;
   private final BooleanSetting autoSwap = (BooleanSetting)this.addSetting("auto-swap", new BooleanSetting("Auto Swap", true));
   private final ModeSetting pick = (ModeSetting)this.addSetting("pick", new ModeSetting("Pick", "Damage", new String[]{"Damage", "Wind Burst"}));
   private final BooleanSetting breachSwap = (BooleanSetting)this.addSetting("breach-swap", new BooleanSetting("Breach Swap", false));
   private final BooleanSetting swapBack = (BooleanSetting)this.addSetting("swap-back", new BooleanSetting("Swap Back", true));
   private final BooleanSetting smashAssist = (BooleanSetting)this.addSetting("smash-assist", new BooleanSetting("Smash Assist", false));
   private final ModeSetting smashTiming = (ModeSetting)this.addSetting("smash-timing", new ModeSetting("Smash Timing", "Delayed", new String[]{"Direct", "Delayed"}));
   private final NumberSetting minFall = (NumberSetting)this.addSetting("min-fall", (new NumberSetting("Min Fall", 3.0F, 1.5F, 30.0F)).step(0.5F));
   private final BooleanSetting mobs = (BooleanSetting)this.addSetting("mobs", new BooleanSetting("Mobs", false));
   private final SlotReturn slots = new SlotReturn(this);
   private boolean smashed;

   public MaceModule() {
      super("mace", "Mace", "Swaps to the strongest mace for smash hits, with an optional smash assist.", ModuleCategory.COMBAT);
      ModeSetting var10000 = this.pick;
      BooleanSetting var10001 = this.autoSwap;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      BooleanSetting var1 = this.breachSwap;
      var10001 = this.autoSwap;
      Objects.requireNonNull(var10001);
      var1.visibleWhen(var10001::get);
      this.swapBack.visibleWhen(() -> this.autoSwap.get() || this.smashAssist.get());
      ModeSetting var2 = this.smashTiming;
      var10001 = this.smashAssist;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      NumberSetting var3 = this.minFall;
      var10001 = this.smashAssist;
      Objects.requireNonNull(var10001);
      var3.visibleWhen(var10001::get);
      BooleanSetting var4 = this.mobs;
      var10001 = this.smashAssist;
      Objects.requireNonNull(var10001);
      var4.visibleWhen(var10001::get);
   }

   protected void onDisable() {
      this.smashed = false;
      this.slots.drop(class_310.method_1551(), this.swapBack.get());
   }

   public void beforeAttack(class_746 player, class_1297 target) {
      if (this.autoSwap.get() && target instanceof class_1309 living) {
         if (!MeleeAttacks.shieldBlocks(living, player) || MeleeAttacks.shieldBroken(living)) {
            boolean smash = class_9362.method_58659(player);
            if (smash || this.breachSwap.get()) {
               int slot = this.bestMace(player, living, smash);
               if (slot >= 0) {
                  if (smash || !(expected(player, player.method_31548().method_5438(slot), living) < expected(player, player.method_6047(), living) * 1.15F)) {
                     this.slots.take(player, slot);
                  }
               }
            }
         }
      }
   }

   private int bestMace(class_746 player, class_1309 target, boolean smash) {
      boolean windBurst = smash && this.pick.is("Wind Burst");
      class_1661 inventory = player.method_31548();
      int best = -1;
      boolean bestBurst = false;
      float bestDamage = 0.0F;

      for(int slot = 0; slot < class_1661.method_7368(); ++slot) {
         class_1799 stack = inventory.method_5438(slot);
         if (stack.method_31574(class_1802.field_49814) && !stack.method_63692() && (smash || level(stack, class_1893.field_50158) != 0)) {
            boolean burst = windBurst && level(stack, class_1893.field_50159) > 0;
            float damage = expected(player, stack, target);
            if (best < 0 || (!bestBurst || burst) && (bestBurst != burst || !(damage <= bestDamage))) {
               best = slot;
               bestBurst = burst;
               bestDamage = damage;
            }
         }
      }

      return best;
   }

   private static float expected(class_746 player, class_1799 stack, class_1309 target) {
      float charge = MeleeAttacks.charge(player);
      double fall = player.field_6017;
      float damage = (float)((double)1.0F + MeleeAttacks.attackDamage(player.method_6047())) * (0.2F + 0.8F * charge * charge);
      if (stack.method_31574(class_1802.field_49814) && class_9362.method_58659(player)) {
         damage += (float)(smashBonus(fall) + (double)0.5F * (double)level(stack, class_1893.field_50157) * fall);
      }

      if (MeleeAttacks.wouldCrit(player, target)) {
         damage *= 1.5F;
      }

      int sharpness = level(stack, class_1893.field_9118);
      if (sharpness > 0) {
         damage += charge * (0.5F * (float)sharpness + 0.5F);
      }

      return afterArmor(target, damage, level(stack, class_1893.field_50158));
   }

   private static double smashBonus(double fall) {
      if (fall <= (double)3.0F) {
         return (double)4.0F * fall;
      } else {
         return fall <= (double)8.0F ? (double)12.0F + (double)2.0F * (fall - (double)3.0F) : (double)22.0F + fall - (double)8.0F;
      }
   }

   private static float afterArmor(class_1309 target, float damage, int breach) {
      float armor = (float)target.method_6096();
      float toughness = (float)target.method_45325(class_5134.field_23725);
      float cut = class_3532.method_15363(armor - damage / (2.0F + toughness / 4.0F), armor * 0.2F, 20.0F) / 25.0F;
      return damage * (1.0F - class_3532.method_15363(cut - 0.15F * (float)breach, 0.0F, 1.0F));
   }

   private static int level(class_1799 stack, class_5321<class_1887> enchantment) {
      for(Object2IntMap.Entry<class_6880<class_1887>> entry : stack.method_58657().method_57539()) {
         if (((class_6880)entry.getKey()).method_40225(enchantment)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.field_1724.method_24828()) {
         this.smashed = false;
      }

      this.slots.tick(mc, this.swapBack.get());
   }

   @EventHandler
   public void onSmashTick(TickEvent event) {
      if (this.smashAssist.get()) {
         this.trySmash(class_310.method_1551());
      }

   }

   private void trySmash(class_310 mc) {
      if (MeleeAttacks.canAct(mc) && !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
         class_746 player = mc.field_1724;
         if (!MeleeAttacks.actedThisTick(player) && !MeleeAttacks.clickPending(mc) && !MeleeAttacks.rightClickComing(mc) && !ServerRotation.pending() && mc.field_1771 <= 0 && MeleeAttacks.handsFree(mc, player)) {
            if (!(player.method_18798().field_1351 >= (double)0.0F)) {
               class_3966 hit = MeleeAttacks.crosshairHit(mc);
               if (hit != null) {
                  class_1297 var5 = hit.method_17782();
                  if (var5 instanceof class_1309) {
                     class_1309 target = (class_1309)var5;
                     if (MeleeAttacks.isEnemy(player, target)) {
                        if (!(target instanceof class_1657) && !this.mobs.get()) {
                           return;
                        }

                        if (!this.smashReady(player, target)) {
                           return;
                        }

                        if (MeleeAttacks.shieldBlocks(target, player) && !MeleeAttacks.shieldBroken(target)) {
                           int axe = ShieldBreakerModule.bestAxe(player);
                           if (canHoldOneTick(player, target) && axe >= 0 && (this.autoSwap.get() || this.swapBack.get()) && this.slots.take(player, axe)) {
                              if (MeleeAttacks.attack(mc, hit)) {
                                 MeleeAttacks.noteShieldBroken(target);
                                 MeleeAttacks.claimTick(mc);
                              } else {
                                 this.slots.drop(mc, true);
                              }
                           }

                           return;
                        }

                        if (this.holdsSmash(player, target)) {
                           InputControl.dropSprint();
                           mc.field_1771 = Math.max(mc.field_1771, 1);
                           return;
                        }

                        if (MeleeAttacks.attack(mc, hit)) {
                           this.smashed = true;
                           MeleeAttacks.claimTick(mc);
                        }

                        return;
                     }
                  }
               }

            }
         }
      }
   }

   public void noteSmash() {
      this.smashed = true;
   }

   public boolean smashReady(class_746 player, class_1309 target) {
      return this.isEnabled() && !this.smashed && class_9362.method_58659(player) && player.field_6017 >= (double)this.minFall.get() && (player.method_6047().method_31574(class_1802.field_49814) || this.autoSwap.get() && this.bestMace(player, target, true) >= 0);
   }

   public boolean holdsSmash(class_746 player, class_1309 target) {
      boolean chargeSoon = MeleeAttacks.charge(player) <= 0.9F && player.method_7261(1.5F) > 0.9F;
      return canHoldOneTick(player, target) && (this.smashTiming.is("Delayed") || chargeSoon || target.field_6235 > 0 || player.method_5624());
   }

   private static boolean canHoldOneTick(class_746 player, class_1309 target) {
      class_243 motion = player.method_18798();
      if (!player.method_73183().method_8587(player, player.method_5829().method_997(motion))) {
         return false;
      } else {
         double reach = player.method_55755() - 0.35;
         if (reach <= (double)0.0F) {
            return false;
         } else {
            class_243 nextEye = player.method_33571().method_1019(motion);
            class_243 targetStep = target.method_73189().method_1023(target.field_6014, target.field_6036, target.field_5969);
            class_238 nextBox = target.method_5829().method_997(targetStep).method_1014((double)target.method_5871());
            if (nextBox.method_1006(nextEye)) {
               return true;
            } else {
               Optional<class_243> aim = nextBox.method_992(nextEye, nextEye.method_1019(player.method_5828(1.0F).method_1021(reach)));
               return aim.isPresent() && MeleeAttacks.lineIsClear(player, nextEye, (class_243)aim.get());
            }
         }
      }
   }
}
