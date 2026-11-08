package mielon.thesift.worldgen;

public final class SiftTerrainShapeNoise {
   private SiftTerrainShapeNoise() {
   }

   public static long hash(long value) {
      value = (value ^ value >>> 30) * -4658895280553007687L;
      value = (value ^ value >>> 27) * -7723592293110705685L;
      return value ^ value >>> 31;
   }

   public static double unit(long value) {
      return (hash(value) >>> 11) * 1.110223E-16F;
   }

   public static double noise(long salt, double coordinate, double scale) {
      double position = coordinate / scale;
      long cell = (long)Math.floor(position);
      double t = position - cell;
      t = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
      double a = unit(salt + cell * -7046029254386353131L);
      double b = unit(salt + (cell + 1L) * -7046029254386353131L);
      return (a + (b - a) * t) * 2.0 - 1.0;
   }
}
