package net.anubis.client.module.impl.client;

import java.util.List;
import java.util.Objects;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HudModule extends ClientModule {
   private final BooleanSetting coordinates = (BooleanSetting)this.addSetting("coordinates", new BooleanSetting("Overworld Coords", true));
   private final BooleanSetting netherCoordinates = (BooleanSetting)this.addSetting("nether-coordinates", new BooleanSetting("Nether Coords", true));
   private final BooleanSetting ping = (BooleanSetting)this.addSetting("ping", new BooleanSetting("Ping", true));
   private final BooleanSetting fps = (BooleanSetting)this.addSetting("fps", new BooleanSetting("FPS", true));
   private final BooleanSetting clock = (BooleanSetting)this.addSetting("clock", new BooleanSetting("Clock", true));
   private final BooleanSetting logo = (BooleanSetting)this.addSetting("logo", new BooleanSetting("Logo", true));
   private final BooleanSetting moduleList = (BooleanSetting)this.addSetting("module-list", new BooleanSetting("Module List", true));
   private final BooleanSetting potions = (BooleanSetting)this.addSetting("potions", new BooleanSetting("Potions", true));
   private final BooleanSetting targetCard = (BooleanSetting)this.addSetting("target-card", new BooleanSetting("Target HUD", true));
   private final BooleanSetting targetPlayersOnly = (BooleanSetting)this.addSetting("target-players-only", new BooleanSetting("Players Only", false));
   private final BooleanSetting targetOnLook = (BooleanSetting)this.addSetting("target-on-look", new BooleanSetting("Show On Look", false));
   private final BooleanSetting targetGear = (BooleanSetting)this.addSetting("target-gear", new BooleanSetting("Show Gear", true));
   private final BooleanSetting armorStatus = (BooleanSetting)this.addSetting("armor-status", new BooleanSetting("Armor HUD", true));
   private final ModeSetting armorLayout = (ModeSetting)this.addSetting("armor-layout", new ModeSetting("Armor Layout", "Row", new String[]{"Row", "Column"}));
   private final ModeSetting armorWear = (ModeSetting)this.addSetting("armor-wear", new ModeSetting("Wear As", "Gauge", new String[]{"Gauge", "Percent", "Gauge + Percent"}));
   private final BooleanSetting armorHands = (BooleanSetting)this.addSetting("armor-hands", new BooleanSetting("Include Hands", false));
   private final BooleanSetting itemCooldowns = (BooleanSetting)this.addSetting("item-cooldowns", new BooleanSetting("Cooldown", true));
   private final ModeSetting cooldownDial = (ModeSetting)this.addSetting("cooldown-dial", new ModeSetting("Cooldown Dial", "Halo", new String[]{"Halo", "Sandbar"}));
   private final ModeSetting toastStyle = (ModeSetting)this.addSetting("toast-style", new ModeSetting("Toast Style", "Bar", new String[]{"Bar", "Card", "Chip"}));
   private final BooleanSetting shadow = (BooleanSetting)this.addSetting("shadow", new BooleanSetting("Shadow", true));
   private final BooleanSetting customAccent = (BooleanSetting)this.addSetting("custom-accent", new BooleanSetting("Custom Accent", false));
   private final ColorSetting accentColor = (ColorSetting)this.addSetting("accent-color", new ColorSetting("Accent Color", -13338378));

   public HudModule() {
      super("hud", "HUD", "Module list, coordinates and status overlays.", ModuleCategory.CLIENT);
      ColorSetting var10000 = this.accentColor;
      BooleanSetting var10001 = this.customAccent;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      BooleanSetting var1 = this.targetPlayersOnly;
      var10001 = this.targetCard;
      Objects.requireNonNull(var10001);
      var1.visibleWhen(var10001::get);
      var1 = this.targetOnLook;
      var10001 = this.targetCard;
      Objects.requireNonNull(var10001);
      var1.visibleWhen(var10001::get);
      var1 = this.targetGear;
      var10001 = this.targetCard;
      Objects.requireNonNull(var10001);
      var1.visibleWhen(var10001::get);
      ModeSetting var4 = this.armorLayout;
      var10001 = this.armorStatus;
      Objects.requireNonNull(var10001);
      var4.visibleWhen(var10001::get);
      var4 = this.armorWear;
      var10001 = this.armorStatus;
      Objects.requireNonNull(var10001);
      var4.visibleWhen(var10001::get);
      BooleanSetting var6 = this.armorHands;
      var10001 = this.armorStatus;
      Objects.requireNonNull(var10001);
      var6.visibleWhen(var10001::get);
      ModeSetting var7 = this.cooldownDial;
      var10001 = this.itemCooldowns;
      Objects.requireNonNull(var10001);
      var7.visibleWhen(var10001::get);
   }

   public boolean enabledByDefault() {
      return true;
   }

   public boolean coordinates() {
      return this.coordinates.get();
   }

   public boolean netherCoordinates() {
      return this.netherCoordinates.get();
   }

   public boolean ping() {
      return this.ping.get();
   }

   public boolean fps() {
      return this.fps.get();
   }

   public boolean clock() {
      return this.clock.get();
   }

   public boolean logo() {
      return this.logo.get();
   }

   public boolean moduleList() {
      return this.moduleList.get();
   }

   public boolean potions() {
      return this.potions.get();
   }

   public boolean targetCard() {
      return this.targetCard.get();
   }

   public boolean targetPlayersOnly() {
      return this.targetPlayersOnly.get();
   }

   public boolean targetOnLook() {
      return this.targetOnLook.get();
   }

   public boolean targetGear() {
      return this.targetGear.get();
   }

   public boolean armorStatus() {
      return this.armorStatus.get();
   }

   public boolean armorColumn() {
      return this.armorLayout.is("Column");
   }

   public boolean armorHands() {
      return this.armorHands.get();
   }

   public boolean itemCooldowns() {
      return this.itemCooldowns.get();
   }

   public boolean cooldownSandbar() {
      return this.cooldownDial.is("Sandbar");
   }

   public boolean shadow() {
      return this.shadow.get();
   }

   public boolean customAccent() {
      return this.customAccent.get();
   }

   public int accentArgb() {
      return this.accentColor.argb();
   }

   public ArmorWear armorWear() {
      ArmorWear var10000;
      switch (this.armorWear.get()) {
         case "Percent" -> var10000 = HudModule.ArmorWear.PERCENT;
         case "Gauge + Percent" -> var10000 = HudModule.ArmorWear.BOTH;
         default -> var10000 = HudModule.ArmorWear.GAUGE;
      }

      return var10000;
   }

   public ToastStyle toastStyle() {
      ToastStyle var10000;
      switch (this.toastStyle.get()) {
         case "Card" -> var10000 = HudModule.ToastStyle.CARD;
         case "Chip" -> var10000 = HudModule.ToastStyle.CHIP;
         default -> var10000 = HudModule.ToastStyle.BAR;
      }

      return var10000;
   }

   public List<Setting> elementSettings(String elementId) {
      List var10000;
      switch (elementId) {
         case "info_bar" -> var10000 = List.of(this.clock, this.fps, this.ping);
         case "coordinates" -> var10000 = List.of(this.coordinates, this.netherCoordinates);
         case "module_list" -> var10000 = List.of(this.moduleList);
         case "potion_tracker" -> var10000 = List.of(this.potions);
         case "target_card" -> var10000 = List.of(this.targetCard, this.targetPlayersOnly, this.targetOnLook, this.targetGear);
         case "armor_status" -> var10000 = List.of(this.armorStatus, this.armorLayout, this.armorWear, this.armorHands);
         case "item_cooldowns" -> var10000 = List.of(this.itemCooldowns, this.cooldownDial);
         case "toggle_toasts" -> var10000 = List.of(this.toastStyle);
         default -> var10000 = List.of();
      }

      return var10000;
   }

   @Environment(EnvType.CLIENT)
   public static enum ArmorWear {
      GAUGE,
      PERCENT,
      BOTH;

      // $FF: synthetic method
      private static ArmorWear[] $values() {
         return new ArmorWear[]{GAUGE, PERCENT, BOTH};
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum ToastStyle {
      BAR,
      CARD,
      CHIP;

      // $FF: synthetic method
      private static ToastStyle[] $values() {
         return new ToastStyle[]{BAR, CARD, CHIP};
      }
   }
}
