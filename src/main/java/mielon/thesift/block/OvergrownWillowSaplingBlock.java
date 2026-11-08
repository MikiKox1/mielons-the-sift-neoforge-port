package mielon.thesift.block;

import mielon.thesift.worldgen.AshWillowTreeFeature;
import mielon.thesift.worldgen.OvergrownWillowTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OvergrownWillowSaplingBlock extends BushBlock implements BonemealableBlock {
   private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);
   private final boolean ash;

   public OvergrownWillowSaplingBlock(Properties properties) {
      this(properties, false);
   }

   public OvergrownWillowSaplingBlock(Properties properties, boolean ash) {
      super(properties);
      this.ash = ash;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
      return super.mayPlaceOn(floor, level, pos) || floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(7) == 0) {
         this.growTree(level, pos, state, random);
      }
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
      return this.ash ? AshWillowTreeFeature.hasSaplingGroup(level, pos) : OvergrownWillowTreeFeature.hasSaplingGroup(level, pos);
   }

   @Override
   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
      return random.nextFloat() < 0.45F;
   }

   @Override
   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
      this.growTree(level, pos, state, random);
   }

   private void growTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
      if (this.ash) {
         AshWillowTreeFeature.growFromSapling(level, random, pos);
      } else {
         OvergrownWillowTreeFeature.growFromSapling(level, random, pos);
      }
   }
}
