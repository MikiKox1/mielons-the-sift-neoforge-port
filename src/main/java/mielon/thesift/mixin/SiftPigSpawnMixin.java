package mielon.thesift.mixin;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public abstract class SiftPigSpawnMixin {
   @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
   private static void theSift$excludeWildPigs(
      EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir
   ) {
      if (type == EntityType.PIG
         && (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION)
         && level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue(false);
      }
   }
}

