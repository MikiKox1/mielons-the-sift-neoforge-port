package mielon.thesift.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SiftShaderSkyMixin {
   @Inject(method = "renderLevel", at = @At("TAIL"))
   private void theSift$restoreSky(
      GraphicsResourceAllocator allocator,
      DeltaTracker delta,
      boolean outline,
      CameraRenderState camera,
      Matrix4fc view,
      GpuBufferSlice fog,
      Vector4f color,
      boolean sky,
      ChunkSectionsToRender sections,
      CallbackInfo ci
   ) {
      // Known packs already own exactly one sky path: early UV for Complementary,
      // Iris post-final for Photon. Keep the old tail only for other shaderpacks.
      if (SiftShaderCompat.isPhotonShaderPackSelected() || SiftShaderCompat.isComplementaryShaderPackSelected()) {
         return;
      }
      SiftProceduralSkyRenderer.restoreAfterShaders(view);
   }
}
