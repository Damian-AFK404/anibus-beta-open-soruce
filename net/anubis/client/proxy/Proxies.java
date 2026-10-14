package net.anubis.client.proxy;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class Proxies {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Proxies INSTANCE = new Proxies();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");
   private static final Pattern HOST_NAME = Pattern.compile("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*");
   private final List<Proxy> proxies = new CopyOnWriteArrayList();
   private final List<Proxy> view;
   private volatile boolean loaded;
   private boolean readOnly;

   private Proxies() {
      this.view = Collections.unmodifiableList(this.proxies);
   }

   public static Proxies get() {
      return INSTANCE;
   }

   public List<Proxy> list() {
      this.ensureLoaded();
      return this.view;
   }

   public Proxy enabled() {
      this.ensureLoaded();

      for(Proxy proxy : this.proxies) {
         if (proxy.enabled) {
            return proxy;
         }
      }

      return null;
   }

   public synchronized void setEnabled(Proxy proxy, boolean on) {
      this.ensureLoaded();
      if (this.proxies.contains(proxy)) {
         if (on) {
            if (!proxy.isValid()) {
               return;
            }

            for(Proxy other : this.proxies) {
               other.enabled = other == proxy;
            }
         } else {
            proxy.enabled = false;
         }

         this.save();
      }
   }

   public synchronized void remove(Proxy proxy) {
      this.ensureLoaded();
      if (this.proxies.remove(proxy)) {
         this.save();
      }

   }

   public synchronized void removeAll(Collection<Proxy> gone) {
      this.ensureLoaded();
      if (this.proxies.removeAll(Set.copyOf(gone))) {
         this.save();
      }

   }

   public synchronized void checked() {
      this.ensureLoaded();
      this.save();
   }

   public synchronized int importText(String text) {
      this.ensureLoaded();
      if (text != null && !text.isBlank()) {
         Set<String> known = new HashSet();

         for(Proxy proxy : this.proxies) {
            known.add(key(proxy));
         }

         List<Proxy> found = new ArrayList();

         for(String line : text.split("\\R")) {
            Proxy proxy = parseLine(line);
            if (proxy != null && known.add(key(proxy))) {
               found.add(proxy);
            }
         }

         if (found.isEmpty()) {
            return 0;
         } else {
            this.proxies.addAll(found);
            this.save();
            return found.size();
         }
      } else {
         return 0;
      }
   }

   synchronized Proxy enabledCopy() {
      Proxy proxy = this.enabled();
      return proxy == null ? null : proxy.copy();
   }

   private void keepOneEnabled() {
      Proxy keep = null;

      for(Proxy proxy : this.proxies) {
         if (keep == null && proxy.enabled && proxy.isValid()) {
            keep = proxy;
         }
      }

      for(Proxy proxy : this.proxies) {
         proxy.enabled = proxy == keep;
      }

   }

   private static String key(Proxy proxy) {
      return proxy.displayAddress().toLowerCase(Locale.ROOT);
   }

   static Proxy parseLine(String line) {
      String rest = line == null ? "" : line.strip();
      if (!rest.isEmpty() && !rest.startsWith("#")) {
         ProxyType type = ProxyType.SOCKS5;
         int scheme = rest.indexOf("://");
         if (scheme >= 0) {
            type = ProxyType.parse(rest.substring(0, scheme));
            if (type == null) {
               return null;
            }

            rest = rest.substring(scheme + 3);
            if (rest.endsWith("/")) {
               rest = rest.substring(0, rest.length() - 1);
            }
         }

         String username = "";
         String password = "";
         int at = rest.lastIndexOf(64);
         boolean uriLogin = at >= 0;
         if (uriLogin) {
            String login = rest.substring(0, at);
            rest = rest.substring(at + 1);
            int colon = login.indexOf(58);
            username = colon < 0 ? login : login.substring(0, colon);
            password = colon < 0 ? "" : login.substring(colon + 1);
         }

         String host;
         if (rest.startsWith("[")) {
            int close = rest.indexOf(93);
            if (close < 0 || close + 1 >= rest.length() || rest.charAt(close + 1) != ':') {
               return null;
            }

            host = rest.substring(1, close);
            rest = rest.substring(close + 2);
         } else {
            int colon = rest.indexOf(58);
            if (colon < 0) {
               return null;
            }

            host = rest.substring(0, colon);
            rest = rest.substring(colon + 1);
         }

         String[] parts = rest.split(":", 3);
         int port = parsePort(parts[0]);
         host = normalizeHost(host);
         if (port >= 0 && host != null) {
            if (parts.length > 1) {
               if (uriLogin || parts[1].isEmpty()) {
                  return null;
               }

               username = parts[1];
               password = parts.length > 2 ? parts[2] : "";
            }

            Proxy proxy = new Proxy();
            proxy.type = type;
            proxy.address = host;
            proxy.port = port;
            proxy.username = username;
            proxy.password = password;
            proxy.name = proxy.displayAddress();
            return proxy;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static int parsePort(String text) {
      if (!text.isEmpty() && text.length() <= 5) {
         for(int i = 0; i < text.length(); ++i) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
               return -1;
            }
         }

         int port = Integer.parseInt(text);
         return port >= 1 && port <= 65535 ? port : -1;
      } else {
         return -1;
      }
   }

   private static String normalizeHost(String host) {
      if (IPV4.matcher(host).matches()) {
         StringBuilder plain = new StringBuilder();

         for(String octet : host.split("\\.")) {
            int value = Integer.parseInt(octet);
            if (value > 255) {
               return null;
            }

            if (!plain.isEmpty()) {
               plain.append('.');
            }

            plain.append(value);
         }

         return plain.toString();
      } else if (host.indexOf(58) >= 0) {
         return InetAddresses.isInetAddress(host) ? host : null;
      } else {
         return HOST_NAME.matcher(host).matches() ? host : null;
      }
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }

   }

   private synchronized void load() {
      if (!this.loaded) {
         Path path = configPath();
         List<Proxy> saved = new ArrayList();
         if (Files.isRegularFile(path, new LinkOption[0])) {
            try {
               Reader reader = Files.newBufferedReader(path);

               try {
                  JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                  int version = integer(root, "version");
                  if (version > 1) {
                     this.readOnly = true;
                     LOGGER.warn("[Anubis] Proxy list version {} is newer than supported {}; skipping load", version, 1);
                  } else {
                     JsonElement var7 = root.get("proxies");
                     if (var7 instanceof JsonArray) {
                        for(JsonElement element : (JsonArray)var7) {
                           if (element instanceof JsonObject) {
                              JsonObject entry = (JsonObject)element;
                              saved.add(readEntry(entry));
                           }
                        }
                     }
                  }
               } catch (Throwable var11) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (reader != null) {
                  reader.close();
               }
            } catch (RuntimeException | IOException e) {
               LOGGER.warn("[Anubis] Failed to load the proxy list; starting empty", e);
               saved.clear();
               this.setAside(path);
            }
         }

         this.proxies.addAll(saved);
         this.keepOneEnabled();
         this.loaded = true;
      }
   }

   private void setAside(Path path) {
      try {
         Files.move(path, path.resolveSibling(String.valueOf(path.getFileName()) + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         LOGGER.warn("[Anubis] Couldn't move the broken proxy list aside; not saving proxies this session", e);
         this.readOnly = true;
      }

   }

   private static Proxy readEntry(JsonObject json) {
      Proxy proxy;
      boolean var10001;
      label24: {
         proxy = new Proxy();
         proxy.name = string(json, "name");
         ProxyType type = ProxyType.parse(string(json, "type"));
         proxy.type = type == null ? ProxyType.SOCKS5 : type;
         proxy.address = string(json, "address");
         proxy.port = integer(json, "port");
         proxy.username = string(json, "username");
         proxy.password = string(json, "password");
         JsonElement var4 = json.get("enabled");
         if (var4 instanceof JsonPrimitive flag) {
            if (flag.isBoolean() && flag.getAsBoolean()) {
               var10001 = true;
               break label24;
            }
         }

         var10001 = false;
      }

      proxy.enabled = var10001;
      JsonElement var6 = json.get("ping");
      if (var6 instanceof JsonPrimitive ping) {
         if (ping.isNumber()) {
            proxy.ping = Math.max(-1, ping.getAsInt());
         }
      }

      return proxy;
   }

   private static String string(JsonObject json, String key) {
      JsonElement var3 = json.get(key);
      String var10000;
      if (var3 instanceof JsonPrimitive value) {
         var10000 = value.getAsString();
      } else {
         var10000 = "";
      }

      return var10000;
   }

   private static int integer(JsonObject json, String key) {
      JsonElement var3 = json.get(key);
      int var10000;
      if (var3 instanceof JsonPrimitive value) {
         if (value.isNumber()) {
            var10000 = value.getAsInt();
            return var10000;
         }
      }

      var10000 = 0;
      return var10000;
   }

   private void save() {
      if (!this.readOnly) {
         JsonObject root = new JsonObject();
         root.addProperty("version", 1);
         JsonArray list = new JsonArray();

         for(Proxy proxy : this.proxies) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", proxy.name == null ? "" : proxy.name);
            entry.addProperty("type", (proxy.type == null ? ProxyType.SOCKS5 : proxy.type).name());
            entry.addProperty("address", proxy.address == null ? "" : proxy.address.trim());
            entry.addProperty("port", proxy.port);
            if (proxy.username != null && !proxy.username.isEmpty()) {
               entry.addProperty("username", proxy.username);
            }

            if (proxy.password != null && !proxy.password.isEmpty()) {
               entry.addProperty("password", proxy.password);
            }

            entry.addProperty("enabled", proxy.enabled);
            if (proxy.ping != -2) {
               entry.addProperty("ping", proxy.ping);
            }

            list.add(entry);
         }

         root.add("proxies", list);
         Path path = configPath();

         try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            Writer writer = Files.newBufferedWriter(tmp);

            try {
               GSON.toJson(root, writer);
            } catch (Throwable var10) {
               if (writer != null) {
                  try {
                     writer.close();
                  } catch (Throwable var8) {
                     var10.addSuppressed(var8);
                  }
               }

               throw var10;
            }

            if (writer != null) {
               writer.close();
            }

            try {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var9) {
               Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
         } catch (IOException e) {
            LOGGER.warn("[Anubis] Failed to save the proxy list", e);
         }

      }
   }

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("proxies.json");
   }
}
