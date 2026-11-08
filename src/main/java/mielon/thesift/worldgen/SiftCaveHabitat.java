package mielon.thesift.worldgen;

import java.util.Arrays;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

final class SiftCaveHabitat {
   private SiftCaveHabitat() {
   }

   static Holder<Biome> biome(WorldGenLevel level, BlockPos pos) {
      return level instanceof WorldGenRegion ? level.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2) : level.getBiome(pos);
   }

   static int rockTop(WorldGenLevel level, int x, int z) {
      int top = Math.min(level.getMaxY() - 1, level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1);
      MutableBlockPos pos = new MutableBlockPos(x, top, z);

      for (int y = top; y >= Math.max(level.getMinY(), top - 48); y--) {
         pos.setY(y);
         if (SiftLandmarkTerrain.isGround(level.getBlockState(pos))) {
            return y;
         }
      }

      return top;
   }

   static int surfaceRockBottom(WorldGenLevel level, int x, int z) {
      int top = rockTop(level, x, z);
      if (!TheSiftDimension.isCaveBiome(biome(level, new BlockPos(x, top, z)))) {
         return top - 5;
      }

      int low = level.getMinY() + 5;
      int high = level.getMaxY() - 1;

      while (low < high) {
         int middle = low + high + 1 >>> 1;
         if (TheSiftDimension.isCaveBiome(biome(level, new BlockPos(x, middle, z)))) {
            low = middle;
         } else {
            high = middle - 1;
         }
      }

      return top < low - 12 ? low + 1 : top - 5;
   }

   static boolean underground(WorldGenLevel level, BlockPos pos) {
      return pos.getY() < surfaceRockBottom(level, pos.getX(), pos.getZ());
   }

   static Holder<Biome> surfaceBiome(WorldGenLevel level, int x, int z) {
      return biome(level, new BlockPos(x, level.getMaxY() - 1, z));
   }

   static boolean lush(WorldGenLevel level, BlockPos pos) {
      return biome(level, pos).is(TheSiftDimension.OVERGROWN_CAVES) && underground(level, pos);
   }

   static boolean grim(WorldGenLevel level, BlockPos pos) {
      return biome(level, pos).is(TheSiftDimension.SOULSHALE_CAVES) && underground(level, pos);
   }

   static boolean spaghettiFloor(WorldGenLevel level, BlockPos floor) {
      if (floor.getY() <= 40 && open(level.getBlockState(floor.above())) && open(level.getBlockState(floor.above(2)))) {
         for (int dy = 3; dy <= 18; dy++) {
            if (SiftLandmarkTerrain.isGround(level.getBlockState(floor.above(dy)))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean open(BlockState state) {
      return !SiftLandmarkTerrain.isGround(state) && state.getFluidState().isEmpty() && SiftLandmarkTerrain.canReplace(state);
   }

   static boolean plantFloor(WorldGenLevel level, BlockPos floor) {
      if (broadFloor(level, floor)) {
         return true;
      }

      if (!spaghettiFloor(level, floor) && !SiftTerrainFeatureMemory.isProtectedTerrain(floor.getX(), floor.getY(), floor.getZ())) {
         return false;
      }

      if (!SiftLandmarkTerrain.isGround(level.getBlockState(floor.below(2)))) {
         return false;
      }

      int neighbours = 0;

      for (Direction face : Plane.HORIZONTAL) {
         BlockPos other = floor.relative(face);

         for (int dy = -1; dy <= 1; dy++) {
            BlockPos pos = other.above(dy);
            if (SiftLandmarkTerrain.isGround(level.getBlockState(pos))
               && !SiftLandmarkTerrain.isGround(level.getBlockState(pos.above()))
               && level.getBlockState(pos.above()).getFluidState().isEmpty()) {
               neighbours++;
               break;
            }
         }
      }

      return neighbours >= 2;
   }

   static boolean surfaceTreeFloor(WorldGenLevel level, BlockPos floor) {
      if (TheSiftDimension.isCaveBiome(biome(level, floor))) {
         return false;
      }

      int[] neighbours = new int[8];
      int index = 0;

      for (int[] offset : new int[][]{{-6, 0}, {6, 0}, {0, -6}, {0, 6}, {-4, -4}, {-4, 4}, {4, -4}, {4, 4}}) {
         neighbours[index++] = rockTop(level, floor.getX() + offset[0], floor.getZ() + offset[1]);
      }

      Arrays.sort(neighbours);
      return floor.getY() >= neighbours[4] - 12;
   }

   static boolean broadFloor(WorldGenLevel level, BlockPos floor) {
      if (!SiftLandmarkTerrain.isGround(level.getBlockState(floor.below(2)))) {
         return false;
      }

      int supported = 0;

      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) <= 2) {
               for (int dy = 1; dy >= -1; dy--) {
                  BlockPos neighbour = floor.offset(dx, dy, dz);
                  if (SiftLandmarkTerrain.isGround(level.getBlockState(neighbour))
                     && SiftLandmarkTerrain.isGround(level.getBlockState(neighbour.below()))
                     && !SiftLandmarkTerrain.isGround(level.getBlockState(neighbour.above()))
                     && level.getBlockState(neighbour.above()).getFluidState().isEmpty()) {
                     supported++;
                     break;
                  }
               }
            }
         }
      }

      return supported >= 10;
   }
}
