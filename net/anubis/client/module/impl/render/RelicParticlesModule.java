package net.anubis.client.module.impl.render;

import java.util.Objects;
import java.util.function.Consumer;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PacketReceiveEvent;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.MobilePerformance;
import net.anubis.client.util.combat.TotemPops;
import net.anubis.client.util.glyph.GlyphCanvas;
import net.anubis.client.util.glyph.GlyphSwarm;
import net.anubis.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1665;
import net.minecraft.class_1684;
import net.minecraft.class_1685;
import net.minecraft.class_238;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_5819;
import net.minecraft.class_638;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class RelicParticlesModule extends ClientModule {
   private static final String GLYPH_ANKH = "Ankh";
   private static final String GLYPH_SCARAB = "Scarab";
   private static final String GLYPH_SAND = "Sand";
   private static final String GLYPH_SPARK = "Spark";
   private static final String GLYPH_MIXED = "Mixed";
   private static final int CAPACITY;
   private static final float BASE_SIZE = 0.085F;
   private static final double PROJECTILE_RANGE_SQR = (double)6400.0F;
   private static final double RESTING_SQR = 9.0E-4;
   private static final double STEP_MIN_SPEED = 0.07;
   private static final int PEARL = 0;
   private static final int ARROW = 1;
   private static final int TRIDENT = 2;
   private static final int NONE = -1;
   private static final byte[] MIXED_ORDER;
   private final BooleanSetting pops = (BooleanSetting)this.addSetting("pops", new BooleanSetting("Totem Pops", true));
   private final ModeSetting popGlyph = (ModeSetting)this.addSetting("pop-glyph", new ModeSetting("Pop Glyph", "Mixed", new String[]{"Mixed", "Ankh", "Scarab", "Sand", "Spark"}));
   private final NumberSetting popBurst = (NumberSetting)this.addSetting("pop-burst", (new NumberSetting("Pop Burst", 26.0F, 4.0F, 80.0F)).step(1.0F));
   private final BooleanSetting ownPops = (BooleanSetting)this.addSetting("own-pops", new BooleanSetting("Own Pops", true));
   private final BooleanSetting projectiles = (BooleanSetting)this.addSetting("projectiles", new BooleanSetting("Projectiles", true));
   private final BooleanSetting pearls = (BooleanSetting)this.addSetting("pearls", new BooleanSetting("Pearls", true));
   private final BooleanSetting arrows = (BooleanSetting)this.addSetting("arrows", new BooleanSetting("Arrows", true));
   private final BooleanSetting tridents = (BooleanSetting)this.addSetting("tridents", new BooleanSetting("Tridents", true));
   private final ModeSetting trailGlyph = (ModeSetting)this.addSetting("trail-glyph", new ModeSetting("Trail Glyph", "Spark", new String[]{"Spark", "Sand", "Ankh", "Scarab", "Mixed"}));
   private final NumberSetting trailDensity = (NumberSetting)this.addSetting("trail-density", (new NumberSetting("Trail Density", 2.0F, 1.0F, 6.0F)).step(1.0F));
   private final BooleanSetting footsteps = (BooleanSetting)this.addSetting("footsteps", new BooleanSetting("Footsteps", false));
   private final ModeSetting stepGlyph = (ModeSetting)this.addSetting("step-glyph", new ModeSetting("Step Glyph", "Sand", new String[]{"Sand", "Spark", "Ankh", "Scarab", "Mixed"}));
   private final NumberSetting stepDensity = (NumberSetting)this.addSetting("step-density", (new NumberSetting("Step Density", 2.0F, 1.0F, 6.0F)).step(1.0F));
   private final NumberSetting size = (NumberSetting)this.addSetting("size", (new NumberSetting("Size", 1.0F, 0.3F, 2.5F)).step(0.05F));
   private final NumberSetting lifetime = (NumberSetting)this.addSetting("lifetime", (new NumberSetting("Lifetime", 1.4F, 0.3F, 4.0F)).step(0.1F));
   private final NumberSetting limit;
   private final BooleanSetting halo;
   private final ModeSetting colorMode;
   private final ColorSetting mainColor;
   private final ColorSetting secondColor;
   private final GlyphSwarm swarm;
   private final GlyphCanvas canvas;
   private final GlyphTint palette;
   private final class_5819 random;
   private final Consumer<class_1309> popSink;
   private class_638 boundLevel;
   private long lastFrame;

   public RelicParticlesModule() {
      super("particles", "Particles", "Egyptian particles for totem pops, projectiles and footsteps.", ModuleCategory.VISUALS);
      this.limit = (NumberSetting)this.addSetting("limit", (new NumberSetting("Limit", 240.0F, 40.0F, (float)CAPACITY)).step(10.0F));
      this.halo = (BooleanSetting)this.addSetting("halo", new BooleanSetting("Halo", true));
      this.colorMode = (ModeSetting)this.addSetting("color-mode", new ModeSetting("Color", "Theme", new String[]{"Theme", "Manual"}));
      this.mainColor = (ColorSetting)this.addSetting("main-color", new ColorSetting("Main Color", -12548362));
      this.secondColor = (ColorSetting)this.addSetting("second-color", new ColorSetting("Second Color", -14992226));
      this.swarm = new GlyphSwarm(CAPACITY);
      this.canvas = new GlyphCanvas();
      this.palette = new GlyphTint();
      this.random = class_5819.method_43047();
      this.popSink = this::burst;
      ModeSetting var10000 = this.popGlyph;
      BooleanSetting var10001 = this.pops;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      NumberSetting var1 = this.popBurst;
      var10001 = this.pops;
      Objects.requireNonNull(var10001);
      var1.visibleWhen(var10001::get);
      BooleanSetting var2 = this.ownPops;
      var10001 = this.pops;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      var2 = this.pearls;
      var10001 = this.projectiles;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      var2 = this.arrows;
      var10001 = this.projectiles;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      var2 = this.tridents;
      var10001 = this.projectiles;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      ModeSetting var6 = this.trailGlyph;
      var10001 = this.projectiles;
      Objects.requireNonNull(var10001);
      var6.visibleWhen(var10001::get);
      NumberSetting var7 = this.trailDensity;
      var10001 = this.projectiles;
      Objects.requireNonNull(var10001);
      var7.visibleWhen(var10001::get);
      ModeSetting var8 = this.stepGlyph;
      var10001 = this.footsteps;
      Objects.requireNonNull(var10001);
      var8.visibleWhen(var10001::get);
      NumberSetting var9 = this.stepDensity;
      var10001 = this.footsteps;
      Objects.requireNonNull(var10001);
      var9.visibleWhen(var10001::get);
      this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));
   }

   protected void onEnable() {
      this.reset();
      this.on(PacketReceiveEvent.TYPE, this::onPacket);
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender);
   }

   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.swarm.clear();
      this.boundLevel = null;
      this.lastFrame = 0L;
   }

   private boolean bind(class_638 level) {
      if (level == null) {
         if (this.boundLevel != null) {
            this.reset();
         }

         return false;
      } else {
         if (level != this.boundLevel) {
            this.swarm.clear();
            this.boundLevel = level;
         }

         return true;
      }
   }

   private void onPacket(PacketReceiveEvent event) {
      if (this.pops.get()) {
         TotemPops.scan(event.packet, this.popSink);
      }

   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (this.bind(mc.field_1687) && player != null) {
         this.refreshPalette();
         if (this.projectiles.get()) {
            this.trails(mc.field_1687, player);
         }

         if (this.footsteps.get()) {
            this.steps(player);
         }

      }
   }

   private void onRender(Render3DEvent event) {
      class_310 mc = class_310.method_1551();
      if (this.bind(mc.field_1687)) {
         long now = System.nanoTime();
         float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
         this.lastFrame = now;
         if (mc.method_1493()) {
            dt = 0.0F;
         }

         this.swarm.trim(this.particleLimit());
         this.swarm.step(dt);
         if (this.swarm.count() != 0) {
            this.canvas.begin(mc.field_1773.method_19418());
            this.swarm.draw(this.canvas, 1.0F, this.halo.get(), MobilePerformance.active() ? event.frustum : null);
            this.canvas.submit(false);
         }
      }
   }

   private void burst(class_1309 entity) {
      class_310 mc = class_310.method_1551();
      if (this.bind(mc.field_1687) && entity.method_73183() == mc.field_1687) {
         if (entity != mc.field_1724 || this.ownPops.get()) {
            this.refreshPalette();
            class_238 box = entity.method_5829();
            int total = MobilePerformance.particles(this.popBurst.getInt(), 6);
            String mode = this.popGlyph.get();

            for(int i = 0; i < total; ++i) {
               byte kind = pick(mode, i);
               double px = box.field_1323 + box.method_17939() * this.random.method_43058();
               double py = box.field_1322 + box.method_17940() * (0.15 + 0.8 * this.random.method_43058());
               double pz = box.field_1321 + box.method_17941() * this.random.method_43058();
               float heading = this.random.method_43057() * ((float)Math.PI * 2F);
               float out = outward(kind) * (0.45F + this.random.method_43057());
               float up = upward(kind) * (0.55F + 0.8F * this.random.method_43057());
               this.emit(kind, px, py, pz, class_3532.method_15362((double)heading) * out, up, class_3532.method_15374((double)heading) * out, 1.0F, 1.0F, this.colorFor(kind, -1));
            }

         }
      }
   }

   private void trails(class_638 level, class_746 player) {
      int density = MobilePerformance.particles(this.trailDensity.getInt(), 1);
      String mode = this.trailGlyph.get();

      for(class_1297 entity : level.method_18112()) {
         int role = this.role(entity);
         if (role != -1) {
            double dx = entity.method_23317() - entity.field_6014;
            double dy = entity.method_23318() - entity.field_6036;
            double dz = entity.method_23321() - entity.field_5969;
            if (!(dx * dx + dy * dy + dz * dz < 9.0E-4) && !(entity.method_5858(player) > (double)6400.0F)) {
               double mid = (double)entity.method_17682() * (double)0.5F;

               for(int i = 0; i < density; ++i) {
                  float f = ((float)i + this.random.method_43057()) / (float)density;
                  byte kind = pick(mode, entity.field_6012 * density + i);
                  this.emit(kind, entity.field_6014 + dx * (double)f + (double)this.jitter(0.06F), entity.field_6036 + dy * (double)f + mid + (double)this.jitter(0.06F), entity.field_5969 + dz * (double)f + (double)this.jitter(0.06F), this.jitter(0.3F), this.jitter(0.3F) + 0.15F, this.jitter(0.3F), 0.55F, 0.8F, this.colorFor(kind, role));
               }
            }
         }
      }

   }

   private void steps(class_746 player) {
      if (player.method_24828() && !player.method_7325() && !player.method_5765() && !player.method_52535()) {
         double dx = player.method_23317() - player.field_6014;
         double dz = player.method_23321() - player.field_5969;
         double speed = Math.sqrt(dx * dx + dz * dz);
         if (!(speed < 0.07)) {
            int count = MobilePerformance.particles(this.stepDensity.getInt() + (player.method_5624() ? 1 : 0), 1);
            String mode = this.stepGlyph.get();

            for(int i = 0; i < count; ++i) {
               byte kind = pick(mode, player.field_6012 * count + i);
               double back = 0.2 + 0.3 * this.random.method_43058();
               this.emit(kind, player.method_23317() - dx / speed * back + (double)this.jitter(0.22F), player.method_23318() + 0.04, player.method_23321() - dz / speed * back + (double)this.jitter(0.22F), (float)(-dx * (double)7.0F) + this.jitter(0.5F), 0.8F + this.random.method_43057() * 1.1F, (float)(-dz * (double)7.0F) + this.jitter(0.5F), 0.7F, 0.75F, this.colorFor(kind, -1));
            }

         }
      }
   }

   private void emit(byte kind, double x, double y, double z, float vx, float vy, float vz, float lifeScale, float sizeScale, int rgb) {
      float life = this.lifetime.get() * lifeScale * (0.75F + 0.5F * this.random.method_43057());
      float scale = 0.085F * this.size.get() * sizeScale * sizeOf(kind) * (0.8F + 0.4F * this.random.method_43057());
      float turn = spinOf(kind) * (this.random.method_43056() ? 1.0F : -1.0F) * (0.5F + this.random.method_43057());
      float angle = kind == 0 ? this.jitter(0.35F) : this.random.method_43057() * ((float)Math.PI * 2F);
      this.swarm.spawn(kind, x, y, z, vx, vy, vz, life, scale, angle, turn, this.random.method_43057(), rgb, this.particleLimit());
   }

   private int particleLimit() {
      return MobilePerformance.particles(this.limit.getInt(), 40);
   }

   private void refreshPalette() {
      this.palette.resolve(this.colorMode.get(), CosmeticAccent.argb(), this.mainColor.argb(), this.secondColor.argb());
   }

   private int role(class_1297 entity) {
      if (entity.method_31481()) {
         return -1;
      } else if (entity instanceof class_1684) {
         return this.pearls.get() ? 0 : -1;
      } else if (entity instanceof class_1685) {
         return this.tridents.get() ? 2 : -1;
      } else if (entity instanceof class_1665) {
         return this.arrows.get() ? 1 : -1;
      } else {
         return -1;
      }
   }

   private static byte pick(String mode, int index) {
      byte var10000;
      switch (mode) {
         case "Ankh" -> var10000 = 0;
         case "Scarab" -> var10000 = 1;
         case "Sand" -> var10000 = 2;
         case "Spark" -> var10000 = 3;
         default -> var10000 = MIXED_ORDER[Math.floorMod(index, MIXED_ORDER.length)];
      }

      return var10000;
   }

   private int colorFor(byte kind, int role) {
      float r = this.random.method_43057();
      int var10000;
      switch (kind) {
         case 0 -> var10000 = r < 0.6F ? this.palette.primary() : this.palette.highlight();
         case 1 -> var10000 = r < 0.55F ? this.palette.secondary() : this.palette.companion();
         case 2 -> var10000 = this.dust(r);
         default -> var10000 = GlyphTint.mix(this.palette.highlight(), -1, r * 0.35F);
      }

      int base = var10000;
      switch (role) {
         case 0 -> var10000 = GlyphTint.mix(base, this.palette.companion(), 0.55F);
         case 2 -> var10000 = GlyphTint.mix(base, this.palette.secondary(), 0.45F);
         default -> var10000 = base;
      }

      return var10000;
   }

   private int dust(float r) {
      int dust = this.palette.dust();
      if (r < 0.4F) {
         return dust;
      } else {
         return r < 0.75F ? GlyphTint.lift(dust, 0.4F) : GlyphTint.mix(dust, this.palette.secondary(), 0.3F);
      }
   }

   private static float sizeOf(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 1.35F;
         case 1 -> var10000 = 1.15F;
         case 2 -> var10000 = 0.5F;
         default -> var10000 = 0.9F;
      }

      return var10000;
   }

   private static float spinOf(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 0.5F;
         case 1 -> var10000 = 1.1F;
         case 2 -> var10000 = 5.0F;
         default -> var10000 = 2.8F;
      }

      return var10000;
   }

   private static float outward(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 0.8F;
         case 1 -> var10000 = 2.1F;
         case 2 -> var10000 = 1.5F;
         default -> var10000 = 3.4F;
      }

      return var10000;
   }

   private static float upward(byte kind) {
      float var10000;
      switch (kind) {
         case 0 -> var10000 = 1.1F;
         case 1 -> var10000 = 1.7F;
         case 2 -> var10000 = 2.6F;
         default -> var10000 = 2.4F;
      }

      return var10000;
   }

   private float jitter(float amount) {
      return (this.random.method_43057() * 2.0F - 1.0F) * amount;
   }

   static {
      CAPACITY = 12000 / GlyphSwarm.WORST_QUADS / 10 * 10;
      MIXED_ORDER = new byte[]{0, 3, 1, 3, 2, 3};
   }
}
