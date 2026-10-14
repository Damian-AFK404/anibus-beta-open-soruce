package net.anubis.client.module.impl.render;

import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.client.PerformanceModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.MobilePerformance;
import net.anubis.client.util.glyph.GlyphCanvas;
import net.anubis.client.util.glyph.GlyphShapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4604;
import net.minecraft.class_5819;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_9848;
import net.minecraft.class_2350.class_2351;

@Environment(EnvType.CLIENT)
public final class FloatingRelicsModule extends ClientModule {
   private static final String SYMBOL_ANUBIS = "Anubis";
   private static final String SYMBOL_LARP = "Larp";
   private static final float[][] ANUBIS_GLYPHS;
   private static final float[] ANUBIS_FIT;
   private static final float[][] LARP_GLYPHS;
   private static final float[] LARP_FIT;
   private static final String COLOR_THEME = "Theme";
   private static final String COLOR_MANUAL = "Manual";
   private static final int MAX_RELICS = 32;
   private static final float GRAVITY = 0.016F;
   private static final float BUOYANCY = 0.045F;
   private static final float AIR_DRAG = 0.982F;
   private static final float GROUND_GRIP = 0.86F;
   private static final float ORBIT_PULL = 0.012F;
   private static final float SWIRL = 0.0025F;
   private static final float FOLLOW = 0.035F;
   private static final float WANDER = 0.003F;
   private static final float MAX_SPEED = 0.9F;
   private static final float SPIN_SETTLE = 0.03F;
   private static final float ANGLE_WRAP = 512.0F;
   private static final float TILT_SPRING = 0.09F;
   private static final float TILT_DAMP = 0.9F;
   private static final float MAX_TILT = 0.55F;
   private static final int APPEAR_TICKS = 12;
   private static final int STAGGER_TICKS = 3;
   private static final float GLYPH_SCALE = 1.25F;
   private static final float THICKNESS = 0.07F;
   private static final float SWAY = 0.12F;
   private static final float BOB = 0.2F;
   private static final float FACE_ALPHA = 0.94F;
   private static final float BACK_ALPHA = 0.9F;
   private static final float GLINT = 0.45F;
   private static final float[] HALO_SCALES;
   private static final float[] HALO_WEIGHTS;
   private static final float HALO_PUSH = 0.6F;
   private static final double DETAIL_RANGE_SQR = (double)576.0F;
   private static final float LIGHT_X;
   private static final float LIGHT_Y;
   private static final float LIGHT_Z;
   private static final long EPOCH;
   private final ModeSetting symbols = (ModeSetting)this.addSetting("symbols", new ModeSetting("Symbols", "Anubis", new String[]{"Anubis", "Larp"}));
   private final NumberSetting count = (NumberSetting)this.addSetting("count", (new NumberSetting("Count", 9.0F, 1.0F, 32.0F)).step(1.0F));
   private final NumberSetting size = (NumberSetting)this.addSetting("size", (new NumberSetting("Size", 0.3F, 0.15F, 0.75F)).step(0.05F));
   private final NumberSetting spread = (NumberSetting)this.addSetting("spread", (new NumberSetting("Spread", 2.6F, 1.2F, 6.0F)).step(0.1F));
   private final NumberSetting bounce = (NumberSetting)this.addSetting("bounce", (new NumberSetting("Bounce", 0.6F, 0.1F, 0.95F)).step(0.05F));
   private final NumberSetting drift = (NumberSetting)this.addSetting("drift", (new NumberSetting("Drift", 0.5F, 0.0F, 2.5F)).step(0.1F));
   private final NumberSetting spinSpeed = (NumberSetting)this.addSetting("spin", (new NumberSetting("Spin", 1.0F, 0.2F, 3.0F)).step(0.1F));
   private final ModeSetting colorMode = (ModeSetting)this.addSetting("color", new ModeSetting("Color", "Theme", new String[]{"Theme", "Manual"}));
   private final ColorSetting mainColor = (ColorSetting)this.addSetting("main-color", new ColorSetting("Main Color", -12614409));
   private final ColorSetting secondColor = (ColorSetting)this.addSetting("second-color", new ColorSetting("Second Color", -14923108));
   private final BooleanSetting halo = (BooleanSetting)this.addSetting("halo", new BooleanSetting("Halo", true));
   private final BooleanSetting blocks = (BooleanSetting)this.addSetting("block-bounce", new BooleanSetting("Block Bounce", true));
   private final Relic[] relics = new Relic[32];
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final class_5819 random = class_5819.method_43047();
   private final class_2338.class_2339 cursor = new class_2338.class_2339();
   private int live;
   private int age;
   private class_638 boundLevel;
   private int tintMain;
   private int tintShade;
   private int tintShine;

   public FloatingRelicsModule() {
      super("floating-relics", "Floating Relics", "Egyptian symbols float and spin around you.", ModuleCategory.VISUALS);
      this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
      this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));

      for(int i = 0; i < 32; ++i) {
         this.relics[i] = new Relic();
      }

   }

   protected void onEnable() {
      this.reset();
      this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      this.on(Render3DEvent.TYPE, this::onRender3D);
   }

   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.live = 0;
      this.age = 0;
      this.boundLevel = null;
   }

   private void syncLevel(class_638 level) {
      if (level != this.boundLevel) {
         this.boundLevel = level;
         this.live = 0;
      }
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      class_638 level = mc.field_1687;
      if (player != null && level != null) {
         this.syncLevel(level);
         ++this.age;
         int want = MobilePerformance.particles(Math.min(32, this.count.getInt()), 6);

         for(int i = this.live; i < want; ++i) {
            this.wake(this.relics[i], (i - this.live) * 3);
         }

         this.live = want;
         double px = player.method_23317();
         double py = player.method_23318();
         double pz = player.method_23321();
         float height = player.method_17682();
         float moveX = (float)(px - player.field_6014);
         float moveZ = (float)(pz - player.field_5969);
         float restitution = this.bounce.get();

         for(int i = 0; i < this.live; ++i) {
            this.step(this.relics[i], level, px, py, pz, height, moveX, moveZ, restitution);
         }

         for(int i = 0; i < this.live; ++i) {
            for(int j = i + 1; j < this.live; ++j) {
               this.knock(this.relics[i], this.relics[j], restitution);
            }
         }

      } else {
         this.reset();
      }
   }

   private void wake(Relic r, int delay) {
      r.placed = false;
      r.wakeAt = this.age + delay;
      r.orbit = 0.55F + 0.45F * this.random.method_43057();
      r.lift = this.random.method_43057();
      r.grain = 0.82F + 0.3F * this.random.method_43057();
      r.phase = this.random.method_43057() * ((float)Math.PI * 2F);
      r.restTurn = this.signed(0.07F, 0.12F);
   }

   private float signed(float min, float max) {
      float value = min + (max - min) * this.random.method_43057();
      return this.random.method_43056() ? value : -value;
   }

   private void place(Relic r, double px, double py, double pz) {
      float angle = this.random.method_43057() * ((float)Math.PI * 2F);
      double radius = (double)(this.spread.get() * r.orbit);
      r.x = px + (double)class_3532.method_15362((double)angle) * radius;
      r.y = py + 0.15;
      r.z = pz + (double)class_3532.method_15374((double)angle) * radius;
      r.lastX = r.x;
      r.lastY = r.y;
      r.lastZ = r.z;
      r.vx = -class_3532.method_15374((double)angle) * 0.03F;
      r.vy = 0.14F + 0.08F * this.random.method_43057();
      r.vz = class_3532.method_15362((double)angle) * 0.03F;
      r.yaw = this.random.method_43057() * ((float)Math.PI * 2F);
      r.lastYaw = r.yaw;
      r.turn = r.restTurn * 3.0F;
      r.tiltX = 0.0F;
      r.tiltZ = 0.0F;
      r.lastTiltX = 0.0F;
      r.lastTiltZ = 0.0F;
      r.swayX = this.signed(0.02F, 0.05F);
      r.swayZ = this.signed(0.02F, 0.05F);
      r.placed = true;
      r.placedAt = this.age;
   }

   private void step(Relic r, class_638 level, double px, double py, double pz, float height, float moveX, float moveZ, float restitution) {
      if (!r.placed) {
         if (this.age < r.wakeAt) {
            return;
         }

         this.place(r, px, py, pz);
      }

      r.lastX = r.x;
      r.lastY = r.y;
      r.lastZ = r.z;
      double dx = r.x - px;
      double dz = r.z - pz;
      double flat = Math.sqrt(dx * dx + dz * dz);
      double hover = py + 0.35 + (double)r.lift * ((double)height + 0.2) + 0.16 * (double)class_3532.method_15374((double)(((float)this.age + r.phase * 16.0F) * 0.045F));
      double leash = (double)this.spread.get() * (double)3.0F + (double)8.0F;
      if (!(flat > leash) && !(Math.abs(r.y - hover) > leash)) {
         float nx = flat > 1.0E-4 ? (float)(dx / flat) : 1.0F;
         float nz = flat > 1.0E-4 ? (float)(dz / flat) : 0.0F;
         float pull = (float)((double)(this.spread.get() * r.orbit) - flat) * 0.012F;
         float swirl = this.drift.get() * 0.0025F;
         r.vx += nx * pull - nz * swirl + moveX * 0.035F;
         r.vz += nz * pull + nx * swirl + moveZ * 0.035F;
         float wander = ((float)this.age + r.phase * 20.0F) * 0.06F;
         r.vx += class_3532.method_15374((double)wander) * 0.003F;
         r.vz += class_3532.method_15362((double)(wander * 1.31F)) * 0.003F;
         r.vy -= 0.016F;
         double sink = hover - r.y;
         if (sink > (double)0.0F) {
            r.vy += (float)Math.min(sink, (double)1.5F) * 0.045F;
         }

         r.vx *= 0.982F;
         r.vy *= 0.982F;
         r.vz *= 0.982F;
         float speed = r.vx * r.vx + r.vy * r.vy + r.vz * r.vz;
         if (speed > 0.80999994F) {
            float k = 0.9F / (float)Math.sqrt((double)speed);
            r.vx *= k;
            r.vy *= k;
            r.vz *= k;
         }

         boolean collide = this.blocks.get();
         float half = this.size.get() * r.grain * 0.5F;
         double next = r.x + (double)r.vx;
         if (collide && this.solid(level, next + (double)Math.copySign(half, r.vx), r.y, r.z)) {
            r.vx = -r.vx * restitution;
            this.tumble(r, 0.6F);
         } else {
            r.x = next;
         }

         next = r.y + (double)r.vy;
         if (collide && this.solid(level, r.x, next + (double)Math.copySign(half, r.vy), r.z)) {
            if (r.vy < 0.0F) {
               r.vx *= 0.86F;
               r.vz *= 0.86F;
            }

            r.vy = -r.vy * restitution;
            this.tumble(r, 0.8F);
         } else {
            r.y = next;
         }

         next = r.z + (double)r.vz;
         if (collide && this.solid(level, r.x, r.y, next + (double)Math.copySign(half, r.vz))) {
            r.vz = -r.vz * restitution;
            this.tumble(r, 0.6F);
         } else {
            r.z = next;
         }

         if (collide && this.solid(level, r.x, r.y, r.z)) {
            r.y += 0.2;
            r.lastY = r.y;
            r.vy = Math.max(r.vy, 0.08F);
         }

         if (Math.abs(r.yaw) > 512.0F) {
            r.yaw %= ((float)Math.PI * 2F);
         }

         r.lastYaw = r.yaw;
         r.lastTiltX = r.tiltX;
         r.lastTiltZ = r.tiltZ;
         r.turn += (r.restTurn * this.spinSpeed.get() - r.turn) * 0.03F;
         r.yaw += r.turn;
         r.swayX = (r.swayX - r.tiltX * 0.09F) * 0.9F;
         r.swayZ = (r.swayZ - r.tiltZ * 0.09F) * 0.9F;
         r.tiltX = class_3532.method_15363(r.tiltX + r.swayX, -0.55F, 0.55F);
         r.tiltZ = class_3532.method_15363(r.tiltZ + r.swayZ, -0.55F, 0.55F);
      } else {
         this.place(r, px, py, pz);
      }
   }

   private void knock(Relic a, Relic b, float restitution) {
      if (a.placed && b.placed) {
         double dx = b.x - a.x;
         double dy = b.y - a.y;
         double dz = b.z - a.z;
         double reach = (double)(this.size.get() * (a.grain + b.grain)) * (double)0.5F * 0.95;
         double d2 = dx * dx + dy * dy + dz * dz;
         if (!(d2 >= reach * reach) && !(d2 < 1.0E-8)) {
            double d = Math.sqrt(d2);
            float nx = (float)(dx / d);
            float ny = (float)(dy / d);
            float nz = (float)(dz / d);
            double push = (reach - d) * (double)0.5F;
            a.x -= (double)nx * push;
            a.y -= (double)ny * push;
            a.z -= (double)nz * push;
            b.x += (double)nx * push;
            b.y += (double)ny * push;
            b.z += (double)nz * push;
            float closing = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny + (a.vz - b.vz) * nz;
            if (!(closing <= 0.0F)) {
               float impulse = closing * (1.0F + restitution) * 0.5F;
               a.vx -= impulse * nx;
               a.vy -= impulse * ny;
               a.vz -= impulse * nz;
               b.vx += impulse * nx;
               b.vy += impulse * ny;
               b.vz += impulse * nz;
               this.tumble(a, 0.5F);
               this.tumble(b, 0.5F);
            }
         }
      }
   }

   private void tumble(Relic r, float strength) {
      r.turn += (this.random.method_43057() - 0.5F) * 0.3F * strength;
      r.swayX += (this.random.method_43057() - 0.5F) * 0.12F * strength;
      r.swayZ += (this.random.method_43057() - 0.5F) * 0.12F * strength;
   }

   private boolean solid(class_638 level, double x, double y, double z) {
      this.cursor.method_10103(class_3532.method_15357(x), class_3532.method_15357(y), class_3532.method_15357(z));
      class_2680 state = level.method_8320(this.cursor);
      if (state.method_26215()) {
         return false;
      } else {
         class_265 shape = state.method_26220(level, this.cursor);
         if (shape.method_1110()) {
            return false;
         } else {
            double lx = x - (double)this.cursor.method_10263();
            double ly = y - (double)this.cursor.method_10264();
            double lz = z - (double)this.cursor.method_10260();
            return lx >= shape.method_1091(class_2351.field_11048) && lx <= shape.method_1105(class_2351.field_11048) && ly >= shape.method_1091(class_2351.field_11052) && ly <= shape.method_1105(class_2351.field_11052) && lz >= shape.method_1091(class_2351.field_11051) && lz <= shape.method_1105(class_2351.field_11051);
         }
      }
   }

   private void onRender3D(Render3DEvent event) {
      if (this.live != 0) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1687 != null && mc.field_1724 != null) {
            this.canvas.begin(mc.field_1773.method_19418());
            float pt = event.partialTick;
            float time = (float)(System.nanoTime() - EPOCH) / 1.0E9F;
            boolean mobile = MobilePerformance.active();
            class_4604 frustum = !PerformanceModule.frustumCulling() && !mobile ? null : event.frustum;
            float base = this.size.get() * 0.5F * 1.25F;
            boolean larp = this.symbols.is("Larp");
            float[][] glyphs = larp ? LARP_GLYPHS : ANUBIS_GLYPHS;
            float[] fits = larp ? LARP_FIT : ANUBIS_FIT;
            boolean glow = this.halo.get();
            this.tint();

            for(int i = 0; i < this.live; ++i) {
               Relic r = this.relics[i];
               if (r.placed) {
                  int kind = i % glyphs.length;
                  float since = (float)(this.age - r.placedAt) + pt;
                  float extent = base * r.grain * fits[kind] * appear(since / 12.0F);
                  if (!(extent < 0.004F)) {
                     double x = class_3532.method_16436((double)pt, r.lastX, r.x);
                     double y = class_3532.method_16436((double)pt, r.lastY, r.y) + (double)(extent * 0.2F * class_3532.method_15374((double)(time * 2.4F + r.phase * 3.0F)));
                     double z = class_3532.method_16436((double)pt, r.lastZ, r.z);
                     double reach = renderReach(extent, glow, mobile);
                     if (frustum == null || frustum.method_23093(new class_238(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach))) {
                        float fade = Math.min(1.0F, since / 7.2000003F);
                        float yaw = class_3532.method_16439(pt, r.lastYaw, r.yaw);
                        float tiltX = class_3532.method_16439(pt, r.lastTiltX, r.tiltX) + 0.12F * class_3532.method_15374((double)(time * 1.3F + r.phase));
                        float tiltZ = class_3532.method_16439(pt, r.lastTiltZ, r.tiltZ) + 0.12F * class_3532.method_15374((double)(time * 0.9F + r.phase * 2.3F));
                        float shimmer = glow ? (0.1F + 0.07F * class_3532.method_15374((double)(time * 2.1F + r.phase))) * fade : 0.0F;
                        float[] glyph = mobile ? RelicGlyphs.mobile(glyphs[kind]) : glyphs[kind];
                        this.drawRelic(glyph, x, y, z, extent, yaw, tiltX, tiltZ, fade, shimmer);
                     }
                  }
               }
            }

            this.canvas.submit(false);
         }
      }
   }

   private static double renderReach(float extent, boolean halo, boolean mobile) {
      return (double)extent * (mobile && halo ? (double)(HALO_SCALES[HALO_SCALES.length - 1] + 0.6F) : (double)2.0F);
   }

   private void tint() {
      if (this.colorMode.is("Manual")) {
         this.tintMain = class_9848.method_61334(this.mainColor.argb());
         this.tintShade = class_9848.method_61334(this.secondColor.argb());
      } else {
         this.tintMain = CosmeticAccent.argb();
         this.tintShade = class_9848.method_61321(this.tintMain, 0.45F);
      }

      this.tintShine = class_9848.method_61319(0.6F, this.tintMain, -1);
   }

   private void drawRelic(float[] glyph, double x, double y, double z, float extent, float yaw, float tiltX, float tiltZ, float fade, float shimmer) {
      float cy = class_3532.method_15362((double)yaw);
      float sy = class_3532.method_15374((double)yaw);
      float cx = class_3532.method_15362((double)tiltX);
      float sx = class_3532.method_15374((double)tiltX);
      float cz = class_3532.method_15362((double)tiltZ);
      float sz = class_3532.method_15374((double)tiltZ);
      float ux = cy * cz + sy * sx * sz;
      float uy = cx * sz;
      float uz = -sy * cz + cy * sx * sz;
      float vx = -cy * sz + sy * sx * cz;
      float vy = cx * cz;
      float vz = sy * sz + cy * sx * cz;
      float nx = sy * cx;
      float ny = -sx;
      float nz = cy * cx;
      double tx = this.canvas.camX() - x;
      double ty = this.canvas.camY() - y;
      double tz = this.canvas.camZ() - z;
      double distSqr = tx * tx + ty * ty + tz * tz;
      if (!(distSqr < 1.0E-8)) {
         double dist = Math.sqrt(distSqr);
         float facing = (float)(((double)nx * tx + (double)ny * ty + (double)nz * tz) / dist);
         if (facing < 0.0F) {
            ux = -ux;
            uy = -uy;
            uz = -uz;
            nx = -nx;
            ny = -ny;
            nz = -nz;
            facing = -facing;
         }

         if (shimmer > 0.0F) {
            double push = (double)(extent * 0.6F) / dist;
            double hx = x - tx * push;
            double hy = y - ty * push;
            double hz = z - tz * push;
            float[] haloShape = MobilePerformance.active() ? GlyphShapes.OCTAGON : GlyphShapes.DISK;

            for(int layer = 0; layer < HALO_SCALES.length; ++layer) {
               this.canvas.sprite(haloShape, hx, hy, hz, extent * HALO_SCALES[layer], 0.0F, withAlpha(this.tintMain, shimmer * HALO_WEIGHTS[layer]));
            }
         }

         double au = (double)(ux * extent);
         double bu = (double)(uy * extent);
         double cu = (double)(uz * extent);
         double av = (double)(vx * extent);
         double bv = (double)(vy * extent);
         double cv = (double)(vz * extent);
         double depth = (double)(extent * 0.07F);
         if (distSqr < (double)576.0F && facing < 0.995F) {
            this.canvas.shape(glyph, x - (double)nx * depth, y - (double)ny * depth, z - (double)nz * depth, au, bu, cu, av, bv, cv, withAlpha(this.tintShade, 0.9F * fade));
         }

         float light = Math.max(0.0F, nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z);
         float glint = facing * facing;
         glint *= glint;
         glint *= glint;
         int face = class_9848.method_61319(glint * 0.45F, class_9848.method_61321(this.tintMain, 0.6F + 0.22F * light + 0.18F * facing), this.tintShine);
         this.canvas.shape(glyph, x + (double)nx * depth, y + (double)ny * depth, z + (double)nz * depth, au, bu, cu, av, bv, cv, withAlpha(face, 0.94F * fade));
      }
   }

   private static float appear(float t) {
      if (t >= 1.0F) {
         return 1.0F;
      } else if (t <= 0.0F) {
         return 0.0F;
      } else {
         float inv = 1.0F - t;
         return 1.0F - inv * inv * inv + 0.22F * class_3532.method_15374((double)(t * (float)Math.PI)) * inv;
      }
   }

   private static int withAlpha(int rgb, float alpha) {
      int a = (int)(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F + 0.5F);
      return a << 24 | rgb & 16777215;
   }

   static {
      ANUBIS_GLYPHS = new float[][]{RelicGlyphs.ANKH, RelicGlyphs.EYE, RelicGlyphs.SCARAB, RelicGlyphs.DJED, RelicGlyphs.FEATHER, RelicGlyphs.WAS};
      ANUBIS_FIT = new float[]{1.0F, 0.84F, 0.94F, 1.0F, 1.0F, 1.0F};
      LARP_GLYPHS = new float[][]{RelicGlyphs.DOLLAR, RelicGlyphs.EURO, RelicGlyphs.BITCOIN};
      LARP_FIT = new float[]{1.0F, 0.92F, 0.96F};
      HALO_SCALES = new float[]{1.35F, 2.0F};
      HALO_WEIGHTS = new float[]{1.0F, 0.5F};
      EPOCH = System.nanoTime();
      float lx = 0.32F;
      float ly = 0.87F;
      float lz = 0.37F;
      float length = (float)Math.sqrt((double)(lx * lx + ly * ly + lz * lz));
      LIGHT_X = lx / length;
      LIGHT_Y = ly / length;
      LIGHT_Z = lz / length;
   }

   @Environment(EnvType.CLIENT)
   private static final class Relic {
      double x;
      double y;
      double z;
      double lastX;
      double lastY;
      double lastZ;
      float vx;
      float vy;
      float vz;
      float yaw;
      float lastYaw;
      float turn;
      float restTurn;
      float tiltX;
      float tiltZ;
      float lastTiltX;
      float lastTiltZ;
      float swayX;
      float swayZ;
      float orbit;
      float lift;
      float grain;
      float phase;
      boolean placed;
      int wakeAt;
      int placedAt;
   }
}
