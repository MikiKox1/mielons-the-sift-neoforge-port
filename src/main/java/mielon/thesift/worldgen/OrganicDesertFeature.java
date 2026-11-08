package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.IchorFluid;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class OrganicDesertFeature implements Feature {
   public static final MapCodec<OrganicDesertFeature> SURFACE_CODEC = MapCodec.unit(() -> new OrganicDesertFeature(0));
   public static final MapCodec<OrganicDesertFeature> CHANNELS_CODEC = MapCodec.unit(() -> new OrganicDesertFeature(1));
   public static final MapCodec<OrganicDesertFeature> FINISH_CODEC = MapCodec.unit(() -> new OrganicDesertFeature(2));
   private final int phase;

   public MapCodec<OrganicDesertFeature> codec() {
      return this.phase == 0 ? SURFACE_CODEC : (this.phase == 1 ? CHANNELS_CODEC : FINISH_CODEC);
   }

   public OrganicDesertFeature(int phase) {
      this.phase = phase;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      int bottom = Math.max(5, level.getMinY());
      int top = Math.min(240, level.getMaxY() - 1);
      boolean changed = false;
      List<BlockPos> floors = new ArrayList<>();
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            boolean organicColumn = TheSiftDimension.isOrganicDesert(SiftCaveHabitat.surfaceBiome(level, x, z));
            int rockBottom = SiftCaveHabitat.surfaceRockBottom(level, x, z);

            for (int y = top; y >= bottom; y--) {
               pos.set(x, y, z);
               BlockState state = level.getBlockState(pos);
               if (!state.isAir()
                  && (this.phase == 0 || soilOrSoulshale(state))
                  && (organicColumn && y >= rockBottom || TheSiftDimension.isOrganicDesert(level.getBiome(pos)))
                  && level.ensureCanWrite(pos)
                  && !SiftFeaturePlacementGuard.intersectsPortal(pos, 2)
                  && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)) {
                  if (this.phase == 0) {
                     Block host = CoralSurfaceFeature.hostRock(state);
                     if (host != null && !state.is(host)) {
                        level.setBlock(pos, host.defaultBlockState(), 2);
                        state = host.defaultBlockState();
                        changed = true;
                     }
                  }

                  if (soilOrSoulshale(state)) {
                     BlockPos above = pos.above();
                     BlockState occupant = level.getBlockState(above);
                     if (this.phase != 1
                        && (state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE) || state.is(ModBlocks.HEALTHY_SCULK_PORES))
                        && soilOrSoulshale(occupant)) {
                        level.setBlock(pos, ModBlocks.SOULSHALE.defaultBlockState(), 2);
                        changed = true;
                     } else if (occupant.getFluidState().isEmpty() && plantSpace(occupant)) {
                        if (this.phase == 1) {
                           int depth = OrganicDesertShape.waterDepth(level.getSeed(), x, z);
                           if (depth != 0 && containedBank(level, pos, depth) && connectedWaterCandidate(level, pos)) {
                              carveWater(level, pos, depth);
                              changed = true;
                           }
                        } else {
                           BlockState ground = ground(level.getSeed(), x, z);
                           if (!state.equals(ground)) {
                              level.setBlock(pos, ground, 2);
                              changed = true;
                           }

                           for (int d = 1; d <= 5; d++) {
                              BlockPos under = pos.below(d);
                              BlockState below = level.getBlockState(under);
                              if (!level.ensureCanWrite(under) || !soilOrSoulshale(below) || SiftTerrainFeatureMemory.isProtectedTerrain(x, under.getY(), z)) {
                                 break;
                              }

                              if (!below.is(ModBlocks.SOULSHALE)) {
                                 level.setBlock(under, ModBlocks.SOULSHALE.defaultBlockState(), 2);
                              }
                           }

                           if (this.phase == 2 && level.ensureCanWrite(above)) {
                              if (!occupant.isAir() && (!SiftTerrainFeatureMemory.isOrganicPlant(above) || !occupant.canSurvive(level, above))) {
                                 level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                                 changed = true;
                              }

                              floors.add(pos.immutable());
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (this.phase == 2) {
         changed |= OrganicDesertVegetation.place(level, generator, random, floors);
      }

      return changed;
   }

   private static boolean containedBank(WorldGenLevel level, BlockPos pos, int depth) {
      for (int d = 1; d <= depth + 1; d++) {
         if (!level.ensureCanWrite(pos.below(d)) || !soilOrSoulshale(level.getBlockState(pos.below(d)))) {
            return false;
         }
      }

      for (Direction direction : Plane.HORIZONTAL) {
         BlockPos side = pos.relative(direction);
         BlockState bank = level.getBlockState(side);
         if (!soilOrSoulshale(bank) && !(bank.getFluidState().getType() instanceof IchorFluid)) {
            return false;
         }
      }

      return true;
   }

   public static BlockState ground(long seed, int x, int z) {
      return switch (OrganicDesertShape.ground(seed, x, z)) {
         case 1 -> ModBlocks.HEALTHY_SCULK_PORES.defaultBlockState();
         case 2 -> ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState();
         default -> ModBlocks.HEALTHY_SCULK_CARTILAGE.defaultBlockState();
      };
   }

   private static boolean connectedWaterCandidate(WorldGenLevel level, BlockPos pos) {
      for (Direction direction : Plane.HORIZONTAL) {
         BlockPos side = pos.relative(direction);
         if (level.getBlockState(side).getFluidState().getType() instanceof IchorFluid) {
            return true;
         }

         if (waterCandidate(level, side)) {
            carveWater(level, side, OrganicDesertShape.waterDepth(level.getSeed(), side.getX(), side.getZ()));
            return true;
         }
      }

      return false;
   }

   private static void carveWater(WorldGenLevel level, BlockPos pos, int depth) {
      level.setBlock(pos.below(depth), ModBlocks.SOULSHALE.defaultBlockState(), 2);
      if (!level.getBlockState(pos.above()).isAir()) {
         level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
      }

      for (int d = 0; d < depth; d++) {
         level.setBlock(pos.below(d), ModFluids.ICHOR_BLOCK.defaultBlockState(), 2);
      }
   }

   private static boolean waterCandidate(WorldGenLevel level, BlockPos pos) {
      int depth = OrganicDesertShape.waterDepth(level.getSeed(), pos.getX(), pos.getZ());
      if (depth != 0
         && soilOrSoulshale(level.getBlockState(pos))
         && plantSpace(level.getBlockState(pos.above()))
         && level.ensureCanWrite(pos)
         && !SiftFeaturePlacementGuard.intersectsPortal(pos, 2)
         && !SiftTerrainFeatureMemory.isProtectedTerrain(pos.getX(), pos.getY(), pos.getZ())) {
         return TheSiftDimension.isOrganicDesert(level.getBiome(pos))
               || TheSiftDimension.isOrganicDesert(SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ()))
                  && pos.getY() >= SiftCaveHabitat.surfaceRockBottom(level, pos.getX(), pos.getZ())
            ? containedBank(level, pos, depth)
            : false;
      } else {
         return false;
      }
   }

   private static boolean plantSpace(BlockState state) {
      return state.isAir()
         || state.is(ModBlocks.GRIM_BUSH)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT);
   }

   private static boolean soilOrSoulshale(BlockState state) {
      return state.is(ModBlocks.SOULSHALE)
         || state.is(ModBlocks.SOULSHALE_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || state.is(ModBlocks.HEALTHY_SCULK_PORES)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }
}
