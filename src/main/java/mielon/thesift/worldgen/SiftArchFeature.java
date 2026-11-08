package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SiftArchFeature extends Feature<NoneFeatureConfiguration> {
   private static final int[][] AXES = new int[][]{{1, 0}, {0, 1}, {1, 1}, {1, -1}};

   public SiftArchFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos placementOrigin = context.origin();
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(placementOrigin);
      int centerX = (placementOrigin.getX() & -16) + 8 + random.nextInt(3) - 1;
      int centerZ = (placementOrigin.getZ() & -16) + 8 + random.nextInt(3) - 1;
      BlockPos featureCenter = new BlockPos(centerX, placementOrigin.getY(), centerZ);
      if (SiftFeaturePlacementGuard.intersectsPortal(featureCenter, 23)) {
         return false;
      }

      int centerY = SiftMonolithFeature.findTerrainSurface(level, centerX, centerZ);
      if (centerY < 0) {
         return false;
      }

      if (SiftCampProtection.near(level, centerX, centerZ, 24)) {
         return false;
      }

      int halfSpan = 13 + random.nextInt(3);
      SiftArchFeature.Candidate best = findBestSpan(level, bounds, centerX, centerZ, centerY, halfSpan, random);
      if (best == null) {
         return false;
      }

      int baselineMiddle = (best.leftY + best.rightY) / 2;
      int archRise = 7 + random.nextInt(6) + Math.min(5, best.valleyDepth / 5);
      int topAtMiddle = baselineMiddle + archRise + 5;
      if (topAtMiddle >= level.getMaxY()) {
         archRise -= topAtMiddle - level.getMaxY() + 1;
      }

      if (archRise < 6) {
         return false;
      }

      BlockState leftFloor = level.getBlockState(best.leftFloor(centerX, centerZ));
      BlockState rightFloor = level.getBlockState(best.rightFloor(centerX, centerZ));
      boolean healthyGround = isHealthy(leftFloor) || isHealthy(rightFloor);
      boolean growthGround = leftFloor.is(ModBlocks.SIFTSLATE_GROWTH) || rightFloor.is(ModBlocks.SIFTSLATE_GROWTH);
      float healthyChance = healthyGround ? 0.78F : (growthGround ? 0.22F : 0.43F);
      boolean coral = TheSiftDimension.isCoralBiome(level.getBiome(new BlockPos(centerX, centerY, centerZ)));
      boolean healthy = random.nextFloat() < healthyChance && !coral;
      BlockState body = (coral ? ModBlocks.SOULSHALE : (healthy ? ModBlocks.HEALTHY_SCULK : ModBlocks.SIFTSLATE)).defaultBlockState();
      boolean growthVariant = !healthy && growthGround;
      double baseThickness = 2.45 + random.nextDouble() * 0.9;
      int span = best.halfSpan * 2;
      int steps = span * 2;
      long shapeSalt = random.nextLong();
      int blocksPlaced = 0;
      SiftPackedLongSet archBlocks = new SiftPackedLongSet(4096);
      List<BlockPos> crownPath = new ArrayList<>();

      for (int step = 0; step <= steps; step++) {
         double t = (double)step / steps;
         SiftArchFeature.CurvePoint point = curvePoint(best, centerX, centerZ, t, archRise, shapeSalt);
         double endStrength = Math.pow(Math.abs(t * 2.0 - 1.0), 4.0);
         double thicknessWave = 0.93 + 0.09 * Math.sin(t * Math.PI * 5.0 + phase(shapeSalt, 11)) + 0.045 * Math.sin(t * Math.PI * 9.0 + phase(shapeSalt, 29));
         double localThickness = baseThickness * thicknessWave + endStrength * 1.15;
         blocksPlaced += fillOrganicSphere(level, bounds, point, localThickness, body, healthy, step, shapeSalt, archBlocks);
         if (step % 5 == 0) {
            crownPath.add(new BlockPos((int)Math.round(point.x), (int)Math.round(point.y + localThickness), (int)Math.round(point.z)));
         }
      }

      blocksPlaced += blendAnchorIntoTerrain(level, bounds, best, centerX, centerZ, true, baseThickness, body, healthy, shapeSalt ^ 1279608404L, archBlocks);
      blocksPlaced += blendAnchorIntoTerrain(level, bounds, best, centerX, centerZ, false, baseThickness, body, healthy, shapeSalt ^ 353416726612L, archBlocks);
      carveGouges(level, random, best, centerX, centerZ, archRise, baseThickness, shapeSalt, archBlocks);
      if (growthVariant) {
         coatExposedCrownWithGrowth(level, bounds, archBlocks);
      }

      for (BlockPos crownPos : crownPath) {
         SiftSurfaceDecorator.decorate(level, random, crownPos, healthy ? 0.82F : 0.72F);
      }

      return blocksPlaced > 0;
   }

   private static int blendAnchorIntoTerrain(
      WorldGenLevel level,
      SiftWorldgenBounds bounds,
      SiftArchFeature.Candidate candidate,
      int centerX,
      int centerZ,
      boolean left,
      double baseThickness,
      BlockState body,
      boolean healthy,
      long salt,
      SiftPackedLongSet archBlocks
   ) {
      double t = left ? 0.0 : 1.0;
      SiftArchFeature.CurvePoint endpoint = curvePoint(candidate, centerX, centerZ, t, 0, salt);
      double outward = left ? -1.0 : 1.0;
      double perpendicularX = -candidate.axisZ;
      double perpendicularZ = candidate.axisX;
      int previousSurface = left ? candidate.leftY : candidate.rightY;
      int placed = 0;
      int samples = 5;

      for (int sample = 0; sample < samples; sample++) {
         double progress = (sample + 1.0) / samples;
         double distance = 0.85 + sample * 0.78;
         double sideWave = Math.sin(progress * Math.PI * 2.4 + phase(salt, 13)) * 0.42 * (1.0 - progress);
         double x = endpoint.x + candidate.axisX * outward * distance + perpendicularX * sideWave;
         double z = endpoint.z + candidate.axisZ * outward * distance + perpendicularZ * sideWave;
         int sampleX = (int)Math.round(x);
         int sampleZ = (int)Math.round(z);
         if (!bounds.contains(sampleX, sampleZ)) {
            break;
         }

         int surface = SiftMonolithFeature.findTerrainSurface(level, sampleX, sampleZ);
         if (surface < 0 || Math.abs(surface - previousSurface) > 9) {
            break;
         }

         previousSurface = surface;
         double eased = smootherstep(progress);
         double centerY = endpoint.y + (surface - 0.35 - endpoint.y) * eased;
         double radius = baseThickness + 0.9 - progress * (baseThickness - 0.55) + Math.sin(progress * Math.PI * 3.0 + phase(salt, 31)) * 0.16;
         placed += fillOrganicSphere(
            level, bounds, new SiftArchFeature.CurvePoint(x, centerY, z), Math.max(1.25, radius), body, healthy, left ? -sample : sample, salt, archBlocks
         );
      }

      return placed;
   }

   private static void coatExposedCrownWithGrowth(WorldGenLevel level, SiftWorldgenBounds bounds, SiftPackedLongSet archBlocks) {
      archBlocks.forEach(
         packed -> {
            BlockPos pos = BlockPos.of(packed);
            BlockPos abovePos = pos.above();
            if (bounds.contains(pos)
               && level.ensureCanWrite(pos)
               && !archBlocks.contains(abovePos.asLong())
               && isOpenCrownSpace(level.getBlockState(abovePos))
               && SiftLandmarkTerrain.allowsGrowth(level, pos)
               && level.getBlockState(pos).is(ModBlocks.SIFTSLATE)) {
               level.setBlock(pos, ModBlocks.SIFTSLATE_GROWTH.defaultBlockState(), 2);
            }
         }
      );
   }

   private static boolean isOpenCrownSpace(BlockState state) {
      return state.isAir()
         || state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS);
   }

   private static SiftArchFeature.Candidate findBestSpan(
      WorldGenLevel level, SiftWorldgenBounds bounds, int centerX, int centerZ, int centerY, int halfSpan, RandomSource random
   ) {
      SiftArchFeature.Candidate best = null;
      double bestScore = -Double.MAX_VALUE;

      for (int[] direction : AXES) {
         double normalization = direction[0] != 0 && direction[1] != 0 ? 0.70710678118 : 1.0;
         double axisX = direction[0] * normalization;
         double axisZ = direction[1] * normalization;
         int leftX = centerX - (int)Math.round(axisX * halfSpan);
         int leftZ = centerZ - (int)Math.round(axisZ * halfSpan);
         int rightX = centerX + (int)Math.round(axisX * halfSpan);
         int rightZ = centerZ + (int)Math.round(axisZ * halfSpan);
         if (bounds.contains(leftX, leftZ) && bounds.contains(rightX, rightZ)) {
            int leftY = SiftMonolithFeature.findTerrainSurface(level, leftX, leftZ);
            int rightY = SiftMonolithFeature.findTerrainSurface(level, rightX, rightZ);
            if (leftY >= 0 && rightY >= 0) {
               int innerDistance = Math.max(7, halfSpan / 2);
               int leftInnerX = centerX - (int)Math.round(axisX * (halfSpan - innerDistance));
               int leftInnerZ = centerZ - (int)Math.round(axisZ * (halfSpan - innerDistance));
               int rightInnerX = centerX + (int)Math.round(axisX * (halfSpan - innerDistance));
               int rightInnerZ = centerZ + (int)Math.round(axisZ * (halfSpan - innerDistance));
               if (bounds.contains(leftInnerX, leftInnerZ) && bounds.contains(rightInnerX, rightInnerZ)) {
                  int leftInnerY = SiftMonolithFeature.findTerrainSurface(level, leftInnerX, leftInnerZ);
                  int rightInnerY = SiftMonolithFeature.findTerrainSurface(level, rightInnerX, rightInnerZ);
                  if (leftInnerY >= 0 && rightInnerY >= 0) {
                     int heightDifference = Math.abs(leftY - rightY);
                     int valleyDepth = Math.min(leftY, rightY) - centerY;
                     int leftDrop = leftY - leftInnerY;
                     int rightDrop = rightY - rightInnerY;
                     if (heightDifference <= 20 && valleyDepth >= 6 && leftDrop >= 3 && rightDrop >= 3 && leftDrop + rightDrop >= 8) {
                        double score = valleyDepth * 5.5 + (leftDrop + rightDrop) * 2.2 - heightDifference * 0.75 + random.nextDouble();
                        if (score > bestScore) {
                           bestScore = score;
                           best = new SiftArchFeature.Candidate(axisX, axisZ, halfSpan, leftY, rightY, valleyDepth);
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private static SiftArchFeature.CurvePoint curvePoint(SiftArchFeature.Candidate candidate, int centerX, int centerZ, double t, int archRise, long salt) {
      double signedDistance = (t * 2.0 - 1.0) * candidate.halfSpan;
      double archMask = Math.sin(Math.PI * t);
      double lateralWave = (Math.sin(t * Math.PI * 2.0 + phase(salt, 7)) * 0.68 + Math.sin(t * Math.PI * 5.0 + phase(salt, 23)) * 0.32) * 1.45 * archMask;
      double verticalWave = (Math.sin(t * Math.PI * 3.0 + phase(salt, 17)) * 0.72 + Math.sin(t * Math.PI * 7.0 + phase(salt, 37)) * 0.28) * 1.05 * archMask;
      double perpendicularX = -candidate.axisZ;
      double perpendicularZ = candidate.axisX;
      double baseY = candidate.leftY + (candidate.rightY - candidate.leftY) * t;
      return new SiftArchFeature.CurvePoint(
         centerX + candidate.axisX * signedDistance + perpendicularX * lateralWave,
         baseY + archRise * 4.0 * t * (1.0 - t) + verticalWave,
         centerZ + candidate.axisZ * signedDistance + perpendicularZ * lateralWave
      );
   }

   private static int fillOrganicSphere(
      WorldGenLevel level,
      SiftWorldgenBounds bounds,
      SiftArchFeature.CurvePoint center,
      double radius,
      BlockState mainState,
      boolean healthy,
      int stripeSeed,
      long salt,
      SiftPackedLongSet placedBlocks
   ) {
      int placed = 0;
      int limit = (int)Math.ceil(radius + 0.5);
      int roundedX = (int)Math.round(center.x);
      int roundedY = (int)Math.round(center.y);
      int roundedZ = (int)Math.round(center.z);
      MutableBlockPos cursor = new MutableBlockPos();

      for (int dx = -limit; dx <= limit; dx++) {
         for (int dy = -limit; dy <= limit; dy++) {
            for (int dz = -limit; dz <= limit; dz++) {
               double roughness = (hash01(roundedX + dx, roundedY + dy, roundedZ + dz, salt) - 0.5) * 0.62;
               double edgeRadius = radius + roughness;
               if (!(dx * dx + dy * dy + dz * dz > edgeRadius * edgeRadius)) {
                  cursor.set(roundedX + dx, roundedY + dy, roundedZ + dz);
                  if (bounds.contains(cursor) && level.ensureCanWrite(cursor) && SiftMonolithFeature.canReplace(level.getBlockState(cursor))) {
                     boolean dryVein = healthy
                        && dy < 0
                        && stripeSeed / 8 % 7 == 3
                        && hash01(cursor.getX(), cursor.getY(), cursor.getZ(), salt ^ -7046029254386353131L) < 0.58;
                     level.setBlock(
                        cursor,
                        TheSiftDimension.isCoralBiome(level.getBiome(cursor))
                           ? ModBlocks.SOULSHALE.defaultBlockState()
                           : (dryVein ? ModBlocks.DRY_HEALTHY_SCULK.defaultBlockState() : mainState),
                        2
                     );
                     placedBlocks.add(cursor.asLong());
                     placed++;
                  }
               }
            }
         }
      }

      return placed;
   }

   private static void carveGouges(
      WorldGenLevel level,
      RandomSource random,
      SiftArchFeature.Candidate candidate,
      int centerX,
      int centerZ,
      int archRise,
      double thickness,
      long salt,
      SiftPackedLongSet placedBlocks
   ) {
      int gouges = 3 + random.nextInt(5);
      double perpendicularX = -candidate.axisZ;
      double perpendicularZ = candidate.axisX;

      for (int i = 0; i < gouges; i++) {
         double t = 0.16 + random.nextDouble() * 0.68;
         SiftArchFeature.CurvePoint curve = curvePoint(candidate, centerX, centerZ, t, archRise, salt);
         double angle = -1.0995574287564276 + random.nextDouble() * Math.PI * 1.7;
         double surfaceOffset = thickness * (0.68 + random.nextDouble() * 0.22);
         double gougeX = curve.x + perpendicularX * Math.cos(angle) * surfaceOffset;
         double gougeY = curve.y + Math.sin(angle) * surfaceOffset;
         double gougeZ = curve.z + perpendicularZ * Math.cos(angle) * surfaceOffset;
         double radius = 0.82 + random.nextDouble() * 0.78;
         int limit = (int)Math.ceil(radius);
         int roundedX = (int)Math.round(gougeX);
         int roundedY = (int)Math.round(gougeY);
         int roundedZ = (int)Math.round(gougeZ);
         MutableBlockPos cursor = new MutableBlockPos();

         for (int dx = -limit; dx <= limit; dx++) {
            for (int dy = -limit; dy <= limit; dy++) {
               for (int dz = -limit; dz <= limit; dz++) {
                  if (!(dx * dx + dy * dy + dz * dz > radius * radius)) {
                     cursor.set(roundedX + dx, roundedY + dy, roundedZ + dz);
                     if (placedBlocks.remove(cursor.asLong()) && level.ensureCanWrite(cursor)) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isHealthy(BlockState state) {
      return state.is(ModBlocks.HEALTHY_SCULK) || state.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   private static double phase(long salt, int shift) {
      return (salt >>> shift & 65535L) / 65535.0 * Math.PI * 2.0;
   }

   private static double hash01(int x, int y, int z, long salt) {
      long value = salt ^ x * -7046029254386353131L;
      value ^= y * -4417276706812531889L;
      value ^= z * 1609587929392839161L;
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      value ^= value >>> 31;
      return (value >>> 11) * 1.110223E-16F;
   }

   private static double smootherstep(double value) {
      double t = Math.max(0.0, Math.min(1.0, value));
      return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
   }

   private record Candidate(double axisX, double axisZ, int halfSpan, int leftY, int rightY, int valleyDepth) {
      private BlockPos leftFloor(int centerX, int centerZ) {
         return new BlockPos(centerX - (int)Math.round(this.axisX * this.halfSpan), this.leftY - 1, centerZ - (int)Math.round(this.axisZ * this.halfSpan));
      }

      private BlockPos rightFloor(int centerX, int centerZ) {
         return new BlockPos(centerX + (int)Math.round(this.axisX * this.halfSpan), this.rightY - 1, centerZ + (int)Math.round(this.axisZ * this.halfSpan));
      }
   }

   private record CurvePoint(double x, double y, double z) {
   }
}
