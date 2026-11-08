package mielon.thesift.entity;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

public final class SculkerPopulation {
   public static final int STALKER_LIMIT = 4;
   public static final int SCAVENGER_LIMIT = 3;
   public static final int COMBINED_LIMIT = 6;
   public static final int ENDURE_STALKER_LIMIT = 2;
   public static final int DARK_SNIFFER_LIMIT = 2;
   private static final double HORIZONTAL_RANGE = 48.0;
   private static final double VERTICAL_RANGE = 32.0;

   private SculkerPopulation() {
   }

   public static boolean canSpawn(ServerLevelAccessor access, BlockPos pos, boolean stalker) {
      ServerLevel level = access.getLevel();
      AABB area = new AABB(pos).inflate(48.0, 32.0, 48.0);
      if (combined(level, area) >= 6) {
         return false;
      } else {
         return stalker
            ? level.getEntitiesOfClass(StalkerEntity.class, area, LivingEntity::isAlive).size() < speciesLimit(level, pos, 4)
            : level.getEntitiesOfClass(DarkSnifferEntity.class, area, LivingEntity::isAlive).size() < 2;
      }
   }

   public static boolean canSpawnEndureStalker(ServerLevelAccessor access, BlockPos pos) {
      AABB area = new AABB(pos).inflate(64.0, 48.0, 64.0);
      return combined(access.getLevel(), area) < 6 && access.getLevel().getEntitiesOfClass(StalkerEntity.class, area, LivingEntity::isAlive).size() < 2;
   }

   public static boolean canSpawnScavenger(ServerLevelAccessor access, BlockPos pos, boolean endure) {
      AABB area = new AABB(pos).inflate(endure ? 64.0 : 48.0, endure ? 48.0 : 32.0, endure ? 64.0 : 48.0);
      return combined(access.getLevel(), area) < 6
         && access.getLevel().getEntitiesOfClass(ScavengerEntity.class, area, LivingEntity::isAlive).size()
            < (endure ? 2 : speciesLimit(access.getLevel(), pos, 3));
   }

   private static int speciesLimit(ServerLevel level, BlockPos pos, int normalLimit) {
      return level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT) ? 2 : normalLimit;
   }

   private static int combined(ServerLevel level, AABB area) {
      return level.getEntitiesOfClass(StalkerEntity.class, area, LivingEntity::isAlive).size()
         + level.getEntitiesOfClass(ScavengerEntity.class, area, LivingEntity::isAlive).size()
         + level.getEntitiesOfClass(DarkSnifferEntity.class, area, LivingEntity::isAlive).size();
   }

   public static boolean despawnCrowded(Mob mob) {
      if (mob.level() instanceof ServerLevel level
         && level.dimension().equals(TheSiftDimension.LEVEL_KEY)
         && Math.floorMod(mob.tickCount + mob.getId(), 100) == 0
         && !mob.isPersistenceRequired()
         && !mob.requiresCustomPersistence()
         && !mob.hasCustomName()
         && !mob.isNoAi()
         && !mob.isVehicle()) {
         Player player = level.getNearestPlayer(mob, -1.0);
         if (player != null && !(mob.distanceToSqr(player) <= 1024.0)) {
            AABB area = mob.getBoundingBox().inflate(48.0, 32.0, 48.0);
            int count = mob instanceof ScavengerEntity
               ? level.getEntitiesOfClass(ScavengerEntity.class, area, LivingEntity::isAlive).size()
               : (
                  mob instanceof StalkerEntity
                     ? level.getEntitiesOfClass(StalkerEntity.class, area, LivingEntity::isAlive).size()
                     : level.getEntitiesOfClass(DarkSnifferEntity.class, area, LivingEntity::isAlive).size()
               );
            int limit = mob instanceof ScavengerEntity ? 3 : (mob instanceof StalkerEntity ? 4 : 2);
            limit = speciesLimit(level, mob.blockPosition(), limit);
            if (count <= limit && combined(level, area) <= 6) {
               return false;
            }

            mob.discard();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
