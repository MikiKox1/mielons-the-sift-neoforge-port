package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftPortalVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class, WorldGenRegion.class})
public abstract class SiftPortalVegetationWriteMixin {
   @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
   private void sift$limitPlantReplay(BlockPos pos, BlockState state, int flags, int recursion, CallbackInfoReturnable<Boolean> ci) {
      if (!SiftPortalVegetation.permitsWrite((BlockGetter)this, pos, state)) {
         ci.setReturnValue(false);
      }
   }

   @Inject(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("HEAD"), cancellable = true)
   private void sift$usePortalDeck(Types type, int x, int z, CallbackInfoReturnable<Integer> ci) {
      if (type == Types.WORLD_SURFACE_WG || type == Types.WORLD_SURFACE || type == Types.MOTION_BLOCKING) {
         Integer height = SiftPortalVegetation.replayHeight(x, z);
         if (height != null) {
            ci.setReturnValue(height);
         }
      }
   }
}
