package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeIchorSnowMixin {
   @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
   private void theSift$restrictIchorSnow(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (level instanceof ServerLevel server && server.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         BlockState state = level.getBlockState(pos);
         cir.setReturnValue(
            server.getBiome(pos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)
               && level.isInsideBuildHeight(pos.getY())
               && level.getBrightness(LightLayer.BLOCK, pos) < 10
               && (state.isAir() || state.is(ModBlocks.ICHOR_SNOW))
               && ModBlocks.ICHOR_SNOW.defaultBlockState().canSurvive(level, pos)
         );
      }
   }
}
