package com.example.client.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_320;
import net.minecraft.class_5520;
import net.minecraft.class_7574;
import net.minecraft.class_7853;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin({class_310.class})
public interface AccessorAccountsMinecraft {
   @Mutable
   @Accessor("field_1726")
   void anubis$setUser(class_320 var1);

   @Mutable
   @Accessor("field_26902")
   void anubis$setUserApiService(UserApiService var1);

   @Mutable
   @Accessor("field_47680")
   void anubis$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> var1);

   @Mutable
   @Accessor("field_45899")
   void anubis$setProfileFuture(CompletableFuture<ProfileResult> var1);

   @Mutable
   @Accessor("field_26842")
   void anubis$setPlayerSocialManager(class_5520 var1);

   @Mutable
   @Accessor("field_39068")
   void anubis$setProfileKeyPairManager(class_7853 var1);

   @Accessor("field_39492")
   void anubis$setReportingContext(class_7574 var1);
}
