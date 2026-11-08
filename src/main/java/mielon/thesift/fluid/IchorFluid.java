package mielon.thesift.fluid;

import java.util.Optional;
import mielon.thesift.entity.MonarchEntity;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class IchorFluid extends WaterFluid {
   @Override
   public Fluid getFlowing() {
      return ModFluids.FLOWING_ICHOR;
   }

   @Override
   public Fluid getSource() {
      return ModFluids.ICHOR;
   }

   @Override
   public Item getBucket() {
      return ModFluids.ICHOR_BUCKET;
   }

   @Override
   public FluidType getFluidType() {
      return ModFluids.ICHOR_TYPE;
   }

   @Override
   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(ModSounds.ICHOR_BUCKET_FILL);
   }

   @Override
   protected void spreadTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluid) {
      if (IchorWaterlogging.flowBreaksPlant(state)) {
         this.beforeDestroyingBlock(level, pos, state);
         level.setBlock(pos, this.createLegacyBlock(fluid), 3);
      } else if (!IchorWaterlogging.canIchorlog(state)) {
         super.spreadTo(level, pos, state, direction, fluid);
      }
   }

   @Override
   public BlockState createLegacyBlock(FluidState state) {
      return ModFluids.ICHOR_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
   }

   @Override
   public boolean isSame(Fluid fluid) {
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   @Override
   protected boolean canConvertToSource(ServerLevel level) {
      return false;
   }

   @Override
   public int getSlopeFindDistance(LevelReader level) {
      return 6;
   }

   @Override
   protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state) {
      FluidState next = super.getNewLiquid(level, pos, state);
      return !next.isEmpty() && next.getType().isSame(this) && !next.isSource() && !next.getValue(FALLING) && next.getAmount() > 6
         ? this.getFlowing(6, false)
         : next;
   }

   @Override
   public int getDropOff(LevelReader level) {
      return 1;
   }

   @Override
   public int getTickDelay(LevelReader level) {
      return 7;
   }

   @Override
   public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
      super.animateTick(level, pos, state, random);
      IchorSoulAmbient.animate(level, pos, state, random);
      if (state.isSource() && level.getBlockState(pos.above()).isAir() && random.nextInt(8) == 0) {
         double surfaceY = pos.getY() + state.getHeight(level, pos) + 0.00625;
         level.addParticle(
            ModParticles.ICHOR_SURFACE_MIST,
            pos.getX() + 0.1 + random.nextDouble() * 0.8,
            surfaceY,
            pos.getZ() + 0.1 + random.nextDouble() * 0.8,
            0.0,
            0.0,
            0.0
         );
      }
   }

   @Override
   protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
      super.entityInside(level, pos, entity, effectApplier);
      applyMovementDrag(entity);
   }

   public static void applyMovementDrag(Entity entity) {
      if (!(entity instanceof Projectile) && !(entity instanceof MonarchEntity)) {
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.92, 0.94, 0.92));
      }
   }

   public static final class Flowing extends IchorFluid {
      @Override
      protected void createFluidStateDefinition(Builder<Fluid, FluidState> builder) {
         super.createFluidStateDefinition(builder);
         builder.add(LEVEL);
      }

      @Override
      public int getAmount(FluidState state) {
         return state.getValue(LEVEL);
      }

      @Override
      public boolean isSource(FluidState state) {
         return false;
      }
   }

   public static final class Source extends IchorFluid {
      @Override
      public int getAmount(FluidState state) {
         return 8;
      }

      @Override
      public boolean isSource(FluidState state) {
         return true;
      }
   }
}
