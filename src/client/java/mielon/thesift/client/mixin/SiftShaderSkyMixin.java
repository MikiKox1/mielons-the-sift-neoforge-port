package mielon.thesift.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.client.render.SiftShaderSkyRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
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
      DeltaTracker delta,
      boolean outline,
      CameraRenderState camera,
      Matrix4fc view,
      GpuBufferSlice fog,
      Vector4f color,
      boolean sky,
      CallbackInfo ci
   ) {
      if (SiftShaderSkyRenderer.isManagedPack()) {
         SiftShaderSkyRenderer.finishFrame(view);
      } else {
         SiftProceduralSkyRenderer.restoreAfterShaders(view);
      }
   }
}
