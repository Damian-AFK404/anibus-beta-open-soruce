package net.anubis.client.util.antivanish;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AntiVanishText {
   private AntiVanishText() {
   }

   public static String normalize(String text) {
      if (text != null && !text.isBlank()) {
         String decomposed = Normalizer.normalize(stripLegacyCodes(text), Form.NFKD).toLowerCase(Locale.ROOT);
         StringBuilder out = new StringBuilder(decomposed.length());
         boolean spaced = true;
         int offset = 0;

         while(offset < decomposed.length()) {
            int codePoint = decomposed.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (!isMark(codePoint) && !isFormatCode(codePoint)) {
               int folded = foldConfusable(codePoint);
               if ((folded < 97 || folded > 122) && (folded < 48 || folded > 57)) {
                  if (!spaced) {
                     out.append(' ');
                     spaced = true;
                  }
               } else {
                  out.appendCodePoint(folded);
                  spaced = false;
               }
            }
         }

         offset = out.length();
         if (offset > 0 && out.charAt(offset - 1) == ' ') {
            out.setLength(offset - 1);
         }

         return out.toString();
      } else {
         return "";
      }
   }

   public static boolean containsPlayerName(String displayedName, String playerName) {
      String rawDisplay = stripLegacyCodes(displayedName == null ? "" : displayedName).toLowerCase(Locale.ROOT);
      String rawPlayer = stripLegacyCodes(playerName == null ? "" : playerName).trim().toLowerCase(Locale.ROOT);
      if (rawPlayer.matches("[a-z0-9_]{1,16}")) {
         int match;
         for(int from = 0; from <= rawDisplay.length() - rawPlayer.length(); from = match + 1) {
            match = rawDisplay.indexOf(rawPlayer, from);
            if (match < 0) {
               return false;
            }

            int end = match + rawPlayer.length();
            boolean leftBoundary = match == 0 || !isUsernameCharacter(rawDisplay.charAt(match - 1));
            boolean rightBoundary = end == rawDisplay.length() || !isUsernameCharacter(rawDisplay.charAt(end));
            if (leftBoundary && rightBoundary) {
               return true;
            }
         }

         return false;
      } else {
         String displayed = normalize(displayedName);
         String player = normalize(playerName);
         if (!displayed.isBlank() && !player.isBlank()) {
            return displayed.equals(player) || (" " + displayed + " ").contains(" " + player + " ");
         } else {
            return false;
         }
      }
   }

   public static boolean looksLikeLeaveMessage(String message, String playerName) {
      if (!containsPlayerName(message, playerName)) {
         return false;
      } else {
         String text = " " + normalize(message) + " ";
         String player = normalize(playerName);
         if (player.isBlank()) {
            return false;
         } else {
            String prefix = " " + player + " ";
            return text.contains(prefix + "left the game ") || text.contains(prefix + "left ") || text.contains(prefix + "has left ") || text.contains(prefix + "quit ") || text.contains(prefix + "has quit ") || text.contains(prefix + "disconnected ") || text.contains(prefix + "logged out ");
         }
      }
   }

   public static boolean isPlausiblePlayerName(String name) {
      if (name == null) {
         return false;
      } else {
         String trimmed = name.trim();
         int len = trimmed.length();
         if (len >= 3 && len <= 16) {
            return trimmed.indexOf(32) < 0;
         } else {
            return false;
         }
      }
   }

   public static boolean isUsername(String name) {
      if (name == null) {
         return false;
      } else {
         int len = name.length();
         if (len >= 2 && len <= 32) {
            boolean hasAlnum = false;

            for(int i = 0; i < len; ++i) {
               char c = name.charAt(i);
               boolean alnum = c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
               if (alnum) {
                  hasAlnum = true;
               } else if (c != '_' && c != '.' && c != ' ') {
                  return false;
               }
            }

            return hasAlnum;
         } else {
            return false;
         }
      }
   }

   private static boolean isUsernameCharacter(char character) {
      return character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '_';
   }

   private static boolean isMark(int codePoint) {
      int type = Character.getType(codePoint);
      return type == 6 || type == 8 || type == 7;
   }

   private static boolean isFormatCode(int codePoint) {
      return codePoint == 167 || codePoint == 8203 || codePoint == 8204 || codePoint == 8205 || codePoint == 65279;
   }

   private static String stripLegacyCodes(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder out = new StringBuilder(text.length());

         for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == 167 && i + 1 < text.length()) {
               char code = Character.toLowerCase(text.charAt(i + 1));
               if ("0123456789abcdefklmnorx".indexOf(code) >= 0) {
                  ++i;
                  continue;
               }
            }

            out.append(c);
         }

         return out.toString();
      } else {
         return "";
      }
   }

   private static int foldConfusable(int codePoint) {
      int var10000;
      switch (codePoint) {
         case 593:
         case 945:
         case 1072:
         case 7424:
            var10000 = 97;
            break;
         case 609:
         case 610:
            var10000 = 103;
            break;
         case 618:
         case 953:
         case 1110:
            var10000 = 105;
            break;
         case 628:
         case 1400:
            var10000 = 110;
            break;
         case 640:
            var10000 = 114;
            break;
         case 655:
         case 1091:
            var10000 = 121;
            break;
         case 665:
         case 946:
         case 1074:
            var10000 = 98;
            break;
         case 668:
         case 1085:
            var10000 = 104;
            break;
         case 671:
            var10000 = 108;
            break;
         case 949:
         case 1077:
         case 7431:
            var10000 = 101;
            break;
         case 954:
         case 1082:
         case 7435:
            var10000 = 107;
            break;
         case 957:
         case 7456:
            var10000 = 118;
            break;
         case 959:
         case 1086:
         case 7439:
            var10000 = 111;
            break;
         case 961:
         case 1088:
         case 7448:
            var10000 = 112;
            break;
         case 964:
         case 1090:
         case 7451:
            var10000 = 116;
            break;
         case 965:
         case 7452:
            var10000 = 117;
            break;
         case 1010:
         case 1089:
         case 7428:
            var10000 = 99;
            break;
         case 1084:
         case 7437:
            var10000 = 109;
            break;
         case 1093:
            var10000 = 120;
            break;
         case 1109:
         case 42801:
            var10000 = 115;
            break;
         case 1112:
         case 7434:
            var10000 = 106;
            break;
         case 1121:
         case 7457:
            var10000 = 119;
            break;
         case 1281:
         case 7429:
            var10000 = 100;
            break;
         case 1307:
            var10000 = 113;
            break;
         case 7458:
         case 7459:
            var10000 = 122;
            break;
         case 42800:
            var10000 = 102;
            break;
         default:
            var10000 = codePoint;
      }

      return var10000;
   }
}
