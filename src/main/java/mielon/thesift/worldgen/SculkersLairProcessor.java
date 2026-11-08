package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Direction.Plane;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class SculkersLairProcessor implements StructureProcessor {
   public static final MapCodec<SculkersLairProcessor> CODEC = MapCodec.unit(SculkersLairProcessor::new);

   @Override
   public MapCodec<SculkersLairProcessor> codec() {
      return CODEC;
   }

   @Override
   public List<StructureBlockInfo> finalizeProcessing(
      ServerLevelAccessor level,
      BlockPos origin,
      BlockPos pivot,
      List<StructureBlockInfo> original,
      List<StructureBlockInfo> processed,
      StructurePlaceSettings settings
   ) {
      if (processed.isEmpty()) {
         return processed;
      }

      HashMap<BlockPos, BlockState> states = new HashMap<>();
      int minX = Integer.MAX_VALUE;
      int minY = minX;
      int minZ = minX;
      int maxX = Integer.MIN_VALUE;
      int maxY = maxX;
      int maxZ = maxX;

      for (StructureBlockInfo info : processed) {
         BlockPos p = info.pos();
         states.put(p, info.state());
         minX = Math.min(minX, p.getX());
         minY = Math.min(minY, p.getY());
         minZ = Math.min(minZ, p.getZ());
         maxX = Math.max(maxX, p.getX());
         maxY = Math.max(maxY, p.getY());
         maxZ = Math.max(maxZ, p.getZ());
      }

      Set<BlockPos> wet = fluidFootprint(states);
      long seed = level.getLevel().getSeed() ^ 5999732914756796745L;
      HashSet<BlockPos> stone = new HashSet<>();
      HashSet<BlockPos> surface = new HashSet<>();

      for (StructureBlockInfo info : processed) {
         BlockPos p = info.pos();
         BlockState state = info.state();
         if (p.getX() > minX + 1
            && p.getX() < maxX - 1
            && p.getY() > minY + 1
            && p.getY() < maxY - 1
            && p.getZ() > minZ + 1
            && p.getZ() < maxZ - 1
            && state.getFluidState().isEmpty()
            && (state.is(ModBlocks.SOULSHALE) || state.is(ModBlocks.SOULSHALE_SLAB) || state.is(ModBlocks.SOULSHALE_STAIRS))) {
            stone.add(p);

            for (Direction face : Direction.values()) {
               if (isExposed(states, p.relative(face))) {
                  surface.add(p);
                  break;
               }
            }
         }
      }

      ArrayList<BlockPos> order = new ArrayList<>(surface);
      order.sort(Comparator.comparingLong(px -> hash(px, seed)));
      HashMap<BlockPos, SculkersLairProcessor.Lobe> lobes = new HashMap<>();

      for (BlockPos p : order) {
         SculkersLairProcessor.Lobe shape = lobe(p, surface, states);
         if (shape != null) {
            lobes.put(p, shape);
         }
      }

      order.sort(Comparator.<BlockPos>comparingInt(px -> priority(lobes.get(px))).thenComparingLong(px -> hash(px, seed)));
      HashSet<BlockPos> visited = new HashSet<>();
      HashSet<BlockPos> patches = new HashSet<>();
      ArrayList<BlockPos> centers = new ArrayList<>();
      int[] counts = new int[3];

      for (BlockPos first : order) {
         if (visited.add(first)) {
            HashSet<BlockPos> component = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(first);

            while (!queue.isEmpty()) {
               BlockPos p = queue.removeFirst();
               component.add(p);

               for (Direction face : Direction.values()) {
                  BlockPos next = p.relative(face);
                  if (surface.contains(next) && visited.add(next)) {
                     queue.addLast(next);
                  }
               }
            }

            if (component.size() >= 12) {
               int count = 0;

               for (BlockPos center : order) {
                  if (component.contains(center) && !centers.stream().anyMatch(px -> px.distSqr(center) < 49.0)) {
                     SculkersLairProcessor.Lobe lobe = lobes.get(center);
                     if (lobe != null) {
                        int kind = priority(lobe);
                        if (counts[kind] < (kind == 2 ? 1 : 2)) {
                           HashSet<BlockPos> colony = new HashSet<>();
                           int dx = (int)Math.ceil(lobe.rx());
                           int dy = (int)Math.ceil(lobe.ry());
                           int dz = (int)Math.ceil(lobe.rz());

                           for (int x = -dx; x <= dx; x++) {
                              for (int y = -dy; y <= dy; y++) {
                                 for (int z = -dz; z <= dz; z++) {
                                    BlockPos p = center.offset(x, y, z);
                                    if (stone.contains(p)) {
                                       double distance = x * x / (lobe.rx() * lobe.rx()) + y * y / (lobe.ry() * lobe.ry()) + z * z / (lobe.rz() * lobe.rz());
                                       double edge = 0.9 + 0.16 * SiftOrganicNoise.sample(seed ^ 31L, p.getX() / 2.9, p.getY() / 2.9, p.getZ() / 2.9);
                                       if (distance < edge) {
                                          colony.add(p);
                                       }
                                    }
                                 }
                              }
                           }

                           HashSet<BlockPos> connected = new HashSet<>();
                           queue.add(center);

                           while (!queue.isEmpty()) {
                              BlockPos p = queue.removeFirst();
                              if (colony.contains(p) && connected.add(p)) {
                                 for (Direction face : Direction.values()) {
                                    queue.addLast(p.relative(face));
                                 }
                              }
                           }

                           if (connected.stream().filter(surface::contains).count() >= 12L) {
                              patches.addAll(connected);
                              centers.add(center);
                              counts[kind]++;
                              if (++count >= (component.size() > 180 ? 4 : 2)) {
                                 break;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      for (BlockPos p : patches) {
         states.put(p, Blocks.SCULK.defaultBlockState());
      }

      ArrayList<BlockPos> sorted = new ArrayList<>(surface);
      sorted.sort(Comparator.comparingLong(px -> hash(px, seed)));
      ArrayList<BlockPos> catalysts = new ArrayList<>();

      for (BlockPos p : sorted) {
         BlockPos above = p.above();
         if (isAir(states, above)
            && !nearWet(wet, above)
            && states.get(p).isFaceSturdy(level, p, Direction.UP)
            && !patches.stream().noneMatch(q -> q.distSqr(p) <= 9.0)) {
            int walls = 0;

            for (Direction face : Plane.HORIZONTAL) {
               if (!isAir(states, above.relative(face))) {
                  walls++;
               }
            }

            if (walls >= 2 && catalysts.size() < 2 && !catalysts.stream().anyMatch(q -> q.distSqr(above) < 36.0)) {
               states.put(above, Blocks.SCULK_CATALYST.defaultBlockState());
               catalysts.add(above);
            }
         }
      }

      HashMap<BlockPos, Integer> fringe = new HashMap<>();

      for (BlockPos p : patches) {
         for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
               for (int z = -2; z <= 2; z++) {
                  int d = x * x + y * y + z * z;
                  if (d <= 5) {
                     fringe.merge(p.offset(x, y, z), d, Math::min);
                  }
               }
            }
         }
      }

      for (StructureBlockInfo info : processed) {
         BlockPos p = info.pos();
         BlockState state = states.get(p);
         if ((state.isAir() || state.is(Blocks.SCULK_VEIN)) && state.getFluidState().isEmpty()) {
            boolean existing = state.is(Blocks.SCULK_VEIN);
            boolean supported = false;
            if (!nearWet(wet, p)) {
               BlockState previous = state.mirror(settings.getMirror()).rotate(settings.getRotation());
               BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();

               for (Direction face : Direction.values()) {
                  BlockPos neighbour = p.relative(face);
                  BlockState support = states.get(neighbour);
                  if (support != null && support.getFluidState().isEmpty()) {
                     boolean keep = existing && previous.getValue(MultifaceBlock.getFaceProperty(face));
                     int d = fringe.getOrDefault(neighbour, 99);
                     double chance = d == 0 ? 0.2 : 0.85 - Math.sqrt(d) * 0.25;
                     double roll = (hash(p.relative(face), seed ^ face.ordinal()) >>> 11) * 1.110223E-16F;
                     if ((keep || d <= 5 && roll < chance)
                        && support.mirror(settings.getMirror()).rotate(settings.getRotation()).isFaceSturdy(level, neighbour, face.getOpposite())) {
                        vein = vein.setValue(MultifaceBlock.getFaceProperty(face), true);
                        supported = true;
                     }
                  }
               }

               if (supported) {
                  states.put(p, templateState(vein, settings));
               } else if (existing) {
                  states.put(p, Blocks.AIR.defaultBlockState());
               }
            }
         }
      }

      List<StructureBlockInfo> result = new ArrayList<>(processed.size());

      for (StructureBlockInfo info : processed) {
         result.add(new StructureBlockInfo(info.pos(), states.get(info.pos()), info.nbt()));
      }

      return result;
   }

   private static SculkersLairProcessor.Lobe lobe(BlockPos p, Set<BlockPos> surface, Map<BlockPos, BlockState> states) {
      SculkersLairProcessor.Lobe best = null;
      double area = 0.0;

      for (Direction face : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN}) {
         if (isExposed(states, p.relative(face))) {
            Axis axis = face.getAxis();
            double[] radii = new double[]{1.8, 1.8, 1.8};
            double product = 1.0;

            for (Axis tangent : Axis.values()) {
               if (tangent != axis) {
                  Direction positive = Direction.fromAxisAndDirection(tangent, AxisDirection.POSITIVE);
                  int a = run(p, positive, face, surface, states);
                  int b = run(p, positive.getOpposite(), face, surface, states);
                  double radius = Math.min(3.8, Math.min(a, b) - 0.35);
                  radii[tangent.ordinal()] = radius;
                  product *= radius;
               }
            }

            if (product >= 5.5
               && radii[0] > 0.0
               && radii[1] > 0.0
               && radii[2] > 0.0
               && (best == null || priority(face) < priority(best.face()) || priority(face) == priority(best.face()) && product > area)) {
               area = product;
               best = new SculkersLairProcessor.Lobe(p, radii[0], radii[1], radii[2], face);
            }
         }
      }

      return best;
   }

   private static int priority(SculkersLairProcessor.Lobe lobe) {
      return lobe == null ? 3 : priority(lobe.face());
   }

   private static int priority(Direction face) {
      return face == Direction.UP ? 0 : (face == Direction.DOWN ? 2 : 1);
   }

   private static int run(BlockPos p, Direction direction, Direction normal, Set<BlockPos> surface, Map<BlockPos, BlockState> states) {
      int n;
      for (n = 0; n < 6; n++) {
         BlockPos next = p.relative(direction, n + 1);
         boolean found = false;

         for (int step = -1; step <= 1; step++) {
            BlockPos q = next.relative(normal, step);
            if (surface.contains(q) && isExposed(states, q.relative(normal))) {
               found = true;
               break;
            }
         }

         if (!found) {
            break;
         }
      }

      return n;
   }

   private static boolean isExposed(Map<BlockPos, BlockState> states, BlockPos p) {
      BlockState s = states.get(p);
      return s != null && s.getFluidState().isEmpty() && (s.isAir() || s.canBeReplaced());
   }

   private static boolean isAir(Map<BlockPos, BlockState> states, BlockPos p) {
      BlockState s = states.get(p);
      return s != null && s.isAir();
   }

   private static Set<BlockPos> fluidFootprint(Map<BlockPos, BlockState> states) {
      HashMap<BlockPos, Integer> reach = new HashMap<>();
      ArrayDeque<SculkersLairProcessor.Wet> queue = new ArrayDeque<>();

      for (Entry<BlockPos, BlockState> entry : states.entrySet()) {
         if (!entry.getValue().getFluidState().isEmpty()) {
            reach.put(entry.getKey(), 7);
            queue.add(new SculkersLairProcessor.Wet(entry.getKey(), 7));
         }
      }

      while (!queue.isEmpty()) {
         SculkersLairProcessor.Wet wet = queue.removeFirst();

         for (Direction face : Direction.values()) {
            if (face != Direction.UP) {
               int nextReach = face == Direction.DOWN ? 7 : wet.reach() - 1;
               if (nextReach >= 0) {
                  BlockPos p = wet.pos().relative(face);
                  BlockState state = states.get(p);
                  if (state != null && (state.isAir() || state.is(Blocks.SCULK_VEIN)) && reach.getOrDefault(p, -1) < nextReach) {
                     reach.put(p, nextReach);
                     queue.addLast(new SculkersLairProcessor.Wet(p, nextReach));
                  }
               }
            }
         }
      }

      return reach.keySet();
   }

   private static boolean nearWet(Set<BlockPos> wet, BlockPos p) {
      if (wet.contains(p)) {
         return true;
      }

      for (Direction face : Direction.values()) {
         if (wet.contains(p.relative(face))) {
            return true;
         }
      }

      return false;
   }

   private static BlockState templateState(BlockState worldState, StructurePlaceSettings settings) {
      Rotation inverse = switch (settings.getRotation()) {
         case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
         case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
         default -> settings.getRotation();
      };
      return worldState.rotate(inverse).mirror(settings.getMirror());
   }

   private static long hash(BlockPos p, long seed) {
      return SiftTerrainShapeNoise.hash(seed + p.getX() * -7046029254386353131L + p.getY() * 7146057691288625177L + p.getZ() * -7723592293110705685L);
   }

   private record Lobe(BlockPos center, double rx, double ry, double rz, Direction face) {
   }

   private record Wet(BlockPos pos, int reach) {
   }
}
