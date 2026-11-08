package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class OvergrownWillowVinesBlock extends VineBlock {
   public static final BooleanProperty CLIPPED = BooleanProperty.create("clipped");
   public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;
   public static final BooleanProperty NORTH_BOTTOM = BooleanProperty.create("north_bottom");
   public static final BooleanProperty EAST_BOTTOM = BooleanProperty.create("east_bottom");
   public static final BooleanProperty SOUTH_BOTTOM = BooleanProperty.create("south_bottom");
   public static final BooleanProperty WEST_BOTTOM = BooleanProperty.create("west_bottom");

   public OvergrownWillowVinesBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         this.defaultBlockState()
            .setValue(CLIPPED, false)
            .setValue(BOTTOM, true)
            .setValue(NORTH_BOTTOM, true)
            .setValue(EAST_BOTTOM, true)
            .setValue(SOUTH_BOTTOM, true)
            .setValue(WEST_BOTTOM, true)
      );
   }

   @Override
   public boolean isRandomlyTicking(BlockState state) {
      return !state.getValue(CLIPPED) && super.isRandomlyTicking(state);
   }

   @Override
   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!state.getValue(CLIPPED)) {
         super.randomTick(state, level, pos, random);
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState placed = super.getStateForPlacement(context);
      return placed == null ? null : refreshBottomFaces(placed, context.getLevel().getBlockState(context.getClickedPos().below()));
   }

   @Override
   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess scheduledTickAccess,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      RandomSource random
   ) {
      BlockState updated = supportedState(state, level, pos);
      if (updated.isAir()) {
         return updated;
      }

      BlockState below = direction == Direction.DOWN ? neighborState : level.getBlockState(pos.below());
      return refreshBottomFaces(updated, below);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(CLIPPED, BOTTOM, NORTH_BOTTOM, EAST_BOTTOM, SOUTH_BOTTOM, WEST_BOTTOM);
   }

   public static BlockState refreshBottomFaces(BlockState state, BlockState below) {
      if (!isVineSegment(state)) {
         return state;
      }

      boolean everyActiveFaceEndsHere = true;

      for (Direction direction : Plane.HORIZONTAL) {
         BooleanProperty face = VineBlock.getPropertyForFace(direction);
         boolean active = state.hasProperty(face) && state.getValue(face);
         boolean continues = active && hasFace(state, below, direction);
         state = state.setValue(getBottomProperty(direction), !continues);
         if (continues) {
            everyActiveFaceEndsHere = false;
         }
      }

      return state.setValue(BOTTOM, everyActiveFaceEndsHere);
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rotation) {
      BlockState result = super.rotate(state, rotation);

      for (Direction face : Plane.HORIZONTAL) {
         result = result.setValue(getBottomProperty(rotation.rotate(face)), state.getValue(getBottomProperty(face)));
      }

      return result;
   }

   @Override
   public BlockState mirror(BlockState state, Mirror mirror) {
      BlockState result = super.mirror(state, mirror);

      for (Direction face : Plane.HORIZONTAL) {
         result = result.setValue(getBottomProperty(mirror.mirror(face)), state.getValue(getBottomProperty(face)));
      }

      return result;
   }

   public static BooleanProperty getBottomProperty(Direction direction) {
      return switch (direction) {
         case NORTH -> NORTH_BOTTOM;
         case EAST -> EAST_BOTTOM;
         case SOUTH -> SOUTH_BOTTOM;
         case WEST -> WEST_BOTTOM;
         default -> throw new IllegalArgumentException("Vine bottom is only defined for horizontal faces");
      };
   }

   public static boolean acceptsSupport(BlockGetter level, BlockPos pos, Direction face) {
      BlockState support = level.getBlockState(pos);
      return support.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE) || support.is(ModBlocks.ASH_WILLOW_FOLIAGE) || VineBlock.isAcceptableNeighbour(level, pos, face);
   }

   public static BlockState supportedState(BlockState state, BlockGetter level, BlockPos pos) {
      boolean roof = acceptsSupport(level, pos.above(), Direction.DOWN);
      BlockState above = level.getBlockState(pos.above());
      boolean hanging = sameFamily(state, above);
      boolean survives = roof || hanging;
      state = state.setValue(UP, roof);

      for (Direction direction : Plane.HORIZONTAL) {
         BooleanProperty face = VineBlock.getPropertyForFace(direction);
         boolean active = state.getValue(face)
            && (roof || hanging || acceptsSupport(level, pos.relative(direction), direction) || hasFace(state, above, direction));
         state = state.setValue(face, active);
         survives |= active;
      }

      return survives ? refreshBottomFaces(state, level.getBlockState(pos.below())) : Blocks.AIR.defaultBlockState();
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return !supportedState(state, level, pos).isAir();
   }

   private static boolean sameFamily(BlockState first, BlockState second) {
      if (!isVineSegment(second)) {
         return false;
      }

      boolean firstAsh = first.is(ModBlocks.ASH_WILLOW_VINES) || first.is(ModBlocks.ASH_WILLOW_VINES_PLANT);
      boolean secondAsh = second.is(ModBlocks.ASH_WILLOW_VINES) || second.is(ModBlocks.ASH_WILLOW_VINES_PLANT);
      return firstAsh == secondAsh;
   }

   private static boolean hasFace(BlockState current, BlockState state, Direction direction) {
      BooleanProperty face = VineBlock.getPropertyForFace(direction);
      boolean currentAsh = current.is(ModBlocks.ASH_WILLOW_VINES) || current.is(ModBlocks.ASH_WILLOW_VINES_PLANT);
      boolean belowAsh = state.is(ModBlocks.ASH_WILLOW_VINES) || state.is(ModBlocks.ASH_WILLOW_VINES_PLANT);
      return currentAsh == belowAsh && isVineSegment(state) && state.hasProperty(face) && state.getValue(face);
   }

   private static boolean isVineSegment(BlockState state) {
      return state.getBlock() instanceof OvergrownWillowVinesBlock;
   }
}
