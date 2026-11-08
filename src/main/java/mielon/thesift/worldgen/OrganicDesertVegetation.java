package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class OrganicDesertVegetation {
   private OrganicDesertVegetation() {
   }

   public static boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, List<BlockPos> floors) {
      List<BlockPos> cartilage = new ArrayList<>();
      List<BlockPos> dry = new ArrayList<>();
      List<BlockPos> edge = new ArrayList<>();
      List<BlockPos> other = new ArrayList<>();

      for (BlockPos pos : floors) {
         BlockState state = level.getBlockState(pos);
         if (state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)) {
            cartilage.add(pos);
         }

         if (state.is(ModBlocks.DRY_HEALTHY_SCULK)) {
            dry.add(pos);
         }

         if (OrganicDesertShape.poreEdge(level.getSeed(), pos.getX(), pos.getZ())) {
            edge.add(pos);
         } else {
            other.add(pos);
         }
      }

      boolean changed = false;
      int i = 0;

      for (int count = 5 + random.nextInt(3); i < count && !cartilage.isEmpty(); i++) {
         changed |= patch(
            level, generator, random, choose(cartilage, random), ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT, 64, 5, ModBlocks.HEALTHY_SCULK_CARTILAGE
         );
      }

      i = 0;

      for (int count = 1 + random.nextInt(2); i < count && !dry.isEmpty(); i++) {
         changed |= patch(level, generator, random, choose(dry, random), ModBlocks.DRY_HEALTHY_SCULK_SPROUTS, 48, 4, ModBlocks.DRY_HEALTHY_SCULK);
      }

      i = 0;

      for (int count = 1 + random.nextInt(3); i < count && !edge.isEmpty(); i++) {
         Block plant = random.nextBoolean() ? ModBlocks.HEALTHY_SCULK_SPROUTS : ModBlocks.GRIM_BUSH;
         changed |= patch(
            level,
            generator,
            random,
            choose(edge, random),
            plant,
            36,
            5,
            ModBlocks.HEALTHY_SCULK_CARTILAGE,
            ModBlocks.HEALTHY_SCULK_PORES,
            ModBlocks.DRY_HEALTHY_SCULK
         );
      }

      if (!other.isEmpty() && random.nextInt(4) == 0) {
         changed |= patch(
            level, generator, random, choose(other, random), ModBlocks.GRIM_BUSH, 28, 5, ModBlocks.HEALTHY_SCULK_CARTILAGE, ModBlocks.DRY_HEALTHY_SCULK
         );
      }

      return changed;
   }

   private static BlockPos choose(List<BlockPos> positions, RandomSource random) {
      return positions.get(random.nextInt(positions.size()));
   }

   private static boolean patch(
      WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos floor, Block plant, int tries, int spread, Block... soils
   ) {
      BlockPos origin = floor.above();
      boolean placed = SiftPlantPatches.place(level, generator, random, origin, plant, tries, spread, soils);
      if (!placed) {
         return false;
      }

      for (int dx = -spread; dx <= spread; dx++) {
         for (int dz = -spread; dz <= spread; dz++) {
            for (int dy = -2; dy <= 2; dy++) {
               BlockPos pos = origin.offset(dx, dy, dz);
               if (level.getBlockState(pos).is(plant)) {
                  if (SiftTerrainFeatureMemory.isProtectedTerrain(pos.getX(), pos.getY() - 1, pos.getZ())) {
                     level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                  } else {
                     SiftTerrainFeatureMemory.organicPlant(pos);
                  }
               }
            }
         }
      }

      return true;
   }
}
