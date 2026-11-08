package mielon.thesift.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import mielon.thesift.mixin.SiftBlendingDataInvoker;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.blending.BlendingData;

public final class SiftBiomeTransition {
   private static final double RANGE = 27.0;

   private SiftBiomeTransition() {
   }

   public static Holder<Biome> sample(Map<Long, BlendingData> sources, int x, int y, int z) {
      SiftBiomeTransition.Nearest nearest = new SiftBiomeTransition.Nearest();
      sources.forEach(
         (packed, data) -> ((SiftBlendingDataInvoker)data)
            .theSift$biomes(
               QuartPos.fromSection(ChunkPos.getX(packed)),
               y,
               QuartPos.fromSection(ChunkPos.getZ(packed)),
               (sx, sz, biome) -> nearest.accept(x, z, sx, sz, biome)
            )
      );
      double distance = Math.sqrt(nearest.distanceSquared);
      if (nearest.biome != null && !(distance > limit(x, z))) {
         if (distance <= 2.0) {
            return nearest.biome;
         }

         double strength = smooth(distance / 6.0) * 3.0;
         double queryX = x + strength * noise(x / 18.0 + 37.7, z / 18.0 - 21.3);
         double queryZ = z + strength * noise(x / 18.0 - 71.1, z / 18.0 + 53.6);
         Map<Holder<Biome>, Double> weights = new HashMap<>();
         sources.forEach(
            (packed, data) -> ((SiftBlendingDataInvoker)data)
               .theSift$biomes(QuartPos.fromSection(ChunkPos.getX(packed)), y, QuartPos.fromSection(ChunkPos.getZ(packed)), (sx, sz, biome) -> {
                  double radiusSquared = (double)(x - sx) * (x - sx) + (double)(z - sz) * (z - sz);
                  if (!(radiusSquared >= 729.0)) {
                     double dx = queryX - sx;
                     double dz = queryZ - sz;
                     double r2 = dx * dx + dz * dz + 1.0;
                     double falloff = 1.0 - radiusSquared / 729.0;
                     weights.merge(biome, falloff * falloff / (r2 * r2), Double::sum);
                  }
               })
         );
         Holder<Biome> result = nearest.biome;
         double largest = -1.0;

         for (Entry<Holder<Biome>, Double> entry : weights.entrySet()) {
            if (entry.getValue() > largest || entry.getValue() == largest && entry.getKey().unwrapKey().toString().compareTo(result.unwrapKey().toString()) < 0
               )
             {
               largest = entry.getValue();
               result = entry.getKey();
            }
         }

         return result;
      } else {
         return null;
      }
   }

   static double limit(double x, double z) {
      return 14.0 + 4.0 * Math.sin(x * 0.12 + z * 0.03 + 1.7) + 3.0 * Math.sin(x * 0.05 + z * 0.16 - 2.3) + 1.2 * Math.sin(x * 0.08 - z * 0.09 + 0.8);
   }

   private static double smooth(double t) {
      t = Math.max(0.0, Math.min(1.0, t));
      return t * t * (3.0 - 2.0 * t);
   }

   private static double noise(double x, double z) {
      int ix = (int)Math.floor(x);
      int iz = (int)Math.floor(z);
      double tx = smooth(x - ix);
      double tz = smooth(z - iz);
      double a = value(ix, iz);
      double b = value(ix + 1, iz);
      double c = value(ix, iz + 1);
      double d = value(ix + 1, iz + 1);
      return (a + (b - a) * tx) * (1.0 - tz) + (c + (d - c) * tx) * tz;
   }

   private static double value(int x, int z) {
      long h = x * 7146057691288625177L ^ z * -7046029254386353131L ^ 7640891576956012809L;
      h = (h ^ h >>> 30) * -4658895280553007687L;
      h = (h ^ h >>> 27) * -7723592293110705685L;
      return ((h ^ h >>> 31) >>> 11) * 2.220446E-16F - 1.0;
   }

   private static final class Nearest {
      private double distanceSquared = Double.POSITIVE_INFINITY;
      private Holder<Biome> biome;
      private int sourceX = Integer.MAX_VALUE;
      private int sourceZ = Integer.MAX_VALUE;

      private void accept(int x, int z, int sx, int sz, Holder<Biome> sample) {
         double r2 = (double)(x - sx) * (x - sx) + (double)(z - sz) * (z - sz);
         if (r2 < this.distanceSquared || r2 == this.distanceSquared && (sx < this.sourceX || sx == this.sourceX && sz < this.sourceZ)) {
            this.distanceSquared = r2;
            this.biome = sample;
            this.sourceX = sx;
            this.sourceZ = sz;
         }
      }
   }
}
