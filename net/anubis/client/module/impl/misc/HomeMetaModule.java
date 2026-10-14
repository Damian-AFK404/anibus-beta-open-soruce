package net.anubis.client.module.impl.misc;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class HomeMetaModule extends ClientModule {
   private static final double TELEPORT_DISTANCE_SQ = (double)25.0F;
   private final NumberSetting homeNumber = (NumberSetting)this.addSetting("home-number", (new NumberSetting("Home Number", 3.0F, 1.0F, 10.0F)).step(1.0F));
   private final NumberSetting delay = (NumberSetting)this.addSetting("delay", (new NumberSetting("Delay (s)", 0.5F, 0.0F, 10.0F)).step(0.5F));
   private final NumberSetting rtpTimeout = (NumberSetting)this.addSetting("rtp-max-timeout", (new NumberSetting("RTP Timeout (s)", 10.0F, 1.0F, 30.0F)).step(1.0F));
   private Step step;
   private long nextAtMs;
   private long rtpDeadlineMs;
   private class_243 rtpStart;
   private int home;

   public HomeMetaModule() {
      super("home-meta", "Home Meta", "Re-sets a home, RTPs away, then teleports back home.", ModuleCategory.BASE_RAID);
   }

   public boolean persistEnabled() {
      return false;
   }

   public boolean inConfigs() {
      return false;
   }

   protected void onEnable() {
      if (class_310.method_1551().field_1724 == null) {
         this.requestDisable();
      } else {
         this.home = this.homeNumber.getInt();
         this.step = HomeMetaModule.Step.DELHOME;
         this.nextAtMs = 0L;
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.method_1562() != null) {
         long now = System.currentTimeMillis();
         if (now >= this.nextAtMs) {
            long delayMs = (long)((double)this.delay.get() * (double)1000.0F);
            switch (this.step.ordinal()) {
               case 0:
                  command(mc, "delhome " + this.home);
                  this.step = HomeMetaModule.Step.SETHOME;
                  this.nextAtMs = now + delayMs;
                  break;
               case 1:
                  command(mc, "sethome " + this.home);
                  this.step = HomeMetaModule.Step.RTP;
                  this.nextAtMs = now + delayMs;
                  break;
               case 2:
                  this.rtpStart = mc.field_1724.method_73189();
                  command(mc, "rtp");
                  this.rtpDeadlineMs = now + (long)((double)this.rtpTimeout.get() * (double)1000.0F);
                  this.step = HomeMetaModule.Step.WAIT_TELEPORT;
                  break;
               case 3:
                  boolean moved = mc.field_1724.method_73189().method_1025(this.rtpStart) > (double)25.0F;
                  if (moved || now >= this.rtpDeadlineMs) {
                     this.step = HomeMetaModule.Step.HOME;
                     this.nextAtMs = now + delayMs;
                  }
                  break;
               case 4:
                  command(mc, "home " + this.home);
                  this.requestDisable();
            }

         }
      } else {
         this.requestDisable();
      }
   }

   private static void command(class_310 mc, String command) {
      mc.method_1562().method_45730(command);
   }

   @Environment(EnvType.CLIENT)
   private static enum Step {
      DELHOME,
      SETHOME,
      RTP,
      WAIT_TELEPORT,
      HOME;

      // $FF: synthetic method
      private static Step[] $values() {
         return new Step[]{DELHOME, SETHOME, RTP, WAIT_TELEPORT, HOME};
      }
   }
}
