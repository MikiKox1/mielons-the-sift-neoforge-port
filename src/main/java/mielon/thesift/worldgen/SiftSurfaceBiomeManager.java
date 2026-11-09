package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SiftSurfaceBiomeManager extends BiomeManager {
   private final BiomeManager original;
   private final ChunkAccess chunk;
   private final long seed;
   private int lastX = Integer.MIN_VALUE;
   private int lastZ = Integer.MIN_VALUE;
   private double offsetX;
   private double offsetZ;

   public SiftSurfaceBiomeManager(BiomeManager original, ChunkAccess chunk, long seed) {
      super(original::getNoiseBiomeAtQuart, 0L);
      this.original = original;
      this.chunk = chunk;
      this.seed = seed;
   }

   public Holder<Biome> getBiome(BlockPos pos) {
      int x = pos.getX();
      int z = pos.getZ();
      int depth = this.chunk.getHeight(Types.WORLD_SURFACE_WG, x & 15, z & 15) - pos.getY();
      if (depth >= 8) {
         return this.original.getBiome(pos);
      }

      if (x != this.lastX || z != this.lastZ) {
         this.lastX = x;
         this.lastZ = z;
         this.offsetX = offset(x, z, this.seed);
         this.offsetZ = offset(x, z, this.seed ^ 6620516959819538809L);
      }

      double strength = Math.min(1.0, (8 - depth) / 4.0);
      return this.original.getBiome(pos.offset((int)Math.round(this.offsetX * strength), 0, (int)Math.round(this.offsetZ * strength)));
   }

   private static double offset(int x, int z, long seed) {
      return 5.0 * SiftOrganicNoise.sample(seed, x / 18.0, 0.0, z / 18.0) + 2.0 * SiftOrganicNoise.sample(seed, x / 4.5, 13.0, z / 4.5);
   }
}
