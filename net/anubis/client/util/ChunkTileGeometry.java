package net.anubis.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(EnvType.CLIENT)
final class ChunkTileGeometry {
   final class_243[] outline;
   final class_243[] curves;
   final class_243[] edges;
   private final double x0;
   private final double z0;
   private final double x1;
   private final double z1;
   private final double radius;
   private final int mask;
   private long usedFrame;

   private ChunkTileGeometry(double x0, double z0, double x1, double z1, double radius, int mask) {
      this.x0 = x0;
      this.z0 = z0;
      this.x1 = x1;
      this.z1 = z1;
      this.radius = radius;
      this.mask = mask;
      boolean north = (mask & 1) != 0;
      boolean south = (mask & 2) != 0;
      boolean west = (mask & 4) != 0;
      boolean east = (mask & 8) != 0;
      double nw = north && west ? radius : (double)0.0F;
      double ne = north && east ? radius : (double)0.0F;
      double se = south && east ? radius : (double)0.0F;
      double sw = south && west ? radius : (double)0.0F;
      List<class_243> outline = new ArrayList(20);
      List<class_243> curves = new ArrayList(32);
      List<class_243> edges = new ArrayList(8);
      corner(outline, curves, x0 + nw, z0 + nw, nw, (double)180.0F);
      corner(outline, curves, x1 - ne, z0 + ne, ne, (double)270.0F);
      corner(outline, curves, x1 - se, z1 - se, se, (double)0.0F);
      corner(outline, curves, x0 + sw, z1 - sw, sw, (double)90.0F);
      if (north) {
         edge(edges, x0 + nw, z0, x1 - ne, z0);
      }

      if (south) {
         edge(edges, x0 + sw, z1, x1 - se, z1);
      }

      if (west) {
         edge(edges, x0, z0 + nw, x0, z1 - sw);
      }

      if (east) {
         edge(edges, x1, z0 + ne, x1, z1 - se);
      }

      this.outline = (class_243[])outline.toArray(new class_243[0]);
      this.curves = (class_243[])curves.toArray(new class_243[0]);
      this.edges = (class_243[])edges.toArray(new class_243[0]);
   }

   private boolean matches(double x0, double z0, double x1, double z1, double radius, int mask) {
      return this.mask == mask && Double.compare(this.x0, x0) == 0 && Double.compare(this.z0, z0) == 0 && Double.compare(this.x1, x1) == 0 && Double.compare(this.z1, z1) == 0 && Double.compare(this.radius, radius) == 0;
   }

   private static void corner(List<class_243> outline, List<class_243> curves, double cx, double cz, double radius, double startDegrees) {
      if (radius <= (double)0.0F) {
         outline.add(new class_243(cx, (double)63.0F, cz));
      } else {
         class_243 previous = null;

         for(int i = 0; i <= 4; ++i) {
            double angle = Math.toRadians(startDegrees + (double)90.0F * (double)i / (double)4.0F);
            class_243 point = new class_243(cx + Math.cos(angle) * radius, (double)63.0F, cz + Math.sin(angle) * radius);
            outline.add(point);
            if (previous != null) {
               curves.add(previous);
               curves.add(point);
            }

            previous = point;
         }

      }
   }

   private static void edge(List<class_243> edges, double ax, double az, double bx, double bz) {
      edges.add(new class_243(ax, (double)63.0F, az));
      edges.add(new class_243(bx, (double)63.0F, bz));
   }

   @Environment(EnvType.CLIENT)
   static final class Cache {
      private static final int CAPACITY = 512;
      private final LinkedHashMap<Long, ChunkTileGeometry> tiles = new LinkedHashMap(64, 0.75F, true);
      private long frame;

      void beginFrame() {
         ++this.frame;
      }

      void clear() {
         this.tiles.clear();
      }

      int size() {
         return this.tiles.size();
      }

      ChunkTileGeometry get(long key, double x0, double z0, double x1, double z1, double radius, int mask) {
         ChunkTileGeometry tile = (ChunkTileGeometry)this.tiles.get(key);
         if (tile != null && tile.matches(x0, z0, x1, z1, radius, mask)) {
            tile.usedFrame = this.frame;
            return tile;
         } else {
            ChunkTileGeometry next = new ChunkTileGeometry(x0, z0, x1, z1, radius, mask);
            next.usedFrame = this.frame;
            if (tile == null && this.tiles.size() >= 512) {
               Iterator<Map.Entry<Long, ChunkTileGeometry>> oldest = this.tiles.entrySet().iterator();
               Map.Entry<Long, ChunkTileGeometry> entry = (Map.Entry)oldest.next();
               if (((ChunkTileGeometry)entry.getValue()).usedFrame < this.frame - 1L) {
                  oldest.remove();
                  this.tiles.put(key, next);
               }
            } else {
               this.tiles.put(key, next);
            }

            return next;
         }
      }
   }
}
