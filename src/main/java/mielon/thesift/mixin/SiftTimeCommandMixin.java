package mielon.thesift.mixin;

import mielon.thesift.world.SiftClockAnimation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TimeCommand.class)
public abstract class SiftTimeCommandMixin {
   @Unique
   private static long theSift$before;

   @Inject(method = {"setTotalTicks", "addTime"}, at = @At("HEAD"))
   private static void theSift$remember(CommandSourceStack source, Holder<WorldClock> clock, int ticks, CallbackInfoReturnable<Integer> cir) {
      theSift$before = source.getServer().clockManager().getTotalTicks(clock);
   }

   @Inject(method = {"setTotalTicks", "addTime"}, at = @At("RETURN"))
   private static void theSift$animate(CommandSourceStack source, Holder<WorldClock> clock, int ticks, CallbackInfoReturnable<Integer> cir) {
      ServerClockManager manager = source.getServer().clockManager();
      ((SiftClockAnimation)manager).theSift$animate(clock, theSift$before, manager.getTotalTicks(clock));
   }

   @Inject(method = "setTimeToTimeMarker", at = @At("HEAD"))
   private static void theSift$rememberMarker(
      CommandSourceStack source, Holder<WorldClock> clock, ResourceKey<ClockTimeMarker> marker, CallbackInfoReturnable<Integer> cir
   ) {
      theSift$before = source.getServer().clockManager().getTotalTicks(clock);
   }

   @Inject(method = "setTimeToTimeMarker", at = @At("RETURN"))
   private static void theSift$animateMarker(
      CommandSourceStack source, Holder<WorldClock> clock, ResourceKey<ClockTimeMarker> marker, CallbackInfoReturnable<Integer> cir
   ) {
      ServerClockManager manager = source.getServer().clockManager();
      ((SiftClockAnimation)manager).theSift$animate(clock, theSift$before, manager.getTotalTicks(clock));
   }
}
