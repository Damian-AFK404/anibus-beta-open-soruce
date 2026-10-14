package net.anubis.client.module.impl.misc;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.anubis.client.gui.WorldLabel;
import net.anubis.client.util.FoglessLines;
import net.anubis.client.util.TracerEngine;
import net.anubis.client.util.glyph.GlyphCanvas;
import net.anubis.client.util.glyph.GlyphShapes;
import net.anubis.client.util.glyph.GlyphSwarm;
import net.anubis.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1109;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3417;
import net.minecraft.class_3532;
import net.minecraft.class_5819;

@Environment(EnvType.CLIENT)
final class NetheriteVisuals {
   static final String DOTS = "Dots";
   static final String CUBES = "Cubes";
   static final String PULSES = "Pulses";
   private static final long PULSE_NANOS = 1400000000L;
   private static final float WAVE_SECONDS = 0.9F;
   private static final int MAX_WAVES = 12;
   private static final int GLOW_LIMIT = 32;
   private static final int DEBRIS_TRACERS = 3;
   private static final int LABEL_LIMIT = 8;
   private static final double PROBE_RADIUS = (double)12.0F;
   private static final int DOT_LIMIT = 1800;
   private static final int CUBE_LIMIT = 450;
   private static final double MISSING_RADIUS = (double)32.0F;
   private static final int MISSING_LIMIT = 1600;
   private static final float PROBE_FRESH_TICKS = 14.0F;
   private static final float[] SHELL_INFLATE = new float[]{0.06F, 0.16F, 0.3F};
   private static final float[] SHELL_ALPHA = new float[]{0.16F, 0.08F, 0.04F};
   private final GlyphCanvas canvas = new GlyphCanvas();
   private final GlyphSwarm swarm = new GlyphSwarm(128);
   private final class_5819 random = class_5819.method_43047();
   private final List<Wave> waves = new ArrayList();
   private final List<Label> labels = new ArrayList();
   private long lastFrame;
   private double pixel;

   void clear() {
      this.swarm.clear();
      this.waves.clear();
      this.labels.clear();
      this.lastFrame = 0L;
   }

   void burst(class_2338 pos, int rgb) {
      double x = (double)pos.method_10263() + (double)0.5F;
      double y = (double)pos.method_10264() + (double)0.5F;
      double z = (double)pos.method_10260() + (double)0.5F;
      if (this.waves.size() >= 12) {
         this.waves.removeFirst();
      }

      this.waves.add(new Wave(x, y, z, System.nanoTime()));
      int light = GlyphTint.lift(rgb, 0.55F);

      for(int i = 0; i < 2; ++i) {
         this.swarm.spawn((byte)0, x + (double)this.jitter(0.3F), y + 0.6, z + (double)this.jitter(0.3F), this.jitter(0.2F), 1.1F, this.jitter(0.2F), 1.8F, 0.22F, this.jitter(0.3F), this.jitter(0.5F), this.random.method_43057(), light, 128);
      }

      for(int i = 0; i < 10; ++i) {
         float angle = this.random.method_43057() * ((float)Math.PI * 2F);
         float speed = 2.2F + this.random.method_43057() * 1.6F;
         int color = GlyphTint.mix(rgb, -1, this.random.method_43057() * 0.5F);
         this.swarm.spawn((byte)3, x, y, z, class_3532.method_15362((double)angle) * speed, 1.2F + this.random.method_43057() * 1.5F, class_3532.method_15374((double)angle) * speed, 0.8F, 0.09F, angle, 2.8F, this.random.method_43057(), color, 128);
      }

      class_310.method_1551().method_1483().method_4873(class_1109.method_4757(class_3417.field_26980, 1.3F, 0.9F));
   }

   void render(class_310 mc, float partialTick, class_243 eye, List<Section> sections, List<class_2338> revealed, AdvancedFinder finder, Options options) {
      long now = System.nanoTime();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.swarm.step(dt);
      this.canvas.begin(mc.field_1773.method_19418());
      this.pixel = FoglessLines.pixelScale(partialTick);
      int accent = GlyphTint.alpha(options.near(), 1.0F);
      int light = GlyphTint.lift(accent, 0.55F);
      float pulse = 0.5F + 0.5F * class_3532.method_15374((double)((float)(now % 1400000000L) / 1.4E9F * ((float)Math.PI * 2F)));
      this.labels.clear();
      this.drawRevealed(eye, revealed, options, accent, light, pulse);
      this.drawWaves(now, accent, light);
      if (options.showProbes()) {
         this.drawProbes(eye, finder, options, accent, light, pulse);
      }

      this.drawSections(sections, options, light, pulse);
      this.drawTracers(sections, options);
      this.swarm.draw(this.canvas, 1.0F, true);
      this.canvas.submit(true);
   }

   void overlay(ImDrawList dl, float screenW, float screenH) {
      class_310 mc = class_310.method_1551();

      for(Label label : this.labels) {
         float[] screen = WorldLabel.project(mc, label.pos(), screenW, screenH);
         if (screen != null) {
            WorldLabel.drawText(dl, screen[0], screen[1], label.primary(), label.secondary(), 1.0F);
         }
      }

   }

   boolean hasLabels() {
      return !this.labels.isEmpty();
   }

   private static int tint(Options options, double distance) {
      float t = (float)class_3532.method_15350(distance / options.fade(), (double)0.0F, (double)1.0F);
      return GlyphTint.alpha(GlyphTint.mix(options.near(), options.far(), t), 1.0F);
   }

   private void drawSections(List<Section> sections, Options options, int light, float pulse) {
      for(Section section : sections) {
         if (section.progress() >= 0.0F) {
            this.drawScanning(section, light, pulse);
         }
      }

      for(Section section : sections) {
         if (section.progress() < 0.0F) {
            this.drawCandidate(section, options);
         }
      }

   }

   private void drawCandidate(Section section, Options options) {
      double far = class_3532.method_15350(section.distance() / options.fade(), (double)0.0F, (double)1.0F);
      int base = tint(options, section.distance());
      this.box(section.box(), GlyphTint.alpha(base, (float)(0.95 - 0.3 * far)), (float)(2.4 - 0.8 * far));
      this.faces(section.box(), GlyphTint.alpha(base, (float)(0.07 - 0.03 * far)));
   }

   private void drawScanning(Section section, int light, float pulse) {
      class_238 box = section.box();
      this.box(box, GlyphTint.alpha(light, 0.6F + 0.4F * pulse), 3.2F);
      this.faces(box, GlyphTint.alpha(light, 0.05F));
      double y = box.field_1322 + (box.field_1325 - box.field_1322) * (double)section.progress();
      int plane = GlyphTint.alpha(light, 0.2F);
      this.canvas.quad(box.field_1323, y, box.field_1321, box.field_1320, y, box.field_1321, box.field_1320, y, box.field_1324, box.field_1323, y, box.field_1324, plane);
      this.canvas.quad(box.field_1323, y, box.field_1321, box.field_1323, y, box.field_1324, box.field_1320, y, box.field_1324, box.field_1320, y, box.field_1321, plane);
      int rim = GlyphTint.alpha(light, 0.95F);
      this.line(box.field_1323, y, box.field_1321, box.field_1320, y, box.field_1321, rim, 2.4F);
      this.line(box.field_1320, y, box.field_1321, box.field_1320, y, box.field_1324, rim, 2.4F);
      this.line(box.field_1320, y, box.field_1324, box.field_1323, y, box.field_1324, rim, 2.4F);
      this.line(box.field_1323, y, box.field_1324, box.field_1323, y, box.field_1321, rim, 2.4F);
   }

   private void drawTracers(List<Section> sections, Options options) {
      int drawn = 0;

      for(Section section : sections) {
         if (drawn >= options.tracerCount()) {
            break;
         }

         if (!(section.distance() < (double)1.0F)) {
            class_243 center = section.box().method_1005();
            TracerEngine.draw(center, GlyphTint.alpha(tint(options, section.distance()), 0.85F));
            String pct = section.progress() >= 0.0F ? Math.round(section.progress() * 100.0F) + "%" : null;
            this.labels.add(new Label(center, String.format(Locale.ROOT, "%.0fm", section.distance()), pct));
            ++drawn;
         }
      }

   }

   private void drawProbes(class_243 eye, AdvancedFinder finder, Options options, int accent, int light, float pulse) {
      int missing = GlyphTint.mix(light, -1, 0.5F);
      finder.forEachMissing(eye, (double)32.0F, 1600, options.missingFrom(), (x, y, z) -> this.canvas.sprite(GlyphShapes.DIAMOND, (double)x + (double)0.5F, (double)y + (double)0.5F, (double)z + (double)0.5F, 0.11F + 0.05F * pulse, 0.0F, GlyphTint.alpha(missing, 0.5F + 0.45F * pulse)));
      int fresh = GlyphTint.mix(light, -1, 0.6F);
      String style = options.probeStyle();
      int limit = "Cubes".equals(style) ? 450 : 1800;
      finder.forEachProbe(eye, (double)12.0F, limit, options.missingFrom(), (x, y, z, age) -> {
         float t = Math.min(1.0F, (float)age / 14.0F);
         int color = GlyphTint.mix(fresh, accent, t);
         double cx = (double)x + (double)0.5F;
         double cy = (double)y + (double)0.5F;
         double cz = (double)z + (double)0.5F;
         if ("Cubes".equals(style)) {
            double half = 0.2 - 0.1 * (double)t;
            this.faces(new class_238(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half), GlyphTint.alpha(color, 0.65F - 0.3F * t));
         } else {
            this.canvas.sprite(GlyphShapes.DIAMOND, cx, cy, cz, 0.13F - 0.06F * t, 0.0F, GlyphTint.alpha(color, 0.95F - 0.5F * t));
            if ("Pulses".equals(style) && t < 1.0F) {
               this.canvas.sprite(GlyphShapes.HALO, cx, cy, cz, 0.15F + 0.55F * t, 0.0F, GlyphTint.alpha(fresh, 0.55F * (1.0F - t) * (1.0F - t)));
            }

         }
      });
   }

   private void drawRevealed(class_243 eye, List<class_2338> revealed, Options options, int accent, int light, float pulse) {
      List<class_2338> sorted = new ArrayList(revealed);
      sorted.sort(Comparator.comparingDouble((posx) -> posx.method_19770(eye)));

      for(int i = 0; i < sorted.size(); ++i) {
         class_2338 pos = (class_2338)sorted.get(i);
         double distance = Math.sqrt(pos.method_19770(eye));
         if (distance > options.range()) {
            break;
         }

         class_238 box = new class_238(pos);
         if (i < 32) {
            for(int s = 0; s < SHELL_INFLATE.length; ++s) {
               this.faces(box.method_1014((double)SHELL_INFLATE[s]), GlyphTint.alpha(accent, SHELL_ALPHA[s] * (0.7F + 0.3F * pulse)));
            }
         }

         this.faces(box, GlyphTint.alpha(accent, 0.3F));
         this.box(box, GlyphTint.alpha(light, 0.95F), 2.6F);
         class_243 center = box.method_1005();
         if (i < 3) {
            TracerEngine.draw(center, GlyphTint.alpha(accent, 0.8F));
         }

         if (i < 8) {
            this.labels.add(new Label(center.method_1031((double)0.0F, 0.9, (double)0.0F), String.format(Locale.ROOT, "%.0fm", distance), (String)null));
         }
      }

   }

   private void drawWaves(long now, int accent, int light) {
      Iterator<Wave> iterator = this.waves.iterator();

      while(iterator.hasNext()) {
         Wave wave = (Wave)iterator.next();
         float t = (float)(now - wave.born()) / 1.0E9F / 0.9F;
         if (t >= 1.0F) {
            iterator.remove();
         } else {
            float ease = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
            float radius = 0.4F + 2.6F * ease;
            float fade = (1.0F - t) * (1.0F - t);
            this.canvas.ring(wave.x(), wave.y(), wave.z(), radius * 0.86F, radius, 48, GlyphTint.alpha(light, fade * 0.9F));
            this.canvas.ring(wave.x(), wave.y(), wave.z(), radius, radius * 1.25F, 48, GlyphTint.alpha(accent, fade * 0.3F));
         }
      }

   }

   private void line(double ax, double ay, double az, double bx, double by, double bz, int argb, float width) {
      this.canvas.ribbon(ax, ay, az, bx, by, bz, argb, width, this.pixel);
   }

   private void box(class_238 b, int argb, float width) {
      this.line(b.field_1323, b.field_1322, b.field_1321, b.field_1320, b.field_1322, b.field_1321, argb, width);
      this.line(b.field_1320, b.field_1322, b.field_1321, b.field_1320, b.field_1322, b.field_1324, argb, width);
      this.line(b.field_1320, b.field_1322, b.field_1324, b.field_1323, b.field_1322, b.field_1324, argb, width);
      this.line(b.field_1323, b.field_1322, b.field_1324, b.field_1323, b.field_1322, b.field_1321, argb, width);
      this.line(b.field_1323, b.field_1325, b.field_1321, b.field_1320, b.field_1325, b.field_1321, argb, width);
      this.line(b.field_1320, b.field_1325, b.field_1321, b.field_1320, b.field_1325, b.field_1324, argb, width);
      this.line(b.field_1320, b.field_1325, b.field_1324, b.field_1323, b.field_1325, b.field_1324, argb, width);
      this.line(b.field_1323, b.field_1325, b.field_1324, b.field_1323, b.field_1325, b.field_1321, argb, width);
      this.line(b.field_1323, b.field_1322, b.field_1321, b.field_1323, b.field_1325, b.field_1321, argb, width);
      this.line(b.field_1320, b.field_1322, b.field_1321, b.field_1320, b.field_1325, b.field_1321, argb, width);
      this.line(b.field_1320, b.field_1322, b.field_1324, b.field_1320, b.field_1325, b.field_1324, argb, width);
      this.line(b.field_1323, b.field_1322, b.field_1324, b.field_1323, b.field_1325, b.field_1324, argb, width);
   }

   private void faces(class_238 b, int argb) {
      this.face(b.field_1323, b.field_1322, b.field_1321, b.field_1320, b.field_1322, b.field_1321, b.field_1320, b.field_1322, b.field_1324, b.field_1323, b.field_1322, b.field_1324, argb);
      this.face(b.field_1323, b.field_1325, b.field_1321, b.field_1320, b.field_1325, b.field_1321, b.field_1320, b.field_1325, b.field_1324, b.field_1323, b.field_1325, b.field_1324, argb);
      this.face(b.field_1323, b.field_1322, b.field_1321, b.field_1320, b.field_1322, b.field_1321, b.field_1320, b.field_1325, b.field_1321, b.field_1323, b.field_1325, b.field_1321, argb);
      this.face(b.field_1323, b.field_1322, b.field_1324, b.field_1320, b.field_1322, b.field_1324, b.field_1320, b.field_1325, b.field_1324, b.field_1323, b.field_1325, b.field_1324, argb);
      this.face(b.field_1323, b.field_1322, b.field_1321, b.field_1323, b.field_1322, b.field_1324, b.field_1323, b.field_1325, b.field_1324, b.field_1323, b.field_1325, b.field_1321, argb);
      this.face(b.field_1320, b.field_1322, b.field_1321, b.field_1320, b.field_1322, b.field_1324, b.field_1320, b.field_1325, b.field_1324, b.field_1320, b.field_1325, b.field_1321, argb);
   }

   private void face(double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz, double dx, double dy, double dz, int argb) {
      this.canvas.quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, argb);
      this.canvas.quad(ax, ay, az, dx, dy, dz, cx, cy, cz, bx, by, bz, argb);
   }

   private float jitter(float amount) {
      return (this.random.method_43057() * 2.0F - 1.0F) * amount;
   }

   @Environment(EnvType.CLIENT)
   static record Section(class_238 box, double distance, float progress) {
   }

   @Environment(EnvType.CLIENT)
   static record Options(int near, int far, double fade, double range, String probeStyle, float missingFrom, int tracerCount, boolean showProbes) {
   }

   @Environment(EnvType.CLIENT)
   private static record Wave(double x, double y, double z, long born) {
   }

   @Environment(EnvType.CLIENT)
   private static record Label(class_243 pos, String primary, String secondary) {
   }
}
