package mielon.thesift.client.mixin;

import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.WorldTimeUniforms", remap = false)
public abstract class IrisWorldTimeUniformsMixin {
   private static final int SHADER_DAY_TIME = 3000;
   private static final int SHADER_NIGHT_TIME = 18000;
   private static final int VANILLA_DAY_LENGTH = 24000;

   @Inject(method = "getWorldDayTime()I", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
   private static void theSift$mapWorldTime(CallbackInfoReturnable<Integer> cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue(theSift$shaderTime(level.getDefaultClockTime()));
      }
   }

   @Inject(method = "getWorldDay()I", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
   private static void theSift$mapWorldDay(CallbackInfoReturnable<Integer> cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue((int)Math.floorDiv(level.getDefaultClockTime(), 25200L));
      }
   }

   private static int theSift$shaderTime(long totalTicks) {
      return SiftTides.shaderTime(totalTicks);
   }

   private static float theSift$smooth(float value) {
      float clamped = Math.max(0.0F, Math.min(1.0F, value));
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }
}
