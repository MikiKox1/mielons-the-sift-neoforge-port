package mielon.thesift.worldgen;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.SculkerPopulation;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.material.Fluid;

public final class SiftDeferredSnifferSpawner {
   private static final int MAX_SPAWNS_PER_TICK = 4;
   private static final int MAX_RETRIES = 80;
   private static final int MAX_REQUESTS_PER_TICK = 8;
   private static final int MAX_SITES_PER_REQUEST = 16;
   private static final ConcurrentLinkedQueue<SiftDeferredSnifferSpawner.SpawnRequest> PENDING = new ConcurrentLinkedQueue<>();

   private SiftDeferredSnifferSpawner() {
   }

   public static void enqueueFamily(WorldGenLevel level, RandomSource random, List<BlockPos> nest) {
      if (!nest.isEmpty()) {
         long[] positions = new long[nest.size()];

         for (int index = 0; index < nest.size(); index++) {
            positions[index] = nest.get(index).asLong();
         }

         long familySeed = level.getSeed() ^ positions[0] ^ 6002815919506936396L;
         PENDING.add(new SiftDeferredSnifferSpawner.SpawnRequest(positions, false, false, mix(familySeed), 0));
         PENDING.add(new SiftDeferredSnifferSpawner.SpawnRequest(positions, false, false, mix(familySeed + 1L), 0));
         if (random.nextFloat() < 0.33F) {
            PENDING.add(new SiftDeferredSnifferSpawner.SpawnRequest(positions, true, false, mix(familySeed + 2L), 0));
         }
      }
   }

   public static void enqueueDark(WorldGenLevel level, List<BlockPos> surfaceCandidates, long regionSeed) {
      if (!surfaceCandidates.isEmpty()) {
         long[] positions = new long[surfaceCandidates.size()];

         for (int index = 0; index < surfaceCandidates.size(); index++) {
            positions[index] = surfaceCandidates.get(index).asLong();
         }

         PENDING.add(new SiftDeferredSnifferSpawner.SpawnRequest(positions, false, true, mix(level.getSeed() ^ regionSeed ^ 4918302751538956614L), 0));
         PENDING.add(new SiftDeferredSnifferSpawner.SpawnRequest(positions, false, true, mix(level.getSeed() ^ regionSeed ^ 6004495999127867980L), 0, true));
         PENDING.add(
            new SiftDeferredSnifferSpawner.SpawnRequest(positions, false, true, mix(level.getSeed() ^ regionSeed ^ 5999710967373383493L), 0, true, true)
         );
      }
   }

   public static void tick(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level != null && SiftSpawnBudget.enabled(level)) {
         SiftSculkBlobResidents.tick(level);
         if (!PENDING.isEmpty()) {
            int availableAtTickStart = 0;

            for (SiftDeferredSnifferSpawner.SpawnRequest ignored : PENDING) {
               if (++availableAtTickStart >= 8) {
                  break;
               }
            }

            int spawned = 0;

            for (int inspected = 0; inspected < availableAtTickStart && spawned < 4; inspected++) {
               SiftDeferredSnifferSpawner.SpawnRequest request = PENDING.poll();
               if (request == null) {
                  break;
               }

               if (!allCandidateChunksReady(level, request.nestPositions())) {
                  requeue(request);
               } else if (spawnOne(level, request)) {
                  spawned++;
               } else {
                  requeue(request);
               }
            }
         }
      }
   }

   public static void clear() {
      PENDING.clear();
   }

   private static boolean allCandidateChunksReady(ServerLevel level, long[] positions) {
      int previousChunkX = Integer.MIN_VALUE;
      int previousChunkZ = Integer.MIN_VALUE;

      for (long packed : positions) {
         BlockPos pos = BlockPos.of(packed);
         int chunkX = Math.floorDiv(pos.getX(), 16);
         int chunkZ = Math.floorDiv(pos.getZ(), 16);
         if (chunkX != previousChunkX || chunkZ != previousChunkZ) {
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null || !level.areEntitiesLoaded(ChunkPos.pack(chunkX, chunkZ))) {
               return false;
            }

            previousChunkX = chunkX;
            previousChunkZ = chunkZ;
         }
      }

      return true;
   }

   private static boolean spawnOne(ServerLevel level, SiftDeferredSnifferSpawner.SpawnRequest request) {
      if (request.dark() && level.getDifficulty() == Difficulty.PEACEFUL) {
         return true;
      }

      boolean blobResident = request.dark() && !level.getBiome(BlockPos.of(request.nestPositions()[0])).is(TheSiftDimension.CORRUPTED_SIFT);
      if (blobResident) {
         return true;
      }

      RandomSource random = RandomSource.create(request.seed());
      long[] nest = request.nestPositions();
      int start = (random.nextInt(nest.length) + request.attempts() * 16) % nest.length;

      for (int offset = 0; offset < Math.min(nest.length, 16); offset++) {
         BlockPos pos = BlockPos.of(nest[(start + offset) % nest.length]).above();
         if (!request.dark() || SiftSpawnBudget.available(level, pos, MobCategory.MONSTER) > 0) {
            if (!request.dark() && level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT)) {
               return true;
            }

            if (!request.dark() || !level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT) || level.getBrightness(LightLayer.BLOCK, pos) <= 0) {
               if (request.dark()
                  && (
                     request.scavenger() ? !SculkerPopulation.canSpawnScavenger(level, pos, false) : !SculkerPopulation.canSpawn(level, pos, request.stalker())
                  )) {
                  return true;
               }

               if (request.stalker()
                  && !level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT)
                  && SiftTides.tide(level.getDefaultClockTime()) == SiftTides.Tide.ENDURE) {
                  if (request.scavenger() ? !SculkerPopulation.canSpawnScavenger(level, pos, true) : !SculkerPopulation.canSpawnEndureStalker(level, pos)) {
                     return true;
                  }

                  if (level.getBrightness(LightLayer.BLOCK, pos) > 0) {
                     continue;
                  }
               }

               if (level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()) && !isIchor(level, pos) && !isIchor(level, pos.below())) {
                  EntitySpawnReason reason = request.dark() ? EntitySpawnReason.CHUNK_GENERATION : EntitySpawnReason.STRUCTURE;
                  Mob sniffer = request.stalker()
                     ? (request.scavenger() ? ModEntities.SCAVENGER.create(level, reason) : ModEntities.STALKER.create(level, reason))
                     : (request.dark() ? ModEntities.DARK_SNIFFER.create(level, reason) : EntityType.SNIFFER.create(level, reason));
                  if (sniffer == null) {
                     return true;
                  }

                  sniffer.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                  sniffer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
                  if (!request.dark()) {
                     ((Sniffer)sniffer).setBaby(request.baby());
                  }

                  if (level.noCollision(sniffer) && level.addFreshEntity(sniffer)) {
                     return true;
                  }

                  sniffer.discard();
               }
            }
         }
      }

      return false;
   }

   private static boolean isIchor(ServerLevel level, BlockPos pos) {
      Fluid fluid = level.getFluidState(pos).getType();
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   private static void requeue(SiftDeferredSnifferSpawner.SpawnRequest request) {
      int attempts = request.attempts() + 1;
      if (attempts <= 80) {
         PENDING.add(
            new SiftDeferredSnifferSpawner.SpawnRequest(
               request.nestPositions(), request.baby(), request.dark(), request.seed(), attempts, request.stalker(), request.scavenger()
            )
         );
      }
   }

   private static long mix(long value) {
      value ^= value >>> 30;
      value *= -4658895280553007687L;
      value ^= value >>> 27;
      value *= -7723592293110705685L;
      return value ^ value >>> 31;
   }

   private record SpawnRequest(long[] nestPositions, boolean baby, boolean dark, long seed, int attempts, boolean stalker, boolean scavenger) {
      SpawnRequest(long[] positions, boolean baby, boolean dark, long seed, int attempts, boolean stalker) {
         this(positions, baby, dark, seed, attempts, stalker, false);
      }

      SpawnRequest(long[] positions, boolean baby, boolean dark, long seed, int attempts) {
         this(positions, baby, dark, seed, attempts, false);
      }
   }
}
