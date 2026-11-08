package mielon.thesift.block;

import mielon.thesift.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SculkflowerCropBlock extends CropBlock {
   public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
   private static final VoxelShape[] SHAPES = new VoxelShape[]{Block.column(10.0, 0.0, 4.0), Block.column(14.0, 0.0, 13.0), Block.column(16.0, 0.0, 16.0)};

   public SculkflowerCropBlock(Properties properties) {
      super(properties);
   }

   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   @Override
   public int getMaxAge() {
      return 2;
   }

   @Override
   public BlockState getStateForAge(int age) {
      return age >= 2 ? ModBlocks.SCULKFLOWER.defaultBlockState() : this.defaultBlockState().setValue(AGE, age);
   }

   @Override
   protected int getBonemealAgeIncrease(Level level) {
      return 1;
   }

   @Override
   protected boolean isRandomlyTicking(BlockState state) {
      return this.getAge(state) >= 2 || super.isRandomlyTicking(state);
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (this.getAge(state) >= 2) {
         level.setBlockAndUpdate(pos, ModBlocks.SCULKFLOWER.defaultBlockState());
      } else {
         super.randomTick(state, level, pos, random);
      }
   }

   @Override
   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos floorPos) {
      return floor.is(ModBlocks.ROYAL_SCULK)
         || floor.is(Blocks.FARMLAND)
         || floor.is(Blocks.SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   @Override
   protected ItemLike getBaseSeedId() {
      return ModItems.SCULKFLOWER_SEEDS;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[this.getAge(state)];
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }
}
