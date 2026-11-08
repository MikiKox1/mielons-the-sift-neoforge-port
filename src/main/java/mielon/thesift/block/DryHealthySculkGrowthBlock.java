package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class DryHealthySculkGrowthBlock extends SiftBonemealBlock {
   public DryHealthySculkGrowthBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos abovePos = pos.above();
      BlockState above = level.getBlockState(abovePos);
      if (!above.getFluidState().isEmpty() || above.canOcclude() && above.isCollisionShapeFullBlock(level, abovePos)) {
         level.setBlockAndUpdate(pos, ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState());
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess scheduledTickAccess,
      BlockPos pos,
      Direction direction,
      BlockPos neighbourPos,
      BlockState neighbourState,
      RandomSource random
   ) {
      return direction != Direction.UP
            || (!neighbourState.canOcclude() || !neighbourState.isCollisionShapeFullBlock(level, neighbourPos)) && neighbourState.getFluidState().isEmpty()
         ? super.updateShape(state, level, scheduledTickAccess, pos, direction, neighbourPos, neighbourState, random)
         : ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState();
   }
}
