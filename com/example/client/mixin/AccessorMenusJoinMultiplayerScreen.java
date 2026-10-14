package com.example.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_500;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin({class_500.class})
public interface AccessorMenusJoinMultiplayerScreen {
   @Invoker("method_2534")
   void anubis$refreshServerList();
}
