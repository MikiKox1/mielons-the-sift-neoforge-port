package mielon.thesift.client.render;

/** Limits Sift's Complementary sky layers to the same 72% budget as MC26.3. */
public final class SiftComplementarySkyOpacity {
   private static final float SKY_COVERAGE = 0.72F;

   private SiftComplementarySkyOpacity() {
   }

   public static float forLayer(int layer, float night, float day, float thrive) {
      float share;
      float strength = 1.0F;
      switch (layer) {
         case 0 -> share = 0.40F;
         case 1 -> share = night * 0.20F;
         case 2 -> { share = day * 0.36F; strength = 0.96F; }
         case 3 -> { share = night * 0.24F; strength = 0.96F; }
         case 4 -> { share = day * 0.24F; strength = 0.78F; }
         case 5 -> { share = night * 0.16F; strength = 0.82F; }
         case 6 -> share = thrive * 0.42F;
         case 7 -> share = thrive * 0.18F;
         default -> throw new IllegalArgumentException("Unknown sky layer " + layer);
      }
      float boundedShare = Math.max(0.0F, Math.min(1.0F, share));
      return strength * (1.0F - (float)Math.pow(1.0F - SKY_COVERAGE, boundedShare));
   }
}
