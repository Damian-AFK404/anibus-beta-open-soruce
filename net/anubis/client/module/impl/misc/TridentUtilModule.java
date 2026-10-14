package net.anubis.client.module.impl.misc;

import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TridentUtilModule extends ClientModule {
   private final NumberSetting chargeScale = (NumberSetting)this.addSetting("scale", (new NumberSetting("Charge Scale", 0.0F, 0.0F, 1.0F)).step(0.1F));
   private final BooleanSetting noWater = (BooleanSetting)this.addSetting("no-water", new BooleanSetting("No Water", true));

   public TridentUtilModule() {
      super("trident-util", "Trident Util", "Use tridents out of water and charge them faster.", ModuleCategory.DONUT);
   }

   private static TridentUtilModule active() {
      TridentUtilModule module = (TridentUtilModule)ModuleRegistry.get(TridentUtilModule.class);
      return module != null && module.isEnabled() ? module : null;
   }

   public static boolean allowOutOfWater() {
      TridentUtilModule module = active();
      return module != null && module.noWater.get();
   }

   public static int minChargeTicks(int vanilla) {
      TridentUtilModule module = active();
      return module == null ? vanilla : Math.max(1, Math.round((float)vanilla * module.chargeScale.get()));
   }
}
