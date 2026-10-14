package net.anubis.client.gui;

import com.example.client.mixin.AccessorHudCooldownInstance;
import com.example.client.mixin.AccessorHudItemCooldowns;
import imgui.ImDrawList;
import imgui.ImFont;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.anubis.client.config.Surface;
import net.anubis.client.imgui.ImGuiManager;
import net.anubis.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1792;
import net.minecraft.class_1796;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_746;
import net.minecraft.class_7923;

@Environment(EnvType.CLIENT)
final class CooldownTray {
   private static final float PAD = 6.0F;
   private static final float TILE_W = 32.0F;
   private static final float TILE_GAP = 5.0F;
   private static final float DIAL = 26.0F;
   private static final float DIAL_ICON = 16.0F;
   private static final float DIAL_STROKE = 2.0F;
   private static final float BAR_ICON = 18.0F;
   private static final float BAR_H = 3.0F;
   private static final float BAR_GAP = 3.0F;
   private static final float TEXT_GAP = 2.0F;
   private static final float APPEAR_TIME = 0.18F;
   private static final float READY_TIME = 0.32F;
   private static final float VANISH_TIME = 0.22F;
   private static final float LOOKUP_INTERVAL = 0.5F;
   private static final float TICKS_PER_SECOND = 20.0F;
   private static final float TAU = ((float)Math.PI * 2F);
   private static final float TOP = (-(float)Math.PI / 2F);
   private static final String[] TENTHS = new String[100];
   private static final String[] SECONDS = new String[100];
   private static final String[] MINUTES = new String[100];
   private final List<Slot> slots = new ArrayList();
   private final List<Slot> pool = new ArrayList();
   private final Slot[] samples = new Slot[]{new Slot(), new Slot()};
   private final List<Slot> sampleView;
   private class_746 owner;
   private boolean sampling;
   private boolean sandbar;
   private float clock;
   private float tickRate;

   CooldownTray() {
      this.sampleView = List.of(this.samples);
      this.tickRate = 20.0F;
      this.samples[0].item = class_1802.field_8634;
      this.samples[0].total = 20.0F;
      this.samples[1].item = class_1802.field_49098;
      this.samples[1].total = 10.0F;

      for(Slot sample : this.samples) {
         sample.presence = 1.0F;
      }

   }

   void reset() {
      for(Slot slot : this.slots) {
         slot.group = null;
         slot.item = null;
         this.pool.add(slot);
      }

      this.slots.clear();
      this.owner = null;
   }

   boolean visible() {
      return this.sampling || !this.slots.isEmpty();
   }

   void update(class_310 mc, HudModule hud, boolean active, boolean editMode, float dt) {
      this.clock += dt;
      this.sandbar = hud.cooldownSandbar();
      float rate = mc.field_1687 == null ? 20.0F : mc.field_1687.method_54719().method_54748();
      this.tickRate = rate > 0.0F ? rate : 20.0F;
      class_746 player = mc.field_1724;
      if (player != this.owner) {
         this.reset();
         this.owner = player;
      }

      for(Slot slot : this.slots) {
         slot.live = false;
      }

      if (active && player != null) {
         this.collect(mc, player);
      }

      for(int i = this.slots.size() - 1; i >= 0; --i) {
         Slot slot = (Slot)this.slots.get(i);
         if (slot.live) {
            slot.presence = Math.min(1.0F, slot.presence + dt / 0.18F);
            if (slot.item == null && this.clock >= slot.lookupAt && player != null) {
               slot.item = resolve(player, slot.group);
               slot.lookupAt = this.clock + 0.5F;
            }
         } else {
            if (!slot.done) {
               slot.done = true;
               slot.remaining = 0.0F;
               slot.ready = active ? 1.0F : 0.0F;
            }

            if (slot.ready > 0.0F) {
               slot.ready = Math.max(0.0F, slot.ready - dt / 0.32F);
            } else {
               slot.presence -= dt / 0.22F;
               if (slot.presence <= 0.0F) {
                  this.slots.remove(i);
                  slot.group = null;
                  slot.item = null;
                  this.pool.add(slot);
               }
            }
         }
      }

      this.sampling = editMode && this.slots.isEmpty();
      if (this.sampling) {
         for(int i = 0; i < this.samples.length; ++i) {
            float phase = (this.clock * 0.45F + (float)i * 0.43F) % 1.0F;
            this.samples[i].remaining = (1.0F - phase) * this.samples[i].total;
         }
      }

   }

   private void collect(class_310 mc, class_746 player) {
      class_1796 cooldowns = player.method_7357();
      AccessorHudItemCooldowns access = (AccessorHudItemCooldowns)cooldowns;
      int tick = access.anubis$hudTickCount();
      float partial = mc.method_61966().method_60637(true);

      for(Map.Entry<class_2960, ?> entry : access.anubis$hudCooldowns().entrySet()) {
         AccessorHudCooldownInstance span = (AccessorHudCooldownInstance)entry.getValue();
         int start = span.anubis$hudStartTime();
         int end = span.anubis$hudEndTime();
         float left = (float)end - ((float)tick + partial);
         if (!(left <= 0.0F)) {
            Slot slot = this.find((class_2960)entry.getKey());
            if (slot == null) {
               slot = this.open((class_2960)entry.getKey());
            }

            slot.live = true;
            slot.done = false;
            slot.ready = 0.0F;
            slot.remaining = left;
            slot.total = Math.max(1.0F, (float)(end - start));
         }
      }

   }

   private Slot find(class_2960 group) {
      for(Slot slot : this.slots) {
         if (slot.group.equals(group)) {
            return slot;
         }
      }

      return null;
   }

   private Slot open(class_2960 group) {
      Slot slot = this.pool.isEmpty() ? new Slot() : (Slot)this.pool.remove(this.pool.size() - 1);
      slot.group = group;
      slot.item = null;
      slot.lookupAt = 0.0F;
      slot.presence = 0.0F;
      slot.ready = 0.0F;
      slot.done = false;
      this.slots.add(slot);
      return slot;
   }

   private static class_1792 resolve(class_746 player, class_2960 group) {
      class_1661 inventory = player.method_31548();
      class_1796 cooldowns = player.method_7357();

      for(int i = 0; i < inventory.method_5439(); ++i) {
         class_1799 stack = inventory.method_5438(i);
         if (!stack.method_7960() && group.equals(cooldowns.method_62836(stack))) {
            return stack.method_7909();
         }
      }

      if (class_7923.field_41178.method_10250(group)) {
         return (class_1792)class_7923.field_41178.method_63535(group);
      } else {
         return null;
      }
   }

   float planWidth(float s) {
      if (!this.visible()) {
         return 1.0F;
      } else {
         float spread = 0.0F;

         for(Slot slot : this.sampling ? this.sampleView : this.slots) {
            spread += HudGlyphs.easeOutQuint(slot.presence) * 37.0F * s;
         }

         return Math.max(1.0F, (float)Math.round(6.0F * s * 2.0F + Math.max(0.0F, spread - 5.0F * s)));
      }
   }

   float planHeight(float s) {
      float body = this.sandbar ? ItemIcons.pixels(18.0F * s) + 3.0F * s + barHeight(s) : (float)Math.round(26.0F * s);
      return (float)Math.round(6.0F * s * 2.0F + body + 2.0F * s + (float)HudChrome.px(14.0F, s));
   }

   private static float barHeight(float s) {
      return Math.max(2.0F, (float)Math.round(3.0F * s));
   }

   void draw(ImDrawList dl, HudLayoutStore.HudElementState state, float x, float y, float alpha, boolean shadow, int accent) {
      if (!(alpha <= 0.004F) && this.visible()) {
         List<Slot> shown = this.sampling ? this.sampleView : this.slots;
         float total = 0.0F;

         for(Slot slot : shown) {
            total += slot.presence;
         }

         float cardAlpha = alpha * HudGlyphs.easeOutQuint(Math.min(1.0F, total));
         if (!(cardAlpha <= 0.004F)) {
            float s = HudRenderer.scaleOf(state);
            float w = state.width;
            float h = state.height;
            Surface surface = HudRenderer.hudSurface(state);
            boolean guard = HudChrome.needsTextGuard(surface);
            HudChrome.card(dl, surface, x, y, w, h, HudRenderer.hudRounding() * s, cardAlpha, shadow, s);
            ImFont label = ImGuiManager.getInstance().hudLabelFont();
            int labelPx = HudChrome.px(14.0F, s);
            float pad = (float)Math.round(6.0F * s);
            float tile = 32.0F * s;
            float dial = this.sandbar ? ItemIcons.pixels(18.0F * s) + 3.0F * s + barHeight(s) : (float)Math.round(26.0F * s);
            float textTop = (float)Math.round(y + pad + dial + 2.0F * s);
            float bleed = 10.0F * s;
            dl.pushClipRect(x, y - bleed, x + w, y + h + bleed, true);
            float cx = x + pad;

            for(Slot slot : shown) {
               float open = HudGlyphs.easeOutQuint(slot.presence);
               float a = alpha * open;
               if (a > 0.004F) {
                  if (this.sandbar) {
                     this.drawSandbar(dl, slot, (float)Math.round(cx), y + pad, tile, s, a, accent);
                  } else {
                     this.drawHalo(dl, slot, cx + tile * 0.5F, y + pad + (float)Math.round(26.0F * s) * 0.5F, s, a, accent);
                  }

                  this.drawCaption(dl, slot, cx + tile * 0.5F, textTop, label, labelPx, s, a, guard);
               }

               cx += open * (tile + 5.0F * s);
            }

            dl.popClipRect();
         }
      }
   }

   private void drawHalo(ImDrawList dl, Slot slot, float cx, float cy, float s, float a, int accent) {
      float r = (float)Math.round(26.0F * s) * 0.5F - 2.0F * s * 0.5F;
      float stroke = Math.max(1.5F, 2.0F * s);
      float left = fraction(slot);
      dl.addCircle(cx, cy, r, HudChrome.scaleAlpha(452984831, a), 0, stroke);
      if (left > 0.0F) {
         int tone = EgyptDraw.mix(accent, GuiPalette.abgr(-871878), 1.0F - left);
         HudGlyphs.arc(dl, cx, cy, r, (-(float)Math.PI / 2F), ((float)Math.PI * 2F) * left, stroke, HudChrome.scaleAlpha(tone, a));
      }

      int icon = (int)ItemIcons.pixels(16.0F * s);
      drawItem(dl, slot, cx - (float)icon * 0.5F, cy - (float)icon * 0.5F, icon, s, a * (0.62F + 0.38F * (1.0F - left)));
      if (slot.ready > 0.0F) {
         float grow = (1.0F - slot.ready) * 7.0F * s;
         dl.addCircle(cx, cy, r + grow, HudGlyphs.ink(-871878, a * slot.ready * 0.9F), 0, stroke);
      }

   }

   private void drawSandbar(ImDrawList dl, Slot slot, float tx, float ty, float tile, float s, float a, int accent) {
      int icon = (int)ItemIcons.pixels(18.0F * s);
      float left = fraction(slot);
      float ix = (float)Math.round(tx + (tile - (float)icon) * 0.5F);
      drawItem(dl, slot, ix, ty, icon, s, a * (0.62F + 0.38F * (1.0F - left)));
      float by = ty + (float)icon + 3.0F * s;
      float bh = barHeight(s);
      float inset = (float)Math.round(3.0F * s);
      float bl = tx + inset;
      float br = tx + tile - inset;
      dl.addRectFilled(bl, by, br, by + bh, HudChrome.scaleAlpha(452984831, a), bh * 0.5F);
      if (left > 0.0F) {
         int tone = EgyptDraw.mix(accent, GuiPalette.abgr(-871878), 1.0F - left);
         float fill = (br - bl) * left;
         dl.addRectFilled(bl, by, bl + fill, by + bh, HudChrome.scaleAlpha(tone, a), Math.min(bh * 0.5F, fill * 0.5F));
      }

      if (slot.ready > 0.0F) {
         float glow = slot.ready;
         dl.addRectFilled(bl, by, br, by + bh, HudGlyphs.ink(-11654, a * glow), bh * 0.5F);
      }

   }

   private static void drawItem(ImDrawList dl, Slot slot, float x, float y, int icon, float s, float a) {
      if (slot.item != null) {
         ItemIcons.draw(dl, slot.item, x, y, (float)icon, a);
      } else {
         HudGlyphs.hourglass(dl, x + (float)icon * 0.5F, y + (float)icon * 0.5F, (float)icon * 0.8F, Math.max(1.0F, 1.3F * s), HudGlyphs.ink(-2513573, a));
      }
   }

   private void drawCaption(ImDrawList dl, Slot slot, float centerX, float top, ImFont label, int labelPx, float s, float a, boolean guard) {
      if (slot.done) {
         float h = (float)labelPx * 0.85F;
         HudGlyphs.ankh(dl, centerX, top + (float)labelPx * 0.5F, h, Math.max(1.0F, 1.2F * s), HudGlyphs.ink(-871878, a * Math.max(slot.ready, 0.4F)));
      } else {
         String value = seconds(slot.remaining / this.tickRate);
         float vw = HudChrome.tabularWidth(label, labelPx, value);
         HudChrome.tabular(dl, label, labelPx, (float)Math.round(centerX - vw * 0.5F), top, HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, a), value, guard);
      }
   }

   private static float fraction(Slot slot) {
      return slot.total <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, slot.remaining / slot.total));
   }

   private static String seconds(float seconds) {
      if (seconds < 9.95F) {
         int tenths = Math.max(1, Math.min(99, (int)Math.ceil((double)(seconds * 10.0F))));
         String cached = TENTHS[tenths];
         if (cached == null) {
            cached = tenths / 10 + "." + tenths % 10 + "s";
            TENTHS[tenths] = cached;
         }

         return cached;
      } else {
         int whole = (int)Math.ceil((double)seconds);
         if (whole < 100) {
            String cached = SECONDS[whole];
            if (cached == null) {
               cached = whole + "s";
               SECONDS[whole] = cached;
            }

            return cached;
         } else {
            int minutes = Math.min(99, (int)Math.ceil((double)(seconds / 60.0F)));
            String cached = MINUTES[minutes];
            if (cached == null) {
               cached = minutes + "m";
               MINUTES[minutes] = cached;
            }

            return cached;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Slot {
      class_2960 group;
      class_1792 item;
      float lookupAt;
      float remaining;
      float total;
      float presence;
      float ready;
      boolean live;
      boolean done;
   }
}
