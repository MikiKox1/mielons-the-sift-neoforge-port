package mielon.thesift.worldgen;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import mielon.thesift.entity.NesterEntity;
import mielon.thesift.entity.PollinatorEntity;
import mielon.thesift.entity.SeedlingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.NaturalSpawner.SpawnState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

public final class SiftSpawnBudget {
   private static final Map<ServerLevel, SiftSpawnBudget.Population> POPULATIONS = new WeakHashMap<>();

   private SiftSpawnBudget() {
   }

   public static void invalidate(ServerLevel level) {
      POPULATIONS.remove(level);
   }

   public static void clear() {
      POPULATIONS.clear();
   }

   private static SiftSpawnBudget.Population population(ServerLevel level) {
      SiftSpawnBudget.Population result = POPULATIONS.get(level);
      if (result != null && result.tick == level.getGameTime()) {
         return result;
      }

      result = new SiftSpawnBudget.Population();
      result.tick = level.getGameTime();

      for (Entity entity : level.getAllEntities()) {
         MobCategory category = entity.getType().getCategory();
         if (countsForCap(entity, category)) {
            result.global.merge(category, 1, Integer::sum);
         }
      }

      POPULATIONS.put(level, result);
      return result;
   }

   public static boolean enabled(ServerLevel level) {
      return level.getGameRules().get(GameRules.SPAWN_MOBS);
   }

   public static int scaledCap(MobCategory category, int spawnableChunks) {
      return Math.max(0, category.getMaxInstancesPerChunk() * spawnableChunks / 289);
   }

   public static boolean safeNaturalPosition(ServerLevel level, BlockPos pos) {
      if (enabled(level) && level.hasChunkAt(pos)) {
         if (SiftSculkBlobResidents.blocksNaturalSpawn(level, pos)) {
            return false;
         }

         boolean inRange = false;

         for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) {
               double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
               if (distance <= 576.0) {
                  return false;
               }

               if (distance <= 16384.0) {
                  inRange = true;
               }
            }
         }

         return inRange;
      } else {
         return false;
      }
   }

   private static boolean countsForCap(Entity entity, MobCategory category) {
      return entity instanceof Mob mob
         && mob.isAlive()
         && mob.getType().getCategory() == category
         && !mob.isPersistenceRequired()
         && !mob.requiresCustomPersistence();
   }

   public static int available(ServerLevel level, BlockPos pos, MobCategory category) {
      if (safeNaturalPosition(level, pos) && (category != MobCategory.MONSTER || level.getDifficulty() != Difficulty.PEACEFUL)) {
         SpawnState state = level.getChunkSource().getLastSpawnState();
         int cap = scaledCap(category, state == null ? 289 : state.getSpawnableChunkCount());
         SiftSpawnBudget.Population population = population(level);
         int count = population.global.getOrDefault(category, 0);
         int globalRoom = Math.max(0, cap - count);
         if (globalRoom == 0) {
            return 0;
         }

         int localRoom = 0;

         for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && !(Math.abs(player.getX() - pos.getX()) > 128.0) && !(Math.abs(player.getZ() - pos.getZ()) > 128.0)
               )
             {
               AABB area = new AABB(
                  player.getX() - 128.0, level.getMinY(), player.getZ() - 128.0, player.getX() + 128.0, level.getMaxY(), player.getZ() + 128.0
               );
               int nearby = population.local
                  .computeIfAbsent(player.getUUID(), k -> new EnumMap<>(MobCategory.class))
                  .computeIfAbsent(category, k -> level.getEntitiesOfClass(Mob.class, area, mob -> countsForCap(mob, category)).size());
               localRoom = Math.max(localRoom, category.getMaxInstancesPerChunk() - nearby);
            }
         }

         return Math.max(0, Math.min(globalRoom, localRoom));
      } else {
         return 0;
      }
   }

   private static boolean isSifter(Mob mob) {
      return mob instanceof SeedlingEntity || mob instanceof NesterEntity || mob instanceof PollinatorEntity;
   }

   public static int availableSifters(ServerLevel level, BlockPos pos) {
      return safeNaturalPosition(level, pos) && level.getDifficulty() != Difficulty.PEACEFUL ? encounterRoom(level, pos, 12, SiftSpawnBudget::isSifter) : 0;
   }

   public static int availableAllays(ServerLevel level, BlockPos pos) {
      return !safeNaturalPosition(level, pos) ? 0 : encounterRoom(level, pos, 6, mob -> mob instanceof Allay);
   }

   private static int encounterRoom(ServerLevel level, BlockPos pos, int limit, Predicate<Mob> kind) {
      int room = 0;

      for (ServerPlayer player : level.players()) {
         if (player.isAlive() && !player.isSpectator() && !(player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 16384.0)) {
            AABB area = new AABB(player.getX() - 128.0, level.getMinY(), player.getZ() - 128.0, player.getX() + 128.0, level.getMaxY(), player.getZ() + 128.0);
            int nearby = level.getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive() && kind.test(mob)).size();
            room = Math.max(room, limit - nearby);
         }
      }

      return Math.max(0, room);
   }

   private static final class Population {
      long tick;
      final Map<MobCategory, Integer> global = new EnumMap<>(MobCategory.class);
      final Map<UUID, Map<MobCategory, Integer>> local = new HashMap<>();
   }
}
