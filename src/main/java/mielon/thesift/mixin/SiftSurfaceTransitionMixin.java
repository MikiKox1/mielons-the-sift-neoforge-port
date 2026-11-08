package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftSurfaceTransition;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class SiftSurfaceTransitionMixin {
   @Inject(
      method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
      at = @At("HEAD"),
      require = 1
   )
   private void theSift$continuousSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
      SiftSurfaceTransition.apply(region, chunk, (NoiseBasedChunkGenerator)(Object)this);
   }
}
