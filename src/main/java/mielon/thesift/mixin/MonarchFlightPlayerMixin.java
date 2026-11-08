package mielon.thesift.mixin;

import mielon.thesift.item.MonarchFlight;
import mielon.thesift.item.MonarchFlightAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MonarchFlightPlayerMixin implements MonarchFlightAccess {
   @Unique
   private final MonarchFlight theSift$flight = new MonarchFlight();

   @Override
   public MonarchFlight theSift$monarchFlight() {
      return this.theSift$flight;
   }

   @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
   private void theSift$saveMonarchCooldown(ValueOutput output, CallbackInfo ci) {
      int ticks = this.theSift$flight.cooldownToSave((Player)(Object)this);
      if (ticks > 0) {
         output.putInt("TheSiftMonarchCooldown", ticks);
      }
   }

   @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
   private void theSift$loadMonarchCooldown(ValueInput input, CallbackInfo ci) {
      this.theSift$flight.loadCooldown(input.getIntOr("TheSiftMonarchCooldown", 0));
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void theSift$boostTick(CallbackInfo ci) {
      this.theSift$flight.tick((Player)(Object)this);
   }
}
