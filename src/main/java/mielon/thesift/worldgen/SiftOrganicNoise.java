package mielon.thesift.worldgen;

public final class SiftOrganicNoise {
   private SiftOrganicNoise() {
   }

   public static double sample(long seed, double x, double y, double z) {
      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      int iz = (int)Math.floor(z);
      double tx = fade(x - ix);
      double ty = fade(y - iy);
      double tz = fade(z - iz);
      double a = lerp(tx, corner(seed, ix, iy, iz), corner(seed, ix + 1, iy, iz));
      double b = lerp(tx, corner(seed, ix, iy + 1, iz), corner(seed, ix + 1, iy + 1, iz));
      double c = lerp(tx, corner(seed, ix, iy, iz + 1), corner(seed, ix + 1, iy, iz + 1));
      double d = lerp(tx, corner(seed, ix, iy + 1, iz + 1), corner(seed, ix + 1, iy + 1, iz + 1));
      return lerp(tz, lerp(ty, a, b), lerp(ty, c, d));
   }

   private static double corner(long seed, int x, int y, int z) {
      long h = seed + x * -7046029254386353131L + y * 7146057691288625177L + z * -7723592293110705685L;
      return (SiftTerrainShapeNoise.hash(h) >>> 11) * 2.220446E-16F - 1.0;
   }

   private static double fade(double t) {
      return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
   }

   private static double lerp(double t, double a, double b) {
      return a + (b - a) * t;
   }
}
