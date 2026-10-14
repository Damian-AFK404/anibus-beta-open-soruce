package net.anubis.client.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class AuthHttp {
   private static final HttpClient CLIENT;
   private static final Duration TIMEOUT;
   private static final String USER_AGENT = "AnubisClient";

   private AuthHttp() {
   }

   static Reply postForm(String url, Map<String, String> form) throws InterruptedException {
      String body = (String)form.entrySet().stream().map((e) -> {
         String var10000 = URLEncoder.encode((String)e.getKey(), StandardCharsets.UTF_8);
         return var10000 + "=" + URLEncoder.encode((String)e.getValue(), StandardCharsets.UTF_8);
      }).collect(Collectors.joining("&"));
      return send(url, HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/x-www-form-urlencoded").POST(BodyPublishers.ofString(body)));
   }

   static Reply postJson(String url, String body, Map<String, String> headers) throws InterruptedException {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json").POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8));
      Objects.requireNonNull(request);
      headers.forEach(request::header);
      return send(url, request);
   }

   static Reply get(String url, String bearerToken) throws InterruptedException {
      return send(url, HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + bearerToken).GET());
   }

   static String text(JsonObject object, String key) {
      JsonElement value = object == null ? null : object.get(key);
      return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
   }

   static JsonObject object(JsonObject object, String key) {
      JsonElement value = object == null ? null : object.get(key);
      return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
   }

   private static Reply send(String url, HttpRequest.Builder request) throws InterruptedException {
      try {
         HttpResponse<String> response = CLIENT.send(request.timeout(TIMEOUT).header("Accept", "application/json").header("User-Agent", "AnubisClient").build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
         return new Reply(response.statusCode(), parse((String)response.body()), response.headers());
      } catch (IOException e) {
         throw new AccountException("Couldn't reach " + URI.create(url).getHost() + " - check your connection", e);
      }
   }

   private static JsonObject parse(String body) {
      try {
         JsonElement root = JsonParser.parseString(body);
         return root.isJsonObject() ? root.getAsJsonObject() : new JsonObject();
      } catch (RuntimeException var2) {
         return new JsonObject();
      }
   }

   static {
      CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(Redirect.NEVER).build();
      TIMEOUT = Duration.ofSeconds(30L);
   }

   @Environment(EnvType.CLIENT)
   static record Reply(int status, JsonObject json, HttpHeaders headers) {
      boolean ok() {
         return this.status >= 200 && this.status < 300;
      }

      String text(String key) {
         return AuthHttp.text(this.json, key);
      }
   }
}
