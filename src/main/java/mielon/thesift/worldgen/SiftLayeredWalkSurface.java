package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

public final class SiftLayeredWalkSurface {
   private SiftLayeredWalkSurface() {
   }

   public static BlockPos findFeet(LevelReader level, int x, int z, int referenceY, int range, int headroom) {
      if (!level.hasChunkAt(new BlockPos(x, referenceY, z))) {
         return null;
      }

      for (int distance = 0; distance <= range; distance++) {
         BlockPos higher = new BlockPos(x, referenceY + distance, z);
         if (canStand(level, higher, headroom)) {
            return higher;
         }

         if (distance > 0) {
            BlockPos lower = new BlockPos(x, referenceY - distance, z);
            if (canStand(level, lower, headroom)) {
               return lower;
            }
         }
      }

      return null;
   }

   public static boolean canStand(LevelReader level, BlockPos feet, int headroom) {
      if (level.hasChunkAt(feet)
         && level.getFluidState(feet.below()).isEmpty()
         && level.getBlockState(feet.below()).isCollisionShapeFullBlock(level, feet.below())) {
         for (int dy = 0; dy < headroom; dy++) {
            BlockPos pos = feet.above(dy);
            if (!level.getFluidState(pos).isEmpty() || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
