package net.anubis.client.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.pack.ModPackResources;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModDependency.Kind;
import net.minecraft.class_2477;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_3262;
import net.minecraft.class_3298;
import net.minecraft.class_5348;
import net.minecraft.class_5481;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class TranslationGuard {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final String MOD_ID = "anubisplus";
   private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);
   private static final ThreadLocal<Load> LOAD = new ThreadLocal();
   private static volatile int activeCount;
   private static volatile Set<String> hidden;
   private static volatile Set<String> ourMods;
   private static volatile Snapshot snapshot;
   private static class_2477 wrapped;
   private static Set<String> guardedKeys;
   private static class_2477 guarded;

   private TranslationGuard() {
   }

   public static boolean active() {
      return activeCount > 0 && ((int[])DEPTH.get())[0] > 0;
   }

   public static String plain(class_2561 component) {
      enter();

      String var1;
      try {
         var1 = component.getString();
      } finally {
         exit();
      }

      return var1;
   }

   public static void run(Runnable action) {
      enter();

      try {
         action.run();
      } finally {
         exit();
      }

   }

   public static synchronized class_2477 language(class_2477 real) {
      if (real == null) {
         return null;
      } else {
         Set<String> keys = hidden();
         if (real != wrapped || keys != guardedKeys) {
            wrapped = real;
            guardedKeys = keys;
            guarded = new GuardedLanguage(real, keys);
         }

         return guarded;
      }
   }

   public static class_2561 keybind(String name) {
      class_304 mapping = class_304.method_65807(name);
      return (class_2561)(mapping != null && !hidden().contains(name) ? mapping.method_16007() : class_2561.method_43471(name));
   }

   public static void beginLoad() {
      LOAD.set(new Load());
   }

   public static BiConsumer<String, String> tap(class_3298 resource, BiConsumer<String, String> output) {
      Load load = (Load)LOAD.get();
      if (load == null) {
         return output;
      } else {
         load.seen = true;
         return ours(resource) ? (key, value) -> {
            output.accept(key, value);
            load.provided.add(key);
         } : (key, value) -> {
            output.accept(key, value);
            load.clean.put(key, value);
         };
      }
   }

   public static void endLoad() {
      Load load = (Load)LOAD.get();
      LOAD.remove();
      if (load != null && load.seen) {
         load.clean.keySet().retainAll(load.provided);
         snapshot = new Snapshot(Map.copyOf(load.clean), Set.copyOf(load.provided));
      }
   }

   private static void enter() {
      int[] depth = (int[])DEPTH.get();
      int var10003 = depth[0];
      int var10000 = depth[0];
      depth[0] = var10003 + 1;
      if (var10000 == 0) {
         synchronized(TranslationGuard.class) {
            ++activeCount;
         }
      }

   }

   private static void exit() {
      int[] depth = (int[])DEPTH.get();
      if (depth[0] > 0 && --depth[0] == 0) {
         synchronized(TranslationGuard.class) {
            --activeCount;
         }
      }

   }

   private static boolean ours(class_3298 resource) {
      class_3262 var2 = resource.method_45304();
      if (var2 instanceof ModPackResources pack) {
         return ours(pack.getFabricModMetadata());
      } else {
         return ourMods().contains(resource.method_14480());
      }
   }

   private static Set<String> ourMods() {
      Set<String> ids = ourMods;
      if (ids != null) {
         return ids;
      } else {
         Set<String> collected = new HashSet();

         for(ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if (ours(mod.getMetadata())) {
               collected.add(mod.getMetadata().getId());
            }
         }

         ids = Set.copyOf(collected);
         ourMods = ids;
         return ids;
      }
   }

   private static Set<String> hidden() {
      Set<String> keys = hidden;
      if (keys != null) {
         return keys;
      } else {
         synchronized(TranslationGuard.class) {
            if (hidden != null) {
               return hidden;
            } else {
               Set<String> collected = new HashSet();
               Set<String> namespaces = new HashSet();
               namespaces.add("anubis");

               for(ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                  if (ours(mod.getMetadata())) {
                     collect(mod, collected, namespaces);
                  }
               }

               class_315 options = class_310.method_1551().field_1690;
               if (options != null) {
                  for(class_304 mapping : options.field_1839) {
                     class_2960 category = mapping.method_1423().comp_4879();
                     if (namespaces.contains(category.method_12836())) {
                        collected.add(mapping.method_1431());
                        String var10001 = category.method_12836();
                        collected.add("key.category." + var10001 + "." + category.method_12832());
                     }
                  }
               }

               Set<String> result = Set.copyOf(collected);
               if (options != null) {
                  hidden = result;
               }

               return result;
            }
         }
      }
   }

   private static boolean ours(ModMetadata metadata) {
      if ("anubisplus".equals(metadata.getId())) {
         return true;
      } else {
         for(ModDependency dependency : metadata.getDependencies()) {
            if (dependency.getKind() == Kind.DEPENDS && "anubisplus".equals(dependency.getModId())) {
               return true;
            }
         }

         return false;
      }
   }

   private static void collect(ModContainer mod, Set<String> keys, Set<String> namespaces) {
      for(Path root : mod.getRootPaths()) {
         Path assets = root.resolve("assets");
         if (Files.isDirectory(assets, new LinkOption[0])) {
            for(Path space : list(assets)) {
               if (Files.isDirectory(space, new LinkOption[0])) {
                  namespaces.add(space.getFileName().toString().replace("/", ""));
                  Path lang = space.resolve("lang");
                  if (Files.isDirectory(lang, new LinkOption[0])) {
                     for(Path file : list(lang)) {
                        if (file.getFileName().toString().endsWith(".json")) {
                           try {
                              Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);

                              try {
                                 JsonElement json = JsonParser.parseReader(reader);
                                 if (json.isJsonObject()) {
                                    keys.addAll(json.getAsJsonObject().keySet());
                                 }
                              } catch (Throwable var15) {
                                 if (reader != null) {
                                    try {
                                       reader.close();
                                    } catch (Throwable var14) {
                                       var15.addSuppressed(var14);
                                    }
                                 }

                                 throw var15;
                              }

                              if (reader != null) {
                                 reader.close();
                              }
                           } catch (RuntimeException | IOException e) {
                              LOGGER.warn("Could not read {}", file, e);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private static List<Path> list(Path directory) {
      try {
         Stream<Path> entries = Files.list(directory);

         List var2;
         try {
            var2 = entries.toList();
         } catch (Throwable var5) {
            if (entries != null) {
               try {
                  entries.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (entries != null) {
            entries.close();
         }

         return var2;
      } catch (IOException e) {
         LOGGER.warn("Could not list {}", directory, e);
         return List.of();
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Load {
      private final Map<String, String> clean = new HashMap();
      private final Set<String> provided = new HashSet();
      private boolean seen;
   }

   @Environment(EnvType.CLIENT)
   private static record Snapshot(Map<String, String> clean, Set<String> provided) {
   }

   @Environment(EnvType.CLIENT)
   private static final class GuardedLanguage extends class_2477 {
      private final class_2477 real;
      private final Set<String> hidden;

      GuardedLanguage(class_2477 real, Set<String> hidden) {
         this.real = real;
         this.hidden = hidden;
      }

      public String method_4679(String key, String fallback) {
         Snapshot current = TranslationGuard.snapshot;
         if (current != null) {
            return current.provided().contains(key) ? (String)current.clean().getOrDefault(key, fallback) : this.real.method_4679(key, fallback);
         } else {
            return this.hidden.contains(key) ? fallback : this.real.method_4679(key, fallback);
         }
      }

      public boolean method_4678(String key) {
         Snapshot current = TranslationGuard.snapshot;
         if (current != null) {
            return current.provided().contains(key) ? current.clean().containsKey(key) : this.real.method_4678(key);
         } else {
            return !this.hidden.contains(key) && this.real.method_4678(key);
         }
      }

      public boolean method_29428() {
         return this.real.method_29428();
      }

      public class_5481 method_30934(class_5348 text) {
         return this.real.method_30934(text);
      }
   }
}
