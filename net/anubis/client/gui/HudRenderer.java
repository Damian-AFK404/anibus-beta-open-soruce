package net.anubis.client.gui;

import com.example.client.mixin.AccessorHudToastInstance;
import com.example.client.mixin.AccessorHudToastManager;
import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.anubis.client.config.HudTheme;
import net.anubis.client.config.Surface;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.ModuleRevision;
import net.anubis.client.module.impl.client.HotkeysModule;
import net.anubis.client.module.impl.client.HudModule;
import net.anubis.client.module.impl.client.RadarModule;
import net.anubis.client.module.impl.client.RegionMapModule;
import net.anubis.client.module.impl.client.SpotifyHudModule;
import net.anubis.client.module.impl.misc.AntiVanishModule;
import net.anubis.client.module.impl.misc.NameProtectModule;
import net.anubis.client.module.impl.misc.StaffListModule;
import net.anubis.client.util.spotify.AlbumArt;
import net.anubis.client.util.spotify.DisplayText;
import net.anubis.client.util.spotify.Lyrics;
import net.anubis.client.util.spotify.NowPlaying;
import net.anubis.client.util.spotify.PlaybackClock;
import net.anubis.client.util.spotify.SpotifyWindow;
import net.anubis.client.util.spotify.SyncedLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import net.minecraft.class_1068;
import net.minecraft.class_1074;
import net.minecraft.class_10868;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1937;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_368;
import net.minecraft.class_408;
import net.minecraft.class_634;
import net.minecraft.class_640;
import net.minecraft.class_642;
import net.minecraft.class_6880;
import net.minecraft.class_742;
import net.minecraft.class_746;
import net.minecraft.class_368.class_369;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public final class HudRenderer {
   private static final HudRenderer INSTANCE = new HudRenderer();
   private static final float CAP_H = 28.0F;
   private static final float PAD_X = 12.0F;
   private static final float GAP_UNIT = 4.0F;
   private static final float GAP_DIV = 10.0F;
   private static final float DIV_H = 12.0F;
   private static final float EMBLEM_H = 24.0F;
   static final float ANIM_SPEED = 12.0F;
   private static final float WIDTH_SPEED = 16.0F;
   private static final float INTRO_DURATION = 0.36F;
   private static final float INTRO_STAGGER = 0.04F;
   private static final float INTRO_SLIDE = 6.0F;
   private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
   private static final String MINUS = "−";
   private static final String INFINITY = "∞";
   private static final String[] SECONDS = (String[])IntStream.range(0, 60).mapToObj((i) -> String.format(":%02d", i)).toArray((x$0) -> new String[x$0]);
   private final HudLayoutStore layout = HudLayoutStore.getInstance();
   private final ModuleListRows moduleRows = new ModuleListRows();
   private final TargetCard targetCard = new TargetCard();
   private final ArmorStatus armorStatus = new ArmorStatus();
   private final CooldownTray cooldownTray = new CooldownTray();
   private final Map<String, Int2ObjectOpenHashMap<StableWidth>> slots = new HashMap();
   private int slotCount;
   private float moduleListW;
   private float toastShift;
   private boolean layoutDirty;
   private long lastWheelNanos;
   private final HudModule hudModule;
   private final RegionMapModule regionMapModule;
   private final RadarModule radarModule;
   private float introTime;
   private boolean hudVisibleLastFrame;
   private String draggingId;
   private float dragOffsetX;
   private static final SpotifyWindow.MediaKey[] SPOTIFY_BUTTONS;
   private final float[] spotifyButtons = new float[12];
   private boolean spotifyButtonsShown;
   private boolean spotifyButtonsDrawn;
   private boolean chatInput;
   private float dragOffsetY;
   private Float snapGuideX;
   private Float snapGuideY;
   private static int studioFrame;
   private static final float HIDDEN_ALPHA = 0.35F;
   private long infoSecond = Long.MIN_VALUE;
   private int infoFps = -1;
   private int infoPing = -1;
   private List<InfoItem> infoItems = List.of();
   private static final float ROWS_RADIUS = 6.0F;
   private static final int ROWS_SHADOW_LAYERS = 3;
   private static final int ROWS_TEXT = -1;
   private static final float COORD_ROW_H = 20.0F;
   private static final float COORD_PAD = 7.0F;
   private static final float COORD_ICON = 16.0F;
   private static final float COORD_GAP_ICON = 8.0F;
   private static final float COORD_GAP = 12.0F;
   private static final float COORD_ELSEWHERE_ALPHA = 0.62F;
   private static final String[] COORD_SLOTS;
   private final Map<String, Float> coordAnim = new HashMap();
   private static final float LIST_ROW_H = 18.0F;
   private static final float LIST_PAD = 6.0F;
   private static final String NO_MODULES = "No active modules";
   private static final float TOAST_GAP = 10.0F;
   private static final float POTION_ROW_H = 22.0F;
   private static final float POTION_PAD = 6.0F;
   private static final float ICON = 18.0F;
   private static final float GAP_ICON = 6.0F;
   private static final float GAP_DURATION = 14.0F;
   private static final String NO_EFFECTS = "No active effects";
   private static final float VANISH_ROW_H = 22.0F;
   private static final float VANISH_PAD = 8.0F;
   private static final float GAP_TAG = 8.0F;
   private static final String NO_VANISH = "No vanish alerts";
   private static final float STAFF_ROW_H = 22.0F;
   private static final float STAFF_HEADER_H = 20.0F;
   private static final float STAFF_PAD = 8.0F;
   private static final float STAFF_FACE = 16.0F;
   private static final float GAP_FACE = 6.0F;
   private static final float GAP_COUNT = 5.0F;
   private static final float GAP_STATE = 14.0F;
   private static final String NO_STAFF = "No staff online";
   private static final String STAFF_HEADER = "STAFF";
   private static final String WATCHING_YOU = "WATCHING YOU";
   private static final float SKIN_FACE_U = 0.125F;
   private static final float SKIN_HAT_U = 0.625F;
   private static final float SKIN_TOP_V = 0.125F;
   private static final float SKIN_PART = 0.125F;
   private final Map<UUID, class_2960> staffSkins = new HashMap();
   private final Set<GpuTexture> nearestSkins = Collections.newSetFromMap(new WeakHashMap());
   private static final float RADAR_DEFAULT_SIZE = 150.0F;
   private static final float[] RADAR_RINGS;
   private static final float RADAR_LETTER_INSET = 10.0F;
   private static final float RADAR_LETTER_CLEAR = 4.0F;
   private static final float RADAR_EDGE = 5.0F;
   private static final float RADAR_SELF = 3.0F;
   private static final float RADAR_MARKER_INSET = 1.5F;
   private static final float RADAR_MARKER_H = 5.0F;
   private static final float RADAR_MARKER_W = 3.5F;
   private static final float RADAR_DOT = 3.2F;
   private static final String[] RADAR_LETTERS;
   private static final int[] RADAR_LETTER_DX;
   private static final int[] RADAR_LETTER_DZ;
   private static final int RADAR_DOT_EDGE = -1946157056;
   private final float[] radarLetterW = new float[4];
   private float radarLetterMid;
   private float radarLetterHalf;
   private ImFont radarLetterFont;
   private int radarLetterPx = -1;
   private static final float HOTKEY_ROW_H = 22.0F;
   private static final float HOTKEY_HEADER_H = 20.0F;
   private static final float HOTKEY_PAD = 8.0F;
   private static final float GAP_HOTKEY = 16.0F;
   private static final float KEYCAP_H = 16.0F;
   private static final float KEYCAP_PAD = 5.0F;
   private static final float KEYCAP_RADIUS = 4.0F;
   private static final String HOTKEYS_HEADER = "HOTKEYS";
   private static final String NO_HOTKEYS = "No keys bound";
   private List<HotkeyLine> hotkeyLines = List.of();
   private long hotkeyRevision = Long.MIN_VALUE;
   private ImFont hotkeyFont;
   private int hotkeyTextPx = -1;
   private int hotkeyKeyPx = -1;
   private float hotkeyNameColumn;
   private float hotkeyKeyColumn;
   private float hotkeyHeaderW;
   private float hotkeyCapMid;
   private static final float SPOTIFY_PAD_Y = 8.0F;
   private static final float SPOTIFY_LINE_GAP = 2.0F;
   private static final float SPOTIFY_GLYPH = 10.0F;
   private static final float SPOTIFY_GLYPH_GAP = 10.0F;
   private static final float SPOTIFY_PAUSED = 0.62F;
   private static final float MARQUEE_SPEED = 30.0F;
   private static final float MARQUEE_HOLD = 2.4F;
   private static final float MARQUEE_GAP = 40.0F;
   private static final String ELLIPSIS = "...";
   private static final float CARD_PAD = 10.0F;
   private static final float CARD_MIN_WIDTH = 200.0F;
   private static final float CARD_DEFAULT_WIDTH = 320.0F;
   private static final float ART_GAP = 10.0F;
   private static final float ART_MIN_COLUMN = 128.0F;
   private static final float ART_RADIUS = 4.0F;
   private static final int ART_PLACEHOLDER = 318767103;
   private static final float LABEL_GAP = 3.0F;
   private static final float ARTIST_GAP = 1.0F;
   private static final float TRANSPORT_GAP = 7.0F;
   private static final float PROGRESS_GAP = 6.0F;
   private static final float PROGRESS_H = 2.0F;
   private static final float SKIP_GLYPH = 8.0F;
   private static final float PLAY_GLYPH = 10.0F;
   private static final float GLYPH_SPACING = 14.0F;
   private static final float GLYPH_MARGIN = 8.0F;
   private static final float LYRICS_GAP = 12.0F;
   private static final float LYRIC_ROW_GAP = 6.0F;
   private static final float LYRIC_LINE_GAP = 6.0F;
   private static final float LYRIC_SPRING = 13.0F;
   private static final float LYRIC_SETTLE_TIME = 1.1F;
   private static final float LYRIC_STAGGER = 0.045F;
   private static final int LYRIC_MAX_STAGGERED = 6;
   private static final int LYRIC_MAX_SHIFTS = 8;
   private static final float LYRIC_GLOW_TIME = 0.35F;
   private static final float LYRIC_ARRIVE_TIME = 0.3F;
   private static final float LYRIC_FADE_TIME = 0.32F;
   private static final float LYRIC_RISE = 8.0F;
   private static final int LYRIC_MAX_ROWS = 4;
   private static final float LYRIC_GROW_SPEED = 14.0F;
   private static final float LYRIC_SHRINK_SPEED = 9.0F;
   private static final float LYRIC_FAR_ALPHA = 0.6F;
   private static final float LYRIC_PAUSED_ALPHA = 0.7F;
   private static final float LYRIC_PAUSE_SPEED = 8.0F;
   private static final float LYRIC_BREATH_PERIOD = 1.6F;
   private static final long LYRIC_LEAD_MS = 200L;
   private static final long LYRIC_SWEEP_MS = 180L;
   private static final float LYRIC_SLOW_PACE = 2.0F;
   private static final float LYRIC_CHAR_MS = 90.0F;
   private static final float LYRIC_LIT_ALL = Float.POSITIVE_INFINITY;
   private static final long LYRIC_QUIET_MS = 700L;
   private static final float LYRIC_MIN_COVERAGE = 0.75F;
   private static final String[] NO_ROWS;
   private static final NowPlaying SAMPLE_TRACK;
   private static final Lyrics SAMPLE_LYRICS;
   private float marqueeTime;
   private String marqueeTitle = "";
   private String marqueeArtist = "";
   private final PlaybackClock spotifyClock = new PlaybackClock();
   private NowPlaying spotifyFolded;
   private String spotifyTitle = "";
   private String spotifyArtist = "";
   private AlbumArt spotifyArt;
   private ThemeImages.Texture spotifyArtTexture;
   private final Map<Long, Float> capTops = new HashMap();
   private Lyrics lyricPrepared;
   private String[] lyricTexts;
   private String[][] lyricRows;
   private int lyricRowsPx;
   private int lyricRowsWidth;
   private LyricWords[] lyricWords;
   private float lyricCharMs;
   private boolean lyricReadable;
   private float lyricViewH;
   private int lyricViewLines;
   private float lyricViewScale;
   private SyncedLyrics lyricSource;
   private int lyricStart;
   private final List<LyricShift> lyricShifts;
   private float lyricClock;
   private List<LyricItem> lyricFadeItems;
   private float lyricFade;
   private List<LyricItem> lyricLastItems;
   private List<LyricItem> lyricLeaveItems;
   private float lyricLeave;
   private float[] lyricShown;
   private float lyricBreath;
   private float lyricPlaying;
   private int lyricCurrentFirst;
   private int lyricCurrentLast;
   private float[] lyricBright;
   private int lyricBrightLo;
   private int lyricBrightHi;
   private float lyricBlockH;
   private long lyricLoadingSince;
   private ImFont glyphFont;
   private final Map<Integer, Boolean> glyphs;
   private static final float SNAP_THRESHOLD = 6.0F;

   private HudRenderer() {
      this.lyricTexts = NO_ROWS;
      this.lyricRows = new String[0][];
      this.lyricWords = new LyricWords[0];
      this.lyricCharMs = 90.0F;
      this.lyricReadable = true;
      this.lyricViewH = -1.0F;
      this.lyricShifts = new ArrayList(8);
      this.lyricFade = 1.0F;
      this.lyricLastItems = List.of();
      this.lyricShown = new float[0];
      this.lyricPlaying = 1.0F;
      this.lyricCurrentFirst = -1;
      this.lyricCurrentLast = -1;
      this.lyricBright = new float[0];
      this.lyricBrightHi = -1;
      this.glyphs = new HashMap();
      this.hudModule = (HudModule)ModuleRegistry.get(HudModule.class);
      this.regionMapModule = (RegionMapModule)ModuleRegistry.get(RegionMapModule.class);
      this.radarModule = (RadarModule)ModuleRegistry.get(RadarModule.class);
   }

   public static HudRenderer getInstance() {
      return INSTANCE;
   }

   static void markStudioPreview() {
      studioFrame = ImGuiManager.getInstance().frameCount();
   }

   static boolean studioPreview() {
      int age = ImGuiManager.getInstance().frameCount() - studioFrame;
      return age == 0 || age == 1;
   }

   boolean isDragging() {
      return this.draggingId != null;
   }

   void studioInput(float screenW, float screenH, boolean blocked) {
      markStudioPreview();
      if (!blocked || this.draggingId != null) {
         this.updateEditorInput(screenW, screenH);
      }
   }

   public void render() {
      class_310 client = class_310.method_1551();
      if (client.field_1687 == null) {
         this.releaseSpotifyArt();
         this.resetCombatElements();
      } else {
         boolean editorScreen = client.field_1755 instanceof HudEditorScreen;
         boolean preview = studioPreview();
         boolean editMode = editorScreen || preview;
         if (client.field_1755 instanceof ImGuiScreen && !preview) {
            if (this.draggingId != null || this.layoutDirty) {
               this.saveLayout();
            }

            this.draggingId = null;
            this.snapGuideX = null;
            this.snapGuideY = null;
            this.hudVisibleLastFrame = false;
         } else {
            boolean chatEditMode = client.field_1755 instanceof class_408;
            boolean acceptInput = editorScreen || chatEditMode;
            this.chatInput = chatEditMode;
            this.spotifyButtonsShown = this.spotifyButtonsDrawn;
            this.spotifyButtonsDrawn = false;
            boolean showHudOverlays = shouldRenderHudOverlays(client);
            float dt = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
            float screenW = ImGui.getIO().getDisplaySizeX();
            float screenH = ImGui.getIO().getDisplaySizeY();
            this.flushLayoutIfIdle();
            boolean hasHudOverlays = computeHudOverlays();
            boolean regionMapEnabled = this.isRegionMapEnabled();
            HudLayoutStore.HudElementState regionMap = this.layout.element("region_map");
            boolean regionMapShown = regionMapEnabled && regionMap.visible;
            boolean hudVisible = this.hudModule != null && (editMode || this.hudModule.isEnabled());
            if (!hudVisible) {
               this.hudVisibleLastFrame = false;
               this.introTime = 0.0F;
               this.clearSlots();
               this.moduleListW = 0.0F;
               this.toastShift = 0.0F;
               this.releaseSpotifyArt();
               this.resetCombatElements();
               if ((hasHudOverlays || regionMapShown) && showHudOverlays) {
                  this.drawOverlayOnly(screenW, screenH, chatEditMode, regionMapShown);
               }

            } else {
               boolean showPing = editMode || this.hudModule.ping();
               boolean showFps = editMode || this.hudModule.fps();
               boolean showClock = editMode || this.hudModule.clock();
               boolean shadow = this.hudModule.shadow();
               this.updateIntro(editMode, dt);
               this.moduleRows.update(ModuleRegistry.enabled(), editMode, dt);
               if (this.slotCount > 256) {
                  this.clearSlots();
               }

               HudLayoutStore.HudElementState infoBar = this.layout.element("info_bar");
               HudLayoutStore.HudElementState coordinates = this.layout.element("coordinates");
               HudLayoutStore.HudElementState moduleList = this.layout.element("module_list");
               HudLayoutStore.HudElementState potionTracker = this.layout.element("potion_tracker");
               HudLayoutStore.HudElementState antiVanish = this.layout.element("anti_vanish");
               HudLayoutStore.HudElementState staffList = this.layout.element("staff_list");
               HudLayoutStore.HudElementState spotify = this.layout.element("spotify");
               HudLayoutStore.HudElementState radar = this.layout.element("radar");
               HudLayoutStore.HudElementState hotkeys = this.layout.element("hotkeys");
               HudLayoutStore.HudElementState target = this.layout.element("target_card");
               HudLayoutStore.HudElementState armor = this.layout.element("armor_status");
               HudLayoutStore.HudElementState cooldowns = this.layout.element("item_cooldowns");
               InfoPlan infoPlan = this.planInfoBar(infoBar, this.buildInfoItems(showFps, showClock, showPing, client), dt);
               this.layout.updateBounds(infoBar.id, infoPlan.width(), infoPlan.height(), screenW, screenH);
               CoordPlan coordsPlan = this.planCoordinates(coordinates, this.coordinateRows(client, editMode, dt), editMode, dt);
               this.layout.updateBounds(coordinates.id, coordsPlan.width(), coordsPlan.height(), screenW, screenH);
               ListPlan listPlan = this.planModuleList(moduleList, editMode, dt);
               this.layout.updateBounds(moduleList.id, listPlan.width(), listPlan.height(), screenW, screenH);
               PotionPlan potionPlan = this.planPotions(potionTracker, this.buildPotionRows(client), editMode, dt);
               this.layout.updateBounds(potionTracker.id, potionPlan.width(), potionPlan.height(), screenW, screenH);
               VanishPlan vanishPlan = this.planVanish(antiVanish, editMode, dt);
               this.layout.updateBounds(antiVanish.id, vanishPlan.width(), vanishPlan.height(), screenW, screenH);
               StaffPlan staffPlan = this.planStaff(staffList, editMode, dt);
               this.layout.updateBounds(staffList.id, staffPlan.width(), staffPlan.height(), screenW, screenH);
               SpotifyPlan spotifyPlan = this.planSpotify(spotify, editMode, dt);
               this.layout.updateBounds(spotify.id, spotifyPlan.width(), spotifyPlan.height(), screenW, screenH);
               float radarSize = this.planRadar(radar, editMode);
               this.layout.updateBounds(radar.id, radarSize, radarSize, screenW, screenH);
               HotkeyPlan hotkeyPlan = this.planHotkeys(hotkeys, editMode, dt);
               this.layout.updateBounds(hotkeys.id, hotkeyPlan.width(), hotkeyPlan.height(), screenW, screenH);
               boolean combatHud = this.hudModule.isEnabled() && !client.field_1690.field_1842;
               boolean targetWanted = combatHud && target.visible && this.hudModule.targetCard();
               boolean armorWanted = combatHud && armor.visible && this.hudModule.armorStatus();
               boolean cooldownWanted = combatHud && cooldowns.visible && this.hudModule.itemCooldowns();
               float targetScale = scaleOf(target);
               float armorScale = scaleOf(armor);
               float cooldownScale = scaleOf(cooldowns);
               this.targetCard.update(client, this.hudModule, targetWanted, editMode, dt);
               this.layout.updateBounds(target.id, this.targetCard.planWidth(targetScale), this.targetCard.planHeight(targetScale), screenW, screenH);
               this.armorStatus.update(client, this.hudModule, armorWanted, editMode, dt);
               this.layout.updateBounds(armor.id, this.armorStatus.planWidth(armorScale), this.armorStatus.planHeight(armorScale), screenW, screenH);
               this.cooldownTray.update(client, this.hudModule, cooldownWanted, editMode, dt);
               this.layout.updateBounds(cooldowns.id, this.cooldownTray.planWidth(cooldownScale), this.cooldownTray.planHeight(cooldownScale), screenW, screenH);
               if (regionMapEnabled || editMode) {
                  this.updateRegionMapBounds(screenW, screenH);
               }

               if (acceptInput) {
                  this.updateEditorInput(screenW, screenH);
               } else if (!preview) {
                  if (this.draggingId != null || this.layoutDirty) {
                     this.saveLayout();
                  }

                  this.draggingId = null;
                  this.snapGuideX = null;
                  this.snapGuideY = null;
               }

               float listShift = this.updateToastShift(client, moduleList, editMode, dt);
               ImGui.setNextWindowPos(0.0F, 0.0F, 1);
               ImGui.setNextWindowSize(screenW, screenH);
               int flags = 205215;
               if (!acceptInput) {
                  flags |= 197120;
               }

               ImGui.pushStyleVar(4, 0.0F);
               if (!ImGui.begin("##hud_overlay", flags)) {
                  ImGui.end();
                  ImGui.popStyleVar();
               } else {
                  ImGui.popStyleVar();
                  ImDrawList dl = ImGui.getWindowDrawList();
                  if (showHudOverlays) {
                     drawHudOverlays(dl, screenW, screenH);
                  }

                  boolean hudOn = this.hudModule.isEnabled();
                  boolean infoBarShown = hudOn && infoBar.visible && (this.hudModule.fps() || this.hudModule.clock() || this.hudModule.ping());
                  boolean coordsShown = hudOn && coordinates.visible;
                  boolean listShown = hudOn && moduleList.visible && this.hudModule.moduleList();
                  boolean potionsShown = hudOn && potionTracker.visible && this.hudModule.potions();
                  boolean vanishShown = hudOn && antiVanish.visible && AntiVanishModule.shouldShowHud();
                  boolean staffShown = hudOn && staffList.visible && StaffListModule.shouldShowHud();
                  boolean spotifyShown = hudOn && spotify.visible && SpotifyHudModule.shouldShowHud();
                  boolean gameHidden = gameHidesHud(client);
                  boolean radarShown = hudOn && radar.visible && RadarModule.shouldShowHud() && !gameHidden;
                  boolean hotkeysShown = hudOn && hotkeys.visible && HotkeysModule.shouldShowHud() && !gameHidden;
                  if (infoBarShown && !infoPlan.segments().isEmpty() || editMode) {
                     this.drawInfoBar(dl, infoBar, infoPlan, shadow, this.transform(infoBar, editMode, infoBarShown));
                  }

                  if (coordsShown && !coordsPlan.rows().isEmpty() || editMode) {
                     this.drawCoordinates(dl, coordinates, coordsPlan, shadow, this.transform(coordinates, editMode, coordsShown), screenW);
                  }

                  if (listShown && !listPlan.rows().isEmpty() || editMode) {
                     this.drawModuleList(dl, moduleList, listPlan, shadow, this.transform(moduleList, editMode, listShown).down(listShift), screenW);
                  }

                  if (potionsShown && !potionPlan.lines().isEmpty() || editMode) {
                     this.drawPotions(dl, potionTracker, potionPlan, shadow, this.transform(potionTracker, editMode, potionsShown));
                  }

                  if (vanishShown && !vanishPlan.lines().isEmpty() || editMode) {
                     this.drawVanish(dl, antiVanish, vanishPlan, shadow, this.transform(antiVanish, editMode, vanishShown));
                  }

                  if (staffShown || editMode) {
                     this.drawStaff(dl, staffList, staffPlan, shadow, this.transform(staffList, editMode, staffShown));
                  }

                  if (spotifyShown || editMode) {
                     this.drawSpotify(dl, spotify, spotifyPlan, shadow, this.transform(spotify, editMode, spotifyShown));
                  }

                  if (radarShown || editMode) {
                     this.drawRadar(dl, radar, radarSize, client, shadow, this.transform(radar, editMode, radarShown));
                  }

                  if (hotkeysShown && !hotkeyPlan.lines().isEmpty() || editMode) {
                     this.drawHotkeys(dl, hotkeys, hotkeyPlan, shadow, this.transform(hotkeys, editMode, hotkeysShown));
                  }

                  if (this.targetCard.visible()) {
                     DrawTransform tr = this.transform(target, editMode, targetWanted);
                     this.targetCard.draw(dl, target, (float)Math.round(target.x + tr.xOffset()), (float)Math.round(target.y + tr.yOffset()), tr.alpha(), shadow, this.hudModule);
                  }

                  if (this.armorStatus.visible()) {
                     DrawTransform tr = this.transform(armor, editMode, armorWanted);
                     this.armorStatus.draw(dl, armor, (float)Math.round(armor.x + tr.xOffset()), (float)Math.round(armor.y + tr.yOffset()), tr.alpha(), shadow, client);
                  }

                  if (this.cooldownTray.visible()) {
                     DrawTransform tr = this.transform(cooldowns, editMode, cooldownWanted);
                     this.cooldownTray.draw(dl, cooldowns, (float)Math.round(cooldowns.x + tr.xOffset()), (float)Math.round(cooldowns.y + tr.yOffset()), tr.alpha(), shadow, this.hudAccent());
                  }

                  if (regionMapShown || editMode) {
                     RegionMapRenderer.render(dl, this.regionMapModule, regionMap, editMode && !regionMapShown ? 0.35F : 1.0F);
                  }

                  if (editorScreen) {
                     this.drawEditorChrome(dl, screenW, screenH);
                  } else if (preview) {
                     this.drawSnapGuides(dl, screenW, screenH);
                  }

                  ImGui.end();
               }
            }
         }
      }
   }

   private DrawTransform transform(HudLayoutStore.HudElementState state, boolean editMode, boolean shownInGame) {
      DrawTransform tr = this.introTransform(state, editMode);
      return editMode && !shownInGame ? new DrawTransform(tr.xOffset(), tr.yOffset(), tr.alpha() * 0.35F) : tr;
   }

   private void resetCombatElements() {
      this.targetCard.reset();
      this.armorStatus.reset();
      this.cooldownTray.reset();
   }

   private void saveLayout() {
      this.layout.save();
      this.layoutDirty = false;
   }

   private void flushLayoutIfIdle() {
      if (this.layoutDirty && System.nanoTime() - this.lastWheelNanos > 400000000L) {
         this.saveLayout();
      }

   }

   private void drawOverlayOnly(float screenW, float screenH, boolean acceptInput, boolean drawRegionMap) {
      if (drawRegionMap) {
         this.updateRegionMapBounds(screenW, screenH);
      }

      ImGui.setNextWindowPos(0.0F, 0.0F, 1);
      ImGui.setNextWindowSize(screenW, screenH);
      int flags = 205215;
      if (!acceptInput) {
         flags |= 197120;
      }

      ImGui.pushStyleVar(4, 0.0F);
      if (!ImGui.begin("##anubis_overlay_only", flags)) {
         ImGui.end();
         ImGui.popStyleVar();
      } else {
         ImGui.popStyleVar();
         if (acceptInput) {
            this.updateEditorInput(screenW, screenH);
         } else {
            if (this.draggingId != null || this.layoutDirty) {
               this.saveLayout();
            }

            this.draggingId = null;
         }

         ImDrawList dl = ImGui.getWindowDrawList();
         drawHudOverlays(dl, screenW, screenH);
         if (drawRegionMap && this.regionMapModule != null) {
            RegionMapRenderer.render(dl, this.regionMapModule, this.layout.element("region_map"));
         }

         ImGui.end();
      }
   }

   private boolean isRegionMapEnabled() {
      return this.regionMapModule != null && this.regionMapModule.isEnabled();
   }

   private void updateRegionMapBounds(float screenW, float screenH) {
      if (this.regionMapModule != null) {
         HudLayoutStore.HudElementState state = this.layout.element("region_map");
         RegionMapRenderer.Size size = RegionMapRenderer.measureSize(this.regionMapModule, scaleOf(state));
         this.layout.updateBounds("region_map", size.width(), size.height(), screenW, screenH);
      }
   }

   public boolean hasWork() {
      class_310 client = class_310.method_1551();
      if (client.field_1687 == null) {
         this.resetCombatElements();
         return false;
      } else {
         return client.field_1755 instanceof HudEditorScreen || studioPreview() || this.hudModule != null && (this.hudModule.isEnabled() || this.hudVisibleLastFrame) || this.isRegionMapEnabled() || computeHudOverlays();
      }
   }

   private static boolean shouldRenderHudOverlays(class_310 client) {
      return client.field_1755 == null || client.field_1755 instanceof class_408;
   }

   private static boolean gameHidesHud(class_310 client) {
      return client.field_1690.field_1842 || client.field_61504.method_72776();
   }

   private static boolean computeHudOverlays() {
      for(HudOverlayProvider provider : ModuleRegistry.instancesOf(HudOverlayProvider.class)) {
         if (provider.isHudOverlayEnabled()) {
            return true;
         }
      }

      return false;
   }

   private static void drawHudOverlays(ImDrawList dl, float screenW, float screenH) {
      for(HudOverlayProvider provider : ModuleRegistry.instancesOf(HudOverlayProvider.class)) {
         if (provider.isHudOverlayEnabled()) {
            provider.renderHudOverlay(dl, screenW, screenH);
         }
      }

   }

   private void updateIntro(boolean editMode, float dt) {
      float end = 0.56F;
      if (editMode) {
         this.introTime = end;
         this.hudVisibleLastFrame = true;
      } else if (!this.hudVisibleLastFrame) {
         this.introTime = 0.0F;
         this.hudVisibleLastFrame = true;
      } else {
         this.introTime = Math.min(this.introTime + dt, end);
      }
   }

   private DrawTransform introTransform(HudLayoutStore.HudElementState state, boolean editMode) {
      if (editMode) {
         return HudRenderer.DrawTransform.identity();
      } else {
         float t = clamp01((this.introTime - introDelay(state.id)) / 0.36F);
         float eased = easeOutCubic(t);
         float slide = 6.0F * scaleOf(state) * (1.0F - eased);
         DrawTransform var10000;
         switch (state.anchorX) {
            case 1 -> var10000 = new DrawTransform(0.0F, -slide, eased);
            case 2 -> var10000 = new DrawTransform(slide, 0.0F, eased);
            default -> var10000 = new DrawTransform(-slide, 0.0F, eased);
         }

         return var10000;
      }
   }

   private static float introDelay(String id) {
      float var10000;
      switch (id) {
         case "info_bar" -> var10000 = 0.04F;
         case "coordinates" -> var10000 = 0.08F;
         case "anti_vanish" -> var10000 = 0.12F;
         case "spotify" -> var10000 = 0.12F;
         case "module_list" -> var10000 = 0.16F;
         case "staff_list" -> var10000 = 0.16F;
         case "hotkeys" -> var10000 = 0.16F;
         case "radar" -> var10000 = 0.19999999F;
         case "potion_tracker" -> var10000 = 0.19999999F;
         case "armor_status" -> var10000 = 0.16F;
         case "item_cooldowns" -> var10000 = 0.12F;
         case "target_card" -> var10000 = 0.08F;
         default -> var10000 = 0.0F;
      }

      return var10000;
   }

   private float slot(String key, int px, float target, float dt) {
      Int2ObjectOpenHashMap<StableWidth> bySize = (Int2ObjectOpenHashMap)this.slots.computeIfAbsent(key, (k) -> new Int2ObjectOpenHashMap());
      StableWidth width = (StableWidth)bySize.get(px);
      if (width == null) {
         width = new StableWidth();
         bySize.put(px, width);
         ++this.slotCount;
      }

      return width.update(target, dt);
   }

   private void removeSlot(String key, int px) {
      Int2ObjectOpenHashMap<StableWidth> bySize = (Int2ObjectOpenHashMap)this.slots.get(key);
      if (bySize != null && bySize.remove(px) != null) {
         --this.slotCount;
      }

   }

   private void clearSlots() {
      this.slots.clear();
      this.slotCount = 0;
   }

   private static String number(long value) {
      return value < 0L ? "−" + -value : Long.toString(value);
   }

   private List<InfoItem> buildInfoItems(boolean showFps, boolean showClock, boolean showPing, class_310 client) {
      long second = showClock ? System.currentTimeMillis() / 1000L : -1L;
      int fps = showFps ? Math.max(0, client.method_47599()) : -1;
      int ping = -1;
      if (showPing) {
         class_634 conn = client.method_1562();
         int latency = 0;
         if (conn != null && client.field_1724 != null) {
            latency = readTabLatency(client, conn);
            if (latency <= 0) {
               latency = readServerPing(conn);
            }
         }

         ping = Math.max(0, latency);
      }

      if (second == this.infoSecond && fps == this.infoFps && ping == this.infoPing) {
         return this.infoItems;
      } else {
         this.infoSecond = second;
         this.infoFps = fps;
         this.infoPing = ping;
         List<InfoItem> items = new ArrayList(3);
         if (showClock) {
            LocalTime now = LocalTime.now();
            items.add(new InfoItem("info.time", now.format(CLOCK_FORMAT), SECONDS[now.getSecond()], (String)null));
         }

         if (showFps) {
            items.add(new InfoItem("info.fps", String.valueOf(fps), (String)null, "fps"));
         }

         if (showPing) {
            items.add(new InfoItem("info.ping", String.valueOf(ping), (String)null, "ms"));
         }

         this.infoItems = List.copyOf(items);
         return this.infoItems;
      }
   }

   private InfoPlan planInfoBar(HudLayoutStore.HudElementState state, List<InfoItem> items, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      List<Segment> segments = new ArrayList(items.size());
      float total = 12.0F * s * 2.0F;

      for(int i = 0; i < items.size(); ++i) {
         InfoItem item = (InfoItem)items.get(i);
         float valueW = HudChrome.tabularWidth(t.value(), t.valuePx(), item.value());
         float dimW = item.dim() != null ? HudChrome.tabularWidth(t.value(), t.valuePx(), item.dim()) : 0.0F;
         float content = valueW + dimW;
         if (item.unit() != null) {
            content += 4.0F * s + HudChrome.width(t.label(), t.labelPx(), item.unit());
         }

         float slotW = this.slot(item.slot(), t.valuePx(), (float)Math.ceil((double)content), dt);
         segments.add(new Segment(item, valueW, dimW, content, slotW));
         total += slotW;
      }

      if (segments.size() > 1) {
         total += (float)(segments.size() - 1) * (10.0F * s * 2.0F + 1.0F);
      }

      float emblemW = 0.0F;
      ThemeImages.Texture emblem = this.hudModule != null && this.hudModule.logo() && !segments.isEmpty() ? HudEmblem.forHeight(Math.round(24.0F * s)) : null;
      if (emblem != null) {
         emblemW = (float)emblem.width();
         total += emblemW + 10.0F * s * 2.0F + 1.0F;
      }

      return new InfoPlan(segments, t, (float)Math.round(total), (float)Math.round(28.0F * s), emblemW);
   }

   private void drawInfoBar(ImDrawList dl, HudLayoutStore.HudElementState state, InfoPlan plan, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Surface surface = hudSurface(state);
         boolean guard = HudChrome.needsTextGuard(surface);
         HudChrome.card(dl, surface, x, y, w, h, hudRounding() * s, a, shadow, s);
         Type t = plan.type();
         float valueTop = y + (float)Math.floor((double)((h - (float)t.valuePx()) * 0.5F));
         float labelTop = valueTop + HudChrome.baselineOffset(t.value(), t.valuePx(), t.label(), t.labelPx());
         float cx = x + 12.0F * s;
         ThemeImages.Texture emblem = plan.emblemW() > 0.0F ? HudEmblem.forHeight(Math.round(24.0F * s)) : null;
         if (emblem != null) {
            float ex = (float)Math.round(cx);
            float ey = (float)Math.round(y + (h - (float)emblem.height()) * 0.5F);
            dl.addImage((long)emblem.id(), ex, ey, ex + (float)emblem.width(), ey + (float)emblem.height(), 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(this.hudAccent(), a));
            cx += plan.emblemW();
            HudChrome.divider(dl, cx + 10.0F * s, y + h * 0.5F, 12.0F * s, a);
            cx += 10.0F * s * 2.0F + 1.0F;
         }

         List<Segment> segments = plan.segments();

         for(int i = 0; i < segments.size(); ++i) {
            Segment seg = (Segment)segments.get(i);
            InfoItem item = seg.item();
            float vx = item.unit() != null ? cx + seg.slotW() - seg.contentW() : cx;
            HudChrome.tabular(dl, t.value(), t.valuePx(), vx, valueTop, HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, a), item.value(), guard);
            vx += seg.valueW();
            if (item.dim() != null) {
               HudChrome.tabular(dl, t.value(), t.valuePx(), vx, valueTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), item.dim(), guard);
               vx += seg.dimW();
            }

            if (item.unit() != null) {
               HudChrome.text(dl, t.label(), t.labelPx(), vx + 4.0F * s, labelTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), item.unit(), guard);
            }

            cx += seg.slotW();
            if (i < segments.size() - 1) {
               HudChrome.divider(dl, cx + 10.0F * s, y + h * 0.5F, 12.0F * s, a);
               cx += 10.0F * s * 2.0F + 1.0F;
            }
         }

      }
   }

   private static float rowsRadius(float s) {
      return Math.min(hudRounding(), 6.0F) * s;
   }

   private void drawRows(ImDrawList dl, List<Step> steps, Surface surface, float x, float y, float w, float h, float radius, float s, boolean shadow) {
      if (shadow) {
         drawRowsShadow(dl, steps, radius, s);
      }

      int fill = GuiPalette.fill(surface);

      for(int i = 0; i < steps.size(); ++i) {
         Step q = (Step)steps.get(i);
         Step up = i > 0 ? (Step)steps.get(i - 1) : null;
         Step dn = i < steps.size() - 1 ? (Step)steps.get(i + 1) : null;
         int corners = convexCorners(q, up, dn);
         int color = HudChrome.scaleAlpha(fill, q.alpha());
         if (corners == 256) {
            dl.addRectFilled(q.l(), q.t(), q.r(), q.b(), color);
         } else {
            dl.addRectFilled(q.l(), q.t(), q.r(), q.b(), color, radius, corners);
         }

         SurfaceBackground.drawPart(dl, surface, x, y, w, h, q.l(), q.t(), q.r() - q.l(), q.b() - q.t(), q.alpha(), radius, corners);
         float stepH = q.b() - q.t();
         if (up != null && up.l() < q.l() - 0.5F) {
            fillet(dl, q.l(), q.t(), -1, 1, Math.min(radius, Math.min(q.l() - up.l(), stepH)), color);
         }

         if (up != null && up.r() > q.r() + 0.5F) {
            fillet(dl, q.r(), q.t(), 1, 1, Math.min(radius, Math.min(up.r() - q.r(), stepH)), color);
         }

         if (dn != null && dn.l() < q.l() - 0.5F) {
            fillet(dl, q.l(), q.b(), -1, -1, Math.min(radius, Math.min(q.l() - dn.l(), stepH)), color);
         }

         if (dn != null && dn.r() > q.r() + 0.5F) {
            fillet(dl, q.r(), q.b(), 1, -1, Math.min(radius, Math.min(dn.r() - q.r(), stepH)), color);
         }
      }

   }

   private static int convexCorners(Step q, Step up, Step dn) {
      int flags = 0;
      if (up == null || up.l() > q.l() + 0.5F) {
         flags |= 16;
      }

      if (up == null || up.r() < q.r() - 0.5F) {
         flags |= 32;
      }

      if (dn == null || dn.l() > q.l() + 0.5F) {
         flags |= 64;
      }

      if (dn == null || dn.r() < q.r() - 0.5F) {
         flags |= 128;
      }

      return flags == 0 ? 256 : flags;
   }

   private static void fillet(ImDrawList dl, float px, float py, int dirX, int dirY, float r, int color) {
      if (!(r < 1.0F)) {
         float cx = px + (float)dirX * r;
         float cy = py + (float)dirY * r;
         float a0 = dirX < 0 ? 0.0F : (float)Math.PI;
         float a1 = dirY > 0 ? (dirX < 0 ? (-(float)Math.PI / 2F) : ((float)Math.PI * 1.5F)) : ((float)Math.PI / 2F);
         dl.pathClear();
         dl.pathLineTo(px, py);
         dl.pathArcTo(cx, cy, r, a0, a1, 8);
         dl.pathFillConcave(color);
      }
   }

   private static void drawRowsShadow(ImDrawList dl, List<Step> steps, float radius, float s) {
      for(int k = 3; k >= 1; --k) {
         float spread = (float)k * Math.max(1.0F, s);

         for(int i = 0; i < steps.size(); ++i) {
            Step q = (Step)steps.get(i);
            boolean first = i == 0;
            boolean last = i == steps.size() - 1;
            int corners = convexCorners(q, first ? null : (Step)steps.get(i - 1), last ? null : (Step)steps.get(i + 1));
            float top = q.t() - (first ? spread : 0.0F);
            float bottom = q.b() + (last ? spread + Math.max(1.0F, s) : 0.0F);
            int color = HudChrome.scaleAlpha(218103808, q.alpha());
            if (corners == 256) {
               dl.addRectFilled(q.l() - spread, top, q.r() + spread, bottom, color);
            } else {
               dl.addRectFilled(q.l() - spread, top, q.r() + spread, bottom, color, radius + spread, corners);
            }
         }
      }

   }

   private static boolean alignsRight(HudLayoutStore.HudElementState state, float screenW) {
      boolean var10000;
      switch (state.anchorX) {
         case 0 -> var10000 = false;
         case 2 -> var10000 = true;
         default -> var10000 = state.x + state.width * 0.5F >= screenW * 0.5F;
      }

      return var10000;
   }

   private List<CoordRow> coordinateRows(class_310 client, boolean editMode, float dt) {
      long x = 0L;
      long y = 0L;
      long z = 0L;
      if (client.field_1724 != null) {
         x = (long)Math.floor(client.field_1724.method_23317());
         y = (long)Math.floor(client.field_1724.method_23318());
         z = (long)Math.floor(client.field_1724.method_23321());
      }

      boolean inNether = client.field_1687 != null && client.field_1687.method_27983() == class_1937.field_25180;
      boolean inEnd = client.field_1687 != null && client.field_1687.method_27983() == class_1937.field_25181;
      boolean overworldOn = this.hudModule.coordinates();
      boolean netherOn = this.hudModule.netherCoordinates();
      float k = 1.0F - (float)Math.exp((double)(-12.0F * dt));
      List<CoordRow> rows = new ArrayList(2);
      if (inEnd) {
         this.addCoordRow(rows, "end", "end_stone", true, overworldOn || netherOn, editMode, new long[]{x, y, z}, k);
      } else {
         long[] overworld = inNether ? new long[]{x * 8L, y, z * 8L} : new long[]{x, y, z};
         long[] nether = inNether ? new long[]{x, y, z} : new long[]{Math.floorDiv(x, 8), y, Math.floorDiv(z, 8)};
         this.addCoordRow(rows, "overworld", "grass_block_side", !inNether, overworldOn, editMode, overworld, k);
         this.addCoordRow(rows, "nether", "netherrack", inNether, netherOn, editMode, nether, k);
      }

      return rows;
   }

   private void addCoordRow(List<CoordRow> rows, String key, String block, boolean here, boolean enabled, boolean editMode, long[] xyz, float k) {
      float target = !enabled && !editMode ? 0.0F : 1.0F;
      float anim = (Float)this.coordAnim.getOrDefault(key, target);
      anim += (target - anim) * k;
      if (Math.abs(anim - target) < 0.005F) {
         anim = target;
      }

      this.coordAnim.put(key, anim);
      if (!(anim <= 0.005F)) {
         rows.add(new CoordRow(block, here, enabled, new String[]{number(xyz[0]), number(xyz[1]), number(xyz[2])}, anim));
      }
   }

   private static float coordIconSize(float s) {
      return s >= 1.0F ? 16.0F * Math.max(1.0F, (float)Math.floor((double)(s + 0.25F))) : (float)Math.round(16.0F * s);
   }

   private CoordPlan planCoordinates(HudLayoutStore.HudElementState state, List<CoordRow> rows, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      float rowFull = 20.0F * s;
      float[] valueW = new float[3];
      float height = 0.0F;

      for(CoordRow row : rows) {
         for(int i = 0; i < 3; ++i) {
            valueW[i] = Math.max(valueW[i], HudChrome.tabularWidth(t.text(), t.textPx(), row.values()[i]));
         }

         height += rowFull * smooth(row.anim());
      }

      float[] slotW = new float[3];
      float total = 7.0F * s * 2.0F + Math.min(coordIconSize(s), rowFull - 2.0F) + 8.0F * s + 12.0F * s * 2.0F;

      for(int i = 0; i < 3; ++i) {
         slotW[i] = this.slot(COORD_SLOTS[i], t.textPx(), (float)Math.ceil((double)valueW[i]), dt);
         total += slotW[i];
      }

      if (editMode) {
         height = Math.max(height, rowFull);
      }

      return new CoordPlan(rows, t, slotW, (float)Math.round(total), (float)Math.max(1, Math.round(height)));
   }

   private void drawCoordinates(ImDrawList dl, HudLayoutStore.HudElementState state, CoordPlan plan, boolean shadow, DrawTransform tr, float screenW) {
      float a = tr.alpha();
      if (!(a <= 0.001F) && !plan.rows().isEmpty()) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Type t = plan.type();
         boolean right = alignsRight(state, screenW);
         float rowFull = 20.0F * s;
         List<Step> steps = new ArrayList(plan.rows().size());
         float cy = y;

         for(int i = 0; i < plan.rows().size(); ++i) {
            CoordRow row = (CoordRow)plan.rows().get(i);
            float open = smooth(row.anim());
            float rowH = rowFull * open;
            float top = (float)Math.round(cy);
            float bottom = (float)Math.round(cy + rowH);
            cy += rowH;
            if (!(bottom <= top)) {
               float slide = (float)Math.round(w * (1.0F - open));
               float l = right ? x + slide : x - slide;
               float rowAlpha = a * (row.enabled() ? 1.0F : 0.35F) * Math.min(1.0F, row.anim() * 1.5F);
               steps.add(new Step(i, l, top, l + w, bottom, rowAlpha));
            }
         }

         this.drawRows(dl, steps, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
         float icon = Math.min(coordIconSize(s), rowFull - 2.0F);
         float iconR = 2.5F * s;
         float textInset = (float)Math.floor((double)((rowFull - (float)t.textPx()) * 0.5F));

         for(Step q : steps) {
            CoordRow row = (CoordRow)plan.rows().get(q.item());
            float stepH = q.b() - q.t();
            float shift = (rowFull - stepH) * 0.5F;
            float contentAlpha = q.alpha() * row.anim();
            boolean clip = stepH < rowFull - 0.5F;
            if (clip) {
               dl.pushClipRect(q.l(), q.t(), q.r(), q.b(), true);
            }

            float ix = (float)Math.round(q.l() + 7.0F * s);
            float iy = (float)Math.round(q.t() + (rowFull - icon) * 0.5F - shift);
            ThemeImages.Texture texture = GameIcons.block(row.block());
            if (texture != null) {
               dl.addImageRounded((long)texture.id(), ix, iy, ix + icon, iy + icon, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, contentAlpha), iconR, 240);
            }

            float cx = ix + icon + 8.0F * s;
            float valueTop = (float)Math.round(q.t() + textInset - shift);
            int color = HudChrome.scaleAlpha(-1, contentAlpha * (row.here() ? 1.0F : 0.62F));

            for(int i = 0; i < 3; ++i) {
               HudChrome.tabular(dl, t.text(), t.textPx(), cx, valueTop, color, row.values()[i], true);
               cx += plan.slotW()[i] + 12.0F * s;
            }

            if (clip) {
               dl.popClipRect();
            }
         }

      }
   }

   private static float rowWidth(float nameW, float s) {
      return (float)Math.round(12.0F * s + nameW);
   }

   private ListPlan planModuleList(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      float rowFull = 18.0F * s;
      List<ModuleListRows.Row> rows = this.moduleRows.ordered(t.text(), t.textPx(), HudChrome::width, state.anchorBottom);
      float widest = 0.0F;
      float height = 0.0F;

      for(int i = 0; i < rows.size(); ++i) {
         ModuleListRows.Row row = (ModuleListRows.Row)rows.get(i);
         widest = Math.max(widest, row.nameW());
         height += rowFull * smooth(row.anim());
      }

      if (rows.isEmpty()) {
         this.moduleListW = editMode ? rowWidth(HudChrome.width(t.label(), t.labelPx(), "No active modules"), s) : 0.0F;
         return new ListPlan(rows, t, (float)Math.max(1, Math.round(this.moduleListW)), (float)Math.round(rowFull));
      } else {
         if (editMode) {
            height = Math.max(height, rowFull);
         }

         float target = rowWidth(widest, s);
         if (!(target >= this.moduleListW) && !(Math.abs(target - this.moduleListW) < 0.5F)) {
            this.moduleListW += (target - this.moduleListW) * (1.0F - (float)Math.exp((double)(-16.0F * dt)));
         } else {
            this.moduleListW = target;
         }

         return new ListPlan(rows, t, (float)Math.round(this.moduleListW), (float)Math.max(1, Math.round(height)));
      }
   }

   private void drawModuleList(ImDrawList dl, HudLayoutStore.HudElementState state, ListPlan plan, boolean shadow, DrawTransform tr, float screenW) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Type t = plan.type();
         boolean right = alignsRight(state, screenW);
         float rowFull = 18.0F * s;
         List<Step> steps = new ArrayList(Math.max(1, plan.rows().size()));
         if (plan.rows().isEmpty()) {
            float rowW = rowWidth(HudChrome.width(t.label(), t.labelPx(), "No active modules"), s);
            float l = right ? x + w - rowW : x;
            steps.add(new Step(-1, l, y, l + rowW, (float)Math.round(y + rowFull), a));
         } else {
            float cy = y;

            for(int i = 0; i < plan.rows().size(); ++i) {
               ModuleListRows.Row row = (ModuleListRows.Row)plan.rows().get(i);
               float open = smooth(row.anim());
               float rowH = rowFull * open;
               float top = (float)Math.round(cy);
               float bottom = (float)Math.round(cy + rowH);
               cy += rowH;
               if (!(bottom <= top)) {
                  float rowW = rowWidth(row.nameW(), s);
                  float slide = (float)Math.round(rowW * (1.0F - open));
                  float l = right ? x + w - rowW + slide : x - slide;
                  steps.add(new Step(i, l, top, l + rowW, bottom, a * Math.min(1.0F, row.anim() * 1.5F)));
               }
            }
         }

         this.drawRows(dl, steps, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
         float textInset = (float)Math.floor((double)((rowFull - (float)t.textPx()) * 0.5F));
         float pad = 6.0F * s;

         for(Step q : steps) {
            if (q.item() < 0) {
               float labelW = HudChrome.width(t.label(), t.labelPx(), "No active modules");
               float tx = right ? q.r() - pad - labelW : q.l() + pad;
               HudChrome.text(dl, t.label(), t.labelPx(), tx, q.t() + (float)Math.floor((double)((rowFull - (float)t.labelPx()) * 0.5F)), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "No active modules", true);
            } else {
               ModuleListRows.Row row = (ModuleListRows.Row)plan.rows().get(q.item());
               float stepH = q.b() - q.t();
               float tx = right ? q.r() - pad - row.nameW() : q.l() + pad;
               float ty = q.t() + textInset - (rowFull - stepH) * 0.5F;
               boolean clip = stepH < rowFull - 0.5F;
               if (clip) {
                  dl.pushClipRect(q.l(), q.t(), q.r(), q.b(), true);
               }

               HudChrome.text(dl, t.text(), t.textPx(), (float)Math.round(tx), (float)Math.round(ty), HudChrome.scaleAlpha(-1, a * row.anim() * row.anim()), row.module().name(), true);
               if (clip) {
                  dl.popClipRect();
               }
            }
         }

      }
   }

   private float updateToastShift(class_310 client, HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float target = !editMode && this.draggingId == null ? toastClearance(client, state) : 0.0F;
      if (!editMode && !(Math.abs(target - this.toastShift) < 0.5F)) {
         this.toastShift += (target - this.toastShift) * (1.0F - (float)Math.exp((double)(-12.0F * dt)));
      } else {
         this.toastShift = target;
      }

      return this.toastShift;
   }

   private static float toastClearance(class_310 client, HudLayoutStore.HudElementState state) {
      if (state.anchorX == 2 && !state.anchorBottom && !client.field_1690.field_1842) {
         class_1041 window = client.method_22683();
         int gui = window.method_4495();
         int guiW = window.method_4486();
         float bottom = 0.0F;

         for(Object entry : ((AccessorHudToastManager)client.method_1566()).anubis$hudVisibleToasts()) {
            AccessorHudToastInstance instance = (AccessorHudToastInstance)entry;
            class_368 toast = instance.anubis$hudToast();
            float shown = instance.anubis$hudVisibility() == class_369.field_2209 ? instance.anubis$hudVisiblePortion() : 1.0F;
            float left = toast.method_71809(guiW, shown) * (float)gui;
            if (!(left >= state.x + state.width) && !(left + (float)(toast.method_29049() * gui) <= state.x)) {
               bottom = Math.max(bottom, (toast.method_71808(instance.anubis$hudFirstSlot()) + (float)toast.method_29050()) * (float)gui);
            }
         }

         if (bottom <= 0.0F) {
            return 0.0F;
         } else {
            return class_3532.method_15363(bottom + 10.0F * ImGuiManager.getInstance().uiScale() - state.y, 0.0F, bottom);
         }
      } else {
         return 0.0F;
      }
   }

   private static float iconSize(float s) {
      return s >= 1.0F ? 18.0F * Math.max(1.0F, (float)Math.floor((double)(s + 0.25F))) : (float)Math.round(18.0F * s);
   }

   private PotionPlan planPotions(HudLayoutStore.HudElementState state, List<PotionRow> rows, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      float rowFull = 22.0F * s;
      List<PotionRow> sorted = new ArrayList(rows);
      sorted.sort(Comparator.comparingInt(PotionRow::ticks).thenComparing(PotionRow::name));
      if (state.anchorBottom) {
         Collections.reverse(sorted);
      }

      List<PotionLine> lines = new ArrayList(sorted.size());
      float target = 0.0F;
      float fixed = 6.0F * s * 2.0F + Math.min(iconSize(s), rowFull - 2.0F) + 6.0F * s + 14.0F * s;

      for(PotionRow row : sorted) {
         float nameW = HudChrome.width(t.text(), t.textPx(), row.name());
         float ampW = row.amplifier().isEmpty() ? 0.0F : HudChrome.width(t.text(), t.textPx(), row.amplifier());
         float durationW = row.infinite() ? HudChrome.width(t.text(), t.textPx(), "∞") : HudChrome.tabularWidth(t.text(), t.textPx(), row.duration());
         lines.add(new PotionLine(row, nameW, ampW, durationW));
         target = Math.max(target, fixed + nameW + (ampW > 0.0F ? 4.0F * s + ampW : 0.0F) + durationW);
      }

      if (lines.isEmpty()) {
         this.removeSlot("potions", t.textPx());
         float w = editMode ? 6.0F * s * 2.0F + HudChrome.width(t.label(), t.labelPx(), "No active effects") : 1.0F;
         return new PotionPlan(lines, t, (float)Math.round(w), (float)Math.round(rowFull));
      } else {
         float w = this.slot("potions", t.textPx(), (float)Math.ceil((double)target), dt);
         return new PotionPlan(lines, t, (float)Math.round(w), (float)Math.round(rowFull * (float)lines.size()));
      }
   }

   private void drawPotions(ImDrawList dl, HudLayoutStore.HudElementState state, PotionPlan plan, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Type t = plan.type();
         float rowFull = 22.0F * s;
         float pad = 6.0F * s;
         int count = Math.max(1, plan.lines().size());
         List<Step> steps = new ArrayList(count);

         for(int i = 0; i < count; ++i) {
            steps.add(new Step(plan.lines().isEmpty() ? -1 : i, x, (float)Math.round(y + rowFull * (float)i), x + w, (float)Math.round(y + rowFull * (float)(i + 1)), a));
         }

         this.drawRows(dl, steps, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
         if (plan.lines().isEmpty()) {
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, y + (float)Math.floor((double)((rowFull - (float)t.labelPx()) * 0.5F)), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "No active effects", true);
         } else {
            float icon = Math.min(iconSize(s), rowFull - 2.0F);
            float textInset = (float)Math.floor((double)((rowFull - (float)t.textPx()) * 0.5F));

            for(Step q : steps) {
               PotionLine line = (PotionLine)plan.lines().get(q.item());
               PotionRow row = line.row();
               float ix = (float)Math.round(q.l() + pad);
               float iy = (float)Math.round(q.t() + (rowFull - icon) * 0.5F);
               float iconAlpha = a;
               if (!row.infinite() && row.ticks() <= 200) {
                  iconAlpha = a * (0.55F + 0.45F * Math.abs((float)Math.cos((double)row.ticks() * Math.PI / (double)10.0F)));
               }

               ThemeImages.Texture texture = GameIcons.effect(row.effect());
               if (texture != null) {
                  dl.addImage((long)texture.id(), ix, iy, ix + icon, iy + icon, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, iconAlpha));
               } else {
                  int color = GuiPalette.abgr(-16777216 | ((class_1291)row.effect().comp_349()).method_5556());
                  dl.addCircleFilled(ix + icon * 0.5F, iy + icon * 0.5F, icon * 0.3F, HudChrome.scaleAlpha(color, iconAlpha), 16);
               }

               float ty = q.t() + textInset;
               float tx = ix + icon + 6.0F * s;
               HudChrome.text(dl, t.text(), t.textPx(), tx, ty, HudChrome.scaleAlpha(-1, a), row.name(), true);
               if (line.ampW() > 0.0F) {
                  HudChrome.text(dl, t.text(), t.textPx(), tx + line.nameW() + 4.0F * s, ty, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), row.amplifier(), true);
               }

               float dx = q.r() - pad - line.durationW();
               if (row.infinite()) {
                  HudChrome.text(dl, t.text(), t.textPx(), dx, ty, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "∞", true);
               } else {
                  int color = row.expiringSoon() ? HudChrome.WARN : HudChrome.TEXT_LABEL;
                  HudChrome.tabular(dl, t.text(), t.textPx(), dx, ty, HudChrome.scaleAlpha(color, a), row.duration(), true);
               }
            }

         }
      }
   }

   private VanishPlan planVanish(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      float rowFull = 22.0F * s;
      List<VanishLine> lines = new ArrayList();

      for(AntiVanishModule.HudEntry entry : AntiVanishModule.hudEntries()) {
         String tag = AntiVanishModule.hudTag(entry);
         String value = AntiVanishModule.hudValue(entry);
         boolean urgent = "ALERT".equals(tag) || "VANISH".equals(tag);
         lines.add(new VanishLine(tag, value, urgent, HudChrome.width(t.label(), t.labelPx(), tag), HudChrome.width(t.text(), t.textPx(), value)));
      }

      if (state.anchorBottom) {
         Collections.reverse(lines);
      }

      if (lines.isEmpty()) {
         this.removeSlot("vanish", t.textPx());
         float w = editMode ? 8.0F * s * 2.0F + HudChrome.width(t.label(), t.labelPx(), "No vanish alerts") : 1.0F;
         return new VanishPlan(lines, t, 0.0F, (float)Math.round(w), (float)Math.round(rowFull));
      } else {
         float tagColumn = 0.0F;
         float valueW = 0.0F;

         for(VanishLine line : lines) {
            tagColumn = Math.max(tagColumn, line.tagW());
            valueW = Math.max(valueW, line.valueW());
         }

         float target = 8.0F * s * 2.0F + tagColumn + 8.0F * s + valueW;
         float w = this.slot("vanish", t.textPx(), (float)Math.ceil((double)target), dt);
         return new VanishPlan(lines, t, tagColumn, (float)Math.round(w), (float)Math.round(rowFull * (float)lines.size()));
      }
   }

   private void drawVanish(ImDrawList dl, HudLayoutStore.HudElementState state, VanishPlan plan, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Type t = plan.type();
         float rowFull = 22.0F * s;
         float pad = 8.0F * s;
         int count = Math.max(1, plan.lines().size());
         List<Step> steps = new ArrayList(count);

         for(int i = 0; i < count; ++i) {
            steps.add(new Step(plan.lines().isEmpty() ? -1 : i, x, (float)Math.round(y + rowFull * (float)i), x + w, (float)Math.round(y + rowFull * (float)(i + 1)), a));
         }

         this.drawRows(dl, steps, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
         if (plan.lines().isEmpty()) {
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, y + (float)Math.floor((double)((rowFull - (float)t.labelPx()) * 0.5F)), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "No vanish alerts", true);
         } else {
            float textInset = (float)Math.floor((double)((rowFull - (float)t.textPx()) * 0.5F));

            for(Step q : steps) {
               VanishLine line = (VanishLine)plan.lines().get(q.item());
               float ty = q.t() + textInset;
               float labelTop = ty + HudChrome.baselineOffset(t.text(), t.textPx(), t.label(), t.labelPx());
               float tx = q.l() + pad;
               HudChrome.text(dl, t.label(), t.labelPx(), tx, labelTop, HudChrome.scaleAlpha(line.urgent() ? HudChrome.WARN : HudChrome.TEXT_LABEL, a), line.tag(), true);
               HudChrome.text(dl, t.text(), t.textPx(), tx + plan.tagColumn() + 8.0F * s, ty, HudChrome.scaleAlpha(-1, a), line.value(), true);
            }

         }
      }
   }

   private StaffPlan planStaff(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      float rowFull = 22.0F * s;
      StaffListModule.HudList list = StaffListModule.hudRows();
      List<StaffLine> lines = new ArrayList(list.rows().size() + 1);

      for(StaffListModule.HudRow row : list.rows()) {
         int face = this.staffFace(row.id(), row.nameHidden());
         String status = row.presence() == StaffListModule.Presence.VANISHED ? "VANISHED" : (row.presence() == StaffListModule.Presence.SPECTATOR ? "SPECTATOR" : "");
         float statusW = status.isEmpty() ? 0.0F : HudChrome.width(t.label(), t.labelPx(), status);
         if (row.watching()) {
            String value = row.nearby() ? row.distanceLabel() : row.watchingFor();
            float valueW = HudChrome.tabularWidth(t.text(), t.textPx(), value);
            float serverTagW = HudChrome.width(t.label(), t.labelPx(), "WATCHING YOU");
            float nameW = HudChrome.width(t.text(), t.textPx(), row.name()) + (status.isEmpty() ? 0.0F : 8.0F * s + statusW);
            lines.add(new StaffLine(row.name(), face, "WATCHING YOU", false, nameW, serverTagW + 4.0F * s + valueW, true, row.nearby(), row.alertLevel(), status, statusW, value, valueW, serverTagW));
         } else {
            lines.add(HudRenderer.StaffLine.plain(row.name(), face, status, false, HudChrome.width(t.text(), t.textPx(), row.name()), statusW));
         }
      }

      if (list.more() > 0) {
         String more = "+" + list.more() + " more";
         lines.add(HudRenderer.StaffLine.plain(more, 0, "", true, HudChrome.width(t.label(), t.labelPx(), more), 0.0F));
      }

      if (state.anchorBottom) {
         Collections.reverse(lines);
      }

      if (lines.isEmpty()) {
         this.removeSlot("staff", t.textPx());
         boolean placeholder = editMode || StaffListModule.shouldShowHud();
         float w = placeholder ? 8.0F * s * 2.0F + HudChrome.width(t.label(), t.labelPx(), "No staff online") : 1.0F;
         return new StaffPlan(lines, t, "", (float)Math.round(w), (float)Math.round(rowFull));
      } else {
         String count = Integer.toString(list.rows().size() + list.more());
         float headerW = HudChrome.width(t.label(), t.labelPx(), "STAFF") + 5.0F * s + HudChrome.tabularWidth(t.text(), t.textPx(), count);
         float nameColumn = 0.0F;
         float stateColumn = 0.0F;

         for(StaffLine line : lines) {
            nameColumn = Math.max(nameColumn, line.nameW());
            stateColumn = Math.max(stateColumn, line.stateW());
         }

         float rowW = faceSize(s) + 6.0F * s + nameColumn + (stateColumn > 0.0F ? 14.0F * s + stateColumn : 0.0F);
         float target = 8.0F * s * 2.0F + Math.max(headerW, rowW);
         float w = this.slot("staff", t.textPx(), (float)Math.ceil((double)target), dt);
         return new StaffPlan(lines, t, count, (float)Math.round(w), (float)Math.round(20.0F * s + rowFull * (float)lines.size()));
      }
   }

   private void drawStaff(ImDrawList dl, HudLayoutStore.HudElementState state, StaffPlan plan, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Type t = plan.type();
         float rowFull = 22.0F * s;
         float pad = 8.0F * s;
         float labelInset = (float)Math.floor((double)((rowFull - (float)t.labelPx()) * 0.5F));
         if (plan.lines().isEmpty()) {
            List<Step> empty = List.of(new Step(-1, x, y, x + w, (float)Math.round(y + rowFull), a));
            this.drawRows(dl, empty, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, y + labelInset, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "No staff online", true);
         } else {
            float headerH = 20.0F * s;
            List<Step> steps = new ArrayList(plan.lines().size() + 1);
            steps.add(new Step(-1, x, y, x + w, (float)Math.round(y + headerH), a));

            for(int i = 0; i < plan.lines().size(); ++i) {
               steps.add(new Step(i, x, (float)Math.round(y + headerH + rowFull * (float)i), x + w, (float)Math.round(y + headerH + rowFull * (float)(i + 1)), a));
            }

            this.drawRows(dl, steps, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);

            for(int i = 1; i < steps.size(); ++i) {
               Step q = (Step)steps.get(i);
               StaffLine line = (StaffLine)plan.lines().get(q.item());
               if (line.server()) {
                  int corners = convexCorners(q, (Step)steps.get(i - 1), i < steps.size() - 1 ? (Step)steps.get(i + 1) : null);
                  float glow = 0.14F;
                  if (line.alert() > 0.0F) {
                     float elapsed = (1.0F - line.alert()) * 4000.0F;
                     float wave = 0.5F + 0.5F * (float)Math.cos((double)elapsed * (double)2.0F * Math.PI / (double)500.0F);
                     glow += 0.24F * line.alert() * wave;
                  }

                  int tint = HudChrome.scaleAlpha(HudChrome.WARN, a * glow);
                  if (corners == 256) {
                     dl.addRectFilled(q.l(), q.t(), q.r(), q.b(), tint);
                  } else {
                     dl.addRectFilled(q.l(), q.t(), q.r(), q.b(), tint, rowsRadius(s), corners);
                  }

                  dl.addRectFilled(q.l() + 3.0F * s, q.t() + 4.0F * s, q.l() + 5.0F * s, q.b() - 4.0F * s, HudChrome.scaleAlpha(HudChrome.WARN, a), 1.0F * s);
               }
            }

            Step head = (Step)steps.get(0);
            float countTop = head.t() + (float)Math.floor((double)((head.b() - head.t() - (float)t.textPx()) * 0.5F));
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, countTop + HudChrome.baselineOffset(t.text(), t.textPx(), t.label(), t.labelPx()), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "STAFF", true);
            HudChrome.tabular(dl, t.text(), t.textPx(), x + pad + HudChrome.width(t.label(), t.labelPx(), "STAFF") + 5.0F * s, countTop, HudChrome.scaleAlpha(-1, a), plan.count(), true);
            float face = faceSize(s);
            float textInset = (float)Math.floor((double)((rowFull - (float)t.textPx()) * 0.5F));
            float nameX = pad + face + 6.0F * s;

            for(int i = 1; i < steps.size(); ++i) {
               Step q = (Step)steps.get(i);
               StaffLine line = (StaffLine)plan.lines().get(q.item());
               if (line.more()) {
                  HudChrome.text(dl, t.label(), t.labelPx(), q.l() + nameX, q.t() + labelInset, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), line.name(), true);
               } else {
                  if (line.face() != 0) {
                     drawFace(dl, line.face(), (float)Math.round(q.l() + pad), (float)Math.round(q.t() + (q.b() - q.t() - face) * 0.5F), face, s, a);
                  }

                  float ty = q.t() + textInset;
                  float labelTop = ty + HudChrome.baselineOffset(t.text(), t.textPx(), t.label(), t.labelPx());
                  HudChrome.text(dl, t.text(), t.textPx(), q.l() + nameX, ty, HudChrome.scaleAlpha(-1, a), line.name(), true);
                  if (line.server()) {
                     if (!line.note().isEmpty()) {
                        HudChrome.text(dl, t.label(), t.labelPx(), q.l() + nameX + line.nameW() - line.noteW(), labelTop, HudChrome.scaleAlpha(HudChrome.WARN, a), line.note(), true);
                     }

                     float sx = q.r() - pad - line.stateW();
                     HudChrome.text(dl, t.label(), t.labelPx(), sx, labelTop, HudChrome.scaleAlpha(HudChrome.WARN, a), line.state(), true);
                     HudChrome.tabular(dl, t.text(), t.textPx(), sx + line.serverTagW() + 4.0F * s, ty, HudChrome.scaleAlpha(line.nearby() ? -1 : HudChrome.TEXT_LABEL, a), line.value(), true);
                  } else if (!line.state().isEmpty()) {
                     HudChrome.text(dl, t.label(), t.labelPx(), q.r() - pad - line.stateW(), labelTop, HudChrome.scaleAlpha(HudChrome.WARN, a), line.state(), true);
                  }
               }
            }

         }
      }
   }

   private static float faceSize(float s) {
      int size = Math.round((float)Math.round(16.0F * s) / 8.0F) * 8;
      if ((float)size > 22.0F * s - 4.0F * s) {
         size -= 8;
      }

      return (float)Math.max(8, size);
   }

   private static void drawFace(ImDrawList dl, int texture, float x, float y, float size, float s, float a) {
      float r = Math.max(1.0F, (float)Math.round(2.0F * s));
      int tint = HudChrome.scaleAlpha(-1, a);
      dl.addImageRounded((long)texture, x, y, x + size, y + size, 0.125F, 0.125F, 0.25F, 0.25F, tint, r, 240);
      dl.addImageRounded((long)texture, x, y, x + size, y + size, 0.625F, 0.125F, 0.75F, 0.25F, tint, r, 240);
      dl.addRect(x, y, x + size, y + size, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
   }

   int staffFace(UUID id, boolean hidden) {
      class_310 mc = class_310.method_1551();
      class_634 conn = mc.method_1562();
      class_640 info = conn == null ? null : conn.method_2871(id);
      class_2960 skin;
      if (info != null) {
         skin = info.method_52810().comp_1626().comp_3627();
         this.staffSkins.put(id, skin);
         if (hidden) {
            skin = NameProtectModule.hiddenHead(id, info.method_52810()).comp_1626().comp_3627();
         }
      } else if (hidden) {
         skin = NameProtectModule.hiddenHead(id, class_1068.method_4648(id)).comp_1626().comp_3627();
      } else {
         skin = (class_2960)this.staffSkins.get(id);
         if (skin == null) {
            skin = class_1068.method_4648(id).comp_1626().comp_3627();
         }
      }

      GpuTexture texture;
      try {
         texture = mc.method_1531().method_4619(skin).method_68004();
      } catch (IllegalStateException var10) {
         return 0;
      }

      if (texture instanceof class_10868 gl) {
         if (!gl.isClosed()) {
            if (this.nearestSkins.add(texture)) {
               int bound = GL11.glGetInteger(32873);
               GL11.glBindTexture(3553, gl.method_68427());
               GL11.glTexParameteri(3553, 10241, 9728);
               GL11.glTexParameteri(3553, 10240, 9728);
               GL11.glBindTexture(3553, bound);
            }

            return gl.method_68427();
         }
      }

      return 0;
   }

   private float planRadar(HudLayoutStore.HudElementState state, boolean editMode) {
      if (!editMode && !RadarModule.shouldShowHud()) {
         return 1.0F;
      } else {
         float size = this.radarModule != null ? this.radarModule.size() : 150.0F;
         return Math.max(2.0F, (float)Math.round(size * scaleOf(state)));
      }
   }

   private void drawRadar(ImDrawList dl, HudLayoutStore.HudElementState state, float size, class_310 client, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F) && !(size <= 1.0F)) {
         float s = scaleOf(state);
         Type t = HudRenderer.Type.of(s);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float r = size * 0.5F;
         float cx = x + r;
         float cy = y + r;
         int accent = this.hudAccent();
         Surface surface = hudSurface(state);
         if (shadow) {
            drawDiscShadow(dl, cx, cy, r, s, a);
         }

         dl.addCircleFilled(cx, cy, r, HudChrome.scaleAlpha(GuiPalette.fill(surface), a), 0);
         SurfaceBackground.drawDisc(dl, surface, cx, cy, r, a);
         dl.addCircle(cx, cy, r - 0.5F, HudChrome.scaleAlpha(402653183, a), 0, 1.0F);
         this.measureRadarLetters(t.label(), t.labelPx());
         float letterR = r - 10.0F * s - this.radarLetterHalf;
         float reach = letterR - this.radarLetterHalf - 4.0F * s;
         float rangeR = r - 5.0F * s;
         int hairline = HudChrome.scaleAlpha(536870911, a);

         for(float ring : RADAR_RINGS) {
            if (rangeR * ring < reach) {
               dl.addCircle(cx, cy, rangeR * ring, hairline, 0, 1.0F);
            }
         }

         float hx = (float)Math.floor((double)cx) + 0.5F;
         float hy = (float)Math.floor((double)cy) + 0.5F;
         dl.addLine(hx, cy - reach, hx, cy + reach, hairline, 1.0F);
         dl.addLine(cx - reach, hy, cx + reach, hy, hairline, 1.0F);
         class_746 player = client.field_1724;
         boolean rotate = this.radarModule == null || this.radarModule.rotate();
         float partial = client.method_61966().method_60637(false);
         double yaw = player != null ? Math.toRadians((double)player.method_5705(partial)) : (double)0.0F;
         float sin = (float)Math.sin(yaw);
         float cos = (float)Math.cos(yaw);

         for(int i = 0; i < RADAR_LETTERS.length; ++i) {
            int wx = RADAR_LETTER_DX[i];
            int wz = RADAR_LETTER_DZ[i];
            float ox = rotate ? (float)(-wx) * cos - (float)wz * sin : (float)wx;
            float oy = rotate ? (float)wx * sin - (float)wz * cos : (float)wz;
            int color = i == 0 ? accent : HudChrome.TEXT_LABEL;
            HudChrome.text(dl, t.label(), t.labelPx(), (float)Math.round(cx + ox * letterR - this.radarLetterW[i] * 0.5F), (float)Math.round(cy + oy * letterR - this.radarLetterMid), HudChrome.scaleAlpha(color, a), RADAR_LETTERS[i], true);
         }

         if (this.radarModule != null && player != null && client.field_1687 != null) {
            float range = Math.max(1.0F, this.radarModule.range());
            double rangeSq = (double)range * (double)range;
            float perBlock = rangeR / range;
            double px0 = class_3532.method_16436((double)partial, player.field_6014, player.method_23317());
            double pz0 = class_3532.method_16436((double)partial, player.field_5969, player.method_23321());
            float dot = 3.2F * s;
            float edge = Math.max(1.0F, 0.75F * s);
            int dotEdge = HudChrome.scaleAlpha(-1946157056, a);
            int dotColor = HudChrome.scaleAlpha(GuiPalette.abgr(this.radarModule.dotColor()), a);

            for(class_742 other : client.field_1687.method_18456()) {
               if (other != player && this.radarModule.shows(other)) {
                  double dx = class_3532.method_16436((double)partial, other.field_6014, other.method_23317()) - px0;
                  double dz = class_3532.method_16436((double)partial, other.field_5969, other.method_23321()) - pz0;
                  if (!(dx * dx + dz * dz > rangeSq)) {
                     float ox = rotate ? (float)(-dx * (double)cos - dz * (double)sin) : (float)dx;
                     float oy = rotate ? (float)(dx * (double)sin - dz * (double)cos) : (float)dz;
                     float bx = cx + ox * perBlock;
                     float by = cy + oy * perBlock;
                     dl.addCircleFilled(bx, by, dot + edge, dotEdge, 12);
                     dl.addCircleFilled(bx, by, dot, dotColor, 12);
                  }
               }
            }
         }

         dl.addCircleFilled(cx, cy, 6.0F * s, HudChrome.scaleAlpha(accent, a * 0.25F), 0);
         dl.addCircleFilled(cx, cy, 3.0F * s, HudChrome.scaleAlpha(accent, a), 0);
         float ux = rotate ? 0.0F : -sin;
         float uy = rotate ? -1.0F : cos;
         float tip = r - 1.0F - 1.5F * s;
         float base = tip - 5.0F * s;
         float half = 3.5F * s;
         dl.addTriangleFilled(cx + ux * tip, cy + uy * tip, cx + ux * base - uy * half, cy + uy * base + ux * half, cx + ux * base + uy * half, cy + uy * base - ux * half, HudChrome.scaleAlpha(accent, a));
      }
   }

   private static void drawDiscShadow(ImDrawList dl, float cx, float cy, float r, float s, float a) {
      float sp = Math.max(1.0F, s);
      int color = HudChrome.scaleAlpha(218103808, a);

      for(int k = 3; k >= 1; --k) {
         dl.addCircleFilled(cx, cy + sp * 0.5F, r + ((float)k + 0.5F) * sp, color, 0);
      }

   }

   private void measureRadarLetters(ImFont font, int px) {
      if (font != this.radarLetterFont || px != this.radarLetterPx) {
         this.radarLetterFont = font;
         this.radarLetterPx = px;

         for(int i = 0; i < RADAR_LETTERS.length; ++i) {
            this.radarLetterW[i] = HudChrome.width(font, px, RADAR_LETTERS[i]);
         }

         float capTop = this.capTop(font, px);
         float ascent = HudChrome.ascent(font, px);
         this.radarLetterMid = (capTop + ascent) * 0.5F;
         this.radarLetterHalf = (ascent - capTop) * 0.5F;
      }
   }

   private HotkeyPlan planHotkeys(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      float rowFull = 22.0F * s;
      List<HotkeyLine> lines = !editMode && !HotkeysModule.shouldShowHud() ? List.of() : this.hotkeyLinesFor(t);
      if (lines.isEmpty()) {
         this.removeSlot("hotkeys", t.textPx());
         float w = editMode ? 8.0F * s * 2.0F + HudChrome.width(t.label(), t.labelPx(), "No keys bound") : 1.0F;
         return new HotkeyPlan(lines, t, (float)Math.round(w), (float)Math.round(rowFull));
      } else {
         float rows = this.hotkeyNameColumn + 16.0F * s + keycapWidth(this.hotkeyKeyColumn, s);
         float target = 8.0F * s * 2.0F + Math.max(this.hotkeyHeaderW, rows);
         float w = this.slot("hotkeys", t.textPx(), (float)Math.ceil((double)target), dt);
         return new HotkeyPlan(lines, t, (float)Math.round(w), (float)Math.round(20.0F * s + rowFull * (float)lines.size()));
      }
   }

   private List<HotkeyLine> hotkeyLinesFor(Type t) {
      long revision = ModuleRevision.current();
      if (revision == this.hotkeyRevision && t.text() == this.hotkeyFont && t.textPx() == this.hotkeyTextPx && t.labelPx() == this.hotkeyKeyPx) {
         return this.hotkeyLines;
      } else {
         this.hotkeyRevision = revision;
         this.hotkeyFont = t.text();
         this.hotkeyTextPx = t.textPx();
         this.hotkeyKeyPx = t.labelPx();
         List<HotkeyLine> lines = new ArrayList();
         float nameColumn = 0.0F;
         float keyColumn = 0.0F;

         for(ClientModule module : ModuleRegistry.all()) {
            int key = module.keybind();
            if (key != -1) {
               String label = ClickGuiRenderer.keyName(key).toUpperCase(Locale.ROOT);
               float nameW = HudChrome.width(t.text(), t.textPx(), module.name());
               float keyW = HudChrome.width(t.text(), t.labelPx(), label);
               lines.add(new HotkeyLine(module.name(), label, module.isEnabled(), nameW, keyW));
               nameColumn = Math.max(nameColumn, nameW);
               keyColumn = Math.max(keyColumn, keyW);
            }
         }

         this.hotkeyNameColumn = nameColumn;
         this.hotkeyKeyColumn = keyColumn;
         this.hotkeyHeaderW = HudChrome.width(t.label(), t.labelPx(), "HOTKEYS");
         this.hotkeyCapMid = (this.capTop(t.text(), t.labelPx()) + HudChrome.ascent(t.text(), t.labelPx())) * 0.5F;
         this.hotkeyLines = List.copyOf(lines);
         return this.hotkeyLines;
      }
   }

   private void drawHotkeys(ImDrawList dl, HudLayoutStore.HudElementState state, HotkeyPlan plan, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F)) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Type t = plan.type();
         float rowFull = 22.0F * s;
         float pad = 8.0F * s;
         if (plan.lines().isEmpty()) {
            List<Step> empty = List.of(new Step(-1, x, y, x + w, (float)Math.round(y + rowFull), a));
            this.drawRows(dl, empty, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, y + (float)Math.floor((double)((rowFull - (float)t.labelPx()) * 0.5F)), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "No keys bound", true);
         } else {
            float headerH = 20.0F * s;
            List<Step> steps = new ArrayList(plan.lines().size() + 1);
            steps.add(new Step(-1, x, y, x + w, (float)Math.round(y + headerH), a));

            for(int i = 0; i < plan.lines().size(); ++i) {
               steps.add(new Step(i, x, (float)Math.round(y + headerH + rowFull * (float)i), x + w, (float)Math.round(y + headerH + rowFull * (float)(i + 1)), a));
            }

            this.drawRows(dl, steps, hudSurface(state), x, y, w, h, rowsRadius(s), s, shadow);
            Step head = (Step)steps.get(0);
            float countTop = head.t() + (float)Math.floor((double)((head.b() - head.t() - (float)t.textPx()) * 0.5F));
            float labelTop = countTop + HudChrome.baselineOffset(t.text(), t.textPx(), t.label(), t.labelPx());
            HudChrome.text(dl, t.label(), t.labelPx(), x + pad, labelTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), "HOTKEYS", true);
            float capW = keycapWidth(this.hotkeyKeyColumn, s);
            float textInset = (float)Math.floor((double)((rowFull - (float)t.textPx()) * 0.5F));

            for(int i = 1; i < steps.size(); ++i) {
               Step q = (Step)steps.get(i);
               HotkeyLine line = (HotkeyLine)plan.lines().get(q.item());
               HudChrome.text(dl, t.text(), t.textPx(), q.l() + pad, q.t() + textInset, HudChrome.scaleAlpha(line.on() ? -1 : HudChrome.TEXT_LABEL, a), line.name(), true);
               this.drawKeycap(dl, t, line, q.r() - pad, (q.t() + q.b()) * 0.5F, capW, s, a);
            }

         }
      }
   }

   private void drawKeycap(ImDrawList dl, Type t, HotkeyLine line, float right, float centerY, float capW, float s, float a) {
      float capH = keycapHeight(s);
      float l = (float)Math.round(right - capW);
      float top = (float)Math.round(centerY - capH * 0.5F);
      float r = Math.min(hudRounding(), 4.0F) * s;
      dl.addRectFilled(l, top, l + capW, top + capH, HudChrome.scaleAlpha(452984831, a), r);
      dl.addRect(l, top, l + capW, top + capH, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
      HudChrome.text(dl, t.text(), t.labelPx(), (float)Math.round(l + (capW - line.keyW()) * 0.5F), (float)Math.round(top + capH * 0.5F - this.hotkeyCapMid), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), line.key(), true);
   }

   private static float keycapHeight(float s) {
      return (float)Math.round(16.0F * s);
   }

   private static float keycapWidth(float keyW, float s) {
      return Math.max(keycapHeight(s), (float)Math.ceil((double)(keyW + 5.0F * s * 2.0F)));
   }

   private SpotifyPlan planSpotify(HudLayoutStore.HudElementState state, boolean editMode, float dt) {
      float s = scaleOf(state);
      Type t = HudRenderer.Type.of(s);
      if (!editMode && !SpotifyHudModule.shouldShowHud()) {
         this.removeSlot("spotify", t.textPx());
         this.releaseSpotifyArt();
         this.lyricBlockH = 0.0F;
         return new SpotifyPlan(t, HudRenderer.SpotifyLook.HIDDEN, HudRenderer.SpotifyGlyph.NONE, "", "", 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false, (SpotifyCard)null, HudRenderer.SpotifyColors.DEFAULT, 1.0F, (float)Math.round(28.0F * s));
      } else {
         SpotifyHudModule module = SpotifyHudModule.get();
         SpotifyHudModule active = SpotifyHudModule.active();
         NowPlaying now = active != null ? active.nowPlaying() : NowPlaying.WAITING;
         boolean track = now.active() && now.hasTrack();
         boolean lyricsSeen = editMode || state.visible;
         if (track && now.source() == NowPlaying.Source.SESSION) {
            return this.planSpotifyCard(module, now, false, lyricsSeen, t, s, editMode, dt);
         } else if (editMode && !track) {
            return this.planSpotifyCard(module, SAMPLE_TRACK, true, true, t, s, true, dt);
         } else {
            this.releaseSpotifyArt();
            this.lyricBlockH = 0.0F;
            return this.planSpotifyCompact(module, now, t, s, editMode, dt);
         }
      }
   }

   private SpotifyPlan planSpotifyCompact(SpotifyHudModule module, NowPlaying now, Type t, float s, boolean editMode, float dt) {
      boolean withArtist = module == null || module.showArtist();
      String artist = "";
      SpotifyGlyph glyph = HudRenderer.SpotifyGlyph.NONE;
      boolean idle = false;
      String title;
      switch (now.status()) {
         case PLAYING:
         case PAUSED:
            glyph = now.playing() ? HudRenderer.SpotifyGlyph.PLAYING : HudRenderer.SpotifyGlyph.PAUSED;
            this.foldTrack(now, t.text());
            title = now.hasTrack() ? this.spotifyTitle : "Not playing";
            if (withArtist) {
               artist = this.spotifyArtist;
            }
            break;
         case NOT_RUNNING:
            title = "Spotify is not open";
            idle = true;
            break;
         case UNSUPPORTED:
            title = "Spotify · Windows only";
            idle = true;
            break;
         default:
            title = "Nothing playing";
            idle = true;
      }

      ImFont titleFont = idle ? t.label() : t.text();
      int titlePx = idle ? t.labelPx() : t.textPx();
      float titleW = HudChrome.width(titleFont, titlePx, title);
      float artistW = artist.isEmpty() ? 0.0F : HudChrome.width(t.label(), t.labelPx(), artist);
      float glyphW = glyph == HudRenderer.SpotifyGlyph.NONE ? 0.0F : 20.0F * s;
      float fixed = 12.0F * s * 2.0F + glyphW;
      float maxText = Math.max(40.0F * s, (module != null ? module.cardWidth() : 320.0F) * s - fixed);
      float textW = Math.min(Math.max(titleW, artistW), maxText);
      float w = this.slot("spotify", t.textPx(), (float)Math.ceil((double)(fixed + textW)), dt);
      float height = artist.isEmpty() ? 28.0F * s : 8.0F * s * 2.0F + (float)t.textPx() + 2.0F * s + (float)t.labelPx();
      float avail = Math.max(1.0F, w - fixed);
      boolean scroll = module == null || module.scrollTitles();
      float loopGap = 40.0F * s;
      float[] offsets = this.marquee(title, artist, titleW, artistW, avail, scroll, now.playing() || editMode, loopGap, s, dt);
      return new SpotifyPlan(t, idle ? HudRenderer.SpotifyLook.IDLE : HudRenderer.SpotifyLook.COMPACT, glyph, title, artist, titleW, artistW, offsets[0], offsets[1], loopGap, scroll, (SpotifyCard)null, HudRenderer.SpotifyColors.of(module), (float)Math.round(w), (float)Math.round(height));
   }

   private void drawSpotify(ImDrawList dl, HudLayoutStore.HudElementState state, SpotifyPlan plan, boolean shadow, DrawTransform tr) {
      float a = tr.alpha();
      if (!(a <= 0.001F) && plan.look() != HudRenderer.SpotifyLook.HIDDEN) {
         float s = scaleOf(state);
         float x = (float)Math.round(state.x + tr.xOffset());
         float y = (float)Math.round(state.y + tr.yOffset());
         float w = state.width;
         float h = state.height;
         Surface surface = hudSurface(state);
         boolean guard = HudChrome.needsTextGuard(surface);
         HudChrome.card(dl, surface, x, y, w, h, hudRounding() * s, a, shadow, s);
         if (plan.look() == HudRenderer.SpotifyLook.CARD) {
            this.drawSpotifyCard(dl, plan, x, y, w, h, s, a, guard);
         } else {
            this.drawSpotifyCompact(dl, plan, x, y, w, h, s, a, guard);
         }

      }
   }

   private void drawSpotifyCompact(ImDrawList dl, SpotifyPlan plan, float x, float y, float w, float h, float s, float a, boolean guard) {
      Type t = plan.type();
      float cx = x + 12.0F * s;
      if (plan.glyph() != HudRenderer.SpotifyGlyph.NONE) {
         this.drawPlayState(dl, cx, y + h * 0.5F, 10.0F * s, plan.glyph() == HudRenderer.SpotifyGlyph.PLAYING, a);
         cx += 20.0F * s;
      }

      float avail = Math.max(1.0F, x + w - 12.0F * s - cx);
      if (plan.look() == HudRenderer.SpotifyLook.IDLE) {
         float top = y + (float)Math.floor((double)((h - (float)t.labelPx()) * 0.5F));
         marqueeLine(dl, t.label(), t.labelPx(), cx, top, avail, plan.titleW(), 0.0F, plan.loopGap(), false, plan.title(), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), guard);
      } else {
         float dim = plan.glyph() == HudRenderer.SpotifyGlyph.PAUSED ? 0.62F : 1.0F;
         boolean twoLines = !plan.artist().isEmpty();
         float contentH = twoLines ? (float)t.textPx() + 2.0F * s + (float)t.labelPx() : (float)t.textPx();
         float titleTop = y + (float)Math.floor((double)((h - contentH) * 0.5F));
         marqueeLine(dl, t.text(), t.textPx(), cx, titleTop, avail, plan.titleW(), plan.titleOffset(), plan.loopGap(), plan.scroll(), plan.title(), HudChrome.scaleAlpha(plan.colors().title(), a * dim), guard);
         if (twoLines) {
            float artistTop = titleTop + (float)t.textPx() + 2.0F * s;
            marqueeLine(dl, t.label(), t.labelPx(), cx, artistTop, avail, plan.artistW(), plan.artistOffset(), plan.loopGap(), plan.scroll(), plan.artist(), HudChrome.scaleAlpha(plan.colors().artist(), a), guard);
         }

      }
   }

   private void drawPlayState(ImDrawList dl, float left, float centerY, float size, boolean playing, float a) {
      float top = (float)Math.round(centerY - size * 0.5F);
      float bottom = top + (float)Math.round(size);
      if (playing) {
         dl.addTriangleFilled(left, top, left + size * 0.9F, (top + bottom) * 0.5F, left, bottom, HudChrome.scaleAlpha(this.hudAccent(), a));
      } else {
         float bar = Math.max(2.0F, (float)Math.round(size * 0.3F));
         float gap = Math.max(2.0F, (float)Math.round(size * 0.25F));
         int color = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a);
         float l = (float)Math.round(left + (size - bar * 2.0F - gap) * 0.5F);
         dl.addRectFilled(l, top, l + bar, bottom, color, 1.0F);
         dl.addRectFilled(l + bar + gap, top, l + bar * 2.0F + gap, bottom, color, 1.0F);
      }
   }

   private SpotifyPlan planSpotifyCard(SpotifyHudModule module, NowPlaying track, boolean sample, boolean lyricsSeen, Type t, float s, boolean editMode, float dt) {
      boolean withArtist = module == null || module.showArtist();
      boolean withArt = module == null || module.showAlbumArt();
      boolean withProgress = module == null || module.showProgress();
      boolean withLyrics = module == null || module.showLyrics();
      boolean scroll = module == null || module.scrollTitles();
      long nowMs = System.currentTimeMillis();
      this.foldTrack(track, t.text());
      String title = this.spotifyTitle;
      String artist = withArtist ? this.spotifyArtist : "";
      boolean timeline = track.hasTimeline();
      long position = track.positionMs();
      if (!sample) {
         this.spotifyClock.update(track, nowMs);
         position = this.spotifyClock.positionAt(nowMs);
      }

      float capTop = this.capTop(t.label(), t.labelPx());
      float transportTop = (float)t.labelPx() + 3.0F * s + (float)t.textPx() + (artist.isEmpty() ? 0.0F : 1.0F * s + (float)t.labelPx()) + 7.0F * s;
      boolean progressBar = withProgress && timeline;
      float columnBottom = progressBar ? transportTop + (float)t.labelPx() + 6.0F * s + progressHeight(s) : transportTop + HudChrome.ascent(t.label(), t.labelPx());
      float header = (float)Math.round(columnBottom - capTop);
      float pad = 10.0F * s;
      float w = (float)Math.round(Math.max(200.0F, module != null ? module.cardWidth() : 320.0F) * s);
      boolean art = withArt && w - pad * 2.0F - header - 10.0F * s >= 128.0F * s;
      float column = w - pad * 2.0F - (art ? header + 10.0F * s : 0.0F);
      String elapsed = "";
      String remaining = "";
      float progress = 0.0F;
      if (timeline) {
         long elapsedSeconds = position / 1000L;
         long totalSeconds = (track.durationMs() + 500L) / 1000L;
         elapsed = formatSeconds(elapsedSeconds);
         remaining = "−" + formatSeconds(Math.max(0L, totalSeconds - elapsedSeconds));
         progress = clamp01((float)position / (float)track.durationMs());
      }

      float titleW = HudChrome.width(t.text(), t.textPx(), title);
      float artistW = artist.isEmpty() ? 0.0F : HudChrome.width(t.label(), t.labelPx(), artist);
      float loopGap = 40.0F * s;
      float[] offsets = this.marquee(title, artist, titleW, artistW, column, scroll, track.playing() || editMode, loopGap, s, dt);
      boolean lyricsWanted = sample ? withLyrics && timeline : lyricsSeen && module != null && module.wantsLyrics(track);
      Lyrics lyrics = !lyricsWanted ? null : (sample ? SAMPLE_LYRICS : module.lyricsFor(track));
      boolean online = module == null || module.lyricsOnline();
      int lines = module == null ? 2 : module.lyricLines();
      SpotifyHudModule.LyricMotion motion = module == null ? SpotifyHudModule.LyricMotion.WAVE : module.lyricMotion();
      SpotifyHudModule.LyricHighlight highlight = module == null ? SpotifyHudModule.LyricHighlight.OFF : module.lyricHighlight();
      LyricView view = this.planLyrics(lyrics, lyricsWanted, online, position, lines, motion, highlight, t, w - pad * 2.0F, s, dt, nowMs);
      float playing = track.playing() ? 1.0F : 0.0F;
      this.lyricPlaying += (playing - this.lyricPlaying) * (1.0F - (float)Math.exp((double)(-8.0F * dt)));
      float lyricAlpha = 0.7F + 0.3F * this.lyricPlaying;
      SpotifyCard card = new SpotifyCard(track, sample, art, header, pad - capTop, column, timeline, elapsed, remaining, progress, progressBar, view, lyricAlpha);
      float height = pad * 2.0F + header + view.height();
      return new SpotifyPlan(t, HudRenderer.SpotifyLook.CARD, HudRenderer.SpotifyGlyph.NONE, title, artist, titleW, artistW, offsets[0], offsets[1], loopGap, scroll, card, HudRenderer.SpotifyColors.of(module), w, (float)Math.round(height));
   }

   private void drawSpotifyCard(ImDrawList dl, SpotifyPlan plan, float x, float y, float w, float h, float s, float a, boolean guard) {
      SpotifyCard card = plan.card();
      Type t = plan.type();
      float pad = 10.0F * s;
      boolean playing = card.track().playing();
      float left = x + pad;
      if (card.art()) {
         this.drawAlbumArt(dl, card, left, y + pad, card.header(), s, a);
         left += card.header() + 10.0F * s;
      } else {
         this.releaseSpotifyArt();
      }

      float column = card.column();
      float top = y + card.textTop();
      HudChrome.text(dl, t.label(), t.labelPx(), left, (float)Math.round(top), HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), playing ? "NOW PLAYING" : "PAUSED", guard);
      top += (float)t.labelPx() + 3.0F * s;
      marqueeLine(dl, t.text(), t.textPx(), left, (float)Math.round(top), column, plan.titleW(), plan.titleOffset(), plan.loopGap(), plan.scroll(), plan.title(), HudChrome.scaleAlpha(plan.colors().title(), a), guard);
      top += (float)t.textPx();
      if (!plan.artist().isEmpty()) {
         top += 1.0F * s;
         marqueeLine(dl, t.label(), t.labelPx(), left, (float)Math.round(top), column, plan.artistW(), plan.artistOffset(), plan.loopGap(), plan.scroll(), plan.artist(), HudChrome.scaleAlpha(plan.colors().artist(), a), guard);
         top += (float)t.labelPx();
      }

      top = (float)Math.round(top + 7.0F * s);
      this.drawTransport(dl, t, card, left, top, column, s, a, guard);
      if (card.progressBar()) {
         float barTop = (float)Math.round(top + (float)t.labelPx() + 6.0F * s);
         this.drawProgress(dl, left, barTop, column, progressHeight(s), card.progress(), playing, a);
      }

      LyricView lyrics = card.lyrics();
      if (lyrics.height() > 0.5F) {
         float blockTop = (float)Math.round(y + pad + card.header() + 12.0F * s);
         this.drawLyrics(dl, t, lyrics, plan.colors(), x + pad, blockTop, w - pad * 2.0F, y + h - Math.max(1.0F, s), s, a * card.lyricAlpha(), guard);
      }

   }

   private void drawAlbumArt(ImDrawList dl, SpotifyCard card, float x, float y, float size, float s, float a) {
      float r = Math.min(hudRounding() * 0.5F, 4.0F) * s;
      if (card.sample()) {
         this.releaseSpotifyArt();
      }

      ThemeImages.Texture texture = card.sample() ? null : this.spotifyArtTexture(card.track().art());
      if (texture != null) {
         dl.addImageRounded((long)texture.id(), x, y, x + size, y + size, 0.0F, 0.0F, 1.0F, 1.0F, HudChrome.scaleAlpha(-1, a), r, 240);
      } else {
         dl.addRectFilled(x, y, x + size, y + size, HudChrome.scaleAlpha(318767103, a), r);
         float bar = Math.max(2.0F, (float)Math.round(size * 0.07F));
         float gap = Math.max(2.0F, (float)Math.round(bar * 0.8F));
         float base = (float)Math.round(y + size * 0.62F);
         float l = (float)Math.round(x + (size - bar * 3.0F - gap * 2.0F) * 0.5F);
         int color = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a * 0.45F);
         float[] heights = new float[]{0.2F, 0.34F, 0.26F};

         for(int i = 0; i < heights.length; ++i) {
            float bx = l + (float)i * (bar + gap);
            dl.addRectFilled(bx, base - (float)Math.round(size * heights[i]), bx + bar, base, color, 1.0F);
         }
      }

      dl.addRect(x, y, x + size, y + size, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
   }

   private void drawTransport(ImDrawList dl, Type t, SpotifyCard card, float left, float top, float column, float s, float a, boolean guard) {
      float elapsedW = 0.0F;
      float remainingW = 0.0F;
      if (card.timeline()) {
         elapsedW = HudChrome.tabularWidth(t.label(), t.labelPx(), card.elapsed());
         remainingW = HudChrome.tabularWidth(t.label(), t.labelPx(), card.remaining());
         int color = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a);
         HudChrome.tabular(dl, t.label(), t.labelPx(), left, top, color, card.elapsed(), guard);
         HudChrome.tabular(dl, t.label(), t.labelPx(), (float)Math.round(left + column - remainingW), top, color, card.remaining(), guard);
      }

      float skip = Math.max(4.0F, (float)Math.round(8.0F * s));
      float play = Math.max(5.0F, (float)Math.round(10.0F * s));
      float spacing = (float)Math.round(14.0F * s);
      float cluster = skip * 2.0F + play + spacing * 2.0F;
      float centre = left + column * 0.5F;
      float margin = 8.0F * s;
      if (!(centre - cluster * 0.5F < left + elapsedW + margin) && !(centre + cluster * 0.5F > left + column - remainingW - margin)) {
         float centerY = top + (this.capTop(t.label(), t.labelPx()) + HudChrome.ascent(t.label(), t.labelPx())) * 0.5F;
         float playLeft = (float)Math.round(centre - play * 0.5F);
         float[] lefts = new float[]{playLeft - spacing - skip, playLeft, playLeft + play + spacing};
         float[] sizes = new float[]{skip, play, skip};
         float reach = (float)Math.round(3.0F * s);
         float mouseX = ImGui.getMousePosX();
         float mouseY = ImGui.getMousePosY();

         for(int i = 0; i < 3; ++i) {
            float l = lefts[i] - reach;
            float r = lefts[i] + sizes[i] + reach;
            float tp = (float)Math.round(centerY - sizes[i] * 0.5F) - reach;
            float b = (float)Math.round(centerY + sizes[i] * 0.5F) + reach;
            this.spotifyButtons[i * 4] = l;
            this.spotifyButtons[i * 4 + 1] = tp;
            this.spotifyButtons[i * 4 + 2] = r;
            this.spotifyButtons[i * 4 + 3] = b;
            boolean hovered = this.chatInput && mouseX >= l && mouseX <= r && mouseY >= tp && mouseY <= b;
            if (hovered) {
               dl.addRectFilled(l, tp, r, b, HudChrome.scaleAlpha(654311423, a), (r - l) * 0.5F);
            }

            int color = HudChrome.scaleAlpha(!hovered && i != 1 ? HudChrome.TEXT_LABEL : HudChrome.TEXT_NAME, a);
            if (i == 1) {
               drawPlayPauseGlyph(dl, lefts[i], centerY, play, card.track().playing(), color);
            } else {
               drawSkipGlyph(dl, lefts[i], centerY, skip, i == 2, color);
            }
         }

         this.spotifyButtonsDrawn = true;
      }
   }

   private int spotifyButtonAt(float x, float y) {
      for(int i = 0; i < SPOTIFY_BUTTONS.length; ++i) {
         if (x >= this.spotifyButtons[i * 4] && x <= this.spotifyButtons[i * 4 + 2] && y >= this.spotifyButtons[i * 4 + 1] && y <= this.spotifyButtons[i * 4 + 3]) {
            return i;
         }
      }

      return -1;
   }

   private static void drawSkipGlyph(ImDrawList dl, float left, float centerY, float size, boolean next, int color) {
      float top = (float)Math.round(centerY - size * 0.5F);
      float bottom = top + size;
      float mid = (top + bottom) * 0.5F;
      float bar = Math.max(1.0F, (float)Math.round(size * 0.2F));
      if (next) {
         dl.addTriangleFilled(left, top, left + size - bar, mid, left, bottom, color);
         dl.addRectFilled(left + size - bar, top, left + size, bottom, color);
      } else {
         dl.addRectFilled(left, top, left + bar, bottom, color);
         dl.addTriangleFilled(left + size, top, left + size, bottom, left + bar, mid, color);
      }

   }

   private static void drawPlayPauseGlyph(ImDrawList dl, float left, float centerY, float size, boolean playing, int color) {
      float top = (float)Math.round(centerY - size * 0.5F);
      float bottom = top + size;
      if (playing) {
         float bar = Math.max(2.0F, (float)Math.round(size * 0.3F));
         float gap = Math.max(2.0F, (float)Math.round(size * 0.24F));
         float l = (float)Math.round(left + (size - bar * 2.0F - gap) * 0.5F);
         dl.addRectFilled(l, top, l + bar, bottom, color, 1.0F);
         dl.addRectFilled(l + bar + gap, top, l + bar * 2.0F + gap, bottom, color, 1.0F);
      } else {
         float inset = (float)Math.round(size * 0.08F);
         dl.addTriangleFilled(left + inset, top, left + inset + size * 0.86F, (top + bottom) * 0.5F, left + inset, bottom, color);
      }
   }

   private void drawProgress(ImDrawList dl, float left, float top, float width, float height, float progress, boolean playing, float a) {
      float r = height * 0.5F;
      dl.addRectFilled(left, top, left + width, top + height, HudChrome.scaleAlpha(452984831, a), r);
      float fill = (float)Math.round(width * clamp01(progress));
      if (!(fill < 1.0F)) {
         dl.addRectFilled(left, top, left + fill, top + height, HudChrome.scaleAlpha(playing ? this.hudAccent() : HudChrome.TEXT_LABEL, a), r);
      }
   }

   private static float progressHeight(float s) {
      return Math.max(1.0F, (float)Math.round(2.0F * s));
   }

   private LyricView planLyrics(Lyrics lyrics, boolean wanted, boolean online, long position, int lines, SpotifyHudModule.LyricMotion motion, SpotifyHudModule.LyricHighlight highlight, Type t, float innerW, float s, float dt, long nowMs) {
      List<LyricItem> items = List.of();
      String status = null;
      float content = -1.0F;
      boolean live = false;
      if (!wanted) {
         this.lyricLoadingSince = 0L;
         content = 0.0F;
      } else {
         if (lyrics != null && lyrics.status() == Lyrics.Status.LOADING) {
            if (this.lyricLoadingSince == 0L) {
               this.lyricLoadingSince = nowMs;
            }
         } else {
            this.lyricLoadingSince = 0L;
         }

         if (lyrics != null) {
            switch (lyrics.status()) {
               case LOADING:
                  status = nowMs - this.lyricLoadingSince < 700L ? null : "Loading lyrics";
                  break;
               case SYNCED:
                  this.prepareLyrics(lyrics, t.text());
                  if (this.lyricReadable) {
                     items = new ArrayList(16);
                     float viewH = this.lyricViewHeight(lyrics.synced(), lines, t, innerW, s);
                     this.syncedItems(items, lyrics.synced(), position, viewH, motion, highlight, t, innerW, s, dt);
                     content = viewH - ((float)t.textPx() - HudChrome.ascent(t.text(), t.textPx()));
                     live = true;
                  } else {
                     status = "Lyrics in a script the HUD font lacks";
                  }
                  break;
               case PLAIN:
                  status = "No synced lyrics";
                  break;
               case INSTRUMENTAL:
                  status = "Instrumental";
                  break;
               case NOT_FOUND:
                  status = online ? "No lyrics found" : "No saved lyrics";
                  break;
               case UNAVAILABLE:
                  status = "Lyrics unavailable";
            }
         }
      }

      if (status != null) {
         content = (float)Math.round((float)(t.textPx() - t.labelPx()) * 0.5F) + HudChrome.ascent(t.label(), t.labelPx());
      }

      if (live) {
         this.lyricLastItems = items;
         this.lyricLeaveItems = null;
      } else {
         items = this.leavingItems(status == null, t, s, dt);
      }

      float target = content < 0.0F ? this.lyricBlockH : (content > 0.0F ? 12.0F * s + (float)Math.round(content) : 0.0F);
      return new LyricView(items, status, this.easeLyricBlock(target, dt));
   }

   private List<LyricItem> leavingItems(boolean quiet, Type t, float s, float dt) {
      this.lyricSource = null;
      if (!this.lyricLastItems.isEmpty()) {
         this.lyricLeaveItems = this.lyricLastItems;
         this.lyricLeave = 0.0F;
         this.lyricLastItems = List.of();
      }

      if (this.lyricLeaveItems == null) {
         return List.of();
      } else {
         this.lyricLeave = Math.min(1.0F, this.lyricLeave + dt / 0.32F);
         if (quiet && !(this.lyricLeave >= 1.0F)) {
            List<LyricItem> items = new ArrayList(this.lyricLeaveItems.size());
            addFaded(items, this.lyricLeaveItems, smooth(this.lyricLeave), 8.0F * s);
            return items;
         } else {
            this.lyricLeaveItems = null;
            return List.of();
         }
      }
   }

   private static void addFaded(List<LyricItem> items, List<LyricItem> frozen, float fade, float rise) {
      for(LyricItem item : frozen) {
         float alpha = item.alpha() * (1.0F - fade);
         if (!(alpha <= 0.004F)) {
            items.add(new LyricItem(item.text(), item.top() - rise * fade, item.highlight(), alpha, item.progress(), item.fill()));
         }
      }

   }

   private void syncedItems(List<LyricItem> items, SyncedLyrics synced, long position, float viewH, SpotifyHudModule.LyricMotion motion, SpotifyHudModule.LyricHighlight highlight, Type t, float innerW, float s, float dt) {
      float lineGap = 6.0F * s;
      int currentLast = synced.indexAt(position + 200L);
      int currentFirst = synced.firstWithTimeOf(currentLast);
      int start = Math.max(0, currentFirst);
      if (synced != this.lyricSource) {
         this.lyricSource = synced;
         this.lyricStart = start;
         this.lyricShifts.clear();
         this.lyricClock = 0.0F;
         this.lyricFadeItems = null;
         this.lyricFade = 1.0F;
         this.lyricShown = new float[synced.size() * 2];
         this.lyricBright = new float[synced.size()];
         this.lyricBrightLo = 0;
         this.lyricBrightHi = -1;

         for(int i = Math.max(0, currentFirst); i <= currentLast; ++i) {
            this.lyricBright[i] = 1.0F;
         }

         if (currentLast >= 0) {
            this.lyricBrightLo = Math.max(0, currentFirst);
            this.lyricBrightHi = currentLast;
         }
      }

      this.lyricClock += dt;
      this.lyricBreath = (this.lyricBreath + dt) % 1.6F;
      this.lyricCurrentFirst = currentFirst;
      this.lyricCurrentLast = currentLast;
      float stagger = motion == SpotifyHudModule.LyricMotion.WAVE ? 0.045F : 0.0F;
      if (start != this.lyricStart) {
         float distance = start > this.lyricStart ? this.restDistance(this.lyricStart, start, t, innerW, s) : -1.0F;
         if (motion != SpotifyHudModule.LyricMotion.FADE && distance > 0.0F && distance <= viewH + lineGap + 0.5F) {
            if (this.lyricShifts.size() == 8) {
               this.lyricShifts.remove(0);
            }

            this.lyricShifts.add(new LyricShift(distance, this.lyricClock, start, stagger));
         } else {
            this.lyricFadeItems = this.lyricLastItems;
            this.lyricFade = 0.0F;
            this.lyricShifts.clear();
            this.showView(synced, start, viewH, t, innerW, s);
         }

         this.lyricStart = start;
      }

      this.updateBrightness(currentFirst, currentLast, dt);
      this.lyricFade = Math.min(1.0F, this.lyricFade + dt / 0.32F);
      if (this.lyricFade >= 1.0F) {
         this.lyricFadeItems = null;
      }

      float settled = 1.37F;
      this.lyricShifts.removeIf((shift) -> this.lyricClock - shift.at() > settled);
      if (this.lyricShifts.isEmpty()) {
         this.lyricClock = 0.0F;
      }

      float rise = 8.0F * s;
      if (this.lyricFadeItems != null) {
         float fade = smooth(this.lyricFade);
         addFaded(items, this.lyricFadeItems, fade, rise);
         this.addLyricView(items, synced, this.lyricStart, fade, rise * (1.0F - fade), viewH, position, highlight, t, innerW, s, dt);
      } else {
         this.addLyricView(items, synced, this.lyricStart, 1.0F, 0.0F, viewH, position, highlight, t, innerW, s, dt);
      }

   }

   private void addLyricView(List<LyricItem> items, SyncedLyrics synced, int from, float alpha, float rise, float viewH, long position, SpotifyHudModule.LyricHighlight mode, Type t, float innerW, float s, float dt) {
      ImFont font = t.text();
      int px = t.textPx();
      float rowGap = 6.0F * s;
      float lineGap = 6.0F * s;
      float slot = (float)px + lineGap;
      boolean karaoke = mode == SpotifyHudModule.LyricHighlight.KARAOKE;
      float restTop = 0.0F;

      for(int i = from; i < synced.size() && restTop <= viewH + slot * 2.0F; ++i) {
         String[] rows = this.rowsFor(i, font, px, innerW);
         int count = Math.max(1, rows.length);
         float lineTop = restTop + this.lyricOffset(i) + rise;
         float depth = 1.0F - 0.39999998F * clamp01(lineTop / slot - 1.0F);
         float highlight = this.lyricGlow(i, mode);
         float progress = rows.length == 0 ? this.breakProgress(synced, i, position) : 1.0F;

         for(int r = 0; r < count; ++r) {
            float rowRest = restTop + (float)r * ((float)px + rowGap);
            float shown = this.updateShown(i, r, rowRest + (float)px <= viewH + 0.5F, dt);
            float top = lineTop + (float)r * ((float)px + rowGap);
            float rowAlpha = alpha * depth * smooth(shown) * topFade(top, s) * bottomFade(top + (float)px - viewH, px);
            if (!(rowAlpha <= 0.004F)) {
               float fill = karaoke && rows.length > 0 ? this.rowFill(synced, i, rows, r, font, px, position) : 0.0F;
               items.add(new LyricItem(rows.length == 0 ? null : rows[r], top, highlight, rowAlpha, progress, fill));
            }
         }

         restTop += lineHeight(rows, px, rowGap) + lineGap;
      }

      float lineRest = 0.0F;

      for(int i = from - 1; i >= 0; --i) {
         String[] rows = this.rowsFor(i, font, px, innerW);
         int count = Math.max(1, rows.length);
         lineRest -= lineHeight(rows, px, rowGap) + lineGap;
         float lineTop = lineRest + this.lyricOffset(i) + rise;
         if (topFade(lineTop + (float)(count - 1) * ((float)px + rowGap), s) <= 0.0F) {
            break;
         }

         float highlight = this.lyricGlow(i, mode);
         float progress = rows.length == 0 ? this.breakProgress(synced, i, position) : 1.0F;

         for(int r = 0; r < count; ++r) {
            float top = lineTop + (float)r * ((float)px + rowGap);
            float rowAlpha = alpha * smooth(this.lyricShown[i * 2 + r]) * topFade(top, s);
            if (!(rowAlpha <= 0.004F)) {
               float fill = karaoke && rows.length > 0 ? this.rowFill(synced, i, rows, r, font, px, position) : 0.0F;
               items.add(new LyricItem(rows.length == 0 ? null : rows[r], top, highlight, rowAlpha, progress, fill));
            }
         }
      }

   }

   private float lyricOffset(int index) {
      float offset = 0.0F;

      for(int k = 0; k < this.lyricShifts.size(); ++k) {
         LyricShift shift = (LyricShift)this.lyricShifts.get(k);
         float delay = (float)Math.max(0, Math.min(6, index - shift.start())) * shift.stagger();
         float time = this.lyricClock - shift.at() - delay;
         offset += time <= 0.0F ? shift.distance() : shift.distance() * springLeft(time);
      }

      return offset;
   }

   private static float springLeft(float time) {
      float wt = 13.0F * time;
      return (1.0F + wt) * (float)Math.exp((double)(-wt));
   }

   private static float topFade(float top, float s) {
      return top >= 0.0F ? 1.0F : smooth(1.0F + top / (12.0F * s * 0.75F));
   }

   private static float bottomFade(float below, int px) {
      return below <= 0.5F ? 1.0F : smooth(1.0F - below / ((float)px * 0.6F));
   }

   private float updateShown(int index, int row, boolean fits, float dt) {
      int k = index * 2 + row;
      float step = dt / 0.3F;
      float shown = fits ? Math.min(1.0F, this.lyricShown[k] + step) : Math.max(0.0F, this.lyricShown[k] - step);
      this.lyricShown[k] = shown;
      return shown;
   }

   private void showView(SyncedLyrics synced, int from, float viewH, Type t, float innerW, float s) {
      Arrays.fill(this.lyricShown, 0.0F);
      int px = t.textPx();
      float rowGap = 6.0F * s;
      float restTop = 0.0F;

      for(int i = from; i < synced.size() && restTop < viewH; ++i) {
         String[] rows = this.rowsFor(i, t.text(), px, innerW);
         int r = 0;

         for(int count = Math.max(1, rows.length); r < count; ++r) {
            if (restTop + (float)r * ((float)px + rowGap) + (float)px <= viewH + 0.5F) {
               this.lyricShown[i * 2 + r] = 1.0F;
            }
         }

         restTop += lineHeight(rows, px, rowGap) + 6.0F * s;
      }

   }

   private float restDistance(int from, int to, Type t, float innerW, float s) {
      float rowGap = 6.0F * s;
      float distance = 0.0F;

      for(int i = from; i < to; ++i) {
         distance += lineHeight(this.rowsFor(i, t.text(), t.textPx(), innerW), t.textPx(), rowGap) + 6.0F * s;
      }

      return distance;
   }

   private void updateBrightness(int currentFirst, int currentLast, float dt) {
      int lo = this.lyricBrightHi >= 0 ? this.lyricBrightLo : Integer.MAX_VALUE;
      int hi = this.lyricBrightHi;
      if (currentLast >= 0) {
         lo = Math.min(lo, Math.max(0, currentFirst));
         hi = Math.max(hi, currentLast);
      }

      float step = dt / 0.35F;
      int lit = Integer.MAX_VALUE;
      int last = -1;

      for(int i = Math.max(0, lo); i <= hi && i < this.lyricBright.length; ++i) {
         boolean sung = i >= currentFirst && i <= currentLast;
         float bright = sung ? Math.min(1.0F, this.lyricBright[i] + step) : Math.max(0.0F, this.lyricBright[i] - step);
         this.lyricBright[i] = bright;
         if (bright > 0.0F) {
            lit = Math.min(lit, i);
            last = i;
         }
      }

      this.lyricBrightLo = last < 0 ? 0 : lit;
      this.lyricBrightHi = last;
   }

   private float lyricGlow(int index, SpotifyHudModule.LyricHighlight mode) {
      if (mode != SpotifyHudModule.LyricHighlight.OFF && index >= 0 && index < this.lyricCurrentFirst) {
         return 1.0F;
      } else {
         return index >= 0 && index < this.lyricBright.length ? smooth(this.lyricBright[index]) : 0.0F;
      }
   }

   private float breakProgress(SyncedLyrics synced, int index, long position) {
      if (index >= this.lyricCurrentFirst && index <= this.lyricCurrentLast) {
         int next = synced.lastWithTimeOf(index) + 1;
         if (next >= synced.size()) {
            return 1.0F;
         } else {
            long begin = synced.line(index).timeMs();
            long end = synced.line(next).timeMs() - 200L;
            return end <= begin ? 1.0F : clamp01((float)(position - begin) / (float)(end - begin));
         }
      } else {
         return 1.0F;
      }
   }

   private float rowFill(SyncedLyrics synced, int index, String[] rows, int row, ImFont font, int px, long position) {
      if (index < this.lyricCurrentFirst) {
         return Float.POSITIVE_INFINITY;
      } else if (index > this.lyricCurrentLast) {
         return 0.0F;
      } else {
         String text = rows[row];
         int rowStart = 0;
         if (row > 0) {
            String line = this.lyricTexts[index];

            for(rowStart = rows[0].length(); rowStart < line.length() && line.charAt(rowStart) == ' '; ++rowStart) {
            }
         }

         float lit = this.litChars(synced, index, position) - (float)rowStart;
         if (lit <= 0.0F) {
            return 0.0F;
         } else if (lit >= (float)text.length()) {
            return Float.POSITIVE_INFINITY;
         } else {
            int whole = (int)lit;
            if (whole > 0 && Character.isLowSurrogate(text.charAt(whole))) {
               --whole;
            }

            int next = whole + Character.charCount(text.codePointAt(whole));
            float x0 = HudChrome.width(font, px, text.substring(0, whole));
            float x1 = HudChrome.width(font, px, text.substring(0, next));
            return x0 + (x1 - x0) * clamp01((lit - (float)whole) / (float)(next - whole));
         }
      }
   }

   private float litChars(SyncedLyrics synced, int index, long position) {
      LyricWords words = this.lyricWords[index];
      if (words == null) {
         this.timeWords(synced, synced.firstWithTimeOf(index));
         words = this.lyricWords[index];
      }

      float lit = 0.0F;
      long[] start = words.start();

      for(int k = 0; k < start.length && position >= start[k]; ++k) {
         float swept = smooth((float)(position - start[k]) / (float)words.sweep()[k]);
         lit = (float)words.from()[k] + (float)(words.to()[k] - words.from()[k]) * swept;
      }

      return lit;
   }

   private void timeWords(SyncedLyrics synced, int first) {
      int last = synced.lastWithTimeOf(first);
      int next = last + 1;
      long begin = synced.line(first).timeMs();
      long handover = next < synced.size() ? synced.line(next).timeMs() - 200L : Long.MAX_VALUE;
      int letters = 0;

      for(int i = first; i <= last; ++i) {
         letters += letterCount(this.lyricTexts[i]);
      }

      long window = next < synced.size() ? Math.min(handover - begin, (long)Math.round((float)letters * this.lyricCharMs * 2.0F)) : (long)Math.round((float)letters * this.lyricCharMs);
      window = Math.max(0L, window);
      int done = 0;

      for(int i = first; i <= last; ++i) {
         String text = this.lyricTexts[i];
         int count = text.isEmpty() ? 0 : text.length() - letterCount(text) + 1;
         int[] from = new int[count];
         int[] to = new int[count];
         long[] start = new long[count];
         long[] sweep = new long[count];
         List<Long> timed = synced.line(i).wordTimes();
         boolean own = timed.size() == count;
         int k = 0;

         for(int c = 0; k < count; ++k) {
            from[k] = c;
            c = text.indexOf(32, c);
            if (c < 0) {
               c = text.length();
            }

            to[k] = c++;
            long at = own ? (Long)timed.get(k) : begin + window * (long)done / (long)letters;
            done += to[k] - from[k];
            long until = own ? (k + 1 < count ? (Long)timed.get(k + 1) : Math.min(handover, at + 180L)) : begin + window * (long)done / (long)letters;
            start[k] = at;
            sweep[k] = Math.max(1L, Math.min(180L, until - at));
         }

         this.lyricWords[i] = new LyricWords(from, to, start, sweep);
      }

   }

   private float lyricPace(SyncedLyrics synced) {
      float[] paces = new float[synced.size()];
      int n = 0;

      int last;
      for(int first = 0; first < synced.size(); first = last + 1) {
         last = synced.lastWithTimeOf(first);
         int letters = 0;

         for(int i = first; i <= last; ++i) {
            letters += letterCount(this.lyricTexts[i]);
         }

         if (letters > 0 && last + 1 < synced.size()) {
            paces[n++] = (float)(synced.line(last + 1).timeMs() - synced.line(first).timeMs()) / (float)letters;
         }
      }

      if (n == 0) {
         return 90.0F;
      } else {
         Arrays.sort(paces, 0, n);
         return paces[n / 2];
      }
   }

   private static int letterCount(String text) {
      int letters = text.length();

      for(int i = 0; i < text.length(); ++i) {
         if (text.charAt(i) == ' ') {
            --letters;
         }
      }

      return letters;
   }

   private float lyricViewHeight(SyncedLyrics synced, int lines, Type t, float innerW, float s) {
      int px = t.textPx();
      this.prepareRows(px, innerW);
      if (this.lyricViewH >= 0.0F && this.lyricViewLines == lines && this.lyricViewScale == s) {
         return this.lyricViewH;
      } else {
         int slots = lines;

         for(int start = 0; start < synced.size(); start = synced.lastWithTimeOf(start) + 1) {
            int rows = 0;
            int i = start;

            for(int last = synced.lastWithTimeOf(start); i <= last; ++i) {
               rows += Math.max(1, this.rowsFor(i, t.text(), px, innerW).length);
            }

            slots = Math.max(slots, Math.min(4, rows));
         }

         this.lyricViewH = (float)(slots * px) + (float)(slots - 1) * 6.0F * s;
         this.lyricViewLines = lines;
         this.lyricViewScale = s;
         return this.lyricViewH;
      }
   }

   private static float lineHeight(String[] rows, int px, float rowGap) {
      return rows.length <= 1 ? (float)px : (float)(rows.length * px) + (float)(rows.length - 1) * rowGap;
   }

   private float easeLyricBlock(float target, float dt) {
      float diff = target - this.lyricBlockH;
      if (Math.abs(diff) < 0.5F) {
         this.lyricBlockH = target;
      } else {
         float speed = diff > 0.0F ? 14.0F : 9.0F;
         this.lyricBlockH += diff * (1.0F - (float)Math.exp((double)(-speed * dt)));
      }

      return this.lyricBlockH;
   }

   private void prepareLyrics(Lyrics lyrics, ImFont font) {
      if (lyrics != this.lyricPrepared) {
         this.lyricPrepared = lyrics;
         this.lyricViewH = -1.0F;
         SyncedLyrics synced = lyrics.synced();
         this.lyricTexts = new String[synced.size()];
         this.lyricRows = new String[synced.size()][];
         this.lyricWords = new LyricWords[synced.size()];
         int letters = 0;
         int missing = 0;
         IntPredicate drawable = (cpx) -> this.hudHasGlyph(font, cpx);

         for(int i = 0; i < synced.size(); ++i) {
            String text = DisplayText.fold(synced.line(i).text(), drawable);
            this.lyricTexts[i] = text;
            int k = 0;

            while(k < text.length()) {
               int cp = text.codePointAt(k);
               k += Character.charCount(cp);
               if (Character.isLetter(cp)) {
                  ++letters;
                  if (cp >= 128 && !this.hudHasGlyph(font, cp)) {
                     ++missing;
                  }
               }
            }
         }

         this.lyricReadable = (float)missing <= (float)letters * 0.25F;
         this.lyricCharMs = this.lyricPace(synced);
      }
   }

   private boolean hudHasGlyph(ImFont font, int cp) {
      if (cp >= 128 && font != null) {
         if (cp > 65535) {
            return false;
         } else {
            if (font != this.glyphFont) {
               this.glyphFont = font;
               this.glyphs.clear();
            }

            Boolean known = (Boolean)this.glyphs.get(cp);
            if (known == null) {
               if (this.glyphs.size() > 1024) {
                  this.glyphs.clear();
               }

               known = font.findGlyphNoFallback(cp).isValidPtr();
               this.glyphs.put(cp, known);
            }

            return known;
         }
      } else {
         return true;
      }
   }

   private void prepareRows(int px, float maxW) {
      int widthKey = (int)maxW;
      if (px != this.lyricRowsPx || widthKey != this.lyricRowsWidth) {
         Arrays.fill(this.lyricRows, (Object)null);
         this.lyricRowsPx = px;
         this.lyricRowsWidth = widthKey;
         this.lyricViewH = -1.0F;
      }
   }

   private String[] rowsFor(int index, ImFont font, int px, float maxW) {
      this.prepareRows(px, maxW);
      String[] rows = this.lyricRows[index];
      if (rows == null) {
         rows = wrapLyric(font, px, this.lyricTexts[index], (float)this.lyricRowsWidth);
         this.lyricRows[index] = rows;
      }

      return rows;
   }

   private static String[] wrapLyric(ImFont font, int px, String text, float maxW) {
      if (text.isEmpty()) {
         return NO_ROWS;
      } else if (HudChrome.width(font, px, text) <= maxW) {
         return new String[]{text};
      } else {
         int best = -1;
         float bestW = Float.MAX_VALUE;

         for(int i = text.indexOf(32); i > 0; i = text.indexOf(32, i + 1)) {
            float firstW = HudChrome.width(font, px, text.substring(0, i).stripTrailing());
            if (firstW > maxW) {
               break;
            }

            float secondW = HudChrome.width(font, px, text.substring(i + 1).stripLeading());
            if (secondW <= maxW && Math.max(firstW, secondW) < bestW) {
               bestW = Math.max(firstW, secondW);
               best = i;
            }
         }

         if (best > 0) {
            return new String[]{text.substring(0, best).stripTrailing(), text.substring(best + 1).stripLeading()};
         } else {
            int cut = fitPrefix(font, px, text, maxW);
            String first = text.substring(0, cut).stripTrailing();
            String rest = text.substring(cut).stripLeading();
            return rest.isEmpty() ? new String[]{first} : new String[]{first, HudChrome.width(font, px, rest) <= maxW ? rest : ellipsize(font, px, rest, maxW)};
         }
      }
   }

   private static int fitPrefix(ImFont font, int px, String text, float maxW) {
      int lastBreak = -1;

      for(int i = text.indexOf(32); i > 0 && !(HudChrome.width(font, px, text.substring(0, i)) > maxW); i = text.indexOf(32, i + 1)) {
         lastBreak = i;
      }

      if (lastBreak > 0) {
         return lastBreak + 1;
      } else {
         int lo = 1;
         int hi = text.length();

         while(lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (HudChrome.width(font, px, text.substring(0, mid)) <= maxW) {
               lo = mid;
            } else {
               hi = mid - 1;
            }
         }

         if (lo > 1 && lo < text.length() && Character.isHighSurrogate(text.charAt(lo - 1))) {
            --lo;
         }

         return lo;
      }
   }

   private void drawLyrics(ImDrawList dl, Type t, LyricView view, SpotifyColors colors, float left, float top, float width, float bottom, float s, float a, boolean guard) {
      float clipTop = top - 12.0F * s + Math.max(1.0F, 2.0F * s);
      if (!(bottom <= clipTop) && !(a <= 0.001F)) {
         float clipLeft = (float)Math.floor((double)(left - 2.0F));
         float clipRight = (float)Math.ceil((double)(left + width + 2.0F));
         dl.pushClipRect(clipLeft, clipTop, clipRight, bottom, true);
         if (view.status() != null) {
            float statusTop = (float)Math.round(top + (float)(t.textPx() - t.labelPx()) * 0.5F);
            HudChrome.text(dl, t.label(), t.labelPx(), left, statusTop, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a * 0.85F), view.status(), guard);
         }

         for(LyricItem item : view.items()) {
            float alpha = a * item.alpha();
            if (!(alpha <= 0.004F)) {
               float rowTop = top + item.top();
               if (item.text() == null) {
                  float centerY = rowTop + (this.capTop(t.text(), t.textPx()) + HudChrome.ascent(t.text(), t.textPx())) * 0.5F;
                  this.drawLyricBreak(dl, colors, left, centerY, s, item.highlight(), item.progress(), alpha);
               } else {
                  int color = HudChrome.scaleAlpha(AnubisTheme.lerpColor(colors.upcoming(), colors.lyric(), item.highlight()), alpha);
                  int lit = HudChrome.scaleAlpha(colors.lit(), alpha);
                  float edge = Math.min(clipRight, (float)Math.floor((double)left) + (float)Math.round(item.fill()));
                  if (item.fill() <= 0.0F) {
                     HudChrome.text(dl, t.text(), t.textPx(), left, (float)Math.round(rowTop), color, item.text(), guard);
                  } else if (edge >= clipRight) {
                     HudChrome.text(dl, t.text(), t.textPx(), left, (float)Math.round(rowTop), lit, item.text(), guard);
                  } else {
                     dl.pushClipRect(clipLeft, clipTop, edge, bottom, true);
                     HudChrome.text(dl, t.text(), t.textPx(), left, (float)Math.round(rowTop), lit, item.text(), guard);
                     dl.popClipRect();
                     dl.pushClipRect(edge, clipTop, clipRight, bottom, true);
                     HudChrome.text(dl, t.text(), t.textPx(), left, (float)Math.round(rowTop), color, item.text(), guard);
                     dl.popClipRect();
                  }
               }
            }
         }

         dl.popClipRect();
      }
   }

   private void drawLyricBreak(ImDrawList dl, SpotifyColors colors, float left, float centerY, float s, float highlight, float progress, float alpha) {
      float r = Math.max(1.0F, 1.6F * s);
      float step = Math.max(4.0F, (float)Math.round(6.0F * s));
      float breath = 0.5F - 0.5F * (float)Math.cos((double)(this.lyricBreath / 1.6F) * (double)2.0F * Math.PI);

      for(int i = 0; i < 3; ++i) {
         float lit = smooth(progress * 3.0F - (float)i) * highlight;
         int color = HudChrome.scaleAlpha(AnubisTheme.lerpColor(colors.upcoming(), colors.lit(), lit), alpha);
         float radius = r * (1.0F + 0.3F * lit + 0.12F * breath * highlight);
         dl.addCircleFilled(left + r + (float)i * step, centerY, radius, color, 12);
      }

   }

   private void foldTrack(NowPlaying now, ImFont font) {
      if (now != this.spotifyFolded) {
         this.spotifyFolded = now;
         IntPredicate drawable = (cp) -> this.hudHasGlyph(font, cp);
         String title = DisplayText.fold(now.title(), drawable);
         this.spotifyTitle = title.isEmpty() ? now.title() : title;
         this.spotifyArtist = DisplayText.fold(now.artist(), drawable);
      }
   }

   private float[] marquee(String title, String artist, float titleW, float artistW, float avail, boolean scroll, boolean advancing, float loopGap, float s, float dt) {
      if (!title.equals(this.marqueeTitle) || !artist.equals(this.marqueeArtist)) {
         this.marqueeTitle = title;
         this.marqueeArtist = artist;
         this.marqueeTime = 0.0F;
      }

      float titleOffset = 0.0F;
      float artistOffset = 0.0F;
      float longest = Math.max(titleW > avail ? titleW : 0.0F, artistW > avail ? artistW : 0.0F);
      if (scroll && longest > 0.0F) {
         float speed = 30.0F * s;
         float period = 2.4F + (longest + loopGap) / speed;
         this.marqueeTime %= period;
         if (advancing || this.marqueeTime > 2.4F) {
            this.marqueeTime = (this.marqueeTime + dt) % period;
         }

         float travelled = Math.max(0.0F, this.marqueeTime - 2.4F) * speed;
         if (titleW > avail) {
            titleOffset = Math.min(travelled, titleW + loopGap);
         }

         if (artistW > avail) {
            artistOffset = Math.min(travelled, artistW + loopGap);
         }
      } else {
         this.marqueeTime = 0.0F;
      }

      return new float[]{titleOffset, artistOffset};
   }

   private static void marqueeLine(ImDrawList dl, ImFont font, int px, float x, float top, float avail, float textW, float offset, float loopGap, boolean scroll, String text, int color, boolean guard) {
      if (textW <= avail + 0.5F) {
         HudChrome.text(dl, font, px, x, top, color, text, guard);
      } else if (!scroll) {
         HudChrome.text(dl, font, px, x, top, color, ellipsize(font, px, text, avail), guard);
      } else {
         dl.pushClipRect(x, top - 2.0F, x + avail, top + (float)px + 3.0F, true);
         HudChrome.text(dl, font, px, x - offset, top, color, text, guard);
         if (offset > 0.0F) {
            HudChrome.text(dl, font, px, x - offset + textW + loopGap, top, color, text, guard);
         }

         dl.popClipRect();
      }
   }

   private static String ellipsize(ImFont font, int px, String text, float maxW) {
      float room = maxW - HudChrome.width(font, px, "...");
      int lo = 0;
      int hi = text.length();

      while(lo < hi) {
         int mid = lo + hi + 1 >>> 1;
         if (HudChrome.width(font, px, text.substring(0, mid)) <= room) {
            lo = mid;
         } else {
            hi = mid - 1;
         }
      }

      String var10000 = text.substring(0, lo);
      return var10000.stripTrailing() + "...";
   }

   private static String formatSeconds(long totalSeconds) {
      long seconds = Math.max(0L, totalSeconds);
      long hours = seconds / 3600L;
      long minutes = seconds / 60L % 60L;
      StringBuilder out = new StringBuilder(8);
      if (hours > 0L) {
         out.append(hours).append(':').append(minutes < 10L ? "0" : "");
      }

      out.append(minutes).append(':');
      if (seconds % 60L < 10L) {
         out.append('0');
      }

      return out.append(seconds % 60L).toString();
   }

   private float capTop(ImFont font, int px) {
      if (font != null && !(font.getFontSize() <= 0.0F)) {
         long key = (long)System.identityHashCode(font) << 16 ^ (long)px;
         Float cached = (Float)this.capTops.get(key);
         if (cached != null) {
            return cached;
         } else {
            ImFontGlyph glyph = font.findGlyphNoFallback(72);
            float top = glyph.isValidPtr() ? glyph.getY0() * (float)px / font.getFontSize() : (float)px * 0.2F;
            float value = Math.max(0.0F, (float)Math.round(top));
            if (this.capTops.size() > 64) {
               this.capTops.clear();
            }

            this.capTops.put(key, value);
            return value;
         }
      } else {
         return (float)Math.round((float)px * 0.2F);
      }
   }

   private ThemeImages.Texture spotifyArtTexture(AlbumArt art) {
      if (art == this.spotifyArt) {
         return this.spotifyArtTexture;
      } else {
         this.releaseSpotifyArt();
         if (art == null) {
            return null;
         } else {
            this.spotifyArt = art;
            ByteBuffer pixels = MemoryUtil.memAlloc(art.byteSize());

            try {
               art.copyTo(pixels);
               pixels.flip();
               this.spotifyArtTexture = ThemeImages.fromPixels(pixels, art.size(), art.size(), false);
            } catch (RuntimeException var7) {
               this.spotifyArtTexture = null;
            } finally {
               MemoryUtil.memFree(pixels);
            }

            return this.spotifyArtTexture;
         }
      }
   }

   private void releaseSpotifyArt() {
      if (this.spotifyArtTexture != null) {
         GL11.glDeleteTextures(this.spotifyArtTexture.id());
      }

      this.spotifyArtTexture = null;
      this.spotifyArt = null;
   }

   private List<PotionRow> buildPotionRows(class_310 client) {
      if (client.field_1724 == null) {
         return List.of();
      } else {
         List<PotionRow> rows = new ArrayList();

         for(class_1293 instance : client.field_1724.method_6026()) {
            class_6880<class_1291> effect = instance.method_5579();
            String name = class_1074.method_4662(((class_1291)effect.comp_349()).method_5567(), new Object[0]);
            int amp = instance.method_5578();
            String amplifier = amp > 0 ? toRomanNumeral(amp + 1) : "";
            int ticks = instance.method_5584();
            if (ticks >= 0 && ticks != Integer.MAX_VALUE && !instance.method_48559()) {
               int seconds = Math.max(0, ticks / 20);
               String formatted = seconds / 60 + ":" + String.format("%02d", seconds % 60);
               rows.add(new PotionRow(effect, name, amplifier, formatted, false, seconds < 30, ticks));
            } else {
               rows.add(new PotionRow(effect, name, amplifier, "", true, false, Integer.MAX_VALUE));
            }
         }

         return rows;
      }
   }

   private static String toRomanNumeral(int n) {
      String var10000;
      switch (n) {
         case 1 -> var10000 = "I";
         case 2 -> var10000 = "II";
         case 3 -> var10000 = "III";
         case 4 -> var10000 = "IV";
         case 5 -> var10000 = "V";
         case 6 -> var10000 = "VI";
         case 7 -> var10000 = "VII";
         case 8 -> var10000 = "VIII";
         case 9 -> var10000 = "IX";
         case 10 -> var10000 = "X";
         default -> var10000 = String.valueOf(n);
      }

      return var10000;
   }

   private int hudAccent() {
      return this.hudModule != null && this.hudModule.customAccent() ? GuiPalette.abgr(this.hudModule.accentArgb()) : GuiPalette.accent();
   }

   static Surface hudSurface(HudLayoutStore.HudElementState state) {
      return HudTheme.get().forElementId(state.id);
   }

   static float hudRounding() {
      return HudTheme.get().rounding.get();
   }

   private void updateEditorInput(float screenW, float screenH) {
      float mouseX = ImGui.getMousePosX();
      float mouseY = ImGui.getMousePosY();
      if (this.draggingId != null) {
         if (ImGui.isMouseDown(0)) {
            HudLayoutStore.HudElementState dragging = this.layout.element(this.draggingId);
            float rawX = mouseX - this.dragOffsetX;
            float rawY = mouseY - this.dragOffsetY;
            Snap xSnap = bestSnap(rawX, dragging.width, this.snapAnchorsX(this.draggingId, screenW));
            Snap ySnap = bestSnap(rawY, dragging.height, this.snapAnchorsY(this.draggingId, screenH));
            this.snapGuideX = xSnap != null ? xSnap.guideAt() : null;
            this.snapGuideY = ySnap != null ? ySnap.guideAt() : null;
            this.layout.moveTo(this.draggingId, xSnap != null ? xSnap.position() : rawX, ySnap != null ? ySnap.position() : rawY, screenW, screenH);
         } else {
            this.draggingId = null;
            this.snapGuideX = null;
            this.snapGuideY = null;
            this.saveLayout();
         }

      } else {
         if (this.chatInput && this.spotifyButtonsShown && ImGui.isMouseClicked(0)) {
            int button = this.spotifyButtonAt(mouseX, mouseY);
            if (button >= 0) {
               SpotifyHudModule.hudButton(SPOTIFY_BUTTONS[button]);
               return;
            }
         }

         if (!ImGui.isAnyItemHovered()) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0.0F) {
               List<HudLayoutStore.HudElementState> states = this.layout.orderedElements();

               for(int i = states.size() - 1; i >= 0; --i) {
                  HudLayoutStore.HudElementState s = (HudLayoutStore.HudElementState)states.get(i);
                  if (contains(s, mouseX, mouseY)) {
                     float next = Math.max(0.5F, Math.min(3.0F, s.scale + wheel * 0.05F));
                     if (next != s.scale) {
                        s.scale = next;
                        this.layoutDirty = true;
                        this.lastWheelNanos = System.nanoTime();
                     }
                     break;
                  }
               }
            }

            if (ImGui.isMouseClicked(0)) {
               List<HudLayoutStore.HudElementState> states = this.layout.orderedElements();

               for(int i = states.size() - 1; i >= 0; --i) {
                  HudLayoutStore.HudElementState s = (HudLayoutStore.HudElementState)states.get(i);
                  if (contains(s, mouseX, mouseY)) {
                     this.draggingId = s.id;
                     this.dragOffsetX = mouseX - s.x;
                     this.dragOffsetY = mouseY - s.y;
                     break;
                  }
               }

            }
         }
      }
   }

   private List<Float> snapAnchorsX(String excludeId, float screenW) {
      List<Float> anchors = new ArrayList(List.of(0.0F, screenW, screenW * 0.5F));

      for(HudLayoutStore.HudElementState s : this.layout.orderedElements()) {
         if (!s.id.equals(excludeId) && s.positioned) {
            anchors.add(s.x);
            anchors.add(s.x + s.width);
            anchors.add(s.x + s.width * 0.5F);
         }
      }

      return anchors;
   }

   private List<Float> snapAnchorsY(String excludeId, float screenH) {
      List<Float> anchors = new ArrayList(List.of(0.0F, screenH, screenH * 0.5F));

      for(HudLayoutStore.HudElementState s : this.layout.orderedElements()) {
         if (!s.id.equals(excludeId) && s.positioned) {
            anchors.add(s.y);
            anchors.add(s.y + s.height);
            anchors.add(s.y + s.height * 0.5F);
         }
      }

      return anchors;
   }

   private static Snap bestSnap(float raw, float size, List<Float> anchors) {
      float[] offsets = new float[]{0.0F, size * 0.5F, size};
      float bestDelta = 6.0F;
      Snap best = null;

      for(float anchor : anchors) {
         for(float offset : offsets) {
            float delta = Math.abs(raw + offset - anchor);
            if (delta < bestDelta) {
               bestDelta = delta;
               best = new Snap(anchor - offset, anchor);
            }
         }
      }

      return best;
   }

   private void drawSnapGuides(ImDrawList dl, float screenW, float screenH) {
      if (this.draggingId != null) {
         int guideColor = HudChrome.scaleAlpha(this.hudAccent(), 0.8F);
         if (this.snapGuideX != null) {
            dl.addLine(this.snapGuideX, 0.0F, this.snapGuideX, screenH, guideColor, 1.0F);
         }

         if (this.snapGuideY != null) {
            dl.addLine(0.0F, this.snapGuideY, screenW, this.snapGuideY, guideColor, 1.0F);
         }

      }
   }

   private void drawEditorChrome(ImDrawList dl, float screenW, float screenH) {
      float mouseX = ImGui.getMousePosX();
      float mouseY = ImGui.getMousePosY();
      float u = ImGuiManager.getInstance().uiScale();
      ImFont font = ImGuiManager.getInstance().hudLabelFont();
      int size = HudChrome.px(14.0F, u);
      this.drawSnapGuides(dl, screenW, screenH);
      float e = (float)Math.round(Math.max(2.0F, 3.0F * u));
      List<HudLayoutStore.HudElementState> states = this.layout.orderedElements();
      List<float[]> placed = new ArrayList(states.size());

      for(HudLayoutStore.HudElementState state : states) {
         boolean active = state.id.equals(this.draggingId);
         boolean hovered = contains(state, mouseX, mouseY);
         float emphasis = active ? 1.0F : (hovered ? 0.85F : 0.4F);
         float ox = (float)Math.round(state.x);
         float oy = (float)Math.round(state.y);
         dl.addRect(ox - e, oy - e, ox + state.width + e, oy + state.height + e, HudChrome.scaleAlpha(this.hudAccent(), emphasis), hudRounding() * scaleOf(state) + e, 0, active ? 1.5F : 1.0F);
         this.drawEditorLabel(dl, font, size, u, state, active || hovered, states, placed, screenW, screenH);
      }

      drawEditorHint(dl, font, size, u, screenW, screenH);
   }

   private static void drawEditorHint(ImDrawList dl, ImFont font, int size, float u, float screenW, float screenH) {
      String hint = "Drag to move  ·  Scroll to resize  ·  Click the eye to hide";
      float w = (float)Math.round(HudChrome.width(font, size, hint) + 12.0F * u * 2.0F);
      float h = (float)Math.round(28.0F * u);
      float x = (float)Math.round((screenW - w) * 0.5F);
      float y = (float)Math.round(screenH - h - 96.0F * u);
      float r = hudRounding() * u;
      dl.addRectFilled(x, y, x + w, y + h, HudChrome.CHIP_FILL, r);
      dl.addRect(x, y, x + w, y + h, 402653183, r, 0, 1.0F);
      HudChrome.text(dl, font, size, x + 12.0F * u, y + (float)Math.floor((double)((h - (float)size) * 0.5F)), HudChrome.TEXT_LABEL, hint, false);
   }

   private void drawEditorLabel(ImDrawList dl, ImFont font, int size, float u, HudLayoutStore.HudElementState state, boolean highlighted, List<HudLayoutStore.HudElementState> states, List<float[]> placed, float screenW, float screenH) {
      String label = editorLabel(state.id);
      float a = highlighted ? 1.0F : 0.6F;
      float padX = 8.0F * u;
      float eyeSize = 12.0F * u;
      float chipH = (float)Math.round(20.0F * u);
      float textW = HudChrome.width(font, size, label);
      float chipW = (float)Math.round(padX + textW + 8.0F * u + eyeSize + padX);
      float[] chip = placeChip(state, chipW, chipH, 7.0F * u, states, placed, screenW, screenH);
      placed.add(chip);
      float chipX = chip[0];
      float chipY = chip[1];
      float r = 6.0F * u;
      dl.addRectFilled(chipX, chipY, chipX + chipW, chipY + chipH, HudChrome.scaleAlpha(HudChrome.CHIP_FILL, a), r);
      dl.addRect(chipX, chipY, chipX + chipW, chipY + chipH, HudChrome.scaleAlpha(402653183, a), r, 0, 1.0F);
      int textColor = HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, a);
      HudChrome.text(dl, font, size, chipX + padX, chipY + (float)Math.floor((double)((chipH - (float)size) * 0.5F)), textColor, label, false);
      float eyeX = chipX + padX + textW + 8.0F * u;
      float eyeY = chipY + (chipH - eyeSize) * 0.5F;
      ImGui.setCursorPos(eyeX, eyeY);
      if (ImGui.invisibleButton("##vis_" + state.id, eyeSize, eyeSize)) {
         state.visible = !state.visible;
         this.saveLayout();
      }

      boolean eyeHovered = ImGui.isItemHovered();
      float ecx = eyeX + eyeSize * 0.5F;
      float ecy = eyeY + eyeSize * 0.5F;
      float er = eyeSize * 0.34F;
      int eyeColor = eyeHovered ? HudChrome.TEXT_VALUE : HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, state.visible ? a : a * 0.6F);
      AnubisWidgets.eye(dl, ecx, ecy, er, eyeColor);
      if (!state.visible) {
         dl.addLine(ecx - er, ecy - er, ecx + er, ecy + er, eyeColor, Math.max(1.0F, 1.4F * u));
      }

   }

   private static float[] placeChip(HudLayoutStore.HudElementState state, float w, float h, float gap, List<HudLayoutStore.HudElementState> states, List<float[]> placed, float screenW, float screenH) {
      float inset = 4.0F;
      float left = state.x;
      float right = state.x + state.width - w;
      float alignX = alignsRight(state, screenW) ? right : left;
      float midY = state.y + (state.height - h) * 0.5F;
      float[][] candidates = new float[][]{{alignX, state.y - gap - h}, {alignX, state.y + state.height + gap}, {state.x + state.width + gap, Math.min(midY, state.y)}, {state.x - gap - w, Math.min(midY, state.y)}};

      for(float[] c : candidates) {
         float x = (float)Math.round(c[0]);
         float y = (float)Math.round(c[1]);
         if (!(x < inset) && !(y < inset) && !(x + w > screenW - inset) && !(y + h > screenH - inset) && !overlapsAny(x, y, w, h, state, states, placed, gap)) {
            return new float[]{x, y, w, h};
         }
      }

      float x = (float)Math.round(Math.max(inset, Math.min(alignX, screenW - inset - w)));
      float y = candidates[0][1] >= inset ? candidates[0][1] : candidates[1][1];
      return new float[]{x, (float)Math.round(Math.max(inset, Math.min(y, screenH - inset - h))), w, h};
   }

   private static boolean overlapsAny(float x, float y, float w, float h, HudLayoutStore.HudElementState self, List<HudLayoutStore.HudElementState> states, List<float[]> placed, float margin) {
      for(HudLayoutStore.HudElementState other : states) {
         if (other != self && other.positioned && overlaps(x, y, w, h, other.x - margin * 0.5F, other.y - margin * 0.5F, other.width + margin, other.height + margin)) {
            return true;
         }
      }

      for(float[] chip : placed) {
         if (overlaps(x, y, w, h, chip[0], chip[1], chip[2], chip[3])) {
            return true;
         }
      }

      return false;
   }

   private static boolean overlaps(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
      return ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah;
   }

   private static boolean contains(HudLayoutStore.HudElementState state, float x, float y) {
      if (!state.positioned) {
         return false;
      } else {
         return x >= state.x && x <= state.x + state.width && y >= state.y && y <= state.y + state.height;
      }
   }

   static String editorLabel(String id) {
      String var10000;
      switch (id) {
         case "info_bar" -> var10000 = "Info Bar";
         case "coordinates" -> var10000 = "Coordinates";
         case "module_list" -> var10000 = "Module List";
         case "potion_tracker" -> var10000 = "Potions";
         case "region_map" -> var10000 = "Region Map";
         case "anti_vanish" -> var10000 = "Anti Vanish";
         case "staff_list" -> var10000 = "Staff List";
         case "spotify" -> var10000 = "Spotify";
         case "radar" -> var10000 = "Radar";
         case "hotkeys" -> var10000 = "Hotkeys";
         case "target_card" -> var10000 = "Target HUD";
         case "armor_status" -> var10000 = "Armor HUD";
         case "item_cooldowns" -> var10000 = "Cooldown";
         default -> var10000 = id;
      }

      return var10000;
   }

   private static int readTabLatency(class_310 client, class_634 connection) {
      class_640 info = connection.method_2871(connection.method_2879().id());
      if (info == null) {
         info = connection.method_2871(client.field_1724.method_5667());
      }

      if (info == null) {
         info = connection.method_73471(connection.method_2879().name());
      }

      if (info == null) {
         info = connection.method_73471(client.field_1724.method_5477().getString());
      }

      return info == null ? 0 : Math.max(0, info.method_2959());
   }

   private static int readServerPing(class_634 connection) {
      class_642 serverData = connection.method_45734();
      return serverData != null && serverData.field_3758 > 0L && serverData.field_3758 <= 60000L ? (int)serverData.field_3758 : 0;
   }

   private static float easeOutCubic(float value) {
      float t = clamp01(value);
      float inv = 1.0F - t;
      return 1.0F - inv * inv * inv;
   }

   private static float smooth(float value) {
      float t = clamp01(value);
      return t * t * (3.0F - 2.0F * t);
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float elementScale(HudLayoutStore.HudElementState state) {
      return !Float.isNaN(state.scale) && !Float.isInfinite(state.scale) ? Math.max(0.5F, Math.min(3.0F, state.scale)) : 1.0F;
   }

   static float scaleOf(HudLayoutStore.HudElementState state) {
      return elementScale(state) * ImGuiManager.getInstance().uiScale();
   }

   static {
      SPOTIFY_BUTTONS = new SpotifyWindow.MediaKey[]{SpotifyWindow.MediaKey.PREVIOUS, SpotifyWindow.MediaKey.PLAY_PAUSE, SpotifyWindow.MediaKey.NEXT};
      studioFrame = -2;
      COORD_SLOTS = new String[]{"coords.0", "coords.1", "coords.2"};
      RADAR_RINGS = new float[]{0.33333334F, 0.6666667F};
      RADAR_LETTERS = new String[]{"N", "E", "S", "W"};
      RADAR_LETTER_DX = new int[]{0, 1, 0, -1};
      RADAR_LETTER_DZ = new int[]{-1, 0, 1, 0};
      NO_ROWS = new String[0];
      SAMPLE_TRACK = new NowPlaying(NowPlaying.Status.PLAYING, "Golden Hour Drive", "Sample Artist", "", NowPlaying.Source.SESSION, 214000L, 84000L, 0L, (double)1.0F, (AlbumArt)null);
      SAMPLE_LYRICS = new Lyrics(Lyrics.Status.SYNCED, SyncedLyrics.parse("[01:21.00]Every light in the city knows my name\n[01:27.40]We keep on driving till the morning comes\n[01:33.80]Nothing but the radio and you\n"), List.of(), 0L);
   }

   @Environment(EnvType.CLIENT)
   private static record Type(ImFont value, int valuePx, ImFont text, int textPx, ImFont label, int labelPx) {
      static Type of(float scale) {
         ImGuiManager m = ImGuiManager.getInstance();
         return new Type(m.hudValueFont(), HudChrome.px(18.0F, scale), m.hudTextFont(), HudChrome.px(17.0F, scale), m.hudLabelFont(), HudChrome.px(14.0F, scale));
      }
   }

   @Environment(EnvType.CLIENT)
   private static record InfoItem(String slot, String value, String dim, String unit) {
   }

   @Environment(EnvType.CLIENT)
   private static record Segment(InfoItem item, float valueW, float dimW, float contentW, float slotW) {
   }

   @Environment(EnvType.CLIENT)
   private static record InfoPlan(List<Segment> segments, Type type, float width, float height, float emblemW) {
   }

   @Environment(EnvType.CLIENT)
   private static record Step(int item, float l, float t, float r, float b, float alpha) {
   }

   @Environment(EnvType.CLIENT)
   private static record CoordRow(String block, boolean here, boolean enabled, String[] values, float anim) {
   }

   @Environment(EnvType.CLIENT)
   private static record CoordPlan(List<CoordRow> rows, Type type, float[] slotW, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record ListPlan(List<ModuleListRows.Row> rows, Type type, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record PotionRow(class_6880<class_1291> effect, String name, String amplifier, String duration, boolean infinite, boolean expiringSoon, int ticks) {
   }

   @Environment(EnvType.CLIENT)
   private static record PotionLine(PotionRow row, float nameW, float ampW, float durationW) {
   }

   @Environment(EnvType.CLIENT)
   private static record PotionPlan(List<PotionLine> lines, Type type, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record VanishLine(String tag, String value, boolean urgent, float tagW, float valueW) {
   }

   @Environment(EnvType.CLIENT)
   private static record VanishPlan(List<VanishLine> lines, Type type, float tagColumn, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record StaffLine(String name, int face, String state, boolean more, float nameW, float stateW, boolean server, boolean nearby, float alert, String note, float noteW, String value, float valueW, float serverTagW) {
      static StaffLine plain(String name, int face, String state, boolean more, float nameW, float stateW) {
         return new StaffLine(name, face, state, more, nameW, stateW, false, false, 0.0F, "", 0.0F, "", 0.0F, 0.0F);
      }
   }

   @Environment(EnvType.CLIENT)
   private static record StaffPlan(List<StaffLine> lines, Type type, String count, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record HotkeyLine(String name, String key, boolean on, float nameW, float keyW) {
   }

   @Environment(EnvType.CLIENT)
   private static record HotkeyPlan(List<HotkeyLine> lines, Type type, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static enum SpotifyGlyph {
      NONE,
      PLAYING,
      PAUSED;

      // $FF: synthetic method
      private static SpotifyGlyph[] $values() {
         return new SpotifyGlyph[]{NONE, PLAYING, PAUSED};
      }
   }

   @Environment(EnvType.CLIENT)
   private static enum SpotifyLook {
      HIDDEN,
      IDLE,
      COMPACT,
      CARD;

      // $FF: synthetic method
      private static SpotifyLook[] $values() {
         return new SpotifyLook[]{HIDDEN, IDLE, COMPACT, CARD};
      }
   }

   @Environment(EnvType.CLIENT)
   private static record SpotifyPlan(Type type, SpotifyLook look, SpotifyGlyph glyph, String title, String artist, float titleW, float artistW, float titleOffset, float artistOffset, float loopGap, boolean scroll, SpotifyCard card, SpotifyColors colors, float width, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record SpotifyColors(int title, int artist, int lyric, int upcoming, int lit) {
      static final SpotifyColors DEFAULT;

      static SpotifyColors of(SpotifyHudModule module) {
         if (module == null) {
            return DEFAULT;
         } else {
            SpotifyHudModule.LyricHighlight mode = module.lyricHighlight();
            int lyric = GuiPalette.abgr(module.lyricsColor());
            int highlight = GuiPalette.abgr(module.highlightColor());
            return new SpotifyColors(GuiPalette.abgr(module.titleColor()), GuiPalette.abgr(module.artistColor()), mode == SpotifyHudModule.LyricHighlight.LINE ? highlight : lyric, GuiPalette.abgr(module.upcomingLyricsColor()), mode == SpotifyHudModule.LyricHighlight.OFF ? lyric : highlight);
         }
      }

      static {
         DEFAULT = new SpotifyColors(HudChrome.TEXT_NAME, HudChrome.TEXT_LABEL, HudChrome.TEXT_VALUE, HudChrome.TEXT_LABEL, HudChrome.TEXT_VALUE);
      }
   }

   @Environment(EnvType.CLIENT)
   private static record SpotifyCard(NowPlaying track, boolean sample, boolean art, float header, float textTop, float column, boolean timeline, String elapsed, String remaining, float progress, boolean progressBar, LyricView lyrics, float lyricAlpha) {
   }

   @Environment(EnvType.CLIENT)
   private static record LyricItem(String text, float top, float highlight, float alpha, float progress, float fill) {
   }

   @Environment(EnvType.CLIENT)
   private static record LyricWords(int[] from, int[] to, long[] start, long[] sweep) {
   }

   @Environment(EnvType.CLIENT)
   private static record LyricShift(float distance, float at, int start, float stagger) {
   }

   @Environment(EnvType.CLIENT)
   private static record LyricView(List<LyricItem> items, String status, float height) {
   }

   @Environment(EnvType.CLIENT)
   private static record Snap(float position, float guideAt) {
   }

   @Environment(EnvType.CLIENT)
   private static record DrawTransform(float xOffset, float yOffset, float alpha) {
      private static DrawTransform identity() {
         return new DrawTransform(0.0F, 0.0F, 1.0F);
      }

      private DrawTransform down(float dy) {
         return dy == 0.0F ? this : new DrawTransform(this.xOffset, this.yOffset + dy, this.alpha);
      }
   }
}
