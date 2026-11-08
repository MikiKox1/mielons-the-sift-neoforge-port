package mielon.thesift.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WeatherEffectRenderer.class)
public abstract class SiftWeatherTextureMixin {
   private static final Identifier VANILLA_RAIN = Identifier.withDefaultNamespace("textures/environment/rain.png");
   private static final Identifier ICHOR_RAIN = Identifier.fromNamespaceAndPath("the_sift", "textures/environment/ichor_rain.png");
   private static final Identifier VANILLA_SNOW = Identifier.withDefaultNamespace("textures/environment/snow.png");
   private static final Identifier ICHOR_SNOW = Identifier.fromNamespaceAndPath("the_sift", "textures/environment/ichor_snow.png");

   // 26.1.2 uses getPrecipitationAt(Level, BlockPos) for both columns and
   // weather particles; later versions store a local in extractRenderState.
   @ModifyReturnValue(method = "getPrecipitationAt", at = @At("RETURN"))
   private Precipitation theSift$restrictSnowColumns(Precipitation original, Level level, BlockPos pos) {
      if (!level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return original;
      } else if (level.getBiome(pos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) {
         return Precipitation.SNOW;
      } else {
         return original == Precipitation.SNOW ? Precipitation.RAIN : original;
      }
   }

   @ModifyArg(
      // The two-argument NeoForge compatibility shim has no texture calls.
      method = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/texture/TextureManager;getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;"
      ),
      index = 0
   )
   private Identifier theSift$useIchorWeatherTexture(Identifier original) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         if (original.equals(VANILLA_RAIN)) {
            return ICHOR_RAIN;
         }

         if (original.equals(VANILLA_SNOW)) {
            return ICHOR_SNOW;
         }
      }

      return original;
   }
}
