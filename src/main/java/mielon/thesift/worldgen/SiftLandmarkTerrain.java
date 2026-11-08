package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SiftLandmarkTerrain {
   public static Block stoneForBiome(Holder<Biome> biome) {
      return !biome.is(TheSiftDimension.SOULSHALE_CAVES) && !TheSiftDimension.isCoralBiome(biome) && !TheSiftDimension.isOrganicDesert(biome)
         ? ModBlocks.SIFTSLATE
         : ModBlocks.SOULSHALE;
   }

   private SiftLandmarkTerrain() {
   }

   static boolean allowsGrowth(WorldGenLevel level, BlockPos pos) {
      Holder<Biome> biome = SiftCaveHabitat.biome(level, pos);
      if (biome.is(TheSiftDimension.OVERGROWN_CAVES) && !SiftCaveHabitat.underground(level, pos)) {
         biome = SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ());
      }

      return TheSiftDimension.isOvergrownBiome(biome) || biome.is(TheSiftDimension.ICHOR_SNOWY_PEAKS);
   }

   static BlockState growthSurface(WorldGenLevel level, BlockPos pos, BlockState state) {
      if (state.is(ModBlocks.SOULSHALE_GROWTH) && !SiftCaveHabitat.grim(level, pos)) {
         return (SiftCaveHabitat.underground(level, pos) ? ModBlocks.SOULSHALE : ModBlocks.SIFTSLATE).defaultBlockState();
      }

      if (state.is(ModBlocks.SOULSHALE)
         && !SiftCaveHabitat.underground(level, pos)
         && !TheSiftDimension.isGrimSurfaceBiome(SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ()))) {
         return ModBlocks.SIFTSLATE.defaultBlockState();
      }

      if (!allowsGrowth(level, pos)) {
         if (state.is(ModBlocks.SIFTSLATE_GROWTH)) {
            return ModBlocks.SIFTSLATE.defaultBlockState();
         }

         if (state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) {
            return ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState();
         }
      }

      return state;
   }

   static boolean isGround(BlockState state) {
      return state.is(ModBlocks.CORAL_SOIL)
         || state.is(ModBlocks.CORAL_GROWTH)
         || state.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || state.is(ModBlocks.SIFTSLATE)
         || state.is(ModBlocks.SOULSHALE)
         || state.is(ModBlocks.SOULSHALE_GROWTH)
         || state.is(ModBlocks.SOULSHALE_COAL_ORE)
         || state.is(ModBlocks.SOULSHALE_DIAMOND_ORE)
         || state.is(ModBlocks.SOULSHALE_EMERALD_ORE)
         || state.is(ModBlocks.SOULSHALE_CHAROITE_ORE)
         || state.is(ModBlocks.SOULSHALE_SIFTITE_ORE)
         || state.is(ModBlocks.COBBLED_SIFTSLATE)
         || state.is(ModBlocks.SIFT_RUBBLE)
         || state.is(ModBlocks.SIFTSLATE_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || state.is(ModBlocks.HEALTHY_SCULK_PORES)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || state.is(ModBlocks.SIFTSLATE_COAL_ORE)
         || state.is(ModBlocks.SIFTSLATE_DIAMOND_ORE)
         || state.is(ModBlocks.SIFTSLATE_EMERALD_ORE)
         || state.is(ModBlocks.SIFTSLATE_CHAROITE_ORE)
         || state.is(ModBlocks.SIFTSLATE_SIFTITE_ORE)
         || state.is(ModBlocks.ICHOR_SNOW_BLOCK)
         || state.is(Blocks.SCULK)
         || state.is(Blocks.BONE_BLOCK);
   }

   static boolean canReplace(BlockState state) {
      return state.isAir()
         || isGround(state)
         || state.getFluidState().isEmpty() && state.canBeReplaced()
         || state.is(ModBlocks.ICHOR_SNOW)
         || state.is(Blocks.SCULK_VEIN)
         || state.is(Blocks.SCULK_SENSOR)
         || state.is(Blocks.SCULK_SHRIEKER)
         || state.is(Blocks.SCULK_CATALYST)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.SIFTSLATE_HANGING_ROOTS)
         || state.is(ModBlocks.OVERGROWN_HANGING_ROOTS);
   }

   static int surfaceBlockY(WorldGenLevel level, int x, int z) {
      int top = level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
      int minimum = Math.max(level.getMinY() + 1, top - 32);
      MutableBlockPos cursor = new MutableBlockPos(x, top, z);

      for (int y = top; y >= minimum; y--) {
         cursor.setY(y);
         BlockState state = level.getBlockState(cursor);
         if (isGround(state)) {
            return y;
         }

         if (!canReplace(state)) {
            return Integer.MIN_VALUE;
         }
      }

      return Integer.MIN_VALUE;
   }

   static List<Integer> exposedSurfaces(WorldGenLevel level, int x, int z) {
      List<Integer> surfaces = new ArrayList<>();
      int top = Math.min(220, level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1);

      for (int y = top; y >= 40; y--) {
         if (exposedFloor(level, x, y, z)) {
            surfaces.add(y);
            y -= 5;
         }
      }

      return surfaces;
   }

   static int surfaceNearY(WorldGenLevel level, int x, int z, int referenceY, int range) {
      for (int distance = 0; distance <= range; distance++) {
         if (exposedFloor(level, x, referenceY + distance, z)) {
            return referenceY + distance;
         }

         if (distance > 0 && exposedFloor(level, x, referenceY - distance, z)) {
            return referenceY - distance;
         }
      }

      return Integer.MIN_VALUE;
   }

   private static boolean exposedFloor(WorldGenLevel level, int x, int y, int z) {
      if (y >= 40 && y <= 220) {
         BlockPos floor = new BlockPos(x, y, z);
         if (isGround(level.getBlockState(floor)) && isGround(level.getBlockState(floor.below()))) {
            for (int above = 1; above <= 5; above++) {
               BlockState state = level.getBlockState(floor.above(above));
               if (isGround(state) || !state.getFluidState().isEmpty() || !canReplace(state)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
