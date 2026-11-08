package mielon.thesift.mixin;

import mielon.thesift.entity.SonicSniffMotionAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class SonicSniffMotionMixin implements SonicSniffMotionAccess {
   @Unique
   private Vec3 theSift$sonicSniffMotion;

   @Override
   public void theSift$markSonicSniffMotion() {
      this.theSift$sonicSniffMotion = ((Player)(Object)this).getDeltaMovement();
   }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
   private void theSift$saveSonicSniffMotion(ValueOutput output, CallbackInfo ci) {
      boolean hasSonicSniffMotion = this.theSift$sonicSniffMotion != null && this.theSift$sonicSniffMotion.equals(((Player)(Object)this).getDeltaMovement());
      output.putBoolean("TheSiftSonicSniffMotion", hasSonicSniffMotion);
      if (!hasSonicSniffMotion) {
         this.theSift$sonicSniffMotion = null;
      }
   }

   @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
   private void theSift$clearRestoredSonicSniffMotion(ValueInput input, CallbackInfo ci) {
      if (input.getBooleanOr("TheSiftSonicSniffMotion", false)) {
         ((Player)(Object)this).setDeltaMovement(Vec3.ZERO);
      }

      this.theSift$sonicSniffMotion = null;
   }
}
