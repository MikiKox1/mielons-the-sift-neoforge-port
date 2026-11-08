package mielon.thesift.client.mixin;

import mielon.thesift.fluid.IchorRegeneration;
import mielon.thesift.particle.ModParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class IchorSoulEntityMixin {
   @Inject(method = "baseTick", at = @At("TAIL"))
   private void theSift$ichorSouls(CallbackInfo ci) {
      LivingEntity entity = (LivingEntity)(Object)this;
      if (entity.level().isClientSide()
         && entity.isAlive()
         && !(entity instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable))
         && (entity.tickCount + entity.getId() & 3) == 0
         && (IchorRegeneration.isTouching(entity) || IchorRegeneration.isExposedToRain(entity))) {
         int absorption = IchorRegeneration.absorptionLevel(entity);
         if (absorption != 1) {
            RandomSource random = entity.getRandom();
            entity.level()
               .addParticle(
                  ModParticles.ICHOR_SOUL_BODY,
                  entity.getX() + (random.nextDouble() - 0.5) * entity.getBbWidth(),
                  entity.getY() + entity.getBbHeight() * (0.2 + random.nextDouble() * 0.65),
                  entity.getZ() + (random.nextDouble() - 0.5) * entity.getBbWidth(),
                  entity.getId(),
                  absorption >= 2 ? 1.0 : 0.0,
                  0.0
               );
         }
      }
   }
}
