package mielon.thesift.worldgen;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SiftslatePlantPatchFeature extends Feature<NoneFeatureConfiguration> {
   public SiftslatePlantPatchFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      int centerX = (context.origin().getX() & -16) + 8;
      int centerZ = (context.origin().getZ() & -16) + 8;
      int target = 4 + random.nextInt(4);
      int placed = 0;

      Block plant = switch (random.nextInt(6)) {
         case 0 -> ModBlocks.OVERGROWN_FRONDS;
         case 1 -> ModBlocks.OVERGROWN_STALKS;
         case 2 -> ModBlocks.OVERGROWN_CHARD;
         case 3 -> ModBlocks.SIFTSLATE_STALKS;
         case 4 -> ModBlocks.HEALTHY_SCULK_SPROUTS;
         default -> ModBlocks.DRY_HEALTHY_SCULK_SPROUTS;
      };

      for (int attempt = 0; attempt < 32 && placed < target; attempt++) {
         int x = centerX + random.nextInt(9) - 4;
         int z = centerZ + random.nextInt(9) - 4;
         int y = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
         BlockPos plantPos = new BlockPos(x, y, z);
         if (level.getBlockState(plantPos.below()).is(ModBlocks.SIFTSLATE) && level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
            BlockState state = plant.defaultBlockState();
            if (state.canSurvive(level, plantPos)) {
               level.setBlock(plantPos, state, 2);
               placed++;
            }
         }
      }

      return placed > 0;
   }
}
