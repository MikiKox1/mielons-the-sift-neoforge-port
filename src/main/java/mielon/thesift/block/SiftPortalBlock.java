package mielon.thesift.block;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.entity.SiftPortalBlockEntity;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.SiftTeleportManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SiftPortalBlock extends BaseEntityBlock {
   public static final EnumProperty<Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 12.0);
   private static final VoxelShape Z_SHAPE = Block.box(4.0, 0.0, 0.0, 12.0, 16.0, 16.0);
   public static final MapCodec<SiftPortalBlock> CODEC = simpleCodec(SiftPortalBlock::new);

   public static boolean intersectsPortal(Entity entity, BlockPos pos, BlockState state) {
      if (!state.getValue(ACTIVE)) {
         return false;
      }

      VoxelShape shape = state.getValue(AXIS) == Axis.X ? X_SHAPE : Z_SHAPE;
      return entity.getBoundingBox().intersects(shape.bounds().move(pos));
   }

   public static boolean isActivePortal(BlockState state) {
      return state.is(ModBlocks.SIFT_PORTAL) && state.getValue(ACTIVE);
   }

   public SiftPortalBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Axis.X).setValue(ACTIVE, true));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(AXIS, ACTIVE);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SiftPortalBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModBlocks.SIFT_PORTAL_BLOCK_ENTITY, SiftPortalBlockEntity::tick);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
      if (!level.isClientSide() && intersectsPortal(entity, pos, state)) {
         SiftTeleportManager.onPortalContact(entity);
      }
   }

   @Override
   protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
      if (level.isClientSide()) {
         RandomSource random = level.getRandom();

         for (int i = 0; i < 48; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double xd = (random.nextDouble() - 0.5) * 0.18;
            double yd = (random.nextDouble() - 0.15) * 0.18;
            double zd = (random.nextDouble() - 0.5) * 0.18;
            level.addParticle(ModParticles.SIFT_PARALLAX, x, y, z, xd, yd, zd);
         }
      }
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(AXIS) == Axis.X ? X_SHAPE : Z_SHAPE;
   }
}
