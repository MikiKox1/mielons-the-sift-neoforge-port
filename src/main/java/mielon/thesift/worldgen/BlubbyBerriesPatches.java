package mielon.thesift.worldgen;

import mielon.thesift.block.BlubbyBerriesBushBlock;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class BlubbyBerriesPatches {
   private BlubbyBerriesPatches() {
   }

   static boolean tryPlace(WorldGenLevel level, RandomSource random, BlockPos floor, SiftCampProtection camp) {
      BlockState soil = level.getBlockState(floor);
      if (!soil.is(ModBlocks.CORAL_GROWTH) && !soil.is(ModBlocks.BRIGHT_CORAL_GROWTH)) {
         return false;
      }

      if (TheSiftDimension.isCoralBiome(SiftCaveHabitat.surfaceBiome(level, floor.getX(), floor.getZ()))
         && floor.getY() >= SiftCaveHabitat.surfaceRockBottom(level, floor.getX(), floor.getZ())) {
         long seed = level.getSeed() ^ 18696599880497484L;
         boolean forest = SiftCaveHabitat.surfaceBiome(level, floor.getX(), floor.getZ()).is(TheSiftDimension.CORAL_FOREST);
         boolean patch = false;

         for (int layer = 0; layer < (forest ? 3 : 1) && !patch; layer++) {
            long layerSeed = seed + layer * 7146057691288625177L;
            boolean ordinary = inPatch(layerSeed, floor.getX(), floor.getZ(), 32, 0.55);
            boolean shore = inPatch(layerSeed + 137L, floor.getX(), floor.getZ(), 24, 0.65);
            patch = ordinary || shore && nearIchor(level, floor);
         }

         if (!patch) {
            return false;
         }

         BlockPos lower = floor.above();
         BlockPos upper = lower.above();
         if (!camp.contains(lower)
            && !camp.contains(upper)
            && !SiftFeaturePlacementGuard.protectsPortalTerrain(lower)
            && !SiftFeaturePlacementGuard.protectsPortalTerrain(upper)
            && level.ensureCanWrite(lower)
            && level.ensureCanWrite(upper)
            && canReplace(level.getBlockState(lower))
            && level.isEmptyBlock(upper)) {
            BlockState plant = ModBlocks.BLUBBY_BERRIES_BUSH.defaultBlockState();
            if (!plant.canSurvive(level, lower)) {
               return false;
            }

            boolean grown = forest ? random.nextInt(4) != 0 : random.nextBoolean();
            level.setBlock(lower, plant.setValue(BlubbyBerriesBushBlock.GROWN, grown), 2);
            level.setBlock(
               upper,
               plant.setValue(BlubbyBerriesBushBlock.HALF, DoubleBlockHalf.UPPER).setValue(BlubbyBerriesBushBlock.GROWN, forest ? grown : random.nextBoolean()),
               2
            );
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean canReplace(BlockState state) {
      return state.isAir() || state.is(ModBlocks.CORAL_BUSH) || state.is(ModBlocks.BRIGHT_CORAL_BUSH);
   }

   private static boolean inPatch(long seed, int x, int z, int spacing, double density) {
      int cellX = Math.floorDiv(x, spacing);
      int cellZ = Math.floorDiv(z, spacing);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long salt = seed ^ (cellX + dx) * -7046029254386353131L ^ (cellZ + dz) * 7146057691288625177L;
            if (!(SiftTerrainShapeNoise.unit(salt) > density)) {
               int cx = (cellX + dx) * spacing + (int)(spacing * SiftTerrainShapeNoise.unit(salt + 17L));
               int cz = (cellZ + dz) * spacing + (int)(spacing * SiftTerrainShapeNoise.unit(salt + 37L));
               if (Math.abs(x - cx) <= 6 && Math.abs(z - cz) <= 6) {
                  int attempts = 6 + (int)(5.0 * SiftTerrainShapeNoise.unit(salt + 57L));

                  for (int slot = 0; slot < attempts; slot++) {
                     int ox = scatterOffset(salt, slot, 71);
                     int oz = scatterOffset(salt, slot, 131);
                     if (x == cx + ox && z == cz + oz) {
                        boolean spaced = true;

                        for (int previous = 0; previous < slot; previous++) {
                           int px = scatterOffset(salt, previous, 71);
                           int pz = scatterOffset(salt, previous, 131);
                           if ((px - ox) * (px - ox) + (pz - oz) * (pz - oz) <= 2) {
                              spaced = false;
                              break;
                           }
                        }

                        if (spaced) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static int scatterOffset(long seed, int slot, int axis) {
      long salt = seed + slot * -7723592293110705685L + axis;
      return (int)(7.0 * SiftTerrainShapeNoise.unit(salt)) - (int)(7.0 * SiftTerrainShapeNoise.unit(salt + 19L));
   }

   private static boolean nearIchor(WorldGenLevel level, BlockPos floor) {
      for (int dx = -4; dx <= 4; dx++) {
         for (int dz = -4; dz <= 4; dz++) {
            if (dx * dx + dz * dz <= 20) {
               for (int dy = -1; dy <= 1; dy++) {
                  BlockPos pos = floor.offset(dx, dy, dz);
                  if (level.ensureCanWrite(pos) && level.getBlockState(pos).is(ModFluids.ICHOR_BLOCK)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }
}
