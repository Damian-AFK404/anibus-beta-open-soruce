package net.anubis.client.module.impl.donutmacros;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1887;
import net.minecraft.class_1890;
import net.minecraft.class_2960;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_9304;

@Environment(EnvType.CLIENT)
record Product(class_1792 item, String id, String search, String displayName, long minPrice, List<Enchant> enchants, boolean restock, int orderAmount, boolean beatTopOrder, long fixedPrice, long bidIncrement, long maxPrice, Predicate<class_1799> filter) {
   Product(class_1792 item, String id, String search, String displayName, long minPrice, List<Enchant> enchants, boolean restock, int orderAmount, boolean beatTopOrder, long fixedPrice, long bidIncrement, long maxPrice) {
      this(item, id, search, displayName, minPrice, enchants, restock, orderAmount, beatTopOrder, fixedPrice, bidIncrement, maxPrice, (Predicate)null);
   }

   static class_1792 resolveItem(String raw) {
      String text = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (text.isEmpty()) {
         return null;
      } else {
         if (!text.contains(":")) {
            text = "minecraft:" + text;
         }

         class_2960 id = class_2960.method_12829(text);
         if (id != null && class_7923.field_41178.method_10250(id)) {
            class_1792 item = (class_1792)class_7923.field_41178.method_63535(id);
            return item == class_1802.field_8162 ? null : item;
         } else {
            return null;
         }
      }
   }

   static String searchText(class_1792 item) {
      return class_7923.field_41178.method_10221(item).method_12832().replace('_', ' ');
   }

   static List<Enchant> parseEnchants(String raw) {
      List<Enchant> out = new ArrayList();
      if (raw == null) {
         return out;
      } else {
         for(String part : raw.split("[,;]")) {
            String text = part.trim();
            if (!text.isEmpty()) {
               int level = 1;
               int space = text.lastIndexOf(32);
               if (space > 0) {
                  String last = text.substring(space + 1).trim();
                  int parsed = parseLevel(last);
                  if (parsed > 0) {
                     level = parsed;
                     text = text.substring(0, space).trim();
                  }
               }

               String key = ServerText.key(text);
               if (!key.isEmpty()) {
                  out.add(new Enchant(key, text, level));
               }
            }
         }

         return out;
      }
   }

   private static int parseLevel(String raw) {
      try {
         int value = Integer.parseInt(raw);
         return value >= 1 && value <= 10 ? value : 0;
      } catch (NumberFormatException var2) {
         return ServerText.roman(raw);
      }
   }

   boolean matches(class_1799 stack) {
      if (!stack.method_7960() && stack.method_31574(this.item)) {
         if (this.filter != null) {
            return this.filter.test(stack);
         } else if (this.enchants.isEmpty()) {
            return true;
         } else {
            class_9304 present = class_1890.method_57532(stack);

            for(Enchant wanted : this.enchants) {
               if (levelOf(present, wanted.key()) < wanted.level()) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   static boolean enchantsMatch(class_1799 stack, List<Enchant> wanted, boolean exact) {
      class_9304 present = class_1890.method_57532(stack);
      if (exact && present.method_57541() != wanted.size()) {
         return false;
      } else {
         Iterator var4 = wanted.iterator();

         while(true) {
            if (!var4.hasNext()) {
               return true;
            }

            Enchant enchant = (Enchant)var4.next();
            int level = levelOf(present, enchant.key());
            if (exact) {
               if (level != enchant.level()) {
                  break;
               }
            } else if (level < enchant.level()) {
               break;
            }
         }

         return false;
      }
   }

   private static int levelOf(class_9304 present, String key) {
      for(Object2IntMap.Entry<class_6880<class_1887>> entry : present.method_57539()) {
         String path = (String)((class_6880)entry.getKey()).method_40230().map((k) -> k.method_29177().method_12832()).orElse("");
         if (ServerText.key(path).equals(key)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   boolean nameMatches(String label, boolean loose) {
      String key = ServerText.key(label);
      String byId = ServerText.key(class_7923.field_41178.method_10221(this.item).method_12832());
      String byName = ServerText.key(this.displayName);
      if (!key.equals(byId) && !key.equals(byName)) {
         return loose && (key.endsWith(byId) || !byName.isEmpty() && key.endsWith(byName));
      } else {
         return true;
      }
   }

   String describe() {
      StringBuilder text = new StringBuilder(this.displayName);
      if (!this.enchants.isEmpty()) {
         List<String> names = new ArrayList();

         for(Enchant enchant : this.enchants) {
            names.add(enchant.label());
         }

         text.append(" (").append(String.join(", ", names)).append(')');
      }

      return text.toString();
   }

   @Environment(EnvType.CLIENT)
   static record Enchant(String key, String name, int level) {
      String label() {
         String var10000 = this.name;
         return var10000 + (this.level > 1 ? " " + this.level : "");
      }
   }
}
