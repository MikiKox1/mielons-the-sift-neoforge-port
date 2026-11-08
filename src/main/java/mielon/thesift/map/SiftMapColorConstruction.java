package mielon.thesift.map;

import mielon.thesift.mixin.MapColorInvoker;
import net.minecraft.world.level.material.MapColor;

public final class SiftMapColorConstruction {
   private static final ThreadLocal<Boolean> PRIVATE = ThreadLocal.withInitial(() -> false);

   private SiftMapColorConstruction() {
   }

   public static boolean isPrivate() {
      return PRIVATE.get();
   }

   public static MapColor create(int rgb, MapColor fallback) {
      PRIVATE.set(true);

      try {
         return MapColorInvoker.theSift$create(fallback.id, rgb);
      } finally {
         PRIVATE.remove();
      }
   }
}
