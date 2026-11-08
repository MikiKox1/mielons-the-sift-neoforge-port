package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.OvergrownWillowVinesBlock;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SiftCaveVegetationFeature extends Feature<NoneFeatureConfiguration> {
   public SiftCaveVegetationFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos origin = context.origin();
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      SiftBlockSampler blocks = new SiftBlockSampler(level, minX, minZ);
      SiftBiomeSampler biomes = new SiftBiomeSampler(level, minX, minZ, 12, 220);
      List<BlockPos> lushFloors = new ArrayList<>();
      List<BlockPos> grimFloors = new ArrayList<>();
      List<BlockPos> dryFloors = new ArrayList<>();
      List<BlockPos> bushFloors = new ArrayList<>();
      List<BlockPos> siftslateFloors = new ArrayList<>();
      boolean changed = false;

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            int top = Math.min(220, Math.min(SiftCaveHabitat.rockTop(level, x, z), SiftCaveHabitat.surfaceRockBottom(level, x, z) - 1));

            for (int y = top; y >= 12; y--) {
               BlockPos floor = new BlockPos(x, y, z);
               if (!SiftTerrainFeatureMemory.isShelter(floor) && !SiftFeaturePlacementGuard.protectsPortalTerrain(floor)) {
                  BlockState state = blocks.get(floor);
                  if (!state.isAir()) {
                     if (biomes.get(floor).is(TheSiftDimension.SIFTSLATE_CAVES)) {
                        if ((
                              state.is(ModBlocks.SIFTSLATE)
                                 || state.is(ModBlocks.SIFTSLATE_GROWTH)
                                 || state.is(ModBlocks.HEALTHY_SCULK)
                                 || state.is(ModBlocks.DRY_HEALTHY_SCULK)
                                 || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
                           )
                           && blocks.get(floor.above()).isAir()
                           && blocks.get(floor.above(2)).isAir()
                           && SiftCaveHabitat.plantFloor(level, floor)) {
                           siftslateFloors.add(floor);
                           y -= 2;
                        }
                     } else {
                        boolean lush = (state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH))
                           && biomes.get(floor).is(TheSiftDimension.OVERGROWN_CAVES);
                        boolean grim = (
                              state.is(ModBlocks.SOULSHALE_GROWTH)
                                 || state.is(ModBlocks.SOULSHALE)
                                 || state.is(ModBlocks.DRY_HEALTHY_SCULK)
                                 || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
                           )
                           && biomes.get(floor).is(TheSiftDimension.SOULSHALE_CAVES);
                        if ((lush || grim)
                           && blocks.get(floor.above()).isAir()
                           && blocks.get(floor.above(2)).isAir()
                           && (lush ? SiftCaveHabitat.broadFloor(level, floor) : SiftCaveHabitat.plantFloor(level, floor))) {
                           if (lush) {
                              lushFloors.add(floor);
                           } else if (!state.is(ModBlocks.DRY_HEALTHY_SCULK) && !state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
                              bushFloors.add(floor);
                              if (state.is(ModBlocks.SOULSHALE_GROWTH)) {
                                 grimFloors.add(floor);
                              }
                           } else {
                              dryFloors.add(floor);
                           }

                           y -= 2;
                        }
                     }
                  }
               }
            }
         }
      }

      changed |= clumps(level, random, lushFloors, minX, minZ, 0);
      changed |= clumps(level, random, grimFloors, minX, minZ, 1);
      changed |= clumps(level, random, dryFloors, minX, minZ, 2);
      changed |= clumps(level, random, bushFloors, minX, minZ, 3);
      changed |= SiftClearingPlants.decorate(level, random, lushFloors);
      changed |= SiftslateCavePlants.decorate(level, random, siftslateFloors);

      for (BlockPos floor : grimFloors) {
         if (random.nextFloat() < 0.68F) {
            changed |= plant(level, floor.above(), ModBlocks.HEALTHY_SCULK_SPROUTS.defaultBlockState());
         }
      }

      for (BlockPos floor : dryFloors) {
         if (random.nextFloat() < 0.64F) {
            changed |= plant(level, floor.above(), ModBlocks.DRY_HEALTHY_SCULK_SPROUTS.defaultBlockState());
         }
      }

      return wallVines(level, random, minX, minZ) || changed;
   }

   private static boolean clumps(WorldGenLevel level, RandomSource random, List<BlockPos> floors, int minX, int minZ, int habitat) {
      Map<Integer, List<BlockPos>> levels = new TreeMap<>();

      for (BlockPos floor : floors) {
         if (floor.getX() >= minX + 3 && floor.getX() <= minX + 12 && floor.getZ() >= minZ + 3 && floor.getZ() <= minZ + 12) {
            levels.computeIfAbsent(Math.floorDiv(floor.getY(), 14), ignored -> new ArrayList<>()).add(floor);
         }
      }

      boolean changed = false;

      for (List<BlockPos> candidates : levels.values()) {
         if (candidates.size() >= 3 && (habitat != 0 || !(random.nextFloat() > 0.75F))) {
            List<BlockPos> anchors = new ArrayList<>();
            int kinds = habitat == 0 ? 1 : (habitat == 3 ? 2 + random.nextInt(2) : grimClumpKinds());
            int first = random.nextInt(kinds);

            for (int batch = 0; batch < kinds; batch++) {
               int kind = (first + batch) % kinds;
               BlockPos anchor = null;

               for (int attempt = 0; attempt < 16 && anchor == null; attempt++) {
                  BlockPos candidate = candidates.get(random.nextInt(candidates.size()));
                  boolean spaced = true;

                  for (BlockPos previous : anchors) {
                     int dx = previous.getX() - candidate.getX();
                     int dz = previous.getZ() - candidate.getZ();
                     if (dx * dx + dz * dz < (habitat == 3 ? 16 : 25)) {
                        spaced = false;
                     }
                  }

                  if (spaced) {
                     anchor = candidate;
                  }
               }

               if (anchor != null) {
                  anchors.add(anchor);
                  boolean wildflowers = habitat != 3 && wildflowerClump(habitat != 0, kind, random);
                  Block var10000;
                  if (habitat == 3) {
                     var10000 = ModBlocks.GRIM_BUSH;
                  } else if (habitat != 0) {
                     var10000 = ModBlocks.WHISPERBLOOM;
                  } else {
                     switch (random.nextInt(3)) {
                        case 0:
                           var10000 = SiftFlowerPatchFeature.saucerBloom(random);
                           break;
                        case 1:
                           var10000 = ModBlocks.SUNBURST_PLANT;
                           break;
                        default:
                           var10000 = ModBlocks.WHISPERBLOOM;
                     }
                  }

                  Block flower = var10000;
                  changed |= SiftFlowerPatchFeature.placeCavePatch(level, random, anchor, flower, wildflowers) > 0;
               }
            }

            if (habitat == 0 && random.nextBoolean()) {
               for (int attempt = 0; attempt < 16; attempt++) {
                  BlockPos candidate = candidates.get(random.nextInt(candidates.size()));
                  boolean separated = true;

                  for (BlockPos previous : anchors) {
                     int dx = previous.getX() - candidate.getX();
                     int dz = previous.getZ() - candidate.getZ();
                     if (dx * dx + dz * dz < 25) {
                        separated = false;
                     }
                  }

                  if (separated) {
                     changed |= SiftFlowerPatchFeature.placeCavePatch(level, random, candidate, SiftFlowerPatchFeature.saucerBloom(random), false) > 0;
                     break;
                  }
               }
            }
         }
      }

      return changed;
   }

   private static int grimClumpKinds() {
      return 2;
   }

   private static boolean wildflowerClump(boolean grim, int kind, RandomSource random) {
      return grim ? kind == 1 : random.nextBoolean();
   }

   private static boolean plant(WorldGenLevel level, BlockPos pos, BlockState plant) {
      if (!SiftTerrainFeatureMemory.isShelter(pos) && level.isEmptyBlock(pos) && level.ensureCanWrite(pos) && plant.canSurvive(level, pos)) {
         level.setBlock(pos, plant, 2);
         return true;
      } else {
         return false;
      }
   }

   private static boolean wallVines(WorldGenLevel level, RandomSource random, int minX, int minZ) {
      if (random.nextInt(5) == 0) {
         return false;
      }

      boolean changed = false;
      int curtains = 0;
      int target = 2 + random.nextInt(3);
      List<BlockPos> anchors = new ArrayList<>();

      for (int attempt = 0; attempt < 28 && curtains < target; attempt++) {
         int x = minX + 2 + random.nextInt(12);
         int z = minZ + 2 + random.nextInt(12);
         int top = Math.min(200, Math.min(SiftCaveHabitat.rockTop(level, x, z) + 1, SiftCaveHabitat.surfaceRockBottom(level, x, z) - 1));
         boolean curtain = false;

         for (int y = top; y >= 18 && !curtain; y--) {
            BlockPos root = new BlockPos(x, y, z);
            if (level.isEmptyBlock(root) && level.getBiome(root).is(TheSiftDimension.OVERGROWN_CAVES)) {
               boolean nearby = false;

               for (BlockPos previous : anchors) {
                  if (previous.distSqr(root) < 64.0) {
                     nearby = true;
                  }
               }

               if (!nearby) {
                  for (Direction face : Plane.HORIZONTAL) {
                     BlockPos wall = root.relative(face);
                     if (SiftLandmarkTerrain.isGround(level.getBlockState(wall)) && level.getBlockState(wall).isFaceSturdy(level, wall, face.getOpposite())) {
                        Direction across = face.getClockWise();
                        int length = 8 + random.nextInt(9);
                        int width = 3 + random.nextInt(3);

                        for (int strand = 0; strand < width; strand++) {
                           BlockPos start = root.relative(across, strand - 1);
                           int strandLength = length - random.nextInt(3);

                           for (int down = 0; down < strandLength; down++) {
                              BlockPos pos = start.below(down);
                              if (SiftTerrainFeatureMemory.isShelter(pos)
                                 || !level.isEmptyBlock(pos)
                                 || !level.ensureCanWrite(pos)
                                 || !SiftCaveHabitat.lush(level, pos)) {
                                 break;
                              }

                              BlockState vine = ModBlocks.OVERGROWN_WILLOW_VINES.defaultBlockState().setValue(VineBlock.getPropertyForFace(face), true);
                              if (!vine.canSurvive(level, pos)) {
                                 break;
                              }

                              level.setBlock(pos, vine, 2);
                              if (down > 0) {
                                 BlockPos above = pos.above();
                                 level.setBlock(above, OvergrownWillowVinesBlock.refreshBottomFaces(level.getBlockState(above), vine), 2);
                              }

                              changed = true;
                              curtain = true;
                           }
                        }

                        if (curtain) {
                           anchors.add(root);
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (curtain) {
            curtains++;
         }
      }

      return changed;
   }
}
