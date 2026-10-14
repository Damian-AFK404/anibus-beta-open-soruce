package net.anubis.client.module.impl.render.character.gltf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class MeshReducer {
   private final float[] pos;
   private final int[] tri;
   private final boolean[] dead;
   private final int[] corner;
   private final List<List<Integer>> around = new ArrayList();
   private final boolean[] locked;
   private final double[] quad;
   private final int[] version;
   private int live;

   MeshReducer(PosedMesh s, List<Integer> source, boolean weld) {
      Map<Long, Integer> index = new HashMap();
      Map<String, Integer> welded = new HashMap();
      List<Integer> order = new ArrayList();
      this.tri = new int[source.size() * 3];

      for(int i = 0; i < source.size(); ++i) {
         int t = (Integer)source.get(i);
         boolean back = t < 0;
         int real = back ? ~t : t;

         for(int c = 0; c < 3; ++c) {
            int v = s.tris[real * 3 + (back ? 2 - c : c)];
            int key = back ? ~v : v;
            Integer id = (Integer)index.get((long)key);
            if (id == null && weld) {
               int var10000 = Float.floatToIntBits(s.pos[v * 3]);
               String spot = var10000 + "," + Float.floatToIntBits(s.pos[v * 3 + 1]) + "," + Float.floatToIntBits(s.pos[v * 3 + 2]) + "," + Float.floatToIntBits(s.uv[v * 2]) + "," + Float.floatToIntBits(s.uv[v * 2 + 1]) + "," + s.material[v] + (back ? "b" : "");
               id = (Integer)welded.get(spot);
               if (id == null) {
                  welded.put(spot, order.size());
               }

               if (id != null) {
                  index.put((long)key, id);
               }
            }

            if (id == null) {
               id = order.size();
               index.put((long)key, id);
               order.add(key);
            }

            this.tri[i * 3 + c] = id;
         }
      }

      int n = order.size();
      this.corner = new int[n];
      this.pos = new float[n * 3];

      for(int i = 0; i < n; ++i) {
         this.corner[i] = (Integer)order.get(i);
         int v = this.corner[i] < 0 ? ~this.corner[i] : this.corner[i];
         System.arraycopy(s.pos, v * 3, this.pos, i * 3, 3);
      }

      this.dead = new boolean[source.size()];
      this.live = source.size();

      for(int i = 0; i < n; ++i) {
         this.around.add(new ArrayList());
      }

      for(int t = 0; t < source.size(); ++t) {
         int a = this.tri[t * 3];
         int b = this.tri[t * 3 + 1];
         int c = this.tri[t * 3 + 2];
         if (a != b && b != c && a != c) {
            for(int k = 0; k < 3; ++k) {
               ((List)this.around.get(this.tri[t * 3 + k])).add(t);
            }
         } else {
            this.dead[t] = true;
            --this.live;
         }
      }

      this.locked = new boolean[n];
      Map<Long, Integer> edges = new HashMap();

      for(int t = 0; t < source.size(); ++t) {
         if (!this.dead[t]) {
            for(int c = 0; c < 3; ++c) {
               edges.merge(key(this.tri[t * 3 + c], this.tri[t * 3 + (c + 1) % 3]), 1, Integer::sum);
            }
         }
      }

      for(Map.Entry<Long, Integer> e : edges.entrySet()) {
         if ((Integer)e.getValue() != 2) {
            this.locked[(int)((Long)e.getKey() >> 32)] = true;
            this.locked[(int)((Long)e.getKey() & 4294967295L)] = true;
         }
      }

      this.quad = new double[n * 10];

      for(int t = 0; t < source.size(); ++t) {
         if (!this.dead[t]) {
            int a = this.tri[t * 3];
            int b = this.tri[t * 3 + 1];
            int c = this.tri[t * 3 + 2];
            float[] f = Mat4.faceNormal(this.pos, a, b, c);
            double len = Math.sqrt((double)(f[0] * f[0] + f[1] * f[1] + f[2] * f[2]));
            if (!(len < 1.0E-12)) {
               double nx = (double)f[0] / len;
               double ny = (double)f[1] / len;
               double nz = (double)f[2] / len;
               double d = -(nx * (double)this.pos[a * 3] + ny * (double)this.pos[a * 3 + 1] + nz * (double)this.pos[a * 3 + 2]);

               for(int v : new int[]{a, b, c}) {
                  this.addPlane(v, nx, ny, nz, d, len * (double)0.5F);
               }
            }
         }
      }

      this.version = new int[n];
   }

   private static long key(int a, int b) {
      return (long)Math.min(a, b) << 32 | (long)Math.max(a, b);
   }

   private void addPlane(int v, double a, double b, double c, double d, double w) {
      int o = v * 10;
      double[] var10000 = this.quad;
      var10000[o] += w * a * a;
      var10000 = this.quad;
      var10000[o + 1] += w * a * b;
      var10000 = this.quad;
      var10000[o + 2] += w * a * c;
      var10000 = this.quad;
      var10000[o + 3] += w * a * d;
      var10000 = this.quad;
      var10000[o + 4] += w * b * b;
      var10000 = this.quad;
      var10000[o + 5] += w * b * c;
      var10000 = this.quad;
      var10000[o + 6] += w * b * d;
      var10000 = this.quad;
      var10000[o + 7] += w * c * c;
      var10000 = this.quad;
      var10000[o + 8] += w * c * d;
      var10000 = this.quad;
      var10000[o + 9] += w * d * d;
   }

   private double error(int v, double x, double y, double z) {
      double[] q = this.quad;
      int o = v * 10;
      return q[o] * x * x + (double)2.0F * q[o + 1] * x * y + (double)2.0F * q[o + 2] * x * z + (double)2.0F * q[o + 3] * x + q[o + 4] * y * y + (double)2.0F * q[o + 5] * y * z + (double)2.0F * q[o + 6] * y + q[o + 7] * z * z + (double)2.0F * q[o + 8] * z + q[o + 9];
   }

   int[][] reduce(int[] targets) {
      PriorityQueue<Move> heap = new PriorityQueue((p, q) -> Double.compare(p.cost, q.cost));

      for(int v = 0; v < this.locked.length; ++v) {
         this.push(heap, v);
      }

      int[][] out = new int[targets.length][];
      Integer[] order = new Integer[targets.length];

      for(int i = 0; i < order.length; ++i) {
         order[i] = i;
      }

      Arrays.sort(order, (a, b) -> Integer.compare(targets[b], targets[a]));
      Integer[] var15 = order;
      int var6 = order.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         int i = var15[var7];

         while(this.live > targets[i] && !heap.isEmpty()) {
            Move m = (Move)heap.poll();
            if (this.version[m.from] == m.vf && this.version[m.to] == m.vt && !this.locked[m.from] && this.collapse(m.from, m.to)) {
               Set<Integer> touched = new HashSet();
               touched.add(m.to);

               for(int t : (List)this.around.get(m.to)) {
                  if (!this.dead[t]) {
                     for(int c = 0; c < 3; ++c) {
                        touched.add(this.tri[t * 3 + c]);
                     }
                  }
               }

               for(int v : touched) {
                  this.push(heap, v);
               }
            }
         }

         out[i] = this.snapshot();
      }

      return out;
   }

   private void push(PriorityQueue<Move> heap, int v) {
      if (!this.locked[v]) {
         for(int n : this.neighbours(v)) {
            double dx = (double)(this.pos[n * 3] - this.pos[v * 3]);
            double dy = (double)(this.pos[n * 3 + 1] - this.pos[v * 3 + 1]);
            double dz = (double)(this.pos[n * 3 + 2] - this.pos[v * 3 + 2]);
            double cost = this.error(v, (double)this.pos[n * 3], (double)this.pos[n * 3 + 1], (double)this.pos[n * 3 + 2]) + 1.0E-4 * (dx * dx + dy * dy + dz * dz);
            heap.add(new Move(cost, v, n, this.version[v], this.version[n]));
         }

      }
   }

   private List<Integer> neighbours(int v) {
      List<Integer> out = new ArrayList();

      for(int t : (List)this.around.get(v)) {
         if (!this.dead[t]) {
            for(int c = 0; c < 3; ++c) {
               int w = this.tri[t * 3 + c];
               if (w != v && !out.contains(w)) {
                  out.add(w);
               }
            }
         }
      }

      return out;
   }

   private boolean collapse(int u, int v) {
      List<Integer> nu = this.neighbours(u);
      List<Integer> nv = this.neighbours(v);
      if (!nu.contains(v)) {
         return false;
      } else {
         int shared = 0;
         int common = 0;

         for(int t : (List)this.around.get(u)) {
            if (!this.dead[t] && this.has(t, v)) {
               ++shared;
            }
         }

         for(int w : nu) {
            if (nv.contains(w)) {
               ++common;
            }
         }

         if (common != shared) {
            return false;
         } else {
            for(int t : (List)this.around.get(u)) {
               if (!this.dead[t] && !this.has(t, v)) {
                  float[] before = Mat4.faceNormal(this.pos, this.tri[t * 3], this.tri[t * 3 + 1], this.tri[t * 3 + 2]);
                  int[] moved = new int[]{this.tri[t * 3], this.tri[t * 3 + 1], this.tri[t * 3 + 2]};

                  for(int c = 0; c < 3; ++c) {
                     if (moved[c] == u) {
                        moved[c] = v;
                     }
                  }

                  float[] after = Mat4.faceNormal(this.pos, moved[0], moved[1], moved[2]);
                  double lb = Math.sqrt((double)(before[0] * before[0] + before[1] * before[1] + before[2] * before[2]));
                  double la = Math.sqrt((double)(after[0] * after[0] + after[1] * after[1] + after[2] * after[2]));
                  if (la < 1.0E-12 || lb < 1.0E-12) {
                     return false;
                  }

                  if ((double)(before[0] * after[0] + before[1] * after[1] + before[2] * after[2]) / (la * lb) < (double)0.5F) {
                     return false;
                  }
               }
            }

            for(int t : (List)this.around.get(u)) {
               if (!this.dead[t]) {
                  if (this.has(t, v)) {
                     this.dead[t] = true;
                     --this.live;
                  } else {
                     for(int c = 0; c < 3; ++c) {
                        if (this.tri[t * 3 + c] == u) {
                           this.tri[t * 3 + c] = v;
                        }
                     }

                     ((List)this.around.get(v)).add(t);
                  }
               }
            }

            ((List)this.around.get(u)).clear();

            for(int k = 0; k < 10; ++k) {
               double[] var10000 = this.quad;
               var10000[v * 10 + k] += this.quad[u * 10 + k];
            }

            int var10002 = this.version[u]++;
            var10002 = this.version[v]++;
            this.locked[u] = true;
            return true;
         }
      }
   }

   private boolean has(int t, int v) {
      return this.tri[t * 3] == v || this.tri[t * 3 + 1] == v || this.tri[t * 3 + 2] == v;
   }

   private int[] snapshot() {
      int[] out = new int[this.live * 3];
      int k = 0;

      for(int t = 0; t < this.dead.length; ++t) {
         if (!this.dead[t]) {
            for(int c = 0; c < 3; ++c) {
               out[k++] = this.corner[this.tri[t * 3 + c]];
            }
         }
      }

      return Arrays.copyOf(out, k);
   }

   @Environment(EnvType.CLIENT)
   private static record Move(double cost, int from, int to, int vf, int vt) {
   }
}
