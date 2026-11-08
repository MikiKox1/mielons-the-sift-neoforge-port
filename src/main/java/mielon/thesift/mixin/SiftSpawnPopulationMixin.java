package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftSpawnBudget;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class SiftSpawnPopulationMixin {
   @Inject(method = "addFreshEntity", at = @At("RETURN"), require = 1)
   private void theSift$refreshPopulation(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (cir.getReturnValue()) {
         SiftSpawnBudget.invalidate((ServerLevel)(Object)this);
      }
   }
}
