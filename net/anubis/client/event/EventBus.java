package net.anubis.client.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EventBus {
   public static final EventBus INSTANCE = new EventBus();
   private final Map<Object, List<RegistrationEntry<?>>> subscriptions = new IdentityHashMap();
   private final ArrayBlockingQueue<Runnable> asyncQueue = new ArrayBlockingQueue(4096);
   private volatile long failedTasks;
   private static final MethodType FACTORY_TYPE = MethodType.methodType(Listener.class, Object.class);
   private static final ClassValue<List<HandlerSpec>> HANDLER_SPECS = new ClassValue<List<HandlerSpec>>() {
      protected List<HandlerSpec> computeValue(Class<?> clazz) {
         return EventBus.scan(clazz);
      }
   };
   private static final ClassValue<EventType<?>> TYPE_CACHE = new ClassValue<EventType<?>>() {
      protected EventType<?> computeValue(Class<?> eventClass) {
         try {
            Field f = eventClass.getField("TYPE");
            if (!EventType.class.isAssignableFrom(f.getType())) {
               throw new IllegalArgumentException(eventClass.getName() + ".TYPE must be an EventType instance.");
            } else {
               return (EventType)f.get((Object)null);
            }
         } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Event class " + eventClass.getName() + " must declare 'public static final EventType<...> TYPE'.", e);
         } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access " + eventClass.getName() + ".TYPE", e);
         }
      }
   };

   public boolean postAsync(Runnable task) {
      return this.asyncQueue.offer((Runnable)Objects.requireNonNull(task));
   }

   public int drainAsync() {
      return this.drainAsync(512, 2000000L);
   }

   public int drainAsync(int maxTasks, long maxNanos) {
      if (maxTasks > 0 && maxNanos > 0L) {
         long started = System.nanoTime();
         int processed = 0;

         while(processed < maxTasks && System.nanoTime() - started < maxNanos) {
            Runnable task = (Runnable)this.asyncQueue.poll();
            if (task == null) {
               break;
            }

            ++processed;

            try {
               task.run();
            } catch (RuntimeException failure) {
               long count = ++this.failedTasks;
               if (count <= 4L || count % 256L == 0L) {
                  System.err.println("[AnubisEventBus] Async task failed (#" + count + ")");
                  failure.printStackTrace();
               }
            }
         }

         return processed;
      } else {
         throw new IllegalArgumentException("Drain limits must be positive");
      }
   }

   public <E extends Event> void off(ListenerHandle<E> handle) {
      handle.unregister();
   }

   public int register(Object subscriber) {
      List<HandlerSpec> specs = (List)HANDLER_SPECS.get(subscriber.getClass());
      if (specs.isEmpty()) {
         return 0;
      } else {
         synchronized(this) {
            return this.register(subscriber, specs);
         }
      }
   }

   private int register(Object subscriber, List<HandlerSpec> specs) {
      if (this.subscriptions.containsKey(subscriber)) {
         throw new IllegalStateException("Subscriber already registered: " + subscriber.getClass().getName());
      } else {
         List<RegistrationEntry<?>> entries = buildEntries(subscriber, specs);

         try {
            for(RegistrationEntry<?> entry : entries) {
               entry.register();
            }
         } catch (Error | RuntimeException var9) {
            Throwable failure = var9;

            for(RegistrationEntry<?> entry : entries) {
               try {
                  entry.unregister();
               } catch (Error | RuntimeException cleanupFailure) {
                  if (failure != cleanupFailure) {
                     failure.addSuppressed(cleanupFailure);
                  }
               }
            }

            throw failure;
         }

         this.subscriptions.put(subscriber, entries);
         return entries.size();
      }
   }

   public synchronized void unregister(Object subscriber) {
      List<RegistrationEntry<?>> entries = (List)this.subscriptions.remove(subscriber);
      if (entries != null) {
         for(RegistrationEntry<?> entry : entries) {
            entry.unregister();
         }

      }
   }

   private static List<RegistrationEntry<?>> buildEntries(Object subscriber, List<HandlerSpec> specs) {
      List<RegistrationEntry<?>> entries = new ArrayList(specs.size());

      for(HandlerSpec spec : specs) {
         Listener<Event> listener = spec.staticListener();
         if (listener == null) {
            try {
               listener = castListener(spec.listenerFactory().invokeExact(subscriber));
            } catch (Throwable t) {
               throw new RuntimeException("Failed to bind listener for " + subscriber.getClass().getName(), t);
            }
         }

         EventType var10002 = spec.type();
         Stage var10003 = spec.stage();
         EventPriority var10004 = spec.priority();
         Objects.requireNonNull(listener);
         ListenerHandle<Event> handle = new ListenerHandle<Event>(var10002, var10003, var10004, subscriber, listener::invoke);
         entries.add(new RegistrationEntry(spec.type(), handle));
      }

      return entries;
   }

   private static Listener<Event> castListener(Listener<?> listener) {
      return listener;
   }

   private static List<HandlerSpec> scan(Class<?> clazz) {
      List<HandlerSpec> specs = new ArrayList();

      for(Class<?> parent = clazz; parent != null; parent = parent.getSuperclass()) {
         for(Method method : parent.getDeclaredMethods()) {
            if (isHandler(method) && !Modifier.isPublic(method.getModifiers())) {
               throw new IllegalArgumentException("Event handler must be public: " + String.valueOf(method));
            }
         }
      }

      for(Method method : clazz.getMethods()) {
         Stage stage = Stage.PRE;
         EventPriority priority = EventPriority.NORMAL;
         boolean matched = false;
         EventHandler newAnnotation = (EventHandler)method.getAnnotation(EventHandler.class);
         if (newAnnotation != null) {
            stage = newAnnotation.stage();
            priority = newAnnotation.priority();
            matched = true;
         } else {
            EventListener legacyAnnotation = (EventListener)method.getAnnotation(EventListener.class);
            if (legacyAnnotation != null) {
               stage = Stage.PRE;
               priority = EventPriority.fromInt(legacyAnnotation.priority());
               matched = true;
            }
         }

         if (matched) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !Event.class.isAssignableFrom(params[0]) || method.getReturnType() != Void.TYPE) {
               throw new IllegalArgumentException("Event handler must return void and accept one Event: " + String.valueOf(method));
            }

            Class<Event> eventClass = params[0];
            EventType<Event> type = resolveTypeFor(eventClass);
            if (Modifier.isStatic(method.getModifiers())) {
               specs.add(new HandlerSpec(type, stage, priority, (MethodHandle)null, LambdaListenerFactory.createStatic(method)));
            } else {
               specs.add(new HandlerSpec(type, stage, priority, LambdaListenerFactory.createFactory(method).asType(FACTORY_TYPE), (Listener)null));
            }
         }
      }

      return specs.isEmpty() ? List.of() : List.copyOf(specs);
   }

   private static boolean isHandler(Method method) {
      return method.isAnnotationPresent(EventHandler.class) || method.isAnnotationPresent(EventListener.class);
   }

   static <E extends Event> EventType<E> resolveTypeFor(Class<E> eventClass) {
      return (EventType)TYPE_CACHE.get(eventClass);
   }

   @Environment(EnvType.CLIENT)
   private static record HandlerSpec(EventType<Event> type, Stage stage, EventPriority priority, MethodHandle listenerFactory, Listener<Event> staticListener) {
   }

   @Environment(EnvType.CLIENT)
   private static record RegistrationEntry<E extends Event>(EventType<E> type, ListenerHandle<E> handle) {
      void register() {
         this.type.register(this.handle);
      }

      void unregister() {
         this.type.unregister(this.handle);
      }
   }
}
