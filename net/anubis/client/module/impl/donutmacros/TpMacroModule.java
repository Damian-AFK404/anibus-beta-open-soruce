package net.anubis.client.module.impl.donutmacros;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_310;
import net.minecraft.class_634;
import net.minecraft.class_7439;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class TpMacroModule extends ClientModule {
   private static final Pattern RATE_LIMIT = Pattern.compile("you need to wait another ([\\d.]+) seconds? to execute a command");
   private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");
   private final ModeSetting command = (ModeSetting)this.addSetting("command", new ModeSetting("Command", "tpa", new String[]{"tpa", "tpahere"}));
   private final StringSetting target = (StringSetting)this.addSetting("player", (new StringSetting("Player", "")).hint("Player name"));
   private final NumberSetting interval = (NumberSetting)this.addSetting("interval", (new NumberSetting("Interval (s)", 1.0F, 0.25F, 60.0F)).step(0.25F));
   private final TeleportConfirm confirm = new TeleportConfirm(this);
   private volatile long holdUntil;
   private long nextAt;
   private int sent;

   public TpMacroModule() {
      super("tp-macro", "Tp Macro", "Sends /tpa or /tpahere to one player on a timer.", ModuleCategory.DONUT_MACROS);
      this.usesNotifications();
      this.target.privateValue();
   }

   public boolean persistEnabled() {
      return false;
   }

   protected void onEnable() {
      String name = this.target.get().trim();
      if (!VALID_NAME.matcher(name).matches()) {
         ModuleChat.send(this, (ModuleChat.Body)ModuleChat.body().warn("Set a valid Player name first."));
         this.requestDisable();
      } else {
         this.confirm.reset();
         this.holdUntil = 0L;
         this.nextAt = 0L;
         this.sent = 0;
         ModuleChat.Body var10001 = ModuleChat.body().text("Sending ");
         String var10002 = this.command.get();
         ModuleChat.send(this, (ModuleChat.Body)var10001.value("/" + var10002 + " " + name).text(" every ").value(this.interval.formatted() + "s").text("."));
      }
   }

   protected void onDisable() {
      if (this.sent > 0) {
         ModuleChat.send(this, (ModuleChat.Body)ModuleChat.body().text("Sent ").value(this.sent).text(this.sent == 1 ? " request." : " requests."));
      }

   }

   @EventHandler
   public void onPacket(PacketReceiveNettyEvent event) {
      class_2596 var3 = event.packet;
      if (var3 instanceof class_7439 chat) {
         if (chat.comp_763() != null) {
            Matcher wait = RATE_LIMIT.matcher(ServerText.normalize(chat.comp_763().getString()));
            if (!wait.find()) {
               return;
            }

            try {
               this.holdUntil = System.currentTimeMillis() + (long)Math.ceil(Double.parseDouble(wait.group(1)) * (double)1000.0F) + 80L;
            } catch (NumberFormatException var5) {
            }

            return;
         }
      }

   }

   @EventHandler
   public void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      class_746 player = mc.field_1724;
      if (player != null && mc.method_1562() != null && !player.method_29504()) {
         this.confirm.tick(mc);
         long now = System.currentTimeMillis();
         if (now >= this.nextAt && now >= this.holdUntil) {
            class_634 var10000 = player.field_3944;
            String var10001 = this.command.get();
            var10000.method_45730(var10001 + " " + this.target.get().trim());
            ++this.sent;
            this.nextAt = now + (long)(this.interval.get() * 1000.0F);
         }
      }
   }
}
