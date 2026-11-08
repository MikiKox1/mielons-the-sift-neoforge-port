package mielon.thesift.worldgen;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class OrganicRibFossilFeature extends Feature<NoneFeatureConfiguration> {
   public OrganicRibFossilFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos origin = context.origin();
      int chunkX = origin.getX() & -16;
      int chunkZ = origin.getZ() & -16;

      for (int attempt = 0; attempt < 8; attempt++) {
         int x = attempt == 0 ? origin.getX() : chunkX + 2 + random.nextInt(12);
         int z = attempt == 0 ? origin.getZ() : chunkZ + 2 + random.nextInt(12);
         if (TheSiftDimension.isOrganicDesert(SiftCaveHabitat.surfaceBiome(level, x, z))) {
            int y = surfaceY(level, x, z);
            if (y != Integer.MIN_VALUE) {
               BlockPos center = new BlockPos(x, y, z);
               int firstVariant = random.nextInt(12);
               int firstRotation = random.nextInt(4);
               int compactFamily = random.nextInt(3);

               for (int fit = 0; fit < 4; fit++) {
                  int variant = fit == 0 ? firstVariant : (compactFamily + fit - 1) % 3 * 4;
                  if (placeVariant(level, center, variant, firstRotation + fit & 3)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public static boolean placeVariant(WorldGenLevel level, BlockPos center, int variant, int rotation) {
      Map<BlockPos, Axis> shape = shape(variant, rotation);
      Map<BlockPos, Integer> feet = new LinkedHashMap<>();
      int lowest = Integer.MAX_VALUE;
      int highest = Integer.MIN_VALUE;

      for (BlockPos relative : shape.keySet()) {
         if (relative.getY() == -2) {
            int floor = surfaceY(level, center.getX() + relative.getX(), center.getZ() + relative.getZ());
            if (floor == Integer.MIN_VALUE) {
               return false;
            }

            feet.put(relative, floor);
            lowest = Math.min(lowest, floor);
            highest = Math.max(highest, floor);
         }
      }

      if (!feet.isEmpty() && highest - lowest <= 4) {
         int fittedY = (lowest + highest + 1) / 2;
         if (Math.abs(fittedY - center.getY()) > 3) {
            return false;
         }

         center = new BlockPos(center.getX(), fittedY, center.getZ());

         for (Entry<BlockPos, Integer> foot : feet.entrySet()) {
            int x = center.getX() + foot.getKey().getX();
            int z = center.getZ() + foot.getKey().getZ();

            for (int buried = 1; buried <= 2; buried++) {
               if (!SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(x, foot.getValue() - buried, z)))) {
                  return false;
               }
            }

            for (int y = foot.getValue() - center.getY() - 2; y < -2; y++) {
               shape.put(new BlockPos(foot.getKey().getX(), y, foot.getKey().getZ()), Axis.Y);
            }
         }

         int minX = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;
         Set<BlockPos> checkedColumns = new HashSet<>();
         Map<BlockPos, int[]> occupiedColumns = new LinkedHashMap<>();

         for (Entry<BlockPos, Axis> entry : shape.entrySet()) {
            BlockPos pos = center.offset(entry.getKey());
            BlockPos column = new BlockPos(pos.getX(), 0, pos.getZ());
            if (level.ensureCanWrite(pos) && !SiftTerrainFeatureMemory.isProtectedTerrain(pos.getX(), pos.getY(), pos.getZ())) {
               if (checkedColumns.add(column) && !TheSiftDimension.isOrganicDesert(SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ()))) {
                  return false;
               }

               BlockState state = level.getBlockState(pos);
               if (state.getFluidState().isEmpty()
                  && !state.is(Blocks.BONE_BLOCK)
                  && (state.isAir() || SiftLandmarkTerrain.isGround(state) || organicPlant(state))) {
                  if (entry.getKey().getY() >= 3 && SiftLandmarkTerrain.isGround(state)) {
                     return false;
                  }

                  minX = Math.min(minX, pos.getX());
                  maxX = Math.max(maxX, pos.getX());
                  minZ = Math.min(minZ, pos.getZ());
                  maxZ = Math.max(maxZ, pos.getZ());
                  int[] span = occupiedColumns.computeIfAbsent(column, key -> new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
                  span[0] = Math.min(span[0], pos.getY());
                  span[1] = Math.max(span[1], pos.getY());
                  continue;
               }

               return false;
            }

            return false;
         }

         if (!SiftFeaturePlacementGuard.tryReserveSolid(minX - 2, minZ - 2, maxX + 2, maxZ + 2)) {
            return false;
         }

         for (Entry<BlockPos, Axis> entry : shape.entrySet()) {
            level.setBlock(center.offset(entry.getKey()), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, entry.getValue()), 2);
         }

         for (Entry<BlockPos, int[]> column : occupiedColumns.entrySet()) {
            SiftTerrainFeatureMemory.protectTerrain(column.getKey().getX(), column.getKey().getZ(), column.getValue()[0], column.getValue()[1]);
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean organicPlant(BlockState state) {
      return state.is(ModBlocks.GRIM_BUSH)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT);
   }

   private static int surfaceY(WorldGenLevel level, int x, int z) {
      int top = level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
      MutableBlockPos cursor = new MutableBlockPos(x, top, z);

      for (int y = top; y >= Math.max(level.getMinY() + 2, top - 16); y--) {
         cursor.setY(y);
         BlockState state = level.getBlockState(cursor);
         if (!state.getFluidState().isEmpty() || state.is(Blocks.BONE_BLOCK)) {
            return Integer.MIN_VALUE;
         }

         if (SiftLandmarkTerrain.isGround(state)) {
            return y;
         }

         if (!state.isAir() && !organicPlant(state)) {
            return Integer.MIN_VALUE;
         }
      }

      return Integer.MIN_VALUE;
   }

   public static Map<BlockPos, Axis> shape(int variant, int rotation) {
      Map<BlockPos, Axis> result = new LinkedHashMap<>();
      int pairs = 3 + variant % 4;
      int width = 3 + variant / 4;
      int height = 5 + variant % 4;
      int length = (pairs - 1) * 3;

      for (int z = -length / 2; z <= length - length / 2; z++) {
         put(result, 0, 0, z, Axis.Z, rotation);
      }

      for (int rib = 0; rib < pairs; rib++) {
         int z = rib * 3 - length / 2;
         double taper = rib != 0 && rib != pairs - 1 ? 1.0 : 0.8;
         int ribWidth = Math.max(2, (int)Math.round(width * taper));
         int ribHeight = Math.max(4, (int)Math.round(height * taper));

         for (int side : new int[]{-1, 1}) {
            for (int y = -2; y <= 0; y++) {
               put(result, side * ribWidth, y, z, Axis.Y, rotation);
            }

            int previousX = side * ribWidth;
            int previousY = 0;
            int steps = ribHeight * 4;

            for (int step = 1; step <= steps && (variant < 8 || (rib + side + variant) % 3 != 0 || !(step > steps * 0.72)); step++) {
               double angle = 1.28 * step / steps;
               int nx = (int)Math.round(side * ribWidth * Math.cos(angle));
               int ny = (int)Math.round(ribHeight * Math.sin(angle));
               if (variant % 3 == 1 && ny > ribHeight / 2) {
                  nx++;
               }

               while (previousY < ny) {
                  put(result, previousX, ++previousY, z, Axis.Y, rotation);
               }

               while (previousX != nx) {
                  previousX += Integer.signum(nx - previousX);
                  put(result, previousX, previousY, z, Axis.X, rotation);
               }
            }
         }
      }

      return result;
   }

   private static void put(Map<BlockPos, Axis> shape, int x, int y, int z, Axis axis, int rotation) {
      int rx = x;
      int rz = z;

      for (int i = 0; i < rotation; i++) {
         int before = rx;
         rx = -rz;
         rz = before;
      }

      if ((rotation & 1) != 0 && axis != Axis.Y) {
         axis = axis == Axis.X ? Axis.Z : Axis.X;
      }

      shape.put(new BlockPos(rx, y, rz), axis);
   }
}
