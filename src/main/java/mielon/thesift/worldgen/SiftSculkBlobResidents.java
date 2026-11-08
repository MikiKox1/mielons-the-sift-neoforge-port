package mielon.thesift.worldgen;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import mielon.thesift.entity.DarkSnifferEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.ScavengerEntity;
import mielon.thesift.entity.StalkerEntity;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public final class SiftSculkBlobResidents {
   private SiftSculkBlobResidents() {
   }

   public static boolean blocksNaturalSpawn(ServerLevelAccessor level, BlockPos pos) {
      return level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)
         && !level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT)
         && level.getBlockState(pos.below()).is(Blocks.SCULK);
   }

   public static void record(BlockPos center) {
      if (!SiftLandmarkTracker.hasRecorded(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_SPAWNED, center)) {
         SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_PENDING, center);
      }
   }

   static int darkSnifferCount(long seed, BlockPos center) {
      return 1 + RandomSource.create(seed ^ center.asLong()).nextInt(2);
   }

   public static void tick(ServerLevel level) {
      if (level.getGameTime() % 100L == 0L && level.getDifficulty() != Difficulty.PEACEFUL && SiftSpawnBudget.enabled(level)) {
         int inspected = 0;
         int spawned = 0;
         List<BlockPos> pending = SiftLandmarkTracker.nearPlayers(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_PENDING, level, 128);
         if (!pending.isEmpty()) {
            int start = (int)Math.floorMod((long)(level.getGameTime() / 100L), (long)pending.size());

            for (int offset = 0; offset < pending.size(); offset++) {
               BlockPos original = pending.get((start + offset) % pending.size());
               BlockPos center = SiftSurfaceSculkRegionFeature.residentCenter(level.getSeed(), original);
               if (!center.equals(original)) {
                  SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_PENDING, original);
                  record(center);
               }

               if (SiftLandmarkTracker.hasRecorded(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_SPAWNED, center)) {
                  SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_PENDING, center);
               } else if (level.getNearestPlayer(center.getX(), center.getY(), center.getZ(), 128.0, p -> p.isAlive() && !p.isSpectator()) != null) {
                  if (++inspected > 8 || spawned >= 2) {
                     break;
                  }

                  int sniffers = darkSnifferCount(level.getSeed(), center);
                  int total = sniffers + 3;
                  Predicate<BlockPos> shape = SiftSurfaceSculkRegionFeature.blobShape(level.getSeed(), center);
                  boolean complete = true;

                  for (int slot = 0; slot < total; slot++) {
                     BlockPos marker = center.above(slot);
                     SiftLandmarkTracker.Kind kind = SiftLandmarkTracker.Kind.SCULK_BLOB_MEMBER_SPAWNED;
                     if (!SiftLandmarkTracker.hasRecorded(kind, marker)) {
                        UUID id = UUID.nameUUIDFromBytes(
                           ("the_sift:sculk_blob:" + level.getSeed() + ":" + center.asLong() + ":" + slot).getBytes(StandardCharsets.UTF_8)
                        );
                        if (level.getEntity(id) != null) {
                           SiftLandmarkTracker.record(kind, marker);
                        } else if (spawned >= 2) {
                           complete = false;
                        } else {
                           Mob resident = slot < sniffers
                              ? new DarkSnifferEntity(ModEntities.DARK_SNIFFER, level)
                              : (slot < sniffers + 2 ? new StalkerEntity(ModEntities.STALKER, level) : new ScavengerEntity(ModEntities.SCAVENGER, level));
                           RandomSource random = RandomSource.create(center.asLong() ^ (long)slot << 32 ^ level.getGameTime());
                           boolean added = false;

                           for (int attempt = 0; attempt < 96 && !added; attempt++) {
                              int x = center.getX() + random.nextInt(121) - 60;
                              int z = center.getZ() + random.nextInt(121) - 60;
                              if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) != null && level.areEntitiesLoaded(ChunkPos.pack(x >> 4, z >> 4))) {
                                 for (int y = Math.min(level.getMaxY() - 2, center.getY() + 32); y >= Math.max(level.getMinY() + 1, center.getY() - 32); y--) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    if (shape.test(pos.below())
                                       && !level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT)
                                       && level.getBlockState(pos.below()).is(Blocks.SCULK)
                                       && level.getFluidState(pos).isEmpty()
                                       && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                                       && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                                       resident.setPos(x + 0.5, y, z + 0.5);
                                       if (level.noCollision(resident)) {
                                          resident.setUUID(id);
                                          resident.setYRot(random.nextFloat() * 360.0F);
                                          resident.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
                                          resident.setPersistenceRequired();
                                          if (level.addFreshEntity(resident)) {
                                             SiftLandmarkTracker.record(kind, marker);
                                             spawned++;
                                             added = true;
                                             break;
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           if (!added) {
                              resident.discard();
                              complete = false;
                           }
                        }
                     }
                  }

                  if (complete) {
                     SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_SPAWNED, center);
                     SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SCULK_BLOB_RESIDENT_PENDING, center);
                  }
               }
            }
         }
      }
   }
}
