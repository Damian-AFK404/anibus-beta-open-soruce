package net.anubis.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CombatMacroLock {
   private static Object holder;

   private CombatMacroLock() {
   }

   public static boolean tryAcquire(Object owner) {
      Object var2 = holder;
      if (var2 instanceof ClientModule module) {
         if (!module.isEnabled()) {
            holder = null;
         }
      }

      if (holder != null && holder != owner) {
         return false;
      } else {
         holder = owner;
         return true;
      }
   }

   public static void release(Object owner) {
      if (holder == owner) {
         holder = null;
      }

   }

   public static boolean heldByOther(Object owner) {
      Object var2 = holder;
      if (var2 instanceof ClientModule module) {
         if (!module.isEnabled()) {
            holder = null;
         }
      }

      return holder != null && holder != owner;
   }
}
