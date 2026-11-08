package mielon.thesift.worldgen;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftslateGrowthBlock;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class CoveredGrowthCleanupFeature extends Feature<NoneFeatureConfiguration> {
   public CoveredGrowthCleanupFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      boolean coral = TheSiftDimension.isCoralBiome(SiftCaveHabitat.surfaceBiome(level, context.origin().getX(), context.origin().getZ()));
      if (coral) {
         new CoralSurfaceFeature(true).place(context);
      }

      int minX = context.origin().getX() & -16;
      int minZ = context.origin().getZ() & -16;
      SiftBlockSampler blocks = new SiftBlockSampler(level, minX, minZ);
      int maxY = level.getMaxY() - 1;
      MutableBlockPos cursor = new MutableBlockPos();
      MutableBlockPos above = new MutableBlockPos();
      SiftCampProtection camp = SiftCampProtection.around(level, minX, minZ, minX + 15, minZ + 15);
      boolean changed = false;

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            int surface = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
            boolean coralColumn = TheSiftDimension.isGrimSurfaceBiome(SiftCaveHabitat.surfaceBiome(level, x, z));
            int rockTop = coralColumn ? SiftCaveHabitat.surfaceRockBottom(level, x, z) : surface;
            int minY = level.getMinY();
            int columnMaxY = Math.min(maxY, surface);

            for (int y = minY; y <= columnMaxY; y++) {
               cursor.set(x, y, z);
               if (!camp.contains(cursor)) {
                  BlockState state = blocks.get(cursor);
                  if (!state.isAir() && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)) {
                     if (coralColumn
                        && y >= rockTop
                        && !SiftTerrainFeatureMemory.isCanyonWall(x, y, z)
                        && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)) {
                        Block host = CoralSurfaceFeature.hostRock(state);
                        if (host != null) {
                           state = host.defaultBlockState();
                           level.setBlock(cursor, state, 2);
                           changed = true;
                        }
                     }

                     if (!theSift$isLakePlant(state) || state.canSurvive(level, cursor) && !theSift$hasUnsupportedIchorBelow(level, cursor)) {
                        if (state.is(ModBlocks.SOULSHALE_GROWTH)) {
                           BlockState cover = blocks.get(cursor.above());
                           boolean snowy = SiftslateGrowthBlock.isIchorSnowCover(cover);
                           if (!SiftCaveHabitat.grim(level, cursor)
                              || !snowy && (cover.canOcclude() && cover.isCollisionShapeFullBlock(level, cursor.above()) || !cover.getFluidState().isEmpty())) {
                              level.setBlock(
                                 cursor,
                                 (!coralColumn && !SiftCaveHabitat.underground(level, cursor) ? ModBlocks.SIFTSLATE : ModBlocks.SOULSHALE).defaultBlockState(),
                                 2
                              );
                              changed = true;
                           } else if (state.getValue(SiftslateGrowthBlock.SNOWY) != snowy) {
                              level.setBlock(cursor, state.setValue(SiftslateGrowthBlock.SNOWY, snowy), 2);
                              changed = true;
                           }
                        } else if (state.is(ModBlocks.SIFTSLATE_GROWTH) || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
                           above.set(x, y + 1, z);
                           BlockState coveringState = blocks.get(above);
                           boolean dry = state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH) || blocks.get(cursor.below()).is(ModBlocks.DRY_HEALTHY_SCULK);
                           boolean ichorSnowCover = SiftslateGrowthBlock.isIchorSnowCover(coveringState);
                           if (SiftLandmarkTerrain.allowsGrowth(level, cursor)
                              && (
                                 (!coveringState.canOcclude() || !coveringState.isCollisionShapeFullBlock(level, above))
                                       && coveringState.getFluidState().isEmpty()
                                    || ichorSnowCover
                              )) {
                              if (dry && state.is(ModBlocks.SIFTSLATE_GROWTH) && !level.getBiome(cursor).is(TheSiftDimension.OVERGROWN_CAVES)) {
                                 level.setBlock(cursor, ModBlocks.DRY_HEALTHY_SCULK_GROWTH.defaultBlockState(), 2);
                                 changed = true;
                              } else if (state.is(ModBlocks.SIFTSLATE_GROWTH) && state.getValue(SiftslateGrowthBlock.SNOWY) != ichorSnowCover) {
                                 level.setBlock(cursor, state.setValue(SiftslateGrowthBlock.SNOWY, ichorSnowCover), 2);
                                 changed = true;
                              }
                           } else {
                              level.setBlock(cursor, (dry ? ModBlocks.DRY_HEALTHY_SCULK : ModBlocks.SIFTSLATE).defaultBlockState(), 2);
                              changed = true;
                           }
                        }
                     } else {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                        changed = true;
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean theSift$isLakePlant(BlockState state) {
      return state.is(ModBlocks.CORAL_BUSH)
         || state.is(ModBlocks.BRIGHT_CORAL_BUSH)
         || state.is(ModBlocks.GRIM_BUSH)
         || state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.BLUE_SAUCER_BLOOM)
         || state.is(ModBlocks.GREEN_SAUCER_BLOOM)
         || state.is(ModBlocks.SUNBURST_PLANT)
         || state.is(ModBlocks.WHISPERBLOOM)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(Blocks.WILDFLOWERS)
         || state.is(Blocks.TORCHFLOWER)
         || state.is(Blocks.TORCHFLOWER_CROP)
         || state.is(Blocks.PITCHER_CROP)
         || state.is(Blocks.PITCHER_PLANT);
   }

   private static boolean theSift$hasUnsupportedIchorBelow(WorldGenLevel level, BlockPos plantPos) {
      MutableBlockPos below = new MutableBlockPos();

      for (int offset = 1; offset <= 6; offset++) {
         below.set(plantPos.getX(), plantPos.getY() - offset, plantPos.getZ());
         BlockState state = level.getBlockState(below);
         if (!state.getFluidState().isEmpty()) {
            return state.getFluidState().getType() == ModFluids.ICHOR || state.getFluidState().getType() == ModFluids.FLOWING_ICHOR;
         }

         if (!state.isAir()) {
            return false;
         }
      }

      return false;
   }
}
