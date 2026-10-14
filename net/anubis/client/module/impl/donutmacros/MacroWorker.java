package net.anubis.client.module.impl.donutmacros;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
final class MacroWorker {
   private static final long POLL_MS = 50L;
   private static final long CALL_TIMEOUT_SECONDS = 10L;
   private final String name;
   private volatile Thread thread;

   MacroWorker(String name) {
      this.name = name;
   }

   synchronized void start(Runnable body, Consumer<Throwable> crashed) {
      this.stop();
      Thread worker = new Thread(() -> {
         try {
            body.run();
         } catch (Cancelled var18) {
         } catch (Throwable failure) {
            if (this.thread == Thread.currentThread()) {
               crashed.accept(failure);
            }
         } finally {
            synchronized(this) {
               if (this.thread == Thread.currentThread()) {
                  this.thread = null;
               }

            }
         }

      }, this.name);
      worker.setDaemon(true);
      this.thread = worker;
      worker.start();
   }

   synchronized void stop() {
      Thread worker = this.thread;
      this.thread = null;
      if (worker != null && worker != Thread.currentThread()) {
         worker.interrupt();
      }

   }

   boolean running() {
      return this.thread != null;
   }

   void check() {
      if (this.thread != Thread.currentThread() || Thread.currentThread().isInterrupted()) {
         throw new Cancelled();
      }
   }

   void sleep(long ms) {
      this.check();

      try {
         Thread.sleep(Math.max(0L, ms));
      } catch (InterruptedException var4) {
         Thread.currentThread().interrupt();
         throw new Cancelled();
      }

      this.check();
   }

   void ticks(int ticks) {
      this.sleep((long)ticks * 50L);
   }

   <T> T call(Supplier<T> task) {
      this.check();
      CompletableFuture<T> future = class_310.method_1551().method_5385(task);

      try {
         return (T)future.get(10L, TimeUnit.SECONDS);
      } catch (InterruptedException var6) {
         Thread.currentThread().interrupt();
         throw new Cancelled();
      } catch (ExecutionException e) {
         Throwable cause = e.getCause();
         if (cause instanceof RuntimeException runtime) {
            throw runtime;
         } else if (cause instanceof Error error) {
            throw error;
         } else {
            throw new IllegalStateException(cause);
         }
      } catch (TimeoutException var8) {
         future.cancel(false);
         throw new IllegalStateException("The game did not respond");
      }
   }

   void run(Runnable task) {
      this.call(() -> {
         task.run();
         return Boolean.TRUE;
      });
   }

   boolean waitFor(BooleanSupplier condition, long timeoutMs) {
      long end = System.currentTimeMillis() + timeoutMs;

      while(true) {
         Objects.requireNonNull(condition);
         if ((Boolean)this.call(condition::getAsBoolean)) {
            return true;
         }

         if (System.currentTimeMillis() >= end) {
            return false;
         }

         this.sleep(50L);
      }
   }

   @Environment(EnvType.CLIENT)
   static final class Cancelled extends RuntimeException {
      Cancelled() {
         super((String)null, (Throwable)null, false, false);
      }
   }
}
