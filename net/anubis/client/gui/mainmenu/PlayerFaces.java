package net.anubis.client.gui.mainmenu;

import com.mojang.blaze3d.textures.GpuTexture;
import imgui.ImDrawList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.anubis.client.account.Account;
import net.anubis.client.account.AccountType;
import net.anubis.client.gui.EgyptDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1068;
import net.minecraft.class_10868;
import net.minecraft.class_11786;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_8685;
import net.minecraft.class_9296;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
final class PlayerFaces {
   private static final float FACE_U = 0.125F;
   private static final float HAT_U = 0.625F;
   private static final float TOP_V = 0.125F;
   private static final float PART = 0.125F;
   private static final Map<UUID, Supplier<class_8685>> SKINS = new HashMap();
   private static final Set<GpuTexture> NEAREST = Collections.newSetFromMap(new WeakHashMap());

   private PlayerFaces() {
   }

   static void draw(ImDrawList dl, Account account, float x, float y, float size, float s, float a) {
      UUID id;
      try {
         id = UUID.fromString(account.uuid());
      } catch (IllegalArgumentException var9) {
         placeholder(dl, x, y, size, s, a);
         return;
      }

      draw(dl, id, account.type() == AccountType.CRACKED, x, y, size, s, a);
   }

   static void draw(ImDrawList dl, UUID id, boolean offline, float x, float y, float size, float s, float a) {
      int texture = texture(skin(id, offline).comp_1626().comp_3627());
      if (texture == 0) {
         placeholder(dl, x, y, size, s, a);
      } else {
         float r = Math.max(1.0F, (float)Math.round(3.0F * s));
         int tint = EgyptDraw.col(-1, a);
         dl.addImageRounded((long)texture, x, y, x + size, y + size, 0.125F, 0.125F, 0.25F, 0.25F, tint, r, 240);
         dl.addImageRounded((long)texture, x, y, x + size, y + size, 0.625F, 0.125F, 0.75F, 0.25F, tint, r, 240);
      }
   }

   private static void placeholder(ImDrawList dl, float x, float y, float size, float s, float a) {
      dl.addRectFilled(x, y, x + size, y + size, EgyptDraw.col(-7372954, 0.3F * a), Math.round(3.0F * s));
   }

   private static class_8685 skin(UUID id, boolean offline) {
      return offline ? class_1068.method_4648(id) : (class_8685)((Supplier)SKINS.computeIfAbsent(id, (key) -> {
         Supplier<class_11786.class_11787> lookup = class_310.method_1551().method_73362().method_74050(class_9296.method_73312(key));
         return () -> ((class_11786.class_11787)lookup.get()).method_73503();
      })).get();
   }

   private static int texture(class_2960 path) {
      GpuTexture texture;
      try {
         texture = class_310.method_1551().method_1531().method_4619(path).method_68004();
      } catch (IllegalStateException var4) {
         return 0;
      }

      if (texture instanceof class_10868 gl) {
         if (!gl.isClosed()) {
            if (NEAREST.add(texture)) {
               int bound = GL11.glGetInteger(32873);
               GL11.glBindTexture(3553, gl.method_68427());
               GL11.glTexParameteri(3553, 10241, 9728);
               GL11.glTexParameteri(3553, 10240, 9728);
               GL11.glBindTexture(3553, bound);
            }

            return gl.method_68427();
         }
      }

      return 0;
   }
}
