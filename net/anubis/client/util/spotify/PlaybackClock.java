package net.anubis.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class PlaybackClock {
   static final long SNAP_MS = 1200L;
   static final long CATCHUP_MS = 1500L;
   private NowPlaying sample;
   private boolean running;
   private long durationMs;
   private double rate = (double)1.0F;
   private double base;
   private long baseAt;
   private double correction;

   public void update(NowPlaying now, long nowMs) {
      if (now != this.sample) {
         NowPlaying previous = this.sample;
         this.sample = now;
         if (now != null && now.hasTimeline()) {
            double shown = this.position(nowMs);
            double target = (double)now.positionAt(nowMs);
            boolean continuous = previous != null && previous.hasTimeline() && previous.sameTrack(now) && this.running && now.playing() && Math.abs(target - shown) < (double)1200.0F * Math.min((double)1.0F, now.rate());
            this.base = continuous ? shown : target;
            this.correction = continuous ? target - shown : (double)0.0F;
            this.baseAt = nowMs;
            this.running = now.playing();
            this.durationMs = now.durationMs();
            this.rate = now.rate();
         } else {
            this.running = false;
            this.durationMs = 0L;
            this.base = (double)0.0F;
            this.correction = (double)0.0F;
            this.baseAt = nowMs;
         }
      }
   }

   public long positionAt(long nowMs) {
      return this.durationMs <= 0L ? 0L : Math.max(0L, Math.min(this.durationMs, Math.round(this.position(nowMs))));
   }

   private double position(long nowMs) {
      if (this.durationMs <= 0L) {
         return (double)0.0F;
      } else if (!this.running) {
         return this.base;
      } else {
         double elapsed = (double)Math.max(0L, nowMs - this.baseAt);
         return this.base + elapsed * this.rate + this.correction * Math.min((double)1.0F, elapsed / (double)1500.0F);
      }
   }
}
