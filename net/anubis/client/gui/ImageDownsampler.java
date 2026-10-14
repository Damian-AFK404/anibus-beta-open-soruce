package net.anubis.client.gui;

import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ImageDownsampler {
   private ImageDownsampler() {
   }

   public static int[] dimensions(int width, int height, int maxSide) {
      if (width > 0 && height > 0 && maxSide > 0) {
         double scale = Math.min((double)1.0F, (double)maxSide / (double)Math.max(width, height));
         return new int[]{Math.max(1, (int)Math.round((double)width * scale)), Math.max(1, (int)Math.round((double)height * scale))};
      } else {
         throw new IllegalArgumentException("Image dimensions");
      }
   }

   public static void rgba(ByteBuffer source, int width, int height, ByteBuffer target, int outWidth, int outHeight) {
      if (outWidth > 0 && outHeight > 0 && outWidth <= width && outHeight <= height) {
         double stepX = (double)width / (double)outWidth;
         double stepY = (double)height / (double)outHeight;

         for(int y = 0; y < outHeight; ++y) {
            double top = (double)y * stepY;
            double bottom = (double)(y + 1) * stepY;

            for(int x = 0; x < outWidth; ++x) {
               double left = (double)x * stepX;
               double right = (double)(x + 1) * stepX;
               double a = (double)0.0F;
               double r = (double)0.0F;
               double g = (double)0.0F;
               double b = (double)0.0F;

               for(int sy = (int)top; sy < Math.min(height, (int)Math.ceil(bottom)); ++sy) {
                  double wy = Math.min(bottom, (double)sy + (double)1.0F) - Math.max(top, (double)sy);

                  for(int sx = (int)left; sx < Math.min(width, (int)Math.ceil(right)); ++sx) {
                     double weight = wy * (Math.min(right, (double)sx + (double)1.0F) - Math.max(left, (double)sx));
                     int i = (sy * width + sx) * 4;
                     double alpha = (double)(source.get(i + 3) & 255) * weight;
                     a += alpha;
                     r += (double)(source.get(i) & 255) * alpha;
                     g += (double)(source.get(i + 1) & 255) * alpha;
                     b += (double)(source.get(i + 2) & 255) * alpha;
                  }
               }

               target.put((byte)((int)(a == (double)0.0F ? 0L : Math.round(r / a))));
               target.put((byte)((int)(a == (double)0.0F ? 0L : Math.round(g / a))));
               target.put((byte)((int)(a == (double)0.0F ? 0L : Math.round(b / a))));
               target.put((byte)((int)Math.round(a / (stepX * stepY))));
            }
         }

         target.flip();
      } else {
         throw new IllegalArgumentException("Only image reduction is supported");
      }
   }
}
