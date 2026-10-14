package net.anubis.client.module.impl.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10538;
import net.minecraft.class_12079;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_7920;
import net.minecraft.class_8685;

@Environment(EnvType.CLIENT)
public final class SkinChangeModule extends ClientModule {
   private final StringSetting selfPlayerSetting = (StringSetting)this.addSetting("self-player", new StringSetting("Self Player", ""));
   private final StringSetting othersPlayerSetting = (StringSetting)this.addSetting("others-player", new StringSetting("Others Player", ""));
   private volatile CustomSkin selfSkin;
   private volatile CustomSkin othersSkin;
   private String loadedSelfName = "";
   private String loadedOthersName = "";
   private boolean selfLoading;
   private boolean othersLoading;
   private final SwapCache selfSwaps = new SwapCache();
   private final SwapCache othersSwaps = new SwapCache();

   public SkinChangeModule() {
      super("skin-change", "Skin Change", "Wear any player's skin client-side.", ModuleCategory.MISC);
   }

   protected void onEnable() {
      this.reloadIfChanged();
      this.on(TickEvent.TYPE, (e) -> this.reloadIfChanged());
   }

   protected void onDisable() {
      this.selfSkin = null;
      this.othersSkin = null;
      this.loadedSelfName = "";
      this.loadedOthersName = "";
      this.selfSwaps.clear();
      this.othersSwaps.clear();
   }

   private void reloadIfChanged() {
      String selfName = this.selfPlayerSetting.get().trim();
      String othersName = this.othersPlayerSetting.get().trim();
      if (!selfName.equals(this.loadedSelfName)) {
         this.selfSkin = null;
         if (selfName.isEmpty()) {
            this.loadedSelfName = "";
         } else if (!this.selfLoading) {
            this.loadedSelfName = selfName;
            this.selfLoading = true;
            this.loadFromUsername(selfName, true);
         }
      }

      if (!othersName.equals(this.loadedOthersName)) {
         this.othersSkin = null;
         if (othersName.isEmpty()) {
            this.loadedOthersName = "";
         } else if (!this.othersLoading) {
            this.loadedOthersName = othersName;
            this.othersLoading = true;
            this.loadFromUsername(othersName, false);
         }
      }

   }

   private void loadFromUsername(String username, boolean isSelf) {
      class_310 mc = class_310.method_1551();
      CompletableFuture.runAsync(() -> {
         try {
            String uuid = fetchUUID(username);
            if (uuid == null) {
               mc.execute(() -> this.finishLoad(username, isSelf, (CustomSkin)null));
               return;
            }

            SkinSource skin = fetchSkin(uuid);
            if (skin == null) {
               mc.execute(() -> this.finishLoad(username, isSelf, (CustomSkin)null));
               return;
            }

            class_2960 id = isSelf ? class_2960.method_60655("anubis", "skin/self") : class_2960.method_60655("anubis", "skin/others");
            Path cacheDir = mc.field_1697.toPath().resolve("cache/anubis_skins");

            try {
               Files.createDirectories(cacheDir);
            } catch (IOException var9) {
            }

            Path cachePath = cacheDir.resolve((isSelf ? "self_" : "others_") + uuid + ".png");
            registerSkin(id, cachePath, skin.url()).whenCompleteAsync((texture, error) -> this.finishLoad(username, isSelf, texture == null ? null : new CustomSkin(texture, skin.model())), mc);
         } catch (Exception var10) {
            mc.execute(() -> this.finishLoad(username, isSelf, (CustomSkin)null));
         }

      });
   }

   private void finishLoad(String username, boolean isSelf, CustomSkin loaded) {
      if (isSelf) {
         this.selfLoading = false;
      } else {
         this.othersLoading = false;
      }

      if (this.isEnabled()) {
         String current = (isSelf ? this.selfPlayerSetting : this.othersPlayerSetting).get().trim();
         if (loaded != null && username.equals(current) && username.equals(isSelf ? this.loadedSelfName : this.loadedOthersName)) {
            if (isSelf) {
               this.selfSkin = loaded;
            } else {
               this.othersSkin = loaded;
            }
         }

         this.reloadIfChanged();
      }
   }

   private static CompletableFuture<class_12079.class_12081> registerSkin(class_2960 id, Path cachePath, String url) {
      class_310 mc = class_310.method_1551();
      class_10538 downloader = new class_10538(mc.method_1487(), mc.method_1531(), mc);
      return downloader.method_65861(id, cachePath, url, true);
   }

   private static String fetchUUID(String username) throws Exception {
      HttpURLConnection conn = openGet("https://api.mojang.com/users/profiles/minecraft/" + username);
      int status = conn.getResponseCode();
      if (status != 204 && status != 404) {
         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

         String var7;
         try {
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
               sb.append(line);
            }

            JsonObject obj = JsonParser.parseString(sb.toString()).getAsJsonObject();
            var7 = obj.get("id").getAsString();
         } catch (Throwable var9) {
            try {
               reader.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         reader.close();
         return var7;
      } else {
         return null;
      }
   }

   private static SkinSource fetchSkin(String uuid) throws Exception {
      HttpURLConnection conn = openGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
      if (conn.getResponseCode() != 200) {
         return null;
      } else {
         BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

         String json;
         try {
            StringBuilder sb = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null) {
               sb.append(line);
            }

            json = sb.toString();
         } catch (Throwable var13) {
            try {
               reader.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         reader.close();
         JsonObject profile = JsonParser.parseString(json).getAsJsonObject();

         for(JsonElement elem : profile.getAsJsonArray("properties")) {
            JsonObject prop = elem.getAsJsonObject();
            if ("textures".equals(prop.get("name").getAsString())) {
               String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()));
               JsonObject textures = JsonParser.parseString(decoded).getAsJsonObject().getAsJsonObject("textures");
               if (!textures.has("SKIN")) {
                  return null;
               }

               JsonObject skin = textures.getAsJsonObject("SKIN");
               JsonObject metadata = skin.has("metadata") ? skin.getAsJsonObject("metadata") : null;
               String model = metadata != null && metadata.has("model") ? metadata.get("model").getAsString() : null;
               return new SkinSource(skin.get("url").getAsString(), class_7920.method_74671(model));
            }
         }

         return null;
      }
   }

   private static HttpURLConnection openGet(String url) throws Exception {
      HttpURLConnection conn = (HttpURLConnection)URI.create(url).toURL().openConnection();
      conn.setConnectTimeout(5000);
      conn.setReadTimeout(5000);
      conn.setRequestProperty("User-Agent", "Anubis/1.0");
      return conn;
   }

   public class_8685 skinFor(int entityId, class_8685 original) {
      class_310 mc = class_310.method_1551();
      return this.skinFor(mc.field_1724 != null && entityId == mc.field_1724.method_5628(), original);
   }

   public class_8685 skinFor(UUID profileId, class_8685 original) {
      class_310 mc = class_310.method_1551();
      return this.skinFor(mc.field_1724 != null && mc.field_1724.method_5667().equals(profileId), original);
   }

   private class_8685 skinFor(boolean isSelf, class_8685 original) {
      CustomSkin custom = isSelf ? this.selfSkin : this.othersSkin;
      return custom == null ? original : (isSelf ? this.selfSwaps : this.othersSwaps).swap(original, custom);
   }

   @Environment(EnvType.CLIENT)
   private static record CustomSkin(class_12079.class_12081 body, class_7920 model) {
   }

   @Environment(EnvType.CLIENT)
   private static record SkinSource(String url, class_7920 model) {
   }

   @Environment(EnvType.CLIENT)
   private static final class SwapCache {
      private static final int MAX = 256;
      private final Map<class_8685, class_8685> swapped = new IdentityHashMap();
      private CustomSkin wearing;

      class_8685 swap(class_8685 original, CustomSkin custom) {
         if (custom != this.wearing || this.swapped.size() >= 256) {
            this.swapped.clear();
            this.wearing = custom;
         }

         class_8685 skin = (class_8685)this.swapped.get(original);
         if (skin == null) {
            skin = new class_8685(custom.body(), original.comp_1627(), original.comp_1628(), custom.model(), original.comp_1630());
            this.swapped.put(original, skin);
         }

         return skin;
      }

      void clear() {
         this.swapped.clear();
         this.wearing = null;
      }
   }
}
