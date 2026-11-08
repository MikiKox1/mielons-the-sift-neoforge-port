package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftCaveGeologyFeature implements Feature {
   public static final MapCodec<SiftCaveGeologyFeature> CODEC = MapCodec.unit(SiftCaveGeologyFeature::new);

   public MapCodec<SiftCaveGeologyFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      int minY = Math.max(5, level.getMinY());
      int maxY = Math.min(240, level.getMaxY() - 3);
      SiftBlockSampler blocks = new SiftBlockSampler(level, minX, minZ);
      SiftBiomeSampler biomes = new SiftBiomeSampler(level, minX, minZ, minY, maxY);
      SiftCaveLayers layers = new SiftCaveLayers(level.getSeed());
      SiftGrimGrowthPattern growth = new SiftGrimGrowthPattern(level.getSeed());
      SiftCampProtection camp = SiftCampProtection.around(level, minX, minZ, minX + 15, minZ + 15);
      boolean changed = false;
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            int[] bands = layers.column(x, z, minY, maxY);
            int surface = SiftCaveHabitat.surfaceRockBottom(level, x, z);
            boolean coralSurface = TheSiftDimension.isGrimSurfaceBiome(SiftCaveHabitat.surfaceBiome(level, x, z));

            for (int y = maxY - 1; y >= minY; y--) {
               pos.set(x, y, z);
               if (!camp.contains(pos)) {
                  BlockState source = blocks.get(pos);
                  if (!source.isAir()
                     && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)
                     && !SiftFeaturePlacementGuard.protectsPortalTerrain(pos)
                     && isRock(source)) {
                     if (!biomes.get(pos).is(TheSiftDimension.CORRUPTED_SIFT) || !source.is(ModBlocks.SIFTSLATE) && !source.is(ModBlocks.SOULSHALE)) {
                        if (y >= surface) {
                           if (!coralSurface) {
                              Block surfaceRock = siftslateOre(source);
                              if (surfaceRock == null && (source.is(ModBlocks.SOULSHALE) || source.is(ModBlocks.SOULSHALE_GROWTH))) {
                                 surfaceRock = ModBlocks.SIFTSLATE;
                              }

                              if (surfaceRock != null && level.ensureCanWrite(pos)) {
                                 level.setBlock(pos, surfaceRock.defaultBlockState(), 2);
                                 changed = true;
                              }
                           }
                        } else {
                           Holder<Biome> biome = biomes.get(pos);
                           boolean grim = biome.is(TheSiftDimension.SOULSHALE_CAVES);
                           boolean lush = biome.is(TheSiftDimension.OVERGROWN_CAVES);
                           if ((grim || lush || biome.is(TheSiftDimension.SIFTSLATE_CAVES))
                              && (!grim || !source.is(ModBlocks.SIFT_RUBBLE))
                              && (
                                 !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)
                                    || source.is(ModBlocks.SIFTSLATE_GROWTH)
                                    || source.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
                                    || source.is(ModBlocks.SOULSHALE_GROWTH)
                              )) {
                              int band = bands[y - minY];
                              int floorY = exposedFloorAbove(blocks, pos);
                              Block target;
                              if (floorY != Integer.MIN_VALUE) {
                                 if (grim) {
                                    boolean spaghetti = SiftCaveHabitat.spaghettiFloor(level, pos);
                                    boolean soil = !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z) && SiftCaveHabitat.plantFloor(level, pos);
                                    boolean green = spaghetti ? growth.spaghettiGrowth(x, z) : growth.grows(x, z);
                                    target = soil && spaghetti && green
                                       ? ModBlocks.SOULSHALE_GROWTH
                                       : (
                                          soil && growth.dry(x, z, spaghetti)
                                             ? ModBlocks.DRY_HEALTHY_SCULK
                                             : (soil && green ? ModBlocks.SOULSHALE_GROWTH : ModBlocks.SOULSHALE)
                                       );
                                 } else if (!lush) {
                                    target = source.is(ModBlocks.SOULSHALE) ? ModBlocks.SIFTSLATE : null;
                                 } else {
                                    boolean grow = !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z) && SiftCaveHabitat.broadFloor(level, pos);
                                    if (grow) {
                                       target = layers.dryFloor(x, z) ? ModBlocks.DRY_HEALTHY_SCULK_GROWTH : ModBlocks.SIFTSLATE_GROWTH;
                                    } else {
                                       target = source.is(ModBlocks.SIFTSLATE_GROWTH)
                                          ? ModBlocks.SIFTSLATE
                                          : (
                                             source.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
                                                ? ModBlocks.DRY_HEALTHY_SCULK
                                                : (source.is(ModBlocks.SOULSHALE) ? ModBlocks.SIFTSLATE : null)
                                          );
                                    }
                                 }
                              } else if (band != 0 && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z) && exposedWall(blocks, pos)) {
                                 target = grim ? ModBlocks.COBBLED_SIFTSLATE : (lush ? ModBlocks.DRY_HEALTHY_SCULK : ModBlocks.SOULSHALE);
                              } else {
                                 target = grim
                                    ? ModBlocks.SOULSHALE
                                    : (
                                       !lush
                                             || !source.is(ModBlocks.SIFTSLATE)
                                                && !source.is(ModBlocks.SOULSHALE)
                                                && !source.is(ModBlocks.SIFTSLATE_GROWTH)
                                                && !source.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
                                          ? null
                                          : ModBlocks.SIFTSLATE
                                    );
                              }

                              Block ore = grimOre(source);
                              if (ore != null) {
                                 if (target != ModBlocks.SOULSHALE && target != ModBlocks.SOULSHALE_GROWTH) {
                                    continue;
                                 }

                                 target = ore;
                              } else if (isGrimOre(source)) {
                                 continue;
                              }

                              if (target != null && !source.is(target) && level.ensureCanWrite(pos)) {
                                 level.setBlock(pos, target.defaultBlockState(), 2);
                                 changed = true;
                              }
                           }
                        }
                     } else {
                        BlockState rock = CorruptedSiftStone.at(pos, level.getSeed());
                        if (!source.equals(rock) && level.ensureCanWrite(pos)) {
                           level.setBlock(pos, rock, 2);
                           changed = true;
                        }
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean exposedWall(SiftBlockSampler blocks, BlockPos pos) {
      for (Direction direction : Plane.HORIZONTAL) {
         if (clear(blocks.get(pos.relative(direction)))) {
            return true;
         }
      }

      return false;
   }

   private static int exposedFloorAbove(SiftBlockSampler blocks, BlockPos pos) {
      return clear(blocks.get(pos.above())) ? pos.getY() : Integer.MIN_VALUE;
   }

   private static boolean clear(BlockState state) {
      return state.getFluidState().isEmpty() && !SiftLandmarkTerrain.isGround(state) && SiftLandmarkTerrain.canReplace(state);
   }

   private static boolean isRock(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE)
         || state.is(ModBlocks.SOULSHALE)
         || state.is(ModBlocks.SOULSHALE_GROWTH)
         || state.is(ModBlocks.COBBLED_SIFTSLATE)
         || state.is(ModBlocks.SIFTSLATE_GROWTH)
         || state.is(ModBlocks.SIFT_RUBBLE)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || grimOre(state) != null
         || isGrimOre(state);
   }

   private static Block siftslateOre(BlockState state) {
      if (state.is(ModBlocks.SOULSHALE_COAL_ORE)) {
         return ModBlocks.SIFTSLATE_COAL_ORE;
      } else if (state.is(ModBlocks.SOULSHALE_DIAMOND_ORE)) {
         return ModBlocks.SIFTSLATE_DIAMOND_ORE;
      } else if (state.is(ModBlocks.SOULSHALE_EMERALD_ORE)) {
         return ModBlocks.SIFTSLATE_EMERALD_ORE;
      } else if (state.is(ModBlocks.SOULSHALE_CHAROITE_ORE)) {
         return ModBlocks.SIFTSLATE_CHAROITE_ORE;
      } else {
         return state.is(ModBlocks.SOULSHALE_SIFTITE_ORE) ? ModBlocks.SIFTSLATE_SIFTITE_ORE : null;
      }
   }

   private static Block grimOre(BlockState state) {
      if (state.is(ModBlocks.SIFTSLATE_COAL_ORE)) {
         return ModBlocks.SOULSHALE_COAL_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_DIAMOND_ORE)) {
         return ModBlocks.SOULSHALE_DIAMOND_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_EMERALD_ORE)) {
         return ModBlocks.SOULSHALE_EMERALD_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_CHAROITE_ORE)) {
         return ModBlocks.SOULSHALE_CHAROITE_ORE;
      } else {
         return state.is(ModBlocks.SIFTSLATE_SIFTITE_ORE) ? ModBlocks.SOULSHALE_SIFTITE_ORE : null;
      }
   }

   private static boolean isGrimOre(BlockState state) {
      return state.is(ModBlocks.SOULSHALE_COAL_ORE)
         || state.is(ModBlocks.SOULSHALE_DIAMOND_ORE)
         || state.is(ModBlocks.SOULSHALE_EMERALD_ORE)
         || state.is(ModBlocks.SOULSHALE_CHAROITE_ORE)
         || state.is(ModBlocks.SOULSHALE_SIFTITE_ORE);
   }
}
