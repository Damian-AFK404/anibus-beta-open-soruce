package net.anubis.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.function.LongFunction;
import net.anubis.client.util.BlockBoxHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_238;

@Environment(EnvType.CLIENT)
public final class BlockBoxCache {
   private final int capacity;
   private final LongFunction<class_238> factory;
   private final Long2ObjectLinkedOpenHashMap<Entry> boxes;
   private float size;
   private long frame;

   BlockBoxCache(int capacity) {
      this(capacity, (LongFunction)null);
   }

   public BlockBoxCache(int capacity, LongFunction<class_238> factory) {
      this.boxes = new Long2ObjectLinkedOpenHashMap();
      this.size = Float.NaN;
      this.capacity = Math.max(1, capacity);
      this.factory = factory;
   }

   public void beginFrame(float size) {
      if (Float.floatToIntBits(this.size) != Float.floatToIntBits(size)) {
         this.boxes.clear();
         this.size = size;
      }

      ++this.frame;
   }

   public class_238 box(long packed) {
      Entry hit = (Entry)this.boxes.getAndMoveToLast(packed);
      if (hit != null) {
         hit.frame = this.frame;
         return hit.box;
      } else {
         class_238 made = this.factory == null ? BlockBoxHelper.scaledBlockBox(class_2338.method_10092(packed), this.size) : (class_238)this.factory.apply(packed);
         if (this.boxes.size() == this.capacity) {
            if (((Entry)this.boxes.get(this.boxes.firstLongKey())).frame == this.frame) {
               return made;
            }

            this.boxes.removeFirst();
         }

         this.boxes.put(packed, new Entry(made, this.frame));
         return made;
      }
   }

   int size() {
      return this.boxes.size();
   }

   @Environment(EnvType.CLIENT)
   private static final class Entry {
      final class_238 box;
      long frame;

      Entry(class_238 box, long frame) {
         this.box = box;
         this.frame = frame;
      }
   }
}
