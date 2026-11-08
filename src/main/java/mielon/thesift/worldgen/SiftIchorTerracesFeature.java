package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftIchorTerracesFeature implements Feature {
   private static final int RAVINE_WATER_Y = 44;
   public static final MapCodec<SiftIchorTerracesFeature> CODEC = MapCodec.unit(SiftIchorTerracesFeature::new);

   public MapCodec<SiftIchorTerracesFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      boolean changed = fillRavines(level, origin);
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;

      for (int attempt = 0; attempt < 22; attempt++) {
         int x = minX + 5 + random.nextInt(6);
         int z = minZ + 5 + random.nextInt(6);
         int top = level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
         int start = Math.min(70, top - 12);
         int y = findFloor(level, x, z, start, 14, 9);
         if (y >= 14
            && !SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(x, y, z), 6)
            && !SiftFeaturePlacementGuard.intersectsReservedLandmark(new BlockPos(x, y, z), 2)
            && (attempt >= 10 || !Double.isNaN(wallDirection(level, SiftWorldgenBounds.aroundWithNeighbourMargin(origin), new BlockPos(x, y, z))))
            && placeTerraces(level, random, new BlockPos(x, y, z), origin)) {
            return true;
         }
      }

      return changed;
   }

   static boolean fillRavines(WorldGenLevel level, BlockPos origin) {
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      boolean changed = false;
      SiftIchorRavineShape shape = new SiftIchorRavineShape(level.getSeed());
      boolean intersectsChannel = false;

      for (int x = minX; x < minX + 16 && !intersectsChannel; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            if (shape.sampleWithLining(x, z) != null) {
               intersectsChannel = true;
               break;
            }
         }
      }

      if (!intersectsChannel) {
         return false;
      }

      final SiftStrataSupportShape.Terrain baseTerrain = SiftStrataSupportFeature.terrainView(level, SiftWorldgenBounds.aroundWithNeighbourMargin(origin));
      SiftStrataSupportShape.Terrain terrain = new SiftStrataSupportShape.Terrain() {
         @Override
         public boolean ground(int x, int y, int z) {
            return baseTerrain.ground(x, y, z);
         }

         @Override
         public boolean air(int x, int y, int z) {
            return baseTerrain.air(x, y, z);
         }

         @Override
         public int surfaceY(int x, int z) {
            return baseTerrain.surfaceY(x, z);
         }

         @Override
         public boolean canSample(int x, int z) {
            return baseTerrain.canSample(x, z);
         }

         @Override
         public boolean protectedRock(int x, int y, int z) {
            return baseTerrain.protectedRock(x, y, z) || SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z);
         }
      };
      BlockState ichor = ModFluids.ICHOR_BLOCK.defaultBlockState();
      BlockState rock = ModBlocks.SIFTSLATE.defaultBlockState();
      BlockState air = Blocks.AIR.defaultBlockState();
      SiftIchorRavineShape.BankSampler bankSampler = new SiftIchorRavineShape.BankSampler(terrain);
      int[][] banks = new int[32][32];

      for (int dx = 0; dx < 32; dx++) {
         for (int dz = 0; dz < 32; dz++) {
            int x = minX + dx - 8;
            int z = minZ + dz - 8;
            int known = SiftTerrainFeatureMemory.ravineBank(x, z);
            banks[dx][dz] = known != Integer.MIN_VALUE && !terrain.protectedRock(x, known - 1, z) ? known : bankSampler.find(x, z);
         }
      }

      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            SiftIchorRavineShape.Cut cut = shape.sampleWithLining(x, z);
            if (cut != null) {
               int bx = x - minX + 8;
               int bz = z - minZ + 8;
               int bank = banks[bx][bz];
               if (bank >= 49 && !SiftTerrainFeatureMemory.isProtectedTerrain(x, bank, z)) {
                  double clearance = SiftFeaturePlacementGuard.clearance(x, z, 8.0);

                  for (int dx = -8; dx <= 8; dx++) {
                     for (int dz = -8; dz <= 8; dz++) {
                        double distance = Math.hypot(dx, dz);
                        if (distance < clearance && banks[bx + dx][bz + dz] < 49) {
                           clearance = distance;
                        }
                     }
                  }

                  double t = Math.max(0.0, Math.min(1.0, (clearance - 1.0) / 7.0));
                  int floor = bank - (int)Math.floor((bank - cut.floorY(bank)) * t * t * (3.0 - 2.0 * t));
                  if (floor >= bank) {
                     if (clearance > 1.0 && cut.distance() >= cut.radius()) {
                        for (int y = 41; y <= 45; y++) {
                           cursor.set(x, y, z);
                           BlockState state = level.getBlockState(cursor);
                           if (!SiftLandmarkTerrain.isGround(state) && SiftLandmarkTerrain.canReplace(state) && level.ensureCanWrite(cursor)) {
                              level.setBlock(cursor, rock, 2);
                           }
                        }
                     }
                  } else {
                     if (floor < bank) {
                        SiftTerrainFeatureMemory.ravine(x, z, floor, bank);
                     }

                     for (int y = Math.max(14, floor - 3); y <= floor; y++) {
                        cursor.set(x, y, z);
                        if (level.ensureCanWrite(cursor) && !SiftLandmarkTerrain.isGround(level.getBlockState(cursor))) {
                           level.setBlock(cursor, rock, 2);
                        }
                     }

                     for (int y = floor + 1; y <= bank; y++) {
                        cursor.set(x, y, z);
                        BlockState state = level.getBlockState(cursor);
                        if (SiftLandmarkTerrain.canReplace(state) && level.ensureCanWrite(cursor)) {
                           level.setBlock(cursor, y <= 44 ? ichor : air, 2);
                           changed = true;
                        }
                     }

                     if (floor < bank) {
                        for (int y = bank + 1; y <= bank + 3; y++) {
                           cursor.set(x, y, z);
                           BlockState state = level.getBlockState(cursor);
                           if (SiftLandmarkTerrain.canReplace(state)
                              && !SiftLandmarkTerrain.isGround(state)
                              && state.getFluidState().isEmpty()
                              && level.ensureCanWrite(cursor)) {
                              level.setBlock(cursor, air, 2);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return changed;
   }

   private static boolean placeTerraces(WorldGenLevel level, RandomSource random, BlockPos center, BlockPos origin) {
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
      long salt = random.nextLong();
      double wallAngle = wallDirection(level, bounds, center);
      boolean attached = !Double.isNaN(wallAngle);
      double angle = attached ? wallAngle : random.nextDouble() * Math.PI * 2.0;
      double dx = Math.cos(angle);
      double dz = Math.sin(angle);
      List<SiftIchorTerracesFeature.Pool> pools = new ArrayList<>();
      if (attached) {
         pools.add(pool(center, dx, dz, 3.0, 0.0, 5, 3.8, salt, 1));
         pools.add(pool(center, dx, dz, -2.0, -3.5, 3, 4.0, salt ^ 1818L, 3));
         pools.add(pool(center, dx, dz, -2.0, 3.5, 2, 3.9, salt ^ 2331L, 4));
         pools.add(pool(center, dx, dz, -7.0, -4.0, 1, 3.7, salt ^ 5004L, -1));
         pools.add(pool(center, dx, dz, -7.0, 3.5, 0, 3.8, salt ^ 6429L, -1));
      } else {
         pools.add(pool(center, dx, dz, 0.0, 0.0, 6, 3.7 + random.nextDouble() * 0.4, salt, 1));

         for (int side = 0; side < 4; side++) {
            double a = angle + side * Math.PI / 2.0 + (random.nextDouble() - 0.5) * 0.25;
            pools.add(
               pool(
                  center,
                  Math.cos(a),
                  Math.sin(a),
                  5.8 + random.nextDouble() * 0.6,
                  0.0,
                  new int[]{3, 4, 2, 3}[side],
                  3.5 + random.nextDouble() * 0.6,
                  salt ^ 5004L * (side + 1),
                  5 + side
               )
            );
         }

         for (int side = 0; side < 4; side++) {
            double a = angle + side * Math.PI / 2.0 + 0.2 + (random.nextDouble() - 0.5) * 0.3;
            pools.add(
               pool(
                  center,
                  Math.cos(a),
                  Math.sin(a),
                  10.0 + random.nextDouble() * 0.8,
                  0.0,
                  side == 1 ? 1 : 0,
                  3.2 + random.nextDouble() * 0.6,
                  salt ^ 6429L * (side + 1),
                  -1
               )
            );
         }
      }

      Map<Long, SiftIchorTerracesFeature.PoolColumn> columns = new LinkedHashMap<>();

      for (SiftIchorTerracesFeature.Pool pool : pools) {
         int radius = (int)Math.ceil(pool.radius() + 5.0);

         for (int ox = -radius; ox <= radius; ox++) {
            for (int oz = -radius; oz <= radius; oz++) {
               double distance = Math.sqrt(poolDistance(ox, oz, pool)) * pool.radius();
               if (!(distance > pool.radius() + 5.0)) {
                  int x = pool.x() + ox;
                  int z = pool.z() + oz;
                  if (!bounds.contains(x, z)) {
                     return false;
                  }

                  int floor = findFloor(level, x, z, center.getY() + 8, center.getY() - 5, 3);
                  boolean wall = attached && solidWall(level, x, center.getY(), z);
                  if (wall) {
                     floor = center.getY();
                  }

                  if (floor == Integer.MIN_VALUE || !supported(level, x, floor, z)) {
                     return false;
                  }

                  boolean core = distance <= pool.radius();
                  boolean water = core && distance < pool.radius() - 1.1;
                  int top;
                  if (core) {
                     top = pool.waterY() + (water ? 0 : 1);
                  } else {
                     double t = (distance - pool.radius()) / 5.0;
                     double falloff = 1.0 - t * t * (3.0 - 2.0 * t);
                     top = floor + (int)Math.round(Math.max(0, pool.waterY() + 1 - floor) * falloff);
                  }

                  if (floor > top + 2 && !wall && core) {
                     return false;
                  }

                  if (core || top > floor) {
                     int bed = pool.waterY() - 2;
                     if (water && wall) {
                        water = false;
                     }

                     long key = BlockPos.asLong(x, 0, z);
                     SiftIchorTerracesFeature.PoolColumn old = columns.get(key);
                     SiftIchorTerracesFeature.PoolColumn column = new SiftIchorTerracesFeature.PoolColumn(x, z, floor, bed, top, water, core, wall);
                     if (old == null || core && !old.core() || core == old.core() && top > old.top()) {
                        columns.put(key, column);
                     }
                  }
               }
            }
         }
      }

      for (SiftIchorTerracesFeature.Pool pool : pools) {
         long visible = columns.values()
            .stream()
            .filter(c -> c.water() && c.top() == pool.waterY() && Math.hypot(c.x() - pool.x(), c.z() - pool.z()) < pool.radius())
            .count();
         if (visible < 7L) {
            return false;
         }
      }

      int minX = columns.values().stream().mapToInt(SiftIchorTerracesFeature.PoolColumn::x).min().orElseThrow();
      int minZ = columns.values().stream().mapToInt(SiftIchorTerracesFeature.PoolColumn::z).min().orElseThrow();
      int maxX = columns.values().stream().mapToInt(SiftIchorTerracesFeature.PoolColumn::x).max().orElseThrow();
      int maxZ = columns.values().stream().mapToInt(SiftIchorTerracesFeature.PoolColumn::z).max().orElseThrow();

      for (SiftIchorTerracesFeature.PoolColumn column : columns.values()) {
         for (int y = Math.min(column.floor() - 1, column.bed() - 1); y <= Math.max(column.floor(), column.top()) + 3; y++) {
            BlockPos pos = new BlockPos(column.x(), y, column.z());
            BlockState state = level.getBlockState(pos);
            if (!level.ensureCanWrite(pos)
               || !SiftLandmarkTerrain.canReplace(state)
               || !state.getFluidState().isEmpty()
               || SiftTerrainFeatureMemory.isSupport(column.x(), y, column.z())) {
               return false;
            }

            if (!column.wall() && y > Math.max(column.floor(), column.top()) && SiftLandmarkTerrain.isGround(state)) {
               return false;
            }
         }
      }

      if (!SiftFeaturePlacementGuard.tryReserveCarver(minX, minZ, maxX, maxZ)) {
         return false;
      }

      BlockState rock = ModBlocks.SIFTSLATE.defaultBlockState();
      boolean overgrown = TheSiftDimension.isOvergrownBiome(level.getBiome(center));
      BlockState rim = (overgrown ? ModBlocks.HEALTHY_SCULK : ModBlocks.DRY_HEALTHY_SCULK).defaultBlockState();
      BlockState ichor = ModFluids.ICHOR_BLOCK.defaultBlockState();
      BlockState air = Blocks.AIR.defaultBlockState();
      List<BlockPos> shores = new ArrayList<>();

      for (SiftIchorTerracesFeature.PoolColumn column : columns.values()) {
         SiftTerrainFeatureMemory.excludeRibs(column.x(), column.z(), column.floor() - 3, column.top() + 3);
         SiftTerrainFeatureMemory.protectTerrain(column.x(), column.z(), column.floor() - 3, column.top() + 3);
         if (!column.wall()) {
            for (int y = column.top() + 1; y <= Math.max(column.floor() + 2, column.top() + 3); y++) {
               level.setBlock(new BlockPos(column.x(), y, column.z()), air, 2);
            }
         }

         for (int y = Math.min(column.floor() - 1, column.bed()); y <= column.top(); y++) {
            BlockPos pos = new BlockPos(column.x(), y, column.z());
            if (!column.wall() || !SiftLandmarkTerrain.isGround(level.getBlockState(pos))) {
               BlockState state = column.water() && y > column.bed() ? ichor : (y == column.top() ? rim : rock);
               level.setBlock(pos, state, 2);
            }
         }

         if (column.core() && !column.water() && !column.wall()) {
            shores.add(new BlockPos(column.x(), column.top(), column.z()));
         }
      }

      for (SiftIchorTerracesFeature.Pool pool : pools) {
         if (pool.downstream() >= 0) {
            SiftIchorTerracesFeature.Pool lower = pools.get(pool.downstream());
            double span = Math.hypot(lower.x() - pool.x(), lower.z() - pool.z());

            for (int step = Math.max(1, (int)pool.radius() - 2); step <= Math.ceil(pool.radius()); step++) {
               BlockPos spill = new BlockPos(
                  pool.x() + (int)Math.round((lower.x() - pool.x()) * step / span),
                  pool.waterY(),
                  pool.z() + (int)Math.round((lower.z() - pool.z()) * step / span)
               );
               SiftIchorTerracesFeature.PoolColumn support = columns.get(BlockPos.asLong(spill.getX(), 0, spill.getZ()));
               if (support != null && !support.wall() && support.top() <= pool.waterY() + 1 && level.ensureCanWrite(spill)) {
                  level.setBlock(spill.above(), air, 2);
                  level.setBlock(spill, ichor, 2);
                  level.scheduleTick(spill, ichor.getFluidState().getType(), 5);
               }
            }
         }
      }

      SiftSurfaceDecorator.decorateLakeShore(level, random, shores, 2);
      return true;
   }

   private static boolean solidWall(WorldGenLevel level, int x, int y, int z) {
      for (int dy = -2; dy <= 8; dy++) {
         if (!SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(x, y + dy, z))) || SiftTerrainFeatureMemory.isSupport(x, y + dy, z)) {
            return false;
         }
      }

      return true;
   }

   private static double wallDirection(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos center) {
      for (int distance = 5; distance <= 10; distance++) {
         for (int direction = 0; direction < 16; direction++) {
            double angle = direction * Math.PI / 8.0;
            int x = center.getX() + (int)Math.round(Math.cos(angle) * distance);
            int z = center.getZ() + (int)Math.round(Math.sin(angle) * distance);
            if (bounds.contains(x, z) && solidWall(level, x, center.getY(), z)) {
               return angle;
            }
         }
      }

      return Double.NaN;
   }

   private static SiftIchorTerracesFeature.Pool pool(
      BlockPos center, double dx, double dz, double along, double side, int height, double radius, long salt, int downstream
   ) {
      return new SiftIchorTerracesFeature.Pool(
         center.getX() + (int)Math.round(dx * along - dz * side),
         center.getZ() + (int)Math.round(dz * along + dx * side),
         center.getY() + height,
         radius,
         salt,
         downstream
      );
   }

   private static double poolDistance(int x, int z, SiftIchorTerracesFeature.Pool pool) {
      double angle = Math.atan2(z, x);
      double phase = SiftTerrainShapeNoise.unit(pool.salt()) * Math.PI * 2.0;
      double edge = 1.0 + 0.07 * Math.sin(angle * 5.0 + phase) + 0.035 * Math.sin(angle * 9.0 - phase);
      return (x * x + z * z * 1.08) / (pool.radius() * pool.radius() * edge * edge);
   }

   private static int findFloor(WorldGenLevel level, int x, int z, int from, int to, int headroom) {
      for (int y = from; y >= to; y--) {
         BlockPos floor = new BlockPos(x, y, z);
         if (SiftLandmarkTerrain.isGround(level.getBlockState(floor))) {
            boolean clear = true;

            for (int above = 1; above <= headroom; above++) {
               BlockState state = level.getBlockState(floor.above(above));
               if (SiftLandmarkTerrain.isGround(state) || !state.getFluidState().isEmpty() || !SiftLandmarkTerrain.canReplace(state)) {
                  clear = false;
                  break;
               }
            }

            if (clear) {
               return y;
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private static boolean supported(WorldGenLevel level, int x, int y, int z) {
      for (int depth = 0; depth < 3; depth++) {
         if (!SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(x, y - depth, z)))) {
            return false;
         }
      }

      return true;
   }

   private record Pool(int x, int z, int waterY, double radius, long salt, int downstream) {
   }

   private record PoolColumn(int x, int z, int floor, int bed, int top, boolean water, boolean core, boolean wall) {
   }
}
