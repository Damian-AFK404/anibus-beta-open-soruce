package net.anubis.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Stage {
   PRE,
   POST;

   // $FF: synthetic method
   private static Stage[] $values() {
      return new Stage[]{PRE, POST};
   }
}
