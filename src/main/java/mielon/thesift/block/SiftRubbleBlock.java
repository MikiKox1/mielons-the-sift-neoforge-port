package mielon.thesift.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SiftRubbleBlock extends FallingBlock {
   public static final MapCodec<SiftRubbleBlock> CODEC = simpleCodec(SiftRubbleBlock::new);

   public SiftRubbleBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends FallingBlock> codec() {
      return CODEC;
   }

   @Override
   public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
      return 6453107;
   }
}
