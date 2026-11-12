package mielon.thesift.client.entity;

import net.minecraft.util.Mth;

/** Relative head yaw with vanilla angle interpolation and seam wrapping. */
public final class NesterHeadYaw {
   private NesterHeadYaw() {}

   public static float relative(float previousHead, float currentHead, float bodyYaw, float partialTick) {
      float head = Mth.rotLerp(partialTick, previousHead, currentHead);
      return -Mth.wrapDegrees(head - bodyYaw);
   }
}
