package net.anubis.client.module.impl.misc.spawnerprotect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class SpawnerAlerts {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Gson GSON = new Gson();
   private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy • HH:mm:ss").withZone(ZoneId.of("Europe/Istanbul"));
   private static final int MAX_ATTEMPTS = 3;
   private static final double MAX_RETRY_SECONDS = (double)30.0F;
   private static final Duration TIMEOUT = Duration.ofSeconds(5L);
   private static final ThreadPoolExecutor WORKER;

   private SpawnerAlerts() {
   }

   public static Map<String, Object> field(String name, String value, boolean inline) {
      return Map.of("name", name, "value", value, "inline", inline);
   }

   public static String payload(String title, String description, int color, List<Map<String, Object>> fields, Instant timestamp) {
      Map<String, Object> embed = Map.of("title", title, "description", description, "color", color, "fields", new ArrayList(fields), "timestamp", timestamp.toString(), "author", Map.of("name", "ANUBIS  /  SPAWNER PROTECT"), "footer", Map.of("text", "Minecraft 1.21.11 • " + LOCAL_TIME.format(timestamp) + " (TR)"));
      return GSON.toJson(Map.of("username", "Spawner Protect • Anubis", "allowed_mentions", Map.of("parse", List.of()), "embeds", List.of(embed)));
   }

   public static void send(String url, String payload) {
      if (url != null && !url.isBlank()) {
         WORKER.execute(() -> post(url.trim(), payload));
      }
   }

   private static void post(String url, String payload) {
      int attempt = 0;

      while(true) {
         label45: {
            if (attempt < 3) {
               try {
                  HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).header("Content-Type", "application/json; charset=utf-8").POST(BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
                  HttpResponse<String> response = SpawnerAlerts.ClientHolder.CLIENT.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
                  int status = response.statusCode();
                  if (status >= 200 && status < 300) {
                     return;
                  }

                  if (status != 429 || attempt >= 2) {
                     LOGGER.warn("[Anubis] Spawner Protect alert failed (HTTP {})", status);
                     return;
                  }

                  double seconds = retryAfter((String)response.body());
                  if (Double.isFinite(seconds) && !(seconds > (double)30.0F)) {
                     Thread.sleep((long)(Math.max((double)0.25F, seconds) * (double)1000.0F) + 100L);
                     break label45;
                  }
               } catch (InterruptedException var8) {
                  Thread.currentThread().interrupt();
                  return;
               } catch (Exception e) {
                  LOGGER.warn("[Anubis] Spawner Protect alert could not be sent ({})", e.getClass().getSimpleName());
                  return;
               }
            }

            LOGGER.warn("[Anubis] Spawner Protect alerts are rate limited");
            return;
         }

         ++attempt;
      }
   }

   private static double retryAfter(String body) {
      if (body != null && !body.isBlank()) {
         try {
            JsonObject response = JsonParser.parseString(body).getAsJsonObject();
            return response.has("retry_after") ? response.get("retry_after").getAsDouble() : (double)1.0F;
         } catch (RuntimeException var2) {
            return (double)1.0F;
         }
      } else {
         return (double)1.0F;
      }
   }

   static {
      WORKER = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue(64), (task) -> {
         Thread thread = new Thread(task, "Anubis-SpawnerProtect-Alerts");
         thread.setDaemon(true);
         return thread;
      }, (task, executor) -> LOGGER.warn("[Anubis] Spawner Protect alert queue is full"));
   }

   @Environment(EnvType.CLIENT)
   private static final class ClientHolder {
      static final HttpClient CLIENT;

      static {
         CLIENT = HttpClient.newBuilder().connectTimeout(SpawnerAlerts.TIMEOUT).build();
      }
   }
}
