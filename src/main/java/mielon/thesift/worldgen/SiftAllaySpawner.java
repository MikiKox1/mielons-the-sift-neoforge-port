package mielon.thesift.worldgen;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public final class SiftAllaySpawner {
   private static final int CHECK_INTERVAL = 200;
   private static int checkCooldown;

   private SiftAllaySpawner() {
   }

   public static void tick(MinecraftServer server) {
      if (--checkCooldown <= 0) {
         checkCooldown = 200;
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null && SiftSpawnBudget.enabled(level)) {
            for (ServerPlayer player : level.players()) {
               if (player.level() == level && player.isAlive() && !player.isSpectator()) {
                  trySpawnNear(level, player.blockPosition());
               }
            }
         }
      }
   }

   private static boolean trySpawnNear(ServerLevel level, BlockPos around) {
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int attempt = 0; attempt < 18; attempt++) {
         double angle = phase + attempt * 2.399963229728653;
         int radius = 34 + level.getRandom().nextInt(47);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * radius);
         if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
            int surface = level.getHeight(Types.WORLD_SURFACE, x, z);
            BlockPos center = new BlockPos(x, surface + 6, z);
            SiftAllaySpawner.SpawnProfile profile = profileAt(level, center);
            if (profile != null) {
               if (level.getRandom().nextDouble() >= profile.chance()) {
                  return false;
               }

               AABB local = new AABB(center).inflate(96.0, 48.0, 96.0);
               int localPopulation = level.getEntitiesOfClass(Allay.class, local).size();
               int available = Math.min(profile.localCap() - localPopulation, SiftSpawnBudget.availableAllays(level, center));
               if (available <= 0) {
                  return false;
               }

               int maximumGroup = Math.min(profile.maximumGroup(), available);
               int minimumGroup = Math.min(profile.minimumGroup(), maximumGroup);
               int groupSize = minimumGroup + level.getRandom().nextInt(maximumGroup - minimumGroup + 1);
               return spawnGroup(level, center, profile, groupSize);
            }
         }
      }

      return false;
   }

   private static boolean spawnGroup(ServerLevel level, BlockPos center, SiftAllaySpawner.SpawnProfile profile, int groupSize) {
      int spawned = 0;

      for (int member = 0; member < groupSize; member++) {
         BlockPos feet = null;

         for (int attempt = 0; attempt < 10 && feet == null; attempt++) {
            int x = center.getX() + level.getRandom().nextInt(9) - 4;
            int z = center.getZ() + level.getRandom().nextInt(9) - 4;
            if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
               int y = level.getHeight(Types.WORLD_SURFACE, x, z) + 5 + level.getRandom().nextInt(8);
               BlockPos candidate = new BlockPos(x, y, z);
               if (profileAt(level, candidate) == profile
                  && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()
                  && level.getBlockState(candidate.above()).getCollisionShape(level, candidate.above()).isEmpty()) {
                  feet = candidate;
               }
            }
         }

         if (feet != null && SiftSpawnBudget.availableAllays(level, feet) > 0) {
            Allay allay = EntityType.ALLAY.create(level, EntitySpawnReason.NATURAL);
            if (allay != null) {
               allay.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
               if (level.noCollision(allay) && !level.getBiome(allay.blockPosition()).is(TheSiftDimension.CORRUPTED_SIFT) && level.addFreshEntity(allay)) {
                  spawned++;
               } else {
                  allay.discard();
               }
            }
         }
      }

      return spawned > 0;
   }

   private static SiftAllaySpawner.SpawnProfile profileAt(ServerLevel level, BlockPos pos) {
      if (TheSiftDimension.isCoralBiome(level.getBiome(pos))) {
         return SiftAllaySpawner.SpawnProfile.CORAL;
      } else if (level.getBiome(pos).is(TheSiftDimension.OVERGROWN_FOREST)) {
         return SiftAllaySpawner.SpawnProfile.FOREST;
      } else if (level.getBiome(pos).is(TheSiftDimension.SIFT_WASTES)) {
         return SiftAllaySpawner.SpawnProfile.WASTES;
      } else {
         return level.getBiome(pos).is(TheSiftDimension.OVERGROWN_CLEARING) ? SiftAllaySpawner.SpawnProfile.CLEARING : null;
      }
   }

   public static void clear() {
      checkCooldown = 0;
   }

   private enum SpawnProfile {
      CORAL(0.15, 1, 2, 3),
      FOREST(0.55, 2, 3, 6),
      WASTES(0.4, 1, 2, 4),
      CLEARING(0.35, 1, 2, 4);

      private final double chance;
      private final int minimumGroup;
      private final int maximumGroup;
      private final int localCap;

      SpawnProfile(double chance, int minimumGroup, int maximumGroup, int localCap) {
         this.chance = chance;
         this.minimumGroup = minimumGroup;
         this.maximumGroup = maximumGroup;
         this.localCap = localCap;
      }

      private double chance() {
         return this.chance;
      }

      private int minimumGroup() {
         return this.minimumGroup;
      }

      private int maximumGroup() {
         return this.maximumGroup;
      }

      private int localCap() {
         return this.localCap;
      }
   }
}
