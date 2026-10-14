package net.anubis.client.module.impl.render;

import com.example.client.mixin.AccessorHandPlayerModel;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_3489;
import net.minecraft.class_4587;
import net.minecraft.class_811;

@Environment(EnvType.CLIENT)
public final class HandModelModule extends ClientModule {
   public static volatile HandModelModule active;
   private static final String ALL_ITEMS = "All Items";
   private static final String WEAPONS_TOOLS = "Weapons & Tools";
   private final ModeSetting items = (ModeSetting)this.addSetting("items", new ModeSetting("Items", "All Items", new String[]{"All Items", "Weapons & Tools"}));
   private final BooleanSetting mainHand = (BooleanSetting)this.addSetting("main-hand", new BooleanSetting("Main Hand", true));
   private final BooleanSetting offHand = (BooleanSetting)this.addSetting("off-hand", new BooleanSetting("Off Hand", true));

   public HandModelModule() {
      super("3d-hand", "3D Hand", "Shows your own arm holding the item in first person: swords, tools, items and blocks.", ModuleCategory.VISUALS);
   }

   protected void onEnable() {
      active = this;
   }

   protected void onDisable() {
      active = null;
   }

   static boolean wants(boolean mainHand) {
      boolean var10000;
      label25: {
         HandModelModule module = active;
         if (module != null) {
            if (mainHand) {
               if (module.mainHand.get()) {
                  break label25;
               }
            } else if (module.offHand.get()) {
               break label25;
            }
         }

         var10000 = false;
         return var10000;
      }

      var10000 = true;
      return var10000;
   }

   static boolean covers(class_1799 stack, boolean mainHand) {
      HandModelModule module = active;
      return wants(mainHand) && module != null && (module.items.is("All Items") || weaponOrTool(stack));
   }

   private static boolean weaponOrTool(class_1799 stack) {
      return stack.method_31573(class_3489.field_42611) || stack.method_31573(class_3489.field_42612) || stack.method_31573(class_3489.field_42614) || stack.method_31573(class_3489.field_42615) || stack.method_31573(class_3489.field_42613) || stack.method_31573(class_3489.field_63257) || stack.method_31574(class_1802.field_49814) || stack.method_31574(class_1802.field_8378) || stack.method_31574(class_1802.field_8868) || stack.method_31574(class_1802.field_8884) || stack.method_31574(class_1802.field_42716) || stack.method_31574(class_1802.field_8184) || stack.method_31574(class_1802.field_23254);
   }

   public static void shoulder(class_4587 pose, boolean right, class_1007<?> renderer) {
      FirstPersonArm.shoulder(pose, right, ((AccessorHandPlayerModel)renderer.method_4038()).anubis$handSlim());
   }

   public static void renderArm(class_4587 pose, class_1309 holder, class_1799 stack, class_811 context, class_11659 collector, int light, boolean mainHand) {
      if (covers(stack, mainHand)) {
         FirstPersonArm.Arm arm = FirstPersonArm.arm();
         if (arm != null) {
            FirstPersonArm.hold(pose, arm, stack, context, holder, collector, light, 0.0F, 0.0F, 0.0F);
         }

      }
   }
}
