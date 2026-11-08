package mielon.thesift.worldgen;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class CorruptedSiftStone {
   private CorruptedSiftStone() {
   }

   public static BlockState at(BlockPos pos, long seed) {
      double x = pos.getX();
      double y = pos.getY();
      double z = pos.getZ();
      double u = (x * 0.8660254 + z * 0.5) / 10.5;
      double v = (y + x * 0.19 - z * 0.13) / 8.5;
      double w = (z * 0.8660254 - x * 0.5 + y * 0.17) / 10.5;
      double shape = SiftOrganicNoise.sample(seed, u, v, w) * 0.78
         + SiftOrganicNoise.sample(seed ^ 6004511400780386114L, u * 2.17 + 31.0, v * 2.17 - 17.0, w * 2.17 + 11.0) * 0.22;
      return (shape < 0.089 ? ModBlocks.SOULSHALE : ModBlocks.SIFTSLATE).defaultBlockState();
   }
}
