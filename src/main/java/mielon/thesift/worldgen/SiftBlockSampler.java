package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

final class SiftBlockSampler {
   private final WorldGenLevel level;
   private final ChunkAccess chunk;
   private final int chunkX;
   private final int chunkZ;

   SiftBlockSampler(WorldGenLevel level, int x, int z) {
      this.level = level;
      this.chunkX = x >> 4;
      this.chunkZ = z >> 4;
      this.chunk = level.getChunk(this.chunkX, this.chunkZ);
   }

   BlockState get(BlockPos pos) {
      return pos.getX() >> 4 == this.chunkX && pos.getZ() >> 4 == this.chunkZ ? this.chunk.getBlockState(pos) : this.level.getBlockState(pos);
   }
}
