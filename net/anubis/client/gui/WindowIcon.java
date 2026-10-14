package net.anubis.client.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class WindowIcon {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final int[] SIZES = new int[]{16, 32, 48, 64, 128, 256};

   private WindowIcon() {
   }

   public static void apply(long window) {
      int platform = GLFW.glfwGetPlatform();
      if (platform == 393217 || platform == 393220) {
         List<ByteBuffer> pixels = new ArrayList(SIZES.length);

         try {
            MemoryStack stack;
            label150: {
               stack = MemoryStack.stackPush();

               try {
                  GLFWImage.Buffer images = GLFWImage.malloc(SIZES.length, stack);
                  int count = 0;

                  for(int size : SIZES) {
                     ByteBuffer image = load("/assets/anubis/icons/icon_" + size + ".png", stack);
                     if (image != null) {
                        pixels.add(image);
                        images.position(count++);
                        images.width(size);
                        images.height(size);
                        images.pixels(image);
                     }
                  }

                  if (count == 0) {
                     break label150;
                  }

                  images.position(0);
                  images.limit(count);
                  GLFW.glfwSetWindowIcon(window, images);
               } catch (Throwable var17) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (stack != null) {
                  stack.close();
               }

               return;
            }

            if (stack != null) {
               stack.close();
            }
         } finally {
            pixels.forEach(STBImage::stbi_image_free);
         }

      }
   }

   private static ByteBuffer load(String resource, MemoryStack stack) {
      try {
         InputStream in = WindowIcon.class.getResourceAsStream(resource);

         byte[] bytes;
         label100: {
            ByteBuffer var8;
            try {
               if (in == null) {
                  bytes = null;
                  break label100;
               }

               bytes = in.readAllBytes();
               ByteBuffer file = MemoryUtil.memAlloc(bytes.length);

               try {
                  file.put(bytes).flip();
                  IntBuffer w = stack.mallocInt(1);
                  IntBuffer h = stack.mallocInt(1);
                  IntBuffer channels = stack.mallocInt(1);
                  var8 = STBImage.stbi_load_from_memory(file, w, h, channels, 4);
               } finally {
                  MemoryUtil.memFree(file);
               }
            } catch (Throwable var16) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var14) {
                     var16.addSuppressed(var14);
                  }
               }

               throw var16;
            }

            if (in != null) {
               in.close();
            }

            return var8;
         }

         if (in != null) {
            in.close();
         }

         return bytes;
      } catch (RuntimeException | IOException e) {
         LOGGER.warn("[Anubis+] Failed to load window icon {}", resource, e);
         return null;
      }
   }
}
