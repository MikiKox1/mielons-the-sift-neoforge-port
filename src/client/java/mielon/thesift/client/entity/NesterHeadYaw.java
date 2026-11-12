package mielon.thesift.client.entity;

import net.minecraft.util.Mth;

/** Match the author's head-relative, shortest-arc interpolation across +/-180°. */
public final class NesterHeadYaw {
   private NesterHeadYaw() {
   }

   public static float relative(float oldHead, float head, float body, float partialTick) {
      return -Mth.wrapDegrees(Mth.rotLerp(partialTick, oldHead, head) - body);
   }
}
