package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SiftSurfaceTransition {
   public static final String REVISION_TAG = "the_sift:surface_transition";
   public static final int REVISION = 2;
   private static final double RANGE = 112.0;
   private static final int STRIDE = 5;

   private SiftSurfaceTransition() {
   }

   public static double[] profile(WorldGenRegion region, ChunkAccess chunk) {
      SiftChunkGenerationAccess data = SiftTerrainUpgrade.data(chunk);
      double[] cached = data.theSift$edgeProfile();
      if (cached != null) {
         return cached;
      }

      int cx = chunk.getPos().x();
      int cz = chunk.getPos().z();
      List<ChunkAccess> old = new ArrayList<>();

      for (int dz = -7; dz <= 7; dz++) {
         for (int dx = -7; dx <= 7; dx++) {
            if (region.hasChunk(cx + dx, cz + dz)) {
               ChunkAccess candidate = region.getChunk(cx + dx, cz + dz);
               if (SiftTerrainUpgrade.legacy(candidate) && candidate.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)) {
                  old.add(candidate);
               }
            }
         }
      }

      if (old.isEmpty()) {
         data.theSift$edgeProfile(new double[0]);
         return data.theSift$edgeProfile();
      }

      double[] result = new double[1280];
      Map<Long, Integer> heights = new HashMap<>();
      List<SiftSurfaceTransition.Sample> samples = new ArrayList<>();
      Map<Long, Boolean> sampled = new HashMap<>();

      for (ChunkAccess source : old) {
         for (int[] face : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            int sx = source.getPos().getMinBlockX() >> 4;
            int sz = source.getPos().getMinBlockZ() >> 4;
            if (region.hasChunk(sx + face[0], sz + face[1]) && !SiftTerrainUpgrade.legacy(region.getChunk(sx + face[0], sz + face[1]))) {
               for (int step = 0; step < 16; step++) {
                  int px = source.getPos().getMinBlockX() + (face[0] < 0 ? 0 : (face[0] > 0 ? 15 : step));
                  int pz = source.getPos().getMinBlockZ() + (face[1] < 0 ? 0 : (face[1] > 0 ? 15 : step));
                  if (sampled.putIfAbsent(key(px, pz), Boolean.TRUE) == null) {
                     samples.add(new SiftSurfaceTransition.Sample(px, pz, height(source, px, pz, heights)));
                  }
               }
            }
         }
      }

      for (int z = 0; z < 16; z++) {
         for (int x = 0; x < 16; x++) {
            int wx = (cx << 4) + x;
            int wz = (cz << 4) + z;
            int index = (z * 16 + x) * 5;
            ChunkAccess nearest = null;
            int qx = 0;
            int qz = 0;
            double best = Double.POSITIVE_INFINITY;

            for (ChunkAccess candidate : old) {
               int nx = Math.max(candidate.getPos().getMinBlockX(), Math.min(candidate.getPos().getMinBlockX() + 15, wx));
               int nz = Math.max(candidate.getPos().getMinBlockZ(), Math.min(candidate.getPos().getMinBlockZ() + 15, wz));
               double d = (double)(wx - nx) * (wx - nx) + (double)(wz - nz) * (wz - nz);
               if (d < best) {
                  best = d;
                  nearest = candidate;
                  qx = nx;
                  qz = nz;
               }
            }

            double length = Math.sqrt(best);
            double distance = Math.max(0.0, length - 1.0);
            int height = height(nearest, qx, qz, heights);
            double weighted = 0.0;
            double total = 0.0;

            for (SiftSurfaceTransition.Sample sample : samples) {
               double r2 = (double)(wx - sample.x()) * (wx - sample.x()) + (double)(wz - sample.z()) * (wz - sample.z());
               if (!(r2 > 12544.0)) {
                  double falloff = 1.0 - r2 / 12544.0;
                  double weight = falloff * falloff / (r2 * r2);
                  weighted += sample.height() * weight;
                  total += weight;
               }
            }

            double broad = !(distance < 1.0E-6) && total != 0.0 ? weighted / total : height;
            result[index] = height;
            result[index + 1] = distance;
            result[index + 2] = qx;
            result[index + 3] = qz;
            result[index + 4] = broad;
         }
      }

      data.theSift$edgeProfile(result);
      return result;
   }

   private static int height(ChunkAccess chunk, int x, int z, Map<Long, Integer> cache) {
      return cache.computeIfAbsent(key(x, z), k -> SiftTerrainUpgrade.surface(chunk, x, z));
   }

   private static long key(int x, int z) {
      return (long)x << 32 ^ z & 4294967295L;
   }

   private static double smooth(double t) {
      t = Math.max(0.0, Math.min(1.0, t));
      return t * t * (3.0 - 2.0 * t);
   }

   private static double cubic(double a, double b, double c, double d, double t) {
      return b + 0.5 * t * (c - a + t * (2.0 * a - 5.0 * b + 4.0 * c - d + t * (3.0 * (b - c) + d - a)));
   }

   private static double rawSurface(WorldGenRegion region, NoiseBasedChunkGenerator generator, int x, int z, Map<Long, Integer> cache) {
      int gx = Math.floorDiv(x, 16);
      int gz = Math.floorDiv(z, 16);
      double tx = (x - gx * 16) / 16.0;
      double tz = (z - gz * 16) / 16.0;
      double[] rows = new double[4];

      for (int dz = -1; dz <= 2; dz++) {
         double[] values = new double[4];

         for (int dx = -1; dx <= 2; dx++) {
            int px = (gx + dx) * 16;
            int pz = (gz + dz) * 16;
            values[dx + 1] = cache.computeIfAbsent(
                  key(px, pz),
                  k -> generator.getBaseHeight(px, pz, Types.WORLD_SURFACE_WG, region.getLevel(), region.getLevel().getChunkSource().randomState()) - 1
               )
               .intValue();
         }

         rows[dz + 1] = cubic(values[0], values[1], values[2], values[3], tx);
      }

      return Math.max(8.0, Math.min(250.0, cubic(rows[0], rows[1], rows[2], rows[3], tz)));
   }

   public static void apply(WorldGenRegion region, ChunkAccess chunk, ChunkGenerator generator) {
      if (region.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)
         && !SiftTerrainUpgrade.legacy(chunk)
         && generator instanceof NoiseBasedChunkGenerator noise
         && SiftTerrainUpgrade.data(chunk).theSift$surfaceRevision() <= 0
         && !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)
         && chunk.getBlockEntitiesPos().isEmpty()) {
         double[] edge = profile(region, chunk);
         if (edge.length != 0) {
            Map<Long, Integer> raw = new HashMap<>();
            MutableBlockPos pos = new MutableBlockPos();
            BlockState[] original = new BlockState[256];

            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  int index = (z * 16 + x) * 5;
                  int wx = chunk.getPos().getMinBlockX() + x;
                  int wz = chunk.getPos().getMinBlockZ() + z;
                  double distance = edge[index + 1];
                  if (!(distance >= 112.0)) {
                     int current = SiftTerrainUpgrade.surface(chunk, wx, wz);
                     double t = distance / 112.0;
                     double desired = edge[index + 4] + (rawSurface(region, noise, wx, wz, raw) - edge[index + 4]) * smooth(t);
                     double correction = 1.0 - smooth((distance - 40.0) / 72.0);
                     int target = (int)Math.round(Math.max(8.0, Math.min(250.0, current + (desired - current) * correction)));
                     int delta = target - current;
                     if (current > 4) {
                        int bottom = Math.max(4, Math.min(current, target) - Math.max(24, Math.abs(delta) * 2));

                        for (int y = 0; y < 256; y++) {
                           original[y] = chunk.getBlockState(pos.set(wx, y, wz));
                        }

                        for (int y = (distance < 1.0E-6 ? 4 : bottom) + 1; y < 256; y++) {
                           double source = y <= bottom
                              ? y
                              : (y <= target ? bottom + (double)((y - bottom) * (current - bottom)) / (target - bottom) : y - delta);
                           int sourceY = (int)Math.round(source);
                           BlockState state = sourceY >= 0 && sourceY < 256 ? original[sourceY] : Blocks.AIR.defaultBlockState();
                           if (distance < 1.0E-6) {
                              ChunkAccess sourceChunk = region.getChunk((int)edge[index + 2] >> 4, (int)edge[index + 3] >> 4);
                              BlockState old = sourceChunk.getBlockState(new BlockPos((int)edge[index + 2], y, (int)edge[index + 3]));
                              if (SiftTerrainUpgrade.isTerrain(old) && !SiftTerrainUpgrade.isTerrain(state)) {
                                 state = noise.generatorSettings().value().defaultBlock();
                              } else if ((old.isAir() || !old.getFluidState().isEmpty()) && SiftTerrainUpgrade.isTerrain(state)) {
                                 state = old.getFluidState().isEmpty() ? Blocks.AIR.defaultBlockState() : old.getFluidState().createLegacyBlock();
                              }
                           }

                           if (state != original[y]) {
                              chunk.setBlockState(pos.set(wx, y, wz), state, 0);
                           }
                        }
                     }
                  }
               }
            }

            EnumSet<Types> maps = EnumSet.of(Types.WORLD_SURFACE_WG, Types.OCEAN_FLOOR_WG);

            for (Types type : maps) {
               chunk.setHeightmap(type, new long[chunk.getOrCreateHeightmapUnprimed(type).getRawData().length]);
            }

            Heightmap.primeHeightmaps(chunk, maps);
            SiftTerrainUpgrade.data(chunk).theSift$surfaceRevision(2);
         }
      }
   }

   public static boolean protects(WorldGenRegion region, ChunkAccess chunk, BlockPos pos, boolean removesGround) {
      double[] edge = profile(region, chunk);
      if (edge.length == 0) {
         return false;
      }

      int index = ((pos.getZ() & 15) * 16 + (pos.getX() & 15)) * 5;
      double d = edge[index + 1];
      if (d >= 48.0) {
         return false;
      }

      if (d < 1.0E-6) {
         ChunkAccess source = region.getChunk((int)edge[index + 2] >> 4, (int)edge[index + 3] >> 4);
         BlockState old = source.getBlockState(new BlockPos((int)edge[index + 2], pos.getY(), (int)edge[index + 3]));
         if (removesGround && SiftTerrainUpgrade.isTerrain(old)) {
            return true;
         }

         if (!removesGround && (old.isAir() || !old.getFluidState().isEmpty())) {
            return true;
         }
      }

      double anchor = edge[index + 4];
      double depth = d + 0.15 * d * d;
      return removesGround ? pos.getY() <= anchor - depth : pos.getY() > anchor + depth;
   }

   private record Sample(int x, int z, int height) {
   }
}
