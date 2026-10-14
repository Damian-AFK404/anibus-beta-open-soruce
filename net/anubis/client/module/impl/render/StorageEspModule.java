package net.anubis.client.module.impl.render;

import java.util.Objects;
import java.util.function.LongPredicate;
import net.anubis.client.esp.BlockLayer;
import net.anubis.client.esp.ChunkScanner;
import net.anubis.client.esp.EspStyle;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2248;
import net.minecraft.class_2281;
import net.minecraft.class_2331;
import net.minecraft.class_2336;
import net.minecraft.class_2363;
import net.minecraft.class_2377;
import net.minecraft.class_2480;
import net.minecraft.class_2496;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3708;

@Environment(EnvType.CLIENT)
public final class StorageEspModule extends ClientModule implements ChunkScanner.Consumer {
   private final EspStyle style = new EspStyle((x$0, x$1) -> this.addSetting(x$0, x$1), "Outline", 0.25F, 256.0F);
   private final BooleanSetting sTracers = (BooleanSetting)this.addSetting("tracers", new BooleanSetting("Tracers", false));
   private final Type chest = new Type("Chest", true, -6530304, "chests", "chest-color", "chest-tracers", "chest-tracer-color");
   private final Type enderChest = new Type("Ender Chest", true, -9109249, "ender-chests", "ender-chest-color", "ender-chest-tracers", "ender-chest-tracer-color");
   private final Type shulker = new Type("Shulker Box", true, -7995234, "shulker-boxes", "shulker-box-color", "shulker-tracers", "shulker-tracer-color");
   private final Type spawner = new Type("Spawner", true, -7700826, "spawners", "spawner-color", "spawner-tracers", "spawner-tracer-color");
   private final Type furnace = new Type("Furnace", false, -10197916, "furnaces", "furnace-color", "furnace-tracers", "furnace-tracer-color");
   private final Type barrel = new Type("Barrel", true, -10204366, "barrels", "barrel-color", "barrel-tracers", "barrel-tracer-color");
   private final Type enchanting = new Type("Enchanting Table", true, -3670016, "enchanting-tables", "enchanting-table-color", "enchanting-tracers", "enchanting-tracer-color");
   private final Type hopper = new Type("Hopper", false, -11513776, "hoppers", "hopper-color", "hopper-tracers", "hopper-tracer-color");
   private final Type[] types;
   private final BlockLayer layer;

   public StorageEspModule() {
      super("storage-esp", "Storage ESP", "Highlights storage blocks and spawners with per-type colours.", ModuleCategory.RENDER);
      this.types = new Type[]{this.chest, this.enderChest, this.shulker, this.spawner, this.furnace, this.barrel, this.enchanting, this.hopper};
      this.layer = new BlockLayer(new BlockLayer.Palette() {
         public int color(Object kind) {
            return ((Type)kind).color.argb();
         }

         public int tracerColor(Object kind) {
            Type t = (Type)kind;
            return t.tracers.get() ? t.tracerColor.argb() : 0;
         }
      }, () -> class_310.method_1551().field_1687 == null ? null : class_310.method_1551().field_1687.method_27983());
   }

   private Type typeOf(class_2248 block) {
      if (block instanceof class_2281) {
         return this.chest;
      } else if (block instanceof class_2336) {
         return this.enderChest;
      } else if (block instanceof class_2480) {
         return this.shulker;
      } else if (block instanceof class_2496) {
         return this.spawner;
      } else if (block instanceof class_2363) {
         return this.furnace;
      } else if (block instanceof class_3708) {
         return this.barrel;
      } else if (block instanceof class_2331) {
         return this.enchanting;
      } else {
         return block instanceof class_2377 ? this.hopper : null;
      }
   }

   private void rescan() {
      if (this.isEnabled()) {
         this.layer.clear();
         ChunkScanner.rescan();
      }
   }

   private boolean anyTracer() {
      if (!this.sTracers.get()) {
         return false;
      } else {
         for(Type type : this.types) {
            if (type.enabled.get() && type.tracers.get()) {
               return true;
            }
         }

         return false;
      }
   }

   protected void onEnable() {
      ChunkScanner.register(this);
   }

   protected void onDisable() {
      ChunkScanner.unregister(this);
      this.layer.clear();
   }

   public boolean wants(class_2680 state) {
      Type type = this.typeOf(state.method_26204());
      return type != null && type.enabled.get();
   }

   public void found(long pos, class_2680 state) {
      this.layer.put(pos, this.typeOf(state.method_26204()));
   }

   public void removed(long pos) {
      this.layer.remove(pos);
   }

   public void reset() {
      this.layer.clear();
   }

   public void chunkScanned(long chunkKey, LongPredicate stillThere) {
      this.layer.retainChunk(chunkKey, stillThere);
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      this.layer.render(event, this.style);
      if (this.anyTracer()) {
         this.layer.renderTracers(this.style);
      }

   }

   @Environment(EnvType.CLIENT)
   private final class Type {
      final BooleanSetting enabled;
      final BooleanSetting tracers;
      final ColorSetting color;
      final ColorSetting tracerColor;

      Type(String label, boolean on, int argb, String idOn, String idColor, String idTracers, String idTracerColor) {
         this.enabled = (BooleanSetting)StorageEspModule.this.addSetting(idOn, new BooleanSetting(label, on));
         this.color = (ColorSetting)StorageEspModule.this.addSetting(idColor, new ColorSetting(label + " Color", argb));
         this.tracers = (BooleanSetting)StorageEspModule.this.addSetting(idTracers, new BooleanSetting(label + " Tracers", false));
         this.tracerColor = (ColorSetting)StorageEspModule.this.addSetting(idTracerColor, new ColorSetting(label + " Tracer Color", argb));
         ColorSetting var10000 = this.color;
         BooleanSetting var10001 = this.enabled;
         Objects.requireNonNull(var10001);
         var10000.visibleWhen(var10001::get);
         this.tracers.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get());
         this.tracerColor.visibleWhen(() -> StorageEspModule.this.sTracers.get() && this.enabled.get() && this.tracers.get());
         this.enabled.onChange(StorageEspModule.this::rescan);
      }
   }
}
