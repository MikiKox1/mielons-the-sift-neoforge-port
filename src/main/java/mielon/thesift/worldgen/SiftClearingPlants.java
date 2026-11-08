package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class SiftClearingPlants {
   private SiftClearingPlants() {
   }

   static boolean decorate(WorldGenLevel level, RandomSource random, List<BlockPos> floors) {
      Map<Integer, List<BlockPos>> layers = new TreeMap<>();

      for (BlockPos floor : floors) {
         layers.computeIfAbsent(Math.floorDiv(floor.getY(), 14), ignored -> new ArrayList<>()).add(floor);
      }

      boolean changed = false;

      for (List<BlockPos> layer : layers.values()) {
         Map<Long, BlockPos> ground = new HashMap<>();

         for (BlockPos floor : layer) {
            ground.put(floor.asLong(), floor);
         }

         changed |= patches(level, random, layer, ground, ModBlocks.OVERGROWN_FRONDS, 8, 48);
         changed |= patches(level, random, layer, ground, ModBlocks.OVERGROWN_CHARD, 5, 34);
         changed |= patches(level, random, layer, ground, ModBlocks.OVERGROWN_STALKS, 3, 26);

         for (BlockPos floor : layer) {
            changed |= SiftSurfaceDecorator.decorate(level, random, floor, 1.0F);
         }
      }

      return changed;
   }

   private static boolean patches(
      WorldGenLevel level, RandomSource random, List<BlockPos> candidates, Map<Long, BlockPos> ground, Block plant, int count, int attempts
   ) {
      boolean changed = false;

      for (int batch = 0; batch < count; batch++) {
         BlockPos anchor = candidates.get(random.nextInt(candidates.size()));

         for (int attempt = 0; attempt < attempts; attempt++) {
            int x = anchor.getX() + random.nextInt(5) - random.nextInt(5);
            int z = anchor.getZ() + random.nextInt(5) - random.nextInt(5);
            int y = anchor.getY() + random.nextInt(2) - random.nextInt(2);
            BlockPos floor = ground.get(BlockPos.asLong(x, y, z));
            if (floor != null) {
               BlockPos pos = floor.above();
               BlockState state = plant.defaultBlockState();
               if (level.isEmptyBlock(pos) && level.ensureCanWrite(pos) && state.canSurvive(level, pos)) {
                  level.setBlock(pos, state, 2);
                  changed = true;
               }
            }
         }
      }

      return changed;
   }
}
