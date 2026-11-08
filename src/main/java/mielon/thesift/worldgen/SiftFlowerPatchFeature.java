package mielon.thesift.worldgen;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SiftFlowerPatchFeature extends Feature<NoneFeatureConfiguration> {
   private final boolean saucerOnly;

   public SiftFlowerPatchFeature() {
      this(false);
   }

   public SiftFlowerPatchFeature(boolean saucerOnly) {
      super(NoneFeatureConfiguration.CODEC);
      this.saucerOnly = saucerOnly;
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      int centerX = (context.origin().getX() & -16) + 8;
      int centerZ = (context.origin().getZ() & -16) + 8;
      int target = 3 + random.nextInt(2);
      int placed = 0;
      int surfaceY = level.getHeight(Types.WORLD_SURFACE_WG, centerX, centerZ);
      boolean overgrown = TheSiftDimension.isOvergrownBiome(level.getBiome(new BlockPos(centerX, surfaceY, centerZ)));
      if (this.saucerOnly && !overgrown) {
         return false;
      }

      int selection = this.saucerOnly ? 3 : random.nextInt(overgrown ? 6 : 24);
      boolean wildflowers = selection < 3;

      Block flower = switch (selection % 3) {
         case 0 -> saucerBloom(random);
         case 1 -> ModBlocks.SUNBURST_PLANT;
         default -> ModBlocks.WHISPERBLOOM;
      };

      for (int attempt = 0; attempt < 24 && placed < target; attempt++) {
         int x = centerX + random.nextInt(7) - 3;
         int z = centerZ + random.nextInt(7) - 3;
         int y = level.getHeight(Types.WORLD_SURFACE_WG, x, z);
         BlockPos plantPos = new BlockPos(x, y, z);
         BlockState floor = level.getBlockState(plantPos.below());
         if (isFlowerSoil(floor) && level.isEmptyBlock(plantPos) && level.ensureCanWrite(plantPos)) {
            BlockState flowerState = wildflowers ? wildflowersState(random) : flower.defaultBlockState();
            if (flowerState.canSurvive(level, plantPos)) {
               level.setBlock(plantPos, flowerState, 2);
               SiftFlowerHabitat.record(level, plantPos, flowerState);
               placed++;
            }
         }
      }

      return placed > 0;
   }

   static int placeCavePatch(WorldGenLevel level, RandomSource random, BlockPos anchor, Block flower, boolean wildflowers) {
      int target = flower == ModBlocks.GRIM_BUSH ? 4 + random.nextInt(3) : 3 + random.nextInt(2);
      int placed = 0;
      boolean grim = SiftCaveHabitat.grim(level, anchor);

      for (int attempt = 0; attempt < 24 && placed < target; attempt++) {
         int x = anchor.getX() + random.nextInt(7) - 3;
         int z = anchor.getZ() + random.nextInt(7) - 3;

         for (int step = 0; step < 5; step++) {
            int dy = step == 0 ? 0 : (step + 1) / 2 * (step % 2 == 1 ? 1 : -1);
            BlockPos floor = new BlockPos(x, anchor.getY() + dy, z);
            BlockState soil = level.getBlockState(floor);
            boolean habitat = grim
               ? (
                     flower == ModBlocks.GRIM_BUSH
                        ? SiftLandmarkTerrain.isGround(soil)
                        : soil.is(ModBlocks.SOULSHALE_GROWTH) || soil.is(ModBlocks.DRY_HEALTHY_SCULK) || soil.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
                  )
                  && SiftCaveHabitat.grim(level, floor)
               : (soil.is(ModBlocks.SIFTSLATE_GROWTH) || soil.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)) && SiftCaveHabitat.lush(level, floor);
            BlockPos pos = floor.above();
            if (habitat
               && !SiftTerrainFeatureMemory.isShelter(pos)
               && level.isEmptyBlock(pos)
               && level.isEmptyBlock(pos.above())
               && SiftCaveHabitat.plantFloor(level, floor)
               && level.ensureCanWrite(pos)) {
               BlockState plant = wildflowers ? wildflowersState(random) : flower.defaultBlockState();
               if (plant.canSurvive(level, pos)) {
                  level.setBlock(pos, plant, 2);
                  SiftFlowerHabitat.record(level, pos, plant);
                  placed++;
                  break;
               }
            }
         }
      }

      return placed;
   }

   static Block saucerBloom(RandomSource random) {
      return random.nextBoolean() ? ModBlocks.BLUE_SAUCER_BLOOM : ModBlocks.GREEN_SAUCER_BLOOM;
   }

   static boolean isFlowerSoil(BlockState floor) {
      return floor.is(ModBlocks.SIFTSLATE)
         || floor.is(ModBlocks.SIFTSLATE_GROWTH)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK);
   }

   static boolean isLakeFlowerSoil(BlockState floor) {
      return isFlowerSoil(floor) || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   static BlockState wildflowersState(RandomSource random) {
      return Blocks.WILDFLOWERS
         .defaultBlockState()
         .setValue(FlowerBedBlock.FACING, Direction.from2DDataValue(random.nextInt(4)))
         .setValue(FlowerBedBlock.AMOUNT, 1 + random.nextInt(4));
   }
}
