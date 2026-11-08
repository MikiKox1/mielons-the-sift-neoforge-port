package mielon.thesift.mixin;

import mielon.thesift.world.SiftClockAnimation;
import mielon.thesift.world.SiftTideTransition;
import mielon.thesift.world.SiftTides;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerClockManager.class)
public abstract class ServerClockManagerMixin implements SiftClockAnimation {
   @Unique
   private SiftTideTransition theSift$transition;
   @Unique
   private Holder<WorldClock> theSift$clock;

   @Override
   public void theSift$animate(Holder<WorldClock> clock, long from, long target) {
      if (clock.unwrapKey().map(key -> key.identifier().equals(SiftTides.CLOCK_ID)).orElse(false)) {
         this.theSift$transition = new SiftTideTransition(from, target);
         this.theSift$clock = clock;
         ((ServerClockManager)(Object)this).setTotalTicks(clock, this.theSift$transition.time());
         if (this.theSift$transition.done()) {
            this.theSift$transition = null;
         }
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void theSift$advance(CallbackInfo ci) {
      if (this.theSift$transition != null) {
         ((ServerClockManager)(Object)this).setTotalTicks(this.theSift$clock, this.theSift$transition.advance());
         if (this.theSift$transition.done()) {
            this.theSift$transition = null;
         }
      }
   }
}
