package net.anubis.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import net.anubis.client.config.BundledDefaults;
import net.anubis.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class PanelStateStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final PanelStateStore INSTANCE = new PanelStateStore();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private final Set<String> collapsed = new LinkedHashSet();
   private boolean loaded;
   private boolean readOnly;

   private PanelStateStore() {
   }

   public static PanelStateStore getInstance() {
      return INSTANCE;
   }

   public synchronized boolean isCollapsed(ModuleCategory category) {
      this.ensureLoaded();
      return this.collapsed.contains(category.name());
   }

   public synchronized void setCollapsed(ModuleCategory category, boolean fold) {
      this.ensureLoaded();
      boolean changed = fold ? this.collapsed.add(category.name()) : this.collapsed.remove(category.name());
      if (changed) {
         this.save();
      }

   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         this.collapsed.clear();
         Path path = this.configPath();
         if (!Files.isRegularFile(path, new LinkOption[0])) {
            this.collapsed.addAll(BundledDefaults.collapsedPanels());
         } else {
            try {
               Reader reader = Files.newBufferedReader(path);

               label96: {
                  label86: {
                     try {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        int version = root.has("version") ? root.get("version").getAsInt() : 0;
                        if (version > 1) {
                           this.readOnly = true;
                           LOGGER.warn("[Anubis] ClickGUI state version {} is newer than supported {}; skipping load", version, 1);
                           break label96;
                        }

                        if (root.has("collapsed") && root.get("collapsed").isJsonArray()) {
                           Iterator var5 = root.getAsJsonArray("collapsed").iterator();

                           while(true) {
                              if (!var5.hasNext()) {
                                 break label86;
                              }

                              JsonElement element = (JsonElement)var5.next();
                              if (element.isJsonPrimitive()) {
                                 this.collapsed.add(element.getAsString());
                              }
                           }
                        }
                     } catch (Throwable var8) {
                        if (reader != null) {
                           try {
                              reader.close();
                           } catch (Throwable var7) {
                              var8.addSuppressed(var7);
                           }
                        }

                        throw var8;
                     }

                     if (reader != null) {
                        reader.close();
                     }

                     return;
                  }

                  if (reader != null) {
                     reader.close();
                  }

                  return;
               }

               if (reader != null) {
                  reader.close();
               }

            } catch (RuntimeException | IOException e) {
               LOGGER.warn("[Anubis] Failed to load the ClickGUI state; preserving the file", e);
               this.collapsed.clear();
               this.readOnly = true;
            }
         }
      }
   }

   public synchronized void save() {
      if (!this.readOnly) {
         JsonObject root = new JsonObject();
         root.addProperty("version", 1);
         JsonArray list = new JsonArray();

         for(String id : this.collapsed) {
            list.add(id);
         }

         root.add("collapsed", list);
         Path path = this.configPath();

         try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Writer writer = Files.newBufferedWriter(tmp);

            try {
               GSON.toJson(root, writer);
            } catch (Throwable var10) {
               if (writer != null) {
                  try {
                     writer.close();
                  } catch (Throwable var8) {
                     var10.addSuppressed(var8);
                  }
               }

               throw var10;
            }

            if (writer != null) {
               writer.close();
            }

            try {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var9) {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException e) {
            LOGGER.warn("[Anubis] Failed to save the ClickGUI state", e);
         }

      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }

   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("clickgui.json");
   }
}
