package net.anubis.client.proxy;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum ProxyType {
   SOCKS4("Socks4"),
   SOCKS5("Socks5");

   private final String label;

   private ProxyType(String label) {
      this.label = label;
   }

   public String label() {
      return this.label;
   }

   static ProxyType parse(String text) {
      if (text == null) {
         return null;
      } else {
         ProxyType var10000;
         switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "socks4":
            case "socks4a":
               var10000 = SOCKS4;
               break;
            case "socks5":
            case "socks5h":
            case "socks":
               var10000 = SOCKS5;
               break;
            default:
               var10000 = null;
         }

         return var10000;
      }
   }

   // $FF: synthetic method
   private static ProxyType[] $values() {
      return new ProxyType[]{SOCKS4, SOCKS5};
   }
}
