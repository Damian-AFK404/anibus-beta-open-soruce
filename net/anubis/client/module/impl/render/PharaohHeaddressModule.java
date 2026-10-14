package net.anubis.client.module.impl.render;

import java.util.Arrays;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_9848;

@Environment(EnvType.CLIENT)
public final class PharaohHeaddressModule extends HeadCosmeticModule {
   private static final String STYLE_NEMES = "Nemes";
   private static final String STYLE_KHEPRESH = "Khepresh";
   private static final String STYLE_SUN_DISC = "Sun Disc";
   private final ModeSetting style = (ModeSetting)this.addSetting("style", new ModeSetting("Style", "Nemes", new String[]{"Nemes", "Khepresh", "Sun Disc"}));
   private final NumberSetting stripes = (NumberSetting)this.addSetting("stripes", (new NumberSetting("Stripes", 5.0F, 3.0F, 9.0F)).step(1.0F));
   private final BooleanSetting collar = (BooleanSetting)this.addSetting("collar", new BooleanSetting("Broad Collar", true));
   private final NumberSetting size = (NumberSetting)this.addSetting("size", (new NumberSetting("Size", 1.0F, 0.8F, 1.3F)).step(0.05F));
   private final ModeSetting color = (ModeSetting)this.addSetting("color", new ModeSetting("Color", "Theme", new String[]{"Theme", "Manual"}));
   private final ColorSetting gold = (ColorSetting)this.addSetting("manual-gold", new ColorSetting("Gold Color", -7688204));
   private final ColorSetting inlay = (ColorSetting)this.addSetting("manual-inlay", new ColorSetting("Inlay Color", -13738055));
   private final ColorSetting gem = (ColorSetting)this.addSetting("manual-gem", new ColorSetting("Gem Color", -10313473));
   private final BooleanSetting gemGlow = (BooleanSetting)this.addSetting("gem-glow", new BooleanSetting("Gem Glow", true));
   private final BooleanSetting glint = (BooleanSetting)this.addSetting("glint", new BooleanSetting("Glint", true));
   private final BooleanSetting friends = (BooleanSetting)this.addSetting("friends", new BooleanSetting("On Friends", false));
   private final PharaohMesh[] meshes = new PharaohMesh[4];
   private long styleRevision = -1L;
   private long stripesRevision = -1L;
   private long collarRevision = -1L;

   public PharaohHeaddressModule() {
      super("pharaoh-headdress", "Pharaoh Headdress", "Puts a pharaoh headdress on your player model.");
      this.stripes.visibleWhen(() -> this.style.is("Nemes"));
      this.gold.visibleWhen(() -> this.color.is("Manual"));
      this.inlay.visibleWhen(() -> this.color.is("Manual"));
      this.gem.visibleWhen(() -> this.color.is("Manual"));
   }

   void reset() {
      super.reset();
      Arrays.fill(this.meshes, (Object)null);
      this.styleRevision = -1L;
   }

   PharaohMesh headMesh(boolean helmet) {
      this.refreshMeshes();
      int slot = helmet ? 1 : 0;
      PharaohMesh mesh = this.meshes[slot];
      if (mesh == null) {
         mesh = PharaohMesh.head(this.styleIndex(), this.stripes.getInt(), helmet);
         this.meshes[slot] = mesh;
      }

      return mesh;
   }

   PharaohMesh bodyMesh(boolean chest) {
      this.refreshMeshes();
      int slot = chest ? 3 : 2;
      PharaohMesh mesh = this.meshes[slot];
      if (mesh == null) {
         mesh = PharaohMesh.body(this.styleIndex(), this.stripes.getInt(), chest, this.collar.get());
         this.meshes[slot] = mesh;
      }

      return mesh;
   }

   private void refreshMeshes() {
      long s = this.style.revision();
      long n = this.stripes.revision();
      long c = this.collar.revision();
      if (s != this.styleRevision || n != this.stripesRevision || c != this.collarRevision) {
         this.styleRevision = s;
         this.stripesRevision = n;
         this.collarRevision = c;
         Arrays.fill(this.meshes, (Object)null);
      }
   }

   private int styleIndex() {
      if (this.style.is("Khepresh")) {
         return 1;
      } else {
         return this.style.is("Sun Disc") ? 2 : 0;
      }
   }

   float scale() {
      return this.size.get();
   }

   int[] palette() {
      int[] palette = new int[5];
      if (this.color.is("Manual")) {
         int metal = this.gold.argb() | -16777216;
         palette[0] = metal;
         palette[1] = this.inlay.argb() | -16777216;
         palette[2] = this.gem.argb() | -16777216;
         palette[3] = bronze(metal);
      } else {
         int accent = CosmeticAccent.argb();
         int metal = metal(accent);
         palette[0] = metal;
         palette[1] = class_9848.method_61321(accent, 0.78F);
         palette[2] = gem(accent);
         palette[3] = class_9848.method_61321(metal, 0.62F);
      }

      return palette;
   }

   boolean glows() {
      return this.gemGlow.get();
   }

   boolean glints() {
      return this.glint.get();
   }

   boolean onFriends() {
      return this.friends.get();
   }

   private static int bronze(int argb) {
      int r = (int)((float)(argb >> 16 & 255) * 0.7F);
      int g = (int)((float)(argb >> 8 & 255) * 0.55F);
      int b = (int)((float)(argb & 255) * 0.4F);
      return -16777216 | r << 16 | g << 8 | b;
   }
}
