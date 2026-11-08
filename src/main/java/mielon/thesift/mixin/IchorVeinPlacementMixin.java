package mielon.thesift.mixin;

import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultifaceBlock.class)
public abstract class IchorVeinPlacementMixin {
   @Inject(
      method = "getStateForPlacement(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/level/block/state/BlockState;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void theSift$preserveVeinFluid(BlockState previous, BlockGetter level, BlockPos pos, Direction face, CallbackInfoReturnable<BlockState> cir) {
      BlockState result = cir.getReturnValue();
      if (result != null && result.is(Blocks.SCULK_VEIN) && IchorWaterlogging.isIchor(level.getFluidState(pos).getType())) {
         cir.setReturnValue(IchorWaterlogging.fillState(result));
      }
   }
}
