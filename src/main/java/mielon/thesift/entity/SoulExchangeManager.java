package mielon.thesift.entity;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import mielon.thesift.worldgen.SiftLayeredWalkSurface;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

public final class SoulExchangeManager {
   private SoulExchangeManager() {
   }

   public static SingerEntity requestSinger(ServerLevel level, EchoGolemEntity golem) {
      if (golem.hasSoulBlock() && golem.canInteractWithSinger()) {
         AABB search = golem.getBoundingBox().inflate(52.0, 32.0, 52.0);
         SingerEntity existing = level.getEntitiesOfClass(SingerEntity.class, search, SingerEntity::isSoulEvent)
            .stream()
            .min(Comparator.comparingDouble(golem::distanceToSqr))
            .orElse(null);
         if (existing != null) {
            if (!golem.tryAcquirePathBudget(level, 6)) {
               return null;
            }

            Path path = golem.getNavigation().createPath(existing.blockPosition(), 2);
            if (path == null || !path.canReach()) {
               existing = null;
            }
         }

         if (existing != null) {
            existing.offerSoulGolem(golem);
            return existing;
         } else {
            BlockPos spawn = findNaturalSpawn(level, golem);
            if (spawn == null) {
               return null;
            } else {
               SingerEntity singer = ModEntities.SINGER.create(level, EntitySpawnReason.EVENT);
               if (singer == null) {
                  return null;
               } else {
                  singer.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
                  if (!level.noCollision(singer)) {
                     singer.discard();
                     return null;
                  } else {
                     singer.beginSoulEvent(golem.getUUID());
                     singer.setPersistenceRequired();
                     if (!level.addFreshEntity(singer)) {
                        singer.discard();
                        return null;
                     } else {
                        return singer;
                     }
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   private static BlockPos findNaturalSpawn(ServerLevel level, EchoGolemEntity golem) {
      if (!golem.tryAcquirePathBudget(level, 6)) {
         return null;
      }

      BlockPos around = golem.blockPosition();
      Set<BlockPos> candidates = new HashSet<>();
      double phase = level.getRandom().nextDouble() * Math.PI * 2.0;

      for (int attempt = 0; attempt < 24; attempt++) {
         double angle = phase + attempt * 2.399963229728653;
         int radius = 6 + level.getRandom().nextInt(7);
         int x = around.getX() + (int)Math.round(Math.cos(angle) * radius);
         int z = around.getZ() + (int)Math.round(Math.sin(angle) * radius);
         BlockPos feet = SiftLayeredWalkSurface.findFeet(level, x, z, around.getY(), 6, 4);
         if (feet != null && golem.isClearOfSoulCanyon(feet)) {
            candidates.add(feet);
         }
      }

      if (candidates.isEmpty()) {
         return null;
      } else {
         Path path = golem.getNavigation().createPath(candidates, 0);
         if (path != null && path.canReach() && path.getEndNode() != null) {
            BlockPos reached = path.getEndNode().asBlockPos();
            return candidates.contains(reached) ? reached.immutable() : null;
         } else {
            return null;
         }
      }
   }
}
