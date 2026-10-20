package mielon.thesift.client.compat;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.lang.reflect.Method;
import java.util.Locale;
import mielon.thesift.TheSiftMod;
import net.neoforged.fml.ModList;

public final class SiftShaderCompat {
   private static boolean initialized;
   private static boolean irisPresent;
   private static Object irisApi;
   private static Method isShaderPackInUse;
   private static Method assignPipeline;
   private static Class<?> irisProgramClass;
   private static Method currentPackName;

   private SiftShaderCompat() {
   }

   public static void initialize() {
      if (!initialized) {
         initialized = true;
         irisPresent = ModList.get().isLoaded("iris");
         if (irisPresent) {
            try {
               Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
               irisApi = apiClass.getMethod("getInstance").invoke(null);
               isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");

               try {
                  irisProgramClass = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
                  assignPipeline = apiClass.getMethod("assignPipeline", RenderPipeline.class, irisProgramClass);
               } catch (ReflectiveOperationException ignored) {
                  assignPipeline = null;
                  irisProgramClass = null;
               }

               // Iris API v0 has no public pack-name getter. The optional
               // Iris internal method is probed defensively to select only
               // Photon's known special post-final sky compositor. It is
               // present in the exact Iris1.11.7 build for Minecraft 26.3.
               try {
                  currentPackName = Class.forName("net.irisshaders.iris.Iris").getMethod("getCurrentPackName");
               } catch (ReflectiveOperationException | LinkageError ignored) {
                  currentPackName = null;
               }

               TheSiftMod.LOGGER.info("Iris detected; The Sift shader compatibility is enabled.");
               if (currentPackName != null) {
                  try {
                     Object packName = currentPackName.invoke(null);
                     TheSiftMod.LOGGER.info(
                        "The Sift MC26.3 Iris pack '{}'; Photon-specific sky compositing: {}",
                        packName, isPhotonShaderPackSelected()
                     );
                  } catch (ReflectiveOperationException ignored) {
                     TheSiftMod.LOGGER.debug("Iris pack name not ready during initial client registration.");
                  }
               }
            } catch (ReflectiveOperationException | LinkageError exception) {
               irisApi = null;
               isShaderPackInUse = null;
               assignPipeline = null;
               irisProgramClass = null;
               currentPackName = null;
               TheSiftMod.LOGGER.warn("Iris was detected, but its compatibility API could not be initialized.", exception);
            }
         }
      }
   }

   public static boolean isIrisPresent() {
      initialize();
      return irisPresent;
   }

   public static boolean isShaderPackInUse() {
      initialize();
      if (irisApi != null && isShaderPackInUse != null) {
         try {
            return Boolean.TRUE.equals(isShaderPackInUse.invoke(irisApi));
         } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
         }
      } else {
         return false;
      }
   }

   /**
    * Selects the Photon-specific sky rendering route when a Photon shaderpack
    * is active. Other shaderpacks and the native sky retain their own routes.
    */
   public static boolean isPhotonShaderPackSelected() {
      initialize();
      if (currentPackName == null) {
         return false;
      }
      try {
         Object value = currentPackName.invoke(null);
         return value instanceof String name
            && name.toLowerCase(Locale.ROOT).startsWith("photon");
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return false;
      }
   }

   public static void assignPipeline(RenderPipeline pipeline, String irisProgram) {
      initialize();
      if (irisApi != null && assignPipeline != null && irisProgramClass != null) {
         try {
            Object program = enumValue(irisProgramClass, irisProgram);
            assignPipeline.invoke(irisApi, pipeline, program);
         } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            TheSiftMod.LOGGER.warn("Could not assign The Sift pipeline {} to Iris program {}.", new Object[]{pipeline.getLocation(), irisProgram, exception});
         }
      }
   }

   private static Object enumValue(Class<?> enumClass, String name) {
      return Enum.valueOf(enumClass.asSubclass(Enum.class), name);
   }
}
