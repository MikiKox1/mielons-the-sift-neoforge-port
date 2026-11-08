package mielon.thesift.mixin;

import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftTerrainUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public abstract class SiftWorldGenUpgradeMixin {
   @Inject(method = "isOldChunkAround", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$findLegacySift(ChunkPos center, int range, CallbackInfoReturnable<Boolean> cir) {
      WorldGenRegion region = (WorldGenRegion)(Object)this;
      if (region.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         cir.setReturnValue(SiftTerrainUpgrade.hasLegacyAround(region, center, range));
      }
   }

   @Inject(method = "ensureCanWrite", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$protectDirectFeatureWrites(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      WorldGenRegion region = (WorldGenRegion)(Object)this;
      if (region.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY) && region.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
         ChunkAccess chunk = region.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
         if (SiftTerrainUpgrade.legacy(chunk) && chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$preserveLegacyBuilds(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> cir) {
      if (SiftTerrainUpgrade.protects((WorldGenRegion)(Object)this, pos, state)) {
         cir.setReturnValue(false);
      }
   }
}
