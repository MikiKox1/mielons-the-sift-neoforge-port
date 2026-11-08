package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftFlowerHabitat;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class SiftFlowerSurveyMixin {
   @Inject(method = "runPostLoad", at = @At("TAIL"))
   private void theSift$surveyOriginalFlowers(CallbackInfo ci) {
      SiftFlowerHabitat.survey((LevelChunk)(Object)this);
   }
}
