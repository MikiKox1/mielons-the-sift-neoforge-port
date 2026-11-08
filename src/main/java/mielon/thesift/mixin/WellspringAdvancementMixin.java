package mielon.thesift.mixin;

import mielon.thesift.advancement.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JukeboxBlockEntity.class)
public abstract class WellspringAdvancementMixin {
   @Inject(method = "tick", at = @At("TAIL"), require = 1)
   private static void theSift$listened(Level level, BlockPos pos, BlockState state, JukeboxBlockEntity jukebox, CallbackInfo ci) {
      ModAdvancements.onJukeboxTick(level, pos, jukebox);
   }
}
