package net.anubis.client.module.impl.movement;

import java.util.Objects;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.anubis.client.module.impl.movement.entitycontrol.VehicleAntiKick;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1316;
import net.minecraft.class_2596;
import net.minecraft.class_2692;
import net.minecraft.class_2833;
import net.minecraft.class_310;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class BoatFlyModule extends ClientModule {
   public static volatile BoatFlyModule active;
   private final NumberSetting speed = (NumberSetting)this.addSetting("speed", (new NumberSetting("Speed", 10.0F, 0.0F, 50.0F)).step(0.5F));
   private final NumberSetting verticalSpeed = (NumberSetting)this.addSetting("vertical-speed", (new NumberSetting("Vertical Speed", 6.0F, 0.0F, 20.0F)).step(0.5F));
   private final NumberSetting fallSpeed = (NumberSetting)this.addSetting("fall-speed", (new NumberSetting("Fall Speed", 0.0F, 0.0F, 10.0F)).step(0.1F));
   private final BooleanSetting antiKick = (BooleanSetting)this.addSetting("anti-fly-kick", new BooleanSetting("Anti Fly Kick", true));
   private final NumberSetting delay = (NumberSetting)this.addSetting("delay", (new NumberSetting("Delay", 40.0F, 1.0F, 80.0F)).step(1.0F));
   private final BooleanSetting cancelServerPackets = (BooleanSetting)this.addSetting("cancel-server-packets", new BooleanSetting("Cancel Server Packets", false));
   private final VehicleAntiKick antiKicker = new VehicleAntiKick();

   public BoatFlyModule() {
      super("boat-fly", "Boat Fly", "Transforms your boat (or any ridden vehicle) into a plane.", ModuleCategory.MOVEMENT);
      NumberSetting var10000 = this.delay;
      BooleanSetting var10001 = this.antiKick;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      BooleanSetting var1 = this.antiKick;
      VehicleAntiKick var2 = this.antiKicker;
      Objects.requireNonNull(var2);
      var1.onChange(var2::forget);
   }

   public boolean persistEnabled() {
      return false;
   }

   protected void onEnable() {
      BoatNoclipModule noclip = (BoatNoclipModule)ModuleRegistry.get(BoatNoclipModule.class);
      if (noclip != null) {
         noclip.setEnabled(false);
      }

      this.antiKicker.start(this.delay.getInt());
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(TickEvent.TYPE, Stage.PRE, (event) -> this.antiKicker.tick());
      this.on(PacketSendEvent.TYPE, this::onPacketSend);
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
   }

   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
      this.antiKicker.stop();
   }

   private void onPacketSend(PacketSendEvent event) {
      if (this.antiKick.get()) {
         class_2596 var3 = event.packet;
         if (var3 instanceof class_2833) {
            class_2833 packet = (class_2833)var3;
            this.antiKicker.onPacketSend(packet, this.delay.getInt());
         }
      }

   }

   private void onPacketReceive(PacketReceiveNettyEvent event) {
      if (this.cancelServerPackets.get() && event.packet instanceof class_2692) {
         event.setCancelled(true);
      }

   }

   public void onVehicleSetback() {
      this.antiKicker.forget();
   }

   public double horizontalSpeed() {
      return (double)this.speed.get();
   }

   public double verticalVelocity() {
      class_310 mc = class_310.method_1551();
      double velY = (double)0.0F;
      if (EntityControlVehicleHook.isPhysicallyDown(mc.field_1690.field_1903)) {
         velY += (double)this.verticalSpeed.get() / (double)20.0F;
      }

      if (EntityControlVehicleHook.isPhysicallyDown(mc.field_1690.field_1867)) {
         velY -= (double)this.verticalSpeed.get() / (double)20.0F;
      } else {
         velY -= (double)this.fallSpeed.get() / (double)20.0F;
      }

      return velY;
   }

   public static boolean cancelsMountJump(class_746 player) {
      return active != null && player.method_5854() instanceof class_1316;
   }
}
