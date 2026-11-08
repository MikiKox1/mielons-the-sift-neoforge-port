package mielon.thesift.mixin;

import mielon.thesift.block.RoyalSculkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class RoyalSculkActivationMixin {
   @Inject(method = "updatePOIOnBlockStateChange", at = @At("RETURN"), require = 1)
   private void theSift$refreshPedestal(BlockPos pos, BlockState previous, BlockState current, CallbackInfo ci) {
      RoyalSculkBlock.onBlockChanged((ServerLevel)(Object)this, pos, previous, current);
   }
}
