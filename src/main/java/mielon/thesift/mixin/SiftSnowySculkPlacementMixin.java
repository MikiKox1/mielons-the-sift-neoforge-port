package mielon.thesift.mixin;

import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftSnowyPeakSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class SiftSnowySculkPlacementMixin {
   @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
   private void theSift$protectSnow(BlockPos pos, BlockState state, int flags, int depth, CallbackInfoReturnable<Boolean> cir) {
      Block block = state.getBlock();
      if (block == Blocks.SCULK
         || block == Blocks.SCULK_VEIN
         || block == Blocks.SCULK_SENSOR
         || block == Blocks.SCULK_SHRIEKER
         || block == Blocks.SCULK_CATALYST) {
         WorldGenRegion region = (WorldGenRegion)(Object)this;
         if (region.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY) && region.ensureCanWrite(pos) && SiftSnowyPeakSurface.rejectsSculk(region, pos)) {
            cir.setReturnValue(false);
         }
      }
   }
}
