package mielon.thesift.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class LoadedBlockSearch {
   public static final int MAX_BLOCK_READS = 131072;
   private static final int MAX_RESULTS = 4096;

   private LoadedBlockSearch() {
   }

   public static Optional<List<BlockPos>> find(ServerLevel level, BlockPos origin, int radius, Predicate<BlockState> predicate) {
      List<BlockPos> result = new ArrayList<>();
      int reads = 0;
      int minY = Math.max(level.getMinY(), origin.getY() - radius);
      int maxY = Math.min(level.getMaxY() - 1, origin.getY() + radius);

      for (int cx = origin.getX() - radius >> 4; cx <= origin.getX() + radius >> 4; cx++) {
         for (int cz = origin.getZ() - radius >> 4; cz <= origin.getZ() + radius >> 4; cz++) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk != null) {
               LevelChunkSection[] sections = chunk.getSections();

               for (int i = 0; i < sections.length; i++) {
                  int baseY = level.getMinY() + i * 16;
                  LevelChunkSection section = sections[i];
                  if (baseY <= maxY && baseY + 15 >= minY && !section.hasOnlyAir() && section.getStates().maybeHas(predicate)) {
                     for (int y = Math.max(minY, baseY); y <= Math.min(maxY, baseY + 15); y++) {
                        for (int x = Math.max(origin.getX() - radius, cx * 16); x <= Math.min(origin.getX() + radius, cx * 16 + 15); x++) {
                           for (int z = Math.max(origin.getZ() - radius, cz * 16); z <= Math.min(origin.getZ() + radius, cz * 16 + 15); z++) {
                              if (++reads > 131072) {
                                 return Optional.empty();
                              }

                              if (predicate.test(section.getBlockState(x & 15, y & 15, z & 15))) {
                                 if (result.size() == 4096) {
                                    return Optional.empty();
                                 }

                                 result.add(new BlockPos(x, y, z));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return Optional.of(result);
   }
}
