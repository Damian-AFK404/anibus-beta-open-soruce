package net.anubis.client.esp;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HorizontalCandidates<T> {
   private final ToDoubleFunction<T> x;
   private final ToDoubleFunction<T> z;
   private final Long2ObjectOpenHashMap<IntArrayList> regions = new Long2ObjectOpenHashMap();
   private final IntArrayList selected = new IntArrayList();
   private final ArrayList<T> candidates = new ArrayList();
   private List<T> indexed;
   private int cameraX;
   private int cameraZ;
   private double range;
   private boolean ready;

   public HorizontalCandidates(ToDoubleFunction<T> x, ToDoubleFunction<T> z) {
      this.x = x;
      this.z = z;
   }

   public List<T> select(List<T> source, double eyeX, double eyeZ, double radius) {
      if (source.size() >= 64 && Double.isFinite(eyeX) && Double.isFinite(eyeZ) && Double.isFinite(radius) && !(radius < (double)0.0F) && !(radius > (double)1000000.0F)) {
         if (this.indexed != source) {
            if (this.indexed != null && source.size() < this.indexed.size() / 4) {
               this.clear();
            }

            this.regions.clear();

            for(int i = 0; i < source.size(); ++i) {
               T point = (T)source.get(i);
               ((IntArrayList)this.regions.computeIfAbsent(key(region(this.x.applyAsDouble(point)), region(this.z.applyAsDouble(point))), (ignored) -> new IntArrayList())).add(i);
            }

            this.indexed = source;
            this.ready = false;
         }

         int cx = (int)Math.floor(eyeX / (double)16.0F);
         int cz = (int)Math.floor(eyeZ / (double)16.0F);
         if (this.ready && this.cameraX == cx && this.cameraZ == cz && this.range == radius) {
            return this.candidates;
         } else {
            this.selected.clear();
            this.candidates.clear();
            int minX = region((double)cx * (double)16.0F - radius);
            int maxX = region(((double)cx + (double)1.0F) * (double)16.0F + radius);
            int minZ = region((double)cz * (double)16.0F - radius);
            int maxZ = region(((double)cz + (double)1.0F) * (double)16.0F + radius);

            for(int rx = minX; rx <= maxX; ++rx) {
               for(int rz = minZ; rz <= maxZ; ++rz) {
                  IntArrayList group = (IntArrayList)this.regions.get(key(rx, rz));
                  if (group != null) {
                     this.selected.addAll(group);
                  }
               }
            }

            IntArrays.quickSort(this.selected.elements(), 0, this.selected.size());

            for(int i = 0; i < this.selected.size(); ++i) {
               this.candidates.add(source.get(this.selected.getInt(i)));
            }

            this.cameraX = cx;
            this.cameraZ = cz;
            this.range = radius;
            this.ready = true;
            return this.candidates;
         }
      } else {
         if (this.indexed != null) {
            this.clear();
         }

         return source;
      }
   }

   public void clear() {
      this.indexed = null;
      this.regions.clear();
      this.regions.trim();
      this.selected.clear();
      this.selected.trim();
      this.candidates.clear();
      this.candidates.trimToSize();
      this.ready = false;
   }

   private static int region(double coordinate) {
      return (int)Math.floor(coordinate / (double)256.0F);
   }

   private static long key(int x, int z) {
      return (long)x & 4294967295L | (long)z << 32;
   }
}
