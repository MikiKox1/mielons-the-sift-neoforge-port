package mielon.thesift.portal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousDeepslateBlock;
import mielon.thesift.world.LoadedBlockSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SonorousConsoles {
   public static final int GROUP_SEARCH_RADIUS = 16;
   public static final int[] TARGET_SEQUENCE = new int[]{1, 3, 7, 6, 5, 2, 4, 8};
   public static final int[] CLOSING_SEQUENCE = reverse(TARGET_SEQUENCE);
   private static final Map<SonorousConsoles.ConsoleKey, Deque<SonorousConsoles.PlayedNote>> BUFFERS = new HashMap<>();
   public static final int MAX_GROUP_SIZE = 256;

   private SonorousConsoles() {
   }

   public static void clearTransientBuffers() {
      BUFFERS.clear();
   }

   public static Optional<SonorousConsoles.MatchedSequence> onSoundPlayed(ServerLevel level, BlockPos noteBlockPos, int soundIndex1to8) {
      List<BlockPos> group = findGroup(level, noteBlockPos);
      if (group.isEmpty()) {
         return Optional.empty();
      }

      SonorousConsoles.ConsoleKey key = new SonorousConsoles.ConsoleKey(level.dimension(), anchor(group));
      if (BUFFERS.size() >= 1024 && !BUFFERS.containsKey(key)) {
         BUFFERS.clear();
      }

      Deque<SonorousConsoles.PlayedNote> buffer = BUFFERS.computeIfAbsent(key, k -> new ArrayDeque<>(TARGET_SEQUENCE.length + 1));
      buffer.addLast(new SonorousConsoles.PlayedNote(noteBlockPos.immutable(), soundIndex1to8));

      while (buffer.size() > TARGET_SEQUENCE.length) {
         buffer.removeFirst();
      }

      if (buffer.size() == TARGET_SEQUENCE.length) {
         int first = buffer.getFirst().soundIndex1to8();
         SonorousConsoles.SequenceType type = first == TARGET_SEQUENCE[0]
            ? SonorousConsoles.SequenceType.OPENING
            : (first == CLOSING_SEQUENCE[0] ? SonorousConsoles.SequenceType.CLOSING : null);
         if (type != null) {
            List<SonorousConsoles.PlayedNote> attempt = List.copyOf(buffer);
            int[] expected = type == SonorousConsoles.SequenceType.OPENING ? TARGET_SEQUENCE : CLOSING_SEQUENCE;
            if (matches(attempt, expected)) {
               BUFFERS.remove(key);
            }

            return Optional.of(new SonorousConsoles.MatchedSequence(attempt, type));
         }
      }

      return Optional.empty();
   }

   public static void reset(ServerLevel level, BlockPos anyPosNearGroup) {
      BlockPos anchorPos = findAnchor(level, anyPosNearGroup);
      if (anchorPos != null) {
         BUFFERS.remove(new SonorousConsoles.ConsoleKey(level.dimension(), anchorPos));
      }
   }

   public static BlockPos findAnchor(ServerLevel level, BlockPos anyPosNearGroup) {
      List<BlockPos> group = findGroup(level, anyPosNearGroup);
      return group.isEmpty() ? null : anchor(group);
   }

   private static boolean matches(List<SonorousConsoles.PlayedNote> notes, int[] sequence) {
      for (int index = 0; index < sequence.length; index++) {
         if (notes.get(index).soundIndex1to8() != sequence[index]) {
            return false;
         }
      }

      return true;
   }

   private static int[] reverse(int[] source) {
      int[] result = new int[source.length];

      for (int i = 0; i < source.length; i++) {
         result[i] = source[source.length - 1 - i];
      }

      return result;
   }

   public static List<BlockPos> findGroup(ServerLevel level, BlockPos origin) {
      BlockPos seed = resolveSeed(level, origin);
      if (seed == null) {
         return List.of();
      }

      Optional<List<BlockPos>> candidates = LoadedBlockSearch.find(
         level, seed, 32, state -> state.is(ModBlocks.SONOROUS_DEEPSLATE) && state.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.NOTE
      );
      return candidates.isEmpty() ? List.of() : boundedGroup(seed, candidates.get());
   }

   public static List<BlockPos> boundedGroup(BlockPos seed, List<BlockPos> candidates) {
      Set<BlockPos> found = new HashSet<>();
      Deque<BlockPos> queue = new ArrayDeque<>();
      found.add(seed);
      queue.add(seed);
      int minX = seed.getX();
      int maxX = minX;
      int minY = seed.getY();
      int maxY = minY;
      int minZ = seed.getZ();
      int maxZ = minZ;

      while (!queue.isEmpty()) {
         BlockPos current = queue.removeFirst();

         for (BlockPos candidate : candidates) {
            if (!found.contains(candidate)
               && Math.abs(candidate.getX() - current.getX()) <= 16
               && Math.abs(candidate.getY() - current.getY()) <= 16
               && Math.abs(candidate.getZ() - current.getZ()) <= 16) {
               found.add(candidate);
               minX = Math.min(minX, candidate.getX());
               maxX = Math.max(maxX, candidate.getX());
               minY = Math.min(minY, candidate.getY());
               maxY = Math.max(maxY, candidate.getY());
               minZ = Math.min(minZ, candidate.getZ());
               maxZ = Math.max(maxZ, candidate.getZ());
               if (found.size() > 256 || maxX - minX > 16 || maxY - minY > 16 || maxZ - minZ > 16) {
                  return List.of();
               }

               queue.addLast(candidate);
            }
         }
      }

      return new ArrayList<>(found);
   }

   private static BlockPos resolveSeed(ServerLevel level, BlockPos origin) {
      BlockPos originImmutable = origin.immutable();
      if (isNoteState(level, originImmutable)) {
         return originImmutable;
      }

      BlockPos below = originImmutable.below();
      return isNoteState(level, below) ? below : null;
   }

   private static boolean isNoteState(ServerLevel level, BlockPos pos) {
      if (!level.hasChunkAt(pos)) {
         return false;
      }

      BlockState state = level.getBlockState(pos);
      return !state.is(ModBlocks.SONOROUS_DEEPSLATE) ? false : state.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.NOTE;
   }

   private static BlockPos anchor(List<BlockPos> group) {
      return group.stream().min(Comparator.<BlockPos>comparingInt(p -> p.getY()).thenComparingInt(Vec3i::getX).thenComparingInt(Vec3i::getZ)).orElseThrow();
   }

   private record ConsoleKey(ResourceKey<Level> dimension, BlockPos anchor) {
   }

   public record MatchedSequence(List<SonorousConsoles.PlayedNote> notes, SonorousConsoles.SequenceType type) {
   }

   public record PlayedNote(BlockPos notePos, int soundIndex1to8) {
   }

   public enum SequenceType {
      OPENING,
      CLOSING;
   }
}
