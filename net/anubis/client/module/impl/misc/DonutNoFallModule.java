package net.anubis.client.module.impl.misc;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_2828;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class DonutNoFallModule extends ClientModule {
   private static final double TRIGGER_FALL_DISTANCE = (double)3.0F;
   private static final int GROUND_PACKETS_TO_RESET = 4;
   private boolean noFall;
   private double lastFallDistance;
   private int groundTicks;
   private boolean sendingReplacement;

   public DonutNoFallModule() {
      super("donut-nofall", "Donut NoFall", "Take no fall damage on DonutSMP.", ModuleCategory.DONUT);
   }

   protected void onEnable() {
      this.noFall = false;
      this.lastFallDistance = (double)0.0F;
      this.groundTicks = 0;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (player != null) {
         if (this.noFall) {
            player.method_24830(false);
         }

         if (this.lastFallDistance > (double)3.0F) {
            this.noFall = true;
         }

         this.lastFallDistance = player.field_6017;
      }
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent event) {
      if (!this.sendingReplacement) {
         class_2596 var3 = event.packet;
         if (var3 instanceof class_2828) {
            class_2828 move = (class_2828)var3;
            class_310 mc = class_310.method_1551();
            class_746 player = mc.field_1724;
            class_634 connection = mc.method_1562();
            if (player != null && connection != null) {
               event.setCancelled(true);
               if (this.noFall && move.method_12273()) {
                  ++this.groundTicks;
               }

               if (this.groundTicks % 2 == 1) {
                  player.method_23327(player.method_23317(), player.method_23318() + 1.0E-8, player.method_23321());
               }

               boolean onGround = !this.noFall && player.method_24828();
               this.sendingReplacement = true;

               try {
                  connection.method_52787(new class_2828.class_2830(player.method_23317(), player.method_23318(), player.method_23321(), player.method_36454(), player.method_36455(), onGround, false));
               } finally {
                  this.sendingReplacement = false;
               }

               if (this.groundTicks == 4) {
                  this.noFall = false;
                  this.groundTicks = 0;
               }

               return;
            }

            return;
         }
      }

   }
}
