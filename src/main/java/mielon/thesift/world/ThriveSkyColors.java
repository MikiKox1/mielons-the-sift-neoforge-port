package mielon.thesift.world;

public final class ThriveSkyColors {
   public static final int SUNRISE_COLOR = 1609540403;

   private ThriveSkyColors() {
   }

   public static float[] color(float longitude, float elevationDegrees) {
      double u = longitude * Math.PI * 2.0;
      double v = (elevationDegrees + 14.0F) / 104.0;
      double first = 0.18 + 0.12 * Math.sin(u * 2.0 + 0.6) + 0.035 * Math.sin(u * 4.0 - 0.8);
      double second = 0.5 + 0.16 * Math.sin(u - 1.2) + 0.035 * Math.sin(u * 3.0 + 0.4);
      double bands = Math.max(band(v, first, 0.105), band(v, second, 0.095));
      float blend = (float)(0.72 * bands * smooth((90.0F - elevationDegrees) / 25.0F) * smooth((elevationDegrees + 70.0F) / 45.0F));
      return new float[]{mix(96.0F, 190.0F, blend) / 255.0F, mix(213.0F, 222.0F, blend) / 255.0F, mix(200.0F, 150.0F, blend) / 255.0F, 1.0F};
   }

   public static float[] sunrise(float elevationDegrees) {
      return new float[]{0.9372549F, 0.6392157F, 0.2F, (float)(0.37254901960784315 * smooth(1.0 - Math.abs(elevationDegrees) / 22.0))};
   }

   private static double band(double v, double center, double width) {
      return smooth(1.0 - Math.abs(v - center) / width);
   }

   private static double smooth(double value) {
      double t = Math.max(0.0, Math.min(1.0, value));
      return t * t * (3.0 - 2.0 * t);
   }

   private static float mix(float a, float b, float t) {
      return a + (b - a) * t;
   }
}
