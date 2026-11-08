package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.LongStream;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class SiftLandmarkTracker {
   private static final int SAVE_INTERVAL_TICKS = 600;
   private static final Identifier STORAGE_ID = Identifier.fromNamespaceAndPath("the_sift", "generated_landmarks");
   private static final Map<SiftLandmarkTracker.Kind, Set<Long>> POSITIONS = new EnumMap<>(SiftLandmarkTracker.Kind.class);
   private static final Map<SiftLandmarkTracker.Kind, Map<Long, Set<Long>>> POSITIONS_BY_CHUNK = new EnumMap<>(SiftLandmarkTracker.Kind.class);
   private static final Map<Long, SiftLandmarkTracker.CanyonActivity> LEGACY_CANYON_ACTIVITY = new ConcurrentHashMap<>();
   private static final Set<SiftLandmarkTracker.Region> DIRTY_REGIONS = ConcurrentHashMap.newKeySet();
   private static final Map<SiftLandmarkTracker.Kind, Set<Long>> REGIONS = new EnumMap<>(SiftLandmarkTracker.Kind.class);
   private static volatile boolean dirty;
   private static boolean loaded;
   private static int saveCooldown;

   private SiftLandmarkTracker() {
   }

   public static synchronized boolean tryClaimShelter(BlockPos entrance, double minimumDistance) {
      if (nearestOther(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, entrance, minimumDistance, null).isPresent()) {
         return false;
      }

      record(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, entrance);
      return true;
   }

   public static boolean hasRecorded(SiftLandmarkTracker.Kind kind, BlockPos pos) {
      return POSITIONS.get(kind).contains(pos.asLong());
   }

   public static void record(SiftLandmarkTracker.Kind kind, BlockPos pos) {
      long packed = pos.asLong();
      if (POSITIONS.get(kind).add(packed)) {
         index(kind, packed);
         changed(kind, packed);
      }
   }

   public static void forget(SiftLandmarkTracker.Kind kind, BlockPos pos) {
      long packed = pos.asLong();
      if (POSITIONS.get(kind).remove(packed)) {
         unindex(kind, packed);
         changed(kind, packed);
      }
   }

   private static void index(SiftLandmarkTracker.Kind kind, long packed) {
      BlockPos pos = BlockPos.of(packed);
      long chunk = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
      POSITIONS_BY_CHUNK.get(kind).computeIfAbsent(chunk, ignored -> ConcurrentHashMap.newKeySet()).add(packed);
   }

   private static void unindex(SiftLandmarkTracker.Kind kind, long packed) {
      BlockPos pos = BlockPos.of(packed);
      long chunk = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
      Set<Long> positions = POSITIONS_BY_CHUNK.get(kind).get(chunk);
      if (positions != null) {
         positions.remove(packed);
         if (positions.isEmpty()) {
            POSITIONS_BY_CHUNK.get(kind).remove(chunk, positions);
         }
      }
   }

   private static long chunkKey(int chunkX, int chunkZ) {
      return (long)chunkX << 32 ^ chunkZ & 4294967295L;
   }

   public static Optional<BlockPos> nearest(SiftLandmarkTracker.Kind kind, BlockPos origin) {
      BlockPos best = null;
      double bestDistance = Double.POSITIVE_INFINITY;
      ArrayList<Entry<Long, Set<Long>>> chunks = new ArrayList<>(POSITIONS_BY_CHUNK.get(kind).entrySet());
      chunks.sort(Comparator.comparingDouble(entryx -> chunkDistanceSqr((Long)entryx.getKey(), origin)));

      for (Entry<Long, Set<Long>> entry : chunks) {
         if (chunkDistanceSqr(entry.getKey(), origin) > bestDistance) {
            break;
         }

         for (long packed : entry.getValue()) {
            BlockPos candidate = BlockPos.of(packed);
            double distance = horizontalDistanceSqr(candidate, origin);
            if (distance < bestDistance || distance == bestDistance && (best == null || packed < best.asLong())) {
               bestDistance = distance;
               best = candidate;
            }
         }
      }

      return Optional.ofNullable(best);
   }

   private static double chunkDistanceSqr(long chunk, BlockPos origin) {
      int x = (int)(chunk >> 32) * 16;
      int z = (int)chunk * 16;
      double dx = Math.max(0, Math.max(x - origin.getX(), origin.getX() - (x + 15)));
      double dz = Math.max(0, Math.max(z - origin.getZ(), origin.getZ() - (z + 15)));
      return dx * dx + dz * dz;
   }

   public static List<BlockPos> near(SiftLandmarkTracker.Kind kind, BlockPos origin, int radius) {
      List<BlockPos> result = new ArrayList<>();
      Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(kind);

      for (int x = origin.getX() - radius >> 4; x <= origin.getX() + radius >> 4; x++) {
         for (int z = origin.getZ() - radius >> 4; z <= origin.getZ() + radius >> 4; z++) {
            Set<Long> bucket = index.get(chunkKey(x, z));
            if (bucket != null) {
               for (long packed : bucket) {
                  BlockPos pos = BlockPos.of(packed);
                  if (horizontalDistanceSqr(origin, pos) <= (double)radius * radius) {
                     result.add(pos);
                  }
               }
            }
         }
      }

      return result;
   }

   private static long regionKey(long packed) {
      BlockPos pos = BlockPos.of(packed);
      return chunkKey(pos.getX() >> 9, pos.getZ() >> 9);
   }

   private static void changed(SiftLandmarkTracker.Kind kind, long packed) {
      long region = regionKey(packed);
      REGIONS.get(kind).add(region);
      DIRTY_REGIONS.add(new SiftLandmarkTracker.Region(kind, region));
      dirty = true;
   }

   private static Identifier regionId(SiftLandmarkTracker.Kind kind, long key) {
      return Identifier.fromNamespaceAndPath("the_sift_landmarks_" + Long.toUnsignedString(key, 16), kind.storageKey());
   }

   public static Optional<BlockPos> nearestOther(SiftLandmarkTracker.Kind kind, BlockPos origin, double maximumDistance, BlockPos excluded) {
      BlockPos best = null;
      double bestDistance = maximumDistance * maximumDistance;
      long excludedPacked = excluded == null ? Long.MIN_VALUE : excluded.asLong();
      int originChunkX = origin.getX() >> 4;
      int originChunkZ = origin.getZ() >> 4;
      int maximumRing = ceilChunks(maximumDistance);
      Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(kind);

      for (int ring = 0; ring <= maximumRing; ring++) {
         for (int chunkX = originChunkX - ring; chunkX <= originChunkX + ring; chunkX++) {
            for (int chunkZ = originChunkZ - ring; chunkZ <= originChunkZ + ring; chunkZ++) {
               if (ring <= 0 || Math.max(Math.abs(chunkX - originChunkX), Math.abs(chunkZ - originChunkZ)) == ring) {
                  Set<Long> positions = index.get(chunkKey(chunkX, chunkZ));
                  if (positions != null) {
                     for (long packed : positions) {
                        if (packed != excludedPacked) {
                           BlockPos candidate = BlockPos.of(packed);
                           double distance = horizontalDistanceSqr(candidate, origin);
                           if (distance < bestDistance) {
                              bestDistance = distance;
                              best = candidate;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (best != null && outsideRingDistanceSqr(origin, originChunkX, originChunkZ, ring) >= bestDistance) {
            break;
         }
      }

      return Optional.ofNullable(best);
   }

   public static void initialize(MinecraftServer server) {
      ensureLoaded(server);
   }

   public static BlockPos nearestInVolume(SiftLandmarkTracker.Kind kind, BlockPos origin, int horizontal, int vertical) {
      BlockPos result = null;
      double best = Double.POSITIVE_INFINITY;
      Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(kind);

      for (int cx = origin.getX() - horizontal >> 4; cx <= origin.getX() + horizontal >> 4; cx++) {
         for (int cz = origin.getZ() - horizontal >> 4; cz <= origin.getZ() + horizontal >> 4; cz++) {
            Set<Long> positions = index.get(chunkKey(cx, cz));
            if (positions != null) {
               for (long packed : positions) {
                  BlockPos candidate = BlockPos.of(packed);
                  if (Math.abs(candidate.getY() - origin.getY()) <= vertical && !(horizontalDistanceSqr(candidate, origin) > horizontal * horizontal)) {
                     double distance = candidate.distSqr(origin);
                     if (distance < best) {
                        best = distance;
                        result = candidate;
                     }
                  }
               }
            }
         }
      }

      return result;
   }

   public static List<BlockPos> nearPlayers(SiftLandmarkTracker.Kind kind, ServerLevel level, int radius) {
      Set<BlockPos> result = new LinkedHashSet<>();

      for (ServerPlayer player : level.players()) {
         if (player.isAlive() && !player.isSpectator()) {
            result.addAll(near(kind, player.blockPosition(), radius));
         }
      }

      return List.copyOf(result);
   }

   public static List<BlockPos> snapshot(SiftLandmarkTracker.Kind kind) {
      return POSITIONS.get(kind).stream().map(BlockPos::of).toList();
   }

   public static Optional<BlockPos> nearestActiveSoulCanyon(ServerLevel level, BlockPos origin, double maximumDistance) {
      return nearestActiveSoulCanyon(level, origin, maximumDistance, Set.of());
   }

   public static Optional<BlockPos> nearestActiveSoulCanyon(ServerLevel level, BlockPos origin, double maximumDistance, Collection<Long> excluded) {
      BlockPos best = null;
      double bestDistance = maximumDistance * maximumDistance;
      int originChunkX = origin.getX() >> 4;
      int originChunkZ = origin.getZ() >> 4;
      int maximumRing = ceilChunks(maximumDistance);
      Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(SiftLandmarkTracker.Kind.SOUL_CANYON_CLUSTER);

      for (int ring = 0; ring <= maximumRing; ring++) {
         for (int chunkX = originChunkX - ring; chunkX <= originChunkX + ring; chunkX++) {
            for (int chunkZ = originChunkZ - ring; chunkZ <= originChunkZ + ring; chunkZ++) {
               if (ring <= 0 || Math.max(Math.abs(chunkX - originChunkX), Math.abs(chunkZ - originChunkZ)) == ring) {
                  Set<Long> positions = index.get(chunkKey(chunkX, chunkZ));
                  if (positions != null) {
                     for (long packed : positions) {
                        if (!excluded.contains(packed)) {
                           BlockPos candidate = BlockPos.of(packed);
                           double distance = horizontalDistanceSqr(candidate, origin);
                           if (distance < bestDistance && hasLoadedSoulBlockNear(level, candidate)) {
                              bestDistance = distance;
                              best = candidate;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (best != null && outsideRingDistanceSqr(origin, originChunkX, originChunkZ, ring) >= bestDistance) {
            break;
         }
      }

      return Optional.ofNullable(best);
   }

   public static List<BlockPos> activeSoulBlocksNear(ServerLevel level, BlockPos center, int horizontal, int vertical) {
      double horizontalSqr = (double)horizontal * horizontal;
      int minimumChunkX = center.getX() - horizontal >> 4;
      int maximumChunkX = center.getX() + horizontal >> 4;
      int minimumChunkZ = center.getZ() - horizontal >> 4;
      int maximumChunkZ = center.getZ() + horizontal >> 4;
      List<BlockPos> result = new ArrayList<>();
      Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK);

      for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
         for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
            Set<Long> positions = index.get(chunkKey(chunkX, chunkZ));
            if (positions != null) {
               for (long packed : positions) {
                  BlockPos candidate = BlockPos.of(packed);
                  if (horizontalDistanceSqr(candidate, center) <= horizontalSqr
                     && Math.abs(candidate.getY() - center.getY()) <= vertical
                     && level.isLoaded(candidate)
                     && level.getBlockState(candidate).is(ModBlocks.SOUL_BLOCK)) {
                     result.add(candidate);
                  }
               }
            }
         }
      }

      return result;
   }

   public static boolean hasIndexedSoulPositionsNear(BlockPos center, int horizontal, int vertical) {
      double horizontalSqr = (double)horizontal * horizontal;
      int minimumChunkX = center.getX() - horizontal >> 4;
      int maximumChunkX = center.getX() + horizontal >> 4;
      int minimumChunkZ = center.getZ() - horizontal >> 4;
      int maximumChunkZ = center.getZ() + horizontal >> 4;
      Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(SiftLandmarkTracker.Kind.SOUL_CANYON_SOUL_BLOCK);

      for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
         for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
            Set<Long> positions = index.get(chunkKey(chunkX, chunkZ));
            if (positions != null) {
               for (long packed : positions) {
                  BlockPos candidate = BlockPos.of(packed);
                  if (horizontalDistanceSqr(candidate, center) <= horizontalSqr && Math.abs(candidate.getY() - center.getY()) <= vertical) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean hasLoadedSoulBlockNear(ServerLevel level, BlockPos center) {
      List<BlockPos> recorded = activeSoulBlocksNear(level, center, 36, 28);
      if (!recorded.isEmpty()) {
         return true;
      }

      if (hasIndexedSoulPositionsNear(center, 36, 28)) {
         return false;
      }

      long now = level.getGameTime();
      SiftLandmarkTracker.CanyonActivity cached = LEGACY_CANYON_ACTIVITY.get(center.asLong());
      if (cached != null && cached.validUntil() > now) {
         return cached.active();
      }

      MutableBlockPos cursor = new MutableBlockPos();

      for (int dy = -20; dy <= 20; dy++) {
         for (int dx = -24; dx <= 24; dx++) {
            for (int dz = -24; dz <= 24; dz++) {
               if (dx * dx + dz * dz <= 576) {
                  cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                  if (level.isLoaded(cursor) && level.getBlockState(cursor).is(ModBlocks.SOUL_BLOCK)) {
                     LEGACY_CANYON_ACTIVITY.put(center.asLong(), new SiftLandmarkTracker.CanyonActivity(true, now + 100L));
                     return true;
                  }
               }
            }
         }
      }

      LEGACY_CANYON_ACTIVITY.put(center.asLong(), new SiftLandmarkTracker.CanyonActivity(false, now + 1200L));
      return false;
   }

   private static int ceilChunks(double distance) {
      return Math.max(0, (int)Math.ceil(distance / 16.0) + 1);
   }

   private static double outsideRingDistanceSqr(BlockPos origin, int originChunkX, int originChunkZ, int ring) {
      int minimumX = originChunkX - ring << 4;
      int maximumX = (originChunkX + ring << 4) + 15;
      int minimumZ = originChunkZ - ring << 4;
      int maximumZ = (originChunkZ + ring << 4) + 15;
      int distance = Math.min(
         Math.min(origin.getX() - (minimumX - 1), maximumX + 1 - origin.getX()), Math.min(origin.getZ() - (minimumZ - 1), maximumZ + 1 - origin.getZ())
      );
      return (double)distance * distance;
   }

   private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
      double dx = first.getX() - second.getX();
      double dz = first.getZ() - second.getZ();
      return dx * dx + dz * dz;
   }

   public static void tick(MinecraftServer server) {
      ensureLoaded(server);
      if (dirty && --saveCooldown <= 0) {
         saveCooldown = 600;
         writeStorage(server);
      }
   }

   public static void flush(MinecraftServer server) {
      ensureLoaded(server);
      if (dirty) {
         writeStorage(server);
      }
   }

   private static void ensureLoaded(MinecraftServer server) {
      if (!loaded) {
         loaded = true;
         CompoundTag stored = server.getCommandStorage().get(STORAGE_ID);
         if (stored.getIntOr("format", 0) >= 2) {
            for (SiftLandmarkTracker.Kind kind : SiftLandmarkTracker.Kind.values()) {
               for (long region : stored.getLongArray(kind.storageKey()).orElse(new long[0])) {
                  REGIONS.get(kind).add(region);
                  CompoundTag shard = server.getCommandStorage()
                     .get(
                        stored.getIntOr("format", 0) == 2
                           ? Identifier.fromNamespaceAndPath("the_sift", "generated_landmarks/" + kind.storageKey() + "/" + Long.toUnsignedString(region, 16))
                           : regionId(kind, region)
                     );

                  for (long packed : shard.getLongArray("positions").orElse(new long[0])) {
                     if (POSITIONS.get(kind).add(packed)) {
                        index(kind, packed);
                     }

                     if (stored.getIntOr("format", 0) == 2) {
                        changed(kind, packed);
                     }
                  }
               }
            }
         } else {
            for (SiftLandmarkTracker.Kind kind : SiftLandmarkTracker.Kind.values()) {
               long[] positions = stored.getLongArray(kind.storageKey()).orElse(new long[0]);
               if (kind == SiftLandmarkTracker.Kind.SOULSHALE_SHELTER && stored.contains("grimstone_shelters")) {
                  positions = LongStream.concat(Arrays.stream(positions), Arrays.stream(stored.getLongArray("grimstone_shelters").orElse(new long[0])))
                     .distinct()
                     .toArray();
                  dirty = true;
               }

               for (long position : positions) {
                  if (POSITIONS.get(kind).add(position)) {
                     index(kind, position);
                     changed(kind, position);
                  }
               }
            }
         }
      }
   }

   private static void writeStorage(MinecraftServer server) {
      dirty = false;

      for (SiftLandmarkTracker.Region region : new ArrayList<>(DIRTY_REGIONS)) {
         DIRTY_REGIONS.remove(region);
         ArrayList<Long> positions = new ArrayList<>();
         int regionX = (int)(region.key() >> 32);
         int regionZ = (int)region.key();
         Map<Long, Set<Long>> index = POSITIONS_BY_CHUNK.get(region.kind());

         for (int x = regionX * 32; x < regionX * 32 + 32; x++) {
            for (int z = regionZ * 32; z < regionZ * 32 + 32; z++) {
               Set<Long> bucket = index.get(chunkKey(x, z));
               if (bucket != null) {
                  positions.addAll(bucket);
               }
            }
         }

         CompoundTag shard = new CompoundTag();
         shard.putLongArray("positions", positions.stream().mapToLong(Long::longValue).sorted().toArray());
         server.getCommandStorage().set(regionId(region.kind(), region.key()), shard);
      }

      CompoundTag manifest = new CompoundTag();
      manifest.putInt("format", 3);

      for (SiftLandmarkTracker.Kind kind : SiftLandmarkTracker.Kind.values()) {
         manifest.putLongArray(kind.storageKey(), REGIONS.get(kind).stream().mapToLong(Long::longValue).sorted().toArray());
      }

      server.getCommandStorage().set(STORAGE_ID, manifest);
   }

   public static void clearTransientState() {
      for (Set<Long> positions : POSITIONS.values()) {
         positions.clear();
      }

      for (Map<Long, Set<Long>> positions : POSITIONS_BY_CHUNK.values()) {
         positions.clear();
      }

      LEGACY_CANYON_ACTIVITY.clear();
      DIRTY_REGIONS.clear();
      REGIONS.values().forEach(Set::clear);
      dirty = false;
      loaded = false;
      saveCooldown = 0;
   }

   static {
      for (SiftLandmarkTracker.Kind kind : SiftLandmarkTracker.Kind.values()) {
         REGIONS.put(kind, ConcurrentHashMap.newKeySet());
         POSITIONS.put(kind, ConcurrentHashMap.newKeySet());
         POSITIONS_BY_CHUNK.put(kind, new ConcurrentHashMap<>());
      }
   }

   private record CanyonActivity(boolean active, long validUntil) {
   }

   public enum Kind {
      SOULSHALE_SHELTER("soulshale_shelters"),
      PORTAL_VEGETATION_REPAIRED("portal_vegetation_repaired"),
      ABANDONED_MAIN_PORTAL("abandoned_main_portals"),
      SNIFFER_CAVE("sniffer_caves"),
      SOUL_CANYON_ENTRANCE("soul_canyon_entrances"),
      SOUL_CANYON_CLUSTER("soul_canyon_clusters"),
      SOUL_CANYON_MIDPOINT("soul_canyon_midpoints"),
      SOUL_CANYON_SOUL_BLOCK("soul_canyon_soul_blocks"),
      SOUL_CANYON_GOLEM_PENDING("soul_canyon_golems_pending"),
      SCULK_BLOB_MEMBER_SPAWNED("sculk_blob_members_spawned"),
      SCULK_BLOB_RESIDENT_PENDING("sculk_blob_residents_pending"),
      SCULK_BLOB_RESIDENT_SPAWNED("sculk_blob_residents_spawned"),
      FLOWER_BLUE_SAUCER("flower_blue_saucer"),
      FLOWER_GREEN_SAUCER("flower_green_saucer"),
      FLOWER_SUNBURST("flower_sunburst"),
      FLOWER_WHISPERBLOOM("flower_whisperbloom"),
      FLOWER_TORCHFLOWER("flower_torchflower"),
      FLOWER_PITCHERFLOWER("flower_pitcherflower"),
      FLOWER_WILDFLOWERS("flower_wildflowers"),
      FLOWER_SURVEYED_CHUNK("flower_surveyed_chunks"),
      ABANDONED_PORTAL_SIFT_WASTES("abandoned_portal_sift_wastes"),
      ABANDONED_PORTAL_OVERGROWN_CLEARING("abandoned_portal_overgrown_clearing"),
      ABANDONED_PORTAL_OVERGROWN_FOREST("abandoned_portal_overgrown_forest"),
      ABANDONED_PORTAL_ICHOR_SNOWY_PEAKS("abandoned_portal_ichor_snowy_peaks"),
      ABANDONED_PORTAL_SIFTSLATE_CAVES("abandoned_portal_siftslate_caves"),
      ABANDONED_PORTAL_OVERGROWN_CAVES("abandoned_portal_overgrown_caves"),
      ABANDONED_PORTAL_SOULSHALE_CAVES("abandoned_portal_soulshale_caves"),
      ABANDONED_PORTAL_ORGANIC_DESERT("abandoned_portal_organic_desert"),
      ABANDONED_PORTAL_CORAL_MEADOW("abandoned_portal_coral_meadow"),
      ABANDONED_PORTAL_CORAL_FOREST("abandoned_portal_coral_forest"),
      ABANDONED_PORTAL_CORRUPTED_SIFT("abandoned_portal_corrupted_sift");

      private final String storageKey;

      Kind(String storageKey) {
         this.storageKey = storageKey;
      }

      private String storageKey() {
         return this.storageKey;
      }
   }

   private record Region(SiftLandmarkTracker.Kind kind, long key) {
   }
}
