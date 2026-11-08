package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class HealthySculkCartilageSproutBlock extends SiftSproutBlock {
   public HealthySculkCartilageSproutBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
      return floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE);
   }
}
