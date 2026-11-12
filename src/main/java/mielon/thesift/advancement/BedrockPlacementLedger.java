package mielon.thesift.advancement;

import net.minecraft.nbt.CompoundTag;

/** Mutates the actual per-chunk CommandStorage payload used by the Bedrock goal. */
public final class BedrockPlacementLedger {
   private BedrockPlacementLedger() {}

   public static boolean wasPlaced(CompoundTag changes, String positionKey) {
      return changes.contains(positionKey);
   }

   /** True only if the modified tag needs to be written to CommandStorage. */
   public static boolean apply(CompoundTag changes, String positionKey, boolean isBedrock) {
      if (isBedrock) {
         changes.putBoolean(positionKey, true);
         return true;
      }
      if (!changes.contains(positionKey)) return false;
      changes.remove(positionKey);
      return true;
   }
}
