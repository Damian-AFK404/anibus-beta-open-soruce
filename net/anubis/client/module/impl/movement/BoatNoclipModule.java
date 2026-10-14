package net.anubis.client.module.impl.movement;

import java.util.Objects;
import net.anubis.client.event.Stage;
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
import net.minecraft.class_10255;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_265;
import net.minecraft.class_2680;
import net.minecraft.class_2833;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3726;
import net.minecraft.class_638;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public final class BoatNoclipModule extends ClientModule {
   public static volatile BoatNoclipModule active;
   private final NumberSetting speed = (NumberSetting)this.addSetting("speed", (new NumberSetting("Speed", 10.0F, 0.0F, 50.0F)).step(0.5F));
   private final NumberSetting speedInsideBlocks = (NumberSetting)this.addSetting("speed-inside-blocks", (new NumberSetting("Speed Inside Blocks", 5.0F, 0.0F, 50.0F)).step(0.5F));
   private final NumberSetting verticalSpeed = (NumberSetting)this.addSetting("vertical-speed", (new NumberSetting("Vertical Speed", 6.0F, 0.0F, 20.0F)).step(0.5F));
   private final NumberSetting fallSpeed = (NumberSetting)this.addSetting("fall-speed", (new NumberSetting("Fall Speed", 0.0F, 0.0F, 10.0F)).step(0.1F));
   private final BooleanSetting antiKick = (BooleanSetting)this.addSetting("anti-fly-kick", new BooleanSetting("Anti Fly Kick", true));
   private final NumberSetting delay = (NumberSetting)this.addSetting("delay", (new NumberSetting("Delay", 40.0F, 1.0F, 80.0F)).step(1.0F));
   private final VehicleAntiKick antiKicker = new VehicleAntiKick();
   private class_10255 noclipBoat;

   public BoatNoclipModule() {
      super("boat-noclip", "Boat Noclip", "Fly your boat through blocks and entities.", ModuleCategory.MOVEMENT);
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
      BoatFlyModule fly = (BoatFlyModule)ModuleRegistry.get(BoatFlyModule.class);
      if (fly != null) {
         fly.setEnabled(false);
      }

      this.antiKicker.start(this.delay.getInt());
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
      this.on(PacketSendEvent.TYPE, this::onPacketSend);
   }

   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
      this.antiKicker.stop();
      this.releaseBoat();
   }

   private void onPreTick(TickEvent event) {
      class_746 player = class_310.method_1551().field_1724;
      if (this.noclipBoat != null && (player == null || player.method_5854() != this.noclipBoat)) {
         this.releaseBoat();
      }

      this.antiKicker.tick();
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

   public void onVehicleSetback() {
      this.antiKicker.forget();
   }

   private void releaseBoat() {
      if (this.noclipBoat != null) {
         this.noclipBoat.field_5960 = false;
      }

      this.noclipBoat = null;
   }

   public class_243 boatMovement(class_10255 boat, class_746 player) {
      if (this.noclipBoat != boat) {
         this.releaseBoat();
         this.noclipBoat = boat;
      }

      boat.field_5960 = true;
      double bps = isInsideBlock(boat, player) ? (double)this.speedInsideBlocks.get() : (double)this.speed.get();
      class_243 horizontal = EntityControlVehicleHook.horizontalVelocity(player, bps);
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

      return new class_243(horizontal.field_1352, velY, horizontal.field_1350);
   }

   private static boolean isInsideBlock(class_10255 boat, class_746 player) {
      class_638 level = class_310.method_1551().field_1687;
      if (level == null) {
         return false;
      } else {
         class_238 boatBox = boat.method_5829();
         class_238 box = boatBox.method_1002((double)0.0F, 0.05, (double)0.0F).method_1009((double)0.5F, (double)0.0F, (double)0.5F);
         int maxX = class_3532.method_15357(box.field_1320);
         int maxY = class_3532.method_15357(box.field_1325);
         int maxZ = class_3532.method_15357(box.field_1324);
         class_2338.class_2339 pos = new class_2338.class_2339();

         for(int x = class_3532.method_15357(box.field_1323); x <= maxX; ++x) {
            for(int y = class_3532.method_15357(box.field_1322); y <= maxY; ++y) {
               for(int z = class_3532.method_15357(box.field_1321); z <= maxZ; ++z) {
                  class_2680 state = level.method_8320(pos.method_10103(x, y, z));
                  if (!state.method_26215()) {
                     class_265 shape = state.method_26194(level, pos, class_3726.method_16194());
                     if (!shape.method_1110() && boatBox.method_994(shape.method_1107().method_996(pos))) {
                        return true;
                     }
                  }
               }
            }
         }

         for(class_1297 other : level.method_8335(boat, box)) {
            if (other != player && !other.method_5626(boat) && !boat.method_5626(other) && other.method_5805() && other.method_5829().method_994(box)) {
               return true;
            }
         }

         return false;
      }
   }
}
