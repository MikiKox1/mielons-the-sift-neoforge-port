package mielon.thesift.client.mixin;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class SiftAtmosphericFogMixin {
   @Redirect(method = "getBaseColor", at = @At(value = "INVOKE", target = "Lorg/joml/Vector3fc;dot(FFF)F"))
   private float theSift$allDirections(Vector3fc direction, float x, float y, float z) {
      ClientLevel level = Minecraft.getInstance().level;
      return level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY) ? 1.0F : direction.dot(x, y, z);
   }
}
