package mielon.thesift.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.commands.RenderPass;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.client.render.SiftShaderSkyRenderer;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SiftSkyRendererMixin {
   @Shadow
   @Final
   private AutoStorageIndexBuffer quadIndices;

   @Invoker("renderSunriseAndSunset")
   public abstract void theSift$vanillaSunrise(RenderPass var1, PoseStack var2, float var3, Vector4fc var4);

   @Inject(method = "render", at = @At("HEAD"))
   private void theSift$prepareSkyBuffers(GpuBufferSlice skyFog, SkyRenderState state, CallbackInfo ci) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         SiftProceduralSkyRenderer.prepareBuffers(this.quadIndices);
         SiftShaderSkyRenderer.prepare(this.quadIndices);
      }
   }

   @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
   private void theSift$renderProceduralSky(
      RenderPass renderPass,
      PoseStack poseStack,
      float sunAngle,
      float moonAngle,
      float starAngle,
      MoonPhase moonPhase,
      float rainBrightness,
      float starBrightness,
      CallbackInfo ci
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         if (SiftShaderSkyRenderer.renderSky(poseStack, level, renderPass)) {
            ci.cancel();
            return;
         }
         if (!SiftShaderCompat.isShaderPackInUse()) {
            SiftProceduralSkyRenderer.render(poseStack, level, renderPass, this.quadIndices);
            float thrive = SiftTides.thriveBlend(level.getDefaultClockTime());
            if (thrive > 0.001F) {
               Vector4f color = new Vector4f(0.9372549F, 0.6392157F, 0.2F, 0.37254903F * thrive * rainBrightness);

               for (int direction = 0; direction < 4; direction++) {
                  poseStack.pushPose();
                  poseStack.rotateDegrees(Axis.YP, direction * 90.0F);
                  this.theSift$vanillaSunrise(renderPass, poseStack, 0.0F, color);
                  poseStack.popPose();
               }
            }

            ci.cancel();
         }
      }
   }

   @Inject(method = "close", at = @At("TAIL"))
   private void theSift$closeProceduralSky(CallbackInfo ci) {
      SiftProceduralSkyRenderer.close();
   }
}
