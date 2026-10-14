package net.anubis.client.event;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LambdaListenerFactory {
   private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
   private static final MethodType ERASED_INVOKE_TYPE;

   private LambdaListenerFactory() {
   }

   public static <E extends Event> Listener<E> create(Object instance, Method method) {
      if (instance == null) {
         return createStatic(method);
      } else {
         try {
            return createFactory(method).invoke(instance);
         } catch (RuntimeException e) {
            throw e;
         } catch (Throwable t) {
            throw new RuntimeException("Unexpected error creating listener for method " + String.valueOf(method), t);
         }
      }
   }

   public static <E extends Event> Listener<E> createStatic(Method method) {
      validateMethod(method);

      try {
         MethodHandles.Lookup targetLookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
         MethodHandle directHandle = targetLookup.unreflect(method);
         MethodType instantiatedType = MethodType.methodType(Void.TYPE, method.getParameterTypes()[0]);
         CallSite site = LambdaMetafactory.metafactory(targetLookup, "invoke", MethodType.methodType(Listener.class), ERASED_INVOKE_TYPE, directHandle, instantiatedType);
         return site.getTarget().invoke();
      } catch (Throwable t) {
         throw wrap(method, t);
      }
   }

   public static MethodHandle createFactory(Method method) {
      validateMethod(method);

      try {
         MethodHandles.Lookup targetLookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
         MethodHandle directHandle = targetLookup.unreflect(method);
         MethodType instantiatedType = MethodType.methodType(Void.TYPE, method.getParameterTypes()[0]);
         MethodType factoryType = MethodType.methodType(Listener.class, method.getDeclaringClass());
         CallSite site = LambdaMetafactory.metafactory(targetLookup, "invoke", factoryType, ERASED_INVOKE_TYPE, directHandle, instantiatedType);
         return site.getTarget();
      } catch (Throwable t) {
         throw wrap(method, t);
      }
   }

   private static RuntimeException wrap(Method method, Throwable t) {
      if (t instanceof LambdaConversionException) {
         return new RuntimeException("LambdaMetafactory failed for method " + String.valueOf(method) + ". Ensure the listener method is public and its parameter type is a concrete Event subclass.", t);
      } else {
         return t instanceof IllegalAccessException ? new RuntimeException("Cannot access method " + String.valueOf(method) + ". Listener methods must be public and their declaring class must be accessible.", t) : new RuntimeException("Unexpected error creating listener for method " + String.valueOf(method), t);
      }
   }

   private static void validateMethod(Method method) {
      if (method.getReturnType() != Void.TYPE) {
         throw new IllegalArgumentException("@EventListener method must return void: " + String.valueOf(method));
      } else {
         Class<?>[] params = method.getParameterTypes();
         if (params.length != 1) {
            throw new IllegalArgumentException("@EventListener method must have exactly one parameter: " + String.valueOf(method));
         } else if (!Event.class.isAssignableFrom(params[0])) {
            throw new IllegalArgumentException("@EventListener parameter must be a subclass of Event: " + String.valueOf(method));
         }
      }
   }

   static {
      ERASED_INVOKE_TYPE = MethodType.methodType(Void.TYPE, Event.class);
   }
}
