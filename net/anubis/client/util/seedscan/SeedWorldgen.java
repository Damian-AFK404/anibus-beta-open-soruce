package net.anubis.client.util.seedscan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11897;
import net.minecraft.class_1923;
import net.minecraft.class_1936;
import net.minecraft.class_1959;
import net.minecraft.class_1972;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2359;
import net.minecraft.class_2370;
import net.minecraft.class_2378;
import net.minecraft.class_2791;
import net.minecraft.class_2806;
import net.minecraft.class_2839;
import net.minecraft.class_2843;
import net.minecraft.class_2919;
import net.minecraft.class_2922;
import net.minecraft.class_3754;
import net.minecraft.class_4543;
import net.minecraft.class_4766;
import net.minecraft.class_5138;
import net.minecraft.class_5284;
import net.minecraft.class_5285;
import net.minecraft.class_5321;
import net.minecraft.class_5455;
import net.minecraft.class_5463;
import net.minecraft.class_5469;
import net.minecraft.class_5485;
import net.minecraft.class_5504;
import net.minecraft.class_5539;
import net.minecraft.class_5742;
import net.minecraft.class_5820;
import net.minecraft.class_5868;
import net.minecraft.class_5873;
import net.minecraft.class_6350;
import net.minecraft.class_6563;
import net.minecraft.class_6568;
import net.minecraft.class_6643;
import net.minecraft.class_6673;
import net.minecraft.class_6736;
import net.minecraft.class_6748;
import net.minecraft.class_6749;
import net.minecraft.class_6796;
import net.minecraft.class_6803;
import net.minecraft.class_6817;
import net.minecraft.class_6832;
import net.minecraft.class_6880;
import net.minecraft.class_6954;
import net.minecraft.class_7138;
import net.minecraft.class_7225;
import net.minecraft.class_7510;
import net.minecraft.class_7877;
import net.minecraft.class_7923;
import net.minecraft.class_7924;
import net.minecraft.class_8197;
import net.minecraft.class_8198;

@Environment(EnvType.CLIENT)
public final class SeedWorldgen {
   public static final int MIN_Y = -64;
   public static final int HEIGHT = 384;
   public static final int MAX_Y = 319;
   public final long seed;
   final class_3754 generator;
   final class_7138 randomState;
   final class_4766 biomeSource;
   final class_5284 settings;
   private final List<OreSpec> specs;
   private final class_11897 containers;
   private final class_5138 noStructures;
   private final class_5539 height = class_5539.method_39034(-64, 384);
   private final long zoomSeed;
   private final Map<Long, class_5485> carverBiomes = new ConcurrentHashMap();
   private final Map<Long, short[]> storedBiomes = new ConcurrentHashMap();
   private final List<class_6880<class_1959>> biomeIds;
   private final Map<class_6880<class_1959>, Integer> biomeIndex = new IdentityHashMap();
   private static final int MIN_QUART = -16;
   private static final int QUARTS_Y = 96;
   private static final int MAX_STORED_BIOMES = 4096;
   private static final int MAX_CARVER_BIOMES = 65536;
   private final class_4543 biomeManager;
   private static final class_5455 CARVER_REGISTRIES;
   private static final Set<class_5321<class_1959>> TALL_TREES;

   private SeedWorldgen(long seed) {
      this.seed = seed;
      class_7225.class_7874 lookup = (new class_7877()).method_46777(class_7924.field_41238, class_5463::method_46843).method_46777(class_7924.field_41247, class_5469::method_46846).method_46777(class_7924.field_41239, class_6803::method_39702).method_46777(class_7924.field_41245, class_6817::method_39735).method_46777(class_7924.field_41236, class_5504::method_40363).method_46777(class_7924.field_43089, class_8198::method_49522).method_46777(class_7924.field_41244, class_6736::method_39216).method_46777(class_7924.field_41240, class_6954::method_41100).method_46777(class_7924.field_41243, class_5284::method_31111).method_46780(class_5455.method_40302(class_7923.field_41167));
      class_6880<class_5284> settingsHolder = lookup.method_46762(class_7924.field_41243).method_46747(class_5284.field_26355);
      this.settings = (class_5284)settingsHolder.comp_349();
      this.randomState = class_7138.method_41556(this.settings, lookup.method_46762(class_7924.field_41244), seed);
      class_6880<class_8197> parameters = lookup.method_46762(class_7924.field_43089).method_46747(class_8198.field_42992);
      this.biomeSource = class_4766.method_49503(parameters);
      this.generator = new class_3754(this.biomeSource, settingsHolder);
      List<class_6880<class_1959>> biomes = List.copyOf(this.biomeSource.method_28443());
      List<class_7510.class_6827> steps = class_7510.method_44210(biomes, (holder) -> this.generator.method_44216(holder).method_30983(), true);
      Map<class_6796, String> names = new IdentityHashMap();
      lookup.method_46762(class_7924.field_41245).method_42017().forEach((reference) -> names.put((class_6796)reference.comp_349(), reference.method_40237().method_29177().method_12832()));
      this.specs = OreSpec.derive(steps, biomes, this.generator, (placed) -> (String)names.getOrDefault(placed, "?"), -64, 319);
      class_6880<class_1959> plains = lookup.method_46762(class_7924.field_41236).method_46747(class_1972.field_9451);
      List<class_6880<class_1959>> ids = new ArrayList(biomes);
      if (!ids.contains(plains)) {
         ids.add(plains);
      }

      this.biomeIds = List.copyOf(ids);

      for(int i = 0; i < this.biomeIds.size(); ++i) {
         this.biomeIndex.put((class_6880)this.biomeIds.get(i), i);
      }

      this.containers = new class_11897(class_6563.method_74162(class_2248.field_10651), class_2246.field_10124.method_9564(), (Codec)null, class_6563.method_74165(listIds(ids)), plains, (Codec)null);
      this.noStructures = new class_5138(emptyLevel(this.containers, this.height), new class_5285(seed, false, false), (class_6832)null);
      this.zoomSeed = class_4543.method_27984(seed);
      this.biomeManager = new class_4543(this::storedBiome, this.zoomSeed);
   }

   public static SeedWorldgen create(long seed) {
      return new SeedWorldgen(seed);
   }

   public List<OreSpec> specs() {
      return this.specs;
   }

   public int treeAllowance(int chunkX, int chunkZ) {
      for(int dx = -1; dx <= 1; ++dx) {
         for(int dz = -1; dz <= 1; ++dz) {
            int x = (chunkX + dx << 4) + 8;
            int z = (chunkZ + dz << 4) + 8;

            for(int y = 60; y <= 130; y += 10) {
               Optional var10000 = this.biomeAt(x, y, z).method_40230();
               Set var10001 = TALL_TREES;
               Objects.requireNonNull(var10001);
               if (var10000.filter(var10001::contains).isPresent()) {
                  return GateHeights.TALL_TREE_ALLOWANCE;
               }
            }
         }
      }

      return GateHeights.TREE_ALLOWANCE;
   }

   public class_6880<class_1959> biomeAt(int x, int y, int z) {
      return this.biomeManager.method_22393(new class_2338(x, y, z));
   }

   class_6880<class_1959> storedBiome(int qx, int qy, int qz) {
      int clamped = Math.max(-16, Math.min(qy, 79));
      short[] quarts = this.storedBiomes(class_5742.method_33103(qx), class_5742.method_33103(qz));
      return (class_6880)this.biomeIds.get(quarts[((clamped - -16) * 4 + (qz & 3)) * 4 + (qx & 3)]);
   }

   private short[] storedBiomes(int chunkX, int chunkZ) {
      long key = class_1923.method_8331(chunkX, chunkZ);
      short[] quarts = (short[])this.storedBiomes.get(key);
      if (quarts != null) {
         return quarts;
      } else {
         class_2839 chunk = new class_2839(new class_1923(chunkX, chunkZ), class_2843.field_12950, this.height, this.containers, (class_6749)null);
         return this.fillBiomes(chunk);
      }
   }

   private short[] fillBiomes(class_2839 chunk) {
      this.generator.method_38275(this.randomState, class_6748.method_39336(), this.noStructures, chunk).join();
      chunk.method_12308(class_2806.field_12794);
      int x0 = class_5742.method_33100(chunk.method_12004().method_8326());
      int z0 = class_5742.method_33100(chunk.method_12004().method_8328());
      short[] quarts = new short[1536];

      for(int qy = 0; qy < 96; ++qy) {
         for(int qz = 0; qz < 4; ++qz) {
            for(int qx = 0; qx < 4; ++qx) {
               Integer id = (Integer)this.biomeIndex.get(chunk.method_16359(x0 + qx, -16 + qy, z0 + qz));
               quarts[(qy * 4 + qz) * 4 + qx] = (short)(id == null ? 0 : id);
            }
         }
      }

      if (this.storedBiomes.size() > 4096) {
         this.storedBiomes.clear();
      }

      this.storedBiomes.put(chunk.method_12004().method_8324(), quarts);
      return quarts;
   }

   public class_2839 generateTerrain(int chunkX, int chunkZ) {
      class_2839 chunk = new class_2839(new class_1923(chunkX, chunkZ), class_2843.field_12950, this.height, this.containers, (class_6749)null);
      this.fillBiomes(chunk);
      this.generator.method_12088(class_6748.method_39336(), this.randomState, this.noStructures, chunk).join();
      chunk.method_12308(class_2806.field_12804);
      this.generator.method_41538(chunk, new class_5868(this.generator, chunk), this.randomState, this.noStructures, this.biomeManager, (class_2378)null, class_6748.method_39336());
      chunk.method_12308(class_2806.field_12796);
      this.carve(chunk);
      chunk.method_12308(class_2806.field_12801);
      return chunk;
   }

   public TerrainChunk terrain(int chunkX, int chunkZ) {
      return TerrainChunk.of(this.generateTerrain(chunkX, chunkZ));
   }

   void carve(class_2839 chunk) {
      if (this.carverBiomes.size() > 65536) {
         this.carverBiomes.clear();
      }

      class_4543 carverBiomeManager = new class_4543((x, y, z) -> this.biomeSource.method_38109(x, y, z, this.randomState.method_42371()), this.zoomSeed);
      class_2919 random = new class_2919(new class_5820(class_6673.method_39001()));
      class_1923 pos = chunk.method_12004();
      class_6568 noise = chunk.method_38255((c) -> {
         throw new IllegalStateException("carving before the noise fill");
      });
      class_6350 aquifer = noise.method_38354();
      class_5873 context = new class_5873(this.generator, CARVER_REGISTRIES, chunk.method_39460(), noise, this.randomState, this.settings.comp_478());
      class_6643 mask = chunk.method_28510();

      for(int dx = -8; dx <= 8; ++dx) {
         for(int dz = -8; dz <= 8; ++dz) {
            class_1923 from = new class_1923(pos.field_9181 + dx, pos.field_9180 + dz);
            class_5485 biome = (class_5485)this.carverBiomes.computeIfAbsent(from.method_8324(), (k) -> this.generator.method_44216(this.biomeSource.method_38109(class_5742.method_33100(from.method_8326()), 0, class_5742.method_33100(from.method_8328()), this.randomState.method_42371())));
            int index = 0;

            for(class_6880<class_2922<?>> holder : biome.method_30976()) {
               random.method_12663(this.seed + (long)index, from.field_9181, from.field_9180);
               if (((class_2922)holder.comp_349()).method_12669(random)) {
                  class_2922 var10000 = (class_2922)holder.comp_349();
                  Objects.requireNonNull(carverBiomeManager);
                  var10000.method_12668(context, chunk, carverBiomeManager::method_22393, random, aquifer, from, mask);
               }

               ++index;
            }
         }
      }

   }

   private static class_2359<class_6880<class_1959>> listIds(final List<class_6880<class_1959>> list) {
      final Map<class_6880<class_1959>, Integer> ids = new IdentityHashMap();

      for(int i = 0; i < list.size(); ++i) {
         ids.put((class_6880)list.get(i), i);
      }

      return new class_2359<class_6880<class_1959>>() {
         public int getId(class_6880<class_1959> biome) {
            Integer id = (Integer)ids.get(biome);
            return id == null ? -1 : id;
         }

         public class_6880<class_1959> byId(int id) {
            return id >= 0 && id < list.size() ? (class_6880)list.get(id) : null;
         }

         public int method_10204() {
            return list.size();
         }

         public Iterator<class_6880<class_1959>> iterator() {
            return list.iterator();
         }
      };
   }

   private static class_1936 emptyLevel(class_11897 containers, class_5539 height) {
      class_2839 empty = new class_2839(new class_1923(0, 0), class_2843.field_12950, height, containers, (class_6749)null);
      InvocationHandler handler = (proxy, method, args) -> {
         if (method.getDeclaringClass() == Object.class) {
            Object var10000;
            switch (method.getName()) {
               case "equals" -> var10000 = proxy == args[0];
               case "hashCode" -> var10000 = System.identityHashCode(proxy);
               default -> var10000 = "SeedWorldgen.emptyLevel";
            }

            return var10000;
         } else if (class_2791.class.isAssignableFrom(method.getReturnType()) && method.getParameterCount() == 4) {
            return empty;
         } else if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
         } else {
            throw new UnsupportedOperationException("structure-free level: " + String.valueOf(method));
         }
      };
      return (class_1936)Proxy.newProxyInstance(class_1936.class.getClassLoader(), new Class[]{class_1936.class}, handler);
   }

   static {
      CARVER_REGISTRIES = new class_5455.class_6891(List.of((new class_2370(class_7924.field_41236, Lifecycle.stable())).method_40276()));
      TALL_TREES = Set.of(class_1972.field_9417, class_1972.field_9440, class_1972.field_35118, class_1972.field_35119, class_1972.field_35113, class_1972.field_38748);
   }
}
