package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealableBlock.Type;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SiftSpreadingBaseBlock extends Block implements BonemealableBlock {
   public SiftSpreadingBaseBlock(Properties properties) {
      super(properties);
   }

   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
      return level.getBlockState(pos.above()).isAir() && hasGrowth(level, pos, growth(state));
   }

   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      return true;
   }

   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      level.setBlockAndUpdate(pos, growth(state).defaultBlockState());
   }

   @Override
   public Type getType() {
      return Type.NEIGHBOR_SPREADER;
   }

   private static Block growth(BlockState state) {
      return state.is(ModBlocks.SOULSHALE) ? ModBlocks.SOULSHALE_GROWTH : ModBlocks.SIFTSLATE_GROWTH;
   }

   private static boolean hasGrowth(LevelReader level, BlockPos pos, Block growth) {
      for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
         if (level.getBlockState(nearby).is(growth)) {
            return true;
         }
      }

      return false;
   }
}
