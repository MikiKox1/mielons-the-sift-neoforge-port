package mielon.thesift.worldgen;

import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;

public final class SiftEchoGolemSpawner {
   private static final int CHECK_INTERVAL = 80;
   private static final double MINIMUM_SPACING = 165.0;
   private static int checkCooldown;
   private static long nextNaturalSpawnTick;

   private SiftEchoGolemSpawner() {
   }

   public static void tick(MinecraftServer server) {
      if (--checkCooldown <= 0) {
         checkCooldown = 80;
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null && SiftSpawnBudget.enabled(level)) {
            if (!spawnCanyonGolem(level)) {
               if (level.getGameTime() >= nextNaturalSpawnTick) {
                  nextNaturalSpawnTick = level.getGameTime() + 1200L;

                  for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                     if (player.level() == level && !player.isSpectator() && player.isAlive()) {
                        AABB nearby = new AABB(player.blockPosition()).inflate(165.0, 96.0, 165.0);
                        if (level.getEntitiesOfClass(EchoGolemEntity.class, nearby).isEmpty() && trySpawn(level, player.blockPosition())) {
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean spawnCanyonGolem(ServerLevel level) {
      for (BlockPos entrance : SiftLandmarkTracker.snapshot(SiftLandmarkTracker.Kind.SOUL_CANYON_GOLEM_PENDING)) {
         if (level.getChunkSource().getChunkNow(entrance.getX() >> 4, entrance.getZ() >> 4) != null) {
            for (int radius = 0; radius <= 12; radius++) {
               for (int dx = -radius; dx <= radius; dx++) {
                  for (int dz = -radius; dz <= radius; dz++) {
                     if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                        int x = entrance.getX() + dx;
                        int z = entrance.getZ() + dz;
                        if (spawnAreaLoaded(level, x, z)) {
                           BlockPos feet = SiftLayeredWalkSurface.findFeet(level, x, z, entrance.getY(), 6, 3);
                           if (feet != null && Math.abs(feet.getY() - entrance.getY()) <= 6 && spawnAt(level, feet, EntitySpawnReason.STRUCTURE)) {
                              SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SOUL_CANYON_GOLEM_PENDING, entrance);
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean spawnAreaLoaded(ServerLevel level, int x, int z) {
      for (int chunkX = x - 1 >> 4; chunkX <= x + 1 >> 4; chunkX++) {
         for (int chunkZ = z - 1 >> 4; chunkZ <= z + 1 >> 4; chunkZ++) {
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean trySpawn(ServerLevel level, BlockPos around) {
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int attempt = 0; attempt < 28; attempt++) {
         double angle = phase + attempt * 2.399963229728653;
         int radius = 82 + level.getRandom().nextInt(39);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * radius);
         if (spawnAreaLoaded(level, x, z)) {
            BlockPos feet = SiftLayeredWalkSurface.findFeet(level, x, z, around.getY(), 16, 3);
            if (feet != null) {
               AABB spacing = new AABB(feet).inflate(165.0, 96.0, 165.0);
               if (level.getEntitiesOfClass(EchoGolemEntity.class, spacing).isEmpty() && spawnAt(level, feet, EntitySpawnReason.NATURAL)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static boolean spawnAt(ServerLevel level, BlockPos feet, EntitySpawnReason reason) {
      if (reason == EntitySpawnReason.NATURAL && SiftSpawnBudget.available(level, feet, MobCategory.CREATURE) <= 0) {
         return false;
      }

      EchoGolemEntity golem = ModEntities.ECHO_GOLEM.create(level, reason);
      if (golem == null) {
         return false;
      }

      golem.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
      if (reason == EntitySpawnReason.STRUCTURE) {
         golem.setPersistenceRequired();
      }

      if (level.noCollision(golem) && !level.getBiome(golem.blockPosition()).is(TheSiftDimension.CORRUPTED_SIFT) && level.addFreshEntity(golem)) {
         level.playSound(null, feet, ModSounds.ECHO_GOLEM_SPAWN, golem.getSoundSource(), 0.8F, 0.94F);
         return true;
      } else {
         golem.discard();
         return false;
      }
   }

   public static void clear() {
      checkCooldown = 0;
      nextNaturalSpawnTick = 0L;
   }
}
