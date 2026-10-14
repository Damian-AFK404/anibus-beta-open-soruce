package net.anubis.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_238;

@Environment(EnvType.CLIENT)
public final class BlockBoxHelper {
   private static final double HALF_BLOCK = (double)0.5F;
   private static final float MIN_SIZE = 0.1F;
   private static final float MAX_SIZE = 2.0F;

   private BlockBoxHelper() {
   }

   public static class_238 scaledBlockBox(class_2338 pos, float size) {
      float clamped = Math.max(0.1F, Math.min(2.0F, size));
      double halfSize = (double)clamped * (double)0.5F;
      double centerX = (double)pos.method_10263() + (double)0.5F;
      double centerY = (double)pos.method_10264() + (double)0.5F;
      double centerZ = (double)pos.method_10260() + (double)0.5F;
      return new class_238(centerX - halfSize, centerY - halfSize, centerZ - halfSize, centerX + halfSize, centerY + halfSize, centerZ + halfSize);
   }
}
