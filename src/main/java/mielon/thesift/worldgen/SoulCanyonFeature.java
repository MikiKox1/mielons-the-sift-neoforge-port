package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SoulCanyonFeature implements Feature {
   public static final MapCodec<SoulCanyonFeature> CODEC = MapCodec.unit(SoulCanyonFeature::new);

   public MapCodec<SoulCanyonFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (!SiftLandmarkPlacement.isSelectedChunk(level.getSeed(), origin, 10, 6003110614342389337L, 3)) {
         return false;
      }

      int centerX = (origin.getX() & -16) + 8;
      int centerZ = (origin.getZ() & -16) + 8;
      if (SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(centerX, 0, centerZ), 34)) {
         return false;
      }

      if (SiftCampProtection.near(level, centerX, centerZ, 34)) {
         return false;
      }

      SiftWorldgenBounds bounds = SiftWorldgenBounds.around(origin);
      int length = 45 + random.nextInt(3);
      int baseRadius = 4;
      int maximumDepth = 18 + random.nextInt(3);
      double phase = random.nextDouble() * Math.PI * 2.0;
      int firstOrientation = random.nextInt(4);
      SoulCanyonFeature.CanyonPlan plan = null;
      List<BlockPos> soulBlocks = null;

      for (int[] offset : new int[][]{{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
         int candidateX = centerX + offset[0];
         int candidateZ = centerZ + offset[1];

         for (int referenceY : SiftLandmarkTerrain.exposedSurfaces(level, candidateX, candidateZ)) {
            for (int attempt = 0; attempt < 24 && plan == null; attempt++) {
               int tier = attempt / 8;
               int candidateLength = tier == 0 ? length : (tier == 1 ? 37 : 31);
               int candidateDepth = tier == 0 ? maximumDepth : (tier == 1 ? 12 : 7);
               int orientation = firstOrientation + attempt & 3;
               double candidatePhase = phase + attempt / 4 * 1.941611038725466;
               SoulCanyonFeature.CanyonPlan candidate = buildPlan(
                  level, bounds, candidateX, candidateZ, candidateLength, baseRadius, candidateDepth, candidatePhase, orientation, referenceY
               );
               if (candidate != null && canCarve(level, candidate.columns().values())) {
                  List<BlockPos> candidateSoulBlocks = selectSoulVeins(candidate, candidatePhase, candidateDepth);
                  if (candidateSoulBlocks.size() >= 8) {
                     plan = candidate;
                     soulBlocks = candidateSoulBlocks;
                  }
               }
            }

            if (plan != null) {
               break;
            }
         }

         if (plan != null) {
            break;
         }
      }

      if (plan != null && soulBlocks != null) {
         int minX = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (SoulCanyonFeature.CanyonColumn column : plan.columns().values()) {
            minX = Math.min(minX, column.x());
            minZ = Math.min(minZ, column.z());
            maxX = Math.max(maxX, column.x());
            maxZ = Math.max(maxZ, column.z());
         }

         if (!SiftFeaturePlacementGuard.tryReserveCarver(minX - 2, minZ - 2, maxX + 2, maxZ + 2)) {
            return false;
         }

         for (SoulCanyonFeature.CanyonColumn column : plan.columns().values()) {
            SiftTerrainFeatureMemory.canyon(column.x(), column.z(), column.floorY(), column.surfaceY());
            MutableBlockPos cursor = new MutableBlockPos();

            for (int y = column.floorY() - 2; y <= column.floorY(); y++) {
               cursor.set(column.x(), y, column.z());
               BlockState state = level.getBlockState(cursor);
               if (!SiftLandmarkTerrain.isGround(state) || state.is(ModBlocks.SIFT_RUBBLE)) {
                  level.setBlock(cursor, ModBlocks.SIFTSLATE.defaultBlockState(), 2);
               }
            }

            for (int y = column.floorY() + 1; y <= Math.max(column.surfaceY() + 3, column.floorY() + 3); y++) {
               cursor.set(column.x(), y, column.z());
               level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
            }
         }

         for (BlockPos soul : soulBlocks) {
            level.setBlock(soul, ModBlocks.SOUL_BLOCK.defaultBlockState(), 2);
            SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK, soul);
         }

         SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_ENTRANCE, plan.entrance());
         SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_MIDPOINT, plan.midpoint());
         SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_CLUSTER, clusterCenter(soulBlocks));
         SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SOUL_CANYON_GOLEM_PENDING, plan.entrance());
         return true;
      } else {
         return false;
      }
   }

   private static SoulCanyonFeature.CanyonPlan buildPlan(
      WorldGenLevel level,
      SiftWorldgenBounds bounds,
      int centerX,
      int centerZ,
      int length,
      int baseRadius,
      int maximumDepth,
      double phase,
      int orientation,
      int referenceY
   ) {
      double directionX = (orientation & 1) == 0 ? 0.7071067811865476 : -0.7071067811865476;
      double directionZ = (orientation & 2) == 0 ? 0.7071067811865476 : -0.7071067811865476;
      double sideX = -directionZ;
      double sideZ = directionX;
      int halfLength = length / 2;
      Map<Long, SoulCanyonFeature.CanyonColumn> columns = new HashMap<>();
      Set<Long> routeColumns = new HashSet<>();
      BlockPos entrance = null;
      BlockPos midpoint = null;
      int previousCenterFloor = Integer.MIN_VALUE;
      int previousCenterSurface = Integer.MIN_VALUE;

      for (int step = 0; step < length; step++) {
         int offset = step - halfLength;
         double meander = Math.sin(phase + step * 0.22) * 1.65 + Math.sin(phase * 1.73 + step * 0.071) * 0.7;
         int radius = Math.max(3, baseRadius + (int)Math.round(Math.sin(phase * 0.63 + step * 0.39)));
         double exactCenterX = centerX + directionX * offset + sideX * meander;
         double exactCenterZ = centerZ + directionZ * offset + sideZ * meander;
         int centerXAtStep = (int)Math.round(exactCenterX);
         int centerZAtStep = (int)Math.round(exactCenterZ);
         int centerSurface = SiftLandmarkTerrain.surfaceNearY(level, centerXAtStep, centerZAtStep, referenceY, 8);
         if (centerSurface == Integer.MIN_VALUE) {
            return null;
         }

         if (previousCenterSurface != Integer.MIN_VALUE && Math.abs(centerSurface - previousCenterSurface) > 8) {
            return null;
         }

         previousCenterSurface = centerSurface;
         double progress = (double)step / Math.max(1, length - 1);
         int rampDepth = Math.min(maximumDepth, (int)Math.round(step * 0.6));
         double farTaper = smootherStep(clamp01((1.0 - progress) / 0.34));
         int centerFloorDepth = (int)Math.round(rampDepth * farTaper);
         int desiredCenterFloor = centerSurface - centerFloorDepth;
         int intendedCenterFloor = previousCenterFloor == Integer.MIN_VALUE
            ? desiredCenterFloor
            : Math.max(previousCenterFloor - 1, Math.min(previousCenterFloor + 1, desiredCenterFloor));
         if (centerSurface + 2 < intendedCenterFloor) {
            return null;
         }

         if (centerSurface - intendedCenterFloor > maximumDepth + 14) {
            return null;
         }

         previousCenterFloor = intendedCenterFloor;
         BlockPos routeFeet = new BlockPos(centerXAtStep, intendedCenterFloor + 1, centerZAtStep);
         if (step == 0) {
            entrance = routeFeet;
         }

         if (step == Math.round((length - 1) * 0.38F)) {
            midpoint = routeFeet;
         }

         int rasterRadius = radius + 2;
         int minX = (int)Math.floor(exactCenterX) - rasterRadius;
         int maxX = (int)Math.ceil(exactCenterX) + rasterRadius;
         int minZ = (int)Math.floor(exactCenterZ) - rasterRadius;
         int maxZ = (int)Math.ceil(exactCenterZ) + rasterRadius;

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               double relativeX = x - exactCenterX;
               double relativeZ = z - exactCenterZ;
               double alongDistance = Math.abs(relativeX * directionX + relativeZ * directionZ);
               double signedSide = relativeX * sideX + relativeZ * sideZ;
               double sideDistance = Math.abs(signedSide);
               if (!(alongDistance > 0.82) && !(sideDistance > radius + 0.35)) {
                  if (!bounds.contains(x, z)) {
                     return null;
                  }

                  double sideRatio = sideDistance / (radius + 0.35);
                  double arch = Math.sqrt(Math.max(0.0, 1.0 - sideRatio * sideRatio));
                  int surfaceY = SiftLandmarkTerrain.surfaceNearY(level, x, z, referenceY, 8);
                  if (surfaceY == Integer.MIN_VALUE) {
                     return null;
                  }

                  boolean routeCorridor = sideDistance <= 2.25;
                  if (routeCorridor && (surfaceY + 2 < intendedCenterFloor || Math.abs(surfaceY - centerSurface) > 9)) {
                     return null;
                  }

                  int centerDepth = surfaceY - intendedCenterFloor;
                  int depth = Math.max(1, (int)Math.round(centerDepth * arch));
                  if (!(sideDistance > radius - 0.35) || !(hashUnit(x, z, maximumDepth) < 0.18)) {
                     int floorY = routeCorridor ? intendedCenterFloor : surfaceY - depth;
                     if (floorY <= level.getMinY() + 3) {
                        return null;
                     }

                     SoulCanyonFeature.CanyonColumn column = new SoulCanyonFeature.CanyonColumn(
                        x, z, surfaceY, floorY, surfaceY - floorY, (double)step / Math.max(1, length - 1), step, (int)Math.round(sideDistance), signedSide
                     );
                     long key = BlockPos.asLong(x, 0, z);
                     if (routeCorridor) {
                        routeColumns.add(key);
                     }

                     SoulCanyonFeature.CanyonColumn previous = columns.get(key);
                     if (previous == null || column.floorY() < previous.floorY()) {
                        columns.put(key, column);
                     }
                  }
               }
            }
         }
      }

      if (!smoothRoute(columns, routeColumns)) {
         return null;
      }

      BlockPos verifiedEntrance = routeFeet(columns, entrance);
      BlockPos verifiedMidpoint = routeFeet(columns, midpoint);
      return verifiedEntrance != null && verifiedMidpoint != null
         ? new SoulCanyonFeature.CanyonPlan(columns, verifiedEntrance, verifiedMidpoint, length, baseRadius)
         : null;
   }

   private static boolean smoothRoute(Map<Long, SoulCanyonFeature.CanyonColumn> columns, Set<Long> route) {
      ArrayDeque<Long> queue = new ArrayDeque<>(route);

      while (!queue.isEmpty()) {
         SoulCanyonFeature.CanyonColumn current = columns.get(queue.removeFirst());

         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               if (dx != 0 || dz != 0) {
                  long key = BlockPos.asLong(current.x() + dx, 0, current.z() + dz);
                  if (route.contains(key)) {
                     SoulCanyonFeature.CanyonColumn next = columns.get(key);
                     int floor = current.floorY() - 1;
                     if (next.floorY() < floor) {
                        if (floor > next.surfaceY() + 2) {
                           return false;
                        }

                        columns.put(
                           key,
                           new SoulCanyonFeature.CanyonColumn(
                              next.x(),
                              next.z(),
                              next.surfaceY(),
                              floor,
                              next.surfaceY() - floor,
                              next.progress(),
                              next.alongStep(),
                              next.sideDistance(),
                              next.signedSide()
                           )
                        );
                        queue.addLast(key);
                     }
                  }
               }
            }
         }
      }

      return true;
   }

   private static BlockPos routeFeet(Map<Long, SoulCanyonFeature.CanyonColumn> columns, BlockPos marker) {
      if (marker == null) {
         return null;
      }

      SoulCanyonFeature.CanyonColumn column = columns.get(BlockPos.asLong(marker.getX(), 0, marker.getZ()));
      return column == null ? null : new BlockPos(marker.getX(), column.floorY() + 1, marker.getZ());
   }

   private static List<BlockPos> selectSoulVeins(SoulCanyonFeature.CanyonPlan plan, double phase, int maximumDepth) {
      List<BlockPos> result = new ArrayList<>();
      Set<Long> reachableFloor = reachableFloorFromRamp(plan);
      if (!reachableFloor.contains(BlockPos.asLong(plan.midpoint().getX(), 0, plan.midpoint().getZ()))) {
         return result;
      }

      for (SoulCanyonFeature.CanyonColumn column : plan.columns().values()) {
         if (!(column.depth() < maximumDepth * 0.5)
            && column.floorY() <= plan.entrance().getY() - 5
            && !(column.progress() < 0.53)
            && !(column.progress() > 0.86)
            && column.sideDistance() <= plan.baseRadius()
            && reachableFloor.contains(BlockPos.asLong(column.x(), 0, column.z()))
            && hasGolemSizedApproach(plan, column)) {
            double along = column.alongStep();
            double veinCenter = Math.sin(phase + along * 0.31) * 1.15 + Math.sin(phase * 0.71 + along * 0.13) * 0.48;
            double width = 0.82 + (Math.sin(phase * 1.37 + along * 0.43) + 1.0) * 0.24;
            double edgeRatio = Math.abs(column.signedSide() - veinCenter) / width;
            double grain = hashUnit(column.x(), column.z(), maximumDepth * 31);
            boolean windingVein = edgeRatio <= 1.0 && (edgeRatio < 0.48 || grain > 0.2 + edgeRatio * 0.42);
            double pocketOne = square((column.progress() - 0.63) / 0.055) + square((column.signedSide() + 1.65 + Math.sin(phase) * 0.55) / 1.55);
            double pocketTwo = square((column.progress() - 0.77) / 0.048) + square((column.signedSide() - 1.35 - Math.cos(phase) * 0.45) / 1.35);
            boolean pocket = Math.min(pocketOne, pocketTwo) <= 1.0 && grain > 0.16;
            if (windingVein || pocket) {
               result.add(new BlockPos(column.x(), column.floorY(), column.z()));
            }
         }
      }

      return result;
   }

   private static Set<Long> reachableFloorFromRamp(SoulCanyonFeature.CanyonPlan plan) {
      Set<Long> visited = new HashSet<>();
      Set<Long> reachable = new HashSet<>();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();

      for (int x = plan.entrance().getX() - 1; x <= plan.entrance().getX(); x++) {
         for (int z = plan.entrance().getZ() - 1; z <= plan.entrance().getZ(); z++) {
            int floor = walkablePadFloor(plan, x, z);
            if (floor != Integer.MIN_VALUE) {
               visited.add(BlockPos.asLong(x, 0, z));
               queue.add(new BlockPos(x, floor, z));
            }
         }
      }

      while (!queue.isEmpty()) {
         BlockPos current = queue.removeFirst();

         for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
               reachable.add(BlockPos.asLong(current.getX() + dx, 0, current.getZ() + dz));
            }
         }

         for (Direction direction : Plane.HORIZONTAL) {
            int x = current.getX() + direction.getStepX();
            int z = current.getZ() + direction.getStepZ();
            long key = BlockPos.asLong(x, 0, z);
            if (!visited.contains(key)) {
               int floor = walkablePadFloor(plan, x, z);
               if (floor != Integer.MIN_VALUE && Math.abs(floor - current.getY()) <= 1) {
                  visited.add(key);
                  queue.addLast(new BlockPos(x, floor, z));
               }
            }
         }
      }

      return reachable;
   }

   private static int walkablePadFloor(SoulCanyonFeature.CanyonPlan plan, int x, int z) {
      int lowest = Integer.MAX_VALUE;
      int highest = Integer.MIN_VALUE;

      for (int dx = 0; dx <= 1; dx++) {
         for (int dz = 0; dz <= 1; dz++) {
            SoulCanyonFeature.CanyonColumn column = plan.columns().get(BlockPos.asLong(x + dx, 0, z + dz));
            if (column == null) {
               return Integer.MIN_VALUE;
            }

            lowest = Math.min(lowest, column.floorY());
            highest = Math.max(highest, column.floorY());
         }
      }

      return highest - lowest <= 1 ? highest : Integer.MIN_VALUE;
   }

   private static boolean hasGolemSizedApproach(SoulCanyonFeature.CanyonPlan plan, SoulCanyonFeature.CanyonColumn target) {
      for (int startX : new int[]{target.x() - 1, target.x()}) {
         for (int startZ : new int[]{target.z() - 1, target.z()}) {
            boolean valid = true;
            boolean hasStep = false;

            for (int dx = 0; dx <= 1 && valid; dx++) {
               for (int dz = 0; dz <= 1; dz++) {
                  SoulCanyonFeature.CanyonColumn column = plan.columns().get(BlockPos.asLong(startX + dx, 0, startZ + dz));
                  if (column == null || column.floorY() > target.floorY() || column.floorY() < target.floorY() - 1) {
                     valid = false;
                     break;
                  }

                  if (column.x() != target.x() || column.z() != target.z()) {
                     hasStep = true;
                  }
               }
            }

            if (valid && hasStep) {
               return true;
            }
         }
      }

      return false;
   }

   private static BlockPos clusterCenter(List<BlockPos> positions) {
      long x = 0L;
      long y = 0L;
      long z = 0L;

      for (BlockPos pos : positions) {
         x += pos.getX();
         y += pos.getY();
         z += pos.getZ();
      }

      int size = positions.size();
      return new BlockPos((int)(x / size), (int)(y / size), (int)(z / size));
   }

   private static boolean canCarve(WorldGenLevel level, Iterable<SoulCanyonFeature.CanyonColumn> columns) {
      MutableBlockPos cursor = new MutableBlockPos();

      for (SoulCanyonFeature.CanyonColumn column : columns) {
         for (int supportY = column.floorY() - 4; supportY <= column.floorY(); supportY++) {
            if (!SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(column.x(), supportY, column.z())))) {
               return false;
            }
         }

         for (int y = column.floorY() - 2; y <= Math.max(column.surfaceY() + 3, column.floorY() + 3); y++) {
            cursor.set(column.x(), y, column.z());
            if (!SiftLandmarkTerrain.canReplace(level.getBlockState(cursor))
               || !level.ensureCanWrite(cursor)
               || SiftTerrainFeatureMemory.isSupport(column.x(), y, column.z())) {
               return false;
            }
         }
      }

      return true;
   }

   private static int findTerrainSurfaceBlockY(WorldGenLevel level, int x, int z) {
      return SiftLandmarkTerrain.surfaceBlockY(level, x, z);
   }

   private static double square(double value) {
      return value * value;
   }

   private static double clamp01(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }

   private static double smootherStep(double value) {
      return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
   }

   private static double hashUnit(int x, int z, int salt) {
      long value = x * -7046029254386353131L ^ z * -4417276706812531889L ^ salt * -7723592293110705685L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      return ((value ^ value >>> 31) >>> 11) * 1.110223E-16F;
   }

   private record CanyonColumn(int x, int z, int surfaceY, int floorY, int depth, double progress, int alongStep, int sideDistance, double signedSide) {
   }

   private record CanyonPlan(Map<Long, SoulCanyonFeature.CanyonColumn> columns, BlockPos entrance, BlockPos midpoint, int length, int baseRadius) {
   }
}
