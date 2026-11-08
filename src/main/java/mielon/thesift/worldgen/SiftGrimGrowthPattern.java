package mielon.thesift.worldgen;

public final class SiftGrimGrowthPattern {
   private final long seed;

   public SiftGrimGrowthPattern(long seed) {
      this.seed = seed ^ 25193109265714L;
   }

   public boolean grows(int x, int z) {
      return this.path(x, z) || this.pocket(x, z);
   }

   public boolean path(int x, int z) {
      double wx = x + 3.2 * SiftTerrainShapeNoise.noise(this.seed + 17L, x * 0.8 + z * 0.6, 26.0);
      double wz = z + 3.2 * SiftTerrainShapeNoise.noise(this.seed + 29L, z * 0.8 - x * 0.6, 26.0);
      int cx = (int)Math.floor(wx / 32.0);
      int cz = (int)Math.floor(wz / 32.0);
      double first = Double.POSITIVE_INFINITY;
      double second = Double.POSITIVE_INFINITY;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long salt = this.seed + (cx + dx) * -7046029254386353131L + (cz + dz) * 7146057691288625177L;
            double px = (cx + dx) * 32 + 6 + 20.0 * SiftTerrainShapeNoise.unit(salt + 41L);
            double pz = (cz + dz) * 32 + 6 + 20.0 * SiftTerrainShapeNoise.unit(salt + 53L);
            double distance = Math.hypot(wx - px, wz - pz);
            if (distance < first) {
               second = first;
               first = distance;
            } else if (distance < second) {
               second = distance;
            }
         }
      }

      return second - first < 2.9;
   }

   public boolean pocket(int x, int z) {
      int cx = Math.floorDiv(x, 32);
      int cz = Math.floorDiv(z, 32);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long salt = this.seed ^ (cx + dx) * 1906678578L ^ (cz + dz) * 324508639L;
            if (!(SiftTerrainShapeNoise.unit(salt + 61L) < 0.4)) {
               double px = (cx + dx) * 32 + 5 + 22.0 * SiftTerrainShapeNoise.unit(salt + 73L);
               double pz = (cz + dz) * 32 + 5 + 22.0 * SiftTerrainShapeNoise.unit(salt + 89L);
               double rx = 4.5 + 3.0 * SiftTerrainShapeNoise.unit(salt + 101L);
               double rz = 4.5 + 3.0 * SiftTerrainShapeNoise.unit(salt + 127L);
               if (Math.pow((x - px) / rx, 2.0) + Math.pow((z - pz) / rz, 2.0) < 1.0) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean spaghettiGrowth(int x, int z) {
      double field = SiftTerrainShapeNoise.noise(this.seed + 191L, x * 0.8 + z * 0.6, 28.0)
         + SiftTerrainShapeNoise.noise(this.seed + 211L, z * 0.8 - x * 0.6, 28.0)
         + 0.35 * SiftTerrainShapeNoise.noise(this.seed + 229L, x + z * 0.35, 11.0);
      return field > 0.0;
   }

   public boolean dry(int x, int z, boolean spaghetti) {
      long drySeed = this.seed ^ 26432132145L;
      if (spaghetti && !this.spaghettiGrowth(x, z)) {
         double bend = 4.0 * SiftTerrainShapeNoise.noise(drySeed + 17L, z, 24.0);
         double field = SiftTerrainShapeNoise.noise(drySeed + 31L, x + bend + z * 0.28, 14.0);
         if (Math.abs(field) < 0.16) {
            return true;
         }
      }

      int cx = Math.floorDiv(x, 48);
      int cz = Math.floorDiv(z, 48);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long salt = drySeed ^ (cx + dx) * 1906678578L ^ (cz + dz) * 324508639L;
            if (!(SiftTerrainShapeNoise.unit(salt + 61L) < 0.74)) {
               double px = (cx + dx) * 48 + 8 + 32.0 * SiftTerrainShapeNoise.unit(salt + 73L);
               double pz = (cz + dz) * 48 + 8 + 32.0 * SiftTerrainShapeNoise.unit(salt + 89L);
               double rx = 4.0 + 3.0 * SiftTerrainShapeNoise.unit(salt + 101L);
               double rz = 4.0 + 3.0 * SiftTerrainShapeNoise.unit(salt + 127L);
               if (Math.pow((x - px) / rx, 2.0) + Math.pow((z - pz) / rz, 2.0) < 1.0) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
