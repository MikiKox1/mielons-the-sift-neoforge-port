package mielon.thesift.mixin;

import mielon.thesift.fluid.ProjectileFluidEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class IchorArrowWaterMixin {
   @Inject(method = "isInWater", at = @At("RETURN"), cancellable = true)
   private void theSift$arrowWaterInertia(CallbackInfoReturnable<Boolean> cir) {
      if (!cir.getReturnValueZ() && (Object)this instanceof AbstractArrow arrow && ProjectileFluidEffects.isTouchingIchor(arrow)) {
         cir.setReturnValue(true);
      }
   }
}
