package mielon.thesift.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.SiftCloudRenderPipelines;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CloudRenderer.class)
public abstract class SiftCloudRendererMixin {
   private static final float CLOUD_CLEAR_OPACITY = 0.44F;
   private static final float CLOUD_RAIN_OPACITY = 0.66F;

   @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private int theSift$fadeCloudsWithRain(int originalColor) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         // Let Iris and the shaderpack own their cloud color and alpha.
         if (SiftShaderCompat.isShaderPackInUse()) {
            return originalColor;
         }

         float rain = level.getRainLevel(1.0F);
         int originalAlpha = originalColor >>> 24 & 0xFF;
         // A rain-only factor makes clear-weather cloud geometry entirely invisible.
         int fadedAlpha = Math.round(originalAlpha * (CLOUD_CLEAR_OPACITY + (CLOUD_RAIN_OPACITY - CLOUD_CLEAR_OPACITY) * rain));

         float night = SiftTides.nightBlend(level.getDefaultClockTime());
         int brightness = Math.round(255.0F * (1.0F - night * 0.24F));
         return fadedAlpha << 24 | brightness << 16 | brightness << 8 | brightness;
      } else {
         return originalColor;
      }
   }

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
