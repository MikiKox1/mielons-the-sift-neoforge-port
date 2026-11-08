package mielon.thesift.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SculkVeinBlock.class)
public abstract class IchorSculkVeinMixin {
   @WrapOperation(
      method = "regrow",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
      )
   )
   private static boolean theSift$regrow(LevelAccessor level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
      FluidState fluid = level.getFluidState(pos);
      if (IchorWaterlogging.isIchor(fluid.getType())) {
         if (state.is(Blocks.SCULK_VEIN)) {
            state = IchorWaterlogging.fillState(state);
         } else if (state.is(Blocks.WATER)) {
            state = fluid.createLegacyBlock();
         }
      }

      return original.call(level, pos, state, flags);
   }

   @WrapOperation(
      method = "onDischarged",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
      )
   )
   private boolean theSift$onDischarged(LevelAccessor level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
      FluidState fluid = level.getFluidState(pos);
      if (IchorWaterlogging.isIchor(fluid.getType())) {
         if (state.is(Blocks.SCULK_VEIN)) {
            state = IchorWaterlogging.fillState(state);
         } else if (state.is(Blocks.WATER)) {
            state = fluid.createLegacyBlock();
         }
      }

      return original.call(level, pos, state, flags);
   }
}
