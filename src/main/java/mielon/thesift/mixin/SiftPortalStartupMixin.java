package mielon.thesift.mixin;

import mielon.thesift.world.SiftPortalStructure;
import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftTerrainFeatureMemory;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class SiftPortalStartupMixin {
   @Inject(method = "<init>", at = @At("RETURN"), require = 1)
   private void theSift$restorePortalProtection(CallbackInfo ci) {
      ServerLevel level = (ServerLevel)(Object)this;
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         SiftTerrainFeatureMemory.initialize(level.getServer());
         SiftPortalStructure.restoreReservation(level.getServer());
      }
   }
}
