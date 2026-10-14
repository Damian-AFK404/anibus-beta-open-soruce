package net.anubis.client.module.impl.render;

import imgui.AnubisAndroidNative;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AtmosphereQuality {
   private static final boolean MOBILE = AnubisAndroidNative.isAndroidRuntime();
   private static final int MOBILE_SKY_SIZE = 640;

   private AtmosphereQuality() {
   }

   static boolean mobile() {
      return MOBILE;
   }

   static int skyWidth(int width, int height) {
      return scaledSize(width, Math.max(width, height));
   }

   static int skyHeight(int width, int height) {
      return scaledSize(height, Math.max(width, height));
   }

   private static int scaledSize(int size, int longest) {
      double scale = Math.min((double)0.5F, (double)640.0F / (double)Math.max(1, longest));
      return Math.max(1, (int)Math.round((double)size * scale));
   }
}
