package mielon.thesift.block;

import mielon.thesift.entity.MonarchRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusFlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class SculkflowerBlock extends CactusFlowerBlock {
   public SculkflowerBlock(Properties properties) {
      super(properties);
   }

   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos floorPos) {
      return floor.is(ModBlocks.ROYAL_SCULK)
         || floor.is(Blocks.FARMLAND)
         || floor.is(Blocks.SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
         || super.mayPlaceOn(floor, level, floorPos);
   }

   @Override
   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(state, level, pos, oldState, moving);
      if (!oldState.is(state.getBlock()) && level instanceof ServerLevel server) {
         MonarchRitual.trySummon(server, pos);
      }
   }
}
