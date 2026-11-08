package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class CoralSurfaceFeature implements Feature {
   public static final MapCodec<CoralSurfaceFeature> SURFACE_CODEC = MapCodec.unit(() -> new CoralSurfaceFeature(false));
   public static final MapCodec<CoralSurfaceFeature> VEGETATION_CODEC = MapCodec.unit(() -> new CoralSurfaceFeature(true));
   private final boolean vegetation;

   public MapCodec<CoralSurfaceFeature> codec() {
      return this.vegetation ? VEGETATION_CODEC : SURFACE_CODEC;
   }

   public CoralSurfaceFeature(boolean vegetation) {
      this.vegetation = vegetation;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      int bottom = Math.max(5, level.getMinY());
      int top = Math.min(240, level.getMaxY() - 1);
      SiftCampProtection camp = SiftCampProtection.around(level, minX, minZ, minX + 15, minZ + 15);
      boolean changed = false;
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            boolean coralColumn = TheSiftDimension.isCoralBiome(SiftCaveHabitat.surfaceBiome(level, x, z));
            int rockTop = SiftCaveHabitat.surfaceRockBottom(level, x, z);

            for (int y = top; y >= bottom; y--) {
               pos.set(x, y, z);
               if (!camp.contains(pos)) {
                  BlockState state = level.getBlockState(pos);
                  if (isCoralRock(state)
                     && (TheSiftDimension.isCoralBiome(level.getBiome(pos)) || coralColumn && y >= rockTop)
                     && !SiftTerrainFeatureMemory.isCanyonWall(x, y, z)
                     && level.ensureCanWrite(pos)
                     && !SiftFeaturePlacementGuard.protectsPortalTerrain(pos)
                     && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)) {
                     Block rock = hostRock(state);
                     if (rock != null && !state.is(rock)) {
                        level.setBlock(pos, rock.defaultBlockState(), 2);
                        state = rock.defaultBlockState();
                        changed = true;
                     }

                     if (!isOre(state)) {
                        BlockPos above = pos.above();
                        BlockState occupant = level.getBlockState(above);
                        if (!occupant.isAir() && !occupant.is(ModBlocks.CORAL_BUSH) && !occupant.is(ModBlocks.BRIGHT_CORAL_BUSH)) {
                           if (SiftLandmarkTerrain.isGround(occupant) && (state.is(ModBlocks.CORAL_GROWTH) || state.is(ModBlocks.BRIGHT_CORAL_GROWTH))) {
                              level.setBlock(pos, ModBlocks.SOULSHALE.defaultBlockState(), 2);
                              changed = true;
                           }
                        } else if (!SiftFeaturePlacementGuard.protectsPortalTerrain(pos)) {
                           boolean bright = state.is(ModBlocks.BRIGHT_CORAL_GROWTH) || brightPatch(level.getSeed(), x, z);
                           BlockState growth = (bright ? ModBlocks.BRIGHT_CORAL_GROWTH : ModBlocks.CORAL_GROWTH).defaultBlockState();
                           if (!state.equals(growth)) {
                              level.setBlock(pos, growth, 2);
                              changed = true;
                           }

                           int soilDepth = SiftTerrainShapeNoise.noise(level.getSeed() ^ 53081457L, x * 0.8 + z * 0.6, 24.0) > 0.0 ? 4 : 3;

                           for (int depth = 1; depth <= soilDepth; depth++) {
                              BlockPos soil = pos.below(depth);
                              if (camp.contains(soil)
                                 || !SiftLandmarkTerrain.isGround(level.getBlockState(soil))
                                 || !level.ensureCanWrite(soil)
                                 || SiftTerrainFeatureMemory.isCanyonWall(x, soil.getY(), z)) {
                                 break;
                              }

                              if (!isOre(level.getBlockState(soil))) {
                                 level.setBlock(soil, ModBlocks.CORAL_SOIL.defaultBlockState(), 2);
                              }
                           }

                           for (int depth = soilDepth + 1; depth <= soilDepth + 4; depth++) {
                              BlockPos soil = pos.below(depth);
                              if (camp.contains(soil) || !level.getBlockState(soil).is(ModBlocks.CORAL_SOIL) || !level.ensureCanWrite(soil)) {
                                 break;
                              }

                              level.setBlock(soil, ModBlocks.SOULSHALE.defaultBlockState(), 2);
                           }

                           if (this.vegetation
                              && (occupant.isAir() || occupant.is(ModBlocks.CORAL_BUSH) || occupant.is(ModBlocks.BRIGHT_CORAL_BUSH))
                              && level.ensureCanWrite(above)) {
                              if (BlubbyBerriesPatches.tryPlace(level, random, pos, camp)) {
                                 changed = true;
                              } else {
                                 float density = 0.66F + 0.08F * (float)SiftTerrainShapeNoise.unit(level.getSeed() + x * 131L + z * 173L);
                                 BlockState bush = (bright ? ModBlocks.BRIGHT_CORAL_BUSH : ModBlocks.CORAL_BUSH).defaultBlockState();
                                 if (SiftTerrainShapeNoise.unit(
                                          level.getSeed() ^ x * -7046029254386353131L ^ z * 7146057691288625177L ^ y * -7723592293110705685L
                                       )
                                       < density
                                    && bush.canSurvive(level, above)) {
                                    level.setBlock(above, bush, 2);
                                    changed = true;
                                 } else if (!occupant.isAir()) {
                                    level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                                    changed = true;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean isOre(BlockState state) {
      return state.is(ModBlocks.SOULSHALE_COAL_ORE)
         || state.is(ModBlocks.SOULSHALE_DIAMOND_ORE)
         || state.is(ModBlocks.SOULSHALE_EMERALD_ORE)
         || state.is(ModBlocks.SOULSHALE_CHAROITE_ORE)
         || state.is(ModBlocks.SOULSHALE_SIFTITE_ORE);
   }

   private static boolean isCoralRock(BlockState state) {
      return hostRock(state) != null
         || state.is(ModBlocks.SOULSHALE)
         || state.is(ModBlocks.CORAL_GROWTH)
         || state.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || state.is(ModBlocks.CORAL_SOIL)
         || state.is(ModBlocks.SOULSHALE_COAL_ORE)
         || state.is(ModBlocks.SOULSHALE_DIAMOND_ORE)
         || state.is(ModBlocks.SOULSHALE_EMERALD_ORE)
         || state.is(ModBlocks.SOULSHALE_CHAROITE_ORE)
         || state.is(ModBlocks.SOULSHALE_SIFTITE_ORE);
   }

   static Block hostRock(BlockState state) {
      if (state.is(ModBlocks.SIFTSLATE_COAL_ORE)) {
         return ModBlocks.SOULSHALE_COAL_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_DIAMOND_ORE)) {
         return ModBlocks.SOULSHALE_DIAMOND_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_EMERALD_ORE)) {
         return ModBlocks.SOULSHALE_EMERALD_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_CHAROITE_ORE)) {
         return ModBlocks.SOULSHALE_CHAROITE_ORE;
      } else if (state.is(ModBlocks.SIFTSLATE_SIFTITE_ORE)) {
         return ModBlocks.SOULSHALE_SIFTITE_ORE;
      } else {
         return !state.is(ModBlocks.SOULSHALE_GROWTH)
               && !state.is(ModBlocks.SIFTSLATE)
               && !state.is(ModBlocks.COBBLED_SIFTSLATE)
               && !state.is(ModBlocks.SIFT_RUBBLE)
               && !state.is(ModBlocks.HEALTHY_SCULK)
               && !state.is(ModBlocks.DRY_HEALTHY_SCULK)
               && !state.is(ModBlocks.SIFTSLATE_GROWTH)
               && !state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
            ? null
            : ModBlocks.SOULSHALE;
      }
   }

   public static boolean brightPatch(long seed, int x, int z) {
      int cellX = Math.floorDiv(x, 32);
      int cellZ = Math.floorDiv(z, 32);
      double warpX = 1.5 * SiftTerrainShapeNoise.noise(seed + 19L, z + x * 0.2, 9.0);
      double warpZ = 1.5 * SiftTerrainShapeNoise.noise(seed + 37L, x - z * 0.2, 9.0);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            long salt = seed ^ (cellX + dx) * -7046029254386353131L ^ (cellZ + dz) * 7146057691288625177L;
            if (!(SiftTerrainShapeNoise.unit(salt + 51L) < 0.35)) {
               double cx = (cellX + dx) * 32 + 5 + 22.0 * SiftTerrainShapeNoise.unit(salt + 71L);
               double cz = (cellZ + dz) * 32 + 5 + 22.0 * SiftTerrainShapeNoise.unit(salt + 91L);
               double rx = 4.5 + 3.0 * SiftTerrainShapeNoise.unit(salt + 111L);
               double rz = 4.5 + 3.0 * SiftTerrainShapeNoise.unit(salt + 131L);
               if (Math.pow((x + warpX - cx) / rx, 2.0) + Math.pow((z + warpZ - cz) / rz, 2.0) < 1.0) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
