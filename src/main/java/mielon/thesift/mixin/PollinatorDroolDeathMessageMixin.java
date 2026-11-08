package mielon.thesift.mixin;

import mielon.thesift.particle.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSource.class)
public abstract class PollinatorDroolDeathMessageMixin {
   @Inject(method = "getLocalizedDeathMessage", at = @At("HEAD"), cancellable = true)
   private void theSift$droolDeathMessage(LivingEntity victim, CallbackInfoReturnable<Component> cir) {
      DamageSource source = (DamageSource)(Object)this;
      if (source.is(DamageTypes.INDIRECT_MAGIC)
         && source.getDirectEntity() instanceof AreaEffectCloud cloud
         && cloud.getParticle().getType() == ModParticles.POLLINATOR_DROOL) {
         cir.setReturnValue(Component.translatable("death.attack.the_sift.pollinator_drool", victim.getDisplayName()));
      }
   }
}
