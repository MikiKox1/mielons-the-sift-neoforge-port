package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.block.ModBlocks;
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

public final class SiftStrataSupportFeature implements Feature {
   public static final MapCodec<SiftStrataSupportFeature> CODEC = MapCodec.unit(SiftStrataSupportFeature::new);

   public MapCodec<SiftStrataSupportFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
      SiftStrataSupportShape.Terrain terrain = terrainView(level, bounds);
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      boolean changed = false;

      for (SiftStrataSupportShape.Voxel fragment : SiftStrataSupportShape.floatingFragments(terrain, minX, minZ)) {
         BlockPos pos = new BlockPos(fragment.x(), fragment.y(), fragment.z());
         if (level.ensureCanWrite(pos) && !SiftFeaturePlacementGuard.intersectsPortal(pos, 2)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            changed = true;
         }
      }

      for (int cellX = Math.floorDiv(minX, 24) - 1; cellX <= Math.floorDiv(minX + 15, 24) + 1; cellX++) {
         for (int cellZ = Math.floorDiv(minZ, 24) - 1; cellZ <= Math.floorDiv(minZ + 15, 24) + 1; cellZ++) {
            long salt = SiftStrataSupportShape.salt(level.getSeed(), cellX, cellZ);
            int x = SiftStrataSupportShape.anchorX(salt, cellX);
            int z = SiftStrataSupportShape.anchorZ(salt, cellZ);
            if (x >= minX && x <= minX + 15 && z >= minZ && z <= minZ + 15 && !SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(x, 0, z), 16)) {
               SiftStrataSupportShape.Support support = SiftStrataSupportShape.findSupport(terrain, x, z);
               if (support != null && !SiftCampProtection.near(level, support.x(), support.z(), 8)) {
                  SiftTerrainFeatureMemory.support(salt, support);
                  MutableBlockPos cursor = new MutableBlockPos();
                  int radius = 8;
                  boolean coral = TheSiftDimension.isGrimSurfaceBiome(SiftCaveHabitat.surfaceBiome(level, x, z));

                  for (int px = support.x() - radius; px <= support.x() + radius; px++) {
                     for (int pz = support.z() - radius; pz <= support.z() + radius; pz++) {
                        if (bounds.contains(px, pz)) {
                           int cap = SiftStrataSupportShape.capTop(terrain, support, px, pz);

                           for (int y = Math.max(12, support.floor() - 4); y <= cap; y++) {
                              if (SiftStrataSupportShape.contains(salt, support.x(), support.z(), support.floor(), support.ceiling(), px, y, pz)) {
                                 cursor.set(px, y, pz);
                                 if (level.ensureCanWrite(cursor) && level.isEmptyBlock(cursor)) {
                                    level.setBlock(cursor, material(salt, support, px, y, pz, coral), 2);
                                    changed = true;
                                 }
                              }
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

   private static BlockState material(long salt, SiftStrataSupportShape.Support support, int x, int y, int z, boolean coral) {
      if (coral) {
         return ModBlocks.SOULSHALE.defaultBlockState();
      } else {
         double pocket = 0.55 * SiftTerrainShapeNoise.noise(salt, x + z * 0.31, 7.0) + 0.45 * SiftTerrainShapeNoise.noise(salt ^ 3717841L, y + x * 0.23, 6.0);
         double distance = Math.hypot(x - support.x(), z - support.z());
         if (y <= support.floor() + 4 && distance > 2.5 && pocket > 0.48) {
            return ModBlocks.SIFT_RUBBLE.defaultBlockState();
         } else if (pocket > 0.57) {
            return ModBlocks.COBBLED_SIFTSLATE.defaultBlockState();
         } else {
            return pocket < -0.68 ? ModBlocks.COBBLED_SIFTSLATE.defaultBlockState() : ModBlocks.SIFTSLATE.defaultBlockState();
         }
      }
   }

   static SiftStrataSupportShape.Terrain terrainView(final WorldGenLevel level, final SiftWorldgenBounds bounds) {
      return new SiftStrataSupportShape.Terrain() {
         @Override
         public boolean ground(int x, int y, int z) {
            return SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(x, y, z)));
         }

         @Override
         public boolean air(int x, int y, int z) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            return state.getFluidState().isEmpty() && !SiftLandmarkTerrain.isGround(state) && SiftLandmarkTerrain.canReplace(state);
         }

         @Override
         public int surfaceY(int x, int z) {
            return level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;
         }

         @Override
         public boolean canSample(int x, int z) {
            return bounds.contains(x, z);
         }

         @Override
         public boolean protectedRock(int x, int y, int z) {
            return SiftTerrainFeatureMemory.isSupport(x, y, z);
         }
      };
   }
}
