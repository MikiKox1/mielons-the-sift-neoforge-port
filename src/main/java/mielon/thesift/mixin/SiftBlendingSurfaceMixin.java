package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftTerrainUpgrade;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlendingData.class)
public abstract class SiftBlendingSurfaceMixin {
   @Inject(method = "getHeightAtXZ", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$measureSiftSurface(ChunkAccess chunk, int x, int z, CallbackInfoReturnable<Integer> cir) {
      if (SiftTerrainUpgrade.legacy(chunk)) {
         cir.setReturnValue(SiftTerrainUpgrade.surface(chunk, x, z));
      }
   }
}
