package mielon.thesift.worldgen;

public final class SiftCaveLayers {
   private final long seed;
   private final int[] heights = new int[36];

   public SiftCaveLayers(long seed) {
      this.seed = seed ^ 2119550791782780511L;
      int height = -24;

      for (int index = 0; index < this.heights.length; index++) {
         height += 9 + (int)(SiftTerrainShapeNoise.unit(this.seed + index * 71L) * 3.0);
         this.heights[index] = height;
      }
   }

   public int[] column(int x, int z, int minY, int maxY) {
      int[] result = new int[maxY - minY];
      double foldedX = x * 0.8 + z * 0.6;
      double foldedZ = z * 0.8 - x * 0.6;
      double fold = 1.15 * field(this.seed, foldedX + 113.0, foldedZ - 97.0, 72.0);
      int previous = Integer.MIN_VALUE;

      for (int index = 0; index < this.heights.length; index++) {
         int relief = (int)Math.round(fold + 0.85 * field(this.seed + index * 33721L, foldedX, foldedZ, 28.0));
         int center = this.heights[index] + relief;
         if (previous != Integer.MIN_VALUE) {
            center = Math.max(previous + 8, Math.min(previous + 12, center));
         }

         previous = center;
         if (center >= minY && center < maxY) {
            result[center - minY] = index + 1;
         }
      }

      return result;
   }

   public boolean dryFloor(int x, int z) {
      return field(this.seed ^ 37179L, x, z, 36.0) > 0.48;
   }

   public int floorStyle(int x, int y, int z) {
      int layer = (int)Math.floor((y + 2.0 * field(this.seed ^ 29601L, x, z, 80.0)) / 12.0);
      return (int)(SiftTerrainShapeNoise.unit(this.seed + layer * -7046029254386353131L) * 5.0);
   }

   public float vegetationDensity(int x, int y, int z) {
      return (float)(0.84 + 0.2 * field(this.seed ^ y / 16 * 25273L, x, z, 18.0));
   }

   private static double field(long salt, double x, double z, double scale) {
      double px = x / scale;
      double pz = z / scale;
      long gx = (long)Math.floor(px);
      long gz = (long)Math.floor(pz);
      double tx = smooth(px - gx);
      double tz = smooth(pz - gz);
      double a = cell(salt, gx, gz);
      double b = cell(salt, gx + 1L, gz);
      double c = cell(salt, gx, gz + 1L);
      double d = cell(salt, gx + 1L, gz + 1L);
      return (a + (b - a) * tx) * (1.0 - tz) + (c + (d - c) * tx) * tz;
   }

   private static double cell(long salt, long x, long z) {
      return SiftTerrainShapeNoise.unit(salt + x * -7046029254386353131L + z * 7146057691288625177L) * 2.0 - 1.0;
   }

   private static double smooth(double t) {
      return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
   }
}
