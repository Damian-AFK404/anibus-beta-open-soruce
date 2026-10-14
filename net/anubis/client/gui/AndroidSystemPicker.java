package net.anubis.client.gui;

import imgui.AnubisAndroidNative;
import imgui.ImGui;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AndroidSystemPicker {
   static final long MAX_IMAGE_BYTES = 67108864L;
   static final int MAX_STATUS_BYTES = 4096;
   private static final String RESOURCE = "/net/anubis/compat/android-picker/";
   private static final String POPUP = "Image selection##AnubisSystemPicker";
   private static final ExecutorService IO = Executors.newSingleThreadExecutor((r) -> {
      Thread t = new Thread(r, "Anubis Android image selection");
      t.setDaemon(true);
      return t;
   });
   private static Request current;
   private static Result completed;
   private static Path completedDirectory;
   private static Path consumedDirectory;
   private static Path runtimeDirectory;
   private static String error;
   private static boolean showError;
   private static boolean dismissError;

   private AndroidSystemPicker() {
   }

   private static native String nativeOpen(String var0, String var1);

   private static native void nativeCancel(String var0);

   public static synchronized void open() {
      if (AnubisAndroidNative.isAndroidRuntime() && current == null && completed == null) {
         cleanConsumed();
         error = null;
         dismissError = true;
         Request request = new Request();
         current = request;
         IO.execute(() -> run(request));
      }
   }

   private static void run(Request request) {
      Path directory = null;
      boolean opened = false;

      Result result;
      try {
         Path runtime = prepareRuntime();
         directory = privateSubdirectory(runtime, "selection-");
         if (request.cancelled) {
            return;
         }

         String failure = nativeOpen(runtime.resolve("classes.dex").toString(), directory.toString());
         if (failure != null) {
            throw new IOException(failure);
         }

         opened = true;
         long deadline = System.nanoTime() + 600000000000L;

         while(true) {
            if (request.cancelled) {
               result = new Result("cancel", (Path)null, (String)null);
               break;
            }

            result = readResult(directory);
            if (!"pending".equals(result.status)) {
               break;
            }

            if (System.nanoTime() >= deadline) {
               throw new IOException("Image selection timed out. Please try again.");
            }

            Thread.sleep(150L);
         }
      } catch (RuntimeException | LinkageError | IOException exception) {
         result = new Result("error", (Path)null, safeMessage(((Throwable)exception).getMessage()));
      } catch (InterruptedException var23) {
         Thread.currentThread().interrupt();
         result = new Result("cancel", (Path)null, (String)null);
      } finally {
         if (opened && directory != null) {
            try {
               nativeCancel(directory.toString());
            } catch (LinkageError | RuntimeException var20) {
            }
         }

         if (request.cancelled) {
            cleanup(directory);
         }

      }

      synchronized(AndroidSystemPicker.class) {
         if (current == request && !request.cancelled) {
            current = null;
            if ("ok".equals(result.status)) {
               completed = result;
               completedDirectory = directory;
            } else {
               cleanup(directory);
               if ("error".equals(result.status)) {
                  error = result.message;
                  showError = true;
                  System.err.println("[Anubis image picker] " + error);
               }
            }

         } else {
            cleanup(directory);
         }
      }
   }

   public static synchronized Path takeSelection() {
      cleanConsumed();
      if (completed == null) {
         return null;
      } else {
         Path path = completed.path;
         consumedDirectory = completedDirectory;
         completed = null;
         completedDirectory = null;
         return path;
      }
   }

   private static void cleanConsumed() {
      Path old = consumedDirectory;
      consumedDirectory = null;
      if (old != null) {
         IO.execute(() -> cleanup(old));
      }

   }

   public static synchronized boolean blocksEditor() {
      return current != null || error != null;
   }

   public static synchronized boolean handleEscape() {
      if (current == null && error == null) {
         return false;
      } else {
         cancel();
         return true;
      }
   }

   public static synchronized void cancel() {
      if (current != null) {
         current.cancelled = true;
      }

      current = null;
      completed = null;
      Path old = completedDirectory;
      completedDirectory = null;
      if (old != null) {
         IO.execute(() -> cleanup(old));
      }

      cleanConsumed();
      error = null;
      showError = false;
      dismissError = true;
   }

   public static synchronized void render() {
      if (showError) {
         ImGui.openPopup("Image selection##AnubisSystemPicker");
         showError = false;
         dismissError = false;
      }

      if (ImGui.isPopupOpen("Image selection##AnubisSystemPicker")) {
         float width = Math.max(200.0F, ImGui.getIO().getDisplaySizeX() - 32.0F);
         ImGui.setNextWindowSize(Math.min(560.0F, width), 0.0F, 1);
         if (ImGui.beginPopupModal("Image selection##AnubisSystemPicker", 320)) {
            try {
               if (!dismissError && error != null) {
                  ImGui.textWrapped(error);
                  if (ImGui.button("OK")) {
                     error = null;
                     ImGui.closeCurrentPopup();
                  }
               } else {
                  ImGui.closeCurrentPopup();
                  dismissError = false;
               }
            } finally {
               ImGui.endPopup();
            }
         }

      }
   }

   static Result readResult(Path directory) throws IOException {
      if (!Files.isDirectory(directory, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
         throw new IOException("Invalid selection directory.");
      } else {
         Path status = directory.resolve("result.txt");
         if (!Files.exists(status, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
            return new Result("pending", (Path)null, (String)null);
         } else if (!Files.isRegularFile(status, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
            throw new IOException("Invalid image selection result.");
         } else {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            InputStream stream = Files.newInputStream(status, LinkOption.NOFOLLOW_LINKS);

            try {
               byte[] buffer = new byte[512];

               int count;
               while((count = stream.read(buffer)) != -1) {
                  if (bytes.size() + count > 4096) {
                     throw new IOException("Image selection result is too large.");
                  }

                  bytes.write(buffer, 0, count);
               }
            } catch (Throwable var11) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (stream != null) {
               stream.close();
            }

            String[] lines = (new String(bytes.toByteArray(), StandardCharsets.UTF_8)).split("\\r?\\n", -1);
            String state = lines.length > 0 ? lines[0] : "";
            String value = lines.length > 1 ? lines[1] : "";

            for(int i = 2; i < lines.length; ++i) {
               if (!lines[i].isEmpty()) {
                  throw new IOException("Invalid image selection result.");
               }
            }

            if ("cancel".equals(state)) {
               return new Result(state, (Path)null, (String)null);
            } else if ("error".equals(state)) {
               return new Result(state, (Path)null, safeMessage(value));
            } else if ("ok".equals(state) && !value.isEmpty() && value.length() <= 240 && value.indexOf(47) < 0 && value.indexOf(92) < 0 && value.indexOf(58) < 0 && !value.equals(".") && !value.equals("..") && !hasControl(value)) {
               String lower = value.toLowerCase(Locale.ROOT);
               if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".gif")) {
                  throw new IOException("Choose a PNG, JPG or GIF image.");
               } else {
                  Path selected = directory.resolve(value);
                  if (Files.isRegularFile(selected, new LinkOption[]{LinkOption.NOFOLLOW_LINKS}) && Files.isReadable(selected)) {
                     long size = Files.size(selected);
                     if (size > 0L && size <= 67108864L) {
                        return new Result("ok", selected, (String)null);
                     } else {
                        throw new IOException("Choose an image smaller than 64 MiB.");
                     }
                  } else {
                     throw new IOException("The selected image could not be read.");
                  }
               }
            } else {
               throw new IOException("Invalid image selection result.");
            }
         }
      }
   }

   private static boolean hasControl(String value) {
      for(int i = 0; i < value.length(); ++i) {
         if (Character.isISOControl(value.charAt(i))) {
            return true;
         }
      }

      return false;
   }

   private static String safeMessage(String text) {
      if (text != null && !text.trim().isEmpty()) {
         StringBuilder result = new StringBuilder();

         for(int i = 0; i < text.length() && result.length() < 350; ++i) {
            char c = text.charAt(i);
            result.append(Character.isISOControl(c) ? ' ' : c);
         }

         return result.toString();
      } else {
         return "Android's image picker could not be opened on this launcher.";
      }
   }

   private static Path prepareRuntime() throws IOException {
      if (runtimeDirectory != null) {
         return runtimeDirectory;
      } else {
         String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
         if (!arch.equals("aarch64") && !arch.equals("arm64") && !arch.equals("arm64-v8a")) {
            throw new IOException("This Android build requires an ARM64 Java runtime.");
         } else {
            List<String> candidates = new ArrayList();
            candidates.add(System.getProperty("imgui.library.path"));
            candidates.add(System.getenv("MOD_ANDROID_RUNTIME"));
            candidates.add(System.getenv("TMPDIR"));
            candidates.add(System.getProperty("java.io.tmpdir"));
            IOException failure = new IOException("The launcher has no writable private cache for Android's image picker.");

            for(String candidate : candidates) {
               if (candidate != null && !candidate.isEmpty()) {
                  Path directory = null;

                  try {
                     Path parent = Paths.get(candidate).toRealPath();
                     String path = parent.toString().replace('\\', '/');
                     if ((path.startsWith("/data/") || path.startsWith("/mnt/expand/")) && !path.startsWith("/data/media/")) {
                        directory = privateSubdirectory(parent, "anubis-system-picker-");
                        extract("classes.dex", directory.resolve("classes.dex"), true);
                        extract("arm64-v8a/libanubis_picker.so", directory.resolve("libanubis_picker.so"), false);
                        System.load(directory.resolve("libanubis_picker.so").toString());
                        runtimeDirectory = directory;
                        return directory;
                     }
                  } catch (SecurityException | IOException exception) {
                     cleanup(directory);
                     failure.addSuppressed(exception);
                  }
               }
            }

            throw failure;
         }
      }
   }

   private static Path privateSubdirectory(Path parent, String prefix) throws IOException {
      return Files.createTempDirectory(parent, prefix, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
   }

   private static void extract(String resource, Path output, boolean dex) throws IOException {
      InputStream input = AndroidSystemPicker.class.getResourceAsStream("/net/anubis/compat/android-picker/" + resource);

      try {
         if (input == null) {
            throw new IOException("Android image picker resource is missing.");
         }

         OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

         try {
            Files.setPosixFilePermissions(output, PosixFilePermissions.fromString(dex ? "r--------" : "r-x------"));
            byte[] buffer = new byte[8192];

            int count;
            while((count = input.read(buffer)) != -1) {
               stream.write(buffer, 0, count);
            }
         } catch (Throwable var9) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (Throwable var10) {
         if (input != null) {
            try {
               input.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (input != null) {
         input.close();
      }

      output.toFile().deleteOnExit();
   }

   private static void cleanup(Path directory) {
      if (directory != null && Files.isDirectory(directory, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
         try {
            DirectoryStream<Path> entries = Files.newDirectoryStream(directory);

            try {
               for(Path entry : entries) {
                  if (!Files.isDirectory(entry, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
                     Files.deleteIfExists(entry);
                  }
               }
            } catch (Throwable var6) {
               if (entries != null) {
                  try {
                     entries.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (entries != null) {
               entries.close();
            }
         } catch (SecurityException | IOException var7) {
         }

         try {
            Files.deleteIfExists(directory);
         } catch (SecurityException | IOException var4) {
         }

      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Request {
      volatile boolean cancelled;
   }

   @Environment(EnvType.CLIENT)
   static final class Result {
      final String status;
      final Path path;
      final String message;

      Result(String status, Path path, String message) {
         this.status = status;
         this.path = path;
         this.message = message;
      }
   }
}
