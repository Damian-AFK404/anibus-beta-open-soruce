package net.anubis.client.gui;

import java.io.IOException;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class GifDecoder {
   private static final int MAX_CANVAS = 4096;
   private static final int IMAGE = 44;
   private static final int EXTENSION = 33;
   private static final int GRAPHIC_CONTROL = 249;
   private static final int DISPOSE_BACKGROUND = 2;
   private static final int DISPOSE_PREVIOUS = 3;
   private static final int MAX_CODES = 4096;
   private final byte[] data;
   private final int width;
   private final int height;
   private final int[] globalPalette;
   private final int blocksStart;
   private final int frameCount;
   private int pos;

   private GifDecoder(byte[] data) throws IOException {
      this.data = data;
      this.pos = 6;
      this.width = this.u16();
      this.height = this.u16();
      int flags = this.u8();
      this.pos += 2;
      this.globalPalette = (flags & 128) != 0 ? this.palette(flags) : null;
      this.blocksStart = this.pos;
      if (this.width != 0 && this.height != 0) {
         if (this.width <= 4096 && this.height <= 4096) {
            this.frameCount = this.countFrames();
            if (this.frameCount == 0) {
               throw new IOException("GIF has no frames");
            }
         } else {
            throw new IOException("GIF is " + this.width + "x" + this.height + ", larger than 4096 px");
         }
      } else {
         throw new IOException("GIF has an empty canvas");
      }
   }

   static GifDecoder open(byte[] data) throws IOException {
      if (data.length >= 13 && data[0] == 71 && data[1] == 73 && data[2] == 70) {
         return new GifDecoder(data);
      } else {
         throw new IOException("Not a GIF file");
      }
   }

   int width() {
      return this.width;
   }

   int height() {
      return this.height;
   }

   int frameCount() {
      return this.frameCount;
   }

   void decode(int outW, int outH, int maxFrames, Sink sink) {
      int[] canvas = new int[this.width * this.height];
      int[] saved = null;
      int dispose = 0;
      int delay = 0;
      int transparent = -1;
      int lastDispose = 0;
      int lastX = 0;
      int lastY = 0;
      int lastW = 0;
      int lastH = 0;
      int emitted = 0;
      this.pos = this.blocksStart;

      while(true) {
         if (emitted < maxFrames && this.pos < this.data.length) {
            int block = this.u8();
            if (block == 33) {
               if (this.u8() == 249) {
                  int size = this.u8();
                  int end = this.pos + size;
                  int flags = this.u8();
                  dispose = flags >> 2 & 7;
                  delay = this.u16();
                  int index = this.u8();
                  transparent = (flags & 1) != 0 ? index : -1;
                  this.pos = end;
               }

               this.skipSubBlocks();
               continue;
            }

            if (block == 44) {
               int x = this.u16();
               int y = this.u16();
               int w = this.u16();
               int h = this.u16();
               int flags = this.u8();
               int[] palette = (flags & 128) != 0 ? this.palette(flags) : this.globalPalette;
               boolean interlaced = (flags & 64) != 0;
               int minCodeSize = this.u8();
               byte[] compressed = this.readSubBlocks();
               if ((long)w * (long)h > (long)this.width * (long)this.height) {
                  return;
               }

               if (lastDispose == 2) {
                  this.clear(canvas, lastX, lastY, lastW, lastH);
               } else if (lastDispose == 3 && saved != null) {
                  System.arraycopy(saved, 0, canvas, 0, canvas.length);
               }

               if (dispose == 3) {
                  if (saved == null) {
                     saved = new int[canvas.length];
                  }

                  System.arraycopy(canvas, 0, saved, 0, canvas.length);
               }

               byte[] indices = new byte[w * h];
               int count = lzw(compressed, minCodeSize, indices);
               if (palette != null) {
                  this.draw(canvas, indices, count, palette, transparent, x, y, w, h, interlaced);
               }

               int[] pixels = outW == this.width && outH == this.height ? (int[])(([I)canvas).clone() : downscale(canvas, this.width, this.height, outW, outH);
               if (!sink.accept(new Frame(pixels, outW, outH, delayMs(delay)))) {
                  return;
               }

               ++emitted;
               lastDispose = dispose;
               lastX = x;
               lastY = y;
               lastW = w;
               lastH = h;
               dispose = 0;
               delay = 0;
               transparent = -1;
               continue;
            }

            return;
         }

         return;
      }
   }

   private static int delayMs(int centiseconds) {
      return centiseconds <= 1 ? 100 : centiseconds * 10;
   }

   private static int lzw(byte[] in, int minCodeSize, byte[] out) {
      if (minCodeSize >= 1 && minCodeSize <= 8) {
         int clear = 1 << minCodeSize;
         int end = clear + 1;
         short[] prefix = new short[4096];
         byte[] suffix = new byte[4096];
         byte[] stack = new byte[4097];

         for(int i = 0; i < clear; ++i) {
            suffix[i] = (byte)i;
         }

         int codeSize = minCodeSize + 1;
         int next = clear + 2;
         int previous = -1;
         byte first = 0;
         int bits = 0;
         int buffer = 0;
         int read = 0;
         int written = 0;

         while(written < out.length) {
            while(bits < codeSize) {
               if (read >= in.length) {
                  return written;
               }

               buffer |= (in[read++] & 255) << bits;
               bits += 8;
            }

            int code = buffer & (1 << codeSize) - 1;
            buffer >>>= codeSize;
            bits -= codeSize;
            if (code == clear) {
               codeSize = minCodeSize + 1;
               next = clear + 2;
               previous = -1;
            } else {
               if (code == end) {
                  break;
               }

               if (previous == -1) {
                  if (code >= clear) {
                     break;
                  }

                  first = suffix[code];
                  out[written++] = first;
                  previous = code;
               } else {
                  int current = code;
                  int top = 0;
                  if (code >= next) {
                     if (code > next) {
                        break;
                     }

                     stack[top++] = first;
                     code = previous;
                  }

                  while(code >= clear) {
                     stack[top++] = suffix[code];
                     code = prefix[code];
                  }

                  first = suffix[code];
                  stack[top++] = first;
                  if (next < 4096) {
                     prefix[next] = (short)previous;
                     suffix[next] = first;
                     ++next;
                     if (next == 1 << codeSize && codeSize < 12) {
                        ++codeSize;
                     }
                  }

                  int var23;
                  for(previous = current; top > 0 && written < out.length; out[var23] = stack[top]) {
                     var23 = written++;
                     --top;
                  }
               }
            }
         }

         return written;
      } else {
         return 0;
      }
   }

   static int[] downscale(int[] src, int srcW, int srcH, int outW, int outH) {
      int[] out = new int[outW * outH];

      for(int oy = 0; oy < outH; ++oy) {
         int y0 = (int)((long)oy * (long)srcH / (long)outH);
         int y1 = Math.max(y0 + 1, (int)((long)(oy + 1) * (long)srcH / (long)outH));

         for(int ox = 0; ox < outW; ++ox) {
            int x0 = (int)((long)ox * (long)srcW / (long)outW);
            int x1 = Math.max(x0 + 1, (int)((long)(ox + 1) * (long)srcW / (long)outW));
            long a = 0L;
            long r = 0L;
            long g = 0L;
            long b = 0L;

            for(int sy = y0; sy < y1; ++sy) {
               int row = sy * srcW;

               for(int sx = x0; sx < x1; ++sx) {
                  int p = src[row + sx];
                  int alpha = p >>> 24;
                  a += (long)alpha;
                  r += (long)(p >> 16 & 255) * (long)alpha;
                  g += (long)(p >> 8 & 255) * (long)alpha;
                  b += (long)(p & 255) * (long)alpha;
               }
            }

            if (a != 0L) {
               int n = (y1 - y0) * (x1 - x0);
               out[oy * outW + ox] = (int)(a / (long)n) << 24 | (int)(r / a) << 16 | (int)(g / a) << 8 | (int)(b / a);
            }
         }
      }

      return out;
   }

   private void draw(int[] canvas, byte[] indices, int count, int[] palette, int transparent, int fx, int fy, int fw, int fh, boolean interlaced) {
      int[] rows = rowOrder(fh, interlaced);
      int i = 0;

      for(int r = 0; r < fh && i < count; ++r) {
         int end = Math.min(count, i + fw);
         int cy = fy + rows[r];
         if (cy >= this.height) {
            i = end;
         } else {
            int base = cy * this.width;

            for(int cx = fx; i < end; ++cx) {
               int index = indices[i] & 255;
               if (index != transparent && index < palette.length && cx < this.width) {
                  canvas[base + cx] = palette[index];
               }

               ++i;
            }
         }
      }

   }

   private static int[] rowOrder(int height, boolean interlaced) {
      int[] rows = new int[height];
      if (!interlaced) {
         for(int i = 0; i < height; rows[i] = i++) {
         }

         return rows;
      } else {
         int[] starts = new int[]{0, 4, 2, 1};
         int[] steps = new int[]{8, 8, 4, 2};
         int i = 0;

         for(int pass = 0; pass < 4; ++pass) {
            for(int y = starts[pass]; y < height; y += steps[pass]) {
               rows[i++] = y;
            }
         }

         return rows;
      }
   }

   private void clear(int[] canvas, int x, int y, int w, int h) {
      int x1 = Math.min(this.width, x + w);
      int y1 = Math.min(this.height, y + h);
      if (x < x1) {
         for(int row = y; row < y1; ++row) {
            Arrays.fill(canvas, row * this.width + x, row * this.width + x1, 0);
         }

      }
   }

   private int countFrames() {
      int frames = 0;
      this.pos = this.blocksStart;

      while(this.pos < this.data.length) {
         int block = this.u8();
         if (block == 44) {
            ++frames;
            this.pos += 8;
            int flags = this.u8();
            if ((flags & 128) != 0) {
               this.pos += 3 * (2 << (flags & 7));
            }

            ++this.pos;
            this.skipSubBlocks();
         } else {
            if (block != 33) {
               break;
            }

            ++this.pos;
            this.skipSubBlocks();
         }
      }

      return frames;
   }

   private int[] palette(int flags) {
      int[] palette = new int[2 << (flags & 7)];

      for(int i = 0; i < palette.length; ++i) {
         palette[i] = -16777216 | this.u8() << 16 | this.u8() << 8 | this.u8();
      }

      return palette;
   }

   private void skipSubBlocks() {
      while(this.pos < this.data.length) {
         int size = this.u8();
         if (size == 0) {
            return;
         }

         this.pos += size;
      }

   }

   private byte[] readSubBlocks() {
      int total = 0;

      int size;
      for(int p = this.pos; p < this.data.length; p += 1 + size) {
         size = this.data[p] & 255;
         if (size == 0) {
            break;
         }

         total += size;
      }

      byte[] out = new byte[total];

      int size;
      for(size = 0; this.pos < this.data.length; this.pos += size) {
         size = this.u8();
         if (size == 0) {
            break;
         }

         int copy = Math.min(size, this.data.length - this.pos);
         System.arraycopy(this.data, this.pos, out, size, copy);
         size += copy;
      }

      return size == total ? out : Arrays.copyOf(out, size);
   }

   private int u8() {
      return this.pos < this.data.length ? this.data[this.pos++] & 255 : 0;
   }

   private int u16() {
      return this.u8() | this.u8() << 8;
   }

   @Environment(EnvType.CLIENT)
   static record Frame(int[] argb, int width, int height, int delayMs) {
   }

   @Environment(EnvType.CLIENT)
   interface Sink {
      boolean accept(Frame var1);
   }
}
