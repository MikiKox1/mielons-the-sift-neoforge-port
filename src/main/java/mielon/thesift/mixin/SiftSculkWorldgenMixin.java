package mielon.thesift.mixin;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SculkSpreader.ChargeCursor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChargeCursor.class)
public abstract class SiftSculkWorldgenMixin {
   @Shadow
   private BlockPos pos;
   @Shadow
   private int charge;

   @Inject(method = "update", at = @At("HEAD"), cancellable = true)
   private void theSift$caveBoundary(LevelAccessor level, BlockPos origin, RandomSource random, SculkSpreader spreader, boolean spreading, CallbackInfo ci) {
      if (spreader.isWorldGeneration()
         && level instanceof WorldGenLevel generation
         && generation.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)
         && !generation.getBiome(this.pos).is(TheSiftDimension.CORRUPTED_SIFT)) {
         this.charge = 0;
         ci.cancel();
      }
   }
}
