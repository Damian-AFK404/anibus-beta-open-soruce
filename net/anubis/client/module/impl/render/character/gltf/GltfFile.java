package net.anubis.client.module.impl.render.character.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class GltfFile {
   private static final int GLTF = 1179937895;
   private static final int JSON = 1313821514;
   private static final int BIN = 5130562;
   private static final int MAX_ELEMENTS = 33554432;
   final JsonObject json;
   private final ByteBuffer bin;

   private GltfFile(JsonObject json, ByteBuffer bin) {
      this.json = json;
      this.bin = bin;
   }

   static GltfFile read(byte[] data) throws IOException {
      ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
      if (data.length >= 20 && buf.getInt(0) == 1179937895) {
         if (buf.getInt(4) != 2) {
            throw new IOException("only glTF 2.0 files are supported");
         } else {
            int end = Math.min(buf.getInt(8), data.length);
            int off = 12;
            JsonObject json = null;

            ByteBuffer bin;
            int length;
            for(bin = ByteBuffer.allocate(0); off + 8 <= end; off += 8 + length) {
               length = buf.getInt(off);
               int type = buf.getInt(off + 4);
               if (length < 0 || length > end - off - 8) {
                  throw new IOException("the file is cut short");
               }

               if (type == 1313821514) {
                  json = JsonParser.parseString(new String(data, off + 8, length, StandardCharsets.UTF_8)).getAsJsonObject();
               } else if (type == 5130562) {
                  bin = ByteBuffer.wrap(data, off + 8, length).slice().order(ByteOrder.LITTLE_ENDIAN);
               }
            }

            if (json == null) {
               throw new IOException("the file has no glTF data");
            } else {
               JsonArray required = array(json, "extensionsRequired");
               if (!required.isEmpty()) {
                  throw new IOException("it needs " + String.valueOf(required) + ", export it without compression");
               } else {
                  return new GltfFile(json, bin);
               }
            }
         }
      } else {
         throw new IOException("not a .glb file");
      }
   }

   static JsonArray array(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
   }

   static JsonObject object(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
   }

   static int integer(JsonObject o, String key, int fallback) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsInt() : fallback;
   }

   private static String string(JsonObject o, String key, String fallback) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
   }

   int nodes() {
      return array(this.json, "nodes").size();
   }

   JsonObject node(int i) {
      return array(this.json, "nodes").get(i).getAsJsonObject();
   }

   String nodeName(int i) {
      return string(this.node(i), "name", "node" + i);
   }

   private JsonObject material(int m) {
      return m < 0 ? new JsonObject() : array(this.json, "materials").get(m).getAsJsonObject();
   }

   String materialName(int m) {
      return string(this.material(m), "name", "material" + m);
   }

   boolean translucent(int m) {
      String mode = string(this.material(m), "alphaMode", "OPAQUE");
      return mode.equals("BLEND") || mode.equals("MASK");
   }

   boolean doubleSided(int m) {
      JsonElement e = this.material(m).get("doubleSided");
      return e != null && e.getAsBoolean();
   }

   private JsonObject baseColorTexture(int m) {
      return object(object(this.material(m), "pbrMetallicRoughness"), "baseColorTexture");
   }

   int texCoord(int m) {
      return integer(this.baseColorTexture(m), "texCoord", 0);
   }

   float[] baseColorFactor(int m) {
      JsonArray f = array(object(this.material(m), "pbrMetallicRoughness"), "baseColorFactor");
      return f.size() != 4 ? new float[]{1.0F, 1.0F, 1.0F, 1.0F} : new float[]{f.get(0).getAsFloat(), f.get(1).getAsFloat(), f.get(2).getAsFloat(), f.get(3).getAsFloat()};
   }

   byte[] baseColorImage(int m) throws IOException {
      JsonObject tex = this.baseColorTexture(m);
      if (!tex.has("index")) {
         return null;
      } else {
         JsonObject texture = array(this.json, "textures").get(tex.get("index").getAsInt()).getAsJsonObject();
         int source = integer(texture, "source", -1);
         if (source < 0) {
            return null;
         } else {
            JsonObject image = array(this.json, "images").get(source).getAsJsonObject();
            if (image.has("bufferView")) {
               ByteBuffer b = this.view(image.get("bufferView").getAsInt(), 0);
               byte[] out = new byte[b.remaining()];
               b.get(out);
               return out;
            } else {
               String uri = string(image, "uri", "");
               if (uri.startsWith("data:")) {
                  return Base64.getDecoder().decode(uri.substring(uri.indexOf(44) + 1));
               } else {
                  throw new IOException("its textures are separate files, export it as a single .glb");
               }
            }
         }
      }
   }

   float[] floats(int accessor) throws IOException {
      JsonObject a = this.accessor(accessor);
      int count = a.get("count").getAsInt();
      int comps = components(a.get("type").getAsString());
      int type = a.get("componentType").getAsInt();
      JsonElement norm = a.get("normalized");
      boolean normalized = norm != null && norm.getAsBoolean();
      float[] out = new float[count * comps];
      if (!a.has("bufferView")) {
         return out;
      } else {
         ByteBuffer b = this.view(a.get("bufferView").getAsInt(), integer(a, "byteOffset", 0));
         int stride = this.stride(a, comps, type);
         int size = size(type);

         for(int i = 0; i < count; ++i) {
            for(int c = 0; c < comps; ++c) {
               int at = i * stride + c * size;
               int var10001 = i * comps + c;
               float var10002;
               switch (type) {
                  case 5120:
                     var10002 = normalized ? Math.max((float)b.get(at) / 127.0F, -1.0F) : (float)b.get(at);
                     break;
                  case 5121:
                     var10002 = (float)(b.get(at) & 255) / (normalized ? 255.0F : 1.0F);
                     break;
                  case 5122:
                     var10002 = normalized ? Math.max((float)b.getShort(at) / 32767.0F, -1.0F) : (float)b.getShort(at);
                     break;
                  case 5123:
                     var10002 = (float)(b.getShort(at) & '\uffff') / (normalized ? 65535.0F : 1.0F);
                     break;
                  case 5124:
                  default:
                     throw new IOException("unknown component type " + type);
                  case 5125:
                     var10002 = (float)b.getInt(at);
                     break;
                  case 5126:
                     var10002 = b.getFloat(at);
               }

               out[var10001] = var10002;
            }
         }

         return out;
      }
   }

   int[] ints(int accessor) throws IOException {
      JsonObject a = this.accessor(accessor);
      int count = a.get("count").getAsInt();
      int comps = components(a.get("type").getAsString());
      int type = a.get("componentType").getAsInt();
      ByteBuffer b = this.view(a.get("bufferView").getAsInt(), integer(a, "byteOffset", 0));
      int stride = this.stride(a, comps, type);
      int size = size(type);
      int[] out = new int[count * comps];

      for(int i = 0; i < count; ++i) {
         for(int c = 0; c < comps; ++c) {
            int at = i * stride + c * size;
            int var10001 = i * comps + c;
            int var10002;
            switch (type) {
               case 5121:
                  var10002 = b.get(at) & 255;
                  break;
               case 5122:
               case 5124:
               default:
                  throw new IOException("unknown index type " + type);
               case 5123:
                  var10002 = b.getShort(at) & '\uffff';
                  break;
               case 5125:
                  var10002 = b.getInt(at);
            }

            out[var10001] = var10002;
         }
      }

      return out;
   }

   private JsonObject accessor(int index) throws IOException {
      JsonObject a = array(this.json, "accessors").get(index).getAsJsonObject();
      if (a.has("sparse")) {
         throw new IOException("sparse accessors aren't supported");
      } else {
         long elements = (long)a.get("count").getAsInt() * (long)components(a.get("type").getAsString());
         if (elements >= 0L && elements <= 33554432L) {
            return a;
         } else {
            throw new IOException("a mesh is far too large");
         }
      }
   }

   private ByteBuffer view(int index, int extra) throws IOException {
      JsonObject v = array(this.json, "bufferViews").get(index).getAsJsonObject();
      if (integer(v, "buffer", 0) != 0) {
         throw new IOException("its data is split over several files");
      } else {
         int off = integer(v, "byteOffset", 0) + extra;
         int length = v.get("byteLength").getAsInt() - extra;
         if (off >= 0 && length >= 0 && off + length <= this.bin.capacity()) {
            return this.bin.slice(off, length).order(ByteOrder.LITTLE_ENDIAN);
         } else {
            throw new IOException("the file is cut short");
         }
      }
   }

   private int stride(JsonObject a, int comps, int type) {
      JsonObject v = array(this.json, "bufferViews").get(a.get("bufferView").getAsInt()).getAsJsonObject();
      return integer(v, "byteStride", comps * size(type));
   }

   private static int size(int type) {
      byte var10000;
      switch (type) {
         case 5120:
         case 5121:
            var10000 = 1;
            break;
         case 5122:
         case 5123:
            var10000 = 2;
            break;
         default:
            var10000 = 4;
      }

      return var10000;
   }

   private static int components(String type) throws IOException {
      byte var10000;
      switch (type) {
         case "SCALAR" -> var10000 = 1;
         case "VEC2" -> var10000 = 2;
         case "VEC3" -> var10000 = 3;
         case "VEC4" -> var10000 = 4;
         case "MAT4" -> var10000 = 16;
         default -> throw new IOException("unknown accessor type " + type);
      }

      return var10000;
   }
}
