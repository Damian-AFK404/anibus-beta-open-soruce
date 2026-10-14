package net.anubis.client.gui.mainmenu;

import imgui.ImDrawList;
import java.util.Objects;
import java.util.function.Supplier;
import net.anubis.client.gui.ImGuiScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_156;
import net.minecraft.class_310;
import net.minecraft.class_4189;
import net.minecraft.class_426;
import net.minecraft.class_429;
import net.minecraft.class_4325;
import net.minecraft.class_437;
import net.minecraft.class_4749;
import net.minecraft.class_500;
import net.minecraft.class_526;

@Environment(EnvType.CLIENT)
public final class MenuContext extends MenuFrame {
   private static final float LOOK_EASE = 2.5F;
   private final class_437 screen;
   private final Runnable openAccounts;
   private final Runnable openCredits;
   private final float[] hover = new float[MenuAction.values().length];
   private float lookX;
   private float lookY;

   public MenuContext(class_437 screen, Runnable openAccounts, Runnable openCredits) {
      this.screen = screen;
      this.openAccounts = openAccounts;
      this.openCredits = openCredits;
   }

   public void begin(ImDrawList bg, ImDrawList fg, float width, float height, float scale, float time, float dt, float mouseX, float mouseY, boolean clicked) {
      super.begin(bg, fg, width, height, scale, time, dt, mouseX, mouseY, clicked);
      boolean onScreen = mouseX >= 0.0F && mouseY >= 0.0F && mouseX <= width && mouseY <= height;
      float targetX = onScreen ? mouseX / width * 2.0F - 1.0F : 0.0F;
      float targetY = onScreen ? mouseY / height * 2.0F - 1.0F : 0.0F;
      float k = Math.min(1.0F, dt * 2.5F);
      this.lookX += (targetX - this.lookX) * k;
      this.lookY += (targetY - this.lookY) * k;
   }

   public float lookX() {
      return this.lookX;
   }

   public float lookY() {
      return this.lookY;
   }

   public boolean enabled(MenuAction action) {
      return action != MenuAction.MULTIPLAYER && action != MenuAction.REALMS || class_310.method_1551().method_29043();
   }

   public float control(MenuAction action, float x, float y, float w, float h) {
      boolean over = this.inside(x, y, w, h);
      if (!this.enabled(action)) {
         if (over) {
            this.tooltip("Multiplayer is disabled", this.mouseX(), y + h + this.px(6.0F));
         }

         return this.hover(action, false);
      } else {
         if (over && this.clicked()) {
            this.activate(action);
         }

         return this.hover(action, over);
      }
   }

   public float hover(MenuAction action, boolean target) {
      float k = this.hover[action.ordinal()];
      k += ((target ? 1.0F : 0.0F) - k) * Math.min(1.0F, this.dt() * 12.0F);
      this.hover[action.ordinal()] = k;
      return k;
   }

   public void activate(MenuAction action) {
      if (this.enabled(action)) {
         class_310 mc = class_310.method_1551();
         Supplier var10000;
         switch (action) {
            case SINGLEPLAYER:
               var10000 = () -> new class_526(this.screen);
               break;
            case MULTIPLAYER:
               var10000 = () -> (class_437)(mc.field_1690.field_21840 ? new class_500(this.screen) : new class_4749(this.screen));
               break;
            case REALMS:
               var10000 = () -> new class_4325(this.screen);
               break;
            case OPTIONS:
               var10000 = () -> new class_429(this.screen, mc.field_1690);
               break;
            case LANGUAGE:
               var10000 = () -> new class_426(this.screen, mc.field_1690, mc.method_1526());
               break;
            case ACCESSIBILITY:
               var10000 = () -> new class_4189(this.screen, mc.field_1690);
               break;
            case ANUBIS:
               var10000 = ImGuiScreen::new;
               break;
            case QUIT:
            case MODS:
            case ACCOUNTS:
            case CREDITS:
               var10000 = null;
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
         }

         Supplier<class_437> next = var10000;
         if (action == MenuAction.QUIT) {
            Objects.requireNonNull(mc);
            mc.execute(mc::method_1592);
         } else if (action == MenuAction.ACCOUNTS) {
            this.openAccounts.run();
         } else if (action == MenuAction.CREDITS) {
            this.openCredits.run();
         } else if (action == MenuAction.MODS) {
            mc.execute(() -> class_156.method_668().method_60932(FabricLoader.getInstance().getGameDir().resolve("mods")));
         } else if (next != null) {
            mc.method_63588(() -> mc.method_1507((class_437)next.get()));
         }

      }
   }
}
