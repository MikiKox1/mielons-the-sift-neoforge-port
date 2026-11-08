package mielon.thesift.map;

import mielon.thesift.block.SiftMapColors;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;

public final class SiftMapSampling {
   private static final ThreadLocal<int[]> PIXEL = ThreadLocal.withInitial(() -> new int[1]);

   private SiftMapSampling() {
   }

   public static byte sample(MapColor color, Brightness brightness) {
      int shaded = 0;
      if (SiftMapColors.isExtended(color)) {
         int shade = brightness.modifier;
         int red = (color.col >> 16 & 0xFF) * shade / 255;
         int green = (color.col >> 8 & 0xFF) * shade / 255;
         int blue = (color.col & 0xFF) * shade / 255;
         shaded = 0xFF000000 | red << 16 | green << 8 | blue;
      }

      PIXEL.get()[0] = shaded;
      return color.getPackedId(brightness);
   }

   public static int take() {
      int[] pixel = PIXEL.get();
      int color = pixel[0];
      pixel[0] = 0;
      return color;
   }
}
