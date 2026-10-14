package net.anubis.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
public final class ItemColorListSetting extends Setting {
   private List<ItemEntry> entries;
   private Supplier<List<ItemEntry>> entriesSupplier;
   private final float[] defaultColor;
   private Map<String, Integer> enabledLookup;

   public ItemColorListSetting(String name, List<ItemEntry> entries, float[] defaultColor) {
      super(name);
      this.entries = new ArrayList(entries);
      this.defaultColor = defaultColor;
      this.onChange(this::invalidateLookup);
   }

   public ItemColorListSetting(String name, Supplier<List<ItemEntry>> entriesSupplier, float[] defaultColor) {
      super(name);
      this.entriesSupplier = entriesSupplier;
      this.defaultColor = defaultColor;
      this.onChange(this::invalidateLookup);
   }

   private List<ItemEntry> entriesInternal() {
      if (this.entries == null) {
         this.entries = new ArrayList((Collection)this.entriesSupplier.get());
         this.entriesSupplier = null;
      }

      return this.entries;
   }

   public List<ItemEntry> entries() {
      return Collections.unmodifiableList(this.entriesInternal());
   }

   public float[] defaultColor() {
      return this.defaultColor;
   }

   public void invalidateLookup() {
      this.enabledLookup = null;
   }

   private Map<String, Integer> lookup() {
      if (this.enabledLookup == null) {
         this.rebuildLookup();
      }

      return this.enabledLookup;
   }

   private void rebuildLookup() {
      Map<String, Integer> map = new HashMap();

      for(ItemEntry entry : this.entriesInternal()) {
         if (entry.enabled().get()) {
            map.put(entry.id(), rgbaToArgb(entry.color()));
         }
      }

      this.enabledLookup = map;
   }

   public boolean isEnabled(class_1792 item) {
      class_2960 key = class_7923.field_41178.method_10221(item);
      return key == null ? false : this.lookup().containsKey(key.toString());
   }

   public int colorArgb(class_1792 item) {
      class_2960 key = class_7923.field_41178.method_10221(item);
      if (key == null) {
         return rgbaToArgb(this.defaultColor);
      } else {
         Integer color = (Integer)this.lookup().get(key.toString());
         return color != null ? color : rgbaToArgb(this.defaultColor);
      }
   }

   public boolean hasAnyEnabled() {
      return !this.lookup().isEmpty();
   }

   public JsonElement serialize() {
      JsonObject root = new JsonObject();
      JsonArray defCol = new JsonArray();

      for(float v : this.defaultColor) {
         defCol.add(clamp01(v));
      }

      root.add("defaultColor", defCol);
      JsonArray arr = new JsonArray();

      for(ItemEntry e : this.entriesInternal()) {
         JsonObject obj = new JsonObject();
         obj.addProperty("id", e.id());
         obj.addProperty("enabled", e.enabled().get());
         JsonArray col = new JsonArray();

         for(float v : e.color()) {
            col.add(clamp01(v));
         }

         obj.add("color", col);
         arr.add(obj);
      }

      root.add("items", arr);
      return root;
   }

   public JsonElement compact(JsonElement value) {
      JsonElement defaults = this.defaultJson();
      if (value != null && value.isJsonObject() && defaults != null && defaults.isJsonObject()) {
         JsonObject root = value.getAsJsonObject();
         JsonObject shipped = defaults.getAsJsonObject();
         if (!root.has("changes") && root.has("items") && shipped.has("items")) {
            try {
               Map<String, String> before = new HashMap();

               for(JsonElement el : shipped.getAsJsonArray("items")) {
                  JsonObject obj = el.getAsJsonObject();
                  before.put(obj.get("id").getAsString(), packItem(obj));
               }

               JsonObject changes = new JsonObject();

               for(JsonElement el : root.getAsJsonArray("items")) {
                  if (el.isJsonObject() && el.getAsJsonObject().has("id")) {
                     JsonObject obj = el.getAsJsonObject();
                     String id = obj.get("id").getAsString();
                     String now = packItem(obj);
                     String was = (String)before.get(id);
                     if (!now.equals(was)) {
                        boolean sameColor = was != null && now.substring(2).equals(was.substring(2));
                        changes.addProperty(BlockColorListSetting.shortId(id), sameColor ? now.substring(0, 1) : now);
                     }
                  }
               }

               JsonObject out = new JsonObject();
               String color = BlockColorListSetting.hexColor(root.get("defaultColor"));
               if (color != null && !color.equals(BlockColorListSetting.hexColor(shipped.get("defaultColor")))) {
                  out.addProperty("defaultColor", color);
               }

               out.add("changes", changes);
               return out;
            } catch (RuntimeException var14) {
               return value;
            }
         } else {
            return value;
         }
      } else {
         return value;
      }
   }

   private static String packItem(JsonObject obj) {
      boolean on = obj.has("enabled") && obj.get("enabled").getAsBoolean();
      return (on ? "1" : "0") + " " + BlockColorListSetting.hexColor(obj.get("color"));
   }

   protected void deserializeValue(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         try {
            JsonObject root = element.getAsJsonObject();
            if (root.has("changes") && root.get("changes").isJsonObject()) {
               this.applyChanges(root);
            } else {
               this.applyFull(root);
            }
         } catch (Exception var3) {
         }

      }
   }

   private void applyFull(JsonObject root) {
      if (root.has("defaultColor")) {
         JsonArray defCol = root.getAsJsonArray("defaultColor");

         for(int i = 0; i < Math.min(4, defCol.size()); ++i) {
            this.defaultColor[i] = clamp01(defCol.get(i).getAsFloat());
         }
      }

      if (root.has("items")) {
         Map<String, ItemEntry> byId = this.byId();

         for(JsonElement el : root.getAsJsonArray("items")) {
            if (el.isJsonObject()) {
               JsonObject obj = el.getAsJsonObject();
               if (obj.has("id")) {
                  ItemEntry entry = (ItemEntry)byId.get(obj.get("id").getAsString());
                  if (entry != null) {
                     if (obj.has("enabled")) {
                        entry.enabled().set(obj.get("enabled").getAsBoolean());
                     }

                     if (obj.has("color")) {
                        JsonArray col = obj.getAsJsonArray("color");
                        float[] c = entry.color();

                        for(int i = 0; i < Math.min(4, col.size()); ++i) {
                           c[i] = clamp01(col.get(i).getAsFloat());
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private void applyChanges(JsonObject root) {
      JsonElement defaults = this.defaultJson();
      if (defaults != null && defaults.isJsonObject()) {
         this.applyFull(defaults.getAsJsonObject());
      }

      if (root.has("defaultColor")) {
         BlockColorListSetting.readHex(root.get("defaultColor").getAsString(), this.defaultColor);
      }

      Map<String, ItemEntry> byId = this.byId();

      for(Map.Entry<String, JsonElement> change : root.getAsJsonObject("changes").entrySet()) {
         ItemEntry entry = (ItemEntry)byId.get(BlockColorListSetting.fullId((String)change.getKey()));
         if (entry != null && ((JsonElement)change.getValue()).isJsonPrimitive()) {
            String[] parts = ((JsonElement)change.getValue()).getAsString().split(" ");

            float[] color;
            try {
               color = parts.length >= 2 ? BlockColorListSetting.parseHex(parts[1]) : null;
            } catch (NumberFormatException var10) {
               continue;
            }

            entry.enabled().set("1".equals(parts[0]));
            if (color != null) {
               System.arraycopy(color, 0, entry.color(), 0, 4);
            }
         }
      }

   }

   private Map<String, ItemEntry> byId() {
      Map<String, ItemEntry> map = new HashMap();

      for(ItemEntry entry : this.entriesInternal()) {
         map.putIfAbsent(entry.id(), entry);
      }

      return map;
   }

   public static ItemColorListSetting createDefault() {
      return new ItemColorListSetting("Item Colors", ItemColorListSetting::buildDefaultEntries, new float[]{1.0F, 1.0F, 1.0F, 0.8F});
   }

   private static List<ItemEntry> buildDefaultEntries() {
      List<ItemEntry> e = new ArrayList();

      for(class_1792 item : class_7923.field_41178) {
         class_2960 key = class_7923.field_41178.method_10221(item);
         if (key != null) {
            String id = key.toString();
            String path = key.method_12832();
            String displayName = prettify(path);
            float[] color = defaultColorFor(path);
            boolean enabled = isPreEnabled(path);
            e.add(entry(id, displayName, enabled, color));
         }
      }

      e.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
      return e;
   }

   private static String prettify(String path) {
      String[] words = path.split("_");
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < words.length; ++i) {
         if (!words[i].isEmpty()) {
            sb.append(Character.toUpperCase(words[i].charAt(0)));
            sb.append(words[i].substring(1));
            if (i < words.length - 1) {
               sb.append(" ");
            }
         }
      }

      return sb.toString();
   }

   private static boolean isPreEnabled(String path) {
      boolean var10000;
      switch (path) {
         case "diamond":
         case "emerald":
         case "netherite_ingot":
         case "ancient_debris":
         case "elytra":
         case "totem_of_undying":
         case "trident":
         case "enchanted_golden_apple":
         case "golden_apple":
         case "nether_star":
         case "heart_of_the_sea":
         case "end_crystal":
         case "shulker_box":
            var10000 = true;
            break;
         case "gold_ingot":
         case "gold_block":
         case "gold_nugget":
            var10000 = true;
            break;
         case "iron_ingot":
            var10000 = true;
            break;
         default:
            var10000 = false;
      }

      return var10000;
   }

   private static float[] defaultColorFor(String path) {
      float[] var10000;
      switch (path) {
         case "diamond":
         case "diamond_sword":
         case "diamond_pickaxe":
         case "diamond_axe":
         case "diamond_shovel":
         case "diamond_hoe":
         case "diamond_helmet":
         case "diamond_chestplate":
         case "diamond_leggings":
         case "diamond_boots":
            var10000 = rgba(0, 255, 255);
            break;
         case "emerald":
            var10000 = rgba(0, 255, 68);
            break;
         case "netherite_ingot":
         case "ancient_debris":
         case "netherite_sword":
         case "netherite_pickaxe":
         case "netherite_axe":
         case "netherite_shovel":
         case "netherite_hoe":
         case "netherite_helmet":
         case "netherite_chestplate":
         case "netherite_leggings":
         case "netherite_boots":
            var10000 = rgba(128, 96, 144);
            break;
         case "gold_ingot":
         case "gold_block":
         case "gold_nugget":
         case "golden_sword":
         case "golden_pickaxe":
         case "golden_axe":
         case "golden_shovel":
         case "golden_hoe":
         case "golden_helmet":
         case "golden_chestplate":
         case "golden_leggings":
         case "golden_boots":
         case "golden_apple":
         case "enchanted_golden_apple":
            var10000 = rgba(255, 215, 0);
            break;
         case "iron_ingot":
            var10000 = rgba(204, 204, 204);
            break;
         case "elytra":
            var10000 = rgba(85, 85, 255);
            break;
         case "totem_of_undying":
            var10000 = rgba(255, 170, 0);
            break;
         case "trident":
            var10000 = rgba(0, 136, 255);
            break;
         case "nether_star":
            var10000 = rgba(255, 255, 0);
            break;
         case "heart_of_the_sea":
            var10000 = rgba(0, 255, 221);
            break;
         case "end_crystal":
            var10000 = rgba(255, 0, 255);
            break;
         case "shulker_box":
            var10000 = rgba(204, 68, 204);
            break;
         default:
            var10000 = rgba(255, 255, 255);
      }

      return var10000;
   }

   private static float[] rgba(int r, int g, int b) {
      return new float[]{(float)r / 255.0F, (float)g / 255.0F, (float)b / 255.0F, 1.0F};
   }

   private static ItemEntry entry(String id, String display, boolean enabled, float[] color) {
      return new ItemEntry(id, display, new ImBoolean(enabled), color);
   }

   public static int rgbaToArgb(float[] rgba) {
      int r = Math.round(clamp01(rgba[0]) * 255.0F);
      int g = Math.round(clamp01(rgba[1]) * 255.0F);
      int b = Math.round(clamp01(rgba[2]) * 255.0F);
      int a = Math.round(clamp01(rgba[3]) * 255.0F);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static float clamp01(float v) {
      return !Float.isNaN(v) && !Float.isInfinite(v) ? Math.max(0.0F, Math.min(1.0F, v)) : 1.0F;
   }

   @Environment(EnvType.CLIENT)
   public static record ItemEntry(String id, String displayName, ImBoolean enabled, float[] color) {
   }
}
