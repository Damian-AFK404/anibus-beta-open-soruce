package net.anubis.client.module.impl.donutmacros;

import java.util.Objects;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_310;
import net.minecraft.class_7439;
import net.minecraft.class_746;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AhSniperModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final String EXACT = "Exactly these";
   private static final String AT_LEAST = "At least these";
   private final StringSetting item;
   private final StringSetting maxPrice;
   private final StringSetting enchants;
   private final ModeSetting enchantMatch;
   private final NumberSetting minHours;
   private final NumberSetting refresh;
   private final NumberSetting maxBuys;
   private final BooleanSetting autoSell;
   private final StringSetting sellPrice;
   private final MacroWorker worker;
   private volatile SniperBot bot;

   public AhSniperModule() {
      super("ah-sniper", "AH Sniper", "Refreshes the AH and instantly buys your item below your price, with optional auto selling.", ModuleCategory.DONUT_MACROS);
      this.item = (StringSetting)this.addSetting("item", (new StringSetting("Item", SnipeTarget.Preset.SHARD_AXE.value)).presets(SnipeTarget.Preset.choices()));
      this.maxPrice = (StringSetting)this.addSetting("max-price", (new StringSetting("Buy Below (each)", "")).hint("e.g. 50m"));
      this.enchants = (StringSetting)this.addSetting("enchants", (new StringSetting("Enchantments", "")).hint("e.g. protection 2, unbreaking 3, mending"));
      this.enchantMatch = (ModeSetting)this.addSetting("enchant-match", new ModeSetting("Enchant Match", "Exactly these", new String[]{"Exactly these", "At least these"}));
      this.minHours = (NumberSetting)this.addSetting("min-hours", (new NumberSetting("Min Time Left (h)", 0.0F, 0.0F, 48.0F)).step(0.5F));
      this.refresh = (NumberSetting)this.addSetting("refresh", (new NumberSetting("Refresh Every (ms)", 300.0F, 100.0F, 2000.0F)).step(50.0F));
      this.maxBuys = (NumberSetting)this.addSetting("max-buys", (new NumberSetting("Stop After (buys, 0 = never)", 0.0F, 0.0F, 100.0F)).step(1.0F));
      this.autoSell = (BooleanSetting)this.addSetting("auto-sell", new BooleanSetting("Auto Sell", false));
      this.sellPrice = (StringSetting)this.addSetting("sell-price", (new StringSetting("Sell Price (each)", "")).hint("e.g. 60m"));
      this.worker = new MacroWorker("Anubis AH Sniper");
      this.usesNotifications();
      this.enchantMatch.visibleWhen(() -> !this.enchants.get().isBlank());
      StringSetting var10000 = this.sellPrice;
      BooleanSetting var10001 = this.autoSell;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
   }

   public boolean persistEnabled() {
      return false;
   }

   protected void onEnable() {
      class_310 mc = class_310.method_1551();
      if (mc.field_1724 != null && mc.method_1562() != null && mc.method_1548() != null) {
         long max = ServerText.parseMoney(this.maxPrice.get());
         if (max <= 0L) {
            this.say(ModuleChat.body().warn("Set Buy Below to a price like 50m."));
            this.requestDisable();
         } else {
            SnipeTarget target = SnipeTarget.parse(this.item.get(), this.enchants.get(), this.enchantMatch.is("Exactly these"), max, Math.round(this.minHours.get() * 60.0F));
            if (target == null) {
               this.say(ModuleChat.body().warn("Pick an item to snipe first."));
               this.requestDisable();
            } else {
               long sell = 0L;
               if (this.autoSell.get()) {
                  sell = ServerText.parseMoney(this.sellPrice.get());
                  if (sell <= 0L) {
                     this.say(ModuleChat.body().warn("Set Sell Price to a price like 60m, or turn Auto Sell off."));
                     this.requestDisable();
                     return;
                  }

                  if (sell <= max) {
                     this.say(ModuleChat.body().warn("Sell Price must be higher than Buy Below").text(", or the sniper would buy your own listing back."));
                     this.requestDisable();
                     return;
                  }
               }

               SniperBot created = new SniperBot(this, this.worker, target, (long)this.refresh.getInt(), this.maxBuys.getInt(), sell, mc.method_1548().method_1676());
               this.bot = created;
               MacroWorker var10000 = this.worker;
               Objects.requireNonNull(created);
               var10000.start(created::run, (failure) -> {
                  LOGGER.error("[Anubis] AH Sniper stopped", failure);
                  this.say(ModuleChat.body().danger("Stopped").text(": " + String.valueOf(failure)));
                  this.stopFromBot(created);
               });
            }
         }
      } else {
         this.say(ModuleChat.body().warn("Join a server first."));
         this.requestDisable();
      }
   }

   void stopFromBot(SniperBot from) {
      class_310.method_1551().execute(() -> {
         if (this.bot == from && this.isEnabled()) {
            this.setEnabled(false);
         }

      });
   }

   protected void onDisable() {
      SniperBot stopped = this.bot;
      this.bot = null;
      this.worker.stop();
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (stopped != null && player != null) {
         if (player.field_7512 != player.field_7498) {
            player.method_7346();
         } else {
            Dialogs.close(mc);
         }

         this.say(ModuleChat.body().text(stopped.summary()));
      }

   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent event) {
      SniperBot current = this.bot;
      if (current != null) {
         class_2596 var4 = event.packet;
         if (var4 instanceof class_7439) {
            class_7439 chat = (class_7439)var4;
            if (chat.comp_763() != null) {
               current.onChat(chat.comp_763().getString());
            }
         }
      }

   }

   private void say(ModuleChat.Body body) {
      ModuleChat.send(this, (ModuleChat.Body)body);
   }
}
