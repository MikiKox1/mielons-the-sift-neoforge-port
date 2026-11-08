package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class SiftslateGrowthBlock extends SiftBonemealBlock {
   public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

   public SiftslateGrowthBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(SNOWY, false));
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      BlockPos abovePos = pos.above();
      BlockState above = level.getBlockState(abovePos);
      boolean ichorSnowCover = isIchorSnowCover(above);
      boolean submerged = !above.getFluidState().isEmpty();
      boolean coveredByOpaqueBlock = above.canOcclude() && above.isCollisionShapeFullBlock(level, abovePos);
      if (ichorSnowCover || !submerged && !coveredByOpaqueBlock) {
         if (state.getValue(SNOWY) != ichorSnowCover) {
            level.setBlockAndUpdate(pos, state.setValue(SNOWY, ichorSnowCover));
         }
      } else {
         level.setBlockAndUpdate(pos, this.baseBlock().defaultBlockState());
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
      if (direction == Direction.UP) {
         if (isIchorSnowCover(neighbourState)) {
            return state.setValue(SNOWY, true);
         } else {
            return (!neighbourState.canOcclude() || !neighbourState.isCollisionShapeFullBlock(level, neighbourPos)) && neighbourState.getFluidState().isEmpty()
               ? state.setValue(SNOWY, false)
               : this.baseBlock().defaultBlockState();
         }
      } else {
         return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighbourPos, neighbourState, random);
      }
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
      super.onPlace(state, level, pos, oldState, movedByPiston);
      if (!level.isClientSide()) {
         boolean snowy = isIchorSnowCover(level.getBlockState(pos.above()));
         if (state.getValue(SNOWY) != snowy) {
            level.setBlockAndUpdate(pos, state.setValue(SNOWY, snowy));
         }
      }
   }

   protected Block baseBlock() {
      return ModBlocks.SIFTSLATE;
   }

   public static boolean isIchorSnowCover(BlockState state) {
      return state.is(ModBlocks.ICHOR_SNOW) || state.is(ModBlocks.ICHOR_SNOW_BLOCK);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(SNOWY);
   }
}
