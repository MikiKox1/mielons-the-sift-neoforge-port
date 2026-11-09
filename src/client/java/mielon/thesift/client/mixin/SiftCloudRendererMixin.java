package mielon.thesift.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.SiftCloudRenderPipelines;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CloudRenderer.class)
public abstract class SiftCloudRendererMixin {
   @ModifyArg(
      method = "render",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"),
      index = 0
   )
   private RenderPipeline theSift$useAnimatedIchorClouds(RenderPipeline original) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null || !level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return original;
      } else if (SiftShaderCompat.isShaderPackInUse()) {
         return original;
      } else {
         return original == RenderPipelines.FLAT_CLOUDS ? SiftCloudRenderPipelines.FLAT
            : (original == RenderPipelines.CLOUDS ? SiftCloudRenderPipelines.FANCY : original);
      }
   }
}
