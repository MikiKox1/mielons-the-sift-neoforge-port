package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class SiftStemMixins {
   private SiftStemMixins() {
   }

   @Mixin(AttachedStemBlock.class)
   public abstract static class Attached {
      @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
      private void theSift$allowHealthySoil(BlockState floor, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
         if (floor.is(ModBlocks.HEALTHY_SCULK)
            || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
            || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
            || floor.is(ModBlocks.DRY_HEALTHY_SCULK)) {
            cir.setReturnValue(true);
         }
      }
   }

   @Mixin(StemBlock.class)
   public abstract static class Growing {
      @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
      private void theSift$allowHealthySoil(BlockState floor, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
         if (floor.is(ModBlocks.HEALTHY_SCULK)
            || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
            || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
            || floor.is(ModBlocks.DRY_HEALTHY_SCULK)) {
            cir.setReturnValue(true);
         }
      }
   }
}
