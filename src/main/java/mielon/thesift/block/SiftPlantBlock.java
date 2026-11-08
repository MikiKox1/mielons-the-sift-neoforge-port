package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SiftPlantBlock extends BushBlock {
   public SiftPlantBlock(Properties properties) {
      super(properties);
   }

   protected SiftPlantBlock(Properties properties, boolean ignoredAllowVanillaVegetationSoil) {
      super(properties);
   }

   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
      return false;
   }

   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
      return false;
   }

   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
   }

   @Override
   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
      return floor.is(ModBlocks.CORAL_GROWTH)
         || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || floor.is(ModBlocks.CORAL_SOIL)
         || floor.is(ModBlocks.SIFTSLATE)
         || floor.is(ModBlocks.SIFTSLATE_GROWTH)
         || floor.is(ModBlocks.SOULSHALE_GROWTH)
         || floor.is(ModBlocks.HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || floor.is(ModBlocks.ASH_WILLOW_FOLIAGE)
         || floor.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE)
         || floor.is(BlockTags.SUPPORTS_VEGETATION);
   }
}
