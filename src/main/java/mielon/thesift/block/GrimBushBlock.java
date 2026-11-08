package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GrimBushBlock extends SiftPlantBlock {
   public GrimBushBlock(Properties properties) {
      super(properties);
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Blocks.DEAD_BUSH.defaultBlockState().getShape(level, pos, context);
   }

   @Override
   public boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
      return floor.is(ModBlocks.SOULSHALE)
         || floor.is(ModBlocks.SOULSHALE_GROWTH)
         || floor.is(ModBlocks.COBBLED_SIFTSLATE)
         || floor.is(ModBlocks.SIFT_RUBBLE)
         || super.mayPlaceOn(floor, level, pos);
   }
}
