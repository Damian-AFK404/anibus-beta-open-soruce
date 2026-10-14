package net.anubis.client.event.impl;

import net.anubis.client.event.CancellableEvent;
import net.anubis.client.event.PooledCancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;

@Environment(EnvType.CLIENT)
public final class PacketReceiveNettyEvent extends CancellableEvent {
   public static final PooledCancellableEventType<PacketReceiveNettyEvent> TYPE = new PooledCancellableEventType<PacketReceiveNettyEvent>("PacketReceiveNettyEvent", PacketReceiveNettyEvent::new, 1024);
   public class_2596<?> packet;

   private PacketReceiveNettyEvent() {
   }

   public static boolean dispatchSync(class_2596<?> packet) {
      if (!TYPE.hasPreListeners()) {
         return false;
      } else {
         PacketReceiveNettyEvent event = (PacketReceiveNettyEvent)TYPE.pool.acquire();
         event.packet = packet;
         event.resetCancellation();

         boolean var2;
         try {
            TYPE.dispatch(event);
            var2 = event.isCancelled();
         } finally {
            event.packet = null;
            TYPE.pool.release(event);
         }

         return var2;
      }
   }
}
