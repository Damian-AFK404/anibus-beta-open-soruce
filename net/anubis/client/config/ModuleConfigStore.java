package net.anubis.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.anubis.client.event.impl.ModuleToggleEvent;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.ModuleRevision;
import net.anubis.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ModuleConfigStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final ModuleConfigStore INSTANCE = new ModuleConfigStore();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 3;
   private static final long AUTOSAVE_DELAY_NANOS = 2000000000L;
   private static final Map<String, String> RENAMED_MODULES = Map.of("activity-bypass", "player-bypass", "Activity Bypass", "player-bypass");
   private final Map<String, JsonObject> orphanModules = new LinkedHashMap();
   private final Map<String, Boolean> pendingEnabled = new LinkedHashMap();
   private boolean loaded;
   private boolean readOnly;
   private final Map<String, JsonObject> retainedEntries = new LinkedHashMap();
   private String activeProfile;
   private long savedRevision = -1L;
   private long observedRevision = -1L;
   private long observedAt;

   private ModuleConfigStore() {
   }

   public static ModuleConfigStore getInstance() {
      return INSTANCE;
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         this.orphanModules.clear();
         this.pendingEnabled.clear();
         BundledDefaults.adopt(ModuleRegistry.all());
         this.readConfig();

         for(ClientModule module : ModuleRegistry.all()) {
            if (BundledDefaults.enabledByDefault(module) && !this.retainedEntries.containsKey(module.id())) {
               this.pendingEnabled.put(module.id(), true);
            }
         }

      }
   }

   private void readConfig() {
      Path path = this.configPath();
      if (Files.isRegularFile(path, new LinkOption[0])) {
         try {
            Reader reader = Files.newBufferedReader(path);

            try {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               this.applyConfigRoot(root);
               if (root.has("activeProfile") && root.get("activeProfile").isJsonPrimitive()) {
                  this.activeProfile = root.get("activeProfile").getAsString();
               }

               int version = root.has("version") ? root.get("version").getAsInt() : 1;
               if (!this.readOnly && version < 3) {
                  String var10001 = String.valueOf(path.getFileName());
                  Path backup = path.resolveSibling(var10001 + ".v" + version + ".bak");
                  if (!Files.exists(backup, new LinkOption[0])) {
                     Files.copy(path, backup);
                  }
               }
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }
         } catch (RuntimeException | IOException e) {
            LOGGER.warn("[Anubis] Failed to load module config; preserving the file", e);
            this.orphanModules.clear();
            this.pendingEnabled.clear();
            this.readOnly = true;
         }

      }
   }

   public synchronized boolean enablesOnLoad(ClientModule module) {
      return Boolean.TRUE.equals(this.pendingEnabled.get(module.id()));
   }

   public synchronized void applyPendingEnabled() {
      if (!this.pendingEnabled.isEmpty()) {
         ModuleToggleEvent.silently(() -> {
            for(Map.Entry<String, Boolean> entry : this.pendingEnabled.entrySet()) {
               if (Boolean.TRUE.equals(entry.getValue())) {
                  ClientModule module = ModuleRegistry.resolve((String)entry.getKey());
                  if (module != null && module.persistEnabled()) {
                     module.setEnabled(true);
                  }
               }
            }

         });
         this.pendingEnabled.clear();
      }
   }

   public synchronized void save() {
      this.ensureLoaded();
      if (!this.readOnly) {
         long revision = ModuleRevision.current();
         JsonObject root = this.buildConfigRoot();
         if (this.activeProfile != null) {
            root.addProperty("activeProfile", this.activeProfile);
         }

         Path path = this.configPath();

         try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Writer writer = Files.newBufferedWriter(tmp);

            try {
               GSON.toJson(root, writer);
            } catch (Throwable var11) {
               if (writer != null) {
                  try {
                     writer.close();
                  } catch (Throwable var9) {
                     var11.addSuppressed(var9);
                  }
               }

               throw var11;
            }

            if (writer != null) {
               writer.close();
            }

            try {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var10) {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }

            this.savedRevision = revision;
         } catch (IOException e) {
            LOGGER.warn("[Anubis] Failed to save module config", e);
         }

      }
   }

   public synchronized void tickAutosave() {
      long now = System.nanoTime();
      if (this.autosaveDue(ModuleRevision.current(), now)) {
         this.save();
         this.observedAt = now;
      }
   }

   private boolean autosaveDue(long revision, long now) {
      if (this.loaded && !this.readOnly) {
         if (this.savedRevision < 0L) {
            this.savedRevision = this.observedRevision = revision;
            return false;
         } else if (revision == this.savedRevision) {
            return false;
         } else if (revision != this.observedRevision) {
            this.observedRevision = revision;
            this.observedAt = now;
            return false;
         } else {
            return now - this.observedAt >= 2000000000L;
         }
      } else {
         return false;
      }
   }

   public synchronized boolean hasUnsavedChanges() {
      return !this.readOnly && this.savedRevision >= 0L && ModuleRevision.current() != this.savedRevision;
   }

   public synchronized boolean isReadOnly() {
      return this.readOnly;
   }

   public synchronized String activeProfile() {
      return this.activeProfile;
   }

   synchronized void setActiveProfile(String name) {
      this.activeProfile = name;
   }

   public synchronized JsonObject snapshot() {
      this.ensureLoaded();
      JsonObject root = this.buildConfigRoot();
      JsonObject modules = root.getAsJsonObject("modules");

      for(ClientModule module : ModuleRegistry.all()) {
         if (module.inConfigs()) {
            modules.add(module.id(), withoutPrivate(module, modules.getAsJsonObject(module.id())));
         } else {
            modules.remove(module.id());
         }
      }

      return root;
   }

   public synchronized boolean applyProfile(JsonObject root) {
      this.ensureLoaded();
      int version = profileVersion(root);
      if (version < 0) {
         return false;
      } else {
         List<ClientModule> modules = ModuleRegistry.all();
         Map<ClientModule, JsonObject> matched = matchEntries(root, version, modules);
         Map<String, Boolean> enable = new HashMap();
         ModuleToggleEvent.silently(() -> {
            for(ClientModule module : modules) {
               if (module.inConfigs()) {
                  JsonObject entry = (JsonObject)matched.get(module);

                  for(Setting setting : module.settings()) {
                     if (!setting.isPrivate()) {
                        setting.resetToDefault();
                     }
                  }

                  module.setKeybind(-1);
                  module.setHoldMode(false);
                  if (entry != null) {
                     entry = withoutPrivate(module, entry);
                     applyModuleEntry(module, entry, enable);
                     this.retainedEntries.put(module.id(), entry);
                  } else {
                     this.retainedEntries.remove(module.id());
                  }

                  if (module.persistEnabled()) {
                     module.setEnabled(Boolean.TRUE.equals(enable.get(module.id())));
                  }
               }
            }

         });
         this.pendingEnabled.clear();
         return true;
      }
   }

   public synchronized JsonObject shareable(JsonObject root) {
      int version = profileVersion(root);
      if (version < 0) {
         return null;
      } else {
         List<ClientModule> modules = ModuleRegistry.all();
         Map<ClientModule, JsonObject> matched = matchEntries(root, version, modules);
         JsonObject shared = new JsonObject();

         for(ClientModule module : modules) {
            JsonObject entry = module.inConfigs() ? (JsonObject)matched.get(module) : null;
            JsonObject kept = entry == null ? null : shareableEntry(module, entry);
            if (kept != null) {
               shared.add(module.id(), kept);
            }
         }

         JsonObject out = new JsonObject();
         out.addProperty("version", 3);
         out.add("modules", shared);
         return out;
      }
   }

   private static int profileVersion(JsonObject root) {
      if (root != null && root.has("modules") && root.get("modules").isJsonObject()) {
         try {
            int version = root.has("version") ? root.get("version").getAsInt() : 0;
            return version > 3 ? -1 : version;
         } catch (RuntimeException var2) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static Map<ClientModule, JsonObject> matchEntries(JsonObject root, int version, List<ClientModule> modules) {
      Map<String, ClientModule> byKey = new HashMap();

      for(ClientModule module : modules) {
         byKey.putIfAbsent(module.name(), module);
      }

      for(ClientModule module : modules) {
         byKey.put(module.id(), module);
      }

      Map<ClientModule, JsonObject> matched = new HashMap();

      for(Map.Entry<String, JsonElement> entry : renameModules(root.getAsJsonObject("modules"), version).entrySet()) {
         if (((JsonElement)entry.getValue()).isJsonObject()) {
            JsonObject moduleEntry = ((JsonElement)entry.getValue()).getAsJsonObject();
            ClientModule module = (ClientModule)byKey.get(entry.getKey());
            if (module == null && moduleEntry.has("name") && moduleEntry.get("name").isJsonPrimitive()) {
               module = (ClientModule)byKey.get(moduleEntry.get("name").getAsString());
            }

            if (module != null) {
               matched.putIfAbsent(module, moduleEntry);
            }
         }
      }

      return matched;
   }

   private static JsonObject shareableEntry(ClientModule module, JsonObject entry) {
      JsonObject out = new JsonObject();
      if (module.persistEnabled() && flag(entry, "enabled")) {
         out.addProperty("enabled", true);
      }

      int key = number(entry, "keybind", -1);
      if (key != -1 && ModuleKeybinds.isBindable(key)) {
         out.addProperty("keybind", key);
      }

      if (flag(entry, "holdMode")) {
         out.addProperty("holdMode", true);
      }

      JsonObject values = entry.has("settings") && entry.get("settings").isJsonObject() ? entry.getAsJsonObject("settings") : null;
      JsonObject settings = new JsonObject();
      if (values != null) {
         for(Setting setting : module.settings()) {
            if (!setting.isPrivate()) {
               JsonElement value = values.has(setting.id()) ? values.get(setting.id()) : values.get(setting.name());
               if (value != null && !value.isJsonNull() && !setting.isDefault(value)) {
                  settings.add(setting.id(), setting.compact(value.deepCopy()));
               }
            }
         }
      }

      if (settings.size() > 0) {
         out.add("settings", settings);
      }

      if (out.size() == 0) {
         return null;
      } else {
         int saved = number(entry, "settingsVersion", 0);
         if (saved > 0) {
            out.addProperty("settingsVersion", saved);
         }

         return out;
      }
   }

   private static JsonObject withoutPrivate(ClientModule module, JsonObject entry) {
      JsonObject copy = entry.deepCopy();
      if (copy.has("settings") && copy.get("settings").isJsonObject()) {
         JsonObject settings = copy.getAsJsonObject("settings");

         for(Setting setting : module.settings()) {
            if (setting.isPrivate()) {
               settings.remove(setting.id());
               settings.remove(setting.name());
            }
         }
      }

      return copy;
   }

   private static boolean flag(JsonObject entry, String key) {
      JsonElement value = entry.get(key);
      if (value != null && value.isJsonPrimitive()) {
         try {
            return value.getAsBoolean();
         } catch (RuntimeException var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int number(JsonObject entry, String key, int fallback) {
      JsonElement value = entry.get(key);
      if (value != null && value.isJsonPrimitive()) {
         try {
            return value.getAsInt();
         } catch (RuntimeException var5) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   public synchronized void adopt(ClientModule module) {
      if (this.loaded) {
         JsonObject entry = (JsonObject)this.orphanModules.remove(module.id());
         if (entry != null) {
            this.retainedEntries.put(module.id(), entry.deepCopy());
            applyModuleEntry(module, entry, this.pendingEnabled);
         }
      }
   }

   public synchronized void release(ClientModule module) {
      if (this.loaded) {
         JsonObject entry = this.serializeModule(module);
         if (Boolean.TRUE.equals(this.pendingEnabled.remove(module.id()))) {
            entry.addProperty("enabled", true);
         }

         this.orphanModules.put(module.id(), entry);
         this.retainedEntries.remove(module.id());
      }
   }

   public static String renamedModuleId(String key) {
      for(Map.Entry<String, String> rename : RENAMED_MODULES.entrySet()) {
         if (((String)rename.getKey()).equalsIgnoreCase(key)) {
            return (String)rename.getValue();
         }
      }

      return null;
   }

   private void applyConfigRoot(JsonObject root) {
      if (root != null) {
         int version = root.has("version") ? root.get("version").getAsInt() : 0;
         if (version > 3) {
            this.readOnly = true;
            LOGGER.warn("[Anubis] Module config version {} is newer than supported {}; skipping load", version, 3);
         } else if (root.has("modules") && root.get("modules").isJsonObject()) {
            JsonObject modules = renameModules(root.getAsJsonObject("modules"), version);

            for(Map.Entry<String, JsonElement> entry : modules.entrySet()) {
               String name = (String)entry.getKey();
               if (((JsonElement)entry.getValue()).isJsonObject()) {
                  JsonObject moduleEntry = ((JsonElement)entry.getValue()).getAsJsonObject();
                  ClientModule module = ModuleRegistry.resolve(name);
                  if (module == null && version >= 2 && moduleEntry.has("name") && moduleEntry.get("name").isJsonPrimitive()) {
                     module = ModuleRegistry.resolve(moduleEntry.get("name").getAsString());
                  }

                  if (module == null) {
                     this.orphanModules.put(name, moduleEntry.deepCopy());
                  } else {
                     this.retainedEntries.put(module.id(), moduleEntry.deepCopy());
                     applyModuleEntry(module, moduleEntry, this.pendingEnabled);
                  }
               }
            }

         }
      }
   }

   private JsonObject buildConfigRoot() {
      JsonObject root = new JsonObject();
      root.addProperty("version", 3);
      JsonObject modulesObj = new JsonObject();

      for(ClientModule module : ModuleRegistry.all()) {
         modulesObj.add(module.id(), this.serializeModule(module));
      }

      for(Map.Entry<String, JsonObject> orphan : this.orphanModules.entrySet()) {
         if (!modulesObj.has((String)orphan.getKey())) {
            modulesObj.add((String)orphan.getKey(), ((JsonObject)orphan.getValue()).deepCopy());
         }
      }

      root.add("modules", modulesObj);
      return root;
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }

   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("modules.json");
   }

   private static void applyModuleEntry(ClientModule module, JsonObject entry, Map<String, Boolean> pendingEnabled) {
      if (entry.has("keybind") && entry.get("keybind").isJsonPrimitive()) {
         try {
            int key = entry.get("keybind").getAsInt();
            if (key == -1 || ModuleKeybinds.isBindable(key)) {
               module.setKeybind(key);
            }
         } catch (RuntimeException var9) {
         }
      }

      if (entry.has("holdMode") && entry.get("holdMode").isJsonPrimitive()) {
         try {
            module.setHoldMode(entry.get("holdMode").getAsBoolean());
         } catch (RuntimeException var8) {
         }
      }

      if (entry.has("settings") && entry.get("settings").isJsonObject()) {
         JsonObject settings = entry.getAsJsonObject("settings");

         for(Setting setting : module.settings()) {
            if (!setting.isSessionOnly()) {
               JsonElement value = settings.has(setting.id()) ? settings.get(setting.id()) : settings.get(setting.name());
               if (value != null) {
                  setting.deserialize(value);
               }
            }
         }
      }

      migrateSettings(module, entry);
      if (entry.has("enabled") && entry.get("enabled").isJsonPrimitive()) {
         boolean enabled;
         try {
            enabled = entry.get("enabled").getAsBoolean();
         } catch (RuntimeException var7) {
            return;
         }

         if (module.persistEnabled()) {
            String name = module.id();
            if (enabled) {
               pendingEnabled.put(name, true);
            } else {
               module.setEnabled(false);
            }

         }
      }
   }

   private static void migrateSettings(ClientModule module, JsonObject entry) {
      int current = module.settingsVersion();
      if (current > 0) {
         int saved = 0;
         if (entry.has("settingsVersion") && entry.get("settingsVersion").isJsonPrimitive()) {
            try {
               saved = entry.get("settingsVersion").getAsInt();
            } catch (RuntimeException var6) {
            }
         }

         if (saved < current) {
            try {
               module.migrateSettings(saved);
            } catch (RuntimeException e) {
               LOGGER.warn("[Anubis] Failed to migrate settings of '{}' from version {}", new Object[]{module.id(), saved, e});
            }

         }
      }
   }

   private static JsonObject renameModules(JsonObject modules, int version) {
      if (version >= 3) {
         return modules;
      } else {
         JsonObject renamed = new JsonObject();

         for(Map.Entry<String, JsonElement> entry : modules.entrySet()) {
            String id = (String)RENAMED_MODULES.get(entry.getKey());
            if (id == null) {
               renamed.add((String)entry.getKey(), (JsonElement)entry.getValue());
            } else if (!modules.has(id) && !renamed.has(id)) {
               renamed.add(id, (JsonElement)entry.getValue());
            }
         }

         return renamed;
      }
   }

   private JsonObject serializeModule(ClientModule module) {
      JsonObject previous = (JsonObject)this.retainedEntries.get(module.id());
      JsonObject entry = previous == null ? new JsonObject() : previous.deepCopy();
      entry.addProperty("name", module.name());
      entry.addProperty("enabled", module.isEnabled());
      entry.addProperty("keybind", module.keybind());
      entry.addProperty("holdMode", module.isHoldMode());
      if (module.settingsVersion() > 0) {
         entry.addProperty("settingsVersion", module.settingsVersion());
      }

      JsonObject settings = entry.has("settings") && entry.get("settings").isJsonObject() ? entry.getAsJsonObject("settings").deepCopy() : new JsonObject();

      for(Setting setting : module.settings()) {
         settings.remove(setting.name());
         if (setting.isSessionOnly()) {
            settings.remove(setting.id());
         } else {
            settings.add(setting.id(), setting.serialize());
         }
      }

      entry.add("settings", settings);
      return entry;
   }
}
