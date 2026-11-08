package mielon.thesift.mixin;

import mielon.thesift.entity.EchoGolemSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
   @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
   private void theSift$summonEchoGolem(Level level, BlockPos pos, CallbackInfo ci) {
      if (EchoGolemSummoning.trySpawn(level, pos)) {
         ci.cancel();
      }
   }

   @Inject(method = "canSpawnGolem", at = @At("HEAD"), cancellable = true)
   private void theSift$allowEchoGolemDispenser(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (EchoGolemSummoning.hasBase(level, pos)) {
         cir.setReturnValue(true);
      }
   }
}
