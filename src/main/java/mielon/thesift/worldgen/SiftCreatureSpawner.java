package mielon.thesift.worldgen;

import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.NesterEntity;
import mielon.thesift.entity.PollinatorEntity;
import mielon.thesift.entity.SeedlingEntity;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

public final class SiftCreatureSpawner {
   private static final int CHECK_INTERVAL = 160;
   private static int checkCooldown;

   private SiftCreatureSpawner() {
   }

   public static void tick(MinecraftServer server) {
      if (--checkCooldown <= 0) {
         checkCooldown = 160;
         ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
         if (level != null && SiftSpawnBudget.enabled(level) && level.getDifficulty() != Difficulty.PEACEFUL) {
            for (ServerPlayer player : level.players()) {
               if (player.level() == level && player.isAlive() && !player.isSpectator()) {
                  trySpawnNear(level, player.blockPosition());
               }
            }
         }
      }
   }

   private static boolean trySpawnNear(ServerLevel level, BlockPos around) {
      RandomSource random = level.getRandom();
      double phase = random.nextDouble() * Math.PI * 2.0;

      for (int attempt = 0; attempt < 18; attempt++) {
         double angle = phase + attempt * 2.399963229728653;
         int radius = 32 + random.nextInt(33);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * radius);
         if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
            boolean surface = random.nextFloat() < 0.8F;
            BlockPos center = findSpawnFeet(level, x, z, surface, random);
            if (center != null) {
               SiftCreatureSpawner.SpawnProfile profile = profileAt(level, center);
               if (profile != null && !(random.nextDouble() >= profile.chance())) {
                  AABB local = new AABB(center).inflate(56.0, 32.0, 56.0);
                  int[] counts = new int[]{
                     level.getEntitiesOfClass(SeedlingEntity.class, local, LivingEntity::isAlive).size(),
                     level.getEntitiesOfClass(NesterEntity.class, local, LivingEntity::isAlive).size(),
                     level.getEntitiesOfClass(PollinatorEntity.class, local, LivingEntity::isAlive).size()
                  };
                  int spawnKind = random.nextInt(3);

                  for (int kind = 0; kind < 3; kind++) {
                     if (counts[kind] < counts[spawnKind]) {
                        spawnKind = kind;
                     }
                  }

                  int available = Math.min(
                     profile.localCap() - counts[0] - counts[1] - counts[2],
                     Math.min((profile.localCap() + 2) / 3 - counts[spawnKind], SiftSpawnBudget.availableSifters(level, center))
                  );
                  if (available > 0) {
                     int maximumGroup = Math.min(profile.maximumGroup(spawnKind), available);
                     int minimumGroup = Math.min(profile.minimumGroup(spawnKind), maximumGroup);
                     int groupSize = minimumGroup + random.nextInt(maximumGroup - minimumGroup + 1);
                     if (spawnGroup(level, center, profile, groupSize, random, spawnKind)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean spawnGroup(
      ServerLevel level, BlockPos center, SiftCreatureSpawner.SpawnProfile profile, int groupSize, RandomSource random, int spawnKind
   ) {
      int spawned = 0;

      for (int member = 0; member < groupSize; member++) {
         BlockPos feet = null;

         for (int attempt = 0; attempt < 12 && feet == null; attempt++) {
            int x = center.getX() + random.nextInt(9) - 4;
            int z = center.getZ() + random.nextInt(9) - 4;
            if (level.getChunkSource().getChunkNow(Math.floorDiv(x, 16), Math.floorDiv(z, 16)) != null) {
               BlockPos candidate = findFloor(level, x, z, center.getY() + 3, 9, -1);
               if (candidate != null
                  && profileAt(level, candidate) == profile
                  && hasEmptyCollision(level, candidate)
                  && hasEmptyCollision(level, candidate.above())
                  && (spawnKind != 1 || hasEmptyCollision(level, candidate.above(2)))) {
                  feet = candidate;
               }
            }
         }

         if (feet != null && SiftSpawnBudget.availableSifters(level, feet) > 0) {
            Mob creature = spawnKind == 2
               ? ModEntities.POLLINATOR.create(level, EntitySpawnReason.NATURAL)
               : (spawnKind == 1 ? ModEntities.NESTER.create(level, EntitySpawnReason.NATURAL) : ModEntities.SEEDLING.create(level, EntitySpawnReason.NATURAL));
            if (creature != null) {
               creature.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
               if (level.noCollision(creature) && level.addFreshEntity(creature)) {
                  spawned++;
               } else {
                  creature.discard();
               }
            }
         }
      }

      return spawned > 0;
   }

   private static BlockPos findSpawnFeet(ServerLevel level, int x, int z, boolean surface, RandomSource random) {
      int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      if (surface) {
         return findFloor(level, x, z, top - 1, 32, -1);
      }

      int minY = level.getMinY() + 2;
      int highestCaveFloor = top - 6;
      if (highestCaveFloor <= minY) {
         return null;
      }

      int startY = minY + random.nextInt(highestCaveFloor - minY + 1);
      return findFloor(level, x, z, startY, 18, -1);
   }

   private static BlockPos findFloor(ServerLevel level, int x, int z, int startY, int attempts, int direction) {
      for (int offset = 0; offset <= attempts; offset++) {
         int floorY = startY + offset * direction;
         BlockPos floor = new BlockPos(x, floorY, z);
         if (SeedlingEntity.canSpawnOn(level.getBlockState(floor)) && level.getBlockState(floor).isCollisionShapeFullBlock(level, floor)) {
            BlockPos feet = floor.above();
            if (hasEmptyCollision(level, feet) && hasEmptyCollision(level, feet.above())) {
               return feet;
            }
         }
      }

      return null;
   }

   private static boolean hasEmptyCollision(ServerLevel level, BlockPos pos) {
      return level.getBlockState(pos).getFluidState().isEmpty() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
   }

   private static SiftCreatureSpawner.SpawnProfile profileAt(ServerLevel level, BlockPos pos) {
      if (level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT)) {
         return null;
      } else if (TheSiftDimension.isOrganicDesert(level.getBiome(pos))) {
         return SiftCreatureSpawner.SpawnProfile.ORGANIC_DESERT;
      } else if (level.getBiome(pos).is(TheSiftDimension.CORAL_MEADOW)) {
         return SiftCreatureSpawner.SpawnProfile.CORAL_MEADOW;
      } else if (level.getBiome(pos).is(TheSiftDimension.CORAL_FOREST)) {
         return SiftCreatureSpawner.SpawnProfile.CORAL_FOREST;
      } else if (TheSiftDimension.isOvergrownBiome(level.getBiome(pos))) {
         return level.getBiome(pos).is(TheSiftDimension.OVERGROWN_FOREST)
            ? SiftCreatureSpawner.SpawnProfile.OVERGROWN_FOREST
            : SiftCreatureSpawner.SpawnProfile.OVERGROWN;
      } else {
         return level.getBiome(pos).is(TheSiftDimension.SIFT_WASTES) ? SiftCreatureSpawner.SpawnProfile.WASTES : SiftCreatureSpawner.SpawnProfile.OTHER_SIFT;
      }
   }

   public static void clear() {
      checkCooldown = 0;
   }

   private enum SpawnProfile {
      ORGANIC_DESERT(0.55, 9),
      CORAL_MEADOW(0.06, 3),
      CORAL_FOREST(0.45, 8),
      OVERGROWN_FOREST(0.4, 8),
      OVERGROWN(0.35, 8),
      WASTES(0.25, 8),
      OTHER_SIFT(0.18, 3);

      private final double chance;
      private final int localCap;

      SpawnProfile(double chance, int localCap) {
         this.chance = chance;
         this.localCap = localCap;
      }

      private double chance() {
         return this.chance;
      }

      private int minimumGroup(int spawnKind) {
         if (this == ORGANIC_DESERT) {
            return 2;
         } else {
            return spawnKind == 2 && this != OVERGROWN && this != OVERGROWN_FOREST && this != CORAL_MEADOW && this != CORAL_FOREST ? 2 : 1;
         }
      }

      private int maximumGroup(int spawnKind) {
         if (this == ORGANIC_DESERT) {
            return 3;
         } else if (this == CORAL_MEADOW) {
            return 2;
         } else if (this != CORAL_FOREST && this != OVERGROWN && this != OVERGROWN_FOREST) {
            return spawnKind == 2 ? 4 : 2;
         } else {
            return 3;
         }
      }

      private int localCap() {
         return this.localCap;
      }
   }
}
