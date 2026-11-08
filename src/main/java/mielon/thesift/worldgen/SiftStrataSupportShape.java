package mielon.thesift.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SiftStrataSupportShape {
   public static final int SPACING = 24;
   public static final int SEARCH_RADIUS = 6;
   public static final int FOOTPRINT_RADIUS = 8;

   private SiftStrataSupportShape() {
   }

   public static long salt(long seed, int cellX, int cellZ) {
      return SiftTerrainShapeNoise.hash(seed ^ 2274514806455132557L ^ cellX * 7146057691288625177L ^ cellZ * -7046029254386353131L);
   }

   public static int anchorX(long salt, int cellX) {
      return cellX * 24 + 12 + (int)(SiftTerrainShapeNoise.unit(salt) * 9.0) - 4;
   }

   public static int anchorZ(long salt, int cellZ) {
      return cellZ * 24 + 12 + (int)(SiftTerrainShapeNoise.unit(salt ^ 9665955L) * 9.0) - 4;
   }

   public static boolean contains(long salt, int anchorX, int anchorZ, int bottom, int top, int x, int y, int z) {
      double t = Math.max(0.0, Math.min(1.0, (double)(y - bottom) / Math.max(1, top - bottom)));
      double lean = Math.sin(t * Math.PI);
      double phase = SiftTerrainShapeNoise.unit(salt ^ 6648737L) * Math.PI * 2.0;
      double cx = anchorX + Math.cos(phase) * lean * 1.3;
      double cz = anchorZ + Math.sin(phase) * lean * 1.3;
      double radius = 2.8
         + SiftTerrainShapeNoise.unit(salt ^ 8641201L) * 0.9
         + 2.1 * Math.pow(1.0 - t, 4.0)
         + 1.6 * Math.pow(t, 6.0)
         + 0.35 * SiftTerrainShapeNoise.noise(salt, y, 8.0);
      double aspect = 0.85 + SiftTerrainShapeNoise.unit(salt ^ 466339L) * 0.3;
      double dx = (x - cx) / aspect;
      double dz = (z - cz) * aspect;
      return dx * dx + dz * dz <= radius * radius;
   }

   public static int attachmentY(SiftStrataSupportShape.Terrain terrain, SiftStrataSupportShape.Support support, int x, int z) {
      if (!terrain.canSample(x, z)) {
         return Integer.MIN_VALUE;
      }

      for (int y = support.ceiling() - 4; y <= support.ceiling() + 12; y++) {
         if (terrain.ground(x, y, z) && terrain.ground(x, y + 1, z) && terrain.ground(x, y + 2, z)) {
            return y;
         }
      }

      return Integer.MIN_VALUE;
   }

   public static int capTop(SiftStrataSupportShape.Terrain terrain, SiftStrataSupportShape.Support support, int x, int z) {
      int attachment = attachmentY(terrain, support, x, z);
      if (attachment != Integer.MIN_VALUE) {
         return attachment + 1;
      }

      double distance = Math.hypot(x - support.x(), z - support.z());
      return support.ceiling() - 3 - (int)Math.ceil(distance * 0.65);
   }

   public static List<SiftStrataSupportShape.Voxel> floatingFragments(SiftStrataSupportShape.Terrain terrain, int minX, int minZ) {
      List<SiftStrataSupportShape.Voxel> removed = new ArrayList<>();
      Set<SiftStrataSupportShape.Voxel> inspected = new HashSet<>();
      int[][] neighbours = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

      for (int x = minX; x <= minX + 15; x++) {
         for (int z = minZ; z <= minZ + 15; z++) {
            if (terrain.canSample(x, z)) {
               for (int y = Math.min(238, terrain.surfaceY(x, z)); y >= 60; y--) {
                  SiftStrataSupportShape.Voxel start = new SiftStrataSupportShape.Voxel(x, y, z);
                  if (!inspected.contains(start) && terrain.ground(x, y, z) && terrain.air(x, y + 1, z) && !terrain.ground(x, y - 4, z)) {
                     ArrayDeque<SiftStrataSupportShape.Voxel> queue = new ArrayDeque<>();
                     Set<SiftStrataSupportShape.Voxel> seen = new HashSet<>();
                     List<SiftStrataSupportShape.Voxel> component = new ArrayList<>();
                     queue.add(start);
                     boolean detached = true;

                     while (!queue.isEmpty()) {
                        SiftStrataSupportShape.Voxel p = queue.removeFirst();
                        if (seen.add(p)) {
                           if (terrain.canSample(p.x(), p.z()) && p.y() >= 60 && p.y() <= 238) {
                              if (!terrain.ground(p.x(), p.y(), p.z())) {
                                 continue;
                              }

                              if (p.x() >= minX && p.x() <= minX + 15 && p.z() >= minZ && p.z() <= minZ + 15) {
                                 component.add(p);
                                 if (component.size() > 192) {
                                    detached = false;
                                    break;
                                 }

                                 for (int[] d : neighbours) {
                                    queue.addLast(new SiftStrataSupportShape.Voxel(p.x() + d[0], p.y() + d[1], p.z() + d[2]));
                                 }
                                 continue;
                              }

                              detached = false;
                              break;
                           }

                           detached = false;
                           break;
                        }
                     }

                     inspected.addAll(seen);
                     if (detached) {
                        removed.addAll(component);
                     }
                  }
               }
            }
         }
      }

      return removed;
   }

   public static SiftStrataSupportShape.Support findSupport(SiftStrataSupportShape.Terrain terrain, int centerX, int centerZ) {
      SiftStrataSupportShape.Support best = null;
      int bestDistance = Integer.MAX_VALUE;
      int radius = 6;

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int distance = dx * dx + dz * dz;
            if (distance <= radius * radius && distance < bestDistance) {
               int x = centerX + dx;
               int z = centerZ + dz;
               if (terrain.canSample(x - 3, z - 3) && terrain.canSample(x + 3, z + 3)) {
                  int top = Math.min(238, terrain.surfaceY(x, z));
                  if (top >= 70 && terrain.ground(x, top, z) && terrain.air(x, top + 1, z) && terrain.ground(x, top - 2, z)) {
                     int ceiling = top;

                     while (ceiling > 12 && terrain.ground(x, ceiling - 1, z)) {
                        ceiling--;
                     }

                     if (ceiling <= 32) {
                        if (distance == 0) {
                           return null;
                        }
                     } else {
                        int floor = 12;

                        while (floor < 79 && !terrain.ground(x, floor, z)) {
                           floor++;
                        }

                        while (floor < 79 && terrain.ground(x, floor + 1, z)) {
                           floor++;
                        }

                        if (floor < ceiling - 7 && terrain.ground(x, floor, z)) {
                           int attachedSides = 0;

                           for (int[] side : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                              int sideTop = terrain.surfaceY(x + side[0], z + side[1]);
                              if (Math.abs(sideTop - top) <= 5) {
                                 attachedSides++;
                              }
                           }

                           if (attachedSides >= 2) {
                              best = new SiftStrataSupportShape.Support(x, z, floor, ceiling);
                              bestDistance = distance;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   public record Support(int x, int z, int floor, int ceiling) {
   }

   public interface Terrain {
      boolean ground(int var1, int var2, int var3);

      boolean air(int var1, int var2, int var3);

      int surfaceY(int var1, int var2);

      boolean canSample(int var1, int var2);

      default boolean protectedRock(int x, int y, int z) {
         return false;
      }
   }

   public record Voxel(int x, int y, int z) {
   }
}
