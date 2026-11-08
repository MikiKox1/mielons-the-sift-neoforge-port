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

final class SiftslateCavePlants {
   private SiftslateCavePlants() {
   }

   static boolean decorate(WorldGenLevel level, RandomSource random, List<BlockPos> floors) {
      Map<Integer, List<BlockPos>> layers = new TreeMap<>();

      for (BlockPos floor : floors) {
         layers.computeIfAbsent(Math.floorDiv(floor.getY(), 14), key -> new ArrayList<>()).add(floor);
      }

      boolean changed = false;

      for (List<BlockPos> layer : layers.values()) {
         if (layer.size() >= 4) {
            Map<Long, BlockPos> ground = new HashMap<>();

            for (BlockPos floor : layer) {
               ground.put(floor.asLong(), floor);
            }

            List<BlockPos> anchors = new ArrayList<>();
            int patches = Math.min(3, 1 + layer.size() / 96);

            for (int patch = 0; patch < patches; patch++) {
               BlockPos anchor = null;

               for (int attempt = 0; attempt < 24 && anchor == null; attempt++) {
                  BlockPos candidate = layer.get(random.nextInt(layer.size()));
                  boolean spaced = true;

                  for (BlockPos other : anchors) {
                     int dx = other.getX() - candidate.getX();
                     int dz = other.getZ() - candidate.getZ();
                     if (dx * dx + dz * dz < 36) {
                        spaced = false;
                     }
                  }

                  if (spaced) {
                     anchor = candidate;
                  }
               }

               if (anchor != null) {
                  anchors.add(anchor);
                  int roll = random.nextInt(100);
                  Block plant = roll < 62
                     ? ModBlocks.SIFTSLATE_STALKS
                     : (
                        roll < 80
                           ? ModBlocks.DRY_HEALTHY_SCULK_SPROUTS
                           : (
                              roll < 94
                                 ? ModBlocks.HEALTHY_SCULK_SPROUTS
                                 : (roll < 96 ? ModBlocks.OVERGROWN_FRONDS : (roll < 98 ? ModBlocks.OVERGROWN_CHARD : ModBlocks.OVERGROWN_STALKS))
                           )
                     );
                  int placed = 0;
                  int target = 4 + random.nextInt(4);

                  for (int attempt = 0; attempt < 48 && placed < target; attempt++) {
                     int x = anchor.getX() + random.nextInt(4) - random.nextInt(4);
                     int z = anchor.getZ() + random.nextInt(4) - random.nextInt(4);

                     for (int dy : new int[]{0, 1, -1}) {
                        BlockPos floor = ground.get(BlockPos.asLong(x, anchor.getY() + dy, z));
                        if (floor != null) {
                           BlockPos pos = floor.above();
                           BlockState state = plant.defaultBlockState();
                           if (level.isEmptyBlock(pos) && level.ensureCanWrite(pos) && state.canSurvive(level, pos)) {
                              level.setBlock(pos, state, 2);
                              placed++;
                              changed = true;
                              break;
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
}
