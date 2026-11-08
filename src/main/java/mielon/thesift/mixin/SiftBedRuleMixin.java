package mielon.thesift.mixin;

import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedRule.class)
public abstract class SiftBedRuleMixin {
   @Inject(method = "canSleep", at = @At("HEAD"), cancellable = true)
   private void theSift$tideSleep(Level level, CallbackInfoReturnable<Boolean> cir) {
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue(SiftTides.canSleep(level.getDefaultClockTime(), level.isThundering()));
      }
   }
}
