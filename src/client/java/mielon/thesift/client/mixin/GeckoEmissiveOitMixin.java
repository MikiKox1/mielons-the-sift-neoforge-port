package mielon.thesift.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mielon.thesift.client.render.SiftGeoEmissivePipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup.RenderSetupBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer$EmissiveRenderType", remap = false)
public abstract class GeckoEmissiveOitMixin {
   @ModifyExpressionValue(
      method = "buildNewInstance",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/rendertype/RenderSetup;builder(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)Lnet/minecraft/client/renderer/rendertype/RenderSetup$RenderSetupBuilder;"
      ),
      remap = false
   )
   private static RenderSetupBuilder theSift$emissiveOit(RenderSetupBuilder builder) {
      return builder.setOitPipelines(SiftGeoEmissivePipelines.EMISSIVE);
   }
}
