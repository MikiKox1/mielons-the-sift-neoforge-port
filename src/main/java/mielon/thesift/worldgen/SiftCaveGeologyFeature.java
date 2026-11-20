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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
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
      ChunkAccess chunk = blocks.chunk();
      LevelChunkSection[] sections = chunk.getSections();
      boolean[] rockSections = new boolean[sections.length];

      for (int index = 0; index < sections.length; index++) {
         rockSections[index] = !sections[index].hasOnlyAir() && sections[index].maybeHas(SiftCaveGeologyFeature::isRock);
      }

      SiftBiomeSampler biomes = new SiftBiomeSampler(level, minX, minZ, minY, maxY);
      SiftCaveLayers layers = new SiftCaveLayers(level.getSeed());
      SiftGrimGrowthPattern growth = new SiftGrimGrowthPattern(level.getSeed());
      SiftCampProtection camp = SiftCampProtection.around(level, minX, minZ, minX + 15, minZ + 15);
      boolean changed = false;
      int[] bands = new int[maxY - minY];
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            layers.fillColumn(x, z, minY, bands);
            int surface = SiftCaveHabitat.surfaceRockBottom(level, x, z);
            boolean coralSurface = TheSiftDimension.isGrimSurfaceBiome(SiftCaveHabitat.surfaceBiome(level, x, z));
            BlockState above = null;

            for (int y = maxY - 1; y >= minY; y--) {
               if (!rockSections[chunk.getSectionIndex(y)]) {
                  y &= -16;
                  above = null;
               } else {
                  pos.set(x, y, z);
                  BlockState source = blocks.get(pos);
                  BlockState previous = above;
                  above = source;
                  if (!camp.contains(pos)
                     && !source.isAir()
                     && isRock(source)
                     && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)
                     && !SiftFeaturePlacementGuard.protectsPortalTerrain(pos)) {
                     Holder<Biome> biome = biomes.get(pos);
                     if ((source.is(ModBlocks.SIFTSLATE) || source.is(ModBlocks.SOULSHALE)) && biome.is(TheSiftDimension.CORRUPTED_SIFT)) {
                        BlockState rock = CorruptedSiftStone.at(pos, level.getSeed());
                        if (!source.equals(rock) && level.ensureCanWrite(pos)) {
                           level.setBlock(pos, rock, 2);
                           above = null;
                           changed = true;
                        }
                     } else if (y >= surface) {
                        if (!coralSurface) {
                           Block surfaceRock = siftslateOre(source);
                           if (surfaceRock == null && (source.is(ModBlocks.SOULSHALE) || source.is(ModBlocks.SOULSHALE_GROWTH))) {
                              surfaceRock = ModBlocks.SIFTSLATE;
                           }

                           if (surfaceRock != null && level.ensureCanWrite(pos)) {
                              level.setBlock(pos, surfaceRock.defaultBlockState(), 2);
                              above = null;
                              changed = true;
                           }
                        }
                     } else {
                        boolean grim = biome.is(TheSiftDimension.SOULSHALE_CAVES);
                        boolean lush = biome.is(TheSiftDimension.OVERGROWN_CAVES);
                        if ((grim || lush || biome.is(TheSiftDimension.SIFTSLATE_CAVES)) && (!grim || !source.is(ModBlocks.SIFT_RUBBLE))) {
                           int band = bands[y - minY];
                           boolean exposed = clear(previous != null ? previous : blocks.get(x, y + 1, z));
                           Block target;
                           if (exposed) {
                              if (grim) {
                                 boolean spaghetti = SiftCaveHabitat.spaghettiFloor(level, pos);
                                 boolean soil = SiftCaveHabitat.plantFloor(level, pos);
                                 boolean green = spaghetti ? growth.spaghettiGrowth(x, z) : growth.grows(x, z);
                                 target = soil && spaghetti && green
                                    ? ModBlocks.SOULSHALE_GROWTH
                                    : (
                                       soil && growth.dry(x, z, spaghetti)
                                          ? ModBlocks.DRY_HEALTHY_SCULK
                                          : (soil && green ? ModBlocks.SOULSHALE_GROWTH : ModBlocks.SOULSHALE)
                                    );
                              } else if (lush) {
                                 boolean grow = SiftCaveHabitat.broadFloor(level, pos);
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
                              } else {
                                 target = source.is(ModBlocks.SOULSHALE) ? ModBlocks.SIFTSLATE : null;
                              }
                           } else if (band != 0 && exposedWall(blocks, pos)) {
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
                              above = null;
                              changed = true;
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

   private static boolean exposedWall(SiftBlockSampler blocks, BlockPos pos) {
      for (Direction direction : Plane.HORIZONTAL) {
         if (clear(blocks.get(pos.getX() + direction.getStepX(), pos.getY(), pos.getZ() + direction.getStepZ()))) {
            return true;
         }
      }

      return false;
   }

   private static boolean clear(BlockState state) {
      return !SiftLandmarkTerrain.isGround(state) && state.getFluidState().isEmpty() && SiftLandmarkTerrain.canReplace(state);
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
