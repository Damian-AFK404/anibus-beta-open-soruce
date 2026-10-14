package net.anubis.client.module.impl.render;

import imgui.ImDrawList;
import imgui.ImGui;
import java.util.Objects;
import net.anubis.client.gui.AnubisTheme;
import net.anubis.client.gui.AnubisWidgets;
import net.anubis.client.gui.GuiPalette;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11631;
import net.minecraft.class_12392;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1934;
import net.minecraft.class_2338;
import net.minecraft.class_239;
import net.minecraft.class_310;
import net.minecraft.class_3908;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_4061;
import net.minecraft.class_408;
import net.minecraft.class_746;
import net.minecraft.class_9334;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public final class CustomCrosshairModule extends ClientModule implements HudOverlayProvider {
   public static volatile CustomCrosshairModule active;
   private static final String STYLE_CROSS = "Cross";
   private static final String STYLE_T = "T";
   private static final String STYLE_X = "X";
   private static final String STYLE_DOT = "Dot";
   private static final String STYLE_CIRCLE = "Circle";
   private static final String COLOR_CUSTOM = "Custom";
   private static final String COLOR_ACCENT = "Accent";
   private static final int OUTLINE = -1946157056;
   private static final int TRACK_ARGB = -870967267;
   private static final float BAR_HALF_WIDTH = 10.0F;
   private static final float BAR_HEIGHT = 3.0F;
   private static final float DIAGONAL = 0.70710677F;
   private static final double FULL_SPREAD_SPEED = 0.28;
   private static final float TINT_RATE = 14.0F;
   private static final float SPREAD_RATE = 12.0F;
   private static final float FADE_RATE = 18.0F;
   private final ModeSetting style = (ModeSetting)this.addSetting("style", new ModeSetting("Style", "Cross", new String[]{"Cross", "T", "X", "Dot", "Circle"}));
   private final NumberSetting size = (NumberSetting)this.addSetting("size", (new NumberSetting("Size", 6.0F, 1.0F, 20.0F)).step(1.0F));
   private final NumberSetting gap = (NumberSetting)this.addSetting("gap", (new NumberSetting("Gap", 2.0F, 0.0F, 16.0F)).step(1.0F));
   private final NumberSetting thickness = (NumberSetting)this.addSetting("thickness", (new NumberSetting("Thickness", 2.0F, 1.0F, 5.0F)).step(1.0F));
   private final BooleanSetting centerDot = (BooleanSetting)this.addSetting("center-dot", new BooleanSetting("Center Dot", false));
   private final BooleanSetting outline = (BooleanSetting)this.addSetting("outline", new BooleanSetting("Outline", true));
   private final ModeSetting colorMode = (ModeSetting)this.addSetting("color-mode", new ModeSetting("Color", "Custom", new String[]{"Custom", "Accent"}));
   private final ColorSetting color = (ColorSetting)this.addSetting("color", new ColorSetting("Custom Color", -657929));
   private final BooleanSetting targetTint = (BooleanSetting)this.addSetting("target-tint", new BooleanSetting("Target Tint", false));
   private final ColorSetting targetColor = (ColorSetting)this.addSetting("target-color", new ColorSetting("Target Color", -47814));
   private final BooleanSetting dynamicGap = (BooleanSetting)this.addSetting("dynamic-gap", new BooleanSetting("Dynamic Gap", false));
   private final NumberSetting spread = (NumberSetting)this.addSetting("spread", (new NumberSetting("Spread", 4.0F, 1.0F, 12.0F)).step(1.0F));
   private float tint;
   private float extraGap;
   private float barAlpha;
   private float barFill;

   public CustomCrosshairModule() {
      super("custom-crosshair", "Custom Crosshair", "Draws a configurable crosshair in place of the vanilla one.", ModuleCategory.VISUALS);
      this.gap.visibleWhen(this::hasArms);
      this.centerDot.visibleWhen(() -> !this.style.is("Dot"));
      this.color.visibleWhen(() -> this.colorMode.is("Custom"));
      ColorSetting var10000 = this.targetColor;
      BooleanSetting var10001 = this.targetTint;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.spread.visibleWhen(() -> this.dynamicGap.get() && !this.style.is("Dot"));
   }

   protected void onEnable() {
      this.tint = 0.0F;
      this.extraGap = 0.0F;
      this.barAlpha = 0.0F;
      this.barFill = 1.0F;
      active = this;
   }

   protected void onDisable() {
      active = null;
   }

   public boolean isHudOverlayEnabled() {
      return this.isEnabled();
   }

   public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && isVisible(mc)) {
         float dt = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
         float scale = ImGuiManager.getInstance().uiScale();
         this.updateMotion(mc, player, dt);
         Geometry geometry = this.geometry(screenW * 0.5F, screenH * 0.5F, scale);
         int fill = GuiPalette.abgr(this.currentColor());
         float fillAlpha = (float)(fill >>> 24 & 255) / 255.0F;
         float edge = this.outline.get() ? Math.max(1.0F, (float)Math.round(scale)) : 0.0F;
         if (edge > 0.0F) {
            this.drawShape(dl, geometry, AnubisWidgets.withAlpha(-1946157056, fillAlpha), edge);
         }

         this.drawShape(dl, geometry, fill, 0.0F);
         this.drawCooldown(dl, mc, player, geometry, fill, edge, scale, dt);
      }
   }

   private static boolean isVisible(class_310 mc) {
      if (mc.field_1687 != null && mc.field_1761 != null && !mc.field_1690.field_1842) {
         if (mc.field_1755 != null && !(mc.field_1755 instanceof class_408)) {
            return false;
         } else if (!mc.field_1690.method_31044().method_31034()) {
            return false;
         } else if (mc.field_61504.method_72772(class_11631.field_61583)) {
            return false;
         } else {
            return mc.field_1761.method_2920() != class_1934.field_9219 || spectatorCanInteract(mc);
         }
      } else {
         return false;
      }
   }

   private static boolean spectatorCanInteract(class_310 mc) {
      class_239 hit = mc.field_1765;
      if (hit instanceof class_3966 entityHit) {
         return entityHit.method_17782() instanceof class_3908;
      } else {
         if (hit instanceof class_3965 blockHit) {
            if (hit.method_17783() == class_240.field_1332) {
               class_2338 pos = blockHit.method_17777();
               return mc.field_1687.method_8320(pos).method_26196(mc.field_1687, pos) != null;
            }
         }

         return false;
      }
   }

   private void updateMotion(class_310 mc, class_746 player, float dt) {
      boolean var10000;
      label23: {
         if (this.targetTint.get()) {
            class_1297 var6 = mc.field_1692;
            if (var6 instanceof class_1309) {
               class_1309 living = (class_1309)var6;
               if (living.method_5805()) {
                  var10000 = true;
                  break label23;
               }
            }
         }

         var10000 = false;
      }

      boolean onTarget = var10000;
      this.tint = approach(this.tint, onTarget ? 1.0F : 0.0F, 14.0F, dt);
      float wanted = 0.0F;
      if (this.dynamicGap.get()) {
         double moved = Math.hypot(player.method_23317() - player.field_6014, player.method_23321() - player.field_5969);
         float moving = (float)Math.min((double)1.0F, moved / 0.28);
         float swing = 1.0F - player.method_7261(0.0F);
         wanted = this.spread.get() * Math.max(moving, swing);
      }

      this.extraGap = approach(this.extraGap, wanted, 12.0F, dt);
   }

   private static float approach(float current, float target, float rate, float dt) {
      float next = current + (target - current) * (1.0F - (float)Math.exp((double)(-rate * dt)));
      return Math.abs(target - next) < 0.001F ? target : next;
   }

   private int currentColor() {
      int base = this.colorMode.is("Accent") ? CosmeticAccent.argb() : this.color.argb();
      return this.tint <= 0.0F ? base : AnubisTheme.lerpColor(base, this.targetColor.argb(), this.tint);
   }

   private boolean hasArms() {
      return this.style.is("Cross") || this.style.is("T") || this.style.is("X");
   }

   private Geometry geometry(float cx, float cy, float scale) {
      float thick = Math.max(1.0F, (float)Math.round(this.thickness.get() * scale));
      float length = Math.max(1.0F, (float)Math.round(this.size.get() * scale));
      float baseGap = this.hasArms() ? this.gap.get() : 0.0F;
      float open = (float)Math.round((baseGap + this.extraGap) * scale);
      float maxExtra = this.dynamicGap.get() && !this.style.is("Dot") ? this.spread.get() : 0.0F;
      float below = this.reachBelow(length, (float)Math.round((baseGap + maxExtra) * scale), thick);
      return new Geometry(cx, cy, length, open, thick, below);
   }

   private float reachBelow(float length, float open, float thick) {
      float var10000;
      switch (this.style.get()) {
         case "Dot" -> var10000 = length * 0.5F;
         case "Circle" -> var10000 = length + open + thick * 0.5F;
         case "X" -> var10000 = (open + length) * 0.70710677F + thick * 0.5F;
         default -> var10000 = open + length + thick * 0.5F;
      }

      float below = var10000;
      return this.centerDot.get() ? Math.max(below, thick * 0.5F) : below;
   }

   private boolean armsJoined(Geometry g) {
      return (this.style.is("Cross") || this.style.is("T")) && g.open() < 1.0F;
   }

   private void drawShape(ImDrawList dl, Geometry g, int color, float grow) {
      switch (this.style.get()) {
         case "Dot" -> dl.addCircleFilled(g.cx(), g.cy(), Math.max(1.0F, g.size() * 0.5F) + grow, color, 0);
         case "Circle" -> dl.addCircle(g.cx(), g.cy(), g.size() + g.open(), color, 0, g.thick() + grow * 2.0F);
         case "X" -> drawDiagonals(dl, g, color, grow);
         default -> drawArms(dl, g, color, grow, !this.style.is("T"));
      }

      if (this.centerDot.get() && !this.style.is("Dot") && !this.armsJoined(g)) {
         float x0 = (float)Math.floor((double)(g.cx() - g.thick() * 0.5F));
         float y0 = (float)Math.floor((double)(g.cy() - g.thick() * 0.5F));
         rect(dl, x0, y0, x0 + g.thick(), y0 + g.thick(), color, grow);
      }

   }

   private static void drawArms(ImDrawList dl, Geometry g, int color, float grow, boolean top) {
      float x0 = (float)Math.floor((double)(g.cx() - g.thick() * 0.5F));
      float x1 = x0 + g.thick();
      float y0 = (float)Math.floor((double)(g.cy() - g.thick() * 0.5F));
      float y1 = y0 + g.thick();
      float near = g.open();
      float far = g.open() + g.size();
      if (near < 1.0F) {
         rect(dl, x0 - far, y0, x1 + far, y1, color, grow);
         dl.addRectFilled(x0 - grow, y1 + grow, x1 + grow, y1 + far + grow, color);
         if (top) {
            dl.addRectFilled(x0 - grow, y0 - far - grow, x1 + grow, y0 - grow, color);
         }

      } else {
         rect(dl, x0 - far, y0, x0 - near, y1, color, grow);
         rect(dl, x1 + near, y0, x1 + far, y1, color, grow);
         rect(dl, x0, y1 + near, x1, y1 + far, color, grow);
         if (top) {
            rect(dl, x0, y0 - far, x1, y0 - near, color, grow);
         }

      }
   }

   private static void drawDiagonals(ImDrawList dl, Geometry g, int color, float grow) {
      float near = Math.max(0.0F, g.open() - grow) * 0.70710677F;
      float far = (g.open() + g.size() + grow) * 0.70710677F;
      float width = g.thick() + grow * 2.0F;

      for(int sx = -1; sx <= 1; sx += 2) {
         for(int sy = -1; sy <= 1; sy += 2) {
            dl.addLine(g.cx() + (float)sx * near, g.cy() + (float)sy * near, g.cx() + (float)sx * far, g.cy() + (float)sy * far, color, width);
         }
      }

   }

   private static void rect(ImDrawList dl, float x0, float y0, float x1, float y1, int color, float grow) {
      dl.addRectFilled(x0 - grow, y0 - grow, x1 + grow, y1 + grow, color);
   }

   private void drawCooldown(ImDrawList dl, class_310 mc, class_746 player, Geometry g, int fill, float edge, float scale, float dt) {
      boolean wanted = mc.field_1690.method_42565().method_41753() == class_4061.field_18152;
      float charge = player.method_7261(0.0F);
      boolean ready = wanted && readyOnTarget(mc, player, charge);
      this.barFill = ready ? 1.0F : Math.min(1.0F, charge);
      this.barAlpha = approach(this.barAlpha, !wanted || !ready && !(charge < 1.0F) ? 0.0F : 1.0F, 18.0F, dt);
      if (!(this.barAlpha < 0.01F)) {
         float w = Math.max(12.0F, 2.0F * (float)Math.round(10.0F * scale));
         float h = Math.max(2.0F, (float)Math.round(3.0F * scale));
         float x0 = (float)Math.floor((double)(g.cx() - w * 0.5F));
         float y0 = (float)Math.round(g.cy() + g.below() + edge + 5.0F * scale);
         float alpha = this.barAlpha * (float)(fill >>> 24 & 255) / 255.0F;
         if (edge > 0.0F) {
            dl.addRectFilled(x0 - edge, y0 - edge, x0 + w + edge, y0 + h + edge, AnubisWidgets.withAlpha(-1946157056, alpha), h * 0.5F + edge);
         }

         dl.addRectFilled(x0, y0, x0 + w, y0 + h, AnubisWidgets.withAlpha(GuiPalette.abgr(-870967267), alpha), h * 0.5F);
         float filled = (float)Math.round(w * this.barFill);
         if (filled >= 1.0F) {
            dl.addRectFilled(x0, y0, x0 + filled, y0 + h, AnubisWidgets.withAlpha(fill | -16777216, alpha), h * 0.5F);
         }

      }
   }

   private static boolean readyOnTarget(class_310 mc, class_746 player, float charge) {
      if (!(charge < 1.0F)) {
         class_1297 var4 = mc.field_1692;
         if (var4 instanceof class_1309) {
            class_1309 target = (class_1309)var4;
            if (target.method_5805()) {
               if (player.method_7279() <= 5.0F) {
                  return false;
               }

               class_12392 range = (class_12392)player.method_76694().method_58694(class_9334.field_64680);
               return range == null || mc.field_1765 == null || range.method_76736(player, mc.field_1765.method_17784());
            }
         }
      }

      return false;
   }

   @Environment(EnvType.CLIENT)
   private static record Geometry(float cx, float cy, float size, float open, float thick, float below) {
   }
}
