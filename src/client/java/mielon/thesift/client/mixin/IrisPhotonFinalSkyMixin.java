package mielon.thesift.client.mixin;

import mielon.thesift.client.render.SiftShaderSkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Iris class linkage is allowed in an optional callback signature. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class IrisPhotonFinalSkyMixin {
   @Inject(method = "finalizeLevelRendering()V", at = @At("TAIL"), require = 0, remap = false)
   private void theSift$afterPhotonFinal(CallbackInfo ci) {
      SiftShaderSkyRenderer.afterPhotonFinal();
   }
}
