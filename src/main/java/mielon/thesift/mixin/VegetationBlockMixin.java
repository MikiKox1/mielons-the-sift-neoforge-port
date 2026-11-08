package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VegetationBlock.class)
public abstract class VegetationBlockMixin {
   @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
   private void theSift$allowMatureTorchflowerOnSiftSoil(BlockState floor, BlockGetter level, BlockPos floorPos, CallbackInfoReturnable<Boolean> cir) {
      if (theSift$isFlower(this) && theSift$isSiftSoil(floor)) {
         cir.setReturnValue(true);
      } else {
         if ((Object)this instanceof SaplingBlock
            && (floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.SOULSHALE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH))) {
            cir.setReturnValue(true);
         }
      }
   }

   private static boolean theSift$isFlower(Object block) {
      return block instanceof Block siftBlock && siftBlock.defaultBlockState().is(BlockTags.FLOWERS);
   }

   private static boolean theSift$isSiftSoil(BlockState floor) {
      return floor.is(ModBlocks.SIFTSLATE)
         || floor.is(ModBlocks.SIFTSLATE_GROWTH)
         || floor.is(ModBlocks.SOULSHALE_GROWTH)
         || floor.is(ModBlocks.HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || floor.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE)
         || floor.is(ModBlocks.ASH_WILLOW_FOLIAGE)
         || floor.is(ModBlocks.CORAL_SOIL)
         || floor.is(ModBlocks.CORAL_GROWTH)
         || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH);
   }
}
