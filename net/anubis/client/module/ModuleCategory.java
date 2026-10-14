package net.anubis.client.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleCategory {
   private static final List<ModuleCategory> REGISTERED = new ArrayList();
   private static volatile List<ModuleCategory> values = List.of();
   public static final ModuleCategory COMBAT;
   public static final ModuleCategory MISC;
   public static final ModuleCategory DONUT;
   public static final ModuleCategory RENDER;
   public static final ModuleCategory CLIENT;
   public static final ModuleCategory MOVEMENT;
   public static final ModuleCategory BASE_RAID;
   public static final ModuleCategory VISUALS;
   public static final ModuleCategory DONUT_MACROS;
   private final String id;
   private final String displayName;
   private final Icon icon;

   private ModuleCategory(String id, String displayName, Icon icon) {
      this.id = id;
      this.displayName = displayName;
      this.icon = icon;
   }

   public static synchronized ModuleCategory register(String id, String displayName, Icon icon) {
      if (!isValidId(id)) {
         throw new IllegalArgumentException("Category id must be upper-case letters, digits and '_': " + id);
      } else {
         for(ModuleCategory category : REGISTERED) {
            if (category.id.equals(id)) {
               throw new IllegalArgumentException("Duplicate category id: " + id);
            }
         }

         ModuleCategory category = new ModuleCategory(id, (String)Objects.requireNonNull(displayName, "displayName"), (Icon)Objects.requireNonNull(icon, "icon"));
         REGISTERED.add(category);
         values = List.copyOf(REGISTERED);
         return category;
      }
   }

   private static boolean isValidId(String id) {
      if (id != null && !id.isEmpty() && id.charAt(0) >= 'A' && id.charAt(0) <= 'Z') {
         for(int i = 1; i < id.length(); ++i) {
            char c = id.charAt(i);
            if ((c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_') {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static ModuleCategory register(String id, String displayName) {
      return register(id, displayName, ModuleCategory.Icon.ANKH);
   }

   public static List<ModuleCategory> values() {
      return values;
   }

   public String name() {
      return this.id;
   }

   public String displayName() {
      return this.displayName;
   }

   public Icon icon() {
      return this.icon;
   }

   public String toString() {
      return this.id;
   }

   static {
      COMBAT = register("COMBAT", "Combat", ModuleCategory.Icon.CROSSHAIR);
      MISC = register("MISC", "Misc", ModuleCategory.Icon.GRID);
      DONUT = register("DONUT", "Donut", ModuleCategory.Icon.RING);
      RENDER = register("RENDER", "Render", ModuleCategory.Icon.EYE);
      CLIENT = register("CLIENT", "Anubis Client", ModuleCategory.Icon.ANKH);
      MOVEMENT = register("MOVEMENT", "Movement", ModuleCategory.Icon.MOVE);
      BASE_RAID = register("BASE_RAID", "Base Finding", ModuleCategory.Icon.HOUSE);
      VISUALS = register("VISUALS", "Visuals", ModuleCategory.Icon.MONITOR);
      DONUT_MACROS = register("DONUT_MACROS", "Macro", ModuleCategory.Icon.PAPYRUS);
   }

   @Environment(EnvType.CLIENT)
   public static enum Icon {
      CROSSHAIR,
      CHEVRON,
      GRID,
      RING,
      EYE,
      MONITOR,
      HOUSE,
      PAPYRUS,
      MOVE,
      ANKH;

      // $FF: synthetic method
      private static Icon[] $values() {
         return new Icon[]{CROSSHAIR, CHEVRON, GRID, RING, EYE, MONITOR, HOUSE, PAPYRUS, MOVE, ANKH};
      }
   }
}
