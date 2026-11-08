package mielon.thesift.worldgen;

public final class OrganicDesertShape {
   private OrganicDesertShape() {
   }

   public static int ground(long seed, int x, int z) {
      if (patch(seed ^ 4477529L, x, z, 56, 0.32, 4.0, 7.0)) {
         return 2;
      } else {
         return !porePath(seed, x, z) && !patch(seed ^ 1112297282L, x, z, 32, 0.65, 4.0, 7.5) ? 0 : 1;
      }
   }

   public static boolean porePath(long seed, int x, int z) {
      double wx = x + 5.0 * noise(seed + 11L, x, z, 27.0);
      double wz = z + 5.0 * noise(seed + 23L, x, z, 27.0);
      return Math.abs(noise(seed ^ 344928175443L, wx, wz, 38.0)) < 0.075;
   }

   public static boolean poreEdge(long seed, int x, int z) {
      if (ground(seed, x, z) == 1) {
         return false;
      }

      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= 3 && porePath(seed, x + dx, z + dz) && ground(seed, x + dx, z + dz) == 1) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean lake(long seed, int x, int z) {
      return patch(seed ^ 1279347525L, x, z, 48, 0.85, 6.5, 11.0);
   }

   public static int waterDepth(long seed, int x, int z) {
      int horizontal = horizontalRiverDepth(seed, x, z);
      if (horizontal > 0) {
         return horizontal;
      }

      int column = Math.floorDiv(x, 128);

      for (int dx = -1; dx <= 1; dx++) {
         long salt = seed ^ (column + dx) * -7046029254386353131L;
         if (!(SiftTerrainShapeNoise.unit(salt + 91L) < 0.55) && riverSegment(salt + 123L, z, 64, 88)) {
            double phase = SiftTerrainShapeNoise.unit(salt + 17L) * Math.PI * 2.0;
            double center = (column + dx) * 128 + 64 + 15.0 * Math.sin(z / 35.0 + phase) + 5.0 * Math.sin(z / 15.0);
            if (Math.abs(x - center) <= 1.6 + 0.7 * (0.5 + 0.5 * Math.sin(z / 39.0 + phase))) {
               return 1;
            }
         }
      }

      return lake(seed, x, z) ? 1 : 0;
   }

   public static int horizontalRiverDepth(long seed, int x, int z) {
      int row = Math.floorDiv(z, 128);

      for (int dz = -1; dz <= 1; dz++) {
         long salt = seed ^ (row + dz) * 7146057691288625177L;
         if (riverSegment(salt + 123L, x, 72, 96)) {
            double phase = SiftTerrainShapeNoise.unit(salt) * Math.PI * 2.0;
            double center = (row + dz) * 128 + 64 + 17.0 * Math.sin(x / 31.0 + phase) + 5.0 * Math.sin(x / 13.0 - phase);
            double width = 1.55 + 0.8 * (0.5 + 0.5 * Math.sin(x / 43.0 + phase));
            double distance = Math.abs(z - center);
            if (distance <= width) {
               return width > 2.15 && distance < 0.65 ? 2 : 1;
            }
         }
      }

      return 0;
   }

   private static boolean riverSegment(long salt, int coordinate, int low, int high) {
      int shift = (int)(SiftTerrainShapeNoise.unit(salt) * 128.0);
      int reach = Math.floorDiv(coordinate + shift, 128);
      int length = low + (int)(SiftTerrainShapeNoise.unit(salt ^ reach * -7723592293110705685L) * (high - low + 1));
      return Math.floorMod(coordinate + shift, 128) < length;
   }

   private static boolean patch(long seed, int x, int z, int size, double chance, double low, double high) {
      int cellX = Math.floorDiv(x, size);
      int cellZ = Math.floorDiv(z, size);
      double warpX = 1.7 * noise(seed + 1L, x, z, 8.0);
      double warpZ = 1.7 * noise(seed + 2L, x, z, 8.0);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long salt = seed ^ (cellX + dx) * -7046029254386353131L ^ (cellZ + dz) * 7146057691288625177L;
            if (!(SiftTerrainShapeNoise.unit(salt + 31L) > chance)) {
               double cx = (cellX + dx) * size + size * (0.25 + 0.5 * SiftTerrainShapeNoise.unit(salt + 47L));
               double cz = (cellZ + dz) * size + size * (0.25 + 0.5 * SiftTerrainShapeNoise.unit(salt + 59L));
               double rx = low + (high - low) * SiftTerrainShapeNoise.unit(salt + 71L);
               double rz = low + (high - low) * SiftTerrainShapeNoise.unit(salt + 83L);
               double nx = (x + warpX - cx) / rx;
               double nz = (z + warpZ - cz) / rz;
               if (nx * nx + nz * nz < 1.0 + 0.15 * noise(seed + 97L, x, z, 5.0)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static double noise(long seed, double x, double z, double scale) {
      double px = x / scale;
      double pz = z / scale;
      long ix = (long)Math.floor(px);
      long iz = (long)Math.floor(pz);
      double tx = px - ix;
      double tz = pz - iz;
      tx = tx * tx * (3.0 - 2.0 * tx);
      tz = tz * tz * (3.0 - 2.0 * tz);
      double a = value(seed, ix, iz);
      double b = value(seed, ix + 1L, iz);
      double c = value(seed, ix, iz + 1L);
      double d = value(seed, ix + 1L, iz + 1L);
      return a + (b - a) * tx + (c + (d - c) * tx - a - (b - a) * tx) * tz;
   }

   private static double value(long seed, long x, long z) {
      return SiftTerrainShapeNoise.unit(seed ^ x * -7046029254386353131L ^ z * 7146057691288625177L) * 2.0 - 1.0;
   }
}
