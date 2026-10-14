package net.anubis.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_156;
import net.minecraft.class_310;
import net.minecraft.class_4844;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class Accounts {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final Accounts INSTANCE = new Accounts();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final int CONFIG_VERSION = 1;
   private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
   private final ExecutorService worker = Executors.newSingleThreadExecutor((runnable) -> {
      Thread thread = new Thread(runnable, "Anubis Accounts");
      thread.setDaemon(true);
      return thread;
   });
   private final GameSession session = new GameSession();
   private final List<Account> accounts = new ArrayList();
   private volatile List<Account> snapshot = List.of();
   private volatile String activeId;
   private volatile boolean busy;
   private volatile String status = "";
   private volatile Thread microsoftSignIn;
   private volatile boolean loaded;
   private boolean readOnly;

   private Accounts() {
   }

   public static Accounts get() {
      return INSTANCE;
   }

   public List<Account> list() {
      this.ensureLoaded();
      return this.snapshot;
   }

   public Account active() {
      String sessionUuid = class_310.method_1551().method_1548().method_44717().toString();
      List<Account> all = this.list();
      String lastSwitched = this.activeId;

      for(Account account : all) {
         if (account.id().equals(lastSwitched) && account.uuid().equals(sessionUuid)) {
            return account;
         }
      }

      for(Account account : all) {
         if (account.uuid().equals(sessionUuid)) {
            return account;
         }
      }

      return null;
   }

   public CompletableFuture<Account> addCracked(String username) {
      String name = username == null ? "" : username.trim();
      return !VALID_NAME.matcher(name).matches() ? this.failed("A cracked name is 1-16 letters, digits or underscores") : this.signIn(AccountType.CRACKED, (String)null, "Logging in as " + name + "...", () -> offline(name));
   }

   public CompletableFuture<Account> addSession(String token) {
      String accessToken = token == null ? "" : token.trim();
      return accessToken.isEmpty() ? this.failed("Paste an access token first") : this.signIn(AccountType.SESSION, (String)null, "Checking the session token...", () -> SessionLogin.signIn(accessToken));
   }

   public CompletableFuture<Account> addMicrosoft() {
      return this.signIn(AccountType.MICROSOFT, (String)null, "Opening Microsoft sign-in...", this::microsoftBrowserSignIn);
   }

   public void cancelMicrosoft() {
      Thread signIn = this.microsoftSignIn;
      if (signIn != null) {
         signIn.interrupt();
      }

   }

   public CompletableFuture<Void> login(Account account) {
      Account saved = this.find(account);
      if (saved == null) {
         return this.<Void>failed("That account is no longer saved");
      } else {
         String who = saved.name().isEmpty() ? saved.type().label() + " account" : saved.name();
         return this.signIn(saved.type(), saved.id(), "Logging in as " + who + "...", () -> {
            Credentials var10000;
            switch (saved.type()) {
               case CRACKED -> var10000 = offline(saved.name());
               case SESSION -> var10000 = SessionLogin.signIn(saved.secret());
               case MICROSOFT -> var10000 = MicrosoftLogin.minecraft(MicrosoftLogin.refresh(saved.secret()));
               default -> throw new MatchException((String)null, (Throwable)null);
            }

            return var10000;
         }).thenAccept((signedIn) -> {
         });
      }
   }

   public synchronized void remove(Account account) {
      int index = this.indexOf(account);
      if (index >= 0) {
         this.accounts.remove(index);
         this.publish();
         this.save();
      }
   }

   public boolean busy() {
      return this.busy;
   }

   public String status() {
      return this.status;
   }

   private static Credentials offline(String name) {
      return new Credentials(name, class_4844.method_43344(name), "", "");
   }

   private Credentials microsoftBrowserSignIn() throws InterruptedException {
      this.microsoftSignIn = Thread.currentThread();

      Credentials var3;
      try {
         MicrosoftLogin.DeviceCode code = MicrosoftLogin.requestDeviceCode();
         String var10001 = code.userCode();
         this.status = "Waiting for Microsoft sign-in in your browser... (code " + var10001 + " at " + code.verificationUri().replaceFirst("^https?://", "") + ")";
         class_156.method_668().method_670(code.directUri());
         MicrosoftLogin.MsaToken token = MicrosoftLogin.awaitApproval(code);
         this.status = "Signing in to Xbox Live...";
         var3 = MicrosoftLogin.minecraft(token);
      } finally {
         this.microsoftSignIn = null;
      }

      return var3;
   }

   private CompletableFuture<Account> signIn(AccountType type, String savedId, String progress, SignIn blocking) {
      synchronized(this) {
         if (this.busy) {
            return CompletableFuture.failedFuture(new AccountException("Another account action is still running"));
         }

         if (inGame()) {
            return this.<Account>failed("Leave the world before switching accounts");
         }

         this.busy = true;
      }

      this.ensureLoaded();
      this.status = progress;
      CompletableFuture<Account> result = new CompletableFuture();
      this.worker.execute(() -> {
         Thread.interrupted();

         try {
            Credentials credentials = blocking.run();
            GameSession.Prepared prepared = this.session.prepare(type, credentials);
            class_310.method_1551().execute(() -> this.finish(type, savedId, credentials, prepared, result));
         } catch (InterruptedException var7) {
            this.settleLater(result, (Account)null, new AccountException(type == AccountType.MICROSOFT ? "Microsoft sign-in cancelled" : "Login cancelled"));
         } catch (AccountException e) {
            LOGGER.warn("[Anubis] {} login failed: {}", type.label(), e.getMessage());
            this.settleLater(result, (Account)null, e);
         } catch (RuntimeException e) {
            LOGGER.warn("[Anubis] {} login failed", type.label(), e);
            this.settleLater(result, (Account)null, new AccountException("Login failed: " + e.getMessage(), e));
         }

      });
      return result;
   }

   private void finish(AccountType type, String savedId, Credentials credentials, GameSession.Prepared prepared, CompletableFuture<Account> result) {
      Account account;
      synchronized(this) {
         account = this.remember(type, savedId, credentials);
      }

      if (inGame()) {
         this.settle(result, (Account)null, new AccountException("Joined a world before the switch finished - leave it and log in again"));
      } else {
         try {
            this.session.apply(prepared);
         } catch (RuntimeException e) {
            LOGGER.error("[Anubis] Switching the session to {} failed", account, e);
            this.settle(result, (Account)null, new AccountException("Couldn't switch the game to " + credentials.name() + ": " + e.getMessage(), e));
            return;
         }

         this.activeId = account.id();
         LOGGER.info("[Anubis] Switched session to {}", account);
         this.status = "Logged in as " + credentials.name();
         this.settle(result, account, (AccountException)null);
      }
   }

   private void settleLater(CompletableFuture<Account> result, Account account, AccountException error) {
      class_310.method_1551().execute(() -> this.settle(result, account, error));
   }

   private void settle(CompletableFuture<Account> result, Account account, AccountException error) {
      if (error != null) {
         this.status = error.getMessage();
      }

      this.busy = false;
      if (error != null) {
         result.completeExceptionally(error);
      } else {
         result.complete(account);
      }

   }

   private Account remember(AccountType type, String savedId, Credentials credentials) {
      String uuid = credentials.uuid().toString();
      int index = -1;

      for(int i = 0; i < this.accounts.size(); ++i) {
         Account existing = (Account)this.accounts.get(i);
         boolean var10000;
         if (savedId != null) {
            var10000 = existing.id().equals(savedId);
         } else {
            label46: {
               label45: {
                  if (existing.type() == type) {
                     if (type == AccountType.CRACKED) {
                        if (existing.name().equalsIgnoreCase(credentials.name())) {
                           break label45;
                        }
                     } else if (existing.uuid().equals(uuid)) {
                        break label45;
                     }
                  }

                  var10000 = false;
                  break label46;
               }

               var10000 = true;
            }
         }

         boolean same = var10000;
         if (same) {
            index = i;
            break;
         }
      }

      if (index < 0 && savedId != null) {
         return new Account(savedId, type, credentials.name(), uuid, credentials.secret());
      } else {
         Account account = index >= 0 ? ((Account)this.accounts.get(index)).withLogin(credentials.name(), uuid, credentials.secret()) : new Account(UUID.randomUUID().toString(), type, credentials.name(), uuid, credentials.secret());
         if (index >= 0) {
            this.accounts.set(index, account);
         } else {
            this.accounts.add(account);
         }

         this.publish();
         this.save();
         return account;
      }
   }

   private static boolean inGame() {
      class_310 mc = class_310.method_1551();
      return mc.field_1687 != null || mc.method_1562() != null;
   }

   private <T> CompletableFuture<T> failed(String message) {
      this.status = message;
      return CompletableFuture.failedFuture(new AccountException(message));
   }

   private synchronized Account find(Account account) {
      int index = this.indexOf(account);
      return index < 0 ? null : (Account)this.accounts.get(index);
   }

   private int indexOf(Account account) {
      this.ensureLoaded();
      if (account == null) {
         return -1;
      } else {
         for(int i = 0; i < this.accounts.size(); ++i) {
            if (((Account)this.accounts.get(i)).id().equals(account.id())) {
               return i;
            }
         }

         return -1;
      }
   }

   private void publish() {
      this.snapshot = List.copyOf(this.accounts);
   }

   private void ensureLoaded() {
      if (!this.loaded) {
         this.load();
      }

   }

   private synchronized void load() {
      if (!this.loaded) {
         this.loaded = true;
         Path path = configPath();
         if (Files.isRegularFile(path, new LinkOption[0])) {
            try {
               label87: {
                  Reader reader = Files.newBufferedReader(path);

                  label78: {
                     label88: {
                        try {
                           JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                           int version = root.has("version") ? root.get("version").getAsInt() : 0;
                           if (version > 1) {
                              this.readOnly = true;
                              LOGGER.warn("[Anubis] Account list version {} is newer than supported {}; skipping load", version, 1);
                              break label78;
                           }

                           if (root.has("accounts") && root.get("accounts").isJsonArray()) {
                              Iterator var5 = root.getAsJsonArray("accounts").iterator();

                              while(true) {
                                 if (!var5.hasNext()) {
                                    break label88;
                                 }

                                 JsonElement element = (JsonElement)var5.next();
                                 if (element.isJsonObject()) {
                                    Account account = read(element.getAsJsonObject());
                                    if (account != null) {
                                       this.accounts.add(account);
                                    }
                                 }
                              }
                           }
                        } catch (Throwable var9) {
                           if (reader != null) {
                              try {
                                 reader.close();
                              } catch (Throwable var8) {
                                 var9.addSuppressed(var8);
                              }
                           }

                           throw var9;
                        }

                        if (reader != null) {
                           reader.close();
                        }

                        return;
                     }

                     if (reader != null) {
                        reader.close();
                     }
                     break label87;
                  }

                  if (reader != null) {
                     reader.close();
                  }

                  return;
               }
            } catch (RuntimeException | IOException e) {
               LOGGER.warn("[Anubis] Failed to load the account list; preserving the file", e);
               this.accounts.clear();
               this.readOnly = true;
            }

            this.publish();
         }
      }
   }

   private static Account read(JsonObject json) {
      String typeName = AuthHttp.text(json, "type");
      AccountType type = null;

      for(AccountType candidate : AccountType.values()) {
         if (candidate.name().equalsIgnoreCase(typeName)) {
            type = candidate;
         }
      }

      if (type == null) {
         return null;
      } else {
         String id = AuthHttp.text(json, "id");
         String name = AuthHttp.text(json, "name");
         String uuid = AuthHttp.text(json, "uuid");
         String token = AuthHttp.text(json, "token");
         if (name == null) {
            name = "";
         }

         if (type == AccountType.CRACKED) {
            if (!VALID_NAME.matcher(name).matches()) {
               return null;
            }
         } else if (token == null || token.isEmpty()) {
            return null;
         }

         if (type == AccountType.CRACKED) {
            uuid = class_4844.method_43344(name).toString();
         }

         return new Account(id != null && !id.isEmpty() ? id : UUID.randomUUID().toString(), type, name, uuid == null ? "" : uuid, token == null ? "" : token);
      }
   }

   private void save() {
      if (!this.readOnly) {
         JsonObject root = new JsonObject();
         root.addProperty("version", 1);
         JsonArray list = new JsonArray();

         for(Account account : this.accounts) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", account.id());
            entry.addProperty("type", account.type().name());
            entry.addProperty("name", account.name());
            entry.addProperty("uuid", account.uuid());
            if (!account.secret().isEmpty()) {
               entry.addProperty("token", account.secret());
            }

            list.add(entry);
         }

         root.add("accounts", list);
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
            LOGGER.warn("[Anubis] Failed to save the account list", e);
         }

      }
   }

   private static Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("anubis").resolve("accounts.json");
   }

   @Environment(EnvType.CLIENT)
   private interface SignIn {
      Credentials run() throws InterruptedException;
   }
}
