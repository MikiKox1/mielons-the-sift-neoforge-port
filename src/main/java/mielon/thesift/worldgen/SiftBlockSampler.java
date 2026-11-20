package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

final class SiftBlockSampler {
   private final WorldGenLevel level;
   private final ChunkAccess chunk;
   private final int chunkX;
   private final int chunkZ;
   private final MutableBlockPos cursor = new MutableBlockPos();

   SiftBlockSampler(WorldGenLevel level, int x, int z) {
      this.level = level;
      this.chunkX = x >> 4;
      this.chunkZ = z >> 4;
      this.chunk = level.getChunk(this.chunkX, this.chunkZ);
   }

   ChunkAccess chunk() {
      return this.chunk;
   }

   BlockState get(int x, int y, int z) {
      return this.get(this.cursor.set(x, y, z));
   }

   BlockState get(BlockPos pos) {
      return pos.getX() >> 4 == this.chunkX && pos.getZ() >> 4 == this.chunkZ ? this.chunk.getBlockState(pos) : this.level.getBlockState(pos);
   }
}
