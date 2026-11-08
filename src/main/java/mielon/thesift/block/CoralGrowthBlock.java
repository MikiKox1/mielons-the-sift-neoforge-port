package mielon.thesift.block;

import mielon.thesift.worldgen.SiftPlantPatches;
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

public final class CoralGrowthBlock extends Block implements BonemealableBlock {
   private final boolean bright;

   public CoralGrowthBlock(boolean bright, Properties properties) {
      super(properties);
      this.bright = bright;
   }

   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
      return level.getBlockState(pos.above()).isAir();
   }

   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      return true;
   }

   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      SiftPlantPatches.bonemeal(level, random, pos, state);
   }

   @Override
   public Type getType() {
      return Type.NEIGHBOR_SPREADER;
   }
}
