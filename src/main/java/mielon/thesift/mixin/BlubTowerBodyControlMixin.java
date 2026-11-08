package mielon.thesift.mixin;

import mielon.thesift.entity.BlubEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BodyRotationControl.class)
public abstract class BlubTowerBodyControlMixin {
   @Shadow
   @Final
   private Mob mob;

   @Inject(method = "notCarryingMobPassengers", at = @At("HEAD"), cancellable = true)
   private void theSift$independentTower(CallbackInfoReturnable<Boolean> cir) {
      if (this.mob instanceof BlubEntity && this.mob.getFirstPassenger() instanceof BlubEntity) {
         cir.setReturnValue(true);
      }
   }
}
