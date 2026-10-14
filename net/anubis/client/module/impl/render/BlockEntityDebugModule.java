package net.anubis.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.anubis.client.esp.BlockBoxCache;
import net.anubis.client.esp.BlockLayer;
import net.anubis.client.esp.EspStyle;
import net.anubis.client.esp.HorizontalCandidates;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.Render3DEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.BlockColorListSetting;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.util.BlockEntityLeakTracker;
import net.anubis.client.util.MobilePerformance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_12179;
import net.minecraft.class_12180;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_2596;
import net.minecraft.class_2626;
import net.minecraft.class_2680;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public final class BlockEntityDebugModule extends ClientModule {
   private static final int SETTINGS_VERSION = 1;
   private static final float FILL_ALPHA = 0.8F;
   private static final Set<String> NEW_IN_TABLE = Set.of("minecraft:jukebox", "minecraft:piston", "minecraft:sticky_piston");
   private final ModeSetting sDetection = (ModeSetting)this.addSetting("detection", new ModeSetting("Detection", "All", new String[]{"All", "Below Zero", "Below -20", "Below -30"}));
   private final BooleanSetting sBlockFilter = (BooleanSetting)this.addSetting("block-filter", new BooleanSetting("Block Filter", false));
   private final EspStyle style = new EspStyle((x$0, x$1) -> this.addSetting(x$0, x$1), "Boxes", 0.8F, 256.0F);
   private final BooleanSetting sTracers = (BooleanSetting)this.addSetting("tracers", new BooleanSetting("Tracers", true));
   private final BooleanSetting sCustomTracerColor = (BooleanSetting)this.addSetting("custom-tracer-color", new BooleanSetting("Custom Tracer Color", false));
   private final ColorSetting sTracerColor = (ColorSetting)this.addSetting("tracer-color", new ColorSetting("Tracer Color", -1));
   private final BlockColorListSetting sBlockColors = (BlockColorListSetting)this.addSetting(BlockColorListSetting.createForBlockEntityDebug());
   private final BlockLayer layer = new BlockLayer(new BlockLayer.Palette() {
      public int color(Object kind) {
         return BlockEntityDebugModule.this.sBlockColors.colorArgb((class_2248)kind);
      }

      public int tracerColor(Object kind) {
         return BlockEntityDebugModule.this.tracerColorOf((class_2248)kind);
      }
   }, () -> class_310.method_1551().field_1687 == null ? null : class_310.method_1551().field_1687.method_27983());
   private final LongOpenHashSet spawners = new LongOpenHashSet();
   private List<SpawnerBeam> mobileBeams;
   private HorizontalCandidates<SpawnerBeam> beamCandidates;
   private BlockBoxCache beamBoxes;
   private int seenGeneration = -1;
   private Set<String> scannedIds = Set.of();

   public BlockEntityDebugModule() {
      super("block-entity-debug", "Block Entity Debug", "Renders every leaked block entity as ESP (always-on Netty capture).", ModuleCategory.BASE_RAID);
      BooleanSetting var10000 = this.sCustomTracerColor;
      BooleanSetting var10001 = this.sTracers;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
      this.sDetection.onChange(this::resync);
      this.sBlockFilter.onChange(this::resync);
      this.sBlockColors.onChange(this::onBlockColorsChanged);
   }

   public int settingsVersion() {
      return 1;
   }

   public void migrateSettings(int savedVersion) {
      this.style.shape.set("Boxes");
      this.style.boxSize.set(1.0F);
      this.style.fillAlpha.set(0.8F);
      this.sBlockColors.resetColors();
      int plain = BlockColorListSetting.rgbaToArgb(this.sBlockColors.defaultColor());
      boolean filtered = this.sBlockFilter.get();

      for(BlockColorListSetting.BlockEntry entry : this.sBlockColors.entries()) {
         if (BlockColorListSetting.rgbaToArgb(entry.color()) != plain && (!filtered || NEW_IN_TABLE.contains(entry.id()))) {
            this.sBlockColors.select(entry);
         }
      }

   }

   private int tracerColorOf(class_2248 block) {
      int own = this.sBlockColors.isEnabled(block) ? this.sBlockColors.tracerColorArgb(block) : this.sBlockColors.colorArgb(block);
      if (own == 0) {
         return 0;
      } else {
         return this.sCustomTracerColor.get() ? this.sTracerColor.argb() : own;
      }
   }

   private void onBlockColorsChanged() {
      if (this.isEnabled() && this.sBlockFilter.get() && !this.sBlockColors.enabledIds().equals(this.scannedIds)) {
         this.resync();
      }

   }

   protected void onEnable() {
      BlockEntityLeakTracker.recordAdditions(true);
      this.resync();
   }

   protected void onDisable() {
      BlockEntityLeakTracker.recordAdditions(false);
      this.layer.clear();
      this.spawners.clear();
      this.mobileBeams = null;
      this.beamCandidates = null;
      this.beamBoxes = null;
   }

   private void resync() {
      if (this.isEnabled()) {
         this.layer.clear();
         this.spawners.clear();
         this.mobileBeams = null;
         this.beamCandidates = null;
         this.beamBoxes = null;
         this.scannedIds = this.sBlockColors.enabledIds();
         this.seenGeneration = BlockEntityLeakTracker.generation();

         while(BlockEntityLeakTracker.pollAdded() != null) {
         }

         for(Map.Entry<class_2338, BlockEntityLeakTracker.Entry> entry : BlockEntityLeakTracker.view().entrySet()) {
            this.accept((class_2338)entry.getKey(), ((BlockEntityLeakTracker.Entry)entry.getValue()).block());
         }

      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (BlockEntityLeakTracker.generation() != this.seenGeneration) {
         this.resync();
      } else {
         class_2338 pos;
         while((pos = BlockEntityLeakTracker.pollAdded()) != null) {
            BlockEntityLeakTracker.Entry entry = (BlockEntityLeakTracker.Entry)BlockEntityLeakTracker.view().get(pos);
            if (entry != null) {
               this.accept(pos, entry.block());
            }
         }

      }
   }

   private void accept(class_2338 pos, class_2248 block) {
      if (this.passesDetection(pos)) {
         if (!this.sBlockFilter.get() || block == class_2246.field_10124 || this.sBlockColors.isEnabled(block)) {
            this.layer.put(pos.method_10063(), block);
            if (block == class_2246.field_10260 && this.spawners.add(pos.method_10063())) {
               this.mobileBeams = null;
            }

         }
      }
   }

   private boolean passesDetection(class_2338 pos) {
      boolean var10000;
      switch (this.sDetection.get()) {
         case "Below Zero" -> var10000 = pos.method_10264() < 0;
         case "Below -20" -> var10000 = pos.method_10264() < -20;
         case "Below -30" -> var10000 = pos.method_10264() < -30;
         default -> var10000 = true;
      }

      return var10000;
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
      class_2596 var3 = event.packet;
      if (var3 instanceof class_2626 packet) {
         if (BlockEntityLeakTracker.contains(packet.method_11309())) {
            class_2680 state = packet.method_11308();
            if (state.method_26215() || state.method_27852(class_2246.field_10340) || state.method_27852(class_2246.field_28888)) {
               event.setCancelled(true);
            }

         }
      }
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      this.layer.render(event, this.style);
      if (this.sTracers.get()) {
         this.layer.renderTracers(this.style);
      }

      if (!this.spawners.isEmpty()) {
         class_310 mc = class_310.method_1551();
         if (mc.field_1724 != null) {
            class_243 eye = mc.field_1724.method_5836(event.partialTick);
            double rangeSq = (double)this.style.range.get() * (double)this.style.range.get();
            int color = this.sBlockColors.colorArgb(class_2246.field_10260);
            class_12179 beam = class_12179.method_75536(color, this.style.lineWidth.get());
            if (MobilePerformance.active()) {
               this.renderMobileBeams(eye, rangeSq, beam);
            } else {
               this.mobileBeams = null;
               this.beamCandidates = null;
               this.beamBoxes = null;
               LongIterator var8 = this.spawners.iterator();

               while(var8.hasNext()) {
                  long packed = (Long)var8.next();
                  class_2338 pos = class_2338.method_10092(packed);
                  if (!(eye.method_1028((double)pos.method_10263() + (double)0.5F, (double)pos.method_10264() + (double)1.0F, (double)pos.method_10260() + (double)0.5F) > rangeSq)) {
                     class_12180.method_75541(new class_238((double)pos.method_10263() + 0.3, (double)pos.method_10264() + (double)1.0F, (double)pos.method_10260() + 0.3, (double)pos.method_10263() + 0.7, (double)pos.method_10264() + (double)257.0F, (double)pos.method_10260() + 0.7), beam).method_75533();
                  }
               }

            }
         }
      }
   }

   private void renderMobileBeams(class_243 eye, double rangeSq, class_12179 style) {
      if (this.mobileBeams == null) {
         this.mobileBeams = new ArrayList(this.spawners.size());
         LongIterator var5 = this.spawners.iterator();

         while(var5.hasNext()) {
            long packed = (Long)var5.next();
            this.mobileBeams.add(new SpawnerBeam(packed));
         }
      }

      if (this.beamCandidates == null) {
         this.beamCandidates = new HorizontalCandidates<SpawnerBeam>((beamx) -> (double)class_2338.method_10061(beamx.position()) + (double)0.5F, (beamx) -> (double)class_2338.method_10083(beamx.position()) + (double)0.5F);
      }

      if (this.beamBoxes == null) {
         this.beamBoxes = new BlockBoxCache(4096, BlockEntityDebugModule::beamBounds);
      }

      this.beamBoxes.beginFrame(1.0F);

      for(SpawnerBeam beam : this.beamCandidates.select(this.mobileBeams, eye.field_1352, eye.field_1350, Math.sqrt(rangeSq))) {
         long packed = beam.position();
         if (!(eye.method_1028((double)class_2338.method_10061(packed) + (double)0.5F, (double)class_2338.method_10071(packed) + (double)1.0F, (double)class_2338.method_10083(packed) + (double)0.5F) > rangeSq)) {
            class_12180.method_75541(this.beamBoxes.box(packed), style).method_75533();
         }
      }

   }

   private static class_238 beamBounds(long packed) {
      int x = class_2338.method_10061(packed);
      int y = class_2338.method_10071(packed);
      int z = class_2338.method_10083(packed);
      return new class_238((double)x + 0.3, (double)y + (double)1.0F, (double)z + 0.3, (double)x + 0.7, (double)y + (double)257.0F, (double)z + 0.7);
   }

   @Environment(EnvType.CLIENT)
   private static record SpawnerBeam(long position) {
   }
}
