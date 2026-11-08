package mielon.thesift.block;

import com.mojang.serialization.MapCodec;
import mielon.thesift.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BlubbyBerriesBushBlock extends DoublePlantBlock implements BonemealableBlock {
   public static final BooleanProperty GROWN = BooleanProperty.create("grown");
   private static final VoxelShape SHAPE = Shapes.box(0.125, 0.0, 0.125, 0.875, 1.0, 0.875);
   public static final MapCodec<BlubbyBerriesBushBlock> CODEC = simpleCodec(BlubbyBerriesBushBlock::new);

   @Override
   public MapCodec<? extends DoublePlantBlock> codec() {
      return CODEC;
   }

   public BlubbyBerriesBushBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(GROWN, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(GROWN);
   }

   @Override
   public boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
      return super.mayPlaceOn(floor, level, pos)
         || floor.is(ModBlocks.CORAL_GROWTH)
         || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || floor.is(ModBlocks.SIFTSLATE_GROWTH)
         || floor.is(ModBlocks.SOULSHALE_GROWTH)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Override
   public boolean isRandomlyTicking(BlockState state) {
      return !state.getValue(GROWN);
   }

   @Override
   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!state.getValue(GROWN) && random.nextInt(10) == 0) {
         level.setBlock(pos, state.setValue(GROWN, true), 2);
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
      return new ItemStack(ModItems.BLUBBY_BERRIES);
   }

   public static int harvestCount(RandomSource random) {
      int roll = random.nextInt(20);
      return roll < 10 ? 1 : (roll < 16 ? 2 : (roll < 19 ? 3 : 4));
   }

   @Override
   public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!state.getValue(GROWN)) {
         return InteractionResult.PASS;
      }

      if (level instanceof ServerLevel) {
         popResource(level, pos, new ItemStack(ModItems.BLUBBY_BERRIES, harvestCount(level.getRandom())));
         BlockState harvested = state.setValue(GROWN, false);
         level.setBlock(pos, harvested, 2);
         level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, Context.of(player, harvested));
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide()) {
         BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
         BlockState other = level.getBlockState(otherPos);
         if (other.is(this) && other.getValue(HALF) != state.getValue(HALF)) {
            if (!player.getAbilities().instabuild) {
               dropResources(other, level, otherPos, null, player, player.getMainHandItem());
            }

            level.levelEvent(null, 2001, otherPos, Block.getId(other));
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 18);
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
      return !state.getValue(GROWN);
   }

   @Override
   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
      level.setBlock(pos, state.setValue(GROWN, true), 2);
   }
}
