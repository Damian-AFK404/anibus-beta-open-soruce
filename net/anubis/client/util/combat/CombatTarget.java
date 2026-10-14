package net.anubis.client.util.combat;

import net.anubis.client.event.EventPriority;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.PlayerAttackEntityEvent;
import net.anubis.client.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_156;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class CombatTarget {
   private static final Object OWNER = new Object();
   private static final long MEMORY_CAP_MS = 30000L;
   private static class_1309 lastHit;
   private static long lastHitAt;

   private CombatTarget() {
   }

   public static void install() {
      PlayerAttackEntityEvent.TYPE.listen(OWNER, Stage.PRE, EventPriority.LOWEST, (event) -> {
         if (!event.isCancelled()) {
            class_1297 patt0$temp = event.target;
            if (patt0$temp instanceof class_1309) {
               class_1309 living = (class_1309)patt0$temp;
               class_310 mc = class_310.method_1551();
               if (mc.method_18854() && living.method_73183() == mc.field_1687) {
                  lastHit = living;
                  lastHitAt = class_156.method_658();
                  return;
               }

               return;
            }
         }

      });
      TickEvent.TYPE.listen(OWNER, (event) -> forgetStale());
   }

   private static void forgetStale() {
      class_1309 entity = lastHit;
      if (entity != null) {
         if (!entity.method_5805() || entity.method_73183() != class_310.method_1551().field_1687 || class_156.method_658() - lastHitAt > 30000L) {
            lastHit = null;
         }

      }
   }

   public static class_1309 lastHit(long timeoutMs) {
      class_1309 entity = lastHit;
      if (entity != null && entity.method_5805() && entity.method_73183() == class_310.method_1551().field_1687) {
         return class_156.method_658() - lastHitAt > timeoutMs ? null : entity;
      } else {
         return null;
      }
   }

   public static class_1309 current(long timeoutMs) {
      class_1309 hit = lastHit(timeoutMs);
      if (hit != null) {
         return hit;
      } else {
         class_1297 looked = class_310.method_1551().field_1692;
         class_1309 var10000;
         if (looked instanceof class_1309) {
            class_1309 living = (class_1309)looked;
            if (living.method_5805()) {
               var10000 = living;
               return var10000;
            }
         }

         var10000 = null;
         return var10000;
      }
   }
}
