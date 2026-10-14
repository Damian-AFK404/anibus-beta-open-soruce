package net.anubis.client.module.impl.render.character;

import net.anubis.client.module.impl.render.character.kit.Lights;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_308;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_2874.class_12325;
import net.minecraft.class_308.class_11274;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class CharacterLighting {
   private static final Vector3f LIGHT_0 = (new Vector3f(0.2F, 1.0F, -0.7F)).normalize();
   private static final Vector3f LIGHT_1 = (new Vector3f(-0.2F, 1.0F, 0.7F)).normalize();
   private static final Lights NETHER = new Lights(0.2F, 1.0F, -0.7F, -0.2F, -1.0F, 0.7F);
   private static final Lights INVENTORY = new Lights(0.2F, -1.0F, 1.0F, -0.2F, -1.0F, 0.0F);
   private static final Lights ITEMS_FLAT = transformed((new Matrix4f()).rotationY((-(float)Math.PI / 8F)).rotateX(2.3561945F));
   private static final Lights ITEMS_3D = transformed((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F).rotateYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((-(float)Math.PI / 8F), 2.3561945F, 0.0F));
   private static class_308.class_11274 entry;

   private CharacterLighting() {
   }

   public static void setup(class_308.class_11274 current) {
      entry = current;
   }

   static Lights current() {
      Lights var10000;
      switch (entry) {
         case field_60025:
            var10000 = nether() ? NETHER : Lights.WORLD;
            break;
         case field_60026:
            var10000 = ITEMS_FLAT;
            break;
         case field_60027:
            var10000 = ITEMS_3D;
            break;
         case field_60028:
         case field_60029:
            var10000 = INVENTORY;
            break;
         default:
            throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static boolean nether() {
      class_638 level = class_310.method_1551().field_1687;
      return level != null && level.method_8597().comp_5221() == class_12325.field_64381;
   }

   private static Lights transformed(Matrix4f matrix) {
      Vector3f a = matrix.transformDirection(LIGHT_0, new Vector3f());
      Vector3f b = matrix.transformDirection(LIGHT_1, new Vector3f());
      return new Lights(a.x, a.y, a.z, b.x, b.y, b.z);
   }

   static {
      entry = class_11274.field_60025;
   }
}
