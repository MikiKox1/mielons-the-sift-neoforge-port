package mielon.thesift.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import mielon.thesift.worldgen.SiftLandmarkPlacement;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.SuggestCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;

public final class SiftLocateCommand {
   private static final Map<Object, SiftLocateCommand.ShelterSearch> SHELTER_SEARCHES = new HashMap<>();
   private static final Map<Object, SiftLocateCommand.PortalSearch> PORTAL_SEARCHES = new HashMap<>();
   private static final Map<Object, SiftLocateCommand.LandmarkSearch> LANDMARK_SEARCHES = new HashMap<>();

   private SiftLocateCommand() {
   }

   public static int locateMainPortal(CommandSourceStack source) {
      if (!source.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         source.sendFailure(Component.translatable("commands.locate.structure.not_found", "the_sift:main_portal"));
         return 0;
      }

      ServerLevel sift = source.getLevel();
      Optional<BlockPos> located = SiftWorldStorage.getPortalAnchor(source.getServer());
      if (located.isEmpty()) {
         located = SiftPortalStructure.ensurePortal(source.getServer(), sift);
      }

      if (located.isEmpty()) {
         source.sendFailure(Component.translatable("commands.locate.structure.not_found", "the_sift:main_portal"));
         return 0;
      } else {
         return sendLocated(source, "the_sift:main_portal", located.get());
      }
   }

   public static void clearPendingSearches() {
      SHELTER_SEARCHES.clear();
      PORTAL_SEARCHES.clear();
      LANDMARK_SEARCHES.clear();
   }

   public static int locateSoulshaleShelter(CommandSourceStack source) {
      if (!isInsideSift(source, "the_sift:soulshale_shelter")) {
         return 0;
      } else {
         SiftLandmarkTracker.tick(source.getServer());
         BlockPos origin = BlockPos.containing(source.getPosition());
         Optional<BlockPos> known = SiftLandmarkTracker.nearest(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, origin);
         if (known.isPresent()) {
            return sendLocated(source, "the_sift:soulshale_shelter", known.get());
         } else {
            Object key = source.getEntity() == null ? source.getTextName() : source.getEntity().getUUID();
            if (SHELTER_SEARCHES.containsKey(key)) {
               return 0;
            } else {
               List<SiftLocateCommand.LandmarkCandidate> candidates = shelterCandidates(source);
               if (candidates.isEmpty()) {
                  source.sendFailure(Component.translatable("commands.locate.structure.not_found", "the_sift:soulshale_shelter"));
                  return 0;
               } else {
                  SiftLocateCommand.ShelterSearch search = new SiftLocateCommand.ShelterSearch(source, key, candidates);
                  SHELTER_SEARCHES.put(key, search);
                  continueShelterSearch(search);
                  return 1;
               }
            }
         }
      }
   }

   private static List<SiftLocateCommand.LandmarkCandidate> shelterCandidates(CommandSourceStack source) {
      ServerLevel level = source.getLevel();
      int size = 8;
      BlockPos origin = BlockPos.containing(source.getPosition());
      int cellX = Math.floorDiv(origin.getX() >> 4, size);
      int cellZ = Math.floorDiv(origin.getZ() >> 4, size);
      List<SiftLocateCommand.LandmarkCandidate> raw = new ArrayList<>();

      for (int dx = -8; dx <= 8; dx++) {
         for (int dz = -8; dz <= 8; dz++) {
            ChunkPos chunk = SiftLandmarkPlacement.candidateChunk(level.getSeed(), cellX + dx, cellZ + dz, size, 7670146607783890456L, 0);
            int x = (chunk.x() << 4) + 8;
            int z = (chunk.z() << 4) + 8;
            raw.add(new SiftLocateCommand.LandmarkCandidate(new BlockPos(x, 0, z), horizontalDistanceSquared(source, x, z)));
         }
      }

      raw.sort(Comparator.comparingDouble(SiftLocateCommand.LandmarkCandidate::distanceSquared));
      List<SiftLocateCommand.LandmarkCandidate> matching = new ArrayList<>();
      ChunkGenerator generator = level.getChunkSource().getGenerator();
      BiomeResolver resolver = generator.getBiomeSource().createUncachedResolver(level.getChunkSource().randomState());

      for (SiftLocateCommand.LandmarkCandidate candidate : raw) {
         boolean grim = false;

         for (int y = 16; y <= 208 && !grim; y += 24) {
            grim = resolver.getNoiseBiome(candidate.pos().getX() >> 2, y >> 2, candidate.pos().getZ() >> 2).is(TheSiftDimension.SOULSHALE_CAVES);
         }

         if (grim) {
            matching.add(candidate);
         }

         if (matching.size() >= 48) {
            break;
         }
      }

      return matching;
   }

   private static void continueShelterSearch(SiftLocateCommand.ShelterSearch search) {
      if (SHELTER_SEARCHES.get(search.key) == search && !search.source.getServer().isStopped()) {
         Optional<BlockPos> known = SiftLandmarkTracker.nearest(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, search.origin);
         if (known.isPresent()) {
            SHELTER_SEARCHES.remove(search.key);
            sendLocated(search.source, "the_sift:soulshale_shelter", known.get());
         } else if (search.next >= search.candidates.size()) {
            SHELTER_SEARCHES.remove(search.key);
            search.source.sendFailure(Component.translatable("commands.locate.structure.not_found", "the_sift:soulshale_shelter"));
         } else {
            BlockPos candidate = search.candidates.get(search.next++).pos();
            ServerChunkCache cache = search.source.getLevel().getChunkSource();
            ChunkPos chunk = new ChunkPos(candidate.getX() >> 4, candidate.getZ() >> 4);
            cache.addTicketWithRadius(TicketType.PORTAL, chunk, 0);
            CompletableFuture.<CompletableFuture<ChunkResult<ChunkAccess>>>supplyAsync(
                  () -> cache.getChunkFuture(candidate.getX() >> 4, candidate.getZ() >> 4, ChunkStatus.FULL, true)
               )
               .thenCompose(future -> (CompletionStage<ChunkResult<ChunkAccess>>)future)
               .whenComplete((result, error) -> search.source.getServer().execute(() -> {
                  cache.removeTicketWithRadius(TicketType.PORTAL, chunk, 0);
                  if (SHELTER_SEARCHES.get(search.key) == search) {
                     if (error != null) {
                        SHELTER_SEARCHES.remove(search.key);
                        search.source.sendFailure(Component.translatable("commands.locate.structure.not_found", "the_sift:soulshale_shelter"));
                     } else {
                        continueShelterSearch(search);
                     }
                  }
               }));
         }
      }
   }

   public static int locateAbandonedPortal(CommandSourceStack source, String biome) {
      if (!isInsideSift(source, SiftPortalVariants.abandonedLocateId(biome))) {
         return 0;
      } else {
         SiftLandmarkTracker.initialize(source.getServer());
         SiftPortalVariants.indexLegacyAbandoned(source.getLevel());
         SiftLandmarkTracker.Kind kind = SiftPortalVariants.abandonedKind(biome);
         Optional<BlockPos> known = SiftLandmarkTracker.nearest(kind, BlockPos.containing(source.getPosition()));
         if (known.isPresent()) {
            return sendLocated(source, SiftPortalVariants.abandonedLocateId(biome), known.get());
         } else {
            Object key = source.getEntity() == null ? source.getTextName() : source.getEntity().getUUID();
            if (PORTAL_SEARCHES.containsKey(key)) {
               return 0;
            } else {
               List<SiftLocateCommand.LandmarkCandidate> candidates = portalBiomeCandidates(source, biome);
               if (candidates.isEmpty()) {
                  source.sendFailure(Component.translatable("commands.locate.structure.not_found", SiftPortalVariants.abandonedLocateId(biome)));
                  return 0;
               } else {
                  SiftLocateCommand.PortalSearch search = new SiftLocateCommand.PortalSearch(source, key, biome, candidates);
                  PORTAL_SEARCHES.put(key, search);
                  continuePortalSearch(search);
                  return 1;
               }
            }
         }
      }
   }

   private static List<SiftLocateCommand.LandmarkCandidate> portalBiomeCandidates(CommandSourceStack source, String biome) {
      ServerLevel level = source.getLevel();
      BlockPos origin = BlockPos.containing(source.getPosition());
      int size = 20;
      int cx = Math.floorDiv(origin.getX() >> 4, size);
      int cz = Math.floorDiv(origin.getZ() >> 4, size);
      List<SiftLocateCommand.LandmarkCandidate> raw = new ArrayList<>();

      for (int dx = -8; dx <= 8; dx++) {
         for (int dz = -8; dz <= 8; dz++) {
            for (int n = 0; n < 2; n++) {
               ChunkPos chunk = SiftLandmarkPlacement.candidateChunk(level.getSeed(), cx + dx, cz + dz, size, 4702392765337521733L, n);
               int x = chunk.getMinBlockX() + 8;
               int z = chunk.getMinBlockZ() + 8;
               raw.add(new SiftLocateCommand.LandmarkCandidate(new BlockPos(x, 0, z), horizontalDistanceSquared(source, x, z)));
            }
         }
      }

      raw.sort(Comparator.comparingDouble(SiftLocateCommand.LandmarkCandidate::distanceSquared));
      RandomState random = level.getChunkSource().randomState();
      ChunkGenerator generator = level.getChunkSource().getGenerator();
      BiomeSource sourceBiomes = generator.getBiomeSource();
      BiomeResolver resolver = sourceBiomes.createUncachedResolver(random);
      List<SiftLocateCommand.LandmarkCandidate> matching = new ArrayList<>();

      for (SiftLocateCommand.LandmarkCandidate candidate : raw) {
         boolean suitable = false;

         for (int y = 16; y <= 304 && !suitable; y += 16) {
            Holder<Biome> sample = resolver.getNoiseBiome(candidate.pos().getX() >> 2, y >> 2, candidate.pos().getZ() >> 2);
            suitable = sample.unwrapKey().orElseThrow().identifier().getPath().equals(biome);
         }

         if (suitable) {
            matching.add(candidate);
         }

         if (matching.size() >= 64) {
            break;
         }
      }

      return matching;
   }

   private static void continuePortalSearch(SiftLocateCommand.PortalSearch search) {
      if (PORTAL_SEARCHES.get(search.key) == search && !search.source.getServer().isStopped()) {
         Optional<BlockPos> known = SiftLandmarkTracker.nearest(SiftPortalVariants.abandonedKind(search.biome), search.origin);
         if (known.isPresent()) {
            PORTAL_SEARCHES.remove(search.key);
            sendLocated(search.source, SiftPortalVariants.abandonedLocateId(search.biome), known.get());
         } else if (search.next >= search.candidates.size()) {
            PORTAL_SEARCHES.remove(search.key);
            search.source.sendFailure(Component.translatable("commands.locate.structure.not_found", SiftPortalVariants.abandonedLocateId(search.biome)));
         } else {
            BlockPos candidate = search.candidates.get(search.next++).pos();
            ServerChunkCache cache = search.source.getLevel().getChunkSource();
            ChunkPos chunk = new ChunkPos(candidate.getX() >> 4, candidate.getZ() >> 4);
            cache.addTicketWithRadius(TicketType.PORTAL, chunk, 0);
            CompletableFuture.<CompletableFuture<ChunkResult<ChunkAccess>>>supplyAsync(
                  () -> cache.getChunkFuture(candidate.getX() >> 4, candidate.getZ() >> 4, ChunkStatus.FULL, true)
               )
               .thenCompose(future -> (CompletionStage<ChunkResult<ChunkAccess>>)future)
               .whenComplete(
                  (result, error) -> search.source
                     .getServer()
                     .execute(
                        () -> {
                           cache.removeTicketWithRadius(TicketType.PORTAL, chunk, 0);
                           if (PORTAL_SEARCHES.get(search.key) == search) {
                              if (error != null) {
                                 PORTAL_SEARCHES.remove(search.key);
                                 search.source
                                    .sendFailure(
                                       Component.translatable("commands.locate.structure.not_found", SiftPortalVariants.abandonedLocateId(search.biome))
                                    );
                              } else {
                                 continuePortalSearch(search);
                              }
                           }
                        }
                     )
               );
         }
      }
   }

   public static int locateAbandonedPortal(CommandSourceStack source) {
      if (!isInsideSift(source, "the_sift:abandoned_main_portal")) {
         return 0;
      }

      ServerLevel level = source.getLevel();
      int sourceChunkX = Math.floorDiv((int)Math.floor(source.getPosition().x), 16);
      int sourceChunkZ = Math.floorDiv((int)Math.floor(source.getPosition().z), 16);
      int cellSize = 20;
      int centerCellX = Math.floorDiv(sourceChunkX, cellSize);
      int centerCellZ = Math.floorDiv(sourceChunkZ, cellSize);
      List<SiftLocateCommand.LandmarkCandidate> candidates = collectCandidates(level, centerCellX, centerCellZ, cellSize, 4702392765337521733L, 2, source);
      return locateGeneratedLandmark(source, "the_sift:abandoned_main_portal", SiftLandmarkTracker.Kind.ABANDONED_MAIN_PORTAL, candidates, 32);
   }

   private static int locateGeneratedLandmark(
      CommandSourceStack source, String id, SiftLandmarkTracker.Kind kind, List<SiftLocateCommand.LandmarkCandidate> candidates, int generationBudget
   ) {
      SiftLandmarkTracker.initialize(source.getServer());
      BlockPos origin = BlockPos.containing(source.getPosition());
      Optional<BlockPos> known = SiftLandmarkTracker.nearest(kind, origin);
      if (known.isPresent()) {
         return sendLocated(source, id, known.get());
      }

      Object key = source.getEntity() == null ? source.getTextName() : source.getEntity().getUUID();
      if (LANDMARK_SEARCHES.containsKey(key)) {
         return 0;
      }

      SiftLocateCommand.LandmarkSearch search = new SiftLocateCommand.LandmarkSearch(
         source, key, id, kind, List.copyOf(candidates.subList(0, Math.min(generationBudget, candidates.size())))
      );
      LANDMARK_SEARCHES.put(key, search);
      continueLandmarkSearch(search);
      return 1;
   }

   private static void continueLandmarkSearch(SiftLocateCommand.LandmarkSearch search) {
      if (LANDMARK_SEARCHES.get(search.key) == search && !search.source.getServer().isStopped()) {
         Optional<BlockPos> known = SiftLandmarkTracker.nearest(search.kind, BlockPos.containing(search.source.getPosition()));
         if (!known.isPresent() && search.next < search.candidates.size()) {
            BlockPos candidate = search.candidates.get(search.next++).pos();
            ServerChunkCache cache = search.source.getLevel().getChunkSource();
            ChunkPos chunk = new ChunkPos(candidate.getX() >> 4, candidate.getZ() >> 4);
            cache.addTicketWithRadius(TicketType.PORTAL, chunk, 0);
            CompletableFuture.<CompletableFuture<ChunkResult<ChunkAccess>>>supplyAsync(
                  () -> cache.getChunkFuture(candidate.getX() >> 4, candidate.getZ() >> 4, ChunkStatus.FULL, true)
               )
               .thenCompose(future -> (CompletionStage<ChunkResult<ChunkAccess>>)future)
               .whenComplete((result, error) -> search.source.getServer().execute(() -> {
                  cache.removeTicketWithRadius(TicketType.PORTAL, chunk, 0);
                  if (LANDMARK_SEARCHES.get(search.key) == search) {
                     if (error != null) {
                        LANDMARK_SEARCHES.remove(search.key);
                        search.source.sendFailure(Component.translatable("commands.locate.structure.not_found", search.id));
                     } else {
                        continueLandmarkSearch(search);
                     }
                  }
               }));
         } else {
            LANDMARK_SEARCHES.remove(search.key);
            if (known.isPresent()) {
               sendLocated(search.source, search.id, known.get());
            } else {
               search.source.sendFailure(Component.translatable("commands.locate.structure.not_found", search.id));
            }
         }
      }
   }

   private static List<SiftLocateCommand.LandmarkCandidate> collectCandidates(
      ServerLevel level, int centerCellX, int centerCellZ, int cellSize, long salt, int candidateCount, CommandSourceStack source
   ) {
      List<SiftLocateCommand.LandmarkCandidate> raw = new ArrayList<>();

      for (int dx = -7; dx <= 7; dx++) {
         for (int dz = -7; dz <= 7; dz++) {
            for (int candidateIndex = 0; candidateIndex < candidateCount; candidateIndex++) {
               ChunkPos chunk = SiftLandmarkPlacement.candidateChunk(level.getSeed(), centerCellX + dx, centerCellZ + dz, cellSize, salt, candidateIndex);
               int x = (chunk.x() << 4) + 8;
               int z = (chunk.z() << 4) + 8;
               double distanceSquared = horizontalDistanceSquared(source, x, z);
               raw.add(new SiftLocateCommand.LandmarkCandidate(new BlockPos(x, 0, z), distanceSquared));
            }
         }
      }

      raw.sort(Comparator.comparingDouble(SiftLocateCommand.LandmarkCandidate::distanceSquared));
      return List.copyOf(raw.subList(0, Math.min(96, raw.size())));
   }

   private static boolean isInsideSift(CommandSourceStack source, String id) {
      if (source.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return true;
      }

      source.sendFailure(Component.translatable("commands.locate.structure.not_found", id));
      return false;
   }

   private static int sendLocated(CommandSourceStack source, String id, BlockPos pos) {
      int distance = (int)Math.floor(Math.sqrt(horizontalDistanceSquared(source, pos.getX(), pos.getZ())));
      String yCoordinate = !"the_sift:soulshale_shelter".equals(id) && SiftPortalVariants.abandonedLocateBiome(id) == null ? "~" : Integer.toString(pos.getY());
      String teleportCommand = "/tp @s " + pos.getX() + " " + yCoordinate + " " + pos.getZ();
      MutableComponent coordinates = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), yCoordinate, pos.getZ()))
         .withStyle(
            style -> style.withColor(ChatFormatting.GREEN)
               .withClickEvent(new SuggestCommand(teleportCommand))
               .withHoverEvent(new ShowText(Component.translatable("chat.coordinates.tooltip")))
         );
      source.sendSuccess(() -> Component.translatable("commands.locate.structure.success", id, coordinates, distance), false);
      return distance;
   }

   private static double horizontalDistanceSquared(CommandSourceStack source, int x, int z) {
      double dx = x + 0.5 - source.getPosition().x;
      double dz = z + 0.5 - source.getPosition().z;
      return dx * dx + dz * dz;
   }

   private record LandmarkCandidate(BlockPos pos, double distanceSquared) {
   }

   private static final class LandmarkSearch {
      final CommandSourceStack source;
      final Object key;
      final String id;
      final SiftLandmarkTracker.Kind kind;
      final List<SiftLocateCommand.LandmarkCandidate> candidates;
      int next;

      LandmarkSearch(CommandSourceStack source, Object key, String id, SiftLandmarkTracker.Kind kind, List<SiftLocateCommand.LandmarkCandidate> candidates) {
         this.source = source;
         this.key = key;
         this.id = id;
         this.kind = kind;
         this.candidates = candidates;
      }
   }

   private static final class PortalSearch {
      final CommandSourceStack source;
      final Object key;
      final String biome;
      final BlockPos origin;
      final List<SiftLocateCommand.LandmarkCandidate> candidates;
      int next;

      PortalSearch(CommandSourceStack source, Object key, String biome, List<SiftLocateCommand.LandmarkCandidate> candidates) {
         this.source = source;
         this.key = key;
         this.biome = biome;
         this.origin = BlockPos.containing(source.getPosition());
         this.candidates = candidates;
      }
   }

   private static final class ShelterSearch {
      final CommandSourceStack source;
      final Object key;
      final BlockPos origin;
      final List<SiftLocateCommand.LandmarkCandidate> candidates;
      int next;

      ShelterSearch(CommandSourceStack source, Object key, List<SiftLocateCommand.LandmarkCandidate> candidates) {
         this.source = source;
         this.key = key;
         this.origin = BlockPos.containing(source.getPosition());
         this.candidates = candidates;
      }
   }
}
