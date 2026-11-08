package mielon.thesift.worldgen;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;

public final class SiftIchorRavineShape {
   public static final int MAX_WIDTH = 12;
   public static final int WATER_Y = 44;
   public static final double MAX_RADIUS = 5.0;
   private static final double SPACING = 96.0;
   private final long seed;
   private final double cosine;
   private final double sine;

   public SiftIchorRavineShape(long seed) {
      this.seed = seed ^ 3649241153021998605L;
      double angle = SiftTerrainShapeNoise.unit(this.seed) * Math.PI;
      this.cosine = Math.cos(angle);
      this.sine = Math.sin(angle);
   }

   public SiftIchorRavineShape.Cut sample(int x, int z) {
      return this.sample(x, z, 0.0);
   }

   public SiftIchorRavineShape.Cut sampleWithLining(int x, int z) {
      return this.sample(x, z, 1.5);
   }

   private SiftIchorRavineShape.Cut sample(int x, int z, double lining) {
      double across = x * this.cosine + z * this.sine;
      double along = -x * this.sine + z * this.cosine;
      long band = (long)Math.floor(across / 96.0);
      SiftIchorRavineShape.Cut closest = null;

      for (long candidate = band - 1L; candidate <= band + 1L; candidate++) {
         long salt = SiftTerrainShapeNoise.hash(this.seed + candidate * -7046029254386353131L);
         double activity = SiftTerrainShapeNoise.noise(salt ^ 104291745L, along, 160.0);
         double fade = Math.max(0.0, Math.min(1.0, (activity + 0.55) / 0.3));
         if (!(fade <= 0.0)) {
            double center = candidate * 96.0
               + 48.0
               + (SiftTerrainShapeNoise.unit(salt) - 0.5) * 28.0
               + 11.0 * SiftTerrainShapeNoise.noise(salt, along, 64.0)
               + 3.0 * SiftTerrainShapeNoise.noise(salt ^ 6543383L, along, 24.0);
            double radius = fade
               * Math.min(
                  5.0, 4.2 + 0.4 * SiftTerrainShapeNoise.noise(salt ^ 3222977L, along, 24.0) + 0.25 * SiftTerrainShapeNoise.noise(salt ^ 496353L, along, 8.0)
               );
            double distance = Math.abs(across - center);
            if (!(distance >= radius + lining) && (closest == null || !(distance >= closest.distance()))) {
               int bed = 39
                  + (int)Math.round(
                     1.6 * SiftTerrainShapeNoise.noise(salt ^ 88275L, along, 24.0)
                        + 0.8 * SiftTerrainShapeNoise.noise(salt ^ 155379L, along, 7.0)
                        + 0.8 * SiftTerrainShapeNoise.noise(salt ^ 529617L, across, 4.0)
                  );
               closest = new SiftIchorRavineShape.Cut(distance, radius, bed, fade * fade * (3.0 - 2.0 * fade));
            }
         }
      }

      return closest;
   }

   public static int findBasalBank(SiftStrataSupportShape.Terrain terrain, int x, int z) {
      return findBasalBank(terrain, x, z, null);
   }

   private static int existingBank(SiftStrataSupportShape.Terrain terrain, int x, int z, Long2IntOpenHashMap cache) {
      if (cache == null) {
         return existingBank(terrain, x, z);
      }

      long key = (long)x << 32 ^ z & 4294967295L;
      int known = cache.getOrDefault(key, Integer.MAX_VALUE);
      if (known != Integer.MAX_VALUE) {
         return known;
      }

      int bank = existingBank(terrain, x, z);
      cache.put(key, bank);
      return bank;
   }

   private static int findBasalBank(SiftStrataSupportShape.Terrain terrain, int x, int z, Long2IntOpenHashMap cache) {
      int bank = existingBank(terrain, x, z, cache);
      if (bank >= 49) {
         return bank;
      }

      int[] banks = new int[24];
      int count = 0;

      for (int distance : new int[]{2, 4, 6, 8, 10, 12}) {
         for (int[] offset : new int[][]{{distance, 0}, {-distance, 0}, {0, distance}, {0, -distance}}) {
            int px = x + offset[0];
            int pz = z + offset[1];
            if (terrain.canSample(px, pz)) {
               int nearby = existingBank(terrain, px, pz, cache);
               if (nearby >= 49) {
                  banks[count++] = nearby;
               }
            }
         }
      }

      if (count < 3) {
         return Integer.MIN_VALUE;
      }

      Arrays.sort(banks, 0, count);
      bank = banks[count / 2];

      for (int y = 32; y <= bank; y++) {
         if (terrain.protectedRock(x, y, z)) {
            return Integer.MIN_VALUE;
         }
      }

      for (int y = bank; y <= bank + 3; y++) {
         if (!terrain.air(x, y, z)) {
            return Integer.MIN_VALUE;
         }
      }

      return bank;
   }

   private static int existingBank(SiftStrataSupportShape.Terrain terrain, int x, int z) {
      for (int y = 32; y <= 80; y++) {
         if (terrain.ground(x, y, z) && terrain.ground(x, y - 1, z) && terrain.ground(x, y - 2, z)) {
            boolean clear = true;

            for (int above = 1; above <= 3; above++) {
               if (!terrain.air(x, y + above, z)) {
                  clear = false;
                  break;
               }
            }

            if (clear) {
               for (int below = y - 3; below >= Math.max(32, y - 40); below--) {
                  if (terrain.protectedRock(x, below, z)) {
                     return Integer.MIN_VALUE;
                  }
               }

               return y;
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   public static final class BankSampler {
      private final SiftStrataSupportShape.Terrain terrain;
      private final Long2IntOpenHashMap banks = new Long2IntOpenHashMap();

      public BankSampler(SiftStrataSupportShape.Terrain terrain) {
         this.terrain = terrain;
      }

      public int find(int x, int z) {
         return SiftIchorRavineShape.findBasalBank(this.terrain, x, z, this.banks);
      }
   }

   public record Cut(double distance, double radius, int bedY, double depth) {
      public Cut(double distance, double radius, int bedY) {
         this(distance, radius, bedY, 1.0);
      }

      public int floorY(int bankY) {
         if (this.distance >= this.radius) {
            return bankY;
         }

         double wall = Math.max(0.0, (this.distance / this.radius - 0.68) / 0.32);
         return bankY - (int)Math.floor((bankY - this.bedY) * this.depth * (1.0 - Math.pow(wall, 0.8)));
      }
   }
}
