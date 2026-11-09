package mielon.thesift.mixin;

import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import net.minecraft.world.level.biome.BiomeManager;

import net.minecraft.resources.ResourceKey;

import net.minecraft.core.registries.Registries;

import mielon.thesift.worldgen.SiftSurfaceBiomeManager;

import mielon.thesift.world.TheSiftDimension;

import com.llamalad7.mixinextras.sugar.Local;

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
   @Unique
   private static final ResourceKey<NoiseGeneratorSettings> THE_SIFT_NOISE = ResourceKey.create(Registries.NOISE_SETTINGS, TheSiftDimension.ID);
   @Inject(
      method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
      at = @At("HEAD"),
      require = 1
   )
   private void theSift$continuousSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
      SiftSurfaceTransition.apply(region, chunk, (NoiseBasedChunkGenerator)(Object)this);
   }

   @ModifyVariable(
      method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/core/Registry;Lnet/minecraft/world/level/levelgen/blending/Blender;)V",
      at = @At("HEAD"),
      argsOnly = true,
      require = 1
   )
   private BiomeManager theSift$softenSurfaceBiomeEdges(
      BiomeManager original, @Local(argsOnly = true) ChunkAccess chunk, @Local(argsOnly = true) RandomState random
   ) {
      if (!((NoiseBasedChunkGenerator)(Object)this).stable(THE_SIFT_NOISE)) {
         return original;
      }

      long seed = random.getOrCreateRandomFactory(TheSiftDimension.ID).at(0, 0, 0).nextLong();
      return new SiftSurfaceBiomeManager(original, chunk, seed);
   }
}
