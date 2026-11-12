package mielon.thesift.advancement;

/** Exact 2.0.3 natural Bedrock generation test, shared by the live sightline path. */
public final class BedrockFloorRules {
   private BedrockFloorRules() {}

   public static boolean matches(int depth, double randomSample) {
      return depth >= 0 && depth < 5 && (depth == 0 || randomSample < 1.0 - depth / 5.0);
   }
}
