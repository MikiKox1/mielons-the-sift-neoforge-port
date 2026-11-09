package mielon.thesift.world;

import net.minecraft.world.level.levelgen.Heightmap.Types;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SafePortalArrival {
   private SafePortalArrival() {
   }

   public static Optional<Vec3> find(ServerLevel level, Entity entity, Vec3 preferred) {
      int centerX = (int)Math.floor(preferred.x) >> 4;
      int centerZ = (int)Math.floor(preferred.z) >> 4;

      for (int x = centerX - 1; x <= centerX + 1; x++) {
         for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            level.getChunk(x, z);
         }
      }

      if (safe(level, entity, preferred)) {
         return Optional.of(preferred);
      }

      for (int ring = 0; ring <= 8; ring++) {
         for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -ring; dx <= ring; dx++) {
               for (int dz = -ring; dz <= ring; dz++) {
                  if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                     Vec3 candidate = preferred.add(dx, dy, dz);
                     if (safe(level, entity, candidate)) {
                        return Optional.of(candidate);
                     }
                  }
               }
            }
         }
      }

      return Optional.empty();
   }

   /**
    * A remembered Ancient City entrance is underground. Never substitute a
    * surface heightmap for its Y coordinate if the immediate portal is blocked.
    */
   public static Optional<Vec3> findNearRecordedPortal(ServerLevel level, Entity entity, Vec3 preferred) {
      Optional<Vec3> immediate = find(level, entity, preferred);
      if (immediate.isPresent()) {
         return immediate;
      }

      // Saved entry positions can be fractional in every axis. The fallback
      // must use centered blocks and exact floor heights or it can miss all
      // valid underground landings while intersecting neighboring walls.
      Vec3 aligned = new Vec3(Math.floor(preferred.x) + 0.5, Math.ceil(preferred.y), Math.floor(preferred.z) + 0.5);
      final int horizontal = 16;
      final int vertical = 8;
      int minChunkX = (int)Math.floor(preferred.x - horizontal - 1.0) >> 4;
      int maxChunkX = (int)Math.floor(preferred.x + horizontal + 1.0) >> 4;
      int minChunkZ = (int)Math.floor(preferred.z - horizontal - 1.0) >> 4;
      int maxChunkZ = (int)Math.floor(preferred.z + horizontal + 1.0) >> 4;
      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            level.getChunk(chunkX, chunkZ);
         }
      }

      for (int ring = 0; ring <= horizontal; ring++) {
         for (int height = 0; height <= vertical; height++) {
            // Prefer standing on the same floor, then search down and up.
            for (int variant = 0; variant < (height == 0 ? 1 : 2); variant++) {
               int dy = variant == 0 ? -height : height;
               for (int dx = -ring; dx <= ring; dx++) {
                  for (int dz = -ring; dz <= ring; dz++) {
                     if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                     }
                     Vec3 candidate = aligned.add(dx, dy, dz);
                     if (safe(level, entity, candidate)) {
                        return Optional.of(candidate);
                     }
                  }
               }
            }
         }
      }
      return Optional.empty();
   }

   public static boolean safe(ServerLevel level, Entity entity, Vec3 target) {
      Vec3 delta = target.subtract(entity.position());

      for (Entity traveler : entity.getSelfAndPassengers().toList()) {
         AABB box = traveler.getBoundingBox().move(delta).deflate(1.0E-6);
         if (box.minY < level.getMinY() || box.maxY >= level.getMaxY() || !level.getWorldBorder().isWithinBounds(box)) {
            return false;
         }

         for (int x = (int)Math.floor(box.minX) >> 4; x <= (int)Math.floor(box.maxX) >> 4; x++) {
            for (int z = (int)Math.floor(box.minZ) >> 4; z <= (int)Math.floor(box.maxZ) >> 4; z++) {
               if (level.getChunkSource().getChunkNow(x, z) == null) {
                  return false;
               }
            }
         }

         if (!level.noCollision(traveler, box)) {
            return false;
         }

         if (level.containsAnyLiquid(box)) {
            return false;
         }
      }

      AABB feet = entity.getBoundingBox().move(delta);
      AABB support = new AABB(feet.minX, feet.minY - 0.1, feet.minZ, feet.maxX, feet.minY + 0.01, feet.maxZ);
      return level.getBlockCollisions(entity, support).iterator().hasNext();
   }

   public static Optional<Vec3> findForEntry(ServerLevel level, Entity entity, Vec3 preferred) {
      Optional<Vec3> nearby = find(level, entity, preferred);
      if (nearby.isPresent()) {
         return nearby;
      }

      for (int ring = 0; ring <= 8; ring++) {
         for (int dx = -ring; dx <= ring; dx++) {
            for (int dz = -ring; dz <= ring; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                  double x = preferred.x + dx;
                  double z = preferred.z + dz;
                  int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
                  Vec3 candidate = new Vec3(x, y, z);
                  if (safe(level, entity, candidate)) {
                     return Optional.of(candidate);
                  }
               }
            }
         }
      }

      return Optional.empty();
   }
}
