package net.anubis.client.module.impl.misc;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.input.AnubisKeyPolling;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.CombatMacroLock;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.combat.melee.MeleeAttacks;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_239;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_3965;
import net.minecraft.class_239.class_240;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class AutoClickerModule extends ClientModule {
   private static final String CLICK = "Click";
   private static final String HOLD = "Hold";
   private static final String OFF = "Off";
   private static final double TICK_MS = (double)50.0F;
   private static final double PACE_MIN_MS = (double)500.0F;
   private static final double PACE_MAX_MS = (double)1500.0F;
   private static final double PACE_EASE = 0.3;
   private static final double HOLD_MEAN_MS = (double)55.0F;
   private static final double HOLD_SPREAD_MS = (double)18.0F;
   private static final double HOLD_MIN_MS = (double)20.0F;
   private static final double HOLD_MAX_MS = (double)110.0F;
   private static final double HOLD_MAX_GAP = 0.7;
   private static final double GAP_MIN_FACTOR = 0.6;
   private static final double GAP_MAX_FACTOR = 2.2;
   private final ModeSetting left = (ModeSetting)this.addSetting("left", new ModeSetting("Left", "Click", new String[]{"Click", "Hold", "Off"}));
   private final NumberSetting leftMin = (NumberSetting)this.addSetting("left-min-cps", (new NumberSetting("Left Min CPS", 9.0F, 1.0F, 20.0F)).step(1.0F));
   private final NumberSetting leftMax = (NumberSetting)this.addSetting("left-max-cps", (new NumberSetting("Left Max CPS", 13.0F, 1.0F, 20.0F)).step(1.0F));
   private final ModeSetting right = (ModeSetting)this.addSetting("right", new ModeSetting("Right", "Off", new String[]{"Click", "Hold", "Off"}));
   private final NumberSetting rightMin = (NumberSetting)this.addSetting("right-min-cps", (new NumberSetting("Right Min CPS", 8.0F, 1.0F, 20.0F)).step(1.0F));
   private final NumberSetting rightMax = (NumberSetting)this.addSetting("right-max-cps", (new NumberSetting("Right Max CPS", 12.0F, 1.0F, 20.0F)).step(1.0F));
   private final NumberSetting randomization = (NumberSetting)this.addSetting("randomization", (new NumberSetting("Randomization", 15.0F, 0.0F, 40.0F)).step(1.0F));
   private final BooleanSetting onlyWhileHeld = (BooleanSetting)this.addSetting("only-while-held", new BooleanSetting("Only While Held", false));
   private final BooleanSetting breakBlocks = (BooleanSetting)this.addSetting("break-blocks", new BooleanSetting("Break Blocks", true));
   private final Button leftButton;
   private final Button rightButton;

   public AutoClickerModule() {
      super("auto-clicker", "Auto Clicker", "Clicks the left and/or right mouse button at a randomised, human CPS.", ModuleCategory.MISC);
      this.leftButton = new Button(true, this.left, this.leftMin, this.leftMax, (mc) -> mc.field_1690.field_1886);
      this.rightButton = new Button(false, this.right, this.rightMin, this.rightMax, (mc) -> mc.field_1690.field_1904);
      this.leftMin.visibleWhen(() -> this.left.is("Click"));
      this.leftMax.visibleWhen(() -> this.left.is("Click"));
      this.rightMin.visibleWhen(() -> this.right.is("Click"));
      this.rightMax.visibleWhen(() -> this.right.is("Click"));
      this.randomization.visibleWhen(this::anyClicking);
      this.onlyWhileHeld.visibleWhen(this::anyClicking);
      this.breakBlocks.visibleWhen(() -> this.left.is("Click"));
   }

   public int settingsVersion() {
      return 1;
   }

   public void migrateSettings(int savedVersion) {
      if (savedVersion < 1) {
         this.onlyWhileHeld.set(false);
      }

   }

   protected void onDisable() {
      class_310 mc = class_310.method_1551();
      this.leftButton.handBack(mc);
      this.rightButton.handBack(mc);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      boolean allowed = MeleeAttacks.canAct(mc) && mc.method_18506() == null && !CombatMacroLock.heldByOther(this) && !MeleeAttacks.actedThisTick(mc.field_1724);
      this.leftButton.tick(mc, allowed);
      this.rightButton.tick(mc, allowed);
   }

   private boolean anyClicking() {
      return this.left.is("Click") || this.right.is("Click");
   }

   private static boolean aimsAtBlock(class_310 mc) {
      class_239 var2 = mc.field_1765;
      boolean var10000;
      if (var2 instanceof class_3965 hit) {
         if (hit.method_17783() == class_240.field_1332 && mc.field_1687 != null && !mc.field_1687.method_8320(hit.method_17777()).method_26215()) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   private static boolean physicallyDown(class_310 mc, class_304 mapping) {
      if (mapping.method_1415()) {
         return false;
      } else {
         class_3675.class_306 key = KeyBindingHelper.getBoundKeyOf(mapping);
         int value = key.method_1444();
         if (value < 0) {
            return false;
         } else {
            boolean var10000;
            switch (key.method_1442()) {
               case field_1672 -> var10000 = GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), value) == 1;
               case field_1668 -> var10000 = AnubisKeyPolling.isKeyDown(mc.method_22683(), value);
               default -> var10000 = mapping.method_1434();
            }

            return var10000;
         }
      }
   }

   private static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   @Environment(EnvType.CLIENT)
   private final class Button {
      private final boolean isLeft;
      private final ModeSetting mode;
      private final NumberSetting minCps;
      private final NumberSetting maxCps;
      private final Function<class_310, class_304> mapping;
      private boolean driving;
      private boolean running;
      private double clock;
      private double nextPress;
      private double releaseAt;
      private double rate;
      private double pace;
      private double repaceAt;

      Button(boolean isLeft, ModeSetting mode, NumberSetting minCps, NumberSetting maxCps, Function<class_310, class_304> mapping) {
         this.isLeft = isLeft;
         this.mode = mode;
         this.minCps = minCps;
         this.maxCps = maxCps;
         this.mapping = mapping;
      }

      void tick(class_310 mc, boolean allowed) {
         if (allowed && !this.mode.is("Off")) {
            class_304 key = (class_304)this.mapping.apply(mc);
            if (this.mode.is("Hold")) {
               this.running = false;
               this.driving = true;
               key.method_23481(true);
            } else if (!key.method_1415() && (!AutoClickerModule.this.onlyWhileHeld.get() || AutoClickerModule.physicallyDown(mc, key)) && (!this.isLeft || !AutoClickerModule.this.breakBlocks.get() || !AutoClickerModule.aimsAtBlock(mc)) && (this.isLeft || !mc.field_1724.method_6115())) {
               if (!this.running) {
                  this.start();
               }

               this.clock += (double)50.0F;
               if (this.isLeft) {
                  mc.field_1771 = 0;
               }

               double gap;
               for(class_3675.class_306 bound = KeyBindingHelper.getBoundKeyOf(key); this.nextPress <= this.clock; this.nextPress += gap) {
                  class_304.method_1420(bound);
                  gap = this.nextGap();
                  this.releaseAt = this.nextPress + this.holdFor(gap);
               }

               this.driving = true;
               key.method_23481(this.clock < this.releaseAt);
            } else {
               this.handBack(mc);
            }
         } else {
            this.handBack(mc);
         }
      }

      private void start() {
         double lo = (double)Math.min(this.minCps.get(), this.maxCps.get());
         double hi = (double)Math.max(this.minCps.get(), this.maxCps.get());
         ThreadLocalRandom random = ThreadLocalRandom.current();
         this.running = true;
         this.clock = (double)0.0F;
         this.releaseAt = (double)0.0F;
         this.pace = lo + random.nextDouble() * (hi - lo);
         this.rate = this.pace;
         this.repaceAt = (double)500.0F + random.nextDouble() * (double)1000.0F;
         this.nextPress = this.nextGap();
      }

      private double nextGap() {
         double lo = (double)Math.min(this.minCps.get(), this.maxCps.get());
         double hi = (double)Math.max(this.minCps.get(), this.maxCps.get());
         ThreadLocalRandom random = ThreadLocalRandom.current();
         if (this.clock >= this.repaceAt) {
            this.pace = lo + random.nextDouble() * (hi - lo);
            this.repaceAt = this.clock + (double)500.0F + random.nextDouble() * (double)1000.0F;
         }

         this.rate = AutoClickerModule.clamp(this.rate + (this.pace - this.rate) * 0.3, lo, hi);
         double gap = (double)1000.0F / this.rate;
         double spread = (double)AutoClickerModule.this.randomization.get() / (double)100.0F;
         if (spread <= (double)0.0F) {
            return gap;
         } else {
            double factor = (double)1.0F + random.nextGaussian() * spread;
            if (random.nextDouble() < spread * (double)0.25F) {
               factor += 0.4 + random.nextDouble() * 0.6;
            }

            return gap * AutoClickerModule.clamp(factor, 0.6, 2.2);
         }
      }

      private double holdFor(double gap) {
         if (AutoClickerModule.this.randomization.get() <= 0.0F) {
            return gap * (double)0.5F;
         } else {
            double hold = (double)55.0F + ThreadLocalRandom.current().nextGaussian() * (double)18.0F;
            return AutoClickerModule.clamp(hold, (double)20.0F, Math.min((double)110.0F, gap * 0.7));
         }
      }

      void handBack(class_310 mc) {
         this.running = false;
         if (this.driving) {
            this.driving = false;
            class_304 key = (class_304)this.mapping.apply(mc);
            key.method_23481(mc.field_1755 == null && AutoClickerModule.physicallyDown(mc, key));
         }
      }
   }
}
