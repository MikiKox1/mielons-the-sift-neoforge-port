package mielon.thesift.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.portal.PortalFrameScanner;
import mielon.thesift.portal.PortalGrowth;
import mielon.thesift.world.LoadedBlockSearch;
import mielon.thesift.platform.NeoEvents.ServerLifecycleEvents;
import mielon.thesift.platform.NeoEvents.ServerTickEvents;
import mielon.thesift.platform.NeoEvents.ServerLifecycleEvents.ServerStopped;
import mielon.thesift.platform.NeoEvents.ServerTickEvents.EndTick;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SingerSummoner {
   private static final int FRAME_RADIUS = 40;
   private static final int SONOROUS_RADIUS = 20;
   private static final double EXISTING_SINGER_RADIUS = 2.0;
   private static final int SUMMON_DELAY_TICKS = 60;
   private static final List<SingerSummoner.PendingSummon> PENDING = new ArrayList<>();

   private SingerSummoner() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> tick());
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> PENDING.clear());
   }

   public static boolean onGoatHornFinished(ServerLevel level, Player player) {
      Optional<PortalFrameScanner.Frame> found = findNearestFrame(level, player.position());
      if (found.isEmpty()) {
         return false;
      }

      PortalFrameScanner.Frame frame = found.get();
      PortalGrowth.startDormant(level, frame.interior(), frame.axis());
      if (PENDING.stream().noneMatch(p -> p.level == level && p.frame.bottomCenter().equals(frame.bottomCenter()))) {
         PENDING.add(new SingerSummoner.PendingSummon(level, frame));
      }

      return true;
   }

   public static boolean isNearAncientCityCenter(ServerLevel level, Vec3 position) {
      return findNearestFrame(level, position).isPresent();
   }

   private static void tick() {
      Iterator<SingerSummoner.PendingSummon> iterator = PENDING.iterator();

      while (iterator.hasNext()) {
         SingerSummoner.PendingSummon pending = iterator.next();
         if (pending.tick()) {
            iterator.remove();
         }
      }
   }

   private static void trySummon(ServerLevel level, PortalFrameScanner.Frame frame) {
      if (!frame.interior().isEmpty() && !frame.interior().stream().anyMatch(p -> !level.hasChunkAt(p))) {
         Optional<PortalFrameScanner.Frame> checked = PortalFrameScanner.scan(level, frame.interior().get(0).below());
         if (!checked.isEmpty() && checked.get().bottomCenter().equals(frame.bottomCenter())) {
            Vec3 spawnPos = frame.bottomCenter();
            BlockPos portalCenter = BlockPos.containing(spawnPos);
            if (!hasSingerForPortal(level, portalCenter, spawnPos)) {
               BlockPos sonorous = findNearestSonorous(level, spawnPos);
               Direction facing = getCardinalDirection(spawnPos, sonorous);
               SingerEntity singer = new SingerEntity(ModEntities.SINGER, level);
               singer.setPortalCenter(portalCenter);
               singer.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
               float yaw = facing.toYRot();
               singer.setYRot(yaw);
               singer.setYBodyRot(yaw);
               singer.yBodyRotO = yaw;
               singer.setYHeadRot(yaw);
               singer.yHeadRotO = yaw;
               singer.beginSequence();
               level.addFreshEntity(singer);
            }
         }
      }
   }

   private static boolean hasSingerForPortal(ServerLevel level, BlockPos portalCenter, Vec3 spawnPos) {
      double radius = 2.0;
      AABB box = new AABB(spawnPos.x - radius, spawnPos.y - radius, spawnPos.z - radius, spawnPos.x + radius, spawnPos.y + radius, spawnPos.z + radius);

      for (SingerEntity singer : level.getEntitiesOfClass(SingerEntity.class, box)) {
         BlockPos singerPortal = singer.getPortalCenter();
         if (portalCenter.equals(singerPortal)) {
            return true;
         }
      }

      return false;
   }

   private static Optional<PortalFrameScanner.Frame> findNearestFrame(ServerLevel level, Vec3 origin) {
      if (!level.dimension().equals(Level.OVERWORLD)) {
         return Optional.empty();
      }

      Optional<List<BlockPos>> positions = LoadedBlockSearch.find(level, BlockPos.containing(origin), 40, state -> state.is(Blocks.REINFORCED_DEEPSLATE));
      if (positions.isEmpty()) {
         return Optional.empty();
      }

      PortalFrameScanner.Frame best = null;
      double bestDistance = 1600.0;
      Set<Vec3> seenCenters = new HashSet<>();

      for (BlockPos pos : positions.get()) {
         if (!(Vec3.atCenterOf(pos).distanceToSqr(origin) > 1600.0)) {
            Optional<PortalFrameScanner.Frame> candidate = PortalFrameScanner.scan(level, pos);
            if (!candidate.isEmpty()) {
               PortalFrameScanner.Frame frame = candidate.get();
               if (seenCenters.add(frame.bottomCenter())) {
                  double distance = frame.bottomCenter().distanceToSqr(origin);
                  if (distance <= bestDistance) {
                     bestDistance = distance;
                     best = frame;
                  }
               }
            }
         }
      }

      return Optional.ofNullable(best);
   }

   private static BlockPos findNearestSonorous(ServerLevel level, Vec3 origin) {
      return LoadedBlockSearch.find(level, BlockPos.containing(origin), 20, state -> state.is(ModBlocks.SONOROUS_DEEPSLATE))
         .orElse(List.of())
         .stream()
         .filter(pos -> Vec3.atCenterOf(pos).distanceToSqr(origin) <= 400.0)
         .min(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(origin)))
         .orElse(null);
   }

   private static Direction getCardinalDirection(Vec3 origin, BlockPos target) {
      if (target == null) {
         return Direction.SOUTH;
      } else {
         double dx = target.getX() + 0.5 - origin.x;
         double dz = target.getZ() + 0.5 - origin.z;
         if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0.0 ? Direction.EAST : Direction.WEST;
         } else {
            return dz >= 0.0 ? Direction.SOUTH : Direction.NORTH;
         }
      }
   }

   private static final class PendingSummon {
      private final ServerLevel level;
      private final PortalFrameScanner.Frame frame;
      private int ticksLeft = 60;

      PendingSummon(ServerLevel level, PortalFrameScanner.Frame frame) {
         this.level = level;
         this.frame = frame;
      }

      boolean tick() {
         if (this.ticksLeft > 0) {
            this.ticksLeft--;
            return false;
         } else {
            SingerSummoner.trySummon(this.level, this.frame);
            return true;
         }
      }
   }
}
