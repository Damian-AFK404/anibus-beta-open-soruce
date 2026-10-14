package net.anubis.client.util.spotify;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DisplayText {
   private DisplayText() {
   }

   public static String fold(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder out = null;
         boolean space = false;

         for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            String replacement = replacement(c);
            if (replacement != null || c == ' ' && space) {
               if (out == null) {
                  out = (new StringBuilder(text.length())).append(text, 0, i);
               }

               if (replacement != null) {
                  for(int k = 0; k < replacement.length(); ++k) {
                     char r = replacement.charAt(k);
                     if (r != ' ' || !space && !out.isEmpty()) {
                        out.append(r);
                        space = r == ' ';
                     }
                  }
               }
            } else {
               if (out != null) {
                  out.append(c);
               }

               space = c == ' ';
            }
         }

         if (out == null) {
            String stripped = text.strip();
            return stripped.length() == text.length() ? text : stripped;
         } else {
            return out.toString().strip();
         }
      } else {
         return "";
      }
   }

   public static String fold(String text, IntPredicate drawable) {
      String folded = fold(text);
      StringBuilder out = null;

      int next;
      for(int i = 0; i < folded.length(); i = next) {
         int cp = folded.codePointAt(i);
         next = i + Character.charCount(cp);
         String base = cp >= 128 && Character.isLetter(cp) && !drawable.test(cp) ? baseLetters(cp) : null;
         if (base != null && !base.codePoints().allMatch((b) -> b < 128 || drawable.test(b))) {
            base = null;
         }

         if (base != null && out == null) {
            out = (new StringBuilder(folded.length())).append(folded, 0, i);
         }

         if (out != null) {
            if (base != null) {
               out.append(base);
            } else {
               out.append(folded, i, next);
            }
         }
      }

      return out == null ? folded : out.toString();
   }

   static String baseLetters(int cp) {
      String var10000;
      switch (cp) {
         case 272 -> var10000 = "D";
         case 273 -> var10000 = "d";
         case 294 -> var10000 = "H";
         case 295 -> var10000 = "h";
         case 306 -> var10000 = "IJ";
         case 307 -> var10000 = "ij";
         case 319 -> var10000 = "L";
         case 320 -> var10000 = "l";
         case 321 -> var10000 = "L";
         case 322 -> var10000 = "l";
         case 338 -> var10000 = "OE";
         case 339 -> var10000 = "oe";
         case 358 -> var10000 = "T";
         case 359 -> var10000 = "t";
         case 383 -> var10000 = "s";
         default -> var10000 = null;
      }

      String special = var10000;
      if (special != null) {
         return special;
      } else {
         String decomposed = Normalizer.normalize(new String(Character.toChars(cp)), Form.NFD);
         StringBuilder base = new StringBuilder(decomposed.length());
         IntStream var4 = decomposed.codePoints().filter((c) -> Character.getType(c) != 6);
         Objects.requireNonNull(base);
         var4.forEach(base::appendCodePoint);
         return !base.isEmpty() && base.length() != decomposed.length() ? base.toString() : null;
      }
   }

   private static String replacement(char c) {
      String var10000;
      switch (c) {
         case '\t':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case ' ':
         case '　':
            var10000 = " ";
            break;
         case '\u200b':
         case '\u200c':
         case '\u200d':
         case '\u2060':
         case '♩':
         case '♪':
         case '♫':
         case '♬':
         case '\ufeff':
            var10000 = "";
            break;
         case '‐':
         case '‑':
         case '‒':
         case '–':
         case '—':
         case '―':
         case '−':
            var10000 = "-";
            break;
         case '‘':
         case '’':
         case '‚':
         case '‛':
         case '′':
            var10000 = "'";
            break;
         case '“':
         case '”':
         case '„':
         case '‟':
         case '″':
            var10000 = "\"";
            break;
         case '•':
         case '‧':
         case '∙':
            var10000 = "·";
            break;
         case '…':
            var10000 = "...";
            break;
         default:
            var10000 = null;
      }

      return var10000;
   }
}
