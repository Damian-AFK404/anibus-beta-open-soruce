package net.anubis.client.module.impl.client;

import com.sun.jna.Platform;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.Stage;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.input.AnubisKeyPolling;
import net.anubis.client.input.ModuleKeybinds;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.module.setting.BooleanSetting;
import net.anubis.client.module.setting.ColorSetting;
import net.anubis.client.module.setting.KeySetting;
import net.anubis.client.module.setting.ModeSetting;
import net.anubis.client.module.setting.NumberSetting;
import net.anubis.client.util.spotify.AlbumArt;
import net.anubis.client.util.spotify.Lyrics;
import net.anubis.client.util.spotify.LyricsQuery;
import net.anubis.client.util.spotify.LyricsService;
import net.anubis.client.util.spotify.MediaSession;
import net.anubis.client.util.spotify.NowPlaying;
import net.anubis.client.util.spotify.SpotifyWindow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class SpotifyHudModule extends ClientModule {
   private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
   private static final long POLL_INTERVAL_MS = 500L;
   private static final long WINDOW_POLL_MS = 1000L;
   private static final long REPOLL_AFTER_KEY_MS = 250L;
   private static final long KEY_COOLDOWN_MS = 200L;
   private static final int ART_MAX_SIDE = 256;
   private static final int STABLE_POLLS = 2;
   private static final int SESSION_HICCUPS = 2;
   private static final long ERROR_LOG_INTERVAL_MS = 60000L;
   private final BooleanSetting showArtist = (BooleanSetting)this.addSetting("show-artist", new BooleanSetting("Show Artist", true));
   private final BooleanSetting showAlbumArt = (BooleanSetting)this.addSetting("show-album-art", new BooleanSetting("Show Album Art", true));
   private final BooleanSetting showProgress = (BooleanSetting)this.addSetting("show-progress", new BooleanSetting("Show Progress", true));
   private final BooleanSetting scrollTitles = (BooleanSetting)this.addSetting("scroll-titles", new BooleanSetting("Scroll Long Titles", true));
   private final BooleanSetting showLyrics = (BooleanSetting)this.addSetting("show-lyrics", new BooleanSetting("Show Lyrics", true));
   private final ModeSetting lyricLines = (ModeSetting)this.addSetting("lyric-lines", new ModeSetting("Lyric Lines", "2", new String[]{"1", "2", "3"}));
   private final ModeSetting lyricMotion = (ModeSetting)this.addSetting("lyric-motion", new ModeSetting("Lyric Motion", "Wave", new String[]{"Wave", "Glide", "Fade"}));
   private final ModeSetting lyricHighlight = (ModeSetting)this.addSetting("lyric-highlight", new ModeSetting("Lyric Highlight", "Karaoke", new String[]{"Karaoke", "Line", "Off"}));
   private final BooleanSetting lyricsOnline = (BooleanSetting)this.addSetting("lyrics-online", new BooleanSetting("Lyrics Online", true));
   private final NumberSetting cardWidth = (NumberSetting)this.addSetting("card-width", (new NumberSetting("Card Width", 320.0F, 200.0F, 560.0F)).step(1.0F));
   private final ColorSetting titleColor = (ColorSetting)this.addSetting("title-color", new ColorSetting("Title Color", -1447442));
   private final ColorSetting artistColor = (ColorSetting)this.addSetting("artist-color", new ColorSetting("Artist Color", -6052949));
   private final ColorSetting lyricsColor = (ColorSetting)this.addSetting("lyrics-color", new ColorSetting("Lyrics Color", -657929));
   private final ColorSetting upcomingLyricsColor = (ColorSetting)this.addSetting("upcoming-lyrics-color", new ColorSetting("Upcoming Lyrics Color", -6052949));
   private final ColorSetting highlightColor = (ColorSetting)this.addSetting("highlight-color", new ColorSetting("Highlight Color", -14756000));
   private final BooleanSetting showWhenIdle = (BooleanSetting)this.addSetting("show-when-idle", new BooleanSetting("Show When Idle", false));
   private final BooleanSetting mediaKeys = (BooleanSetting)this.addSetting("media-keys", new BooleanSetting("Media Keys", false));
   private final KeySetting playPauseKey = (KeySetting)this.addSetting("play-pause-key", new KeySetting("Play / Pause", -1));
   private final KeySetting nextKey = (KeySetting)this.addSetting("next-key", new KeySetting("Next Track", -1));
   private final KeySetting previousKey = (KeySetting)this.addSetting("previous-key", new KeySetting("Previous Track", -1));
   private volatile NowPlaying nowPlaying;
   private volatile TrackLyrics lyrics;
   private volatile Poller poller;
   private final Object publishLock;
   private final KeySetting[] keys;
   private final SpotifyWindow.MediaKey[] actions;
   private final boolean[] wasDown;
   private long lastKeyAt;

   public SpotifyHudModule() {
      super("spotify-hud", "Spotify HUD", "Shows what Spotify is playing, with synced lyrics and optional media keys.", ModuleCategory.CLIENT);
      this.nowPlaying = NowPlaying.WAITING;
      this.publishLock = new Object();
      this.keys = new KeySetting[]{this.playPauseKey, this.nextKey, this.previousKey};
      this.actions = new SpotifyWindow.MediaKey[]{SpotifyWindow.MediaKey.PLAY_PAUSE, SpotifyWindow.MediaKey.NEXT, SpotifyWindow.MediaKey.PREVIOUS};
      this.wasDown = new boolean[this.keys.length];
      this.usesNotifications();
      ColorSetting var10000 = this.artistColor;
      BooleanSetting var10001 = this.showArtist;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.lyricsColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Line"));
      var10000 = this.upcomingLyricsColor;
      var10001 = this.showLyrics;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::get);
      this.highlightColor.visibleWhen(() -> this.showLyrics.get() && !this.lyricHighlight.is("Off"));
      ModeSetting var2 = this.lyricLines;
      var10001 = this.showLyrics;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      var2 = this.lyricMotion;
      var10001 = this.showLyrics;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      var2 = this.lyricHighlight;
      var10001 = this.showLyrics;
      Objects.requireNonNull(var10001);
      var2.visibleWhen(var10001::get);
      BooleanSetting var5 = this.lyricsOnline;
      var10001 = this.showLyrics;
      Objects.requireNonNull(var10001);
      var5.visibleWhen(var10001::get);
      this.lyricsOnline.onChange(() -> {
         Poller running = this.poller;
         if (running != null) {
            running.lyricsService.setOnline(this.lyricsOnline.get());
         }

      });
      KeySetting var6 = this.playPauseKey;
      var10001 = this.mediaKeys;
      Objects.requireNonNull(var10001);
      var6.visibleWhen(var10001::get);
      var6 = this.nextKey;
      var10001 = this.mediaKeys;
      Objects.requireNonNull(var10001);
      var6.visibleWhen(var10001::get);
      var6 = this.previousKey;
      var10001 = this.mediaKeys;
      Objects.requireNonNull(var10001);
      var6.visibleWhen(var10001::get);
   }

   protected void onEnable() {
      Arrays.fill(this.wasDown, true);
      if (!Platform.isWindows()) {
         synchronized(this.publishLock) {
            this.nowPlaying = NowPlaying.UNSUPPORTED;
            this.lyrics = null;
         }

         ModuleChat.send(this, (ModuleChat.Body)ModuleChat.body().danger("Not supported").text(", only the Spotify app on Windows works"));
      } else {
         Poller started = new Poller();
         synchronized(this.publishLock) {
            this.poller = started;
            this.nowPlaying = NowPlaying.WAITING;
            this.lyrics = null;
         }

         started.start();
         this.on(TickEvent.TYPE, Stage.POST, this::onTick);
      }
   }

   protected void onDisable() {
      Poller stopped;
      synchronized(this.publishLock) {
         stopped = this.poller;
         this.poller = null;
         this.nowPlaying = Platform.isWindows() ? NowPlaying.WAITING : NowPlaying.UNSUPPORTED;
         this.lyrics = null;
      }

      if (stopped != null) {
         stopped.stop();
      }

   }

   private void publish(Poller from, NowPlaying state, TrackLyrics trackLyrics) {
      synchronized(this.publishLock) {
         if (from == this.poller) {
            this.nowPlaying = state;
            this.lyrics = trackLyrics;
         }
      }
   }

   public static SpotifyHudModule get() {
      return (SpotifyHudModule)ModuleRegistry.get(SpotifyHudModule.class);
   }

   public static SpotifyHudModule active() {
      SpotifyHudModule module = get();
      return module != null && module.isEnabled() ? module : null;
   }

   public static void hudButton(SpotifyWindow.MediaKey key) {
      SpotifyHudModule module = active();
      if (module != null) {
         module.press(key);
      }

   }

   public static boolean shouldShowHud() {
      SpotifyHudModule module = active();
      if (module == null) {
         return false;
      } else {
         NowPlaying now = module.nowPlaying;
         if (now.status() != NowPlaying.Status.UNSUPPORTED && now.status() != NowPlaying.Status.WAITING) {
            return now.active() || module.showWhenIdle.get();
         } else {
            return false;
         }
      }
   }

   public NowPlaying nowPlaying() {
      return this.nowPlaying;
   }

   public Lyrics lyricsFor(NowPlaying now) {
      TrackLyrics current = this.lyrics;
      Poller running = this.poller;
      if (current != null && running != null && this.wantsLyrics(now)) {
         return current.title().equals(now.title()) && current.artist().equals(now.artist()) ? running.lyricsService.lookup(current.query()) : null;
      } else {
         return null;
      }
   }

   public boolean wantsLyrics(NowPlaying now) {
      return this.showLyrics.get() && now != null && now.lyricsEligible();
   }

   public boolean showArtist() {
      return this.showArtist.get();
   }

   public boolean showAlbumArt() {
      return this.showAlbumArt.get();
   }

   public boolean showProgress() {
      return this.showProgress.get();
   }

   public boolean scrollTitles() {
      return this.scrollTitles.get();
   }

   public boolean showLyrics() {
      return this.showLyrics.get();
   }

   public int lyricLines() {
      try {
         return Math.max(1, Math.min(3, Integer.parseInt(this.lyricLines.get())));
      } catch (NumberFormatException var2) {
         return 2;
      }
   }

   public LyricMotion lyricMotion() {
      LyricMotion var10000;
      switch (this.lyricMotion.get()) {
         case "Glide" -> var10000 = SpotifyHudModule.LyricMotion.GLIDE;
         case "Fade" -> var10000 = SpotifyHudModule.LyricMotion.FADE;
         default -> var10000 = SpotifyHudModule.LyricMotion.WAVE;
      }

      return var10000;
   }

   public LyricHighlight lyricHighlight() {
      LyricHighlight var10000;
      switch (this.lyricHighlight.get()) {
         case "Line" -> var10000 = SpotifyHudModule.LyricHighlight.LINE;
         case "Off" -> var10000 = SpotifyHudModule.LyricHighlight.OFF;
         default -> var10000 = SpotifyHudModule.LyricHighlight.KARAOKE;
      }

      return var10000;
   }

   public boolean lyricsOnline() {
      return this.lyricsOnline.get();
   }

   public float cardWidth() {
      return this.cardWidth.get();
   }

   public int titleColor() {
      return this.titleColor.argb();
   }

   public int artistColor() {
      return this.artistColor.argb();
   }

   public int lyricsColor() {
      return this.lyricsColor.argb();
   }

   public int upcomingLyricsColor() {
      return this.upcomingLyricsColor.argb();
   }

   public int highlightColor() {
      return this.highlightColor.argb();
   }

   private void onTick(TickEvent event) {
      class_310 mc = class_310.method_1551();
      boolean accept = this.mediaKeys.get() && mc.field_1755 == null && mc.method_1569() && mc.field_1724 != null;

      for(int i = 0; i < this.keys.length; ++i) {
         boolean down = isDown(mc, this.keys[i].value());
         if (down && !this.wasDown[i] && accept) {
            this.press(this.actions[i]);
         }

         this.wasDown[i] = down;
      }

   }

   private void press(SpotifyWindow.MediaKey key) {
      long now = System.currentTimeMillis();
      Poller running = this.poller;
      if (running != null && now - this.lastKeyAt >= 200L) {
         this.lastKeyAt = now;
         if (running.press(key)) {
            if (key == SpotifyWindow.MediaKey.PLAY_PAUSE) {
               synchronized(this.publishLock) {
                  if (this.poller == running) {
                     this.nowPlaying = this.nowPlaying.toggled();
                  }
               }
            }

         }
      }
   }

   private static boolean isDown(class_310 mc, int key) {
      if (!ModuleKeybinds.isBindable(key)) {
         return false;
      } else {
         return key >= 1000 ? GLFW.glfwGetMouseButton(mc.method_22683().method_4490(), key - 1000) == 1 : AnubisKeyPolling.isKeyDown(mc.method_22683(), key);
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum LyricMotion {
      WAVE,
      GLIDE,
      FADE;

      // $FF: synthetic method
      private static LyricMotion[] $values() {
         return new LyricMotion[]{WAVE, GLIDE, FADE};
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum LyricHighlight {
      KARAOKE,
      LINE,
      OFF;

      // $FF: synthetic method
      private static LyricHighlight[] $values() {
         return new LyricHighlight[]{KARAOKE, LINE, OFF};
      }
   }

   @Environment(EnvType.CLIENT)
   private static record TrackLyrics(String title, String artist, LyricsQuery query) {
   }

   @Environment(EnvType.CLIENT)
   private final class Poller {
      final ScheduledThreadPoolExecutor executor;
      final LyricsService lyricsService = new LyricsService();
      private MediaSession media;
      private boolean mediaFailed;
      private SpotifyWindow window;
      private boolean windowFailed;
      private long nextWindowReadAt;
      private AlbumArt art;
      private int artVersion;
      private boolean artDecoded;
      private LyricsQuery lyricsCandidate;
      private int lyricsCandidatePolls;
      private LyricsQuery lyricsQuery;
      private TrackLyrics published;
      private int sessionFailures;
      private String lastError;
      private long lastErrorLoggedAt;

      Poller() {
         this.lyricsService.setOnline(SpotifyHudModule.this.lyricsOnline.get());
         this.executor = new ScheduledThreadPoolExecutor(1, (runnable) -> {
            Thread thread = new Thread(runnable, "Anubis-Spotify");
            thread.setDaemon(true);
            return thread;
         });
         this.executor.setRemoveOnCancelPolicy(true);
         this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      }

      void start() {
         this.executor.scheduleWithFixedDelay(this::poll, 0L, 500L, TimeUnit.MILLISECONDS);
      }

      void stop() {
         try {
            this.executor.execute(this::closeMedia);
         } catch (RejectedExecutionException var2) {
         }

         this.executor.shutdown();
         this.lyricsService.close();
      }

      private void closeMedia() {
         if (this.media != null) {
            this.media.close();
         }

      }

      boolean press(SpotifyWindow.MediaKey key) {
         try {
            this.executor.execute(() -> this.control(key));
            this.executor.schedule(this::poll, 250L, TimeUnit.MILLISECONDS);
            return true;
         } catch (RejectedExecutionException var3) {
            return false;
         }
      }

      private void poll() {
         if (SpotifyHudModule.this.poller == this) {
            try {
               NowPlaying next = this.readSession();
               if (next == null) {
                  if (this.sessionFailures > 0 && this.sessionFailures <= 2 && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
                     return;
                  }

                  long now = System.currentTimeMillis();
                  if (SpotifyHudModule.this.nowPlaying.source() != NowPlaying.Source.SESSION && now < this.nextWindowReadAt) {
                     return;
                  }

                  this.nextWindowReadAt = now + 1000L;
                  next = this.readWindow();
               }

               if (next != null) {
                  SpotifyHudModule.this.publish(this, next, this.lyricsFor(next));
               }
            } catch (LinkageError | RuntimeException failure) {
               SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD poll failed", failure);
            }

         }
      }

      private NowPlaying readSession() {
         if (this.mediaFailed) {
            return null;
         } else {
            if (this.media == null) {
               try {
                  this.media = MediaSession.spotify();
               } catch (LinkageError failure) {
                  SpotifyHudModule.LOGGER.warn("[Anubis] Spotify HUD: media session unavailable ({})", failure.toString());
                  this.mediaFailed = true;
                  return null;
               }
            }

            if (this.media.isUnsupported()) {
               return null;
            } else {
               MediaSession.Snapshot snapshot = this.media.poll();
               if (snapshot != null) {
                  this.sessionFailures = 0;
                  return NowPlaying.fromSession(snapshot, this.artFor(snapshot), SpotifyHudModule.this.nowPlaying);
               } else {
                  String error = this.media.lastError();
                  this.logError(error);
                  this.sessionFailures = error != null && !this.media.isUnsupported() ? this.sessionFailures + 1 : 0;
                  return null;
               }
            }
         }
      }

      private AlbumArt artFor(MediaSession.Snapshot snapshot) {
         if (!SpotifyHudModule.this.showAlbumArt.get()) {
            this.art = null;
            this.artDecoded = false;
            return null;
         } else {
            if (!this.artDecoded || snapshot.thumbnailVersion() != this.artVersion) {
               this.artDecoded = true;
               this.artVersion = snapshot.thumbnailVersion();
               this.art = null;
               if (snapshot.hasThumbnail()) {
                  try {
                     this.art = AlbumArt.decode(snapshot.thumbnail(), 256);
                  } catch (RuntimeException | LinkageError failure) {
                     SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD: album art could not be decoded", failure);
                  }
               }
            }

            return this.art;
         }
      }

      private NowPlaying readWindow() {
         if (!this.windowFailed && this.window == null) {
            try {
               this.window = new SpotifyWindow();
            } catch (LinkageError failure) {
               this.windowFailed(failure);
            }
         }

         if (this.window != null) {
            try {
               return NowPlaying.fromWindowTitle(this.window.readTitle(), SpotifyHudModule.this.nowPlaying);
            } catch (LinkageError failure) {
               this.windowFailed(failure);
            }
         }

         if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
            return NowPlaying.NOT_RUNNING;
         } else {
            this.executor.shutdown();
            return NowPlaying.UNSUPPORTED;
         }
      }

      private void windowFailed(LinkageError failure) {
         SpotifyHudModule.LOGGER.warn("[Anubis] Spotify HUD: Win32 access unavailable ({})", failure.toString());
         this.window = null;
         this.windowFailed = true;
      }

      private TrackLyrics lyricsFor(NowPlaying now) {
         if (!SpotifyHudModule.this.wantsLyrics(now)) {
            this.lyricsCandidate = null;
            this.lyricsCandidatePolls = 0;
            this.lyricsQuery = null;
            this.published = null;
            return null;
         } else {
            LyricsQuery query = (new LyricsQuery(now.title(), now.artist(), now.album(), 0)).withDurationMillis(now.durationMs());
            if (query.equals(this.lyricsCandidate)) {
               ++this.lyricsCandidatePolls;
            } else {
               this.lyricsCandidate = query;
               this.lyricsCandidatePolls = 1;
            }

            if (this.lyricsCandidatePolls >= 2) {
               this.lyricsQuery = this.lyricsCandidate;
            }

            if (this.lyricsQuery != null && this.lyricsQuery.title().equals(now.title()) && this.lyricsQuery.artist().equals(now.artist())) {
               if (this.published == null || !this.published.query().equals(this.lyricsQuery) || !this.published.title().equals(now.title()) || !this.published.artist().equals(now.artist())) {
                  this.published = new TrackLyrics(now.title(), now.artist(), this.lyricsQuery);
               }

               return this.published;
            } else {
               return this.published;
            }
         }
      }

      private void control(SpotifyWindow.MediaKey key) {
         if (SpotifyHudModule.this.poller == this) {
            if (this.media != null && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
               boolean var10000;
               switch (key) {
                  case PLAY_PAUSE -> var10000 = this.media.togglePlayPause();
                  case NEXT -> var10000 = this.media.skipNext();
                  case PREVIOUS -> var10000 = this.media.skipPrevious();
                  default -> throw new MatchException((String)null, (Throwable)null);
               }

               boolean accepted = var10000;
               if (accepted || this.media.lastError() == null) {
                  return;
               }

               SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD: media session control failed ({}); sending the media key", this.media.lastError());
            }

            try {
               SpotifyWindow.tap(key);
            } catch (RuntimeException | LinkageError failure) {
               SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD media key failed", failure);
            }

         }
      }

      private void logError(String error) {
         if (error != null) {
            long now = System.currentTimeMillis();
            if (!Objects.equals(error, this.lastError) || now - this.lastErrorLoggedAt >= 60000L) {
               this.lastError = error;
               this.lastErrorLoggedAt = now;
               SpotifyHudModule.LOGGER.debug("[Anubis] Spotify HUD media session: {}", error);
            }
         }
      }
   }
}
