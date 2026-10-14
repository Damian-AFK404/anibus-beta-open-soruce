package net.anubis.client.module.impl.movement.speed;

import net.anubis.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10185;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_243;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class VanillaSpeedMode extends SpeedMode {
   private static final double DIAGONAL = (double)1.0F / Math.sqrt((double)2.0F);

   public VanillaSpeedMode(SpeedModule settings) {
      super(settings);
   }

   public class_243 onMove(class_746 player, class_243 movement) {
      class_243 vel = horizontalVelocity(player, this.settings.vanillaSpeed());
      double velX = vel.field_1352;
      double velZ = vel.field_1350;
      class_1293 speedEffect = player.method_6112(class_1294.field_5904);
      if (speedEffect != null) {
         double value = (double)(speedEffect.method_5578() + 1) * 0.205;
         velX += velX * value;
         velZ += velZ * value;
      }

      return new class_243(velX, movement.field_1351, velZ);
   }

   private static class_243 horizontalVelocity(class_746 player, double bps) {
      float yaw = player.method_36454();
      class_243 forward = class_243.method_1030(0.0F, yaw);
      class_243 right = class_243.method_1030(0.0F, yaw + 90.0F);
      class_10185 keys = player.field_3913.field_54155;
      double velX = (double)0.0F;
      double velZ = (double)0.0F;
      boolean a = false;
      if (keys.comp_3159()) {
         velX += forward.field_1352 / (double)20.0F * bps;
         velZ += forward.field_1350 / (double)20.0F * bps;
         a = true;
      }

      if (keys.comp_3160()) {
         velX -= forward.field_1352 / (double)20.0F * bps;
         velZ -= forward.field_1350 / (double)20.0F * bps;
         a = true;
      }

      boolean b = false;
      if (keys.comp_3162()) {
         velX += right.field_1352 / (double)20.0F * bps;
         velZ += right.field_1350 / (double)20.0F * bps;
         b = true;
      }

      if (keys.comp_3161()) {
         velX -= right.field_1352 / (double)20.0F * bps;
         velZ -= right.field_1350 / (double)20.0F * bps;
         b = true;
      }

      if (a && b) {
         velX *= DIAGONAL;
         velZ *= DIAGONAL;
      }

      return new class_243(velX, (double)0.0F, velZ);
   }
}
