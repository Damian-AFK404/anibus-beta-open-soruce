package net.anubis.client.module.impl.combat;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.EventPriority;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.pvp.InputControl;
import net.anubis.client.util.pvp.Rotation;
import net.anubis.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12125;
import net.minecraft.class_12392;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_746;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public final class TriggerbotModule extends ClientModule {
   private static final String CRITS_OFF = "Off";
   private static final String CRITS_SMART = "Smart";
   private static final String CRITS_ONLY = "Only";
   private static final int TRACK_GRACE_TICKS = 4;
   private static final int SMART_MAX_WAIT_TICKS = 12;
   private final NumberSetting reaction = (NumberSetting)this.addSetting("reaction", (new NumberSetting("Reaction", 3.0F, 0.0F, 10.0F)).step(1.0F));
   private final ModeSetting crits = (ModeSetting)this.addSetting("crits", new ModeSetting("Crits", "Smart", new String[]{"Off", "Smart", "Only"}));
   private final BooleanSetting holdAttack = (BooleanSetting)this.addSetting("hold-attack", new BooleanSetting("Hold Attack", false));
   private final BooleanSetting players = (BooleanSetting)this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting hostiles = (BooleanSetting)this.addSetting("hostiles", new BooleanSetting("Hostiles", false));
   private final BooleanSetting invisible = (BooleanSetting)this.addSetting("invisible", new BooleanSetting("Invisible", false));
   private final BooleanSetting weaponsOnly = (BooleanSetting)this.addSetting("weapons-only", new BooleanSetting("Weapons Only", true));
   private int ticks;
   private int trackedId = -1;
   private int seenTick;
   private int reactionDoneTick;
   private int chargedTick = -1;
   private int chargePause;
   private int critWait;
   private int critWaitTick = -1;

   public TriggerbotModule() {
      super("triggerbot", "Triggerbot", "Hits the entity under the crosshair once the swing is charged, with a reaction delay.", ModuleCategory.COMBAT);
   }

   protected void onEnable() {
      this.reset();
   }

   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.ticks = 0;
      this.trackedId = -1;
      this.seenTick = 0;
      this.reactionDoneTick = 0;
      this.chargedTick = -1;
      this.chargePause = 0;
      this.critWait = 0;
      this.critWaitTick = -1;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onTick(TickEvent event) {
      ++this.ticks;
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (!MeleeAttacks.canAct(mc)) {
         this.trackedId = -1;
      } else {
         this.trackCharge(player);
         class_3966 hit = MeleeAttacks.crosshairHit(mc);
         if (hit != null) {
            class_1297 var6 = hit.method_17782();
            if (var6 instanceof class_1309) {
               class_1309 target = (class_1309)var6;
               if (this.isTarget(player, target)) {
                  if (target.method_5628() != this.trackedId) {
                     this.trackedId = target.method_5628();
                     this.reactionDoneTick = this.ticks + this.reaction.getInt() + MeleeAttacks.spread(2);
                  }

                  this.seenTick = this.ticks;
                  if (this.ticks >= this.reactionDoneTick && this.chargedTick >= 0 && this.ticks - this.chargedTick >= this.chargePause) {
                     if (this.holdAttack.get() && !mc.field_1690.field_1886.method_1434()) {
                        return;
                     }

                     if (this.weaponsOnly.get() && !MeleeAttacks.isMeleeWeapon(player.method_6047())) {
                        return;
                     }

                     if (!CombatMacroLock.heldByOther(this) && !MeleeAttacks.wastedOnShield(player, target)) {
                        if (mc.field_1771 <= 0 && !MeleeAttacks.clickPending(mc) && !MeleeAttacks.rightClickComing(mc) && !ServerRotation.pending()) {
                           boolean stabbing = player.method_6047().method_57826(class_9334.field_63631);
                           if (!stabbing && !this.critsAllow(mc, player, target)) {
                              return;
                           }

                           if (stabbing) {
                              hit = this.stabHit(player, target);
                              if (hit == null) {
                                 return;
                              }
                           }

                           if (stabbing) {
                              if (!MeleeAttacks.stab(mc, hit)) {
                                 return;
                              }
                           } else if (!MeleeAttacks.attack(mc, hit)) {
                              return;
                           }

                           this.chargedTick = -1;
                           MeleeAttacks.claimTick(mc);
                           return;
                        }

                        return;
                     }

                     return;
                  }

                  return;
               }
            }
         }

         if (this.ticks - this.seenTick > 4) {
            this.trackedId = -1;
         }

      }
   }

   private void trackCharge(class_746 player) {
      if (MeleeAttacks.charge(player) < 1.0F) {
         this.chargedTick = -1;
      } else if (this.chargedTick < 0) {
         this.chargedTick = this.ticks;
         this.chargePause = MeleeAttacks.spread(1);
      }

   }

   private class_3966 stabHit(class_746 player, class_1309 target) {
      Rotation sent = ServerRotation.sent();
      if (sent == null) {
         return null;
      } else {
         class_12392 range = player.method_76693();
         class_243 look = class_243.method_1030(sent.pitch(), sent.yaw());
         class_243 eye = player.method_33571();
         class_243 start = eye.method_1019(look.method_1021((double)range.method_76732(player)));
         class_243 end = eye.method_1019(look.method_1021((double)range.method_76739(player) + Math.max((double)0.0F, player.method_60478().method_1026(look))));
         float margin = range.comp_5262();
         class_243 at = (class_243)target.method_5829().method_1014((double)margin).method_992(eye, end).orElse((Object)null);
         if (at != null && MeleeAttacks.lineIsClear(player, eye, at)) {
            for(class_1297 entity : player.method_73183().method_8335(player, (new class_238(start, end)).method_1014((double)1.0F))) {
               if (entity != target && entity instanceof class_1309) {
                  class_1309 other = (class_1309)entity;
                  if (class_12125.method_75244(player, entity) && !this.isTarget(player, other)) {
                     class_238 box = entity.method_5829().method_1014((double)margin);
                     if (box.method_1006(start) || box.method_992(start, end).isPresent()) {
                        return null;
                     }
                  }
               }
            }

            return new class_3966(target, at);
         } else {
            return null;
         }
      }
   }

   private boolean isTarget(class_746 self, class_1309 entity) {
      if (!MeleeAttacks.isEnemy(self, entity)) {
         return false;
      } else if (entity.method_5767() && !this.invisible.get()) {
         return false;
      } else if (entity instanceof class_1657) {
         return this.players.get();
      } else {
         return this.hostiles.get() && MeleeAttacks.isHostile(entity);
      }
   }

   private boolean critsAllow(class_310 mc, class_746 player, class_1309 target) {
      boolean var10000;
      switch (this.crits.get()) {
         case "Only":
            if (MeleeAttacks.airborne(player) && player.method_5624() && player.field_6017 > (double)0.0F) {
               InputControl.dropSprint();
            }

            var10000 = MeleeAttacks.wouldCrit(player, target);
            break;
         case "Smart":
            if (MeleeAttacks.wouldCrit(player, target)) {
               var10000 = true;
            } else {
               boolean jumping = player.method_24828() && mc.field_1690.field_1903.method_1434() && !player.method_5624();
               if (!MeleeAttacks.critStance(player, target) && !jumping) {
                  var10000 = true;
               } else {
                  if (this.critWaitTick != this.ticks - 1) {
                     this.critWait = 0;
                  }

                  this.critWaitTick = this.ticks;
                  var10000 = this.critWait++ >= 12;
               }
            }
            break;
         default:
            var10000 = true;
      }

      return var10000;
   }
}
