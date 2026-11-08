package mielon.thesift.fluid;

public final class IchorSoulMotion {
   public static final int AIR_LIFETIME = 60;
   public static final double AIR_RISE = 16.0;
   public static final double CANYON_RISE = 16.0;

   private IchorSoulMotion() {
   }

   public static float smooth(double value) {
      double t = Math.max(0.0, Math.min(1.0, value));
      return (float)(t * t * (3.0 - 2.0 * t));
   }

   public static float airAlpha(int age, int lifetime) {
      return 0.88F * smooth(age / 8.0) * smooth((lifetime - age) / 15.0);
   }

   public static float canyonAlpha(double rise) {
      return 0.86F * smooth((16.0 - rise) / 2.5);
   }

   public static double wander(int age, double phase) {
      return Math.sin(phase + age * 0.11) * 0.065 + Math.sin(phase * 1.7 + age * 0.047) * 0.035;
   }
}
