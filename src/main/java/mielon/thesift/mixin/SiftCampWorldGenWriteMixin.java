package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftCampProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class SiftCampWorldGenWriteMixin {
   @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
   private void sift$keepCampBlocks(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> result) {
      if (SiftCampProtection.protectsDecoration(pos)) {
         result.setReturnValue(false);
      }
   }

   @Inject(method = "ensureCanWrite", at = @At("HEAD"), cancellable = true)
   private void sift$keepCampFromOreBlobs(BlockPos pos, CallbackInfoReturnable<Boolean> result) {
      if (SiftCampProtection.protectsDecoration(pos)) {
         result.setReturnValue(false);
      }
   }
}
