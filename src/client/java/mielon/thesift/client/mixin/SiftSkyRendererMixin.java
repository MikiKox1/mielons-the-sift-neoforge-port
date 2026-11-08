package mielon.thesift.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.compat.SiftSkyShaderCompat;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SiftSkyRendererMixin {
   @Shadow
   @Final
   private AutoStorageIndexBuffer quadIndices;

   @Shadow
   public abstract void renderSunriseAndSunset(PoseStack var1, float var2, int var3);

   @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
   private void theSift$renderProceduralSky(
      PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         if (!SiftShaderCompat.isShaderPackInUse()) {
            SiftProceduralSkyRenderer.render(poseStack, level, Minecraft.getInstance().getMainRenderTarget(), this.quadIndices);
            float thrive = SiftTides.thriveBlend(level.getDefaultClockTime());
            if (thrive > 0.001F) {
               int color = Math.round(95.0F * thrive * rainBrightness) << 24 | 15704883;

               for (int direction = 0; direction < 4; direction++) {
                  poseStack.pushPose();
                  poseStack.mulPose(Axis.YP.rotationDegrees(direction * 90.0F));
                  this.renderSunriseAndSunset(poseStack, 0.0F, color);
                  poseStack.popPose();
               }
            }

            ci.cancel();
         } else if (SiftSkyShaderCompat.restoreSky()
            && (SiftShaderCompat.isPhotonShaderPackSelected() || SiftShaderCompat.isComplementaryShaderPackSelected())) {
            // Photon queues the final-pass compositor; Complementary draws a
            // UV-interpolated sky here. Neither may be drawn again at level TAIL.
            SiftProceduralSkyRenderer.render(poseStack, level, Minecraft.getInstance().getMainRenderTarget(), this.quadIndices);
            ci.cancel();
         }
      }
   }

   @Inject(method = "close", at = @At("TAIL"))
   private void theSift$closeProceduralSky(CallbackInfo ci) {
      SiftProceduralSkyRenderer.close();
   }
}
