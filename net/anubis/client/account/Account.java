package net.anubis.client.account;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Account {
   private final String id;
   private final AccountType type;
   private final String name;
   private final String uuid;
   private final String secret;

   Account(String id, AccountType type, String name, String uuid, String secret) {
      this.id = id;
      this.type = type;
      this.name = name;
      this.uuid = uuid;
      this.secret = secret;
   }

   public AccountType type() {
      return this.type;
   }

   public String name() {
      return this.name;
   }

   public String uuid() {
      return this.uuid;
   }

   public String id() {
      return this.id;
   }

   String secret() {
      return this.secret;
   }

   Account withLogin(String name, String uuid, String secret) {
      return new Account(this.id, this.type, name, uuid, secret);
   }

   public boolean equals(Object other) {
      boolean var10000;
      if (other instanceof Account account) {
         if (account.id.equals(this.id)) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   public int hashCode() {
      return this.id.hashCode();
   }

   public String toString() {
      String var10000 = this.type.label();
      return var10000 + " " + this.name;
   }
}
