package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftslateGrowthBlock;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SiftSurfaceDecorator {
   private SiftSurfaceDecorator() {
   }

   public static boolean decorate(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale) {
      return decorate(level, random, floorPos, densityScale, 2);
   }

   public static boolean decorate(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale, int updateFlags) {
      ensureCoralPortalSoil(level, floorPos, updateFlags);
      BlockState floor = level.getBlockState(floorPos);
      BlockPos plantPos = floorPos.above();
      if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
         float roll = random.nextFloat();
         float chance;
         Block plant;
         if (floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)) {
            chance = 0.22F;
            plant = ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT;
         } else if (floor.is(ModBlocks.HEALTHY_SCULK_PORES)) {
            chance = 0.16F;
            plant = ModBlocks.HEALTHY_SCULK_SPROUTS;
         } else if (floor.is(ModBlocks.CORAL_SOIL) || floor.is(ModBlocks.CORAL_GROWTH) || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH)) {
            chance = 0.275F;
            plant = floor.is(ModBlocks.BRIGHT_CORAL_GROWTH) ? ModBlocks.BRIGHT_CORAL_BUSH : ModBlocks.CORAL_BUSH;
         } else if (floor.is(ModBlocks.SOULSHALE_GROWTH)) {
            chance = 0.255F;
            plant = roll < 0.6F ? ModBlocks.HEALTHY_SCULK_SPROUTS : (roll < 0.9F ? ModBlocks.GRIM_BUSH : ModBlocks.WHISPERBLOOM);
         } else if (floor.is(ModBlocks.SIFTSLATE_GROWTH)) {
            chance = 0.255F;
            plant = roll < 0.43F
               ? ModBlocks.OVERGROWN_FRONDS
               : (roll < 0.68F ? ModBlocks.OVERGROWN_STALKS : (roll < 0.86F ? ModBlocks.OVERGROWN_CHARD : ModBlocks.SIFTSLATE_STALKS));
         } else if (floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
            chance = 0.235F;
            plant = roll < 0.34F
               ? ModBlocks.DRY_HEALTHY_SCULK_SPROUTS
               : (
                  roll < 0.59F
                     ? ModBlocks.OVERGROWN_FRONDS
                     : (roll < 0.8F ? ModBlocks.OVERGROWN_STALKS : (roll < 0.93F ? ModBlocks.OVERGROWN_CHARD : ModBlocks.SIFTSLATE_STALKS))
               );
         } else if (floor.is(ModBlocks.HEALTHY_SCULK)) {
            chance = 0.225F;
            plant = roll < 0.76F ? ModBlocks.HEALTHY_SCULK_SPROUTS : (roll < 0.92F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.OVERGROWN_FRONDS);
         } else if (floor.is(ModBlocks.DRY_HEALTHY_SCULK)) {
            chance = 0.145F;
            plant = roll < 0.74F ? ModBlocks.DRY_HEALTHY_SCULK_SPROUTS : (roll < 0.94F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.OVERGROWN_STALKS);
         } else {
            if (!floor.is(ModBlocks.SIFTSLATE)) {
               return false;
            }

            chance = 0.045F;
            plant = roll < 0.72F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.OVERGROWN_FRONDS;
         }

         if (random.nextFloat() >= chance * densityScale) {
            return false;
         }

         BlockState plantState = plant.defaultBlockState();
         if (!plantState.canSurvive(level, plantPos)) {
            return false;
         }

         level.setBlock(plantPos, plantState, updateFlags);
         SiftFlowerHabitat.record(level, plantPos, plantState);
         return true;
      } else {
         return false;
      }
   }

   private static void ensureCoralPortalSoil(WorldGenLevel level, BlockPos floorPos, int flags) {
      BlockState floor = level.getBlockState(floorPos);
      if (floor.is(ModBlocks.CORAL_GROWTH) || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH)) {
         BlockPos soil = floorPos.below();
         if (level.ensureCanWrite(soil)) {
            BlockState support = level.getBlockState(soil);
            if (!support.is(ModBlocks.CORAL_SOIL)) {
               if ((support.isAir() || SiftLandmarkTerrain.isGround(support)) && support.getFluidState().isEmpty()) {
                  level.setBlock(soil, ModBlocks.CORAL_SOIL.defaultBlockState(), flags);
                  SiftTerrainFeatureMemory.protectTerrain(soil.getX(), soil.getZ(), soil.getY(), soil.getY());
               }
            }
         }
      }
   }

   public static boolean decoratePortalTerrain(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale, int flags) {
      BlockState floor = level.getBlockState(floorPos);
      BlockState above = level.getBlockState(floorPos.above());
      if (floor.is(ModBlocks.ICHOR_SNOW_BLOCK) || floor.is(ModBlocks.ICHOR_SNOW)) {
         if (level.getBlockState(floorPos.below()).is(ModBlocks.SIFTSLATE) && level.ensureCanWrite(floorPos.below())) {
            level.setBlock(floorPos.below(), ModBlocks.SIFTSLATE_GROWTH.defaultBlockState().setValue(SiftslateGrowthBlock.SNOWY, true), flags);
            SiftTerrainFeatureMemory.protectTerrain(floorPos.getX(), floorPos.getZ(), floorPos.getY() - 1, floorPos.getY() - 1);
         }

         return false;
      } else {
         if (floor.is(ModBlocks.SIFTSLATE) && SiftslateGrowthBlock.isIchorSnowCover(above) && level.ensureCanWrite(floorPos)) {
            level.setBlock(floorPos, ModBlocks.SIFTSLATE_GROWTH.defaultBlockState().setValue(SiftslateGrowthBlock.SNOWY, true), flags);
            SiftTerrainFeatureMemory.protectTerrain(floorPos.getX(), floorPos.getZ(), floorPos.getY(), floorPos.getY());
            return false;
         }

         if (above.isAir() && level.ensureCanWrite(floorPos) && level.ensureCanWrite(floorPos.above())) {
            Holder<Biome> biome = SiftCaveHabitat.biome(level, floorPos);
            BlockState cap = floor;
            if (!TheSiftDimension.isCoralBiome(biome) || !floor.is(ModBlocks.SOULSHALE) && !floor.is(ModBlocks.SIFTSLATE) && !floor.is(ModBlocks.CORAL_SOIL)) {
               if (!TheSiftDimension.isOrganicDesert(biome) || !floor.is(ModBlocks.SOULSHALE) && !floor.is(ModBlocks.SIFTSLATE)) {
                  if (biome.is(TheSiftDimension.SOULSHALE_CAVES) && floor.is(ModBlocks.SOULSHALE)) {
                     cap = ModBlocks.SOULSHALE_GROWTH.defaultBlockState();
                  } else if ((TheSiftDimension.isOvergrownBiome(biome) || biome.is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) && floor.is(ModBlocks.SIFTSLATE)) {
                     cap = ModBlocks.SIFTSLATE_GROWTH.defaultBlockState();
                  }
               } else {
                  cap = ModBlocks.HEALTHY_SCULK_CARTILAGE.defaultBlockState();
               }
            } else {
               cap = (CoralSurfaceFeature.brightPatch(level.getSeed(), floorPos.getX(), floorPos.getZ())
                     ? ModBlocks.BRIGHT_CORAL_GROWTH
                     : ModBlocks.CORAL_GROWTH)
                  .defaultBlockState();
            }

            if (cap != floor) {
               level.setBlock(floorPos, cap, flags);
            }

            SiftTerrainFeatureMemory.protectTerrain(floorPos.getX(), floorPos.getZ(), floorPos.getY(), floorPos.getY());
            return decorateStructureSurface(level, random, floorPos, densityScale, flags);
         } else {
            return false;
         }
      }
   }

   public static void decorateLakeShore(WorldGenLevel level, RandomSource random, List<BlockPos> shore, int updateFlags) {
      if (!shore.isEmpty()) {
         List<BlockPos> flowerFloors = new ArrayList<>();

         for (BlockPos floorPos : shore) {
            if (SiftFlowerPatchFeature.isLakeFlowerSoil(level.getBlockState(floorPos))) {
               flowerFloors.add(floorPos);
            }
         }

         int batches = Math.max(2, Math.min(12, 2 + flowerFloors.size() / 30));
         int firstSpecies = random.nextInt(6);
         List<BlockPos> anchors = new ArrayList<>();

         for (int batch = 0; batch < batches && !flowerFloors.isEmpty(); batch++) {
            BlockPos anchor = chooseSeparatedAnchor(flowerFloors, anchors, random);
            if (anchor == null) {
               break;
            }

            anchors.add(anchor);
            placeFlowerBatch(level, random, flowerFloors, anchor, (firstSpecies + batch) % 6, updateFlags);
         }

         for (BlockPos floorPos : shore) {
            ensureCoralPortalSoil(level, floorPos, updateFlags);
            BlockState floor = level.getBlockState(floorPos);
            float chance = !floor.is(ModBlocks.SIFTSLATE_GROWTH) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
               ? (floor.is(ModBlocks.HEALTHY_SCULK) ? 0.6F : (floor.is(ModBlocks.DRY_HEALTHY_SCULK) ? 0.5F : (floor.is(ModBlocks.SIFTSLATE) ? 0.45F : 0.0F)))
               : 0.68F;
            if (!(random.nextFloat() >= chance)) {
               BlockPos plantPos = floorPos.above();
               if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
                  float roll = random.nextFloat();
                  Block plant;
                  if (!floor.is(ModBlocks.SIFTSLATE_GROWTH) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
                     if (floor.is(ModBlocks.SIFTSLATE)) {
                        plant = roll < 0.82F ? ModBlocks.SIFTSLATE_STALKS : ModBlocks.HEALTHY_SCULK_SPROUTS;
                     } else if (floor.is(ModBlocks.HEALTHY_SCULK)) {
                        plant = roll < 0.82F ? ModBlocks.HEALTHY_SCULK_SPROUTS : ModBlocks.SIFTSLATE_STALKS;
                     } else {
                        if (!floor.is(ModBlocks.DRY_HEALTHY_SCULK) && !floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
                           continue;
                        }

                        plant = ModBlocks.DRY_HEALTHY_SCULK_SPROUTS;
                     }
                  } else {
                     plant = roll < 0.38F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.72F ? ModBlocks.OVERGROWN_STALKS : ModBlocks.OVERGROWN_CHARD);
                  }

                  BlockState plantState = plant.defaultBlockState();
                  if (plantState.canSurvive(level, plantPos)) {
                     level.setBlock(plantPos, plantState, updateFlags);
                     SiftFlowerHabitat.record(level, plantPos, plantState);
                  }
               }
            }
         }
      }
   }

   private static BlockPos chooseSeparatedAnchor(List<BlockPos> candidates, List<BlockPos> used, RandomSource random) {
      for (int attempt = 0; attempt < 32; attempt++) {
         BlockPos candidate = candidates.get(random.nextInt(candidates.size()));
         boolean separated = true;

         for (BlockPos anchor : used) {
            int dx = candidate.getX() - anchor.getX();
            int dz = candidate.getZ() - anchor.getZ();
            if (dx * dx + dz * dz < 16) {
               separated = false;
               break;
            }
         }

         if (separated) {
            return candidate;
         }
      }

      return null;
   }

   private static void placeFlowerBatch(WorldGenLevel level, RandomSource random, List<BlockPos> candidates, BlockPos anchor, int species, int updateFlags) {
      int target = 3 + random.nextInt(2);
      List<BlockPos> nearby = new ArrayList<>();

      for (BlockPos candidate : candidates) {
         if (Math.abs(candidate.getX() - anchor.getX()) <= 2
            && Math.abs(candidate.getZ() - anchor.getZ()) <= 2
            && Math.abs(candidate.getY() - anchor.getY()) <= 1) {
            nearby.add(candidate);
         }
      }

      int placed = 0;

      while (placed < target && !nearby.isEmpty()) {
         BlockPos floorPos = nearby.remove(random.nextInt(nearby.size()));
         if (placeOasisFlower(level, random, floorPos.above(), species, updateFlags)) {
            placed++;
         }
      }
   }

   private static boolean placeOasisFlower(WorldGenLevel level, RandomSource random, BlockPos plantPos, int species, int updateFlags) {
      if (!SiftFlowerPatchFeature.isLakeFlowerSoil(level.getBlockState(plantPos.below())) || !level.isEmptyBlock(plantPos) || !level.ensureCanWrite(plantPos)) {
         return false;
      }

      if (species == 4) {
         if (level.isEmptyBlock(plantPos.above()) && level.ensureCanWrite(plantPos.above())) {
            BlockState pitcher = Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE, 3 + random.nextInt(2));
            if (!pitcher.canSurvive(level, plantPos)) {
               return false;
            }

            DoublePlantBlock.placeAt(level, pitcher, plantPos, updateFlags);
            SiftFlowerHabitat.record(level, plantPos, pitcher);
            return true;
         } else {
            return false;
         }
      } else if (species == 5) {
         BlockState wildflowers = SiftFlowerPatchFeature.wildflowersState(random);
         if (!wildflowers.canSurvive(level, plantPos)) {
            return false;
         }

         level.setBlock(plantPos, wildflowers, updateFlags);
         SiftFlowerHabitat.record(level, plantPos, wildflowers);
         return true;
      } else {
         Block flower = switch (species) {
            case 0 -> SiftFlowerPatchFeature.saucerBloom(random);
            case 1 -> ModBlocks.SUNBURST_PLANT;
            case 2 -> ModBlocks.WHISPERBLOOM;
            default -> Blocks.TORCHFLOWER;
         };
         BlockState state = flower.defaultBlockState();
         if (!state.canSurvive(level, plantPos)) {
            return false;
         }

         level.setBlock(plantPos, state, updateFlags);
         SiftFlowerHabitat.record(level, plantPos, state);
         return true;
      }
   }

   public static boolean decorateStructureSurface(WorldGenLevel level, RandomSource random, BlockPos floorPos, float densityScale, int updateFlags) {
      ensureCoralPortalSoil(level, floorPos, updateFlags);
      BlockState floor = level.getBlockState(floorPos);
      if (!floor.is(ModBlocks.ICHOR_SNOW_BLOCK) && !floor.is(ModBlocks.ICHOR_SNOW)) {
         Holder<Biome> biome = SiftCaveHabitat.biome(level, floorPos);
         if (TheSiftDimension.isCaveBiome(biome) && !SiftCaveHabitat.underground(level, floorPos)) {
            biome = SiftCaveHabitat.surfaceBiome(level, floorPos.getX(), floorPos.getZ());
         }

         boolean lush = TheSiftDimension.isOvergrownBiome(biome);
         boolean dry = biome.is(TheSiftDimension.SIFT_WASTES) || biome.is(TheSiftDimension.SIFTSLATE_CAVES);
         BlockPos plantPos = floorPos.above();
         BlockState existing = level.getBlockState(plantPos);
         if (level.ensureCanWrite(plantPos)
            && (
               lush && existing.is(ModBlocks.SIFTSLATE_STALKS)
                  || dry && (existing.is(ModBlocks.OVERGROWN_FRONDS) || existing.is(ModBlocks.OVERGROWN_STALKS) || existing.is(ModBlocks.OVERGROWN_CHARD))
            )) {
            level.setBlock(plantPos, Blocks.AIR.defaultBlockState(), updateFlags);
         }

         if (level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
            float roll = random.nextFloat();
            boolean commonFloor = floor.is(ModBlocks.SIFTSLATE)
               || floor.is(ModBlocks.SIFTSLATE_GROWTH)
               || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
               || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
               || floor.is(ModBlocks.HEALTHY_SCULK);
            float chance;
            Block plant;
            if (lush && commonFloor) {
               chance = 0.5F;
               plant = roll < 0.45F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.78F ? ModBlocks.OVERGROWN_STALKS : ModBlocks.OVERGROWN_CHARD);
            } else if (dry && commonFloor) {
               chance = 0.22F;
               plant = roll < 0.76F
                  ? ModBlocks.SIFTSLATE_STALKS
                  : (floor.is(ModBlocks.HEALTHY_SCULK) ? ModBlocks.HEALTHY_SCULK_SPROUTS : ModBlocks.DRY_HEALTHY_SCULK_SPROUTS);
            } else if (floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)) {
               chance = 0.42F;
               plant = ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT;
            } else if (floor.is(ModBlocks.HEALTHY_SCULK_PORES)) {
               chance = 0.24F;
               plant = ModBlocks.HEALTHY_SCULK_SPROUTS;
            } else if (floor.is(ModBlocks.SOULSHALE_GROWTH)) {
               chance = 0.42F;
               plant = roll < 0.65F ? ModBlocks.HEALTHY_SCULK_SPROUTS : ModBlocks.GRIM_BUSH;
            } else if (floor.is(ModBlocks.CORAL_SOIL) || floor.is(ModBlocks.CORAL_GROWTH) || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH)) {
               chance = 0.42F;
               plant = floor.is(ModBlocks.BRIGHT_CORAL_GROWTH) ? ModBlocks.BRIGHT_CORAL_BUSH : ModBlocks.CORAL_BUSH;
            } else if (floor.is(ModBlocks.SIFTSLATE_GROWTH) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
               chance = 0.72F;
               plant = roll < 0.4F ? ModBlocks.OVERGROWN_FRONDS : (roll < 0.72F ? ModBlocks.OVERGROWN_STALKS : ModBlocks.OVERGROWN_CHARD);
            } else if (floor.is(ModBlocks.SIFTSLATE)) {
               chance = 0.36F;
               plant = ModBlocks.SIFTSLATE_STALKS;
            } else if (floor.is(ModBlocks.HEALTHY_SCULK)) {
               chance = 0.42F;
               plant = ModBlocks.HEALTHY_SCULK_SPROUTS;
            } else {
               if (!floor.is(ModBlocks.DRY_HEALTHY_SCULK)) {
                  return false;
               }

               chance = 0.3F;
               plant = ModBlocks.DRY_HEALTHY_SCULK_SPROUTS;
            }

            if (random.nextFloat() >= chance * densityScale) {
               return false;
            }

            BlockState plantState = plant.defaultBlockState();
            if (!plantState.canSurvive(level, plantPos)) {
               return false;
            }

            level.setBlock(plantPos, plantState, updateFlags);
            SiftFlowerHabitat.record(level, plantPos, plantState);
            return true;
         } else {
            return false;
         }
      } else {
         return decoratePortalTerrain(level, random, floorPos, densityScale, updateFlags);
      }
   }
}
