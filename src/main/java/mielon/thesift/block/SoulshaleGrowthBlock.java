package mielon.thesift.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class SoulshaleGrowthBlock extends SiftslateGrowthBlock {
   public SoulshaleGrowthBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected Block baseBlock() {
      return ModBlocks.SOULSHALE;
   }
}
