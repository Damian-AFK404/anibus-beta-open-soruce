package net.anubis.client.util;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12175;
import net.minecraft.class_12177;
import net.minecraft.class_12180;
import net.minecraft.class_1923;
import net.minecraft.class_243;
import net.minecraft.class_9848;

@Environment(EnvType.CLIENT)
public final class ChunkOverlay {
   public static final double MARKER_Y = (double)63.0F;
   private static final float EDGE_SHADE = 0.77F;
   private static final float LINE_WIDTH = 1.5F;
   private static final double CORNER_RADIUS = (double)2.0F;
   private static final int ARC_SEGMENTS = 4;
   private static final float GROW_SECONDS = 0.3F;
   private static final float SHRINK_SECONDS = 0.25F;
   private static final float SETTLE_SECONDS = 0.25F;
   private static final float MAX_WAIT_SECONDS = 1.0F;
   private static final float RESET_GAP_SECONDS = 0.5F;
   private final Long2FloatOpenHashMap shown = new Long2FloatOpenHashMap();
   private final LongOpenHashSet pending = new LongOpenHashSet();
   private final LongOpenHashSet done = new LongOpenHashSet();
   private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
   private final LongArrayList group = new LongArrayList();
   private final ChunkTileGeometry.Cache mobileTiles = new ChunkTileGeometry.Cache();
   private long lastNanos;
   private long pendingSince;
   private long lastAdded;

   public void draw(LongSet chunks, int fill, float partialTick) {
      boolean mobile = MobilePerformance.active();
      if (mobile) {
         this.mobileTiles.beginFrame();
      } else {
         this.mobileTiles.clear();
      }

      long now = System.nanoTime();
      float dt = this.lastNanos == 0L ? 0.0F : (float)(now - this.lastNanos) / 1.0E9F;
      this.lastNanos = now;
      if (dt > 0.5F) {
         this.shown.clear();
         this.pending.clear();
         this.mobileTiles.clear();
      }

      this.collect(chunks, now);
      this.step(chunks, dt);
      if (this.shown.isEmpty()) {
         this.mobileTiles.clear();
      } else {
         int edge = class_9848.method_61330(Math.min(255, class_9848.method_61320(fill) * 3 / 2), class_9848.method_61321(fill, 0.77F));
         FoglessLines lines = FoglessLines.forFrame(partialTick);
         this.done.clear();
         LongIterator keys = this.shown.keySet().iterator();

         while(keys.hasNext()) {
            long start = keys.nextLong();
            if (this.done.add(start)) {
               this.drawGroup(lines, chunks, start, fill, edge);
            }
         }

      }
   }

   private void collect(LongSet chunks, long now) {
      LongIterator keys = chunks.iterator();

      while(keys.hasNext()) {
         long key = keys.nextLong();
         if (!this.shown.containsKey(key) && !this.pending.contains(key)) {
            if (this.pending.isEmpty()) {
               this.pendingSince = now;
            }

            this.pending.add(key);
            this.lastAdded = now;
         }
      }

      keys = this.pending.iterator();

      while(keys.hasNext()) {
         if (!chunks.contains(keys.nextLong())) {
            keys.remove();
         }
      }

      if (!this.pending.isEmpty()) {
         if (!((float)(now - this.lastAdded) / 1.0E9F < 0.25F) || !((float)(now - this.pendingSince) / 1.0E9F < 1.0F)) {
            keys = this.pending.iterator();

            while(keys.hasNext()) {
               this.shown.put(keys.nextLong(), 0.0F);
            }

            this.pending.clear();
         }
      }
   }

   private void step(LongSet chunks, float dt) {
      ObjectIterator<Long2FloatMap.Entry> entries = this.shown.long2FloatEntrySet().fastIterator();

      while(entries.hasNext()) {
         Long2FloatMap.Entry entry = (Long2FloatMap.Entry)entries.next();
         if (chunks.contains(entry.getLongKey())) {
            entry.setValue(Math.min(1.0F, entry.getFloatValue() + dt / 0.3F));
         } else {
            float left = entry.getFloatValue() - dt / 0.25F;
            if (left <= 0.0F) {
               entries.remove();
            } else {
               entry.setValue(left);
            }
         }
      }

   }

   private void drawGroup(FoglessLines lines, LongSet chunks, long start, int fill, int edge) {
      float progress = this.shown.get(start);
      boolean present = chunks.contains(start);
      this.group.clear();
      this.queue.enqueue(start);
      double sumX = (double)0.0F;
      double sumZ = (double)0.0F;

      while(!this.queue.isEmpty()) {
         long key = this.queue.dequeueLong();
         this.group.add(key);
         int cx = class_1923.method_8325(key);
         int cz = class_1923.method_8332(key);
         sumX += (double)(cx << 4) + (double)8.0F;
         sumZ += (double)(cz << 4) + (double)8.0F;
         this.visit(chunks, class_1923.method_8331(cx, cz - 1), progress, present);
         this.visit(chunks, class_1923.method_8331(cx, cz + 1), progress, present);
         this.visit(chunks, class_1923.method_8331(cx - 1, cz), progress, present);
         this.visit(chunks, class_1923.method_8331(cx + 1, cz), progress, present);
      }

      float size = easeOut(progress);
      if (!(size <= 0.001F)) {
         double mx = sumX / (double)this.group.size();
         double mz = sumZ / (double)this.group.size();
         int groupFill = size >= 1.0F ? fill : fade(fill, size);
         int groupEdge = size >= 1.0F ? edge : fade(edge, size);

         for(int i = 0; i < this.group.size(); ++i) {
            long key = this.group.getLong(i);
            int cx = class_1923.method_8325(key);
            int cz = class_1923.method_8332(key);
            double x0 = mx + ((double)(cx << 4) - mx) * (double)size;
            double z0 = mz + ((double)(cz << 4) - mz) * (double)size;
            double x1 = x0 + (double)16.0F * (double)size;
            double z1 = z0 + (double)16.0F * (double)size;
            boolean north = !this.joined(chunks, class_1923.method_8331(cx, cz - 1), progress, present);
            boolean south = !this.joined(chunks, class_1923.method_8331(cx, cz + 1), progress, present);
            boolean west = !this.joined(chunks, class_1923.method_8331(cx - 1, cz), progress, present);
            boolean east = !this.joined(chunks, class_1923.method_8331(cx + 1, cz), progress, present);
            if (MobilePerformance.active() && size >= 1.0F) {
               int mask = (north ? 1 : 0) | (south ? 2 : 0) | (west ? 4 : 0) | (east ? 8 : 0);
               ChunkTileGeometry geometry = this.mobileTiles.get(key, x0, z0, x1, z1, (double)2.0F * (double)size, mask);

               for(int edgeIndex = 0; edgeIndex < geometry.curves.length; edgeIndex += 2) {
                  lines.line(geometry.curves[edgeIndex], geometry.curves[edgeIndex + 1], groupEdge, 1.5F);
               }

               class_12180.method_75553(new Plane(geometry.outline, groupFill)).method_75533();

               for(int edgeIndex = 0; edgeIndex < geometry.edges.length; edgeIndex += 2) {
                  lines.line(geometry.edges[edgeIndex], geometry.edges[edgeIndex + 1], groupEdge, 1.5F);
               }
            } else {
               tile(lines, x0, z0, x1, z1, (double)2.0F * (double)size, north, south, west, east, groupFill, groupEdge);
            }
         }

      }
   }

   private void visit(LongSet chunks, long key, float progress, boolean present) {
      if (this.joined(chunks, key, progress, present) && this.done.add(key)) {
         this.queue.enqueue(key);
      }

   }

   private boolean joined(LongSet chunks, long key, float progress, boolean present) {
      return this.shown.containsKey(key) && this.shown.get(key) == progress && chunks.contains(key) == present;
   }

   private static void tile(FoglessLines lines, double x0, double z0, double x1, double z1, double radius, boolean north, boolean south, boolean west, boolean east, int fill, int edge) {
      double nw = north && west ? radius : (double)0.0F;
      double ne = north && east ? radius : (double)0.0F;
      double se = south && east ? radius : (double)0.0F;
      double sw = south && west ? radius : (double)0.0F;
      List<class_243> outline = new ArrayList(20);
      corner(outline, lines, x0 + nw, z0 + nw, nw, (double)180.0F, edge);
      corner(outline, lines, x1 - ne, z0 + ne, ne, (double)270.0F, edge);
      corner(outline, lines, x1 - se, z1 - se, se, (double)0.0F, edge);
      corner(outline, lines, x0 + sw, z1 - sw, sw, (double)90.0F, edge);
      class_12180.method_75553(new Plane((class_243[])outline.toArray(new class_243[0]), fill)).method_75533();
      if (north) {
         edge(lines, x0 + nw, z0, x1 - ne, z0, edge);
      }

      if (south) {
         edge(lines, x0 + sw, z1, x1 - se, z1, edge);
      }

      if (west) {
         edge(lines, x0, z0 + nw, x0, z1 - sw, edge);
      }

      if (east) {
         edge(lines, x1, z0 + ne, x1, z1 - se, edge);
      }

   }

   private static void corner(List<class_243> outline, FoglessLines lines, double cx, double cz, double radius, double startDegrees, int edge) {
      if (radius <= (double)0.0F) {
         outline.add(new class_243(cx, (double)63.0F, cz));
      } else {
         class_243 previous = null;

         for(int i = 0; i <= 4; ++i) {
            double angle = Math.toRadians(startDegrees + (double)90.0F * (double)i / (double)4.0F);
            class_243 point = new class_243(cx + Math.cos(angle) * radius, (double)63.0F, cz + Math.sin(angle) * radius);
            outline.add(point);
            if (previous != null) {
               lines.line(previous, point, edge, 1.5F);
            }

            previous = point;
         }

      }
   }

   private static float easeOut(float t) {
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   private static int fade(int color, float amount) {
      return class_9848.method_61330(Math.round((float)class_9848.method_61320(color) * amount), color);
   }

   private static void edge(FoglessLines lines, double ax, double az, double bx, double bz, int color) {
      lines.line(new class_243(ax, (double)63.0F, az), new class_243(bx, (double)63.0F, bz), color, 1.5F);
   }

   @Environment(EnvType.CLIENT)
   private static record Plane(class_243[] outline, int color) implements class_12175 {
      public void method_75531(class_12177 primitives, float alpha) {
         primitives.method_75478(this.outline, ChunkOverlay.fade(this.color, alpha));
      }
   }
}
