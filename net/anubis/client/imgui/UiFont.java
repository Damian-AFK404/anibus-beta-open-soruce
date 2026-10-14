package net.anubis.client.imgui;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum UiFont {
   GEIST("Geist", "geist-medium.ttf", "geist-semibold.ttf", 1.0F, 0, 0, false, 0.546F, 0.0F),
   MANROPE("Manrope", "manrope-medium.ttf", "manrope-bold.ttf", 1.05F, 0, 0, false, 0.527F, 0.0F),
   RAJDHANI("Rajdhani", "rajdhani-semibold.ttf", "rajdhani-bold.ttf", 1.12F, 0, 0, false, 0.504F, 0.0F),
   SILKSCREEN("Silkscreen", "silkscreen-regular.ttf", "silkscreen-bold.ttf", 0.874F, 8, 2, true, 0.7F, 0.18F),
   JERSEY("Jersey 10", "jersey10-regular.ttf", "jersey10-regular.ttf", 1.09F, 20, 1, false, 0.5F, 0.0F);

   private static final String DIR = "/assets/anubis/fonts/";
   public final String label;
   private final String regular;
   private final String strong;
   private final float scale;
   private final int step;
   private final int minSteps;
   private final boolean emSized;
   private final float capRatio;
   private final float lift;

   private UiFont(String label, String regular, String strong, float scale, int step, int minSteps, boolean emSized, float capRatio, float lift) {
      this.label = label;
      this.regular = regular;
      this.strong = strong;
      this.scale = scale;
      this.step = step;
      this.minSteps = minSteps;
      this.emSized = emSized;
      this.capRatio = capRatio;
      this.lift = lift;
   }

   public static String[] labels() {
      return (String[])Arrays.stream(values()).map((font) -> font.label).toArray((x$0) -> new String[x$0]);
   }

   public static UiFont of(String label) {
      for(UiFont font : values()) {
         if (font.label.equals(label)) {
            return font;
         }
      }

      return GEIST;
   }

   String resource(boolean bold) {
      String var10000 = bold ? this.strong : this.regular;
      return "/assets/anubis/fonts/" + var10000;
   }

   boolean pixel() {
      return this.step > 0;
   }

   float size(float geistSize) {
      float size = geistSize * this.scale;
      return !this.pixel() ? (float)Math.round(size) : (float)(this.step * Math.max(this.minSteps, Math.round(size / (float)this.step)));
   }

   int flags() {
      if (!this.pixel()) {
         return 8;
      } else {
         int flags = 129;
         return this.emSized ? flags | 512 : flags;
      }
   }

   float offsetY(float size) {
      return (float)(-Math.round(this.lift * size));
   }

   float fallbackSize(float size) {
      return (float)Math.round(size * this.capRatio / GEIST.capRatio);
   }

   // $FF: synthetic method
   private static UiFont[] $values() {
      return new UiFont[]{GEIST, MANROPE, RAJDHANI, SILKSCREEN, JERSEY};
   }
}
