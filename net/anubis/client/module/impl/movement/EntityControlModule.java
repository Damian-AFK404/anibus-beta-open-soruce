package net.anubis.client.module.impl.movement;

import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.anubis.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10255;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_2692;

@Environment(EnvType.CLIENT)
public final class EntityControlModule extends ClientModule {
   public static volatile EntityControlModule active;
   private final BooleanSetting boats = (BooleanSetting)this.addSetting("boats", new BooleanSetting("Boats", true));
   private final BooleanSetting horses = (BooleanSetting)this.addSetting("horses", new BooleanSetting("Horses", true));
   private final BooleanSetting camels = (BooleanSetting)this.addSetting("camels", new BooleanSetting("Camels", true));
   private final BooleanSetting pigs = (BooleanSetting)this.addSetting("pigs", new BooleanSetting("Pigs", true));
   private final BooleanSetting striders = (BooleanSetting)this.addSetting("striders", new BooleanSetting("Striders", true));
   private final BooleanSetting nautiluses = (BooleanSetting)this.addSetting("nautiluses", new BooleanSetting("Nautiluses", true));
   private final BooleanSetting happyGhasts = (BooleanSetting)this.addSetting("happy-ghasts", new BooleanSetting("Happy Ghasts", true));
   private final BooleanSetting spoofSaddle = (BooleanSetting)this.addSetting("spoof-saddle", new BooleanSetting("Spoof Saddle", false));
   private final BooleanSetting maxJump = (BooleanSetting)this.addSetting("max-jump", new BooleanSetting("Max Jump", true));
   private final BooleanSetting lockYaw = (BooleanSetting)this.addSetting("lock-yaw", new BooleanSetting("Lock Yaw", true));
   private final BooleanSetting cancelServerPackets = (BooleanSetting)this.addSetting("cancel-server-packets", new BooleanSetting("Cancel Server Packets", false));

   public EntityControlModule() {
      super("entity-control", "Entity Control", "Lets you control rideable entities without a saddle.", ModuleCategory.MOVEMENT);
   }

   protected void onEnable() {
      active = this;
      EntityControlVehicleHook.refresh();
      this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
   }

   protected void onDisable() {
      active = null;
      EntityControlVehicleHook.refresh();
   }

   private void onPacketReceive(PacketReceiveNettyEvent event) {
      if (this.cancelServerPackets.get() && event.packet instanceof class_2692) {
         event.setCancelled(true);
      }

   }

   public static boolean spoofSaddle() {
      EntityControlModule module = active;
      return module != null && module.spoofSaddle.get();
   }

   public static boolean maxJump() {
      EntityControlModule module = active;
      return module != null && module.maxJump.get();
   }

   public boolean locksYaw(class_1297 vehicle) {
      return this.lockYaw.get() && this.isTarget(vehicle);
   }

   private boolean isTarget(class_1297 entity) {
      if (entity instanceof class_10255) {
         return this.boats.get();
      } else {
         class_1299<?> type = entity.method_5864();
         if (type != class_1299.field_6139 && type != class_1299.field_6067 && type != class_1299.field_6057 && type != class_1299.field_6075 && type != class_1299.field_6048) {
            if (type != class_1299.field_40116 && type != class_1299.field_64132) {
               if (type == class_1299.field_6093) {
                  return this.pigs.get();
               } else if (type == class_1299.field_23214) {
                  return this.striders.get();
               } else if (type != class_1299.field_63289 && type != class_1299.field_63290) {
                  return type == class_1299.field_59668 ? this.happyGhasts.get() : false;
               } else {
                  return this.nautiluses.get();
               }
            } else {
               return this.camels.get();
            }
         } else {
            return this.horses.get();
         }
      }
   }
}
