package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
   @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
   private void theSift$allowCropsOnSiftSoil(BlockState floor, BlockGetter level, BlockPos floorPos, CallbackInfoReturnable<Boolean> cir) {
      if (floor.is(ModBlocks.HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
         || (Object)this == Blocks.TORCHFLOWER_CROP && theSift$isSiftSoil(floor)) {
         cir.setReturnValue(true);
      }
   }

   @Redirect(
      method = "getGrowthSpeed",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
         ordinal = 0
      )
   )
   private static BlockState theSift$useHydratedSoilGrowth(BlockGetter level, BlockPos pos) {
      BlockState soil = level.getBlockState(pos);
      return !soil.is(ModBlocks.HEALTHY_SCULK)
            && !soil.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
            && !soil.is(ModBlocks.HEALTHY_SCULK_PORES)
            && !soil.is(ModBlocks.DRY_HEALTHY_SCULK)
         ? soil
         : Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7);
   }

   private static boolean theSift$isSiftSoil(BlockState floor) {
      return floor.is(ModBlocks.SIFTSLATE)
         || floor.is(ModBlocks.SIFTSLATE_GROWTH)
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
