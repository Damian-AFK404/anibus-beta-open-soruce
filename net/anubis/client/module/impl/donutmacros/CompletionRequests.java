package net.anubis.client.module.impl.donutmacros;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2596;
import net.minecraft.class_2639;
import net.minecraft.class_2805;
import net.minecraft.class_310;
import net.minecraft.class_634;

@Environment(EnvType.CLIENT)
final class CompletionRequests {
   private static final int FIRST_ID = 10000;
   private static final int LAST_ID = 29999;
   private static final long FORGET_MS = 15000L;
   private final Map<Integer, Request> requests = new ConcurrentHashMap();
   private int nextId = 10000;
   private Form form;

   CompletionRequests() {
      this.form = CompletionRequests.Form.BOTH;
   }

   Form form() {
      return this.form;
   }

   void form(Form form) {
      this.form = form;
   }

   int fire(String partial) {
      return this.fire(partial, this.form);
   }

   int fire(String partial, Form using) {
      class_634 connection = class_310.method_1551().method_1562();
      if (connection == null) {
         return -1;
      } else {
         int id = this.nextId++;
         if (this.nextId > 29999) {
            this.nextId = 10000;
         }

         this.requests.put(id, new Request(System.currentTimeMillis()));

         try {
            if (using != CompletionRequests.Form.BARE) {
               connection.method_52787(new class_2805(id, "/" + partial));
            }

            if (using != CompletionRequests.Form.SLASH) {
               connection.method_52787(new class_2805(id, partial));
            }

            return id;
         } catch (RuntimeException var6) {
            this.requests.remove(id);
            return -1;
         }
      }
   }

   List<String> poll(int id) {
      Request request = (Request)this.requests.get(id);
      return request != null && !request.done ? request.names : null;
   }

   void forget(int id) {
      Request request = (Request)this.requests.get(id);
      if (request != null) {
         request.done = true;
      }

   }

   void prune(long now) {
      this.requests.values().removeIf((request) -> now - request.at > 15000L);
   }

   void clear() {
      this.requests.clear();
      this.form = CompletionRequests.Form.BOTH;
   }

   boolean ingest(class_2596<?> packet) {
      if (packet instanceof class_2639 suggestions) {
         Request request = (Request)this.requests.get(suggestions.comp_2262());
         if (request == null) {
            return false;
         } else {
            if (request.names == null) {
               List<String> names = new ArrayList();

               for(Suggestion suggestion : suggestions.method_11397().getList()) {
                  String text = suggestion.getText();
                  if (text != null && !text.isBlank()) {
                     names.add(text.trim());
                  }
               }

               request.names = List.copyOf(names);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum Form {
      BOTH,
      SLASH,
      BARE;

      // $FF: synthetic method
      private static Form[] $values() {
         return new Form[]{BOTH, SLASH, BARE};
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Request {
      final long at;
      volatile List<String> names;
      volatile boolean done;

      Request(long at) {
         this.at = at;
      }
   }
}
