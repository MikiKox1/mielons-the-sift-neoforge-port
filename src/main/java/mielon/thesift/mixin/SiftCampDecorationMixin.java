package mielon.thesift.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mielon.thesift.worldgen.SiftCampProtection;
import mielon.thesift.worldgen.SiftPortalVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
public abstract class SiftCampDecorationMixin {
   @WrapOperation(
      method = "applyBiomeDecoration",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/placement/FeaturePlacer;placeWithBiomeCheck(Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
      )
   )
   private boolean sift$protectCamp(
      FeaturePlacer placer,
      PlacedFeature feature,
      RandomSource random,
      BlockPos origin,
      Operation<Boolean> original,
      WorldGenLevel level,
      ChunkAccess chunk,
      StructureManager structures
   ) {
      SiftCampProtection previous = SiftCampProtection.beginDecoration(level, origin);
      Boolean vegetation = SiftPortalVegetation.beginNatural(feature, level);

      try {
         return (Boolean)original.call(new Object[]{placer, feature, random, origin});
      } finally {
         SiftPortalVegetation.endNatural(vegetation);
         SiftCampProtection.endDecoration(previous);
      }
   }
}
