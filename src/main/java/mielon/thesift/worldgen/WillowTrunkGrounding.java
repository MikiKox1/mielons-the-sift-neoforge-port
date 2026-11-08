package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

final class WillowTrunkGrounding {
   private WillowTrunkGrounding() {
   }

   static List<BlockPos> plan(WorldGenLevel level, SiftWorldgenBounds bounds, List<StructureBlockInfo> logs) {
      int baseY = logs.stream().mapToInt(infox -> infox.pos().getY()).min().orElse(0);
      List<BlockPos> extensions = new ArrayList<>();

      for (StructureBlockInfo info : logs) {
         if (info.pos().getY() == baseY) {
            boolean grounded = false;
            int depth = 1;

            while (true) {
               if (depth <= 16) {
                  BlockPos pos = info.pos().below(depth);
                  if (!bounds.contains(pos) || !level.isInsideBuildHeight(pos) || !level.ensureCanWrite(pos)) {
                     return null;
                  }

                  BlockState state = level.getBlockState(pos);
                  if (!state.getFluidState().isEmpty()) {
                     return null;
                  }

                  if (!state.isFaceSturdy(level, pos, Direction.UP) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                     if (!state.getCollisionShape(level, pos).isEmpty()) {
                        return null;
                     }

                     extensions.add(pos);
                     depth++;
                     continue;
                  }

                  grounded = true;
               }

               if (!grounded) {
                  return null;
               }
               break;
            }
         }
      }

      return extensions;
   }

   static int place(WorldGenLevel level, List<BlockPos> extensions, Block log) {
      for (BlockPos pos : extensions) {
         level.setBlock(pos, log.defaultBlockState(), 2);
      }

      return extensions.size();
   }
}
