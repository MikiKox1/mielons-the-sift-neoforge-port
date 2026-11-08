package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class IchorLakeFeature extends Feature<NoneFeatureConfiguration> {
   private static final int MAX_SAFE_EXTENT = 24;

   public IchorLakeFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos placementOrigin = context.origin();

      for (int attempt = 0; attempt < 3; attempt++) {
         if (placeLake(level, random, placementOrigin)) {
            return true;
         }
      }

      return false;
   }

   private static boolean placeLake(WorldGenLevel level, RandomSource random, BlockPos placementOrigin) {
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(placementOrigin);
      int centerX = (placementOrigin.getX() & -16) + 5 + random.nextInt(6);
      int centerZ = (placementOrigin.getZ() & -16) + 5 + random.nextInt(6);
      int centerSurface = SiftMonolithFeature.findTerrainSurface(level, centerX, centerZ);
      if (centerSurface < 0) {
         return false;
      }

      if (SiftCampProtection.near(level, centerX, centerZ, 32)) {
         return false;
      }

      int sampledRelief = sampleRelief(level, bounds, centerX, centerZ, 16);
      if (sampledRelief < 0) {
         return false;
      }

      IchorLakeFeature.CascadeSite cascade = sampledRelief >= 9 ? findCascadeSite(level, bounds, centerX, centerZ) : null;
      if (cascade != null && random.nextFloat() < 0.82F) {
         IchorLakeFeature.LakePalette palette = choosePalette(level, cascade.lowerCenter(), random);
         return placeCascade(level, random, bounds, cascade, palette) > 0;
      }

      IchorLakeFeature.LakeVariant variant = chooseVariant(random, sampledRelief);
      if (hasNearbyIchor(level, bounds, centerX, centerZ, variant == IchorLakeFeature.LakeVariant.MEGA ? 20 : 15)) {
         return false;
      }

      boolean expandedMega = variant == IchorLakeFeature.LakeVariant.MEGA && random.nextFloat() < 0.68F;
      int radiusX = expandedMega ? 15 + random.nextInt(2) : variant.minimumRadius() + random.nextInt(variant.maximumRadius() - variant.minimumRadius() + 1);
      int radiusZ = expandedMega
         ? Mth.clamp(radiusX + random.nextInt(5) - 2, 14, 16)
         : Mth.clamp(radiusX + random.nextInt(7) - 3, variant.minimumRadius(), variant.maximumRadius());
      int beachWidth = expandedMega ? 5 : variant.minimumBeach() + random.nextInt(variant.maximumBeach() - variant.minimumBeach() + 1);
      int extent = Mth.ceil(Math.max(radiusX, radiusZ) * 1.14) + beachWidth;
      if (extent <= 24 && !SiftFeaturePlacementGuard.intersectsPortal(centerX - extent, centerZ - extent, centerX + extent, centerZ + extent)) {
         double angle = random.nextDouble() * Math.PI;
         double cos = Math.cos(angle);
         double sin = Math.sin(angle);
         long salt = random.nextLong();
         Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water = buildWaterMask(level, bounds, centerX, centerZ, radiusX, radiusZ, angle, cos, sin, salt);
         if (water.size() < variant.minimumArea()) {
            return false;
         }

         Map<IchorLakeFeature.Xz, Integer> beach = buildBeachMask(water, beachWidth);
         List<Integer> waterSurfaces = new ArrayList<>(water.size());
         int minimumSurface = Integer.MAX_VALUE;
         int maximumSurface = Integer.MIN_VALUE;

         for (IchorLakeFeature.LakeColumn column : water.values()) {
            waterSurfaces.add(column.surfaceY());
            minimumSurface = Math.min(minimumSurface, column.surfaceY());
            maximumSurface = Math.max(maximumSurface, column.surfaceY());
         }

         if (maximumSurface - minimumSurface > variant.maximumRelief()) {
            return false;
         }

         waterSurfaces.sort(Comparator.naturalOrder());
         int waterY = waterSurfaces.get((int)Math.floor((waterSurfaces.size() - 1) * 0.36)) - 1;
         if (waterY >= level.getMinY() + 6 && waterY <= level.getMaxY() - 7) {
            Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> beachColumns = new LinkedHashMap<>();

            for (Entry<IchorLakeFeature.Xz, Integer> entry : beach.entrySet()) {
               IchorLakeFeature.Xz cell = entry.getKey();
               if (!bounds.contains(cell.x(), cell.z())) {
                  return false;
               }

               int surfaceY = SiftMonolithFeature.findTerrainSurface(level, cell.x(), cell.z());
               if (surfaceY >= 0 && SiftMonolithFeature.isSiftTerrain(level.getBlockState(new BlockPos(cell.x(), surfaceY - 1, cell.z())))) {
                  int layer = entry.getValue();
                  int naturalTop = surfaceY - 1;
                  int targetTop = targetShoreTop(naturalTop, waterY, layer, beachWidth);
                  if (naturalTop >= waterY - beachWidth
                     && naturalTop <= waterY + variant.maximumRelief() + beachWidth
                     && Math.abs(targetTop - naturalTop) <= beachWidth
                     && hasNaturalSupport(level, cell.x(), naturalTop, cell.z(), 3)) {
                     beachColumns.put(cell, new IchorLakeFeature.LakeColumn(cell.x(), cell.z(), surfaceY, 1.0 + (double)layer / beachWidth));
                     continue;
                  }

                  return false;
               }

               return false;
            }

            Map<IchorLakeFeature.Xz, Integer> shoreTargets = buildShoreTargets(level, water, beach, beachColumns, waterY, beachWidth);
            if (shoreTargets.isEmpty()) {
               return false;
            }

            IchorLakeFeature.LakePalette palette = choosePalette(level, new BlockPos(centerX, waterY, centerZ), random);
            if (!SiftFeaturePlacementGuard.tryReserveCarver(centerX - extent, centerZ - extent, centerX + extent, centerZ + extent)) {
               return false;
            }

            int placed = 0;

            for (Entry<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> entry : beachColumns.entrySet()) {
               placed += shapeBeach(level, entry.getValue(), shoreTargets.get(entry.getKey()), palette, bounds);
            }

            for (IchorLakeFeature.LakeColumn column : water.values()) {
               placed += carveAndFill(level, column, waterY, palette.basin(), salt, variant.maximumDepth(), bounds);
            }

            if (variant == IchorLakeFeature.LakeVariant.MEGA || variant.rocks() && random.nextFloat() < 0.82F) {
               placed += placeRockFormations(level, random, water, waterY, palette, salt, bounds);
            }

            decorateLakeShore(level, random, water, shoreTargets, bounds);
            return placed >= variant.minimumArea();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static IchorLakeFeature.LakeVariant chooseVariant(RandomSource random, int relief) {
      if (relief <= 3 && random.nextInt(3) == 0) {
         return IchorLakeFeature.LakeVariant.MEGA;
      } else {
         int roll = random.nextInt(100);
         if (relief <= 5 && roll < 72) {
            return IchorLakeFeature.LakeVariant.LARGE;
         } else {
            return relief <= 7 && roll < 82 ? IchorLakeFeature.LakeVariant.MEDIUM : IchorLakeFeature.LakeVariant.SMALL;
         }
      }
   }

   private static int sampleRelief(WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ, int radius) {
      int minimum = Integer.MAX_VALUE;
      int maximum = Integer.MIN_VALUE;

      for (int dx = -radius; dx <= radius; dx += 4) {
         for (int dz = -radius; dz <= radius; dz += 4) {
            if (dx * dx + dz * dz <= radius * radius) {
               if (!bounds.contains(centerX + dx, centerZ + dz)) {
                  return -1;
               }

               int y = SiftMonolithFeature.findTerrainSurface(level, centerX + dx, centerZ + dz);
               if (y < 0) {
                  return -1;
               }

               minimum = Math.min(minimum, y);
               maximum = Math.max(maximum, y);
            }
         }
      }

      return maximum - minimum;
   }

   private static Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> buildWaterMask(
      WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ, int radiusX, int radiusZ, double angle, double cos, double sin, long salt
   ) {
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water = new LinkedHashMap<>();
      int limit = Mth.ceil(Math.max(radiusX, radiusZ) * 1.14);

      for (int dx = -limit; dx <= limit; dx++) {
         for (int dz = -limit; dz <= limit; dz++) {
            int x = centerX + dx;
            int z = centerZ + dz;
            if (bounds.contains(x, z)) {
               double localX = (dx * cos - dz * sin) / radiusX;
               double localZ = (dx * sin + dz * cos) / radiusZ;
               double warpX = localX + Math.sin(localZ * 2.7 + angle * 1.9) * 0.045;
               double warpZ = localZ + Math.sin(localX * 2.2 - angle * 1.3) * 0.04;
               double distance = Math.sqrt(warpX * warpX + warpZ * warpZ);
               double polar = Math.atan2(localZ, localX);
               double shoreline = 0.95
                  + Math.sin(polar * 3.0 + angle * 1.7) * 0.09
                  + Math.sin(polar * 5.0 - angle * 0.8) * 0.055
                  + Math.cos(polar * 7.0 + angle * 0.37) * 0.028
                  + (hash01(x >> 2, 0, z >> 2, salt) - 0.5) * 0.028;
               if (!(distance > shoreline)) {
                  int surfaceY = SiftMonolithFeature.findTerrainSurface(level, x, z);
                  if (surfaceY < 0 || !SiftMonolithFeature.isSiftTerrain(level.getBlockState(new BlockPos(x, surfaceY - 1, z)))) {
                     return Map.of();
                  }

                  water.put(new IchorLakeFeature.Xz(x, z), new IchorLakeFeature.LakeColumn(x, z, surfaceY, distance / Math.max(0.01, shoreline)));
               }
            }
         }
      }

      return water;
   }

   private static Map<IchorLakeFeature.Xz, Integer> buildBeachMask(Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water, int width) {
      Map<IchorLakeFeature.Xz, Integer> beach = new HashMap<>();

      for (IchorLakeFeature.Xz source : water.keySet()) {
         for (int dx = -width; dx <= width; dx++) {
            for (int dz = -width; dz <= width; dz++) {
               int layer = (int)Math.ceil(Math.sqrt(dx * dx + dz * dz));
               if (layer != 0 && layer <= width) {
                  IchorLakeFeature.Xz candidate = new IchorLakeFeature.Xz(source.x() + dx, source.z() + dz);
                  if (!water.containsKey(candidate)) {
                     beach.merge(candidate, layer, Math::min);
                  }
               }
            }
         }
      }

      return beach;
   }

   private static IchorLakeFeature.LakePalette choosePalette(WorldGenLevel level, BlockPos center, RandomSource random) {
      if (!TheSiftDimension.isCoralBiome(SiftCaveHabitat.biome(level, center))
         && (
            !TheSiftDimension.isCoralBiome(SiftCaveHabitat.surfaceBiome(level, center.getX(), center.getZ()))
               || center.getY() < SiftCaveHabitat.rockTop(level, center.getX(), center.getZ()) - 12
         )) {
         boolean overgrown = TheSiftDimension.isOvergrownBiome(SiftCaveHabitat.biome(level, center));
         int basinRoll = random.nextInt(100);
         BlockState basin = basinRoll < 70
            ? ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState()
            : (basinRoll < 91 ? ModBlocks.HEALTHY_SCULK.defaultBlockState() : ModBlocks.SIFTSLATE.defaultBlockState());
         int shoreRoll = random.nextInt(100);
         BlockState shore;
         if (overgrown && shoreRoll < 58) {
            shore = ModBlocks.SIFTSLATE_GROWTH.defaultBlockState();
         } else if (shoreRoll < (overgrown ? 79 : 72)) {
            shore = ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState();
         } else if (shoreRoll < 93) {
            shore = ModBlocks.HEALTHY_SCULK.defaultBlockState();
         } else {
            shore = ModBlocks.SIFTSLATE.defaultBlockState();
         }

         return new IchorLakeFeature.LakePalette(basin, shore);
      } else {
         return new IchorLakeFeature.LakePalette(ModBlocks.SOULSHALE.defaultBlockState(), ModBlocks.BRIGHT_CORAL_GROWTH.defaultBlockState());
      }
   }

   private static int shapeBeach(
      WorldGenLevel level, IchorLakeFeature.LakeColumn column, int targetTop, IchorLakeFeature.LakePalette palette, SiftWorldgenBounds bounds
   ) {
      int naturalTop = column.surfaceY() - 1;
      MutableBlockPos cursor = new MutableBlockPos();
      int placed = 0;

      for (int y = targetTop + 1; y <= Math.max(column.surfaceY() + 3, targetTop + 3); y++) {
         cursor.set(column.x(), y, column.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            BlockState state = level.getBlockState(cursor);
            if (SiftMonolithFeature.canReplace(state) || SiftMonolithFeature.isSiftTerrain(state) || state.is(ModFluids.ICHOR_BLOCK) || isIchorSnow(state)) {
               level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
            }
         }
      }

      int supportBottom = Math.min(naturalTop, targetTop) - 2;

      for (int y = supportBottom; y < targetTop; y++) {
         cursor.set(column.x(), y, column.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            level.setBlock(cursor, y >= targetTop - 2 ? palette.basin() : foundation(palette), 2);
            placed++;
         }
      }

      cursor.set(column.x(), targetTop - 1, column.z());
      BlockState below = level.getBlockState(cursor);
      BlockState top = shorelineState(below, palette);
      cursor.set(column.x(), targetTop, column.z());
      if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
         level.setBlock(cursor, SiftLandmarkTerrain.growthSurface(level, cursor, top), 2);
         placed++;
      }

      return placed;
   }

   private static int targetShoreTop(int naturalTop, int waterY, int layer, int width) {
      if (layer > 1 && width > 1) {
         double outward = Mth.clamp((double)(layer - 1) / (width - 1), 0.0, 1.0);
         double smooth = outward * outward * (3.0 - 2.0 * outward);
         return Mth.floor(Mth.lerp(smooth, waterY, naturalTop) + 0.5);
      } else {
         return waterY;
      }
   }

   private static Map<IchorLakeFeature.Xz, Integer> buildShoreTargets(
      WorldGenLevel level,
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water,
      Map<IchorLakeFeature.Xz, Integer> beach,
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> beachColumns,
      int waterY,
      int width
   ) {
      Map<IchorLakeFeature.Xz, Integer> targets = new LinkedHashMap<>();

      for (Entry<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> entry : beachColumns.entrySet()) {
         int layer = beach.get(entry.getKey());
         int naturalTop = entry.getValue().surfaceY() - 1;
         targets.put(entry.getKey(), layer == width ? naturalTop : targetShoreTop(naturalTop, waterY, layer, width));
      }

      int[] steps = new int[]{-1, 0, 1, 0, -1};

      for (int pass = 0; pass < width * 6; pass++) {
         Map<IchorLakeFeature.Xz, Integer> next = new LinkedHashMap<>(targets.size());

         for (Entry<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> entry : beachColumns.entrySet()) {
            IchorLakeFeature.Xz cell = entry.getKey();
            int layer = beach.get(cell);
            if (layer == 1) {
               next.put(cell, waterY);
            } else if (layer == width) {
               next.put(cell, entry.getValue().surfaceY() - 1);
            } else {
               int desired = targetShoreTop(entry.getValue().surfaceY() - 1, waterY, layer, width);
               int sum = desired * 3;
               int weight = 3;

               for (int direction = 0; direction < 4; direction++) {
                  IchorLakeFeature.Xz neighbour = new IchorLakeFeature.Xz(cell.x() + steps[direction], cell.z() + steps[direction + 1]);
                  if (water.containsKey(neighbour)) {
                     sum += waterY * 3;
                     weight += 3;
                  } else if (targets.containsKey(neighbour)) {
                     sum += targets.get(neighbour);
                     weight++;
                  } else if (layer == width) {
                     int surface = SiftMonolithFeature.findTerrainSurface(level, neighbour.x(), neighbour.z());
                     if (surface < 0) {
                        return Map.of();
                     }

                     sum += (surface - 1) * 2;
                     weight += 2;
                  }
               }

               int smoothed = Mth.floor((double)sum / weight + 0.5);
               next.put(cell, Mth.clamp(smoothed, waterY - layer, waterY + layer));
            }
         }

         targets = next;
      }

      for (Entry<IchorLakeFeature.Xz, Integer> entry : targets.entrySet()) {
         IchorLakeFeature.Xz cell = entry.getKey();
         int layer = beach.get(cell);

         for (int direction = 0; direction < 4; direction++) {
            IchorLakeFeature.Xz neighbour = new IchorLakeFeature.Xz(cell.x() + steps[direction], cell.z() + steps[direction + 1]);
            int neighbourTop;
            if (water.containsKey(neighbour)) {
               neighbourTop = waterY;
            } else if (targets.containsKey(neighbour)) {
               neighbourTop = targets.get(neighbour);
            } else {
               if (layer != width) {
                  continue;
               }

               int surface = SiftMonolithFeature.findTerrainSurface(level, neighbour.x(), neighbour.z());
               if (surface < 0) {
                  return Map.of();
               }

               neighbourTop = surface - 1;
            }

            if (Math.abs(entry.getValue() - neighbourTop) > 1) {
               return Map.of();
            }
         }
      }

      return targets;
   }

   private static BlockState shorelineState(BlockState below, IchorLakeFeature.LakePalette palette) {
      if (!palette.shore().is(ModBlocks.SIFTSLATE_GROWTH)) {
         return palette.shore();
      } else {
         return below.is(ModBlocks.DRY_HEALTHY_SCULK) ? ModBlocks.DRY_HEALTHY_SCULK_GROWTH.defaultBlockState() : ModBlocks.SIFTSLATE_GROWTH.defaultBlockState();
      }
   }

   private static void decorateLakeShore(
      WorldGenLevel level,
      RandomSource random,
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water,
      Map<IchorLakeFeature.Xz, Integer> shoreTargets,
      SiftWorldgenBounds bounds
   ) {
      Map<IchorLakeFeature.Xz, Integer> oasisBand = buildBeachMask(water, 4);
      List<BlockPos> shore = new ArrayList<>(oasisBand.size());

      for (IchorLakeFeature.Xz cell : oasisBand.keySet()) {
         if (bounds.contains(cell.x(), cell.z())) {
            Integer shapedTop = shoreTargets.get(cell);
            int floorY = shapedTop != null ? shapedTop : SiftMonolithFeature.findTerrainSurface(level, cell.x(), cell.z()) - 1;
            if (floorY >= level.getMinY()) {
               shore.add(new BlockPos(cell.x(), floorY, cell.z()));
            }
         }
      }

      SiftSurfaceDecorator.decorateLakeShore(level, random, shore, 2);
   }

   private static int carveAndFill(
      WorldGenLevel level, IchorLakeFeature.LakeColumn column, int waterY, BlockState lining, long salt, int maximumDepth, SiftWorldgenBounds bounds
   ) {
      double centerStrength = 1.0 - Mth.clamp(column.normalizedDistance(), 0.0, 1.0);
      int depth = Mth.clamp(
         1 + (int)Math.floor(centerStrength * (maximumDepth - 0.55) + hash01(column.x(), waterY, column.z(), salt ^ -6752110988234923001L) * 0.7),
         1,
         maximumDepth
      );
      int floorY = waterY - depth;
      MutableBlockPos cursor = new MutableBlockPos();

      for (int y = floorY + 1; y <= Math.max(column.surfaceY() + 2, waterY + 1); y++) {
         cursor.set(column.x(), y, column.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            BlockState state = level.getBlockState(cursor);
            if (SiftMonolithFeature.canReplace(state) || state.is(ModFluids.ICHOR_BLOCK) || isIchorSnow(state)) {
               level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
            }
         }
      }

      for (int y = floorY - 1; y <= floorY; y++) {
         cursor.set(column.x(), y, column.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            level.setBlock(cursor, lining, 2);
         }
      }

      int placed = 0;

      for (int y = floorY + 1; y <= waterY; y++) {
         cursor.set(column.x(), y, column.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            level.setBlock(cursor, ModFluids.ICHOR_BLOCK.defaultBlockState(), 2);
            placed++;
         }
      }

      return placed;
   }

   private static int placeRockFormations(
      WorldGenLevel level,
      RandomSource random,
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water,
      int waterY,
      IchorLakeFeature.LakePalette palette,
      long salt,
      SiftWorldgenBounds bounds
   ) {
      List<IchorLakeFeature.LakeColumn> candidates = water.values()
         .stream()
         .filter(column -> column.normalizedDistance() > 0.2 && column.normalizedDistance() < 0.72)
         .toList();
      if (candidates.isEmpty()) {
         return 0;
      }

      boolean islandScale = water.size() > 400;
      IchorLakeFeature.LakeColumn lakeSample = candidates.get(0);
      boolean overgrown = TheSiftDimension.isOvergrownBiome(SiftCaveHabitat.biome(level, new BlockPos(lakeSample.x(), waterY, lakeSample.z())));
      boolean willowVariant = islandScale && overgrown && random.nextFloat() < 0.27F;
      if (willowVariant) {
         IchorLakeFeature.LakeColumn islandCenter = findWillowIslandCenter(candidates, random);
         if (islandCenter != null) {
            return placeWillowIsland(level, random, water, waterY, palette, salt, bounds, islandCenter);
         }
      }

      int formations = islandScale ? 3 + random.nextInt(2) : 1 + random.nextInt(3);
      List<IchorLakeFeature.Xz> usedCenters = new ArrayList<>();
      int placed = 0;

      for (int formation = 0; formation < formations; formation++) {
         IchorLakeFeature.LakeColumn center = null;

         for (int attempt = 0; attempt < 24 && center == null; attempt++) {
            IchorLakeFeature.LakeColumn candidate = candidates.get(random.nextInt(candidates.size()));
            boolean separated = true;

            for (IchorLakeFeature.Xz used : usedCenters) {
               int dx = candidate.x() - used.x();
               int dz = candidate.z() - used.z();
               if (dx * dx + dz * dz < (islandScale ? 49 : 16)) {
                  separated = false;
                  break;
               }
            }

            if (separated) {
               center = candidate;
            }
         }

         if (center != null) {
            usedCenters.add(new IchorLakeFeature.Xz(center.x(), center.z()));
            int radius = islandScale ? 2 + random.nextInt(3) : 1 + random.nextInt(2);
            int peak = islandScale ? 2 + random.nextInt(4) : 1 + random.nextInt(4);
            long formationSalt = salt ^ formation * -7046029254386353131L;

            for (int dx = -radius; dx <= radius; dx++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  double distance = Math.sqrt(dx * dx + dz * dz);
                  double irregularRadius = radius + (hash01(center.x() + dx, waterY, center.z() + dz, formationSalt) - 0.5) * 0.85;
                  if (!(distance > irregularRadius) && water.containsKey(new IchorLakeFeature.Xz(center.x() + dx, center.z() + dz))) {
                     int height = Math.max(0, peak - Mth.floor(distance * 1.35));
                     height += hash01(center.x() + dx, waterY, center.z() + dz, formationSalt) > 0.78 ? 1 : 0;
                     int bottom = waterY;

                     while (
                        bottom > waterY - 7 && !SiftMonolithFeature.isSiftTerrain(level.getBlockState(new BlockPos(center.x() + dx, bottom, center.z() + dz)))
                     ) {
                        bottom--;
                     }

                     for (int y = bottom; y <= waterY + height; y++) {
                        BlockPos pos = new BlockPos(center.x() + dx, y, center.z() + dz);
                        if (bounds.contains(pos) && level.ensureCanWrite(pos)) {
                           BlockState belowTop = y >= waterY - 1 ? palette.basin() : foundation(palette);
                           BlockState requestedTop = palette.shore();
                           BlockState rock = y == waterY + height ? islandSurfaceState(requestedTop, level.getBlockState(pos.below())) : belowTop;
                           level.setBlock(pos, SiftLandmarkTerrain.growthSurface(level, pos, rock), 2);
                           placed++;
                        }
                     }
                  }
               }
            }
         }
      }

      return placed;
   }

   private static IchorLakeFeature.LakeColumn findWillowIslandCenter(List<IchorLakeFeature.LakeColumn> candidates, RandomSource random) {
      IchorLakeFeature.LakeColumn best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int attempt = 0; attempt < 36; attempt++) {
         IchorLakeFeature.LakeColumn candidate = candidates.get(random.nextInt(candidates.size()));
         double distance = candidate.normalizedDistance();
         if (!(distance < 0.2) && !(distance > 0.46)) {
            double targetDistance = Math.abs(distance - 0.32);
            if (targetDistance < bestDistance) {
               best = candidate;
               bestDistance = targetDistance;
            }
         }
      }

      return best;
   }

   private static int placeWillowIsland(
      WorldGenLevel level,
      RandomSource random,
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> water,
      int waterY,
      IchorLakeFeature.LakePalette palette,
      long salt,
      SiftWorldgenBounds bounds,
      IchorLakeFeature.LakeColumn center
   ) {
      double radiusX = 6.7 + random.nextDouble() * 0.9;
      double radiusZ = 5.5 + random.nextDouble() * 0.9;
      double rotation = random.nextDouble() * Math.PI;
      double cos = Math.cos(rotation);
      double sin = Math.sin(rotation);
      double outlinePhase = random.nextDouble() * Math.PI * 2.0;
      long islandSalt = salt ^ 6289642245231954259L;
      int limit = Mth.ceil(Math.max(radiusX, radiusZ) + 1.0);
      Map<IchorLakeFeature.Xz, Integer> surface = new LinkedHashMap<>();

      for (int dx = -limit; dx <= limit; dx++) {
         for (int dz = -limit; dz <= limit; dz++) {
            int x = center.x() + dx;
            int z = center.z() + dz;
            IchorLakeFeature.Xz cell = new IchorLakeFeature.Xz(x, z);
            if (water.containsKey(cell) && bounds.contains(x, z)) {
               double localX = dx * cos - dz * sin;
               double localZ = dx * sin + dz * cos;
               double normalized = Math.sqrt(localX * localX / (radiusX * radiusX) + localZ * localZ / (radiusZ * radiusZ));
               double theta = Math.atan2(localZ, localX);
               double edge = 1.0 + Math.sin(theta * 3.0 + outlinePhase) * 0.075 + Math.sin(theta * 5.0 - outlinePhase * 0.63) * 0.045;
               if (!(normalized > edge)) {
                  double inward = 1.0 - normalized / edge;
                  int height = Mth.floor(inward * 3.15);
                  surface.put(cell, waterY + Mth.clamp(height, 0, 3));
               }
            }
         }
      }

      if (surface.size() < 70) {
         return 0;
      }

      MutableBlockPos cursor = new MutableBlockPos();
      int placed = 0;

      for (Entry<IchorLakeFeature.Xz, Integer> entry : surface.entrySet()) {
         IchorLakeFeature.Xz cell = entry.getKey();
         int topY = entry.getValue();
         int bottomY = waterY;

         while (bottomY > waterY - 7 && !SiftMonolithFeature.isSiftTerrain(level.getBlockState(new BlockPos(cell.x(), bottomY, cell.z())))) {
            bottomY--;
         }

         double patch = hash01(cell.x() >> 1, topY, cell.z() >> 1, islandSalt);
         BlockState surfaceState = patch < 0.13
            ? ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState()
            : (patch < 0.38 ? ModBlocks.HEALTHY_SCULK.defaultBlockState() : ModBlocks.SIFTSLATE_GROWTH.defaultBlockState());

         for (int y = bottomY; y <= topY; y++) {
            cursor.set(cell.x(), y, cell.z());
            if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
               BlockState state;
               if (y == topY) {
                  state = islandSurfaceState(surfaceState, level.getBlockState(cursor.below()));
               } else if (y >= waterY && patch < 0.38) {
                  state = patch < 0.13 ? ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState() : ModBlocks.HEALTHY_SCULK.defaultBlockState();
               } else {
                  state = y >= topY - 2 ? palette.basin() : foundation(palette);
               }

               level.setBlock(cursor, SiftLandmarkTerrain.growthSurface(level, cursor, state), 2);
               placed++;
            }
         }

         cursor.set(cell.x(), topY + 1, cell.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            BlockState above = level.getBlockState(cursor);
            if (above.is(ModFluids.ICHOR_BLOCK) || SiftMonolithFeature.canReplace(above) || isIchorSnow(above)) {
               level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
            }
         }
      }

      IchorLakeFeature.Xz rootCell = new IchorLakeFeature.Xz(center.x(), center.z());
      Integer rootTop = surface.get(rootCell);
      if (rootTop == null) {
         rootCell = surface.keySet().stream().min(Comparator.comparingInt(cellx -> {
            int dx = cellx.x() - center.x();
            int dz = cellx.z() - center.z();
            return dx * dx + dz * dz;
         })).orElse(null);
         rootTop = rootCell == null ? null : surface.get(rootCell);
      }

      if (rootCell != null && rootTop != null) {
         BlockPos root = new BlockPos(rootCell.x(), rootTop + 1, rootCell.z());
         OvergrownWillowTreeFeature.placeAt(level, random, root, bounds, false);
      }

      for (Entry<IchorLakeFeature.Xz, Integer> entry : surface.entrySet()) {
         IchorLakeFeature.Xz cell = entry.getKey();
         BlockPos floor = new BlockPos(cell.x(), entry.getValue(), cell.z());
         SiftSurfaceDecorator.decorate(level, random, floor, 2.85F, 2);
      }

      return placed;
   }

   private static BlockState islandSurfaceState(BlockState requestedTop, BlockState below) {
      return requestedTop.is(ModBlocks.SIFTSLATE_GROWTH) && below.is(ModBlocks.DRY_HEALTHY_SCULK)
         ? ModBlocks.DRY_HEALTHY_SCULK_GROWTH.defaultBlockState()
         : requestedTop;
   }

   private static IchorLakeFeature.CascadeSite findCascadeSite(WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ) {
      IchorLakeFeature.CascadeSite best = null;
      int bestScore = Integer.MIN_VALUE;

      for (int direction = 0; direction < 16; direction++) {
         double angle = direction * Math.PI * 2.0 / 16.0;
         double forwardX = Math.cos(angle);
         double forwardZ = Math.sin(angle);
         int upperX = centerX - (int)Math.round(forwardX * 7.0);
         int upperZ = centerZ - (int)Math.round(forwardZ * 7.0);
         int lowerX = centerX + (int)Math.round(forwardX * 9.0);
         int lowerZ = centerZ + (int)Math.round(forwardZ * 9.0);
         if (bounds.contains(upperX, upperZ) && bounds.contains(lowerX, lowerZ)) {
            int upperSurface = SiftMonolithFeature.findTerrainSurface(level, upperX, upperZ);
            int lowerSurface = SiftMonolithFeature.findTerrainSurface(level, lowerX, lowerZ);
            if (upperSurface >= 0 && lowerSurface >= 0) {
               int drop = upperSurface - lowerSurface;
               if (drop >= 10 && drop <= 34) {
                  int upperRelief = sampleRelief(level, bounds, upperX, upperZ, 4);
                  int lowerRelief = sampleRelief(level, bounds, lowerX, lowerZ, 7);
                  if (upperRelief >= 0 && lowerRelief >= 0 && upperRelief <= 4 && lowerRelief <= 5) {
                     int minX = Math.min(upperX - 8, lowerX - 12);
                     int maxX = Math.max(upperX + 8, lowerX + 12);
                     int minZ = Math.min(upperZ - 8, lowerZ - 12);
                     int maxZ = Math.max(upperZ + 8, lowerZ + 12);
                     if (bounds.contains(minX, minZ) && bounds.contains(maxX, maxZ) && !SiftFeaturePlacementGuard.intersectsPortal(minX, minZ, maxX, maxZ)) {
                        int score = drop * 8 - upperRelief * 3 - lowerRelief * 2;
                        if (score > bestScore) {
                           bestScore = score;
                           best = new IchorLakeFeature.CascadeSite(
                              new BlockPos(upperX, upperSurface - 1, upperZ), new BlockPos(lowerX, lowerSurface - 1, lowerZ), forwardX, forwardZ
                           );
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static int placeCascade(
      WorldGenLevel level, RandomSource random, SiftWorldgenBounds bounds, IchorLakeFeature.CascadeSite site, IchorLakeFeature.LakePalette palette
   ) {
      long salt = random.nextLong();
      int lowerRadiusX = 10 + random.nextInt(2);
      int lowerRadiusZ = 9 + random.nextInt(2);
      int beachWidth = 4;
      double angle = Math.atan2(site.forwardZ(), site.forwardX());
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> lowerWater = buildWaterMask(
         level, bounds, site.lowerCenter().getX(), site.lowerCenter().getZ(), lowerRadiusX, lowerRadiusZ, angle, Math.cos(angle), Math.sin(angle), salt
      );
      if (lowerWater.size() < 135) {
         return 0;
      }

      List<Integer> lowerSurfaces = lowerWater.values().stream().map(IchorLakeFeature.LakeColumn::surfaceY).sorted().toList();
      int lowerWaterY = lowerSurfaces.get(lowerSurfaces.size() / 2) - 1;
      if (lowerSurfaces.get(lowerSurfaces.size() - 1) - lowerSurfaces.get(0) > 5) {
         return 0;
      }

      Map<IchorLakeFeature.Xz, Integer> lowerBeach = buildBeachMask(lowerWater, beachWidth);
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> lowerBeachColumns = validateBeach(level, bounds, lowerBeach, beachWidth, lowerWaterY, 5);
      if (lowerBeachColumns.isEmpty()) {
         return 0;
      }

      Map<IchorLakeFeature.Xz, Integer> lowerShoreTargets = buildShoreTargets(level, lowerWater, lowerBeach, lowerBeachColumns, lowerWaterY, beachWidth);
      if (lowerShoreTargets.isEmpty()) {
         return 0;
      }

      Map<IchorLakeFeature.Xz, IchorLakeFeature.ShelfColumn> shelf = buildShelfMask(level, bounds, site, salt);
      if (shelf.size() < 155) {
         return 0;
      }

      Map<IchorLakeFeature.Xz, Double> upperPond = buildUpperPondMask(site, shelf, salt);
      if (upperPond.size() < 52) {
         return 0;
      }

      int minX = Integer.MAX_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxZ = Integer.MIN_VALUE;

      for (IchorLakeFeature.Xz cell : lowerBeach.keySet()) {
         minX = Math.min(minX, cell.x());
         minZ = Math.min(minZ, cell.z());
         maxX = Math.max(maxX, cell.x());
         maxZ = Math.max(maxZ, cell.z());
      }

      for (IchorLakeFeature.Xz cell : shelf.keySet()) {
         minX = Math.min(minX, cell.x());
         minZ = Math.min(minZ, cell.z());
         maxX = Math.max(maxX, cell.x());
         maxZ = Math.max(maxZ, cell.z());
      }

      minX = Math.min(minX, Math.min(site.upperCenter().getX(), site.lowerCenter().getX()) - 5);
      minZ = Math.min(minZ, Math.min(site.upperCenter().getZ(), site.lowerCenter().getZ()) - 5);
      maxX = Math.max(maxX, Math.max(site.upperCenter().getX(), site.lowerCenter().getX()) + 5);
      maxZ = Math.max(maxZ, Math.max(site.upperCenter().getZ(), site.lowerCenter().getZ()) + 5);
      if (!SiftFeaturePlacementGuard.tryReserveCarver(minX, minZ, maxX, maxZ)) {
         return 0;
      }

      int placed = 0;

      for (Entry<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> entry : lowerBeachColumns.entrySet()) {
         placed += shapeBeach(level, entry.getValue(), lowerShoreTargets.get(entry.getKey()), palette, bounds);
      }

      for (IchorLakeFeature.LakeColumn column : lowerWater.values()) {
         placed += carveAndFill(level, column, lowerWaterY, palette.basin(), salt ^ 5498709625288019790L, 5, bounds);
      }

      placed += shapeMountainShelf(level, shelf, site.upperCenter().getY(), palette, bounds);
      placed += carveUpperPond(level, upperPond, site.upperCenter().getY(), palette.basin(), salt, bounds);
      placed += carveCascadeChannel(level, site, lowerWaterY, palette.basin(), bounds);
      if (random.nextFloat() < 0.88F) {
         placed += placeRockFormations(level, random, lowerWater, lowerWaterY, palette, salt ^ 4846246222350271820L, bounds);
      }

      decorateLakeShore(level, random, lowerWater, lowerShoreTargets, bounds);
      return placed;
   }

   private static Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> validateBeach(
      WorldGenLevel level, SiftWorldgenBounds bounds, Map<IchorLakeFeature.Xz, Integer> beach, int width, int waterY, int maximumRelief
   ) {
      Map<IchorLakeFeature.Xz, IchorLakeFeature.LakeColumn> columns = new LinkedHashMap<>();

      for (Entry<IchorLakeFeature.Xz, Integer> entry : beach.entrySet()) {
         IchorLakeFeature.Xz cell = entry.getKey();
         if (!bounds.contains(cell.x(), cell.z())) {
            return Map.of();
         }

         int surfaceY = SiftMonolithFeature.findTerrainSurface(level, cell.x(), cell.z());
         if (surfaceY >= 0 && SiftMonolithFeature.isSiftTerrain(level.getBlockState(new BlockPos(cell.x(), surfaceY - 1, cell.z())))) {
            int naturalTop = surfaceY - 1;
            int layer = entry.getValue();
            int targetTop = targetShoreTop(naturalTop, waterY, layer, width);
            if (naturalTop >= waterY - width
               && naturalTop <= waterY + maximumRelief + width
               && Math.abs(targetTop - naturalTop) <= width
               && hasNaturalSupport(level, cell.x(), naturalTop, cell.z(), 3)) {
               columns.put(cell, new IchorLakeFeature.LakeColumn(cell.x(), cell.z(), surfaceY, 1.0 + (double)layer / width));
               continue;
            }

            return Map.of();
         }

         return Map.of();
      }

      return columns;
   }

   private static Map<IchorLakeFeature.Xz, IchorLakeFeature.ShelfColumn> buildShelfMask(
      WorldGenLevel level, SiftWorldgenBounds bounds, IchorLakeFeature.CascadeSite site, long salt
   ) {
      Map<IchorLakeFeature.Xz, IchorLakeFeature.ShelfColumn> shelf = new LinkedHashMap<>();
      int centerX = site.upperCenter().getX();
      int centerZ = site.upperCenter().getZ();
      int topY = site.upperCenter().getY();

      for (int dx = -10; dx <= 10; dx++) {
         for (int dz = -10; dz <= 10; dz++) {
            double forward = dx * site.forwardX() + dz * site.forwardZ();
            double side = -dx * site.forwardZ() + dz * site.forwardX();
            double forwardRadius = forward >= 0.0 ? 8.5 : 8.8;
            double distance = Math.sqrt(forward * forward / (forwardRadius * forwardRadius) + side * side / 77.44000000000001);
            double edge = 1.0 + (hash01(centerX + dx >> 1, topY, centerZ + dz >> 1, salt) - 0.5) * 0.13;
            if (!(distance > edge)) {
               int x = centerX + dx;
               int z = centerZ + dz;
               if (!bounds.contains(x, z)) {
                  return Map.of();
               }

               int surfaceY = SiftMonolithFeature.findTerrainSurface(level, x, z);
               if (surfaceY < 0 || !hasNaturalSupport(level, x, surfaceY - 1, z, 3)) {
                  return Map.of();
               }

               int naturalTop = surfaceY - 1;
               if (naturalTop > topY + 5 || topY - naturalTop > 26) {
                  return Map.of();
               }

               shelf.put(new IchorLakeFeature.Xz(x, z), new IchorLakeFeature.ShelfColumn(x, z, naturalTop, distance, forward, side));
            }
         }
      }

      return shelf;
   }

   private static Map<IchorLakeFeature.Xz, Double> buildUpperPondMask(
      IchorLakeFeature.CascadeSite site, Map<IchorLakeFeature.Xz, IchorLakeFeature.ShelfColumn> shelf, long salt
   ) {
      Map<IchorLakeFeature.Xz, Double> pond = new LinkedHashMap<>();
      double centerX = site.upperCenter().getX() - site.forwardX() * 1.8;
      double centerZ = site.upperCenter().getZ() - site.forwardZ() * 1.8;

      for (IchorLakeFeature.ShelfColumn column : shelf.values()) {
         double dx = column.x() - centerX;
         double dz = column.z() - centerZ;
         double forward = dx * site.forwardX() + dz * site.forwardZ();
         double side = -dx * site.forwardZ() + dz * site.forwardX();
         double distance = Math.sqrt(forward * forward / 32.49 + side * side / 27.040000000000003);
         double edge = 1.0 + (hash01(column.x(), site.upperCenter().getY(), column.z(), salt ^ 6147501750024687438L) - 0.5) * 0.12;
         if (distance <= edge) {
            pond.put(new IchorLakeFeature.Xz(column.x(), column.z()), distance / edge);
         }
      }

      return pond;
   }

   private static int shapeMountainShelf(
      WorldGenLevel level,
      Map<IchorLakeFeature.Xz, IchorLakeFeature.ShelfColumn> shelf,
      int topY,
      IchorLakeFeature.LakePalette palette,
      SiftWorldgenBounds bounds
   ) {
      MutableBlockPos cursor = new MutableBlockPos();
      int placed = 0;

      for (IchorLakeFeature.ShelfColumn column : shelf.values()) {
         double edgeStrength = smoothstep(1.0, 0.7, column.normalizedDistance());
         boolean channelCore = column.forward() >= -3.0 && column.forward() <= 7.6 && Math.abs(column.side()) <= 3.8;
         double strength = channelCore ? 1.0 : edgeStrength;
         int targetTop = Mth.floor(Mth.lerp(strength, column.naturalTop(), topY) + 0.5);

         for (int y = targetTop + 1; y <= Math.max(column.naturalTop() + 3, targetTop + 3); y++) {
            cursor.set(column.x(), y, column.z());
            if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
               BlockState state = level.getBlockState(cursor);
               if (SiftMonolithFeature.canReplace(state) || SiftMonolithFeature.isSiftTerrain(state) || isIchorSnow(state)) {
                  level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
               }
            }
         }

         int bottom = Math.min(column.naturalTop(), targetTop) - 2;

         for (int y = bottom; y <= targetTop; y++) {
            cursor.set(column.x(), y, column.z());
            if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
               BlockState state = y == targetTop ? palette.shore() : (y <= targetTop - 2 ? foundation(palette) : palette.basin());
               level.setBlock(cursor, state, 2);
               placed++;
            }
         }
      }

      return placed;
   }

   private static int carveUpperPond(
      WorldGenLevel level, Map<IchorLakeFeature.Xz, Double> pond, int waterY, BlockState lining, long salt, SiftWorldgenBounds bounds
   ) {
      MutableBlockPos cursor = new MutableBlockPos();
      int placed = 0;

      for (Entry<IchorLakeFeature.Xz, Double> entry : pond.entrySet()) {
         IchorLakeFeature.Xz cell = entry.getKey();
         double centerStrength = 1.0 - Mth.clamp(entry.getValue(), 0.0, 1.0);
         int depth = Mth.clamp(1 + Mth.floor(centerStrength * 2.8 + hash01(cell.x(), waterY, cell.z(), salt) * 0.55), 1, 4);
         int floorY = waterY - depth;

         for (int y = floorY - 1; y <= floorY; y++) {
            cursor.set(cell.x(), y, cell.z());
            if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
               level.setBlock(cursor, lining, 2);
               placed++;
            }
         }

         for (int y = floorY + 1; y <= waterY; y++) {
            cursor.set(cell.x(), y, cell.z());
            if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
               level.setBlock(cursor, ModFluids.ICHOR_BLOCK.defaultBlockState(), 2);
               placed++;
            }
         }

         cursor.set(cell.x(), waterY + 1, cell.z());
         if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
         }
      }

      return placed;
   }

   private static int carveCascadeChannel(WorldGenLevel level, IchorLakeFeature.CascadeSite site, int lowerWaterY, BlockState lining, SiftWorldgenBounds bounds) {
      int placed = 0;
      int upperWaterY = site.upperCenter().getY();
      double sideX = -site.forwardZ();
      double sideZ = site.forwardX();
      int lipX = site.upperCenter().getX() + (int)Math.round(site.forwardX() * 7.0);
      int lipZ = site.upperCenter().getZ() + (int)Math.round(site.forwardZ() * 7.0);
      MutableBlockPos cursor = new MutableBlockPos();
      BlockState fallingIchor = ModFluids.ICHOR_BLOCK.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);

      for (int step = 1; step <= 7; step++) {
         int centerX = site.upperCenter().getX() + (int)Math.round(site.forwardX() * step);
         int centerZ = site.upperCenter().getZ() + (int)Math.round(site.forwardZ() * step);
         int halfWidth = step < 3 ? 2 : 3;

         for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            int x = centerX + (int)Math.round(sideX * lateral);
            int z = centerZ + (int)Math.round(sideZ * lateral);
            placed += placeChannelCell(level, x, upperWaterY, z, lining, bounds);
         }
      }

      for (int lateral = -3; lateral <= 3; lateral++) {
         int x = lipX + (int)Math.round(sideX * lateral);
         int z = lipZ + (int)Math.round(sideZ * lateral);

         for (int y = lowerWaterY; y <= upperWaterY; y++) {
            cursor.set(x, y, z);
            if (bounds.contains(cursor) && level.ensureCanWrite(cursor)) {
               level.setBlock(cursor, y == upperWaterY ? ModFluids.ICHOR_BLOCK.defaultBlockState() : fallingIchor, 2);
               placed++;
            }
         }
      }

      double dx = site.lowerCenter().getX() - lipX;
      double dz = site.lowerCenter().getZ() - lipZ;
      int length = Math.max(1, Mth.ceil(Math.sqrt(dx * dx + dz * dz)));

      for (int step = 0; step <= length; step++) {
         double progress = (double)step / length;
         int x = Mth.floor(Mth.lerp(progress, lipX, site.lowerCenter().getX()) + 0.5);
         int z = Mth.floor(Mth.lerp(progress, lipZ, site.lowerCenter().getZ()) + 0.5);
         placed += placeChannelCell(level, x, lowerWaterY, z, lining, bounds);
      }

      return placed;
   }

   private static int placeChannelCell(WorldGenLevel level, int x, int y, int z, BlockState lining, SiftWorldgenBounds bounds) {
      BlockPos floor = new BlockPos(x, y - 1, z);
      BlockPos fluid = new BlockPos(x, y, z);
      BlockPos above = new BlockPos(x, y + 1, z);
      if (bounds.contains(fluid) && level.ensureCanWrite(fluid)) {
         if (bounds.contains(floor) && level.ensureCanWrite(floor)) {
            level.setBlock(floor, lining, 2);
         }

         level.setBlock(fluid, ModFluids.ICHOR_BLOCK.defaultBlockState(), 2);
         if (bounds.contains(above) && level.ensureCanWrite(above)) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
         }

         return 1;
      } else {
         return 0;
      }
   }

   private static boolean hasNearbyIchor(WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ, int radius) {
      for (int dx = -radius; dx <= radius; dx += 5) {
         for (int dz = -radius; dz <= radius; dz += 5) {
            if (dx * dx + dz * dz <= radius * radius) {
               int x = centerX + dx;
               int z = centerZ + dz;
               if (bounds.contains(x, z)) {
                  int surface = SiftMonolithFeature.findTerrainSurface(level, x, z);
                  if (surface >= 0) {
                     for (int dy = -6; dy <= 1; dy++) {
                        if (level.getBlockState(new BlockPos(x, surface + dy, z)).is(ModFluids.ICHOR_BLOCK)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean hasNaturalSupport(WorldGenLevel level, int x, int y, int z, int depth) {
      for (int offset = 0; offset < depth; offset++) {
         if (!SiftMonolithFeature.isSiftTerrain(level.getBlockState(new BlockPos(x, y - offset, z)))) {
            return false;
         }
      }

      return true;
   }

   private static double smoothstep(double edge0, double edge1, double value) {
      double t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
      return t * t * (3.0 - 2.0 * t);
   }

   private static double hash01(int x, int y, int z, long salt) {
      long value = salt;
      value ^= x * -7046029254386353131L;
      value ^= y * -4417276706812531889L;
      value ^= z * 1609587929392839161L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      value ^= value >>> 31;
      return (value >>> 11) * 1.110223E-16F;
   }

   private static boolean isIchorSnow(BlockState state) {
      return state.is(ModBlocks.ICHOR_SNOW) || state.is(ModBlocks.ICHOR_SNOW_BLOCK);
   }

   private static BlockState foundation(IchorLakeFeature.LakePalette palette) {
      return palette.basin().is(ModBlocks.SOULSHALE) ? palette.basin() : ModBlocks.SIFTSLATE.defaultBlockState();
   }

   private record CascadeSite(BlockPos upperCenter, BlockPos lowerCenter, double forwardX, double forwardZ) {
   }

   private record LakeColumn(int x, int z, int surfaceY, double normalizedDistance) {
   }

   private record LakePalette(BlockState basin, BlockState shore) {
   }

   private enum LakeVariant {
      SMALL(4, 7, 1, 2, 3, 4, 28, false),
      MEDIUM(8, 12, 2, 3, 4, 4, 90, false),
      LARGE(12, 15, 4, 5, 5, 5, 300, true),
      MEGA(14, 14, 6, 6, 6, 4, 450, true);

      private final int minimumRadius;
      private final int maximumRadius;
      private final int minimumBeach;
      private final int maximumBeach;
      private final int maximumDepth;
      private final int maximumRelief;
      private final int minimumArea;
      private final boolean rocks;

      LakeVariant(int minimumRadius, int maximumRadius, int minimumBeach, int maximumBeach, int maximumDepth, int maximumRelief, int minimumArea, boolean rocks) {
         this.minimumRadius = minimumRadius;
         this.maximumRadius = maximumRadius;
         this.minimumBeach = minimumBeach;
         this.maximumBeach = maximumBeach;
         this.maximumDepth = maximumDepth;
         this.maximumRelief = maximumRelief;
         this.minimumArea = minimumArea;
         this.rocks = rocks;
      }

      int minimumRadius() {
         return this.minimumRadius;
      }

      int maximumRadius() {
         return this.maximumRadius;
      }

      int minimumBeach() {
         return this.minimumBeach;
      }

      int maximumBeach() {
         return this.maximumBeach;
      }

      int maximumDepth() {
         return this.maximumDepth;
      }

      int maximumRelief() {
         return this.maximumRelief;
      }

      int minimumArea() {
         return this.minimumArea;
      }

      boolean rocks() {
         return this.rocks;
      }
   }

   private record ShelfColumn(int x, int z, int naturalTop, double normalizedDistance, double forward, double side) {
   }

   private record Xz(int x, int z) {
   }
}
