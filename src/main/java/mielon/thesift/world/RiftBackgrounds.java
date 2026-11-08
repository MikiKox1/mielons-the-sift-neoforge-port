package mielon.thesift.world;

import java.util.UUID;

public final class RiftBackgrounds {
   public static final int VARIANT_COUNT = 8;

   private RiftBackgrounds() {
   }

   public static int variant(UUID riftId) {
      return Math.floorMod(riftId.hashCode(), 8);
   }
}
