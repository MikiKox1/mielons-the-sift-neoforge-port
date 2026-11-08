package mielon.thesift.worldgen;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SnifferPlantPatchFeature extends Feature<NoneFeatureConfiguration> {
   public SnifferPlantPatchFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      int centerX = (context.origin().getX() & -16) + 8;
      int centerZ = (context.origin().getZ() & -16) + 8;
      int target = 5 + random.nextInt(5);
      int placed = 0;
      SiftSnifferPlantPlacement.PlantType blobType = SiftSnifferPlantPlacement.chooseBlobType(random, 0.58F);

      for (int attempt = 0; attempt < 28 && placed < target; attempt++) {
         int x = centerX + random.nextInt(13) - 6;
         int z = centerZ + random.nextInt(13) - 6;
         int y = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
         BlockPos plantPos = new BlockPos(x, y, z);
         BlockState floor = level.getBlockState(plantPos.below());
         if ((floor.is(ModBlocks.HEALTHY_SCULK) || floor.is(ModBlocks.DRY_HEALTHY_SCULK) || floor.is(ModBlocks.SIFTSLATE_GROWTH))
            && level.isEmptyBlock(plantPos)
            && level.ensureCanWrite(plantPos)
            && SiftSnifferPlantPlacement.place(level, plantPos, blobType, random)) {
            placed++;
         }
      }

      return placed > 0;
   }
}
