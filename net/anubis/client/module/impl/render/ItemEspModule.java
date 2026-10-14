package net.anubis.client.module.impl.render;

import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ItemColorListSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.MobilePerformance;
import net.anubis.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1542;
import net.minecraft.class_1792;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class ItemEspModule extends ClientModule {
   private static ItemEspModule instance;
   private final NumberSetting sRange = (NumberSetting)this.addSetting("range", (new NumberSetting("Range", 128.0F, 10.0F, 512.0F)).step(1.0F));
   private final BooleanSetting sOutline = (BooleanSetting)this.addSetting("outline", new BooleanSetting("Outline", true));
   private final BooleanSetting sTracers = (BooleanSetting)this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final ItemColorListSetting sItemColors = (ItemColorListSetting)this.addSetting(ItemColorListSetting.createDefault());

   public ItemEspModule() {
      super("item-esp", "Item ESP", "Highlights selected dropped items with model outlines and tracers.", ModuleCategory.RENDER);
      instance = this;
   }

   public static boolean shouldGlow(class_1297 entity) {
      ItemEspModule module = instance;
      if (module != null && module.isEnabled() && module.sOutline.get()) {
         boolean var10000;
         if (entity instanceof class_1542) {
            class_1542 item = (class_1542)entity;
            if (module.wants(item, 1.0F)) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      } else {
         return false;
      }
   }

   public static int glowColor(class_1297 entity) {
      ItemEspModule module = instance;
      if (module != null && entity instanceof class_1542 item) {
         return module.sItemColors.colorArgb(item.method_6983().method_7909()) & 16777215;
      } else {
         return 16777215;
      }
   }

   private boolean wants(class_1542 item, float partialTick) {
      if (item.method_31481()) {
         return false;
      } else {
         class_1792 kind = item.method_6983().method_7909();
         if (!this.sItemColors.isEnabled(kind)) {
            return false;
         } else {
            class_310 mc = class_310.method_1551();
            if (mc.field_1724 == null) {
               return true;
            } else {
               float range = Math.max(1.0F, this.sRange.get());
               return item.method_30950(partialTick).method_1025(mc.field_1724.method_5836(partialTick)) <= (double)range * (double)range;
            }
         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.sTracers.get() && this.sItemColors.hasAnyEnabled()) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1687 != null) {
            if (!MobilePerformance.active()) {
               for(class_1297 entity : mc.field_1687.method_18112()) {
                  if (entity instanceof class_1542) {
                     class_1542 item = (class_1542)entity;
                     if (this.wants(item, event.partialTick)) {
                        class_243 pos = item.method_30950(event.partialTick);
                        TracerEngine.draw(pos, this.sItemColors.colorArgb(item.method_6983().method_7909()));
                     }
                  }
               }

            } else {
               class_243 eye = mc.field_1724 == null ? null : mc.field_1724.method_5836(event.partialTick);
               float range = Math.max(1.0F, this.sRange.get());
               double rangeSq = (double)range * (double)range;

               for(class_1297 entity : mc.field_1687.method_18112()) {
                  if (entity instanceof class_1542) {
                     class_1542 item = (class_1542)entity;
                     if (!item.method_31481()) {
                        class_1792 kind = item.method_6983().method_7909();
                        if (this.sItemColors.isEnabled(kind)) {
                           class_243 pos = item.method_30950(event.partialTick);
                           if (eye == null || pos.method_1025(eye) <= rangeSq) {
                              TracerEngine.draw(pos, this.sItemColors.colorArgb(kind));
                           }
                        }
                     }
                  }
               }

            }
         }
      }
   }
}
