package mielon.thesift.worldgen;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class SiftPlantPatches {
   private static final Map<String, PlacedFeature> PATCHES = new ConcurrentHashMap<>();

   private SiftPlantPatches() {
   }

   public static boolean place(
      WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, Block plant, int tries, int spread, Block... floors
   ) {
      String key = plant + ":" + tries + ":" + spread + ":" + Arrays.toString(floors);
      PlacedFeature patch = PATCHES.computeIfAbsent(
         key,
         ignored -> new PlacedFeature(
            Holder.direct(new SimpleBlockFeature(BlockStateProvider.of(plant))),
            List.of(
               CountPlacement.of(tries),
               OffsetPlacement.ofTriangle(spread, 2),
               BlockPredicateFilter.forPredicate(
                  BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(new BlockPos(0, -1, 0), Arrays.asList(floors)))
               )
            )
         )
      );
      return patch.place(level, generator, random, origin);
   }

   public static Block[] bonemealPalette(BlockState soil) {
      if (soil.is(ModBlocks.SIFTSLATE_GROWTH)) {
         return new Block[]{ModBlocks.OVERGROWN_FRONDS, ModBlocks.OVERGROWN_CHARD, ModBlocks.OVERGROWN_STALKS};
      } else if (soil.is(ModBlocks.CORAL_GROWTH)) {
         return new Block[]{ModBlocks.CORAL_BUSH, ModBlocks.BLUBBY_BERRIES_BUSH};
      } else if (soil.is(ModBlocks.BRIGHT_CORAL_GROWTH)) {
         return new Block[]{ModBlocks.BRIGHT_CORAL_BUSH, ModBlocks.BLUBBY_BERRIES_BUSH};
      } else if (soil.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)) {
         return new Block[]{ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT};
      } else if (soil.is(ModBlocks.DRY_HEALTHY_SCULK)) {
         return new Block[]{ModBlocks.DRY_HEALTHY_SCULK_SPROUTS};
      } else if (soil.is(ModBlocks.HEALTHY_SCULK) || soil.is(ModBlocks.HEALTHY_SCULK_PORES)) {
         return new Block[]{ModBlocks.HEALTHY_SCULK_SPROUTS};
      } else {
         return soil.is(ModBlocks.SOULSHALE_GROWTH)
            ? new Block[]{ModBlocks.HEALTHY_SCULK_SPROUTS, ModBlocks.GRIM_BUSH}
            : new Block[]{
               ModBlocks.OVERGROWN_FRONDS,
               ModBlocks.OVERGROWN_CHARD,
               ModBlocks.OVERGROWN_STALKS,
               ModBlocks.SIFTSLATE_STALKS,
               ModBlocks.HEALTHY_SCULK_SPROUTS,
               ModBlocks.DRY_HEALTHY_SCULK_SPROUTS
            };
      }
   }

   public static void bonemeal(ServerLevel level, RandomSource random, BlockPos soil, BlockState state) {
      Block[] palette = bonemealPalette(state);

      for (int attempt = 0; attempt < 64; attempt++) {
         plantBonemeal(level, random, soil, state, palette);
      }
   }

   private static boolean plantBonemeal(ServerLevel level, RandomSource random, BlockPos soil, BlockState source, Block[] palette) {
      int dx = random.nextInt(6) - random.nextInt(6);
      int dz = random.nextInt(6) - random.nextInt(6);
      int[] var7 = new int[]{0, 1, -1};
      int var8 = var7.length;
      int var9 = 0;

      while (true) {
         if (var9 >= var8) {
            return false;
         }

         int dy = var7[var9];
         BlockPos ground = soil.offset(dx, dy, dz);
         BlockPos pos = ground.above();
         if (level.hasChunkAt(ground) && level.getBlockState(ground).is(source.getBlock()) && level.isEmptyBlock(pos)) {
            Block plant = palette[random.nextInt(palette.length)];
            if ((source.is(ModBlocks.CORAL_GROWTH) || source.is(ModBlocks.BRIGHT_CORAL_GROWTH)) && random.nextInt(4) != 0) {
               plant = palette[0];
            }

            if (random.nextInt(8) == 0) {
               Block flower = SiftFlowerHabitat.flowerAt(level, ground);
               if (flower != null
                  && (!source.is(ModBlocks.SIFTSLATE_GROWTH) || flower != Blocks.TORCHFLOWER && flower != Blocks.PITCHER_CROP && flower != Blocks.PITCHER_PLANT)
                  )
                {
                  plant = flower;
               }
            }

            BlockState placed = plant == Blocks.WILDFLOWERS ? SiftFlowerPatchFeature.wildflowersState(random) : plant.defaultBlockState();
            if (plant == Blocks.PITCHER_CROP) {
               placed = placed.setValue(PitcherCropBlock.AGE, 4);
            }

            if (placed.canSurvive(level, pos)) {
               if (!(plant instanceof DoublePlantBlock)) {
                  level.setBlock(pos, placed, 3);
                  break;
               }

               if (level.isEmptyBlock(pos.above())) {
                  DoublePlantBlock.placeAt(level, placed, pos, 2);
                  break;
               }
            }
         }

         var9++;
      }

      return true;
   }
}
