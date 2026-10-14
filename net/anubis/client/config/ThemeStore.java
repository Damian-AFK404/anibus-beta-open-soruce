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
import java.util.ArrayList;
import java.util.List;
import net.anubis.client.imgui.UiFont;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_310;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class ThemeStore {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final ThemeStore INSTANCE = new ThemeStore();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 3;
   private static final String THEME_KEY = "anubisTheme";
   public static final List<Preset> PRESETS = List.of(new Preset("Default", -13338378, -14736856, -15263458), new Preset("Midnight", -7643914, -15001050, -15461603), new Preset("Rose", -770210, -14410720, -15002344), new Preset("Mint", -15681151, -15064288, -15459560), new Preset("Amber", -680437, -14409445, -15001324), new Preset("Mono", -1710101, -14671840, -15263977));
   public final ColorSetting accent = new ColorSetting("Accent", -13338378);
   public final NumberSetting rounding = (new NumberSetting("Corner radius", 16.0F, 0.0F, 16.0F)).step(1.0F);
   public final NumberSetting backgroundDim = (new NumberSetting("Background dim", 0.55F, 0.0F, 0.8F)).step(0.05F);
   public final BooleanSetting menuBlur = new BooleanSetting("Menu blur", true);
   public final BooleanSetting moduleDividers = new BooleanSetting("Module dividers", true);
   public final NumberSetting uiScale = (new NumberSetting("UI scale", 1.0F, 0.75F, 1.5F)).step(0.05F);
   public final ModeSetting font;
   public final NumberSetting animationSpeed;
   public final BooleanSetting animateLines;
   public final BooleanSetting customMainMenu;
   public final BooleanSetting communityPopup;
   public final Surface panels;
   public final Surface cards;
   public final Surface tabBar;
   private final List<Surface> surfaces;
   private final List<Setting> shared;
   private final List<Setting> settings;
   private final JsonObject defaults;
   private boolean loaded;
   private boolean readOnly;

   private ThemeStore() {
      this.font = new ModeSetting("Font", UiFont.GEIST.label, UiFont.labels());
      this.animationSpeed = (new NumberSetting("Animation speed", 1.3F, 0.0F, 2.0F)).step(0.1F);
      this.animateLines = new BooleanSetting("Animate patterns", true);
      this.customMainMenu = new BooleanSetting("Custom menus", true);
      this.communityPopup = new BooleanSetting("Community popup", true);
      this.panels = new Surface("panels", "Panels", false, -14736856, 1.0F, 0.45F, "Flow");
      this.cards = new Surface("cards", "Cards", true, -15263458, 1.0F, 0.85F);
      this.tabBar = new Surface("tabbar", "Tab bar", false, -15263458, 1.0F, 0.5F, "Flow");
      this.surfaces = List.of(this.panels, this.cards, this.tabBar);
      List<Setting> all = new ArrayList(List.of(this.accent, this.rounding, this.backgroundDim, this.menuBlur, this.moduleDividers, this.uiScale, this.font, this.animationSpeed, this.animateLines));

      for(Surface surface : this.surfaces) {
         all.addAll(surface.settings());
      }

      this.shared = List.copyOf(all);
      all.add(this.customMainMenu);
      all.add(this.communityPopup);
      this.settings = List.copyOf(all);
      this.defaults = snapshot(this.settings, true);
      this.customMainMenu.onChange(() -> {
         class_310 mc = class_310.method_1551();
         if (mc != null && mc.method_22683() != null) {
            mc.method_24288();
         }

      });
   }

   public static ThemeStore get() {
      INSTANCE.ensureLoaded();
      return INSTANCE;
   }

   public List<Surface> surfaces() {
      return this.surfaces;
   }

   public void applyPreset(Preset preset) {
      this.accent.setArgb(preset.accent());
      this.panels.color.setArgb(preset.panel());
      this.cards.color.setArgb(preset.card());
      this.tabBar.color.setArgb(preset.card());
   }

   public boolean matchesPreset(Preset preset) {
      return this.accent.rgb() == (preset.accent() & 16777215) && this.panels.color.rgb() == (preset.panel() & 16777215) && this.cards.color.rgb() == (preset.card() & 16777215);
   }

   public void resetToDefaults() {
      apply(this.shared, this.defaults, true);
   }

   public static Path backgroundsDir() {
      return FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("backgrounds");
   }

   public String exportString() {
      JsonObject root = new JsonObject();
      root.add("anubisTheme", snapshot(this.shared, false));
      return root.toString();
   }

   public boolean importString(String text) {
      if (text != null && !text.isBlank()) {
         try {
            JsonElement parsed = JsonParser.parseString(text.trim());
            if (parsed.isJsonObject() && parsed.getAsJsonObject().has("anubisTheme")) {
               JsonElement values = parsed.getAsJsonObject().get("anubisTheme");
               if (!values.isJsonObject()) {
                  return false;
               } else {
                  apply(this.shared, values.getAsJsonObject(), false);
                  return true;
               }
            } else {
               return false;
            }
         } catch (RuntimeException var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   public JsonObject profileSnapshot() {
      return snapshot(this.shared, true);
   }

   public void applyProfile(JsonObject values) {
      for(Setting setting : this.shared) {
         JsonElement value = values.get(setting.id());
         if (value == null && !setting.isPrivate()) {
            value = this.defaults.get(setting.id());
         }

         if (value != null) {
            setting.deserialize(value);
         }
      }

   }

   public JsonObject shareable(JsonObject values) {
      JsonObject kept = new JsonObject();

      for(Setting setting : this.shared) {
         JsonElement value = values.get(setting.id());
         if (value != null && !setting.isPrivate() && !Setting.sameValue(value, this.defaults.get(setting.id()))) {
            kept.add(setting.id(), value.deepCopy());
         }
      }

      return kept;
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         Path path = this.configPath();
         if (Files.isRegularFile(path, new LinkOption[0])) {
            try {
               Reader reader = Files.newBufferedReader(path);

               label82: {
                  try {
                     JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                     int version = root.has("version") ? root.get("version").getAsInt() : 0;
                     if (version > 3) {
                        this.readOnly = true;
                        LOGGER.warn("[Anubis] Theme version {} is newer than supported {}; skipping load", version, 3);
                        break label82;
                     }

                     if (root.has("values") && root.get("values").isJsonObject()) {
                        apply(this.settings, root.getAsJsonObject("values"), true);
                     }

                     if (version < 3) {
                        this.adoptNewDefaults();
                     }
                  } catch (Throwable var6) {
                     if (reader != null) {
                        try {
                           reader.close();
                        } catch (Throwable var5) {
                           var6.addSuppressed(var5);
                        }
                     }

                     throw var6;
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
               LOGGER.warn("[Anubis] Failed to load theme; preserving the file", e);
               this.readOnly = true;
            }
         }
      }
   }

   private void adoptNewDefaults() {
      if (this.panels.background.is("None")) {
         this.panels.background.set("Flow");
      }

      if (Math.abs(this.panels.opacity.get() - 0.92F) < 0.001F) {
         this.panels.opacity.set(1.0F);
      }

      for(Surface surface : List.of(this.cards, this.tabBar)) {
         if (Math.abs(surface.opacity.get() - 0.97F) < 0.001F) {
            surface.opacity.set(1.0F);
         }
      }

   }

   public synchronized void save() {
      if (!this.readOnly && this.loaded) {
         JsonObject root = new JsonObject();
         root.addProperty("version", 3);
         root.add("values", snapshot(this.settings, true));
         Path path = this.configPath();

         try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Writer writer = Files.newBufferedWriter(tmp);

            try {
               GSON.toJson(root, writer);
            } catch (Throwable var9) {
               if (writer != null) {
                  try {
                     writer.close();
                  } catch (Throwable var7) {
                     var9.addSuppressed(var7);
                  }
               }

               throw var9;
            }

            if (writer != null) {
               writer.close();
            }

            try {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var8) {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException e) {
            LOGGER.warn("[Anubis] Failed to save theme", e);
         }

      }
   }

   private static JsonObject snapshot(List<Setting> from, boolean withPrivate) {
      JsonObject values = new JsonObject();

      for(Setting setting : from) {
         if (withPrivate || !setting.isPrivate()) {
            values.add(setting.id(), setting.serialize());
         }
      }

      return values;
   }

   private static void apply(List<Setting> from, JsonObject values, boolean withPrivate) {
      for(Setting setting : from) {
         if ((withPrivate || !setting.isPrivate()) && values.has(setting.id())) {
            setting.deserialize(values.get(setting.id()));
         }
      }

   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }

   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("theme.json");
   }

   @Environment(EnvType.CLIENT)
   public static record Preset(String name, int accent, int panel, int card) {
   }
}
