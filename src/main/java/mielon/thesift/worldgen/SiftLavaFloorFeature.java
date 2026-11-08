package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SiftLavaFloorFeature extends Feature<NoneFeatureConfiguration> {
   private static final int LAVA_SURFACE_Y = 11;

   public SiftLavaFloorFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      int chunkMinX = context.origin().getX() & -16;
      int chunkMinZ = context.origin().getZ() & -16;
      int maxY = Math.min(11, level.getMaxY() - 1);
      int placed = 0;
      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = chunkMinX; x < chunkMinX + 16; x++) {
         for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
            for (int y = level.getMinY(); y <= maxY; y++) {
               cursor.set(x, y, z);
               if (level.getBlockState(cursor).isAir() && level.ensureCanWrite(cursor)) {
                  level.setBlock(cursor, Blocks.LAVA.defaultBlockState(), 2);
                  placed++;
               }
            }
         }
      }

      return placed > 0;
   }
}
