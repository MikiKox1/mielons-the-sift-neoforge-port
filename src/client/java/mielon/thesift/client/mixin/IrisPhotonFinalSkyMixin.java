package mielon.thesift.client.mixin;

import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Iris 1.11.4 / MC26.1.2 final compositor callback.
 * The early sky is queued under Photon; restoring at LevelRenderer TAIL is
 * too early because Photon overwrites the main color target afterwards.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class IrisPhotonFinalSkyMixin {
   @Inject(method = "finalizeLevelRendering()V", at = @At("TAIL"), require = 0, remap = false)
   private void theSift$afterPhotonFinalPass(CallbackInfo ci) {
      SiftProceduralSkyRenderer.renderAfterPhotonFinalPass();
   }
}
