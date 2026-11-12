package mielon.thesift.client.entity;

import net.minecraft.util.Mth;

/** Head/body-relative angle from the original 2.0.3 model adjustment. */
public final class NesterHeadRotation {
   private NesterHeadRotation() { }

   public static float relativeYaw(float head, float body) {
      return -Mth.wrapDegrees(head - body);
   }
}
