package mielon.thesift.client.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.client.render.SiftShaderSkyRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SiftShaderSkyMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void theSift$restoreSky(
      GraphicsResourceAllocator allocator,
      boolean outline,
      CameraRenderState camera,
      GpuBufferSlice fog,
      Vector4f color,
      boolean sky,
      boolean consistentDepth,
      CallbackInfo ci
   ) {
      if (!SiftShaderSkyRenderer.isManagedPack()) {
         SiftProceduralSkyRenderer.restoreAfterShaders(camera.viewRotationMatrix);
      }
   }
}
