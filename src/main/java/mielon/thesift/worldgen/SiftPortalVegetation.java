package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class SiftPortalVegetation {
   private static final Map<Long, Set<BlockPos>> FLOORS = new LinkedHashMap<>();
   private static SiftPortalVegetation.Repair repair;
   private static final ThreadLocal<Boolean> PLANTS = new ThreadLocal<>();
   private static final ThreadLocal<Set<BlockPos>> REPLAY = new ThreadLocal<>();
   private static final ThreadLocal<Map<Long, Integer>> HEIGHTS = new ThreadLocal<>();

   private static long chunkKey(int x, int z) {
      return (long)x << 32 ^ z & 4294967295L;
   }

   private SiftPortalVegetation() {
   }

   public static boolean soil(BlockState s) {
      return s.is(ModBlocks.SIFTSLATE)
         || s.is(ModBlocks.SOULSHALE)
         || s.is(ModBlocks.CORAL_SOIL)
         || s.is(ModBlocks.SIFTSLATE_GROWTH)
         || s.is(ModBlocks.SOULSHALE_GROWTH)
         || s.is(ModBlocks.CORAL_GROWTH)
         || s.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || s.is(ModBlocks.HEALTHY_SCULK)
         || s.is(ModBlocks.DRY_HEALTHY_SCULK)
         || s.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || s.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || s.is(ModBlocks.HEALTHY_SCULK_PORES)
         || s.is(ModBlocks.ICHOR_SNOW_BLOCK);
   }

   public static synchronized void register(BlockPos floor) {
      FLOORS.computeIfAbsent(chunkKey(floor.getX() >> 4, floor.getZ() >> 4), k -> new HashSet<>()).add(floor.immutable());

      while (FLOORS.size() > 4096) {
         FLOORS.remove(FLOORS.keySet().iterator().next());
      }
   }

   public static synchronized void clear() {
      FLOORS.clear();
      repair = null;
   }

   private static synchronized boolean containsFloor(BlockPos pos) {
      Set<BlockPos> bucket = FLOORS.get(chunkKey(pos.getX() >> 4, pos.getZ() >> 4));
      return bucket != null && bucket.contains(pos);
   }

   public static synchronized List<BlockPos> around(BlockPos origin, int width, int length, int margin) {
      List<BlockPos> result = new ArrayList<>();

      for (int x = origin.getX() - margin >> 4; x <= origin.getX() + width + margin >> 4; x++) {
         for (int z = origin.getZ() - margin >> 4; z <= origin.getZ() + length + margin >> 4; z++) {
            Set<BlockPos> bucket = FLOORS.get(chunkKey(x, z));
            if (bucket != null) {
               for (BlockPos p : bucket) {
                  if (p.getX() >= origin.getX() - margin
                     && p.getX() < origin.getX() + width + margin
                     && p.getZ() >= origin.getZ() - margin
                     && p.getZ() < origin.getZ() + length + margin
                     && p.getY() >= origin.getY() - 3
                     && p.getY() < origin.getY() + 23) {
                     result.add(p);
                  }
               }
            }
         }
      }

      return result;
   }

   private static boolean plants(String path) {
      return path.startsWith("overgrown_fronds")
         || path.startsWith("overgrown_chard")
         || path.startsWith("overgrown_stalks")
         || path.startsWith("siftslate_stalks")
         || path.startsWith("healthy_sculk_sprouts")
         || path.startsWith("dry_healthy_sculk_sprouts")
         || path.equals("coral_vegetation")
         || path.equals("cave_vegetation")
         || path.equals("organic_desert_finish")
         || path.equals("siftslate_plant_patch")
         || path.equals("sniffer_plant_patch")
         || path.startsWith("sift_flower_patch")
         || path.equals("saucer_bloom_patch");
   }

   public static Boolean beginNatural(PlacedFeature feature, WorldGenLevel level) {
      Boolean previous = PLANTS.get();
      Identifier id = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getKey(feature);
      if (id != null && id.getNamespace().equals("the_sift") && plants(id.getPath())) {
         PLANTS.set(true);
      }

      return previous;
   }

   public static void endNatural(Boolean previous) {
      if (previous == null) {
         PLANTS.remove();
      } else {
         PLANTS.set(previous);
      }
   }

   public static boolean allowsFloor(BlockPos pos) {
      return Boolean.TRUE.equals(PLANTS.get()) && (REPLAY.get() != null ? REPLAY.get().contains(pos) : containsFloor(pos));
   }

   public static boolean allowsPlant(BlockPos pos) {
      return allowsFloor(pos.below()) || allowsFloor(pos.below(2));
   }

   public static Integer replayHeight(int x, int z) {
      return HEIGHTS.get() == null ? null : HEIGHTS.get().getOrDefault(BlockPos.asLong(x, 0, z), 0);
   }

   public static boolean permitsWrite(BlockGetter level, BlockPos pos, BlockState state) {
      Set<BlockPos> replay = REPLAY.get();
      if (replay == null) {
         return true;
      }

      BlockState current = level.getBlockState(pos);
      return replay.contains(pos)
         ? soil(current) && soil(state)
         : allowsPlant(pos)
            && current.isAir()
            && !state.isAir()
            && state.getFluidState().isEmpty()
            && !state.hasBlockEntity()
            && state.getCollisionShape(level, pos).isEmpty();
   }

   public static void restore(WorldGenLevel level, Collection<BlockPos> positions) {
      restore(level, positions, false);
   }

   static void restoreBare(WorldGenLevel level, Collection<BlockPos> positions) {
      restore(level, positions, true);
   }

   private static void restore(WorldGenLevel level, Collection<BlockPos> positions, boolean skipVegetatedDecks) {
      Set<BlockPos> floors = new HashSet<>();

      for (BlockPos p : positions) {
         if (level.hasChunkAt(p)
            && soil(level.getBlockState(p))
            && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
            && level.getBlockState(p.above()).getFluidState().isEmpty()) {
            floors.add(p.immutable());
            register(p);
         }
      }

      if (!floors.isEmpty()) {
         Set<BlockPos> old = REPLAY.get();
         Boolean previous = PLANTS.get();
         Map<Long, Integer> oldHeights = HEIGHTS.get();
         PLANTS.set(true);

         try {
            Map<SiftPortalVegetation.Deck, List<BlockPos>> chunks = new TreeMap<>(
               Comparator.comparingInt(SiftPortalVegetation.Deck::x)
                  .thenComparingInt(SiftPortalVegetation.Deck::z)
                  .thenComparingInt(SiftPortalVegetation.Deck::y)
            );

            for (BlockPos p : floors) {
               chunks.computeIfAbsent(new SiftPortalVegetation.Deck(p.getX() >> 4, p.getZ() >> 4, Math.floorDiv(p.getY(), 8)), ignored -> new ArrayList<>())
                  .add(p);
            }

            for (List<BlockPos> chunk : chunks.values()) {
               if (!skipVegetatedDecks || !chunk.stream().anyMatch(p -> !level.getBlockState(p.above()).isAir())) {
                  REPLAY.set(new HashSet<>(chunk));
                  Map<Long, Integer> heights = new HashMap<>();

                  for (BlockPos p : chunk) {
                     heights.merge(BlockPos.asLong(p.getX(), 0, p.getZ()), p.getY() + 1, Math::max);
                  }

                  HEIGHTS.set(heights);
                  chunk.sort(Comparator.comparingLong(BlockPos::asLong));
                  BlockPos first = chunk.get(0);
                  Holder<Biome> biome = SiftCaveHabitat.biome(level, first);
                  BlockPos origin = new BlockPos(first.getX() & -16, first.getY(), first.getZ() & -16);
                  RandomSource random = RandomSource.create(level.getSeed() ^ origin.asLong() ^ 5786934568104513362L);
                  ChunkGenerator generator = level.getLevel().getChunkSource().getGenerator();
                  if (TheSiftDimension.isOrganicDesert(biome)) {
                     OrganicDesertVegetation.place(level, generator, random, chunk);
                  } else {
                     for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
                        for (Holder<PlacedFeature> feature : step) {
                           Identifier id = feature.unwrapKey().orElseThrow().identifier();
                           if (id.getNamespace().equals("the_sift") && plants(id.getPath())) {
                              feature.value().placeWithBiomeCheck(level, generator, random, origin);
                           }
                        }
                     }
                  }
               }
            }
         } finally {
            if (old == null) {
               REPLAY.remove();
            } else {
               REPLAY.set(old);
            }

            if (oldHeights == null) {
               HEIGHTS.remove();
            } else {
               HEIGHTS.set(oldHeights);
            }

            endNatural(previous);
         }
      }
   }

   public static void tick(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level != null) {
         if (repair != null) {
            if (!ready(level, repair.center)) {
               repair = null;
            } else {
               if (repair.inspect(level)) {
                  SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.PORTAL_VEGETATION_REPAIRED, repair.center);
                  repair = null;
               }
            }
         } else if (level.getGameTime() % 100L == 0L) {
            for (ServerPlayer player : level.players()) {
               if (player.isAlive() && !player.isSpectator()) {
                  for (BlockPos center : SiftLandmarkTracker.near(SiftLandmarkTracker.Kind.ABANDONED_MAIN_PORTAL, player.blockPosition(), 64)) {
                     if (!SiftLandmarkTracker.hasRecorded(SiftLandmarkTracker.Kind.PORTAL_VEGETATION_REPAIRED, center) && ready(level, center)) {
                        repair = new SiftPortalVegetation.Repair(center);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean ready(ServerLevel level, BlockPos center) {
      for (int x = center.getX() - 23 >> 4; x <= center.getX() + 22 >> 4; x++) {
         for (int z = center.getZ() - 23 >> 4; z <= center.getZ() + 22 >> 4; z++) {
            if (level.getChunkSource().getChunkNow(x, z) == null) {
               return false;
            }
         }
      }

      return true;
   }

   private record Deck(int x, int z, int y) {
   }

   private static final class Repair {
      final BlockPos center;
      final Map<SiftPortalVegetation.Deck, List<BlockPos>> floors = new LinkedHashMap<>();
      int cursor;

      Repair(BlockPos center) {
         this.center = center.immutable();
      }

      boolean inspect(ServerLevel level) {
         int budget = 1024;

         while (this.cursor < 42320 && budget-- > 0) {
            int i = this.cursor++;
            BlockPos pos = this.center.offset(i / 920 - 23, i % 20, i / 20 % 46 - 23);
            if (SiftPortalVegetation.soil(level.getBlockState(pos))) {
               this.floors
                  .computeIfAbsent(new SiftPortalVegetation.Deck(pos.getX() >> 4, pos.getZ() >> 4, Math.floorDiv(pos.getY(), 8)), k -> new ArrayList<>())
                  .add(pos);
            }
         }

         if (this.cursor < 42320) {
            return false;
         }

         Iterator<List<BlockPos>> iterator = this.floors.values().iterator();
         if (iterator.hasNext()) {
            SiftPortalVegetation.restoreBare(level, iterator.next());
            iterator.remove();
         }

         return this.floors.isEmpty();
      }
   }
}
