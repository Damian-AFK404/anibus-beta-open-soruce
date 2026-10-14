package net.anubis.client.module.impl.render;

import imgui.ImDrawList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.anubis.client.config.FriendStore;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.gui.GuiPalette;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.HudOverlayProvider;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.misc.FreecamModule;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.MobilePerformance;
import net.anubis.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_156;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_2583;
import net.minecraft.class_310;
import net.minecraft.class_5251;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class TracersModule extends ClientModule implements HudOverlayProvider {
   private static final double DISTANCE_COLOR_RANGE = (double)60.0F;
   private static final float ARROW_TIP = 0.73205F;
   private static final int WHITE = 16777215;
   private final Vector4f mobileClip = new Vector4f();
   private ArrayList<class_1297> mobileTargets;
   private final BooleanSetting players = (BooleanSetting)this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting animals = (BooleanSetting)this.addSetting("animals", new BooleanSetting("Animals", false));
   private final BooleanSetting waterAnimals = (BooleanSetting)this.addSetting("water-animals", new BooleanSetting("Water Animals", false));
   private final BooleanSetting monsters = (BooleanSetting)this.addSetting("monsters", new BooleanSetting("Monsters", false));
   private final BooleanSetting ambient = (BooleanSetting)this.addSetting("ambient", new BooleanSetting("Ambient", false));
   private final BooleanSetting misc = (BooleanSetting)this.addSetting("misc", new BooleanSetting("Misc", false));
   private final BooleanSetting ignoreSelf = (BooleanSetting)this.addSetting("ignore-self", new BooleanSetting("Ignore Self", false));
   private final BooleanSetting ignoreFriends = (BooleanSetting)this.addSetting("ignore-friends", new BooleanSetting("Ignore Friends", false));
   private final BooleanSetting showInvisible = (BooleanSetting)this.addSetting("show-invisible", new BooleanSetting("Show Invisible", true));
   private final ModeSetting style = (ModeSetting)this.addSetting("style", new ModeSetting("Style", "Lines", new String[]{"Lines", "Offscreen"}));
   private final ModeSetting target = (ModeSetting)this.addSetting("target", new ModeSetting("Target", "Body", new String[]{"Head", "Body", "Feet"}));
   private final BooleanSetting stem = (BooleanSetting)this.addSetting("stem", new BooleanSetting("Stem", true));
   private final NumberSetting maxDistance = (NumberSetting)this.addSetting("max-distance", (new NumberSetting("Max Distance", 256.0F, 0.0F, 256.0F)).step(1.0F));
   private final NumberSetting distanceOffscreen = (NumberSetting)this.addSetting("distance-offscreen", (new NumberSetting("Distance Offscreen", 200.0F, 0.0F, 500.0F)).step(1.0F));
   private final NumberSetting sizeOffscreen = (NumberSetting)this.addSetting("size-offscreen", (new NumberSetting("Size Offscreen", 10.0F, 2.0F, 50.0F)).step(1.0F));
   private final BooleanSetting blinkOffscreen = (BooleanSetting)this.addSetting("blink-offscreen", new BooleanSetting("Blink Offscreen", true));
   private final NumberSetting blinkOffscreenSpeed = (NumberSetting)this.addSetting("blink-offscreen-speed", (new NumberSetting("Blink Offscreen Speed", 4.0F, 1.0F, 15.0F)).step(1.0F));
   private final BooleanSetting distanceColors = (BooleanSetting)this.addSetting("distance-colors", new BooleanSetting("Distance Colors", false));
   private final BooleanSetting friendOverride = (BooleanSetting)this.addSetting("show-friend-colors", new BooleanSetting("Show Friend Colors", true));
   private final BooleanSetting teamColors = (BooleanSetting)this.addSetting("team-colors", new BooleanSetting("Team Colors", true));
   private final ColorSetting friendColor = (ColorSetting)this.addSetting("friend-color", new ColorSetting("Friend Color", -16711756));
   private final ColorSetting playersColor = (ColorSetting)this.addSetting("players-colors", new ColorSetting("Players Color", 2144193997));
   private final ColorSetting animalsColor = (ColorSetting)this.addSetting("animals-color", new ColorSetting("Animals Color", 2140274577));
   private final ColorSetting waterAnimalsColor = (ColorSetting)this.addSetting("water-animals-color", new ColorSetting("Water Animals Color", 2140246527));
   private final ColorSetting monstersColor = (ColorSetting)this.addSetting("monsters-color", new ColorSetting("Monsters Color", 2147455377));
   private final ColorSetting ambientColor = (ColorSetting)this.addSetting("ambient-color", new ColorSetting("Ambient Color", 2135640907));
   private final ColorSetting miscColor = (ColorSetting)this.addSetting("misc-color", new ColorSetting("Misc Color", 2140246417));

   public TracersModule() {
      super("tracers", "Tracers", "Displays tracer lines to specified entities.", ModuleCategory.RENDER);
      this.target.visibleWhen(() -> this.style.is("Lines"));
      this.stem.visibleWhen(() -> this.style.is("Lines"));
      this.distanceOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
      this.sizeOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
      this.blinkOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
      this.blinkOffscreenSpeed.visibleWhen(() -> this.style.is("Offscreen") && this.blinkOffscreen.get());
      this.friendOverride.visibleWhen(() -> this.distanceColors.get() && !this.ignoreFriends.get());
      this.teamColors.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
      this.friendColor.visibleWhen(() -> !this.ignoreFriends.get() && this.players.get() && (!this.distanceColors.get() || this.friendOverride.get()));
      this.playersColor.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
      this.animalsColor.visibleWhen(() -> !this.distanceColors.get() && this.animals.get());
      this.waterAnimalsColor.visibleWhen(() -> !this.distanceColors.get() && this.waterAnimals.get());
      this.monstersColor.visibleWhen(() -> !this.distanceColors.get() && this.monsters.get());
      this.ambientColor.visibleWhen(() -> !this.distanceColors.get() && this.ambient.get());
      this.miscColor.visibleWhen(() -> !this.distanceColors.get() && this.misc.get());
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      class_310 mc = class_310.method_1551();
      if (!mc.field_1690.field_1842 && this.style.is("Lines")) {
         boolean mobile = MobilePerformance.active();

         try {
            for(class_1297 entity : this.targets(mc)) {
               int color = this.colorOf(mc, entity);
               class_243 feet = entity.method_30950(event.partialTick);
               double height = entity.method_5829().method_17940();
               double var10000;
               switch (this.target.get()) {
                  case "Head" -> var10000 = height;
                  case "Body" -> var10000 = height / (double)2.0F;
                  default -> var10000 = (double)0.0F;
               }

               double up = var10000;
               if (mobile) {
                  TracerEngine.draw(feet.field_1352, feet.field_1351 + up, feet.field_1350, color);
               } else {
                  TracerEngine.draw(feet.method_1031((double)0.0F, up, (double)0.0F), color);
               }

               if (this.stem.get()) {
                  TracerEngine.stem(feet, height, color);
               }
            }
         } finally {
            this.clearTargets();
         }

      }
   }

   public boolean isHudOverlayEnabled() {
      return this.isEnabled() && this.style.is("Offscreen") && !class_310.method_1551().field_1690.field_1842;
   }

   public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
      class_310 mc = class_310.method_1551();
      float partialTick = mc.method_61966().method_60637(false);
      float blink = this.blinkOffscreen.get() ? this.blinkAlpha() : 1.0F;
      float reach = this.distanceOffscreen.get();
      float size = this.sizeOffscreen.get();
      boolean mobile = MobilePerformance.active();

      try {
         for(class_1297 entity : this.targets(mc)) {
            Vector4f clip = mobile ? TracerEngine.clip(entity.method_30950(partialTick), this.mobileClip) : TracerEngine.clip(entity.method_30950(partialTick));
            if (clip == null) {
               return;
            }

            if (!(clip.w > 0.0F) || !(Math.abs(clip.x) < clip.w) || !(Math.abs(clip.y) < clip.w)) {
               double angle = Math.atan2((double)(-clip.y * screenH), (double)(clip.x * screenW));
               int color = this.colorOf(mc, entity);
               int alpha = (int)((float)(color >>> 24) * blink);
               arrow(dl, screenW * 0.5F + reach * (float)Math.cos(angle), screenH * 0.5F + reach * (float)Math.sin(angle), size, (float)angle, GuiPalette.abgr(alpha << 24 | color & 16777215));
            }
         }

      } finally {
         this.clearTargets();
      }
   }

   protected void onDisable() {
      this.clearTargets();
   }

   private void clearTargets() {
      if (this.mobileTargets != null) {
         this.mobileTargets.clear();
      }

   }

   private List<class_1297> targets(class_310 mc) {
      if (mc.field_1687 != null && mc.field_1724 != null) {
         FreecamModule freecam = (FreecamModule)ModuleRegistry.get(FreecamModule.class);
         boolean skipSelf = this.ignoreSelf.get() || freecam == null || !freecam.isEnabled();
         double maxSq = (double)this.maxDistance.get() * (double)this.maxDistance.get();
         class_243 camera = mc.field_1773.method_19418().method_71156();
         double view = (double)((Integer)mc.field_1690.method_42503().method_41753() + 1) * (double)16.0F;
         List<class_1297> result;
         if (MobilePerformance.active()) {
            if (this.mobileTargets == null) {
               this.mobileTargets = new ArrayList();
            }

            this.mobileTargets.clear();
            result = this.mobileTargets;
         } else {
            result = new ArrayList();
         }

         for(class_1297 entity : mc.field_1687.method_18112()) {
            if ((entity != mc.field_1724 || !skipSelf) && this.groupColor(entity) != null && !(entity.method_5858(mc.field_1724) > maxSq) && (!this.ignoreFriends.get() || !isFriend(entity)) && (this.showInvisible.get() || !entity.method_5767()) && !(Math.abs(camera.field_1352 - entity.method_23317()) >= view) && !(Math.abs(camera.field_1350 - entity.method_23321()) >= view)) {
               result.add(entity);
            }
         }

         return result;
      } else {
         return List.of();
      }
   }

   private ColorSetting groupColor(class_1297 entity) {
      if (entity instanceof class_1657) {
         return this.players.get() ? this.playersColor : null;
      } else {
         ColorSetting var10000;
         switch (entity.method_5864().method_5891()) {
            case field_6294:
               var10000 = this.animals.get() ? this.animalsColor : null;
               break;
            case field_24460:
            case field_6300:
            case field_30092:
            case field_34447:
               var10000 = this.waterAnimals.get() ? this.waterAnimalsColor : null;
               break;
            case field_6302:
               var10000 = this.monsters.get() ? this.monstersColor : null;
               break;
            case field_6303:
               var10000 = this.ambient.get() ? this.ambientColor : null;
               break;
            default:
               var10000 = this.misc.get() ? this.miscColor : null;
         }

         return var10000;
      }
   }

   private static boolean isFriend(class_1297 entity) {
      boolean var10000;
      if (entity instanceof class_1657 player) {
         if (FriendStore.getInstance().isFriend(player.method_7334().name())) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   private int colorOf(class_310 mc, class_1297 entity) {
      boolean friend = isFriend(entity);
      if (!this.distanceColors.get()) {
         if (entity instanceof class_1657) {
            class_1657 player = (class_1657)entity;
            int base = this.playersColor.argb();
            int alpha = base & -16777216;
            if (friend) {
               return alpha | this.friendColor.rgb();
            } else {
               int team = this.teamColors.get() ? teamColor(player) : 16777215;
               return team == 16777215 ? base : alpha | team;
            }
         } else {
            return this.groupColor(entity).argb();
         }
      } else {
         return friend && this.friendOverride.get() ? this.friendColor.argb() : distanceColor(mc, entity);
      }
   }

   private static int distanceColor(class_310 mc, class_1297 entity) {
      double percent = mc.field_1773.method_19418().method_71156().method_1022(entity.method_33571()) / (double)60.0F;
      if (percent > (double)1.0F) {
         return -16711936;
      } else {
         int r = percent < (double)0.5F ? 255 : 255 - (int)((double)255.0F * (percent - (double)0.5F) / (double)0.5F);
         int g = percent < (double)0.5F ? (int)((double)255.0F * percent / (double)0.5F) : 255;
         return -16777216 | r << 16 | g << 8;
      }
   }

   private static int teamColor(class_1657 player) {
      Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
      player.method_5476().method_27658((segment, text) -> {
         class_5251 color = segment.method_10973();
         counts.addTo(color == null ? 16777215 : color.method_27716(), text.length());
         return Optional.empty();
      }, class_2583.field_24360);
      int best = 16777215;
      int bestCount = 0;
      ObjectIterator var4 = counts.int2IntEntrySet().iterator();

      while(var4.hasNext()) {
         Int2IntMap.Entry entry = (Int2IntMap.Entry)var4.next();
         if (entry.getIntValue() > bestCount) {
            best = entry.getIntKey();
            bestCount = entry.getIntValue();
         }
      }

      return best;
   }

   private float blinkAlpha() {
      double time = (double)class_156.method_658() * ((double)this.blinkOffscreenSpeed.get() / (double)4.0F);
      return (float)Math.abs(time % (double)1000.0F - (double)500.0F) / 500.0F;
   }

   private static void arrow(ImDrawList dl, float x, float y, float size, float angle, int abgr) {
      float cos = (float)Math.cos((double)angle);
      float sin = (float)Math.sin((double)angle);
      float cx = x - size * 1.26795F / 3.0F;
      if (MobilePerformance.active()) {
         float dx0 = x - size - cx;
         float dy0 = y - size - y;
         float dx1 = x + size * 0.73205F - cx;
         float dy1 = y - y;
         float dx2 = x - size - cx;
         float dy2 = y + size - y;
         dl.addTriangleFilled(cx + dx0 * cos - dy0 * sin, y + dx0 * sin + dy0 * cos, cx + dx1 * cos - dy1 * sin, y + dx1 * sin + dy1 * cos, cx + dx2 * cos - dy2 * sin, y + dx2 * sin + dy2 * cos, abgr);
      } else {
         float[] px = new float[]{x - size, x + size * 0.73205F, x - size};
         float[] py = new float[]{y - size, y, y + size};

         for(int i = 0; i < 3; ++i) {
            float dx = px[i] - cx;
            float dy = py[i] - y;
            px[i] = cx + dx * cos - dy * sin;
            py[i] = y + dx * sin + dy * cos;
         }

         dl.addTriangleFilled(px[0], py[0], px[1], py[1], px[2], py[2], abgr);
      }
   }
}
