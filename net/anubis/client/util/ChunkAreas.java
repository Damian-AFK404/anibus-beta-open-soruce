package net.anubis.client.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1923;
import net.minecraft.class_243;

@Environment(EnvType.CLIENT)
public final class ChunkAreas {
   private final DoubleArrayList areaX = new DoubleArrayList();
   private final DoubleArrayList areaZ = new DoubleArrayList();
   private int areasSize = -1;
   private int areasHash;
   private final LongOpenHashSet seen = new LongOpenHashSet();
   private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
   private final LongArrayList area = new LongArrayList();

   public void drawTracers(LongSet chunks, double y, int color) {
      if (!chunks.isEmpty()) {
         this.find(chunks);

         for(int i = 0; i < this.areaX.size(); ++i) {
            if (MobilePerformance.active()) {
               TracerEngine.draw(this.areaX.getDouble(i), y, this.areaZ.getDouble(i), color);
            } else {
               TracerEngine.draw(new class_243(this.areaX.getDouble(i), y, this.areaZ.getDouble(i)), color);
            }
         }

      }
   }

   private void find(LongSet chunks) {
      int hash = chunks.hashCode();
      if (chunks.size() != this.areasSize || hash != this.areasHash) {
         this.areasSize = chunks.size();
         this.areasHash = hash;
         this.areaX.clear();
         this.areaZ.clear();
         this.seen.clear();
         LongIterator var3 = chunks.iterator();

         while(var3.hasNext()) {
            long start = (Long)var3.next();
            if (this.seen.add(start)) {
               this.area.clear();
               this.queue.enqueue(start);
               double sumX = (double)0.0F;
               double sumZ = (double)0.0F;

               while(!this.queue.isEmpty()) {
                  long key = this.queue.dequeueLong();
                  this.area.add(key);
                  int cx = class_1923.method_8325(key);
                  int cz = class_1923.method_8332(key);
                  sumX += (double)(cx << 4) + (double)8.0F;
                  sumZ += (double)(cz << 4) + (double)8.0F;

                  for(int dx = -1; dx <= 1; ++dx) {
                     for(int dz = -1; dz <= 1; ++dz) {
                        long next = class_1923.method_8331(cx + dx, cz + dz);
                        if (chunks.contains(next) && this.seen.add(next)) {
                           this.queue.enqueue(next);
                        }
                     }
                  }
               }

               double mx = sumX / (double)this.area.size();
               double mz = sumZ / (double)this.area.size();
               if (!chunks.contains(class_1923.method_8331((int)Math.floor(mx) >> 4, (int)Math.floor(mz) >> 4))) {
                  double best = Double.MAX_VALUE;
                  double bx = mx;
                  double bz = mz;

                  for(int i = 0; i < this.area.size(); ++i) {
                     long key = this.area.getLong(i);
                     double x = (double)(class_1923.method_8325(key) << 4) + (double)8.0F;
                     double z = (double)(class_1923.method_8332(key) << 4) + (double)8.0F;
                     double d = (x - mx) * (x - mx) + (z - mz) * (z - mz);
                     if (d < best) {
                        best = d;
                        bx = x;
                        bz = z;
                     }
                  }

                  mx = bx;
                  mz = bz;
               }

               this.areaX.add(mx);
               this.areaZ.add(mz);
            }
         }

      }
   }
}
