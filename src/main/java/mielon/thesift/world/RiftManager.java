package mielon.thesift.world;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import mielon.thesift.TheSiftMod;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.entity.MiniRiftEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.entity.SiftiteReturnEntity;
import mielon.thesift.network.RiftLoadingPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.portal.TeleportTransition.PostTeleportTransition;
import net.minecraft.world.level.storage.LevelData.RespawnData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiftManager {
   private static final int REENTRY_COOLDOWN_TICKS = 100;
   private static final Map<UUID, Long> NEXT_SPAWN = new HashMap<>();
   private static GameRule<Boolean> spawnRifts;
   private static final Map<UUID, RiftManager.Request> PENDING = new LinkedHashMap<>();
   private static final Map<UUID, RiftManager.RiftArrival> RIFT_EXIT_LATCHES = new HashMap<>();
   private static final Map<UUID, RiftManager.ExitSearch> EXIT_SEARCHES = new HashMap<>();
   private static final int EXIT_SEARCH_COLUMNS_PER_TICK = 16;

   public static void registerGameRules() {
      // Called only during NeoForge's GAME_RULE RegisterEvent, while writable.
      spawnRifts = GameRules.registerBoolean("the_sift:spawn_rifts", GameRuleCategory.SPAWNING, true);
   }

   public static void tick(MinecraftServer server) {
      RiftLightCleanup.tick(server);
      List<RiftManager.Request> requests = List.copyOf(PENDING.values());
      PENDING.clear();

      for (RiftManager.Request request : requests) {
         finish(server, request);
      }

      long now = server.overworld().getGameTime();
      if (now % 600L == 0L) {
         PortalTransitGuard.prune(server);
      }

      releaseRiftExitLatches(server);
      tickNaturalSpawns(server, now);
      Set<UUID> visited = new HashSet<>();

      for (RiftEntity rift : RiftDirectory.loaded()) {
         if (!rift.isRemoved() && !rift.isClosing()) {
            for (Entity candidate : List.copyOf(rift.level().getEntities(rift, rift.getPortalBounds().inflate(8.0), RiftManager::traveler))) {
               Entity entity = candidate.getRootVehicle();
               UUID id = entity.getUUID();
               if (visited.add(id) && !entity.isRemoved() && !isRiftExitBlocked(entity) && !PortalTransitGuard.isBlocked(entity)) {
                  if (!touches(entity, rift)) {
                     visited.remove(id);
                  } else {
                     queue(entity, rift, false, true);
                  }
               }
            }
         }
      }
   }

   private static void tickNaturalSpawns(MinecraftServer server, long now) {
      if (!server.getGameRules().get(spawnRifts)) {
         NEXT_SPAWN.clear();
      } else {
         for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (player.level().dimension().equals(TheSiftDimension.LEVEL_KEY) && player.isAlive()) {
               long due = NEXT_SPAWN.computeIfAbsent(player.getUUID(), id -> now + 5400L + player.getRandom().nextInt(1201));
               if (now >= due) {
                  Optional<RiftManager.Placement> found = findNatural(player.level(), player.blockPosition(), player.getRandom());
                  if (found.isPresent()) {
                     RiftManager.Placement p = found.get();
                     spawn(player.level(), p, false, now + 6000L, UUID.randomUUID());
                  }

                  NEXT_SPAWN.put(player.getUUID(), now + (found.isPresent() ? 5400 + player.getRandom().nextInt(1201) : 200));
               }
            } else {
               NEXT_SPAWN.remove(player.getUUID());
            }
         }
      }
   }

   private static boolean isRiftExitBlocked(Entity entity) {
      return entity.getSelfAndPassengers().anyMatch(passenger -> RIFT_EXIT_LATCHES.containsKey(passenger.getUUID()));
   }

   private static void latchRiftExit(MinecraftServer server, Entity entity) {
      long cooldownUntil = server.overworld().getGameTime() + 100L;
      entity.getSelfAndPassengers()
         .forEach(
            passenger -> RIFT_EXIT_LATCHES.put(
               passenger.getUUID(), new RiftManager.RiftArrival(passenger.level().dimension(), passenger.position(), cooldownUntil)
            )
         );
   }

   private static void releaseRiftExitLatches(MinecraftServer server) {
      Iterator<Entry<UUID, RiftManager.RiftArrival>> iterator = RIFT_EXIT_LATCHES.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<UUID, RiftManager.RiftArrival> entry = iterator.next();
         RiftManager.RiftArrival arrival = entry.getValue();
         if (server.overworld().getGameTime() >= arrival.cooldownUntil()) {
            Entity entity = server.overworld().getEntityInAnyDimension(entry.getKey());
            if (entity != null && !entity.isRemoved()) {
               if (!arrival.dimension.equals(entity.level().dimension())) {
                  iterator.remove();
               } else if (!isTouchingRift(entity) && (hasLoadedRiftAt(arrival) || arrival.pos.distanceToSqr(entity.position()) >= 36.0)) {
                  iterator.remove();
               }
            } else {
               iterator.remove();
            }
         }
      }
   }

   private static boolean hasLoadedRiftAt(RiftManager.RiftArrival arrival) {
      for (RiftEntity rift : RiftDirectory.loaded()) {
         if (!rift.isRemoved() && rift.level().dimension().equals(arrival.dimension) && rift.getPortalBounds().inflate(0.5).contains(arrival.pos)) {
            return true;
         }
      }

      return false;
   }

   private static boolean traveler(Entity e) {
      return e.isAlive() && !e.isSpectator() && !(e instanceof RiftEntity) && !(e instanceof MiniRiftEntity) && !(e instanceof SiftiteReturnEntity);
   }

   private static boolean touches(Entity entity, RiftEntity rift) {
      Vec3 delta = entity.oldPosition().subtract(entity.position());
      if (delta.lengthSqr() > 256.0) {
         delta = Vec3.ZERO;
      }

      return entity.getBoundingBox().expandTowards(delta).intersects(rift.getPortalBounds());
   }

   public static boolean isTouchingRift(Entity entity) {
      for (RiftEntity rift : RiftDirectory.loaded()) {
         if (!rift.isRemoved() && rift.level() == entity.level() && rift.getPortalBounds().intersects(entity.getBoundingBox())) {
            return true;
         }
      }

      return false;
   }

   private static void screen(Entity entity, boolean start, boolean rift) {
      entity.getSelfAndPassengers().forEach(e -> {
         if (e instanceof ServerPlayer player) {
            RiftLoadingPayload.send(player, start, rift);
         }
      });
   }

   private static void queue(Entity entity, RiftEntity origin, boolean closed, boolean riftScreen) {
      if (!PENDING.containsKey(entity.getUUID())) {
         PortalTransitGuard.block(entity);
         SiftTeleportManager.suppressUntilExit(entity);
         screen(entity, true, riftScreen);
         PENDING.put(entity.getUUID(), new RiftManager.Request(entity, origin, closed, riftScreen));
      }
   }

   public static void returnFromPortal(Entity entity, boolean closed) {
      queue(entity.getRootVehicle(), null, closed, false);
   }

   private static void finish(MinecraftServer server, RiftManager.Request request) {
      Entity entity = request.entity;
      List<ServerPlayer> players = entity.getSelfAndPassengers().filter(e -> e instanceof ServerPlayer).map(e -> (ServerPlayer)e).toList();
      boolean keepLoading = false;

      try {
         try {
            if (entity.isRemoved() || !entity.isAlive()) {
               return;
            }

            RiftEntity origin = request.origin;
            if (origin != null && (entity.level() != origin.level()
               || origin.isRemoved() || origin.isClosing()
               || origin.getExpiresAt() <= origin.level().getGameTime()
               || !touches(entity, origin))) {
               // Searching for a counterpart may take several ticks. A player
               // who left an expired Rift must not teleport later by surprise.
               return;
            }

            boolean intoSift = origin != null && origin.targetsSift();
            RiftManager.Destination destination = intoSift ? intoSift(server, entity, origin) : outOfSift(server, entity, origin);
            if (destination == null && EXIT_SEARCHES.containsKey(entity.getUUID())) {
               PENDING.put(entity.getUUID(), request);
               keepLoading = true;
               return;
            }

            if (destination != null) {
               Optional<Vec3> safe = SafePortalArrival.find(destination.level, entity, destination.pos);
               if (safe.isEmpty()) {
                  return;
               }

               destination = new RiftManager.Destination(destination.level, safe.get(), destination.fallback, destination.exit);
            }

            if (destination == null || !entity.canTeleport(entity.level(), destination.level)) {
               return;
            }

            Vec3 motion = entity instanceof ServerPlayer ? Vec3.ZERO : entity.getDeltaMovement();
            Entity moved = moveToDestination(entity, destination, motion, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
            if (moved != null) {
               if (intoSift) {
                  // The source-world ticket must survive a rejected/failed
                  // teleport. Only a completed Rift entry consumes it.
                  moved.getSelfAndPassengers().forEach(SiftWorldStorage::markRiftEntry);
                  if (!moved.getUUID().equals(entity.getUUID())) {
                     SiftWorldStorage.clearReturnPoint(server, entity.getUUID());
                  }
               }
               if (request.riftScreen) {
                  for (ServerPlayer player : players) {
                     ModAdvancements.award(player, "the_sift/rifter");
                  }
               }

               PortalTransitGuard.block(moved);
               SiftTeleportManager.suppressUntilExit(moved);
               latchRiftExit(server, moved);
               if (!intoSift) {
                  moved.getSelfAndPassengers().forEach(
                     passenger -> SiftWorldStorage.clearReturnPoint(server, passenger.getUUID())
                  );
                  if (!moved.getUUID().equals(entity.getUUID())) {
                     SiftWorldStorage.clearReturnPoint(server, entity.getUUID());
                  }
                  if (origin != null && destination.exit != null) {
                     RiftDirectory.Exit exit = destination.exit;
                     origin.setLinkedPos(exit.pos(), exit.expires());
                     moved.getSelfAndPassengers().forEach(
                        e -> SiftWorldStorage.saveRiftRoute(server, e.getUUID(), exit.id(), origin.getAnchorPos(), origin.getExpiresAt())
                     );
                  }
               }

               for (ServerPlayer player : players) {
                  if (destination.fallback) {
                     player.sendSystemMessage(Component.translatable("message.the_sift.rift_no_exit").withStyle(ChatFormatting.RED));
                  }

                  if (request.closedPortal) {
                     player.sendSystemMessage(Component.translatable("message.the_sift.portal_closed"));
                  }
               }
            }
         } catch (RuntimeException failure) {
            TheSiftMod.LOGGER.error("Rift transfer failed", failure);
            if (entity.level().dimension().equals(TheSiftDimension.LEVEL_KEY) && !entity.isRemoved()) {
               RiftManager.Destination fallback = fallback(server, entity);
               Entity moved = moveToDestination(entity, fallback, Vec3.ZERO, TeleportTransition.PLACE_PORTAL_TICKET);
               if (moved != null) {
                  PortalTransitGuard.block(moved);
                  latchRiftExit(server, moved);
               }

               for (ServerPlayer player : players) {
                  player.sendSystemMessage(Component.translatable("message.the_sift.rift_no_exit").withStyle(ChatFormatting.RED));
               }
            }
         }
      } finally {
         if (!keepLoading) {
            EXIT_SEARCHES.remove(entity.getUUID());

            for (ServerPlayer player : players) {
               RiftLoadingPayload.send(player, false, request.riftScreen);
            }
         }
      }
   }

   private static RiftManager.Destination intoSift(MinecraftServer server, Entity entity, RiftEntity origin) {
      ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (sift == null) {
         return null;
      }

      BlockPos pos = SiftWorldStorage.getRiftRoute(server, entity.getUUID(), origin.getUUID(), server.overworld().getGameTime()).orElse(origin.getLinkedPos());
      if (pos != null) {
         Optional<Boolean> link = linkedRiftExists(sift, pos, origin);
         if (link.isEmpty()) {
            // Loading a chunk does not synchronously load its entity sections.
            // Keep the transfer pending rather than spawning a duplicate pair.
            EXIT_SEARCHES.computeIfAbsent(entity.getUUID(), id -> new RiftManager.ExitSearch(origin.getAnchorPos()));
            return null;
         }
         if (!link.get()) {
            // Coordinates alone do not prove the paired Rift still exists.
            pos = null;
         }
      }
      if (pos == null) {
         // A Rift must lead to another Rift, never to the main Sift portal.
         // Reuse the incremental loaded-chunk search already used for exits.
         RiftManager.ExitSearch search = EXIT_SEARCHES.computeIfAbsent(entity.getUUID(), id -> new RiftManager.ExitSearch(origin.getAnchorPos()));
         Optional<RiftManager.Placement> found = search.step(sift);
         if (found.isEmpty()) {
            if (search.complete()) {
               EXIT_SEARCHES.remove(entity.getUUID());
            }
            return null;
         }
         EXIT_SEARCHES.remove(entity.getUUID());
         long expiry = Math.max(sift.getGameTime() + 40L, origin.getExpiresAt());
         RiftEntity paired = spawn(sift, found.get(), false, expiry, origin.getPairId());
         if (paired == null) {
            return null;
         }
         paired.setLinkedPos(origin.getAnchorPos(), origin.getExpiresAt());
         origin.setLinkedPos(paired.getAnchorPos(), expiry);
         pos = paired.getAnchorPos();
      }

      load(sift, pos);
      return new RiftManager.Destination(sift, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), false);
   }

   private static Optional<Boolean> linkedRiftExists(ServerLevel level, BlockPos pos, RiftEntity origin) {
      // Newly spawned Rifts are already tracked only AFTER addFreshEntity
      // succeeds. They can be used immediately without waiting for the
      // persistence manager to mark their chunk's disk entity sections ready.
      if (RiftDirectory.loaded().stream().anyMatch(rift -> matchesLinkedRift(level, pos, origin, rift))) {
         return Optional.of(true);
      }
      load(level, pos);
      if (!level.areEntitiesLoaded(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4).pack())) {
         return Optional.empty();
      }
      // Use actual entities in the loaded section, not only RiftDirectory's
      // tick-populated cache (which can lag behind a freshly loaded chunk).
      return Optional.of(!level.getEntitiesOfClass(
         RiftEntity.class, new AABB(pos).inflate(3.0),
         rift -> matchesLinkedRift(level, pos, origin, rift)
      ).isEmpty());
   }

   private static boolean matchesLinkedRift(ServerLevel level, BlockPos pos, RiftEntity origin, RiftEntity rift) {
      return rift.level() == level && !rift.isRemoved() && !rift.isClosing()
         && rift.getExpiresAt() > level.getGameTime() + 20L
         && !rift.targetsSift() && rift.getPairId().equals(origin.getPairId())
         && rift.getAnchorPos().distSqr(pos) <= 2.0
         && origin.getAnchorPos().equals(rift.getLinkedPos());
   }

   private static RiftManager.Destination outOfSift(MinecraftServer server, Entity entity, RiftEntity origin) {
      ServerLevel overworld = server.overworld();
      BlockPos linkedOverworld = origin == null ? null : origin.getLinkedPos();
      BlockPos anchor = linkedOverworld == null ? overworldAnchor(server, entity) : linkedOverworld;
      long expiry = origin == null ? overworld.getGameTime() + 6000L : Math.max(overworld.getGameTime() + 20L, origin.getExpiresAt());
      Optional<RiftDirectory.Exit> existing = Optional.empty();
      boolean waitingForEntitySections = false;
      if (origin == null) {
         existing = RiftDirectory.near(anchor, overworld.getGameTime());
      } else {
         for (RiftDirectory.Exit candidate : RiftDirectory.nearby(anchor, overworld.getGameTime())) {
            if (RiftDirectory.loaded().stream().anyMatch(rift -> matchesOverworldRift(overworld, candidate, origin, rift))) {
               existing = Optional.of(candidate);
               break;
            }
            load(overworld, candidate.pos());
            BlockPos candidatePos = candidate.pos();
            if (!overworld.areEntitiesLoaded(new ChunkPos(candidatePos.getX() >> 4, candidatePos.getZ() >> 4).pack())) {
               waitingForEntitySections = true;
               break;
            }
            if (!overworld.getEntitiesOfClass(RiftEntity.class, new AABB(candidatePos).inflate(3.0), rift ->
               matchesOverworldRift(overworld, candidate, origin, rift)
            ).isEmpty()) {
               existing = Optional.of(candidate);
               break;
            }
         }
      }
      if (waitingForEntitySections) {
         EXIT_SEARCHES.computeIfAbsent(entity.getUUID(), id -> new RiftManager.ExitSearch(anchor));
         return null;
      }
      RiftDirectory.Exit exit;
      if (existing.isPresent()) {
         exit = RiftDirectory.extend(existing.get(), expiry);
         load(overworld, exit.pos());
      } else {
         RiftManager.ExitSearch search = EXIT_SEARCHES.computeIfAbsent(entity.getUUID(), id -> new RiftManager.ExitSearch(anchor));
         Optional<RiftManager.Placement> found = search.step(overworld);
         if (found.isEmpty()) {
            if (!search.complete()) {
               return null;
            }

            EXIT_SEARCHES.remove(entity.getUUID());
            return fallback(server, entity);
         }

         EXIT_SEARCHES.remove(entity.getUUID());
         RiftEntity rift = spawn(overworld, found.get(), true, expiry, origin == null ? UUID.randomUUID() : origin.getPairId());
         if (rift == null) {
            return null;
         }
         if (origin != null) {
            rift.setLinkedPos(origin.getAnchorPos(), origin.getExpiresAt());
         }

         RiftDirectory.add(rift, anchor);
         exit = new RiftDirectory.Exit(rift.getUUID(), rift.getAnchorPos(), anchor, expiry);
      }

      return new RiftManager.Destination(overworld, new Vec3(exit.pos().getX() + 0.5, exit.pos().getY(), exit.pos().getZ() + 0.5), false, origin == null ? null : exit);
   }

   private static boolean matchesOverworldRift(ServerLevel overworld, RiftDirectory.Exit candidate, RiftEntity origin, RiftEntity rift) {
      return rift.level() == overworld && rift.getUUID().equals(candidate.id())
         && !rift.isRemoved() && !rift.isClosing()
         && rift.getExpiresAt() > overworld.getGameTime() + 20L
         && rift.targetsSift() && rift.getPairId().equals(origin.getPairId())
         && candidate.pos().equals(rift.getAnchorPos())
         && origin.getAnchorPos().equals(rift.getLinkedPos());
   }

   private static void load(ServerLevel level, BlockPos pos) {
      level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), 3);
      level.getChunk(pos);
   }

   public static BlockPos overworldAnchor(MinecraftServer server, Entity entity) {
      if (entity instanceof ServerPlayer player && player.getRespawnConfig() != null) {
         RespawnData data = player.getRespawnConfig().respawnData();
         if (data.dimension().equals(Level.OVERWORLD)) {
            return data.pos();
         }
      }

      RespawnData data = server.overworld().getRespawnData();
      return data.dimension().equals(Level.OVERWORLD) ? data.pos() : new BlockPos(0, server.overworld().getSeaLevel(), 0);
   }

   private static RiftManager.Destination fallback(MinecraftServer server, Entity entity) {
      ServerLevel level = server.overworld();
      BlockPos anchor = overworldAnchor(server, entity);
      load(level, anchor);
      BlockPos safe = entity.adjustSpawnLocation(level, anchor);
      return new RiftManager.Destination(level, new Vec3(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5), true);
   }

   private static Entity moveToDestination(Entity entity, RiftManager.Destination destination, Vec3 motion, PostTeleportTransition postTransition) {
      Optional<Vec3> safe = SafePortalArrival.find(destination.level, entity, destination.pos);
      if (safe.isEmpty()) {
         return null;
      }

      destination = new RiftManager.Destination(destination.level, safe.get(), destination.fallback);
      if (destination.fallback && entity instanceof ItemEntity item) {
         if (item.getItem().isEmpty()) {
            return null;
         }

         ItemEntity returned = new ItemEntity(destination.level, destination.pos.x, destination.pos.y, destination.pos.z, item.getItem().copy(), 0.0, 0.0, 0.0);
         returned.setDefaultPickUpDelay();
         if (!destination.level.addFreshEntity(returned)) {
            return null;
         }

         item.discard();
         return returned;
      } else {
         return entity.teleport(new TeleportTransition(destination.level, destination.pos, motion, entity.getYRot(), entity.getXRot(), postTransition));
      }
   }

   public static boolean tryPlaceCreativeRift(ServerLevel level, BlockPos pos, boolean alongX) {
      if (!space(level, pos, alongX)) {
         return false;
      }

      RiftEntity rift = spawn(
         level, new RiftManager.Placement(pos, alongX), !level.dimension().equals(TheSiftDimension.LEVEL_KEY), level.getGameTime() + 6000L, UUID.randomUUID()
      );
      if (rift == null) {
         return false;
      }
      if (level.dimension().equals(Level.OVERWORLD)) {
         RiftDirectory.add(rift, pos);
      }

      return true;
   }

   private static RiftEntity spawn(ServerLevel level, RiftManager.Placement p, boolean target, long expiry, UUID pair) {
      RiftEntity rift = new RiftEntity(ModEntities.RIFT, level);
      rift.configure(target, p.alongX, expiry, pair);
      rift.setPos(p.base.getX() + 0.5, p.base.getY() + 1, p.base.getZ() + 0.5);
      rift.setBoundingBox(rift.getPortalBounds());
      if (!level.addFreshEntity(rift)) {
         return null;
      }
      RiftDirectory.track(rift);
      return rift;
   }

   private static boolean plant(BlockState state) {
      return state.getFluidState().isEmpty() && !(state.getBlock() instanceof LeavesBlock)
         ? state.getBlock() instanceof VegetationBlock || state.getBlock() instanceof VineBlock || state.getBlock() instanceof HangingRootsBlock
         : false;
   }

   private static boolean space(ServerLevel level, BlockPos base, boolean alongX) {
      if (!level.hasChunkAt(base)) {
         return false;
      }

      if (base.getY() >= level.getMinY() && base.getY() + 5 < level.getMaxY()) {
         boolean supported = false;

         for (int gap = 1; gap <= 3; gap++) {
            BlockState under = level.getBlockState(base.below(gap));
            if (!under.isAir() && !plant(under)) {
               supported = true;
               break;
            }
         }

         if (!supported) {
            return false;
         }

         for (int a = -4; a <= 4; a++) {
            for (int y = 0; y < 5; y++) {
               BlockPos p = alongX ? base.offset(a, y, 0) : base.offset(0, y, a);
               if (!level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p)) {
                  return false;
               }

               BlockState state = level.getBlockState(p);
               if (!state.isAir() && !plant(state)) {
                  return false;
               }
            }
         }

         AABB volume = alongX
            ? new AABB(base.getX() - 4, base.getY(), base.getZ(), base.getX() + 5, base.getY() + 5, base.getZ() + 1)
            : new AABB(base.getX(), base.getY(), base.getZ() - 4, base.getX() + 1, base.getY() + 5, base.getZ() + 5);
         return level.getEntities((Entity)null, volume, e -> !e.isRemoved()).isEmpty();
      } else {
         return false;
      }
   }

   private static boolean columnReady(ServerLevel level, int x, int z) {
      for (int cx = x - 4 >> 4; cx <= x + 4 >> 4; cx++) {
         for (int cz = z - 4 >> 4; cz <= z + 4 >> 4; cz++) {
            if (level.getChunkSource().getChunkNow(cx, cz) == null) {
               return false;
            }
         }
      }

      return true;
   }

   private static Optional<RiftManager.Placement> column(ServerLevel level, int x, int z, boolean axis) {
      if (!columnReady(level, x, z)) {
         return Optional.empty();
      }

      int surface = level.getHeight(Types.MOTION_BLOCKING, x, z);

      for (int rise = 0; rise <= 2; rise++) {
         BlockPos base = new BlockPos(x, surface + rise, z);
         if (space(level, base, axis)) {
            return Optional.of(new RiftManager.Placement(base, axis));
         }

         if (space(level, base, !axis)) {
            return Optional.of(new RiftManager.Placement(base, !axis));
         }
      }

      return Optional.empty();
   }

   private static Optional<RiftManager.Placement> findNatural(ServerLevel level, BlockPos center, RandomSource random) {
      for (int i = 0; i < 48; i++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         int distance = 20 + random.nextInt(45);
         Optional<RiftManager.Placement> found = column(
            level, center.getX() + (int)(Math.cos(angle) * distance), center.getZ() + (int)(Math.sin(angle) * distance), random.nextBoolean()
         );
         if (found.isPresent()) {
            return found;
         }
      }

      return Optional.empty();
   }

   public static boolean axisFromFacing(Direction facing) {
      return facing.getAxis() == Axis.Z;
   }

   public static void clearTransientState() {
      NEXT_SPAWN.clear();
      PENDING.clear();
      EXIT_SEARCHES.clear();
      RIFT_EXIT_LATCHES.clear();
      RiftDirectory.clear();
      PortalTransitGuard.clear();
      RiftLightCleanup.clear();
   }

   private record Destination(ServerLevel level, Vec3 pos, boolean fallback, RiftDirectory.Exit exit) {
      private Destination(ServerLevel level, Vec3 pos, boolean fallback) {
         this(level, pos, fallback, null);
      }
   }

   private static final class ExitSearch {
      final BlockPos anchor;
      int next;

      ExitSearch(BlockPos anchor) {
         this.anchor = anchor.immutable();
      }

      boolean complete() {
         return this.next >= 257;
      }

      Optional<RiftManager.Placement> step(ServerLevel level) {
         for (int budget = 16; budget > 0 && !this.complete(); budget--) {
            int index = this.next;
            int x = this.anchor.getX();
            int z = this.anchor.getZ();
            boolean axis = true;
            if (index > 0) {
               int ring = (index - 1) / 16 + 1;
               int radius = ring <= 4 ? ring * 4 : (ring - 3) * 16;
               int direction = (index - 1) % 16;
               double angle = (direction + radius * 0.017) * Math.PI / 8.0;
               x += (int)Math.round(Math.cos(angle) * radius);
               z += (int)Math.round(Math.sin(angle) * radius);
               axis = (direction & 1) == 0;
            }

            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, this.anchor.getY(), z))) {
               this.next++;
            } else {
               if (!RiftManager.columnReady(level, x, z)) {
                  level.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(x >> 4, z >> 4), 1);
                  return Optional.empty();
               }

               this.next++;
               Optional<RiftManager.Placement> result = RiftManager.column(level, x, z, axis);
               if (result.isPresent()) {
                  return result;
               }
            }
         }

         return Optional.empty();
      }
   }

   private record Placement(BlockPos base, boolean alongX) {
   }

   private record Request(Entity entity, RiftEntity origin, boolean closedPortal, boolean riftScreen) {
   }

   private record RiftArrival(ResourceKey<Level> dimension, Vec3 pos, long cooldownUntil) {
   }
}
