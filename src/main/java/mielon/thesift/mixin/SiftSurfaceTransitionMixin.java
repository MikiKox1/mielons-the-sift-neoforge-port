package mielon.thesift.mixin;

import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import net.minecraft.resources.ResourceKey;

import net.minecraft.core.registries.Registries;

import mielon.thesift.worldgen.SiftSurfaceBiomeManager;

import mielon.thesift.world.TheSiftDimension;

import com.llamalad7.mixinextras.sugar.Local;

import java.util.Set;
import mielon.thesift.worldgen.SiftSurfaceTransition;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class SiftSurfaceTransitionMixin {
   @Unique
   private static final ResourceKey<NoiseGeneratorSettings> THE_SIFT_NOISE = ResourceKey.create(Registries.NOISE_SETTINGS, TheSiftDimension.ID);
   @Inject(
      method = "lambda$buildTerrain$0",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Ljava/util/Set;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V"
      ),
      require = 1
   )
   private void theSift$continuousSurface(
      ChunkAccess chunk,
      StructureManager structures,
      Blender blender,
      RandomState random,
      NoiseSettings settings,
      BiomeManager biomes,
      Set<Holder<Biome>> possibleBiomes,
      WorldGenRegion region,
      CallbackInfoReturnable<ChunkAccess> cir
   ) {
      if (region != null) {
         SiftSurfaceTransition.apply(region, chunk, (NoiseBasedChunkGenerator)(Object)this);
      }
   }

   @ModifyVariable(
      method = "buildSurface(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Ljava/util/Set;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V",
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
