package mielon.thesift.advancement;

/** Pure, testable version of the Sift's original five-layer bedrock floor rule. */
public final class BedrockFloorRules {
   private BedrockFloorRules() {
   }

   public static boolean matches(int depth, double sample) {
      return depth >= 0 && depth < 5 && (depth == 0 || sample < 1.0 - depth / 5.0);
   }
}
