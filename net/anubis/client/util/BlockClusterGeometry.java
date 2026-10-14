package net.anubis.client.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_243;

@Environment(EnvType.CLIENT)
public final class BlockClusterGeometry {
   private static final int[][] AXES = new int[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

   private BlockClusterGeometry() {
   }

   public static Mesh build(Collection<class_2338> blocks, Membership same) {
      List<int[]> unitEdges = new ArrayList(blocks.size() * 4);
      List<Face> faces = new ArrayList(blocks.size() * 3);

      for(class_2338 pos : blocks) {
         int[] p = new int[]{pos.method_10263(), pos.method_10264(), pos.method_10260()};
         addFaces(p, same, faces);
         addEdges(p, same, unitEdges);
      }

      return new Mesh(mergeCollinear(unitEdges), faces);
   }

   private static List<Segment> mergeCollinear(List<int[]> unitEdges) {
      Map<List<Integer>, List<Integer>> lines = new HashMap();

      for(int[] e : unitEdges) {
         int axis = e[0];
         int u = (axis + 1) % 3;
         int v = (axis + 2) % 3;
         ((List)lines.computeIfAbsent(List.of(axis, e[1 + u], e[1 + v]), (k) -> new ArrayList())).add(e[1 + axis]);
      }

      List<Segment> merged = new ArrayList(unitEdges.size());

      for(Map.Entry<List<Integer>, List<Integer>> line : lines.entrySet()) {
         int axis = (Integer)((List)line.getKey()).get(0);
         int u = (axis + 1) % 3;
         int v = (axis + 2) % 3;
         List<Integer> starts = (List)line.getValue();
         starts.sort((Comparator)null);
         int runStart = (Integer)starts.get(0);
         int runEnd = runStart + 1;

         for(int i = 1; i <= starts.size(); ++i) {
            if (i < starts.size() && (Integer)starts.get(i) == runEnd) {
               ++runEnd;
            } else {
               int[] a = new int[3];
               int[] b = new int[3];
               a[u] = b[u] = (Integer)((List)line.getKey()).get(1);
               a[v] = b[v] = (Integer)((List)line.getKey()).get(2);
               a[axis] = runStart;
               b[axis] = runEnd;
               merged.add(new Segment(vec(a), vec(b)));
               if (i < starts.size()) {
                  runStart = (Integer)starts.get(i);
                  runEnd = runStart + 1;
               }
            }
         }
      }

      return merged;
   }

   public static List<List<class_2338>> connectedComponents(Collection<class_2338> blocks) {
      Set<class_2338> remaining = new HashSet(blocks);
      List<List<class_2338>> components = new ArrayList();
      ArrayDeque<class_2338> queue = new ArrayDeque();

      while(!remaining.isEmpty()) {
         class_2338 seed = (class_2338)remaining.iterator().next();
         remaining.remove(seed);
         List<class_2338> component = new ArrayList();
         queue.add(seed);

         while(!queue.isEmpty()) {
            class_2338 current = (class_2338)queue.poll();
            component.add(current);

            for(int[] axis : AXES) {
               for(int sign = -1; sign <= 1; sign += 2) {
                  class_2338 next = current.method_10069(axis[0] * sign, axis[1] * sign, axis[2] * sign);
                  if (remaining.remove(next)) {
                     queue.add(next);
                  }
               }
            }
         }

         components.add(component);
      }

      return components;
   }

   private static void addFaces(int[] p, Membership same, List<Face> out) {
      for(int axis = 0; axis < 3; ++axis) {
         int u = (axis + 1) % 3;
         int v = (axis + 2) % 3;

         for(int sign = -1; sign <= 1; sign += 2) {
            if (!same.contains(p[0] + AXES[axis][0] * sign, p[1] + AXES[axis][1] * sign, p[2] + AXES[axis][2] * sign)) {
               int[] base = (int[])(([I)p).clone();
               if (sign > 0) {
                  int var10002 = base[axis]++;
               }

               out.add(new Face(corner(base, u, 0, v, 0), corner(base, u, 1, v, 0), corner(base, u, 1, v, 1), corner(base, u, 0, v, 1)));
            }
         }
      }

   }

   private static void addEdges(int[] p, Membership same, List<int[]> out) {
      for(int axis = 0; axis < 3; ++axis) {
         int u = (axis + 1) % 3;
         int v = (axis + 2) % 3;

         for(int su = -1; su <= 1; su += 2) {
            for(int sv = -1; sv <= 1; sv += 2) {
               boolean nu = contains(same, p, u, su, -1, 0);
               boolean nv = contains(same, p, v, sv, -1, 0);
               boolean diagonal = contains(same, p, u, su, v, sv);
               boolean convex = !nu && !nv;
               boolean concave = nu && nv && !diagonal;
               if (convex || concave) {
                  int[] start = (int[])(([I)p).clone();
                  if (su > 0) {
                     int var10002 = start[u]++;
                  }

                  if (sv > 0) {
                     int var14 = start[v]++;
                  }

                  out.add(new int[]{axis, start[0], start[1], start[2]});
               }
            }
         }
      }

   }

   private static boolean contains(Membership same, int[] p, int axisA, int signA, int axisB, int signB) {
      int[] q = (int[])(([I)p).clone();
      q[axisA] += signA;
      if (axisB >= 0) {
         q[axisB] += signB;
      }

      return same.contains(q[0], q[1], q[2]);
   }

   private static class_243 corner(int[] base, int u, int du, int v, int dv) {
      int[] q = (int[])(([I)base).clone();
      q[u] += du;
      q[v] += dv;
      return vec(q);
   }

   private static class_243 vec(int[] p) {
      return new class_243((double)p[0], (double)p[1], (double)p[2]);
   }

   @Environment(EnvType.CLIENT)
   public static record Segment(class_243 a, class_243 b) {
   }

   @Environment(EnvType.CLIENT)
   public static record Face(class_243 a, class_243 b, class_243 c, class_243 d) {
   }

   @Environment(EnvType.CLIENT)
   public static record Mesh(List<Segment> edges, List<Face> faces) {
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Membership {
      boolean contains(int var1, int var2, int var3);
   }
}
