package mielon.thesift.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.WeatherData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelWeatherMixin {
   @ModifyExpressionValue(
      method = "tickPrecipitation",
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;SNOW:Lnet/minecraft/world/level/block/Block;")
   )
   private Block theSift$useIchorSnowDuringPrecipitation(Block original) {
      ServerLevel self = (ServerLevel)(Object)this;
      return self.dimension().equals(TheSiftDimension.LEVEL_KEY) ? ModBlocks.ICHOR_SNOW : original;
   }

   @ModifyExpressionValue(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
   private int theSift$capIchorSnowLayers(int original) {
      ServerLevel self = (ServerLevel)(Object)this;
      return self.dimension().equals(TheSiftDimension.LEVEL_KEY) ? Math.min(original, 2) : original;
   }

   @Inject(method = "getWeatherData", at = @At("HEAD"), cancellable = true)
   private void theSift$getPerDimensionWeather(CallbackInfoReturnable<WeatherData> cir) {
      ServerLevel self = (ServerLevel)(Object)this;
      if (!self.dimension().equals(Level.OVERWORLD)) {
         cir.setReturnValue(self.getDataStorage().computeIfAbsent(WeatherData.TYPE));
      }
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   private void theSift$initializePerDimensionWeatherLevels(CallbackInfo ci) {
      ServerLevel self = (ServerLevel)(Object)this;
      if (!self.dimension().equals(Level.OVERWORLD)) {
         WeatherData weather = self.getWeatherData();
         self.setRainLevel(weather.isRaining() ? 1.0F : 0.0F);
         self.setThunderLevel(weather.isThundering() ? 1.0F : 0.0F);
      }
   }
}
