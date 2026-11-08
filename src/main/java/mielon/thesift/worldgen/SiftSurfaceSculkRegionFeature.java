package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SiftSurfaceSculkRegionFeature extends Feature<NoneFeatureConfiguration> {
   private static final int CELL_SIZE = 416;
   private static final int MIN_DIAMETER = 50;
   private static final int MAX_DIAMETER = 120;
   private static final int MAX_RADIUS = 60;
   private static final int CENTER_MARGIN = 76;
   private static final int MIN_SCULK_DEPTH = 6;
   private static final int SCULK_DEPTH_VARIATION = 4;
   private static final long REGION_SALT = 5999732914757190983L;
   private static final long DARK_SNIFFER_SALT = 4918302751538956614L;

   public SiftSurfaceSculkRegionFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      int chunkMinX = context.origin().getX() & -16;
      int chunkMinZ = context.origin().getZ() & -16;
      int chunkMaxX = chunkMinX + 15;
      int chunkMaxZ = chunkMinZ + 15;
      int minCellX = Math.floorDiv(chunkMinX - 60, 416);
      int maxCellX = Math.floorDiv(chunkMaxX + 60, 416);
      int minCellZ = Math.floorDiv(chunkMinZ - 60, 416);
      int maxCellZ = Math.floorDiv(chunkMaxZ + 60, 416);
      boolean changed = false;

      for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
         for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
            SiftSurfaceSculkRegionFeature.SculkRegion region = createRegion(level.getSeed(), cellX, cellZ);
            if (region.intersects(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ)) {
               List<SiftSurfaceSculkRegionFeature.DarkSpawnPlan> darkSpawnPlans = new ArrayList<>();
               int darkSnifferCount = 1 + (int)(mix(region.seed() ^ 4918302751538956614L) & 1L);
               int firstDarkBlob = Math.floorMod(mix(region.seed() ^ 4918302751538956614L ^ 1848679592227504671L), region.blobs().length);
               int secondDarkBlob = Math.floorMod(mix(region.seed() ^ 4918302751538956614L ^ 8312979105109307661L), region.blobs().length - 1);
               if (secondDarkBlob >= firstDarkBlob) {
                  secondDarkBlob++;
               }

               for (int blobIndex = 0; blobIndex < region.blobs().length; blobIndex++) {
                  SiftSurfaceSculkRegionFeature.SculkBlob blob = region.blobs()[blobIndex];
                  boolean ownerChunk = Math.floorDiv((int)Math.floor(blob.centerX()), 16) == Math.floorDiv(chunkMinX, 16)
                     && Math.floorDiv((int)Math.floor(blob.centerZ()), 16) == Math.floorDiv(chunkMinZ, 16);
                  boolean selected = blobIndex == firstDarkBlob || darkSnifferCount == 2 && blobIndex == secondDarkBlob;
                  darkSpawnPlans.add(new SiftSurfaceSculkRegionFeature.DarkSpawnPlan(blob, new ArrayList<>(32), ownerChunk && selected));
               }

               for (int x = chunkMinX; x <= chunkMaxX; x++) {
                  for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                     int top = Math.min(region.maxY(), level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1);
                     int bottom = Math.max(40, region.minY());

                     for (int surfaceY = top; surfaceY >= bottom; surfaceY--) {
                        boolean core = region.contains(x, surfaceY, z);
                        boolean fringe = !core && region.isVeinFringe(x, surfaceY, z);
                        if (core || fringe) {
                           BlockPos surfacePos = new BlockPos(x, surfaceY, z);
                           if (!TheSiftDimension.isGrimSurfaceBiome(level.getBiome(surfacePos))
                              && !level.getBiome(surfacePos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)) {
                              BlockState surfaceState = level.getBlockState(surfacePos);
                              if (isNaturalSiftTerrain(surfaceState)) {
                                 BlockPos abovePos = surfacePos.above();
                                 BlockState aboveState = level.getBlockState(abovePos);
                                 if (aboveState.isAir() || isReplaceableSiftPlant(aboveState)) {
                                    if (isReplaceableSiftPlant(aboveState)) {
                                       clearUpperSnifferHalf(level, abovePos, aboveState);
                                       level.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
                                       aboveState = Blocks.AIR.defaultBlockState();
                                    }

                                    if (core) {
                                       boolean replaced = replaceSurface(level, surfacePos, region, x, z);
                                       changed |= replaced;
                                       if (aboveState.isAir()) {
                                          boolean decorated = placeSculkFamilyBlock(level, abovePos, region, x, z);
                                          changed |= decorated;
                                          if (replaced && !decorated && x > chunkMinX && x < chunkMaxX && z > chunkMinZ && z < chunkMaxZ) {
                                             for (SiftSurfaceSculkRegionFeature.DarkSpawnPlan plan : darkSpawnPlans) {
                                                if (plan.sites().size() < 64 && plan.blob().contains(x, surfaceY, z)) {
                                                   plan.sites().add(surfacePos.immutable());
                                                }
                                             }
                                          }
                                       }
                                    } else if (aboveState.isAir() && level.ensureCanWrite(abovePos)) {
                                       BlockState vein = Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
                                       level.setBlock(abovePos, vein, 2);
                                       changed = true;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               for (SiftSurfaceSculkRegionFeature.DarkSpawnPlan plan : darkSpawnPlans) {
                  if (!plan.sites().isEmpty()) {
                     if (!level.getBiome(plan.sites().get(0)).is(TheSiftDimension.CORRUPTED_SIFT)) {
                        SiftSculkBlobResidents.record(BlockPos.containing(region.centerX(), region.blobs()[0].centerY(), region.centerZ()));
                     }

                     if (plan.ambient() && level.getBiome(plan.sites().get(0)).is(TheSiftDimension.CORRUPTED_SIFT)) {
                        SiftDeferredSnifferSpawner.enqueueDark(level, plan.sites(), plan.blob().seed());
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean replaceSurface(WorldGenLevel level, BlockPos surfacePos, SiftSurfaceSculkRegionFeature.SculkRegion region, int x, int z) {
      int depth = 6 + Math.floorMod(mix(region.seed() ^ positionSalt(x, z)), 4);
      boolean changed = false;

      for (int offset = 0; offset < depth; offset++) {
         BlockPos target = surfacePos.below(offset);
         if (!level.ensureCanWrite(target) || !isNaturalSiftTerrain(level.getBlockState(target)) || !region.contains(x, target.getY(), z)) {
            break;
         }

         level.setBlock(target, Blocks.SCULK.defaultBlockState(), 2);
         changed = true;
      }

      return changed;
   }

   private static boolean placeSculkFamilyBlock(WorldGenLevel level, BlockPos pos, SiftSurfaceSculkRegionFeature.SculkRegion region, int x, int z) {
      double roll = unit(mix(region.seed() ^ positionSalt(x, z) ^ pos.getY() * 7146057691288625177L ^ 4922508400677442629L));
      BlockState decoration;
      if (roll < 0.0015) {
         decoration = Blocks.SCULK_CATALYST.defaultBlockState();
      } else if (roll < 0.0055) {
         decoration = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true);
      } else if (roll < 0.021) {
         decoration = Blocks.SCULK_SENSOR.defaultBlockState();
      } else {
         if (!(roll < 0.115) || !region.touchesBlobEdge(x, pos.getY() - 1, z)) {
            return false;
         }

         decoration = Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
      }

      if (!level.ensureCanWrite(pos)) {
         return false;
      }

      level.setBlock(pos, decoration, 2);
      return true;
   }

   private static boolean isNaturalSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SIFTSLATE)
         || state.is(ModBlocks.SIFTSLATE_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || state.is(Blocks.SCULK);
   }

   private static boolean isReplaceableSiftPlant(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.BLUE_SAUCER_BLOOM)
         || state.is(ModBlocks.GREEN_SAUCER_BLOOM)
         || state.is(ModBlocks.SUNBURST_PLANT)
         || state.is(ModBlocks.WHISPERBLOOM)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(Blocks.WILDFLOWERS)
         || state.is(Blocks.TORCHFLOWER)
         || state.is(Blocks.TORCHFLOWER_CROP)
         || state.is(Blocks.PITCHER_CROP)
         || state.is(Blocks.PITCHER_PLANT);
   }

   private static void clearUpperSnifferHalf(WorldGenLevel level, BlockPos lowerPos, BlockState lowerState) {
      if (lowerState.is(Blocks.PITCHER_CROP) || lowerState.is(Blocks.PITCHER_PLANT)) {
         BlockPos upperPos = lowerPos.above();
         BlockState upperState = level.getBlockState(upperPos);
         if ((upperState.is(Blocks.PITCHER_CROP) || upperState.is(Blocks.PITCHER_PLANT)) && level.ensureCanWrite(upperPos)) {
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 3);
         }
      }
   }

   private static SiftSurfaceSculkRegionFeature.SculkRegion createRegion(long worldSeed, int cellX, int cellZ) {
      long seed = mix(worldSeed ^ 5999732914757190983L ^ cellX * -7046029254386353131L ^ cellZ * -4417276706812531889L);
      RandomSource random = RandomSource.create(seed);
      int availableCenterRange = 264;
      double centerX = cellX * 416.0 + 76.0 + random.nextInt(availableCenterRange + 1);
      double centerZ = cellZ * 416.0 + 76.0 + random.nextInt(availableCenterRange + 1);
      int diameter = 50 + random.nextInt(71);
      double majorRadius = diameter * 0.5;
      double minorRadius = majorRadius * (0.72 + random.nextDouble() * 0.23);
      double angle = random.nextDouble() * Math.PI;
      int blobCount = 6 + random.nextInt(6);
      SiftSurfaceSculkRegionFeature.SculkBlob[] blobs = new SiftSurfaceSculkRegionFeature.SculkBlob[blobCount];
      double storey = unit(mix(seed ^ 8314618445576101681L));
      int baseY = storey < 0.8 ? 102 : (storey < 0.95 ? 70 : 138);

      for (int index = 0; index < blobCount; index++) {
         double direction = random.nextDouble() * Math.PI * 2.0;
         double distance = index == 0 ? 0.0 : Math.sqrt(random.nextDouble()) * majorRadius * 0.68;
         double localX = Math.cos(direction) * distance;
         double localZ = Math.sin(direction) * distance * (minorRadius / majorRadius);
         double cosRegion = Math.cos(angle);
         double sinRegion = Math.sin(angle);
         double blobCenterX = centerX + localX * cosRegion - localZ * sinRegion;
         double blobCenterZ = centerZ + localX * sinRegion + localZ * cosRegion;
         double blobRadius = majorRadius * (0.18 + random.nextDouble() * 0.18);
         double blobAspect = 0.62 + random.nextDouble() * 0.36;
         double blobAngle = random.nextDouble() * Math.PI;
         long blobSeed = random.nextLong();
         blobs[index] = new SiftSurfaceSculkRegionFeature.SculkBlob(
            blobCenterX,
            blobCenterZ,
            blobRadius,
            blobRadius * blobAspect,
            Math.cos(blobAngle),
            Math.sin(blobAngle),
            blobSeed,
            baseY + (unit(mix(blobSeed ^ 8314618445576101682L)) - 0.5) * 14.0,
            18.0 + unit(mix(blobSeed ^ 8314618445576101683L)) * 6.0
         );
      }

      return new SiftSurfaceSculkRegionFeature.SculkRegion(
         centerX, centerZ, majorRadius, minorRadius, Math.cos(angle), Math.sin(angle), seed, blobs, baseY - 32, baseY + 32
      );
   }

   private static long positionSalt(int x, int z) {
      return x * 7146057691288625177L ^ z * -7046029288634856825L;
   }

   private static long mix(long value) {
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      return value ^ value >>> 31;
   }

   private static double unit(long value) {
      return (value >>> 11) * 1.110223E-16F;
   }

   static Predicate<BlockPos> blobShape(long worldSeed, BlockPos center) {
      int cx = Math.floorDiv(center.getX(), 416);
      int cz = Math.floorDiv(center.getZ(), 416);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            SiftSurfaceSculkRegionFeature.SculkRegion region = createRegion(worldSeed, cx + dx, cz + dz);

            for (SiftSurfaceSculkRegionFeature.SculkBlob blob : region.blobs()) {
               if (BlockPos.containing(blob.centerX(), blob.centerY(), blob.centerZ()).equals(center)) {
                  return floor -> region.contains(floor.getX(), floor.getY(), floor.getZ());
               }
            }
         }
      }

      return floor -> false;
   }

   static BlockPos residentCenter(long worldSeed, BlockPos point) {
      int cx = Math.floorDiv(point.getX(), 416);
      int cz = Math.floorDiv(point.getZ(), 416);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            SiftSurfaceSculkRegionFeature.SculkRegion region = createRegion(worldSeed, cx + dx, cz + dz);

            for (SiftSurfaceSculkRegionFeature.SculkBlob blob : region.blobs()) {
               if (BlockPos.containing(blob.centerX(), blob.centerY(), blob.centerZ()).equals(point)) {
                  return BlockPos.containing(region.centerX(), region.blobs()[0].centerY(), region.centerZ());
               }
            }
         }
      }

      return point;
   }

   private record DarkSpawnPlan(SiftSurfaceSculkRegionFeature.SculkBlob blob, List<BlockPos> sites, boolean ambient) {
   }

   private record SculkBlob(double centerX, double centerZ, double radiusX, double radiusZ, double cos, double sin, long seed, double centerY, double radiusY) {
      boolean contains(int x, int y, int z) {
         double dx = x + 0.5 - this.centerX;
         double dz = z + 0.5 - this.centerZ;
         double localX = dx * this.cos + dz * this.sin;
         double localZ = -dx * this.sin + dz * this.cos;
         double ellipse = localX * localX / (this.radiusX * this.radiusX) + localZ * localZ / (this.radiusZ * this.radiusZ);
         double dy = y + 0.5 - this.centerY;
         ellipse += dy * dy / (this.radiusY * this.radiusY);
         double edgeNoise = 0.11
            * (
               0.4 * SiftTerrainShapeNoise.noise(this.seed, x, 6.0)
                  + 0.4 * SiftTerrainShapeNoise.noise(this.seed ^ 8897329L, z, 6.0)
                  + 0.2 * SiftTerrainShapeNoise.noise(this.seed ^ 10834417L, y, 6.0)
            );
         return ellipse <= 1.0 + edgeNoise;
      }
   }

   private record SculkRegion(
      double centerX,
      double centerZ,
      double majorRadius,
      double minorRadius,
      double cos,
      double sin,
      long seed,
      SiftSurfaceSculkRegionFeature.SculkBlob[] blobs,
      int minY,
      int maxY
   ) {
      boolean intersects(int minX, int minZ, int maxX, int maxZ) {
         return this.centerX + this.majorRadius >= minX
            && this.centerX - this.majorRadius <= maxX
            && this.centerZ + this.majorRadius >= minZ
            && this.centerZ - this.majorRadius <= maxZ;
      }

      boolean contains(int x, int y, int z) {
         if (!this.insideEnvelope(x, z)) {
            return false;
         }

         for (SiftSurfaceSculkRegionFeature.SculkBlob blob : this.blobs) {
            if (blob.contains(x, y, z)) {
               return true;
            }
         }

         return false;
      }

      boolean isVeinFringe(int x, int y, int z) {
         return !this.insideEnvelope(x, z)
            ? false
            : this.contains(x + 1, y, z)
               || this.contains(x - 1, y, z)
               || this.contains(x, y, z + 1)
               || this.contains(x, y, z - 1)
               || this.contains(x + 2, y, z)
               || this.contains(x - 2, y, z)
               || this.contains(x, y, z + 2)
               || this.contains(x, y, z - 2);
      }

      boolean touchesBlobEdge(int x, int y, int z) {
         return !this.contains(x + 1, y, z) || !this.contains(x - 1, y, z) || !this.contains(x, y, z + 1) || !this.contains(x, y, z - 1);
      }

      private boolean insideEnvelope(int x, int z) {
         double dx = x + 0.5 - this.centerX;
         double dz = z + 0.5 - this.centerZ;
         double localX = dx * this.cos + dz * this.sin;
         double localZ = -dx * this.sin + dz * this.cos;
         double ellipse = localX * localX / (this.majorRadius * this.majorRadius) + localZ * localZ / (this.minorRadius * this.minorRadius);
         return ellipse <= 1.0;
      }
   }
}
