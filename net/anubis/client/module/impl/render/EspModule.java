package net.anubis.client.module.impl.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.anubis.client.config.FriendStore;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.MobilePerformance;
import net.anubis.client.util.StableTopK;
import net.anubis.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_1297;
import net.minecraft.class_1308;
import net.minecraft.class_1309;
import net.minecraft.class_1569;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_638;

@Environment(EnvType.CLIENT)
public final class EspModule extends ClientModule {
   private static EspModule instance;
   private static final int ENTITY_CAP = 96;
   private static final Comparator<Target> PRIORITY = Comparator.comparing((target) -> target.group() != EspModule.Group.PLAYER).thenComparingDouble(Target::distanceSq);
   private final BooleanSetting sPlayers = (BooleanSetting)this.addSetting("players", new BooleanSetting("Players", true));
   private final BooleanSetting sMobs = (BooleanSetting)this.addSetting("mobs", new BooleanSetting("Hostile Mobs", true));
   private final BooleanSetting sAnimals = (BooleanSetting)this.addSetting("animals", new BooleanSetting("Passive Mobs", false));
   private final BooleanSetting sIgnoreSelf = (BooleanSetting)this.addSetting("ignore-self", new BooleanSetting("Ignore Self", true));
   private final BooleanSetting sBox = (BooleanSetting)this.addSetting("box", new BooleanSetting("Box", true));
   private final BooleanSetting sTracers = (BooleanSetting)this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sCustomTracerColor = (BooleanSetting)this.addSetting("custom-tracer-color", new BooleanSetting("Custom Tracer Color", false));
   private final ColorSetting sTracerColor = (ColorSetting)this.addSetting("tracer-color", new ColorSetting("Tracer Color", -1));
   private final ModeSetting sShape = (ModeSetting)this.addSetting("shape", new ModeSetting("Shape", "Glow", new String[]{"Glow", "Box", "Fill", "Both"}));
   private final NumberSetting sRange = (NumberSetting)this.addSetting("range", (new NumberSetting("Range", 96.0F, 16.0F, 256.0F)).step(1.0F));
   private final NumberSetting sMaxEntities = (NumberSetting)this.addSetting("max-entities", (new NumberSetting("Max Entities", 48.0F, 8.0F, 96.0F)).step(1.0F));
   private final ColorSetting sPlayerColor = (ColorSetting)this.addSetting("player-color", new ColorSetting("Player Color", -1));
   private final BooleanSetting sFriends = (BooleanSetting)this.addSetting("friends", new BooleanSetting("Highlight Friends", true));
   private final ColorSetting sFriendColor = (ColorSetting)this.addSetting("friend-color", new ColorSetting("Friend Color", -10169601));
   private final ColorSetting sMobColor = (ColorSetting)this.addSetting("mob-color", new ColorSetting("Hostile Color", -47814));
   private final ColorSetting sAnimalColor = (ColorSetting)this.addSetting("animal-color", new ColorSetting("Passive Color", -13577896));
   private final NumberSetting sFillAlpha = (NumberSetting)this.addSetting("fill-alpha", (new NumberSetting("Fill Alpha", 0.3F, 0.0F, 1.0F)).step(0.05F));
   private final NumberSetting sLineWidth = (NumberSetting)this.addSetting("line-width", (new NumberSetting("Line Width", 1.5F, 0.5F, 5.0F)).step(0.5F));
   private final Map<class_1297, Integer> glowing = new IdentityHashMap();
   private final StableTopK<Target> mobileTargets;
   private final Map<Integer, class_12179> mobileStyles;
   private long glowFrame;
   private class_638 glowLevel;

   public EspModule() {
      super("esp", "ESP", "Highlights players, hostile and passive mobs with outlines, boxes and tracers.", ModuleCategory.RENDER);
      this.mobileTargets = new StableTopK<Target>(PRIORITY);
      this.mobileStyles = new HashMap();
      this.glowFrame = -1L;
      instance = this;
      this.sBox.visibleWhen(() -> !this.sShape.is("Glow"));
      this.sFillAlpha.visibleWhen(() -> this.sShape.is("Fill") || this.sShape.is("Both"));
      this.sLineWidth.visibleWhen(() -> this.sShape.is("Box") || this.sShape.is("Both"));
      BooleanSetting var10000 = this.sCustomTracerColor;
      BooleanSetting var10001 = this.sTracers;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
      ColorSetting var1 = this.sPlayerColor;
      var10001 = this.sPlayers;
      Objects.requireNonNull(var10001);
      var1.visibleWhen(var10001::get);
      BooleanSetting var2 = this.sFriends;
      var10001 = this.sPlayers;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      this.sFriendColor.visibleWhen(() -> this.sPlayers.get() && this.sFriends.get());
      ColorSetting var3 = this.sMobColor;
      var10001 = this.sMobs;
      Objects.requireNonNull(var10001);
      var3.visibleWhen(var10001::get);
      var3 = this.sAnimalColor;
      var10001 = this.sAnimals;
      Objects.requireNonNull(var10001);
      var3.visibleWhen(var10001::get);
   }

   protected void onDisable() {
      this.resetGlow();
   }

   public static boolean shouldGlow(class_1297 entity) {
      EspModule module = instance;
      return module != null && module.isEnabled() && module.sShape.is("Glow") ? module.glowing().containsKey(entity) : false;
   }

   public static int glowColor(class_1297 entity) {
      EspModule module = instance;
      Integer color = module == null ? null : (Integer)module.glowing().get(entity);
      return (color == null ? 16777215 : color) & 16777215;
   }

   private Map<class_1297, Integer> glowing() {
      long frame = Render3DEvent.frame();
      if (frame != this.glowFrame) {
         this.glowFrame = frame;
         this.glowing.clear();

         for(Target target : this.targets(1.0F)) {
            this.glowing.put(target.entity(), this.colorFor(target));
         }
      }

      return this.glowing;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_638 level = class_310.method_1551().field_1687;
      if (level != this.glowLevel || !this.sShape.is("Glow")) {
         this.resetGlow();
      }

      this.glowLevel = level;
   }

   private void resetGlow() {
      this.glowing.clear();
      this.mobileTargets.reset(0);
      this.mobileStyles.clear();
      this.glowFrame = -1L;
      this.glowLevel = null;
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      boolean box = this.sBox.get() && !this.sShape.is("Glow");
      boolean tracers = this.sTracers.get();
      if (box || tracers) {
         String shape = this.sShape.get();
         float lineWidth = this.sLineWidth.get();
         int fillAlpha = Math.round(this.sFillAlpha.get() * 255.0F);
         boolean mobile = MobilePerformance.active();
         if (mobile) {
            this.mobileStyles.clear();
         }

         for(Target target : this.targets(event.partialTick)) {
            int color = this.colorFor(target);
            class_238 bounds = mobile ? interpolatedBox(target.entity(), target.interpolated()) : interpolatedBox(target.entity(), event.partialTick);
            if (box) {
               int fill = color & 16777215 | fillAlpha << 24;
               class_12179 style = mobile ? (class_12179)this.mobileStyles.get(color) : null;
               if (style == null) {
                  class_12179 var10000;
                  switch (shape) {
                     case "Fill" -> var10000 = class_12179.method_75539(fill);
                     case "Both" -> var10000 = class_12179.method_75537(color, lineWidth, fill);
                     default -> var10000 = class_12179.method_75536(color, lineWidth);
                  }

                  style = var10000;
                  if (mobile) {
                     this.mobileStyles.put(color, style);
                  }
               }

               class_12180.method_75541(bounds, style).method_75533();
            }

            if (tracers) {
               TracerEngine.draw(bounds.method_1005(), this.sCustomTracerColor.get() ? this.sTracerColor.argb() : color);
            }
         }

      }
   }

   private List<Target> targets(float partialTick) {
      class_310 mc = class_310.method_1551();
      if (mc.field_1687 != null && mc.field_1724 != null) {
         int max = Math.max(1, Math.min(96, this.sMaxEntities.getInt()));
         float range = Math.max(1.0F, this.sRange.get());
         double rangeSq = (double)range * (double)range;
         class_243 eye = mc.field_1724.method_5836(partialTick);
         boolean mobile = MobilePerformance.active();
         if (mobile) {
            this.mobileTargets.reset(max);
         }

         List<Target> result = mobile ? null : new ArrayList();

         for(class_1297 entity : mc.field_1687.method_18112()) {
            Group group = groupOf(entity);
            if (group != null && this.shown(group) && (entity != mc.field_1724 || !this.sIgnoreSelf.get()) && !isGone(entity)) {
               if (entity instanceof class_1657) {
                  class_1657 player = (class_1657)entity;
                  if (player.method_7325()) {
                     continue;
                  }
               }

               class_243 interpolated = entity.method_30950(partialTick);
               double distanceSq = interpolated.method_1025(eye);
               if (!(distanceSq > rangeSq)) {
                  Target target = new Target(entity, group, distanceSq, mobile ? interpolated : null);
                  if (mobile) {
                     this.mobileTargets.offer(target);
                  } else {
                     result.add(target);
                  }
               }
            }
         }

         if (mobile) {
            return this.mobileTargets.result();
         } else if (result.size() <= max) {
            return result;
         } else {
            result.sort(PRIORITY);
            return result.subList(0, max);
         }
      } else {
         return List.of();
      }
   }

   private static boolean isGone(class_1297 entity) {
      boolean var10000;
      if (!entity.method_31481()) {
         label26: {
            if (entity instanceof class_1309) {
               class_1309 living = (class_1309)entity;
               if (living.method_29504()) {
                  break label26;
               }
            }

            var10000 = false;
            return var10000;
         }
      }

      var10000 = true;
      return var10000;
   }

   private static Group groupOf(class_1297 entity) {
      if (entity instanceof class_1657) {
         return EspModule.Group.PLAYER;
      } else if (entity instanceof class_1569) {
         return EspModule.Group.HOSTILE;
      } else {
         return entity instanceof class_1308 ? EspModule.Group.PASSIVE : null;
      }
   }

   private boolean shown(Group group) {
      boolean var10000;
      switch (group.ordinal()) {
         case 0 -> var10000 = this.sPlayers.get();
         case 1 -> var10000 = this.sMobs.get();
         case 2 -> var10000 = this.sAnimals.get();
         default -> throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private int colorFor(Target target) {
      int var10000;
      switch (target.group().ordinal()) {
         case 0:
            if (this.sFriends.get()) {
               class_1297 var3 = target.entity();
               if (var3 instanceof class_1657) {
                  class_1657 player = (class_1657)var3;
                  if (FriendStore.getInstance().isFriend(player.method_7334().name())) {
                     var10000 = this.sFriendColor.argb();
                     break;
                  }
               }
            }

            var10000 = this.sPlayerColor.argb();
            break;
         case 1:
            var10000 = this.sMobColor.argb();
            break;
         case 2:
            var10000 = this.sAnimalColor.argb();
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static class_238 interpolatedBox(class_1297 entity, float partialTick) {
      class_243 current = entity.method_73189();
      class_243 interpolated = entity.method_30950(partialTick);
      return entity.method_5829().method_989(interpolated.field_1352 - current.field_1352, interpolated.field_1351 - current.field_1351, interpolated.field_1350 - current.field_1350);
   }

   private static class_238 interpolatedBox(class_1297 entity, class_243 interpolated) {
      class_243 current = entity.method_73189();
      return entity.method_5829().method_989(interpolated.field_1352 - current.field_1352, interpolated.field_1351 - current.field_1351, interpolated.field_1350 - current.field_1350);
   }

   @Environment(EnvType.CLIENT)
   private static enum Group {
      PLAYER,
      HOSTILE,
      PASSIVE;

      // $FF: synthetic method
      private static Group[] $values() {
         return new Group[]{PLAYER, HOSTILE, PASSIVE};
      }
   }

   @Environment(EnvType.CLIENT)
   private static record Target(class_1297 entity, Group group, double distanceSq, class_243 interpolated) {
   }
}
