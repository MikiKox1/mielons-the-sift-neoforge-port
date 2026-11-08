package mielon.thesift.world;

import net.minecraft.resources.Identifier;

public final class SiftTides {
   public static final Identifier CLOCK_ID = Identifier.fromNamespaceAndPath("the_sift", "the_sift");
   public static final int TIDE_TICKS = 8400;
   public static final int TRANSITION_TICKS = 60;
   public static final int THRIVE_START = 0;
   public static final int FLOW_START = 8400;
   public static final int ENDURE_START = 16800;
   public static final int PERIOD_TICKS = 25200;

   private SiftTides() {
   }

   public static SiftTides.Tide tide(long ticks) {
      return SiftTides.Tide.values()[(int)(Math.floorMod(ticks, 25200L) / 8400L)];
   }

   public static boolean canSleep(long ticks, boolean thunderstorm) {
      return thunderstorm || tide(ticks) == SiftTides.Tide.ENDURE;
   }

   public static float thriveBlend(long ticks) {
      return blend(ticks, 0);
   }

   public static float flowBlend(long ticks) {
      return blend(ticks, 1);
   }

   public static float nightBlend(long ticks) {
      return blend(ticks, 2);
   }

   private static float blend(long ticks, int phase) {
      long tick = Math.floorMod(ticks, 25200L);
      int current = (int)(tick / 8400L);
      int local = (int)(tick % 8400L);
      float t = Math.max(0.0F, Math.min(1.0F, (local - 8340) / 60.0F));
      t = t * t * (3.0F - 2.0F * t);
      return (current == phase ? 1.0F - t : 0.0F) + ((current + 1) % 3 == phase ? t : 0.0F);
   }

   public static int shaderTime(long ticks) {
      long tick = Math.floorMod(ticks, 25200L);
      int phase = (int)(tick / 8400L);
      int[] times = new int[]{1000, 3000, 18000};
      int start = times[phase];
      int end = times[(phase + 1) % 3];
      if (end < start) {
         end += 24000;
      }

      float t = 1.0F - blend(ticks, phase);
      return Math.floorMod(Math.round(start + (end - start) * t), 24000);
   }

   public enum Tide {
      THRIVE,
      FLOW,
      ENDURE;
   }
}
