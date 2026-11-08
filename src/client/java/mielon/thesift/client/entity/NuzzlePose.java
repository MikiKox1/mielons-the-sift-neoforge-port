package mielon.thesift.client.entity;

import net.minecraft.util.Mth;

public final class NuzzlePose {
   private NuzzlePose() {
   }

   public static float shadowRadius(float width) {
      return 0.65F * width / 0.9F;
   }

   public static float headPitch(float nativeAngle) {
      return -Mth.clamp(nativeAngle * 0.45F, -0.5F, 0.5F);
   }

   public static float headY(float grazing) {
      return -5.0F * Mth.clamp(grazing, 0.0F, 1.0F);
   }

   public static float headZ(float pitch) {
      return -Math.max(0.0F, 1.1F - 2.0F * Mth.cos(pitch) + 4.0F * Math.abs(Mth.sin(pitch)));
   }
}
