package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;

public final class SiftFeaturePlacementGuard {
   private static final int PORTAL_MARGIN = 15;
   private static volatile SiftFeaturePlacementGuard.ProtectedRectangle portalRectangle;
   private static volatile SiftFeaturePlacementGuard.PortalVolume portalVolume;

   private SiftFeaturePlacementGuard() {
   }

   public static void reservePortal(BlockPos structureOrigin, int sizeX, int sizeZ) {
      portalVolume = new SiftFeaturePlacementGuard.PortalVolume(
         structureOrigin.getX() - 1,
         structureOrigin.getZ() - 1,
         structureOrigin.getX() + sizeX,
         structureOrigin.getZ() + sizeZ,
         structureOrigin.getY() - 3,
         structureOrigin.getY() + 22
      );
      portalRectangle = new SiftFeaturePlacementGuard.ProtectedRectangle(
         structureOrigin.getX() - 15, structureOrigin.getZ() - 15, structureOrigin.getX() + sizeX - 1 + 15, structureOrigin.getZ() + sizeZ - 1 + 15
      );
   }

   public static boolean protectsPortalTerrain(BlockPos pos) {
      if (!SiftPortalVegetation.allowsFloor(pos) && !SiftPortalVegetation.allowsPlant(pos)) {
         SiftFeaturePlacementGuard.PortalVolume volume = portalVolume;
         return volume != null
            && pos.getX() >= volume.minX
            && pos.getX() <= volume.maxX
            && pos.getZ() >= volume.minZ
            && pos.getZ() <= volume.maxZ
            && pos.getY() >= volume.minY
            && pos.getY() <= volume.maxY;
      } else {
         return false;
      }
   }

   public static boolean intersectsPortal(int minX, int minZ, int maxX, int maxZ) {
      SiftFeaturePlacementGuard.ProtectedRectangle rectangle = portalRectangle;
      return rectangle != null && maxX >= rectangle.minX && minX <= rectangle.maxX && maxZ >= rectangle.minZ && minZ <= rectangle.maxZ;
   }

   public static boolean intersectsPortal(BlockPos center, int horizontalRadius) {
      return intersectsPortal(
         center.getX() - horizontalRadius, center.getZ() - horizontalRadius, center.getX() + horizontalRadius, center.getZ() + horizontalRadius
      );
   }

   public static synchronized boolean tryReserveCarver(int minX, int minZ, int maxX, int maxZ) {
      return tryReserve(new SiftFeaturePlacementGuard.ProtectedRectangle(minX, minZ, maxX, maxZ));
   }

   public static synchronized boolean intersectsReservedLandmark(BlockPos center, int radius) {
      return SiftTerrainFeatureMemory.intersectsReservation(center.getX() - radius, center.getZ() - radius, center.getX() + radius, center.getZ() + radius);
   }

   static synchronized double clearance(int x, int z, double maximum) {
      double distance = maximum;
      SiftFeaturePlacementGuard.ProtectedRectangle portal = portalRectangle;
      if (portal != null) {
         distance = Math.min(distance, portal.distance(x, z));
      }

      return distance;
   }

   public static synchronized boolean tryReserveSolid(int minX, int minZ, int maxX, int maxZ) {
      return tryReserve(new SiftFeaturePlacementGuard.ProtectedRectangle(minX, minZ, maxX, maxZ));
   }

   private static boolean tryReserve(SiftFeaturePlacementGuard.ProtectedRectangle candidate) {
      return intersectsPortal(candidate.minX, candidate.minZ, candidate.maxX, candidate.maxZ)
         ? false
         : SiftTerrainFeatureMemory.reserve(candidate.minX, candidate.minZ, candidate.maxX, candidate.maxZ);
   }

   public static void clear() {
      SiftPortalVegetation.clear();
      portalRectangle = null;
      portalVolume = null;
      SiftTerrainFeatureMemory.clear();
   }

   private record PortalVolume(int minX, int minZ, int maxX, int maxZ, int minY, int maxY) {
   }

   private record ProtectedRectangle(int minX, int minZ, int maxX, int maxZ) {
      double distance(int x, int z) {
         return Math.hypot(Math.max(0, Math.max(this.minX - x, x - this.maxX)), Math.max(0, Math.max(this.minZ - z, z - this.maxZ)));
      }

      boolean intersects(SiftFeaturePlacementGuard.ProtectedRectangle other) {
         return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ;
      }
   }
}
