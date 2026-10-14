package net.anubis.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
final class OreSim {
   static final byte NO = 0;
   static final byte YES = 1;
   static final byte MAYBE = 2;
   static final byte CERTAIN = 1;
   static final byte POSSIBLE = 2;
   static final int MAX_STATES = Integer.getInteger("anubis.oreStates", 1024);
   static final int MAX_WORK = Integer.getInteger("anubis.oreWork", 30000);
   static final int MIN_Y = -64;
   static final int MAX_Y = 319;
   private static final int[][] SIDES = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

   private OreSim() {
   }

   static Chain run(OreSpec spec, int chunkX, int chunkZ, World world) {
      WgRandom random = new WgRandom();
      long decoration = random.setDecorationSeed(world.seed(), chunkX << 4, chunkZ << 4);
      random.setFeatureSeed(decoration, spec.index(), spec.step());
      Chain chain = new Chain(spec, chunkX, chunkZ);
      int count = spec.sampleCount(random);
      List<WgRandom> states = new ArrayList();
      states.add(random);
      Long2ByteOpenHashMap written = chain.writes;
      Long2ByteOpenHashMap mine = new Long2ByteOpenHashMap();
      int work = 0;

      for(int a = 0; a < count; ++a) {
         List<WgRandom> next = new ArrayList(states.size() + 2);
         Long2ByteOpenHashMap attemptWrites = new Long2ByteOpenHashMap();
         LongOpenHashSet everywhere = null;

         for(WgRandom state : states) {
            mine.clear();
            attempt(spec, chunkX, chunkZ, world, state, written, mine, next);
            LongOpenHashSet certainHere = new LongOpenHashSet();
            ObjectIterator<Long2ByteMap.Entry> it = mine.long2ByteEntrySet().fastIterator();

            while(it.hasNext()) {
               Long2ByteMap.Entry entry = (Long2ByteMap.Entry)it.next();
               attemptWrites.put(entry.getLongKey(), (byte)2);
               if (entry.getByteValue() == 1) {
                  certainHere.add(entry.getLongKey());
               }
            }

            if (everywhere == null) {
               everywhere = certainHere;
            } else {
               everywhere.retainAll(certainHere);
            }
         }

         ObjectIterator<Long2ByteMap.Entry> it = attemptWrites.long2ByteEntrySet().fastIterator();

         while(it.hasNext()) {
            long key = ((Long2ByteMap.Entry)it.next()).getLongKey();
            if (written.get(key) != 1) {
               written.put(key, (byte)2);
            }
         }

         if (everywhere != null) {
            LongIterator var23 = everywhere.iterator();

            while(var23.hasNext()) {
               long key = (Long)var23.next();
               written.put(key, (byte)1);
            }
         }

         work += states.size();
         states = distinct(next);
         if ((states.size() > MAX_STATES || work > MAX_WORK) && a + 1 < count) {
            chain.fogFrom = a + 1;
            break;
         }
      }

      return chain;
   }

   private static void attempt(OreSpec spec, int chunkX, int chunkZ, World world, WgRandom random, Long2ByteOpenHashMap written, Long2ByteOpenHashMap mine, List<WgRandom> next) {
      int x = random.nextInt(16) + (chunkX << 4);
      int z = random.nextInt(16) + (chunkZ << 4);
      int y = spec.sampleY(random);
      if (!world.inBiome(spec, x, y, z)) {
         next.add(random);
      } else {
         int size = spec.size();
         float f = random.nextFloat() * (float)Math.PI;
         float g = (float)size / 8.0F;
         int i = class_3532.method_15386(((float)size / 16.0F * 2.0F + 1.0F) / 2.0F);
         double d = (double)x + Math.sin((double)f) * (double)g;
         double e = (double)x - Math.sin((double)f) * (double)g;
         double h = (double)z + Math.cos((double)f) * (double)g;
         double j = (double)z - Math.cos((double)f) * (double)g;
         double l = (double)(y + random.nextInt(3) - 2);
         double m = (double)(y + random.nextInt(3) - 2);
         int n = x - class_3532.method_15386(g) - i;
         int o = y - 2 - i;
         int p = z - class_3532.method_15386(g) - i;
         int q = 2 * (class_3532.method_15386(g) + i);
         int r = 2 * (2 + i);
         byte gate = gate(world, o, n, p, q);
         if (gate == 0) {
            next.add(random);
         } else {
            if (gate == 2) {
               next.add(random.copy());
            }

            doPlace(spec, chunkX, chunkZ, world, random, written, mine, gate == 2, d, e, h, j, l, m, n, o, p, q, r, next);
         }
      }
   }

   private static byte gate(World world, int o, int n, int p, int q) {
      boolean maybe = false;

      for(int s = n; s <= n + q; ++s) {
         for(int t = p; t <= p + q; ++t) {
            if (o <= world.gateLow(s, t)) {
               return 1;
            }

            if (o <= world.gateHigh(s, t)) {
               maybe = true;
            }
         }
      }

      return (byte)(maybe ? 2 : 0);
   }

   private static void doPlace(OreSpec spec, int chunkX, int chunkZ, World world, WgRandom random, Long2ByteOpenHashMap written, Long2ByteOpenHashMap mine, boolean onlyIfPassed, double d, double e, double h, double j, double l, double m, int n, int o, int p, int q, int r, List<WgRandom> next) {
      int size = spec.size();
      double[] ds = new double[size * 4];

      for(int k = 0; k < size; ++k) {
         float t = (float)k / (float)size;
         double sx = class_3532.method_16436((double)t, d, e);
         double sy = class_3532.method_16436((double)t, l, m);
         double sz = class_3532.method_16436((double)t, h, j);
         double v = random.nextDouble() * (double)size / (double)16.0F;
         double w = ((double)(class_3532.method_15374((double)((float)Math.PI * t)) + 1.0F) * v + (double)1.0F) / (double)2.0F;
         ds[k * 4] = sx;
         ds[k * 4 + 1] = sy;
         ds[k * 4 + 2] = sz;
         ds[k * 4 + 3] = w;
      }

      for(int a = 0; a < size - 1; ++a) {
         if (!(ds[a * 4 + 3] <= (double)0.0F)) {
            for(int b = a + 1; b < size; ++b) {
               if (!(ds[b * 4 + 3] <= (double)0.0F)) {
                  double sx = ds[a * 4] - ds[b * 4];
                  double sy = ds[a * 4 + 1] - ds[b * 4 + 1];
                  double sz = ds[a * 4 + 2] - ds[b * 4 + 2];
                  double sr = ds[a * 4 + 3] - ds[b * 4 + 3];
                  if (sr * sr > sx * sx + sy * sy + sz * sz) {
                     if (sr > (double)0.0F) {
                        ds[b * 4 + 3] = (double)-1.0F;
                     } else {
                        ds[a * 4 + 3] = (double)-1.0F;
                     }
                  }
               }
            }
         }
      }

      boolean draws = spec.drawsOnTerrain();
      float discard = spec.discard();
      boolean baseKind = VBlock.isBase(spec.kind());
      WgRandom afterShape = draws ? random.copy() : null;
      int sure = 0;
      int open = 0;
      boolean diverged = false;
      BitSet bits = new BitSet(q * r * q);

      for(int k = 0; k < size; ++k) {
         double s = ds[k * 4 + 3];
         if (!(s < (double)0.0F)) {
            double t = ds[k * 4];
            double u = ds[k * 4 + 1];
            double v = ds[k * 4 + 2];
            int x0 = Math.max(class_3532.method_15357(t - s), n);
            int y0 = Math.max(class_3532.method_15357(u - s), o);
            int z0 = Math.max(class_3532.method_15357(v - s), p);
            int x1 = Math.max(class_3532.method_15357(t + s), x0);
            int y1 = Math.max(class_3532.method_15357(u + s), y0);
            int z1 = Math.max(class_3532.method_15357(v + s), z0);

            for(int ae = x0; ae <= x1; ++ae) {
               double af = ((double)ae + (double)0.5F - t) / s;
               if (!(af * af >= (double)1.0F)) {
                  for(int ag = y0; ag <= y1; ++ag) {
                     double ah = ((double)ag + (double)0.5F - u) / s;
                     if (!(af * af + ah * ah >= (double)1.0F)) {
                        for(int ai = z0; ai <= z1; ++ai) {
                           double aj = ((double)ai + (double)0.5F - v) / s;
                           if (!(af * af + ah * ah + aj * aj >= (double)1.0F) && ag >= -64 && ag <= 319) {
                              int ak = ae - n + (ag - o) * q + (ai - p) * q * r;
                              if (!bits.get(ak)) {
                                 bits.set(ak);
                                 if (Math.abs((ae >> 4) - chunkX) <= 1 && Math.abs((ai >> 4) - chunkZ) <= 1) {
                                    long key = class_2338.method_10064(ae, ag, ai);
                                    byte base = baseNow(world, written, baseKind, ae, ag, ai, key);
                                    if (base != 0) {
                                       if (base == 2) {
                                          if (draws) {
                                             ++open;
                                             diverged = true;
                                          }

                                          mine.put(key, (byte)2);
                                       } else {
                                          byte place;
                                          if (discard <= 0.0F) {
                                             place = 1;
                                          } else if (discard >= 1.0F) {
                                             place = clearOfAir(world, written, ae, ag, ai);
                                          } else {
                                             ++sure;
                                             if (!diverged) {
                                                place = random.nextFloat() >= discard ? 1 : clearOfAir(world, written, ae, ag, ai);
                                             } else {
                                                place = (byte)(clearOfAir(world, written, ae, ag, ai) == 1 ? 1 : 2);
                                             }
                                          }

                                          if (place == 1) {
                                             mine.put(key, (byte)(onlyIfPassed ? 2 : 1));
                                          } else if (place == 2) {
                                             mine.put(key, (byte)2);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (!draws) {
         next.add(random);
      } else {
         for(int k = 0; k <= open; ++k) {
            WgRandom state = k == open ? afterShape : afterShape.copy();
            state.skip(sure + k);
            next.add(state);
         }

      }
   }

   private static byte baseNow(World world, Long2ByteOpenHashMap written, boolean baseKind, int x, int y, int z, long key) {
      byte before = written.get(key);
      if (before != 0 && !baseKind) {
         return (byte)(before == 1 ? 0 : 2);
      } else {
         return world.base(x, y, z);
      }
   }

   private static byte clearOfAir(World world, Long2ByteOpenHashMap written, int x, int y, int z) {
      byte result = 1;

      for(int[] side : SIDES) {
         int nx = x + side[0];
         int ny = y + side[1];
         int nz = z + side[2];
         if (ny < -64 || ny > 319) {
            return 0;
         }

         if (written.get(class_2338.method_10064(nx, ny, nz)) != 1) {
            byte air = world.air(nx, ny, nz);
            if (air == 1) {
               return 0;
            }

            if (air == 2) {
               result = 2;
            }
         }
      }

      return result;
   }

   private static List<WgRandom> distinct(List<WgRandom> states) {
      if (states.size() <= 1) {
         return states;
      } else {
         Map<Long, List<WgRandom>> byHash = new HashMap();
         List<WgRandom> out = new ArrayList(states.size());

         for(WgRandom state : states) {
            List<WgRandom> same = (List)byHash.computeIfAbsent(state.stateHash(), (k) -> new ArrayList(1));
            boolean seen = false;

            for(WgRandom other : same) {
               if (other.sameState(state)) {
                  seen = true;
                  break;
               }
            }

            if (!seen) {
               same.add(state);
               out.add(state);
            }
         }

         return out;
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Chain {
      final OreSpec spec;
      final int chunkX;
      final int chunkZ;
      final Long2ByteOpenHashMap writes = new Long2ByteOpenHashMap();
      int fogFrom = -1;

      Chain(OreSpec spec, int chunkX, int chunkZ) {
         this.spec = spec;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
      }

      boolean fogged() {
         return this.fogFrom >= 0;
      }

      int[] fogBox() {
         int half = this.spec.halfWidth();
         int reach = this.spec.reach();
         int x0 = this.chunkX << 4;
         int z0 = this.chunkZ << 4;
         return new int[]{Math.max(x0 - half, x0 - 16), Math.max(Math.min(this.spec.yMin(), this.spec.yMax()) - 2 - reach, -64), Math.max(z0 - half, z0 - 16), Math.min(x0 + 15 + half, x0 + 31), Math.min(Math.max(this.spec.yMin(), this.spec.yMax()) + 2 + reach, 319), Math.min(z0 + 15 + half, z0 + 31)};
      }
   }

   @Environment(EnvType.CLIENT)
   interface World {
      long seed();

      int gateLow(int var1, int var2);

      int gateHigh(int var1, int var2);

      byte base(int var1, int var2, int var3);

      byte air(int var1, int var2, int var3);

      boolean inBiome(OreSpec var1, int var2, int var3, int var4);
   }
}
