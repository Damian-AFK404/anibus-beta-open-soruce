package net.anubis.client.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
final class HudEmblem {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final String SOURCE = "/assets/anubis/textures/gui/hud_emblem.png";
   private static final float INK_GAMMA = 0.65F;
   private static final float INK_GAIN = 1.35F;
   private static float[] ink;
   private static int inkW;
   private static int inkH;
   private static boolean missing;
   private static ThemeImages.Texture sized;
   private static int sizedHeight = -1;

   private HudEmblem() {
   }

   static ThemeImages.Texture forHeight(int height) {
      if (height >= 4 && !missing) {
         if (height == sizedHeight && sized != null) {
            return sized;
         } else if (ink == null && !load()) {
            missing = true;
            return null;
         } else {
            int width = Math.max(1, Math.round((float)inkW * (float)height / (float)inkH));
            ByteBuffer rgba = MemoryUtil.memAlloc(width * height * 4);

            ThemeImages.Texture var9;
            try {
               for(int y = 0; y < height; ++y) {
                  for(int x = 0; x < width; ++x) {
                     float a = coverage(x, y, width, height);
                     a = Math.min(1.0F, (float)Math.pow((double)a, (double)0.65F) * 1.35F);
                     rgba.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)Math.round(a * 255.0F));
                  }
               }

               rgba.flip();
               if (sized != null) {
                  GL11.glDeleteTextures(sized.id());
               }

               sized = ThemeImages.fromPixels(rgba, width, height, false);
               sizedHeight = height;
               var9 = sized;
            } finally {
               MemoryUtil.memFree(rgba);
            }

            return var9;
         }
      } else {
         return null;
      }
   }

   private static float coverage(int x, int y, int width, int height) {
      float x0 = (float)x * (float)inkW / (float)width;
      float x1 = (float)(x + 1) * (float)inkW / (float)width;
      float y0 = (float)y * (float)inkH / (float)height;
      float y1 = (float)(y + 1) * (float)inkH / (float)height;
      float sum = 0.0F;
      float area = 0.0F;

      for(int sy = (int)y0; sy < Math.min(inkH, (int)Math.ceil((double)y1)); ++sy) {
         float wy = Math.min(y1, (float)(sy + 1)) - Math.max(y0, (float)sy);

         for(int sx = (int)x0; sx < Math.min(inkW, (int)Math.ceil((double)x1)); ++sx) {
            float wx = Math.min(x1, (float)(sx + 1)) - Math.max(x0, (float)sx);
            float weight = wx * wy;
            sum += ink[sy * inkW + sx] * weight;
            area += weight;
         }
      }

      return area > 0.0F ? sum / area : 0.0F;
   }

   private static boolean load() {
      byte[] bytes;
      try {
         InputStream in = HudEmblem.class.getResourceAsStream("/assets/anubis/textures/gui/hud_emblem.png");

         label223: {
            boolean var2;
            try {
               if (in != null) {
                  bytes = in.readAllBytes();
                  break label223;
               }

               LOGGER.warn("[Anubis] Missing texture {}", "/assets/anubis/textures/gui/hud_emblem.png");
               var2 = false;
            } catch (Throwable var19) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }
               }

               throw var19;
            }

            if (in != null) {
               in.close();
            }

            return var2;
         }

         if (in != null) {
            in.close();
         }
      } catch (IOException e) {
         LOGGER.warn("[Anubis] Failed to read {}", "/assets/anubis/textures/gui/hud_emblem.png", e);
         return false;
      }

      ByteBuffer file = MemoryUtil.memAlloc(bytes.length);
      ByteBuffer pixels = null;

      boolean var24;
      try {
         MemoryStack stack;
         label236: {
            stack = MemoryStack.stackPush();

            try {
               file.put(bytes).flip();
               IntBuffer w = stack.mallocInt(1);
               IntBuffer h = stack.mallocInt(1);
               IntBuffer channels = stack.mallocInt(1);
               pixels = STBImage.stbi_load_from_memory(file, w, h, channels, 4);
               if (pixels == null) {
                  LOGGER.warn("[Anubis] Can't decode {}: {}", "/assets/anubis/textures/gui/hud_emblem.png", STBImage.stbi_failure_reason());
                  var24 = false;
                  break label236;
               }

               inkW = w.get(0);
               inkH = h.get(0);
               ink = new float[inkW * inkH];

               for(int i = 0; i < ink.length; ++i) {
                  ink[i] = (float)(pixels.get(i * 4 + 3) & 255) / 255.0F;
               }

               var24 = true;
            } catch (Throwable var17) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var15) {
                     var17.addSuppressed(var15);
                  }
               }

               throw var17;
            }

            if (stack != null) {
               stack.close();
            }

            return var24;
         }

         if (stack != null) {
            stack.close();
         }
      } finally {
         if (pixels != null) {
            STBImage.stbi_image_free(pixels);
         }

         MemoryUtil.memFree(file);
      }

      return var24;
   }
}
