package mielon.thesift.mixin;

import mielon.thesift.entity.StalkerEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class StalkerClawKnockbackMixin {
   @ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private double theSift$firstClawKnockback(double strength) {
      return StalkerEntity.firstClawKnockbackStrength((LivingEntity)(Object)this, strength);
   }
}
