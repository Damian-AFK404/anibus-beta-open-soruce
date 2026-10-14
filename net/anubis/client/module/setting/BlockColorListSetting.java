package net.anubis.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import imgui.type.ImBoolean;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2248;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
public final class BlockColorListSetting extends Setting {
   static final String CHANGES = "changes";
   private static final String VANILLA = "minecraft:";
   private List<BlockEntry> entries;
   private Supplier<List<BlockEntry>> entriesSupplier;
   private final Supplier<List<BlockEntry>> shipped;
   private final float[] defaultColor;
   private final float[] shippedDefaultColor;
   private final boolean colorsHidden;
   private final List<String> defaultOrder;
   private final Map<String, BlockEntry> byId;
   private final ArrayList<BlockEntry> selection;
   private final ReferenceOpenHashSet<BlockEntry> selectionSet;
   private final List<BlockEntry> selectionView;
   private long selectionVersion;
   private Map<String, Integer> enabledLookup;
   private Map<String, Integer> tracerLookup;
   private Reference2IntOpenHashMap<class_2248> enabledByBlock;
   private Reference2IntOpenHashMap<class_2248> tracerByBlock;
   private long lookupVersion;
   private long currentVersion;
   private static final String WALL_BANNER = "_wall_banner";

   public BlockColorListSetting(String name, List<BlockEntry> entries, float[] defaultColor) {
      super(name);
      this.byId = new HashMap();
      this.selection = new ArrayList();
      this.selectionSet = new ReferenceOpenHashSet();
      this.selectionView = Collections.unmodifiableList(this.selection);
      this.selectionVersion = -1L;
      this.lookupVersion = -1L;
      this.currentVersion = 0L;
      this.entries = new ArrayList(entries);
      List<BlockEntry> shippedEntries = entries.stream().map(BlockColorListSetting::copyEntry).toList();
      this.shipped = () -> shippedEntries;
      this.defaultColor = defaultColor;
      this.shippedDefaultColor = copyColor(defaultColor);
      this.colorsHidden = false;
      this.defaultOrder = List.of();
      this.index();
      this.onChange(this::invalidateLookup);
   }

   public BlockColorListSetting(String name, Supplier<List<BlockEntry>> entriesSupplier, float[] defaultColor) {
      this(name, entriesSupplier, defaultColor, false, List.of());
   }

   private BlockColorListSetting(String name, Supplier<List<BlockEntry>> entriesSupplier, float[] defaultColor, boolean colorsHidden, List<String> defaultOrder) {
      super(name);
      this.byId = new HashMap();
      this.selection = new ArrayList();
      this.selectionSet = new ReferenceOpenHashSet();
      this.selectionView = Collections.unmodifiableList(this.selection);
      this.selectionVersion = -1L;
      this.lookupVersion = -1L;
      this.currentVersion = 0L;
      this.entriesSupplier = entriesSupplier;
      this.shipped = entriesSupplier;
      this.defaultColor = defaultColor;
      this.shippedDefaultColor = copyColor(defaultColor);
      this.colorsHidden = colorsHidden;
      this.defaultOrder = defaultOrder;
      this.onChange(this::invalidateLookup);
   }

   public boolean colorsHidden() {
      return this.colorsHidden;
   }

   private List<BlockEntry> entriesInternal() {
      if (this.entries == null) {
         this.entries = new ArrayList((Collection)this.entriesSupplier.get());
         this.entriesSupplier = null;
         this.index();
      }

      return this.entries;
   }

   private void index() {
      for(BlockEntry entry : this.entries) {
         this.byId.putIfAbsent(entry.id(), entry);
      }

      for(String id : this.defaultOrder) {
         BlockEntry entry = (BlockEntry)this.byId.get(id);
         if (entry != null && entry.enabled().get()) {
            this.addToSelection(entry);
         }
      }

      for(BlockEntry entry : this.entries) {
         if (entry.enabled().get()) {
            this.addToSelection(entry);
         }
      }

      this.selectionVersion = this.currentVersion;
   }

   public List<BlockEntry> entries() {
      return Collections.unmodifiableList(this.entriesInternal());
   }

   public BlockEntry entry(String id) {
      this.entriesInternal();
      return (BlockEntry)this.byId.get(id);
   }

   public float[] defaultColor() {
      return this.defaultColor;
   }

   public void resetColors() {
      this.entriesInternal();

      for(BlockEntry original : (List)this.shipped.get()) {
         BlockEntry entry = (BlockEntry)this.byId.get(original.id());
         if (entry != null) {
            System.arraycopy(original.color(), 0, entry.color(), 0, 4);
            entry.tracerEnabled().set(original.tracerEnabled().get());
            System.arraycopy(original.tracerColor(), 0, entry.tracerColor(), 0, 4);
         }
      }

      System.arraycopy(this.shippedDefaultColor, 0, this.defaultColor, 0, 4);
      this.notifyChanged();
   }

   public List<BlockEntry> selected() {
      List<BlockEntry> all = this.entriesInternal();
      if (this.selectionVersion != this.currentVersion) {
         for(int i = this.selection.size() - 1; i >= 0; --i) {
            BlockEntry entry = (BlockEntry)this.selection.get(i);
            if (!entry.enabled().get()) {
               this.selection.remove(i);
               this.selectionSet.remove(entry);
            }
         }

         for(BlockEntry entry : all) {
            if (entry.enabled().get()) {
               this.addToSelection(entry);
            }
         }

         this.selectionVersion = this.currentVersion;
      }

      return this.selectionView;
   }

   public boolean select(BlockEntry entry) {
      this.selected();
      if (!this.addToSelection(entry)) {
         return false;
      } else {
         entry.enabled().set(true);
         this.notifyChanged();
         return true;
      }
   }

   public boolean deselect(BlockEntry entry) {
      this.selected();
      if (!this.selectionSet.remove(entry)) {
         return false;
      } else {
         for(int i = 0; i < this.selection.size(); ++i) {
            if (this.selection.get(i) == entry) {
               this.selection.remove(i);
               break;
            }
         }

         entry.enabled().set(false);
         this.notifyChanged();
         return true;
      }
   }

   public int selectAll(Iterable<BlockEntry> toAdd) {
      this.selected();
      int added = 0;

      for(BlockEntry entry : toAdd) {
         if (this.addToSelection(entry)) {
            entry.enabled().set(true);
            ++added;
         }
      }

      if (added > 0) {
         this.notifyChanged();
      }

      return added;
   }

   public int deselectAll() {
      this.selected();
      int removed = this.selection.size();
      if (removed == 0) {
         return 0;
      } else {
         for(BlockEntry entry : this.selection) {
            entry.enabled().set(false);
         }

         this.selection.clear();
         this.selectionSet.clear();
         this.notifyChanged();
         return removed;
      }
   }

   private boolean addToSelection(BlockEntry entry) {
      if (!this.selectionSet.add(entry)) {
         return false;
      } else {
         this.selection.add(entry);
         return true;
      }
   }

   public void invalidateLookup() {
      ++this.currentVersion;
   }

   private Map<String, Integer> lookup() {
      if (this.enabledLookup == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.enabledLookup;
   }

   private void rebuildLookup() {
      Map<String, Integer> map = new HashMap();
      Map<String, Integer> tracers = new HashMap();
      Reference2IntOpenHashMap<class_2248> byBlock = new Reference2IntOpenHashMap();
      Reference2IntOpenHashMap<class_2248> tracerByBlockMap = new Reference2IntOpenHashMap();

      for(BlockEntry entry : this.entriesInternal()) {
         if (entry.enabled().get()) {
            BlockEntry look = this.colorSource(entry);
            int argb = rgbaToArgb(look.color());
            map.put(entry.id(), argb);
            class_2248 block = resolveBlock(entry.id());
            if (block != null) {
               byBlock.put(block, argb);
            }

            if (look.tracerEnabled().get()) {
               int tracerArgb = rgbaToArgb(look.tracerColor());
               tracers.put(entry.id(), tracerArgb);
               if (block != null) {
                  tracerByBlockMap.put(block, tracerArgb);
               }
            }
         }
      }

      this.enabledLookup = map;
      this.tracerLookup = tracers;
      this.enabledByBlock = byBlock;
      this.tracerByBlock = tracerByBlockMap;
      this.lookupVersion = this.currentVersion;
   }

   private BlockEntry colorSource(BlockEntry entry) {
      String id = entry.id();
      if (!id.endsWith("_wall_banner")) {
         return entry;
      } else {
         Map var10000 = this.byId;
         String var10001 = id.substring(0, id.length() - "_wall_banner".length());
         BlockEntry banner = (BlockEntry)var10000.get(var10001 + "_banner");
         return banner != null && banner.enabled().get() ? banner : entry;
      }
   }

   private static class_2248 resolveBlock(String id) {
      class_2960 key = class_2960.method_12829(id);
      return key == null ? null : (class_2248)class_7923.field_41175.method_17966(key).orElse((Object)null);
   }

   private Map<String, Integer> tracerLookup() {
      if (this.tracerLookup == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.tracerLookup;
   }

   private Reference2IntOpenHashMap<class_2248> blockLookup() {
      if (this.enabledByBlock == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.enabledByBlock;
   }

   private Reference2IntOpenHashMap<class_2248> tracerBlockLookup() {
      if (this.tracerByBlock == null || this.lookupVersion != this.currentVersion) {
         this.rebuildLookup();
      }

      return this.tracerByBlock;
   }

   public boolean isEnabled(class_2248 block) {
      return this.blockLookup().containsKey(block);
   }

   public Set<class_2248> enabledBlocks() {
      return Collections.unmodifiableSet(this.blockLookup().keySet());
   }

   public int colorArgb(class_2248 block) {
      Reference2IntOpenHashMap<class_2248> map = this.blockLookup();
      return map.containsKey(block) ? map.getInt(block) : rgbaToArgb(this.defaultColor);
   }

   public boolean hasAnyEnabled() {
      return !this.lookup().isEmpty();
   }

   public Set<String> enabledIds() {
      return this.lookup().keySet();
   }

   public int tracerColorArgb(class_2248 block) {
      Reference2IntOpenHashMap<class_2248> map = this.tracerBlockLookup();
      return map.containsKey(block) ? map.getInt(block) : 0;
   }

   public boolean hasAnyTracerEnabled() {
      return !this.tracerLookup().isEmpty();
   }

   public JsonElement serialize() {
      JsonObject root = new JsonObject();
      root.add("defaultColor", colorJson(this.defaultColor));
      JsonArray arr = new JsonArray();

      for(BlockEntry e : this.selected()) {
         arr.add(entryJson(e));
      }

      for(BlockEntry e : this.entriesInternal()) {
         if (!this.selectionSet.contains(e)) {
            arr.add(entryJson(e));
         }
      }

      root.add("blocks", arr);
      return root;
   }

   private static JsonObject entryJson(BlockEntry e) {
      JsonObject obj = new JsonObject();
      obj.addProperty("id", e.id());
      obj.addProperty("enabled", e.enabled().get());
      obj.add("color", colorJson(e.color()));
      obj.addProperty("tracerEnabled", e.tracerEnabled().get());
      obj.add("tracerColor", colorJson(e.tracerColor()));
      return obj;
   }

   private static JsonArray colorJson(float[] rgba) {
      JsonArray arr = new JsonArray();

      for(float v : rgba) {
         arr.add(clamp01(v));
      }

      return arr;
   }

   public JsonElement compact(JsonElement value) {
      JsonElement defaults = this.defaultJson();
      if (value != null && value.isJsonObject() && defaults != null && defaults.isJsonObject()) {
         JsonObject root = value.getAsJsonObject();
         JsonObject shipped = defaults.getAsJsonObject();
         if (!root.has("changes") && root.has("blocks") && shipped.has("blocks")) {
            try {
               Map<String, String[]> before = new HashMap();

               for(JsonElement el : shipped.getAsJsonArray("blocks")) {
                  JsonObject obj = el.getAsJsonObject();
                  before.put(obj.get("id").getAsString(), fields(obj));
               }

               JsonObject changes = new JsonObject();

               for(JsonElement el : root.getAsJsonArray("blocks")) {
                  if (el.isJsonObject() && el.getAsJsonObject().has("id")) {
                     JsonObject obj = el.getAsJsonObject();
                     String id = obj.get("id").getAsString();
                     String[] now = fields(obj);
                     String[] was = (String[])before.get(id);
                     if (was == null || !Arrays.equals(now, was)) {
                        boolean tracer = was == null || !now[2].equals(was[2]) || !now[3].equals(was[3]);
                        StringBuilder change = new StringBuilder(now[0]);
                        if (tracer || !now[1].equals(was[1])) {
                           change.append(' ').append(now[1]);
                        }

                        if (tracer) {
                           change.append(' ').append(now[2]).append(' ').append(now[3]);
                        }

                        changes.addProperty(shortId(id), change.toString());
                     }
                  }
               }

               JsonObject out = new JsonObject();
               String color = hexColor(root.get("defaultColor"));
               if (color != null && !color.equals(hexColor(shipped.get("defaultColor")))) {
                  out.addProperty("defaultColor", color);
               }

               out.add("changes", changes);
               return out;
            } catch (RuntimeException var15) {
               return value;
            }
         } else {
            return value;
         }
      } else {
         return value;
      }
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
         readColor(root.getAsJsonArray("defaultColor"), this.defaultColor);
      }

      if (root.has("blocks")) {
         JsonArray arr = root.getAsJsonArray("blocks");
         List<BlockEntry> previous = new ArrayList(this.selected());
         this.selection.clear();
         this.selectionSet.clear();

         for(JsonElement el : arr) {
            if (el.isJsonObject()) {
               JsonObject obj = el.getAsJsonObject();
               if (obj.has("id")) {
                  BlockEntry entry = (BlockEntry)this.byId.get(obj.get("id").getAsString());
                  if (entry != null) {
                     if (obj.has("enabled")) {
                        entry.enabled().set(obj.get("enabled").getAsBoolean());
                     }

                     if (obj.has("color")) {
                        readColor(obj.getAsJsonArray("color"), entry.color());
                     }

                     if (obj.has("tracerEnabled")) {
                        entry.tracerEnabled().set(obj.get("tracerEnabled").getAsBoolean());
                     }

                     if (obj.has("tracerColor")) {
                        readColor(obj.getAsJsonArray("tracerColor"), entry.tracerColor());
                     }

                     if (entry.enabled().get()) {
                        this.addToSelection(entry);
                     }
                  }
               }
            }
         }

         for(BlockEntry entry : previous) {
            if (entry.enabled().get()) {
               this.addToSelection(entry);
            }
         }
      }

   }

   private void applyChanges(JsonObject root) {
      this.entriesInternal();
      JsonElement defaults = this.defaultJson();
      if (defaults != null && defaults.isJsonObject()) {
         this.applyFull(defaults.getAsJsonObject());
      }

      if (root.has("defaultColor")) {
         readHex(root.get("defaultColor").getAsString(), this.defaultColor);
      }

      for(Map.Entry<String, JsonElement> change : root.getAsJsonObject("changes").entrySet()) {
         BlockEntry entry = (BlockEntry)this.byId.get(fullId((String)change.getKey()));
         if (entry != null && ((JsonElement)change.getValue()).isJsonPrimitive()) {
            String[] parts = ((JsonElement)change.getValue()).getAsString().split(" ");

            float[] color;
            float[] tracerColor;
            try {
               color = parts.length >= 2 ? parseHex(parts[1]) : null;
               tracerColor = parts.length >= 4 ? parseHex(parts[3]) : null;
            } catch (NumberFormatException var10) {
               continue;
            }

            entry.enabled().set("1".equals(parts[0]));
            if (color != null) {
               System.arraycopy(color, 0, entry.color(), 0, 4);
            }

            if (tracerColor != null) {
               entry.tracerEnabled().set("1".equals(parts[2]));
               System.arraycopy(tracerColor, 0, entry.tracerColor(), 0, 4);
            }

            if (entry.enabled().get()) {
               this.addToSelection(entry);
            }
         }
      }

   }

   private static String[] fields(JsonObject obj) {
      return new String[]{flag(obj, "enabled"), String.valueOf(hexColor(obj.get("color"))), flag(obj, "tracerEnabled"), String.valueOf(hexColor(obj.get("tracerColor")))};
   }

   private static String flag(JsonObject obj, String key) {
      return obj.has(key) && obj.get(key).getAsBoolean() ? "1" : "0";
   }

   static String hexColor(JsonElement color) {
      if (color != null && color.isJsonArray()) {
         float[] rgba = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
         readColor(color.getAsJsonArray(), rgba);
         return ColorSetting.hex(rgbaToArgb(rgba));
      } else {
         return null;
      }
   }

   static void readHex(String hex, float[] into) {
      System.arraycopy(parseHex(hex), 0, into, 0, 4);
   }

   static float[] parseHex(String hex) {
      int argb = (int)Long.parseLong(hex, 16);
      return new float[]{(float)(argb >> 16 & 255) / 255.0F, (float)(argb >> 8 & 255) / 255.0F, (float)(argb & 255) / 255.0F, (float)(argb >>> 24) / 255.0F};
   }

   static String shortId(String id) {
      return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
   }

   static String fullId(String id) {
      return id.indexOf(58) < 0 ? "minecraft:" + id : id;
   }

   private static void readColor(JsonArray from, float[] into) {
      for(int i = 0; i < Math.min(4, from.size()); ++i) {
         into[i] = clamp01(from.get(i).getAsFloat());
      }

   }

   public static BlockColorListSetting createDefault() {
      return new BlockColorListSetting("Block Colors", () -> buildEntries((id) -> false), white());
   }

   public static BlockColorListSetting blockList(String name, List<String> defaults) {
      Set<String> enabled = Set.copyOf(defaults);
      return new BlockColorListSetting(name, () -> {
         Objects.requireNonNull(enabled);
         return buildEntries(enabled::contains);
      }, white(), true, List.copyOf(defaults));
   }

   private static List<BlockEntry> buildEntries(Predicate<String> enabledByDefault) {
      List<BlockEntry> e = new ArrayList();

      for(class_2248 block : class_7923.field_41175) {
         if (!block.method_9564().method_26215()) {
            class_2960 key = class_7923.field_41175.method_10221(block);
            if (key != null) {
               String id = key.toString();
               e.add(entry(id, prettify(key.method_12832()), "Blocks", enabledByDefault.test(id), 1.0F, 1.0F, 1.0F, 1.0F));
            }
         }
      }

      return e;
   }

   public static BlockColorListSetting createForBlockEntityDebug() {
      return new BlockColorListSetting("Block Colors", BlockColorListSetting::buildBlockEntityDebugEntries, white());
   }

   private static List<BlockEntry> buildBlockEntityDebugEntries() {
      List<BlockEntry> e = new ArrayList();

      for(class_2248 block : class_7923.field_41175) {
         if (!block.method_9564().method_26215()) {
            class_2960 key = class_7923.field_41175.method_10221(block);
            if (key != null) {
               String path = key.method_12832();
               int rgb = blockEntityDebugRgb(path);
               boolean enabled = rgb != -1 || blockEntityDebugPicked(path);
               e.add(entry(key.toString(), prettify(path), "Blocks", enabled, rgb == -1 ? 16777215 : rgb, true));
            }
         }
      }

      return e;
   }

   private static int blockEntityDebugRgb(String path) {
      if (path.endsWith("copper_chest")) {
         return 10246912;
      } else if (path.endsWith("shulker_box")) {
         return 8781982;
      } else {
         int var10000;
         switch (path) {
            case "chest":
               var10000 = 10246912;
               break;
            case "trapped_chest":
               var10000 = 13775410;
               break;
            case "ender_chest":
               var10000 = 7667967;
               break;
            case "barrel":
               var10000 = 6572850;
               break;
            case "hopper":
               var10000 = 5263440;
               break;
            case "dispenser":
            case "dropper":
               var10000 = 7887420;
               break;
            case "brewing_stand":
               var10000 = 7874760;
               break;
            case "furnace":
            case "blast_furnace":
            case "smoker":
               var10000 = 6579300;
               break;
            case "spawner":
               var10000 = 9076390;
               break;
            case "enchanting_table":
               var10000 = 13107200;
               break;
            case "crafter":
               var10000 = 3978400;
               break;
            case "moving_piston":
            case "piston":
            case "sticky_piston":
            case "piston_head":
               var10000 = 65280;
               break;
            case "lectern":
               var10000 = 13806160;
               break;
            case "jukebox":
               var10000 = 2017380;
               break;
            case "decorated_pot":
               var10000 = 11822120;
               break;
            case "beehive":
            case "bee_nest":
               var10000 = 15779840;
               break;
            case "beacon":
               var10000 = 61660;
               break;
            case "conduit":
               var10000 = 46320;
               break;
            case "comparator":
               var10000 = 13120050;
               break;
            default:
               var10000 = -1;
         }

         return var10000;
      }
   }

   private static boolean blockEntityDebugPicked(String path) {
      if (!path.contains("sign") && !path.endsWith("_bed") && !path.endsWith("_banner") && !path.endsWith("_skull") && !path.endsWith("_head")) {
         boolean var10000;
         switch (path) {
            case "bell":
            case "campfire":
            case "soul_campfire":
            case "jigsaw":
            case "structure_block":
            case "command_block":
            case "chain_command_block":
            case "repeating_command_block":
            case "end_portal":
            case "end_gateway":
            case "sculk_sensor":
            case "calibrated_sculk_sensor":
            case "sculk_catalyst":
            case "sculk_shrieker":
            case "suspicious_sand":
            case "suspicious_gravel":
            case "trial_spawner":
            case "vault":
            case "chiseled_bookshelf":
            case "daylight_detector":
               var10000 = true;
               break;
            default:
               var10000 = false;
         }

         return var10000;
      } else {
         return true;
      }
   }

   private static String prettify(String path) {
      String[] words = path.split("_");
      StringBuilder display = new StringBuilder();

      for(int i = 0; i < words.length; ++i) {
         if (!words[i].isEmpty()) {
            display.append(Character.toUpperCase(words[i].charAt(0)));
            display.append(words[i].substring(1));
            if (i < words.length - 1) {
               display.append(" ");
            }
         }
      }

      return display.toString();
   }

   private static float[] white() {
      return new float[]{1.0F, 1.0F, 1.0F, 1.0F};
   }

   private static BlockEntry entry(String id, String display, String cat, boolean enabled, float r, float g, float b, float a) {
      float[] color = new float[]{r, g, b, a};
      return new BlockEntry(id, display, cat, new ImBoolean(enabled), color, new ImBoolean(false), copyColor(color));
   }

   private static BlockEntry entry(String id, String display, String cat, boolean enabled, int rgb, boolean tracer) {
      float[] color = new float[]{(float)(rgb >> 16 & 255) / 255.0F, (float)(rgb >> 8 & 255) / 255.0F, (float)(rgb & 255) / 255.0F, 1.0F};
      return new BlockEntry(id, display, cat, new ImBoolean(enabled), color, new ImBoolean(tracer), copyColor(color));
   }

   private static BlockEntry copyEntry(BlockEntry e) {
      return new BlockEntry(e.id(), e.displayName(), e.category(), new ImBoolean(e.enabled().get()), copyColor(e.color()), new ImBoolean(e.tracerEnabled().get()), copyColor(e.tracerColor()));
   }

   private static float[] copyColor(float[] rgba) {
      return new float[]{rgba[0], rgba[1], rgba[2], rgba[3]};
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
   public static record BlockEntry(String id, String displayName, String category, ImBoolean enabled, float[] color, ImBoolean tracerEnabled, float[] tracerColor) {
   }
}
