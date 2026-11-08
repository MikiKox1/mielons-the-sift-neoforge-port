package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftCaveWillowFeature implements Feature {
   public static final MapCodec<SiftCaveWillowFeature> CODEC = MapCodec.unit(SiftCaveWillowFeature::new);

   public MapCodec<SiftCaveWillowFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (random.nextInt(4) != 0) {
         return false;
      }

      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);

      for (int attempt = 0; attempt < 24; attempt++) {
         int x = (origin.getX() & -16) + 2 + random.nextInt(12);
         int z = (origin.getZ() & -16) + 2 + random.nextInt(12);

         for (int y = Math.min(210, Math.min(SiftCaveHabitat.rockTop(level, x, z) + 1, SiftCaveHabitat.surfaceRockBottom(level, x, z) - 1)); y >= 16; y--) {
            BlockPos root = new BlockPos(x, y, z);
            if (level.isEmptyBlock(root)
               && SiftCaveHabitat.lush(level, root.below())
               && SiftCaveHabitat.broadFloor(level, root.below())
               && !SiftTerrainFeatureMemory.isProtectedTerrain(x, y - 1, z)) {
               boolean clear = true;

               for (int dy = 0; dy < 10 && clear; dy++) {
                  for (int dx = -2; dx <= 2 && clear; dx++) {
                     for (int dz = -2; dz <= 2; dz++) {
                        BlockPos pos = root.offset(dx, dy, dz);
                        if (!level.isEmptyBlock(pos)) {
                           clear = false;
                           break;
                        }
                     }
                  }
               }

               if (clear && OvergrownWillowTreeFeature.placeAt(level, random, root, bounds, false)) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
