package net.anubis.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRevision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class ModuleListRows {
   private static final float SNAP = 0.005F;
   private static final Comparator<Row> WIDEST_FIRST = Comparator.comparingDouble((row) -> (double)(-row.nameW)).thenComparing((row) -> row.module.name());
   private static final Comparator<Row> WIDEST_LAST;
   private final List<Row> rows = new ArrayList();
   private final List<Row> view;
   private List<ClientModule> seenEnabled;
   private long seenRevision;
   private boolean seenEditMode;
   private boolean unsorted;
   private boolean sortedBottomFirst;

   ModuleListRows() {
      this.view = Collections.unmodifiableList(this.rows);
      this.seenRevision = Long.MIN_VALUE;
   }

   void update(List<ClientModule> enabled, boolean editMode, float dt) {
      long revision = ModuleRevision.current();
      if (enabled != this.seenEnabled || revision != this.seenRevision || editMode != this.seenEditMode) {
         this.seenEnabled = enabled;
         this.seenRevision = revision;
         this.seenEditMode = editMode;
         this.rows.removeIf((rowx) -> !listed(rowx.module, editMode) || rowx.module.isEnabled() && !enabled.contains(rowx.module));

         for(ClientModule module : enabled) {
            if (listed(module, editMode) && !this.hasRow(module)) {
               this.rows.add(new Row(module));
               this.unsorted = true;
            }
         }
      }

      float k = 1.0F - (float)Math.exp((double)(-12.0F * dt));

      for(int i = this.rows.size() - 1; i >= 0; --i) {
         Row row = (Row)this.rows.get(i);
         float target = row.module.isEnabled() ? 1.0F : 0.0F;
         if (row.anim != target) {
            float next = row.anim + (target - row.anim) * k;
            if (Math.abs(next - target) < 0.005F) {
               next = target;
            }

            row.anim = next;
            if (next == 0.0F) {
               this.rows.remove(i);
            }
         }
      }

   }

   <F> List<Row> ordered(F font, int px, TextWidth<F> width, boolean bottomFirst) {
      for(int i = 0; i < this.rows.size(); ++i) {
         Row row = (Row)this.rows.get(i);
         if (row.measuredPx != px || row.measuredFont != font) {
            row.nameW = width.of(font, px, row.module.name());
            row.measuredFont = font;
            row.measuredPx = px;
            this.unsorted = true;
         }
      }

      if (this.unsorted || bottomFirst != this.sortedBottomFirst) {
         this.rows.sort(bottomFirst ? WIDEST_LAST : WIDEST_FIRST);
         this.unsorted = false;
         this.sortedBottomFirst = bottomFirst;
      }

      return this.view;
   }

   private static boolean listed(ClientModule module, boolean editMode) {
      return module.category() != ModuleCategory.CLIENT && (editMode || module.showInHud() == null || module.showInHud().get());
   }

   private boolean hasRow(ClientModule module) {
      for(int i = 0; i < this.rows.size(); ++i) {
         if (((Row)this.rows.get(i)).module == module) {
            return true;
         }
      }

      return false;
   }

   static {
      WIDEST_LAST = WIDEST_FIRST.reversed();
   }

   @Environment(EnvType.CLIENT)
   static final class Row {
      private final ClientModule module;
      private float anim;
      private float nameW;
      private Object measuredFont;
      private int measuredPx = -1;

      private Row(ClientModule module) {
         this.module = module;
      }

      ClientModule module() {
         return this.module;
      }

      float anim() {
         return this.anim;
      }

      float nameW() {
         return this.nameW;
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface TextWidth<F> {
      float of(F var1, int var2, String var3);
   }
}
