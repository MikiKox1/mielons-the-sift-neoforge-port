package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class SiftCaveEntranceFeature extends Feature<NoneFeatureConfiguration> {
   public SiftCaveEntranceFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos origin = context.origin();
      if (random.nextInt(4) != 0) {
         return false;
      }

      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;

      for (int attempt = 0; attempt < 20; attempt++) {
         int x = minX + 4 + random.nextInt(8);
         int z = minZ + 4 + random.nextInt(8);
         int caveFloor = caveFloor(level, x, z);
         if (caveFloor != Integer.MIN_VALUE) {
            int first = random.nextInt(4);

            for (int angle = 0; angle < 4; angle++) {
               List<SiftCaveEntranceFeature.Column> plan = plan(level, bounds, x, caveFloor, z, (first + angle & 3) * Math.PI / 2.0);
               if (plan != null) {
                  int left = plan.stream().mapToInt(SiftCaveEntranceFeature.Column::x).min().orElseThrow();
                  int right = plan.stream().mapToInt(SiftCaveEntranceFeature.Column::x).max().orElseThrow();
                  int near = plan.stream().mapToInt(SiftCaveEntranceFeature.Column::z).min().orElseThrow();
                  int far = plan.stream().mapToInt(SiftCaveEntranceFeature.Column::z).max().orElseThrow();
                  if (!SiftCampProtection.intersects(level, new BoundingBox(left, caveFloor - 8, near, right, caveFloor + 24, far))
                     && SiftFeaturePlacementGuard.tryReserveCarver(left, near, right, far)) {
                     for (SiftCaveEntranceFeature.Column column : plan) {
                        SiftTerrainFeatureMemory.excludeRibs(column.x(), column.z(), column.floor() - 3, column.roof() + 2);
                        SiftTerrainFeatureMemory.protectTerrain(column.x(), column.z(), column.floor() - 3, column.roof() + 2);

                        for (int y = foundation(level, column) + 1; y <= column.floor(); y++) {
                           BlockPos pos = new BlockPos(column.x(), y, column.z());
                           if (!SiftLandmarkTerrain.isGround(level.getBlockState(pos))) {
                              level.setBlock(pos, ModBlocks.SIFTSLATE.defaultBlockState(), 2);
                           }
                        }

                        for (int y = column.floor() + 1; y <= column.roof(); y++) {
                           level.setBlock(new BlockPos(column.x(), y, column.z()), Blocks.AIR.defaultBlockState(), 2);
                        }
                     }

                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static int caveFloor(WorldGenLevel level, int x, int z) {
      for (int y = 32; y >= 14; y--) {
         BlockPos floor = new BlockPos(x, y, z);
         if (SiftLandmarkTerrain.isGround(level.getBlockState(floor))
            && level.isEmptyBlock(floor.above())
            && level.isEmptyBlock(floor.above(2))
            && level.isEmptyBlock(floor.above(3))) {
            for (int ceiling = 4; ceiling <= 12; ceiling++) {
               if (SiftLandmarkTerrain.isGround(level.getBlockState(floor.above(ceiling)))) {
                  return y;
               }
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private static List<SiftCaveEntranceFeature.Column> plan(WorldGenLevel level, SiftWorldgenBounds bounds, int x, int caveY, int z, double angle) {
      double dx = Math.cos(angle);
      double dz = Math.sin(angle);
      int startX = x + (int)Math.round(dx * 16.0);
      int startZ = z + (int)Math.round(dz * 16.0);
      SiftStrataSupportShape.Terrain terrain = SiftStrataSupportFeature.terrainView(level, bounds);
      if (!bounds.contains(startX, startZ)) {
         return null;
      }

      int startY = SiftIchorRavineShape.findBasalBank(terrain, startX, startZ);
      if (startY >= 49 && startY <= 80 && startY - caveY >= 12) {
         List<BlockPos> route = new ArrayList<>();
         int previousX = startX;
         int previousZ = startZ;
         route.add(new BlockPos(previousX, 0, previousZ));

         for (int sample = 1; sample <= 64; sample++) {
            double t = sample / 64.0;
            double bend = Math.sin(t * Math.PI) * 18.0;
            int nextX = x + (int)Math.round(dx * 16.0 * (1.0 - t) - dz * bend);
            int nextZ = z + (int)Math.round(dz * 16.0 * (1.0 - t) + dx * bend);

            while (previousX != nextX) {
               previousX += Integer.signum(nextX - previousX);
               route.add(new BlockPos(previousX, 0, previousZ));
            }

            while (previousZ != nextZ) {
               previousZ += Integer.signum(nextZ - previousZ);
               route.add(new BlockPos(previousX, 0, previousZ));
            }
         }

         if (startY - caveY > route.size() - 1) {
            return null;
         }

         Map<Long, SiftCaveEntranceFeature.Column> columns = new LinkedHashMap<>();

         for (int index = 0; index < route.size(); index++) {
            double t = (double)index / (route.size() - 1);
            int floor = startY + (int)Math.round((caveY - startY) * t);
            BlockPos center = route.get(index);
            double radius = index < 6 ? 2.6 : 1.8;

            for (int ox = -3; ox <= 3; ox++) {
               for (int oz = -3; oz <= 3; oz++) {
                  if (!(ox * ox + oz * oz > radius * radius)) {
                     int px = center.getX() + ox;
                     int pz = center.getZ() + oz;
                     int roof = floor + (ox * ox + oz * oz < 2 ? 4 : 3);
                     long key = BlockPos.asLong(px, 0, pz);
                     SiftCaveEntranceFeature.Column existing = columns.get(key);
                     columns.put(
                        key,
                        existing == null
                           ? new SiftCaveEntranceFeature.Column(px, pz, floor, roof)
                           : new SiftCaveEntranceFeature.Column(px, pz, Math.min(floor, existing.floor()), Math.max(roof, existing.roof()))
                     );
                  }
               }
            }
         }

         for (SiftCaveEntranceFeature.Column column : columns.values()) {
            if (!bounds.contains(column.x(), column.z())
               || SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(column.x(), 0, column.z()), 2)
               || foundation(level, column) == Integer.MIN_VALUE) {
               return null;
            }

            for (int y = foundation(level, column) + 1; y <= column.roof(); y++) {
               BlockPos pos = new BlockPos(column.x(), y, column.z());
               if (!level.ensureCanWrite(pos)
                  || !SiftLandmarkTerrain.canReplace(level.getBlockState(pos))
                  || SiftTerrainFeatureMemory.isSupport(column.x(), y, column.z())) {
                  return null;
               }
            }
         }

         return new ArrayList<>(columns.values());
      } else {
         return null;
      }
   }

   private static int foundation(WorldGenLevel level, SiftCaveEntranceFeature.Column column) {
      for (int y = column.floor() - 1; y >= column.floor() - 6; y--) {
         if (SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(column.x(), y, column.z())))) {
            return y;
         }
      }

      return Integer.MIN_VALUE;
   }

   private record Column(int x, int z, int floor, int roof) {
   }
}
