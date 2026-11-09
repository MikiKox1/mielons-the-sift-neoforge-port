package mielon.thesift.world;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mielon.thesift.TheSiftMod;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import mielon.thesift.portal.PortalFrameScanner;
import mielon.thesift.network.RiftLoadingPayload;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.worldgen.SiftAllaySpawner;
import mielon.thesift.worldgen.SiftCreatureSpawner;
import mielon.thesift.worldgen.SiftDeferredSnifferSpawner;
import mielon.thesift.worldgen.SiftEchoGolemSpawner;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import mielon.thesift.worldgen.SiftPortalVegetation;
import mielon.thesift.worldgen.SiftSoulCanyonParticles;
import mielon.thesift.worldgen.SiftSpawnBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SiftTeleportManager {
   private static final Set<UUID> WAITING_FOR_EXIT = new HashSet<>();
   private static final Set<UUID> PORTAL_GUARD_CHECKED = new HashSet<>();
   private static final Set<UUID> INITIALIZED_PLAYERS = new HashSet<>();
   private static final Set<UUID> PORTAL_CONTACTS = new HashSet<>();
   private static final Map<UUID, PlayerContact> PLAYER_CONTACTS = new HashMap<>();
   private static int cleanupCooldown;

   private SiftTeleportManager() {
   }

   public static void tick(MinecraftServer server) {
      SiftLandmarkTracker.tick(server);
      SiftPortalVegetation.tick(server);
      SiftSoulCanyonParticles.tick(server);
      ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (sift != null && sift.getGameRules().get(GameRules.SPAWN_MOBS)) {
         SiftDeferredSnifferSpawner.tick(server);
         SiftEchoGolemSpawner.tick(server);
         SiftAllaySpawner.tick(server);
         SiftCreatureSpawner.tick(server);
      }

      ServerLevel overworld = server.getLevel(Level.OVERWORLD);
      if (overworld != null && sift != null) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            processPlayer(server, player);
         }

         processPortalContacts(server, overworld);
         releaseNonPlayerGuards(server, overworld);
         if (--cleanupCooldown <= 0) {
            cleanupCooldown = 600;
            WAITING_FOR_EXIT.removeIf(id -> overworld.getEntityInAnyDimension(id) == null);
            PORTAL_GUARD_CHECKED.removeIf(id -> overworld.getEntityInAnyDimension(id) == null);
            INITIALIZED_PLAYERS.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
            PLAYER_CONTACTS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
         }
      }
   }

   public static void playFirstIntro(ServerPlayer player) {
      MinecraftServer server = player.level().getServer();
      if (server != null && player.level().dimension().equals(TheSiftDimension.LEVEL_KEY) && SiftWorldStorage.claimFirstSiftEntry(server, player.getUUID())) {
         player.connection
            .send(
               new ClientboundSoundEntityPacket(
                  BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SIFT_INTRO), SoundSource.AMBIENT, player, 0.4F, 1.0F, player.getRandom().nextLong()
               )
            );
      }
   }

   public static void clearTransientState() {
      WAITING_FOR_EXIT.clear();
      PORTAL_GUARD_CHECKED.clear();
      INITIALIZED_PLAYERS.clear();
      PORTAL_CONTACTS.clear();
      PLAYER_CONTACTS.clear();
      cleanupCooldown = 0;
      SiftDeferredSnifferSpawner.clear();
      SiftSpawnBudget.clear();
      SiftLandmarkTracker.clearTransientState();
      SiftSoulCanyonParticles.clear();
      SiftEchoGolemSpawner.clear();
      SiftAllaySpawner.clear();
      SiftCreatureSpawner.clear();
   }

   public static void suppressUntilExit(Entity entity) {
      WAITING_FOR_EXIT.add(entity.getUUID());
      PORTAL_GUARD_CHECKED.add(entity.getUUID());
      SiftWorldStorage.markPortalExitRequired(entity);
      PortalTransitGuard.block(entity);
   }

   private static void processPlayer(MinecraftServer server, ServerPlayer player) {
      PlayerContact contact = PLAYER_CONTACTS.remove(player.getUUID());
      if (player.isAlive()) {
         boolean touchingPortal = isTouchingPortal(player);
         UUID id = player.getUUID();
         if (player.isSpectator()) {
            if (touchingPortal) {
               WAITING_FOR_EXIT.add(id);
            }
         } else if (INITIALIZED_PLAYERS.add(id)) {
            if (touchingPortal) {
               suppressUntilExit(player);
            }
         } else {
            BlockPos crossedPortal = contact != null && contact.level() == player.level() ? contact.portal() : null;
            processTouchState(server, player, touchingPortal, crossedPortal);
         }
      }
   }

   public static void onPortalContact(Entity entity) {
      if (!(entity instanceof ServerPlayer) && !entity.isRemoved() && entity.isAlive() && entity.level() instanceof ServerLevel) {
         PORTAL_CONTACTS.add(entity.getUUID());
      }
   }

   private static void processPortalContacts(MinecraftServer server, ServerLevel overworld) {
      if (!PORTAL_CONTACTS.isEmpty()) {
         Set<UUID> contacts = new HashSet<>(PORTAL_CONTACTS);
         PORTAL_CONTACTS.clear();

         for (UUID id : contacts) {
            Entity entity = overworld.getEntityInAnyDimension(id);
            if (entity != null && !(entity instanceof ServerPlayer) && !entity.isRemoved() && entity.isAlive()) {
               processTouchState(server, entity, true, null);
            }
         }
      }
   }

   private static void releaseNonPlayerGuards(MinecraftServer server, ServerLevel overworld) {
      Iterator<UUID> iterator = WAITING_FOR_EXIT.iterator();

      while (iterator.hasNext()) {
         UUID id = iterator.next();
         Entity entity = overworld.getEntityInAnyDimension(id);
         if (entity != null
            && !(entity instanceof ServerPlayer)
            && !PortalTransitGuard.isBlocked(entity)
            && !isTouchingPortal(entity)
            && !RiftManager.isTouchingRift(entity)) {
            iterator.remove();
            SiftWorldStorage.clearPortalExitRequired(server, id);
         }
      }
   }

   private static void processTouchState(MinecraftServer server, Entity entity, boolean touchingPortal, BlockPos crossedPortal) {
      UUID id = entity.getUUID();
      if (PORTAL_GUARD_CHECKED.add(id) && SiftWorldStorage.requiresPortalExit(server, id)) {
         WAITING_FOR_EXIT.add(id);
      }

      if (!PortalTransitGuard.isBlocked(entity)) {
         if (WAITING_FOR_EXIT.contains(id)) {
            if (!touchingPortal) {
               WAITING_FOR_EXIT.remove(id);
               SiftWorldStorage.clearPortalExitRequired(server, id);
            }
         } else if (touchingPortal || crossedPortal != null) {
            SiftTeleportManager.ResourceKeyMatch dimension = classifyDimension(entity.level());
            if (dimension == SiftTeleportManager.ResourceKeyMatch.OVERWORLD || dimension == SiftTeleportManager.ResourceKeyMatch.THE_SIFT) {
               ServerPlayer player = entity instanceof ServerPlayer serverPlayer ? serverPlayer : null;
               if (player != null) {
                  RiftLoadingPayload.send(player, true, false);
               }

               boolean transferring = false;

               try {
                  transferring = dimension == SiftTeleportManager.ResourceKeyMatch.OVERWORLD
                     ? teleportIntoSift(server, entity, crossedPortal)
                     : teleportBackToOverworld(server, entity);
               } finally {
                  if (player != null && (!transferring || classifyDimension(player.level()) != dimension)) {
                     RiftLoadingPayload.send(player, false, false);
                  }
               }
            }
         }
      }
   }

   private static boolean teleportIntoSift(MinecraftServer server, Entity entity, BlockPos crossedPortal) {
      ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (sift == null) {
         return false;
      }

      Optional<BlockPos> anchorOptional = SiftPortalStructure.ensurePortal(server, sift);
      if (anchorOptional.isEmpty()) {
         return false;
      }

      BlockPos anchor = anchorOptional.get();
      Vec3 destination = new Vec3(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
      Optional<Vec3> safe = SafePortalArrival.findForEntry(sift, entity, destination);
      if (safe.isEmpty()) {
         return false;
      }

      // Capture source positions before teleport, but commit them only after
      // the destination accepted the actual traveler and its passengers.
      Map<UUID, SiftWorldStorage.ReturnPoint> entranceTickets = new LinkedHashMap<>();
      entity.getSelfAndPassengers().forEach(traveler ->
         entranceTickets.put(traveler.getUUID(), SiftWorldStorage.captureReturnPoint(traveler, crossedPortal))
      );
      Entity teleported = teleport(entity, sift, safe.get(), entity.getYRot(), entity.getXRot());
      if (teleported != null) {
         Set<UUID> movedIds = new HashSet<>();
         teleported.getSelfAndPassengers().forEach(passenger -> movedIds.add(passenger.getUUID()));
         entranceTickets.forEach((id, point) -> {
            if (movedIds.contains(id)) {
               SiftWorldStorage.saveReturnPoint(server, id, point);
            }
         });
         suppressUntilExit(teleported);
      }

      return teleported != null;
   }

   private static boolean teleportBackToOverworld(MinecraftServer server, Entity entity) {
      ServerLevel overworld = server.getLevel(Level.OVERWORLD);
      if (overworld == null) {
         return false;
      }

      UUID entityId = entity.getUUID();
      UUID ticketOwner = entityId;
      Optional<SiftWorldStorage.ReturnPoint> immediate = Optional.empty();
      Optional<SiftWorldStorage.ReturnPoint> durable = Optional.empty();
      // A vehicle can touch the portal while its player/passenger does not.
      // Prefer the root's existing route, otherwise use the first rider's
      // valid portal entrance rather than sending everyone to world spawn.
      for (Entity traveler : entity.getSelfAndPassengers().toList()) {
         immediate = SiftWorldStorage.getReturnPoint(server, traveler.getUUID());
         durable = immediate.isPresent()
            ? Optional.empty() : SiftWorldStorage.getMainPortalEntrance(server, traveler.getUUID());
         if (immediate.isPresent() || durable.isPresent()) {
            ticketOwner = traveler.getUUID();
            break;
         }
      }
      Optional<SiftWorldStorage.ReturnPoint> stored = immediate.isPresent() ? immediate : durable;
      if (stored.isPresent()) {
         SiftWorldStorage.ReturnPoint point = stored.get();
         BlockPos entrance = point.portalBlock();
         overworld.getChunk(entrance);
         Optional<BlockPos> activeEntrance = findActiveEntranceNearby(overworld, entrance);
         Vec3 target = activeEntrance.isPresent() && !activeEntrance.get().equals(entrance)
            ? Vec3.atBottomCenterOf(activeEntrance.get())
            : new Vec3(point.x(), point.y(), point.z());
         Optional<Vec3> safe = SafePortalArrival.findNearRecordedPortal(overworld, entity, target);
         if (safe.isEmpty()) {
            // An obstructed *underground* entrance must never silently turn
            // into a surface spawn. Keep the traveler in Sift and let them
            // step out of the portal to retry after the blockage is removed.
            TheSiftMod.LOGGER.warn(
               "Sift MAIN return: no safe landing within saved portal level entity={} originalPortal={} active={}",
               entityId, entrance, activeEntrance.isPresent()
            );
            PortalTransitGuard.block(entity);
            return false;
         }

         TheSiftMod.LOGGER.info(
            "Sift MAIN return entity={} ticketOwner={} source={} savedPortal={} activePortal={} selectedDestination={}",
            entityId, ticketOwner, immediate.isPresent() ? "one_shot" : "durable", entrance, activeEntrance.orElse(null), safe.get()
         );
         Entity teleported = teleport(entity, overworld, safe.get(), point.yaw(), point.pitch());
         if (teleported != null) {
            if (activeEntrance.isPresent()) {
               suppressUntilExit(teleported);
            } else {
               PortalTransitGuard.block(teleported);
            }
            // A vehicle and each actually transported passenger can have a
            // separate one-shot ticket. Keep their durable main entrances.
            teleported.getSelfAndPassengers().forEach(
               passenger -> SiftWorldStorage.clearReturnPoint(server, passenger.getUUID())
            );
            if (!teleported.getUUID().equals(entityId)) {
               SiftWorldStorage.clearReturnPoint(server, entityId);
            }
         }

         return teleported != null;
      } else {
         // No historical main-portal entrance can be reconstructed; do not
         // fabricate an unrelated Rift route or overwrite a player's Rift pair.
         BlockPos spawn = RiftManager.overworldAnchor(server, entity);
         overworld.getChunk(spawn);
         Optional<Vec3> safe = SafePortalArrival.find(overworld, entity, Vec3.atBottomCenterOf(entity.adjustSpawnLocation(overworld, spawn)));
         if (safe.isEmpty()) {
            TheSiftMod.LOGGER.error("Sift MAIN return: no saved entrance and no safe Overworld spawn for entity={}", entityId);
            return false;
         }
         TheSiftMod.LOGGER.warn("Sift MAIN return entity={} source=no_data; direct Overworld spawn={}", entityId, safe.get());
         Entity moved = teleport(entity, overworld, safe.get(), entity.getYRot(), entity.getXRot());
         if (moved != null) {
            PortalTransitGuard.block(moved);
         }
         return moved != null;
      }
   }

   private static Optional<BlockPos> findActiveEntranceNearby(ServerLevel level, BlockPos saved) {
      if (SiftPortalBlock.isActivePortal(level.getBlockState(saved))) {
         return Optional.of(saved);
      }
      // Only recover an adjacent interior cell of the same opening. A broad
      // nearest-portal scan could redirect a player to a different nearby frame.
      for (int chunkX = (saved.getX() - 1) >> 4; chunkX <= (saved.getX() + 1) >> 4; chunkX++) {
         for (int chunkZ = (saved.getZ() - 1) >> 4; chunkZ <= (saved.getZ() + 1) >> 4; chunkZ++) {
            level.getChunk(chunkX, chunkZ);
         }
      }
      BlockPos nearest = null;
      double nearestDistance = Double.POSITIVE_INFINITY;
      for (BlockPos candidate : BlockPos.betweenClosed(saved.offset(-1, -1, -1), saved.offset(1, 1, 1))) {
         if (candidate.getY() < level.getMinY() || candidate.getY() >= level.getMaxY() || !level.getWorldBorder().isWithinBounds(candidate)) {
            continue;
         }
         int manhattan = Math.abs(candidate.getX() - saved.getX())
            + Math.abs(candidate.getY() - saved.getY()) + Math.abs(candidate.getZ() - saved.getZ());
         if (manhattan == 1 && SiftPortalBlock.isActivePortal(level.getBlockState(candidate))
            && belongsToOriginalFrame(level, saved, candidate)) {
            double distance = candidate.distSqr(saved);
            if (distance < nearestDistance) {
               nearest = candidate.immutable();
               nearestDistance = distance;
            }
         }
      }
      return Optional.ofNullable(nearest);
   }

   private static boolean belongsToOriginalFrame(ServerLevel level, BlockPos saved, BlockPos active) {
      BlockState portal = level.getBlockState(active);
      for (int dy = 1; dy <= 23 && active.getY() - dy >= level.getMinY(); dy++) {
         BlockPos below = active.below(dy);
         BlockState state = level.getBlockState(below);
         if (state.is(Blocks.REINFORCED_DEEPSLATE)) {
            return PortalFrameScanner.scanForClosing(level, below)
               .filter(frame -> frame.axis() == portal.getValue(SiftPortalBlock.AXIS))
               .filter(frame -> frame.interior().contains(saved) && frame.interior().contains(active))
               .isPresent();
         }
         if (!state.is(ModBlocks.SIFT_PORTAL) && !PortalFrameScanner.isAirLikeInterior(level, below)) {
            break;
         }
      }
      return false;
   }

   private static Entity teleport(Entity entity, ServerLevel destinationLevel, Vec3 destination, float yaw, float pitch) {
      return entity.teleport(
         new TeleportTransition(
            destinationLevel, destination, Vec3.ZERO, yaw, pitch, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET)
         )
      );
   }

   public static boolean isTouchingPortal(Entity entity) {
      return findPortalContact(entity, null) != null;
   }

   private static SiftTeleportManager.ResourceKeyMatch classifyDimension(Level level) {
      if (level.dimension().equals(Level.OVERWORLD)) {
         return SiftTeleportManager.ResourceKeyMatch.OVERWORLD;
      } else {
         return level.dimension().equals(TheSiftDimension.LEVEL_KEY)
            ? SiftTeleportManager.ResourceKeyMatch.THE_SIFT
            : SiftTeleportManager.ResourceKeyMatch.OTHER;
      }
   }

   private enum ResourceKeyMatch {
      OVERWORLD,
      THE_SIFT,
      OTHER;
   }

   public static void onPlayerMovement(ServerPlayer player, Vec3 from) {
      if (PortalCrossing.isUsableMove(from, player.position())
         && !player.isSpectator()
         && player.isAlive()
         && classifyDimension(player.level()) != SiftTeleportManager.ResourceKeyMatch.OTHER
         && !PLAYER_CONTACTS.containsKey(player.getUUID())) {
         BlockPos portal = findPortalContact(player, from);
         if (portal != null) {
            PLAYER_CONTACTS.put(player.getUUID(), new SiftTeleportManager.PlayerContact(player.level(), portal));
         }
      }
   }

   private record PlayerContact(Level level, BlockPos portal) {
   }

   private static BlockPos findPortalContact(Entity entity, Vec3 from) {
      AABB box = entity.getBoundingBox().deflate(1.0E-7);
      if (from != null) {
         box = box.expandTowards(from.subtract(entity.position()));
      }

      Level level = entity.level();
      int minX = Mth.floor(box.minX);
      int minY = Mth.floor(box.minY);
      int minZ = Mth.floor(box.minZ);
      int maxX = Mth.floor(box.maxX);
      int maxY = Mth.floor(box.maxY);
      int maxZ = Mth.floor(box.maxZ);
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            ChunkAccess chunk = (ChunkAccess)(level instanceof ServerLevel serverLevel
               ? serverLevel.getChunkSource().getChunkNow(x >> 4, z >> 4)
               : level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false));
            if (chunk != null) {
               for (int y = minY; y <= maxY; y++) {
                  pos.set(x, y, z);
                  BlockState state = chunk.getBlockState(pos);
                  if (state.is(ModBlocks.SIFT_PORTAL)
                     && (from == null ? SiftPortalBlock.intersectsPortal(entity, pos, state) : SiftPortalBlock.crossesPortal(entity, pos, state, from))) {
                     return pos.immutable();
                  }
               }
            }
         }
      }

      return null;
   }
}
