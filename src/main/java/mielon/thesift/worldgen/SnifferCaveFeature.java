package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class SnifferCaveFeature extends Feature<NoneFeatureConfiguration> {
   private static final Direction[] HORIZONTAL = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   private static final Block[] NEST_PLANTS = new Block[]{
      ModBlocks.OVERGROWN_FRONDS,
      ModBlocks.OVERGROWN_CHARD,
      ModBlocks.OVERGROWN_STALKS,
      ModBlocks.SIFTSLATE_STALKS,
      ModBlocks.HEALTHY_SCULK_SPROUTS,
      ModBlocks.DRY_HEALTHY_SCULK_SPROUTS
   };
   private final int cellChunks;
   private final long placementSalt;
   private final int candidateCount;

   public SnifferCaveFeature(int cellChunks, long placementSalt, int candidateCount) {
      super(NoneFeatureConfiguration.CODEC);
      this.cellChunks = cellChunks;
      this.placementSalt = placementSalt;
      this.candidateCount = candidateCount;
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      if (!SiftLandmarkPlacement.isSelectedChunk(level.getSeed(), context.origin(), this.cellChunks, this.placementSalt, this.candidateCount)) {
         return false;
      }

      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(context.origin());
      int length = 10 + random.nextInt(5);
      SnifferCaveFeature.CaveEntrance entrance = findEntrance(context);
      Direction direction = entrance == null ? null : chooseDirection(level, bounds, entrance.pos(), length, random);
      if (entrance != null && direction != null) {
         Set<BlockPos> interior = new HashSet<>();
         Set<BlockPos> shell = new HashSet<>();
         Direction sideways = direction.getClockWise();
         double phase = random.nextDouble() * Math.PI * 2.0;
         BlockPos lastCenter = entrance.pos();

         for (int distance = 0; distance <= length; distance += 2) {
            double progress = (double)distance / length;
            int lateral = (int)Math.round(Math.sin(phase + progress * Math.PI * 1.7) * (0.6 + progress * 1.35));
            int lift = (int)Math.round(Math.sin(progress * Math.PI) * 0.9) - (int)Math.round(progress * 6.0);
            BlockPos center = entrance.pos().relative(direction, distance).relative(sideways, lateral).above(lift);
            double swell = Math.sin(progress * Math.PI);
            double radiusXz = 2.1 + swell * 1.65 + random.nextDouble() * 0.35;
            double radiusY = 1.9 + swell * 1.15 + random.nextDouble() * 0.25;
            addEllipsoid(center, radiusXz, radiusY, radiusXz * 0.92, interior, shell);
            lastCenter = center;
         }

         BlockPos nestCenter = lastCenter.relative(direction).above();
         addEllipsoid(nestCenter, 5.2, 3.7, 5.0, interior, shell);
         SnifferCaveFeature.IchorPoolPlan pool = createIchorPoolPlan(nestCenter, sideways, random);
         addOrientedEllipsoid(
            pool.chamberCenter(), pool.chamberOutwardRadius(), pool.chamberVerticalRadius(), pool.chamberCrossRadius(), pool.outward(), interior, shell
         );
         shell.removeAll(interior);
         SnifferCaveFeature.IchorBasin basin = createIchorBasin(pool);
         Set<BlockPos> writes = new HashSet<>(interior);
         writes.addAll(shell);
         writes.addAll(basin.fluid());
         writes.addAll(basin.lining());
         writes.addAll(basin.clearance());

         for (BlockPos write : writes) {
            if (TheSiftDimension.isOrganicDesert(SiftCaveHabitat.surfaceBiome(level, write.getX(), write.getZ()))) {
               return false;
            }
         }

         if (!SiftFeaturePlacementGuard.intersectsPortal(nestCenter, 12)
            && validMass(level, bounds, nestCenter)
            && canCarveAll(level, bounds, writes)
            && enclosedShell(level, shell, entrance.pos())) {
            Map<Long, int[]> spans = new HashMap<>();

            for (BlockPos pos : writes) {
               long column = BlockPos.asLong(pos.getX(), 0, pos.getZ());
               int[] span = spans.computeIfAbsent(column, key -> new int[]{pos.getY(), pos.getY()});
               span[0] = Math.min(span[0], pos.getY());
               span[1] = Math.max(span[1], pos.getY());
            }

            for (Entry<Long, int[]> entry : spans.entrySet()) {
               BlockPos column = BlockPos.of(entry.getKey());
               SiftTerrainFeatureMemory.protectTerrain(column.getX(), column.getZ(), entry.getValue()[0], entry.getValue()[1]);
               SiftTerrainFeatureMemory.excludeRibs(column.getX(), column.getZ(), entry.getValue()[0], entry.getValue()[1]);
            }

            for (BlockPos pos : shell) {
               if (!bounds.contains(pos)) {
                  return false;
               }

               BlockState current = level.getBlockState(pos);
               if ((isSiftTerrain(current) || current.isAir())
                  && level.ensureCanWrite(pos)
                  && (isSiftTerrain(current) || distanceSquared(pos, entrance.pos()) > 64)) {
                  float healthyChance = 0.18F + (distanceSquared(pos, nestCenter) < 90 ? 0.13F : 0.0F);
                  BlockState lining = caveSoil(level, pos, random.nextFloat() < healthyChance);
                  level.setBlock(pos, lining, 2);
               }
            }

            for (BlockPos pos : interior) {
               if (bounds.contains(pos) && level.ensureCanWrite(pos)) {
                  level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
               }
            }

            placeIchorPool(level, random, basin);
            scatterSnifferPlants(level, random, interior, nestCenter);
            List<BlockPos> nest = buildNest(level, random, nestCenter);
            SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.SNIFFER_CAVE, nestCenter);
            if (nest.isEmpty()) {
               return true;
            }

            placeNestContents(level, random, nest);
            SiftDeferredSnifferSpawner.enqueueFamily(level, random, nest);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static SnifferCaveFeature.CaveEntrance findEntrance(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      int x = (context.origin().getX() & -16) + 8;
      int z = (context.origin().getZ() & -16) + 8;
      int surface = SiftCaveHabitat.rockTop(level, x, z) + 1;
      BlockPos entrance = new BlockPos(x, surface, z);
      return surface > level.getMinY() + 18 && isSiftTerrain(level.getBlockState(entrance.below())) ? new SnifferCaveFeature.CaveEntrance(entrance) : null;
   }

   private static Direction chooseDirection(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos entrance, int length, RandomSource random) {
      List<SnifferCaveFeature.DirectionScore> candidates = new ArrayList<>();
      int start = random.nextInt(HORIZONTAL.length);

      for (int index = 0; index < HORIZONTAL.length; index++) {
         Direction direction = HORIZONTAL[(start + index) % HORIZONTAL.length];
         BlockPos end = entrance.relative(direction, length + 1).below(5);
         if (bounds.contains(end.offset(6, 4, 6)) && bounds.contains(end.offset(-6, -5, -6))) {
            int surface = level.getHeight(Types.WORLD_SURFACE_WG, end.getX(), end.getZ());
            int cover = surface - end.getY();
            if (cover >= 5) {
               candidates.add(new SnifferCaveFeature.DirectionScore(direction, cover + random.nextInt(4)));
            }
         }
      }

      return candidates.stream()
         .max((first, second) -> Integer.compare(first.score(), second.score()))
         .map(SnifferCaveFeature.DirectionScore::direction)
         .orElse(null);
   }

   private static void addEllipsoid(BlockPos center, double radiusX, double radiusY, double radiusZ, Set<BlockPos> interior, Set<BlockPos> shell) {
      int maxX = (int)Math.ceil(radiusX + 1.0);
      int maxY = (int)Math.ceil(radiusY + 1.0);
      int maxZ = (int)Math.ceil(radiusZ + 1.0);

      for (int dx = -maxX; dx <= maxX; dx++) {
         for (int dy = -maxY; dy <= maxY; dy++) {
            for (int dz = -maxZ; dz <= maxZ; dz++) {
               double inner = square(dx / radiusX) + square(dy / radiusY) + square(dz / radiusZ);
               BlockPos pos = center.offset(dx, dy, dz);
               if (inner <= 1.0) {
                  interior.add(pos);
               } else {
                  double outer = square(dx / (radiusX + 1.0)) + square(dy / (radiusY + 1.0)) + square(dz / (radiusZ + 1.0));
                  if (outer <= 1.0) {
                     shell.add(pos);
                  }
               }
            }
         }
      }
   }

   private static void addOrientedEllipsoid(
      BlockPos center, double outwardRadius, double verticalRadius, double crossRadius, Direction outward, Set<BlockPos> interior, Set<BlockPos> shell
   ) {
      double radiusX = outward.getAxis() == Axis.X ? outwardRadius : crossRadius;
      double radiusZ = outward.getAxis() == Axis.Z ? outwardRadius : crossRadius;
      addEllipsoid(center, radiusX, verticalRadius, radiusZ, interior, shell);
   }

   private static SnifferCaveFeature.IchorPoolPlan createIchorPoolPlan(BlockPos nestCenter, Direction sideways, RandomSource random) {
      int variant = random.nextInt(4);
      Direction outward = random.nextBoolean() ? sideways : sideways.getOpposite();

      double chamberOutward = switch (variant) {
         case 1 -> 4.4;
         case 2 -> 3.8;
         case 3 -> 4.1;
         default -> 4.0;
      };

      double chamberCross = switch (variant) {
         case 1 -> 2.7;
         case 2 -> 3.5;
         case 3 -> 3.1;
         default -> 3.0;
      };
      double chamberVertical = variant == 3 ? 3.0 : 2.7;
      BlockPos center = nestCenter.relative(outward, 5).below();
      return new SnifferCaveFeature.IchorPoolPlan(center, outward, variant, chamberOutward, chamberVertical, chamberCross);
   }

   private static SnifferCaveFeature.IchorBasin createIchorBasin(SnifferCaveFeature.IchorPoolPlan plan) {
      int outwardRadius = switch (plan.variant()) {
         case 1 -> 3;
         case 2 -> 2;
         case 3 -> 3;
         default -> 2;
      };

      int crossRadius = switch (plan.variant()) {
         case 1 -> 2;
         case 2 -> 3;
         case 3 -> 2;
         default -> 2;
      };
      Direction cross = plan.outward().getClockWise();
      Set<BlockPos> surface = new HashSet<>();
      Set<BlockPos> fluid = new HashSet<>();
      Set<BlockPos> lining = new HashSet<>();
      Set<BlockPos> clearance = new HashSet<>();

      for (int along = -outwardRadius; along <= outwardRadius; along++) {
         for (int across = -crossRadius; across <= crossRadius; across++) {
            double normalized = square(along / (outwardRadius + 0.35)) + square(across / (crossRadius + 0.35));
            double edgeWobble = Math.sin(along * 2.17 + across * 1.41 + plan.variant() * 0.83) * 0.11;
            if (!(normalized > 1.0 + edgeWobble)) {
               BlockPos top = plan.chamberCenter().below(3).relative(plan.outward(), along).relative(cross, across);
               surface.add(top);
               fluid.add(top);
               if (plan.variant() == 3 && normalized < 0.55) {
                  fluid.add(top.below());
               }
            }
         }
      }

      for (BlockPos wet : fluid) {
         lining.add(wet.below());

         for (Direction side : HORIZONTAL) {
            lining.add(wet.relative(side));
         }
      }

      lining.removeAll(fluid);

      for (BlockPos top : surface) {
         for (int dy = 1; dy <= 3; dy++) {
            clearance.add(top.above(dy));
         }

         for (Direction side : HORIZONTAL) {
            BlockPos bank = top.relative(side);
            if (!surface.contains(bank)) {
               lining.add(bank.below());
               lining.add(bank.below(2));
               clearance.add(bank.above());
               clearance.add(bank.above(2));
            }
         }
      }

      lining.removeAll(fluid);
      return new SnifferCaveFeature.IchorBasin(Set.copyOf(fluid), Set.copyOf(lining), Set.copyOf(clearance));
   }

   private static void placeIchorPool(WorldGenLevel level, RandomSource random, SnifferCaveFeature.IchorBasin basin) {
      for (BlockPos pos : basin.clearance()) {
         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
      }

      for (BlockPos pos : basin.lining()) {
         level.setBlock(pos, caveSoil(level, pos, random.nextFloat() < 0.55F), 2);
      }

      for (BlockPos pos : basin.fluid()) {
         level.setBlock(pos, ModFluids.ICHOR_BLOCK.defaultBlockState(), 2);
      }
   }

   private static boolean enclosedShell(WorldGenLevel level, Set<BlockPos> shell, BlockPos entrance) {
      int gaps = 0;
      int checked = 0;

      for (BlockPos pos : shell) {
         if (distanceSquared(pos, entrance) > 64) {
            checked++;
            if (!isSiftTerrain(level.getBlockState(pos))) {
               if (!level.isEmptyBlock(pos)) {
                  return false;
               }

               boolean rockNearby = false;

               for (Direction direction : Direction.values()) {
                  if (isSiftTerrain(level.getBlockState(pos.relative(direction)))) {
                     rockNearby = true;
                     break;
                  }
               }

               if (!rockNearby || ++gaps > shell.size() / 8) {
                  return false;
               }
            }
         }
      }

      return checked > 0;
   }

   private static boolean canCarveAll(WorldGenLevel level, SiftWorldgenBounds bounds, Set<BlockPos> interior) {
      int siftMass = 0;
      int protectedBlocks = 0;

      for (BlockPos pos : interior) {
         if (!bounds.contains(pos) || !level.ensureCanWrite(pos)) {
            return false;
         }

         BlockState state = level.getBlockState(pos);
         if (state.is(ModBlocks.REINFORCED_SIFTSLATE)
            || state.is(ModBlocks.REINFORCED_SOULSHALE)
            || state.is(ModBlocks.SIFT_PORTAL)
            || state.is(ModBlocks.SONOROUS_DEEPSLATE)
            || state.is(ModBlocks.OVERGROWN_WILLOW_LOG)
            || state.is(ModBlocks.OVERGROWN_WILLOW_WOOD)
            || state.is(ModBlocks.ASH_WILLOW_LOG)
            || state.is(ModBlocks.ASH_WILLOW_WOOD)) {
            protectedBlocks++;
         } else if (isSiftTerrain(state)) {
            siftMass++;
         } else if (!isCarvableVegetation(state) && !state.isAir()) {
            return false;
         }
      }

      return protectedBlocks == 0 && siftMass >= interior.size() * 0.4;
   }

   private static boolean validMass(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos center) {
      int solid = 0;
      int total = 0;

      for (int dx = -5; dx <= 5; dx += 2) {
         for (int dy = -3; dy <= 3; dy += 2) {
            for (int dz = -5; dz <= 5; dz += 2) {
               total++;
               BlockPos sample = center.offset(dx, dy, dz);
               if (!bounds.contains(sample)) {
                  return false;
               }

               if (isSiftTerrain(level.getBlockState(sample))) {
                  solid++;
               }
            }
         }
      }

      return solid >= total * 0.78;
   }

   private static void scatterSnifferPlants(WorldGenLevel level, RandomSource random, Set<BlockPos> interior, BlockPos nestCenter) {
      List<BlockPos> sample = new ArrayList<>(interior);
      int placed = 0;
      SiftSnifferPlantPlacement.PlantType blobType = SiftSnifferPlantPlacement.chooseBlobType(random, 0.6F);

      for (int attempt = 0; attempt < 72 && placed < 14; attempt++) {
         BlockPos air = sample.get(random.nextInt(sample.size()));
         BlockPos floor = findFloor(level, air.getX(), air.getZ(), nestCenter.getY());
         if (floor != null && level.isEmptyBlock(floor.above())) {
            BlockState floorState = level.getBlockState(floor);
            if (caveFloor(floorState)) {
               BlockPos plantPos = floor.above();
               if (SiftSnifferPlantPlacement.place(level, plantPos, blobType, random)) {
                  placed++;
               }
            }
         }
      }
   }

   static boolean coralCave(WorldGenLevel level, BlockPos pos) {
      return TheSiftDimension.isCoralBiome(SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ()));
   }

   static BlockState caveSoil(WorldGenLevel level, BlockPos pos, boolean growth) {
      if (!coralCave(level, pos)) {
         return (growth ? ModBlocks.HEALTHY_SCULK : ModBlocks.DRY_HEALTHY_SCULK).defaultBlockState();
      } else {
         return !growth
            ? ModBlocks.CORAL_SOIL.defaultBlockState()
            : (CoralSurfaceFeature.brightPatch(level.getSeed(), pos.getX(), pos.getZ()) ? ModBlocks.BRIGHT_CORAL_GROWTH : ModBlocks.CORAL_GROWTH)
               .defaultBlockState();
      }
   }

   private static boolean caveFloor(BlockState state) {
      return state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK)
         || state.is(ModBlocks.CORAL_SOIL)
         || state.is(ModBlocks.CORAL_GROWTH)
         || state.is(ModBlocks.BRIGHT_CORAL_GROWTH);
   }

   private static List<BlockPos> buildNest(WorldGenLevel level, RandomSource random, BlockPos center) {
      List<BlockPos> nest = new ArrayList<>();
      BlockState foliage = (coralCave(level, center) ? ModBlocks.ASH_WILLOW_FOLIAGE : ModBlocks.OVERGROWN_WILLOW_FOLIAGE)
         .defaultBlockState()
         .setValue(LeavesBlock.PERSISTENT, true);

      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            double distance = square(dx / 3.25) + square(dz / 3.25);
            if (!(distance > 1.0) && (!(distance > 0.68) || !(random.nextFloat() < 0.34F))) {
               BlockPos floor = findFloor(level, center.getX() + dx, center.getZ() + dz, center.getY());
               if (floor != null && level.isEmptyBlock(floor.above())) {
                  BlockState current = level.getBlockState(floor);
                  if (caveFloor(current)) {
                     level.setBlock(floor, foliage, 2);
                     nest.add(floor);
                  }
               }
            }
         }
      }

      return nest;
   }

   private static void placeNestContents(WorldGenLevel level, RandomSource random, List<BlockPos> nest) {
      int eggs = random.nextFloat() < 0.5F ? 1 : 0;
      if (eggs == 1 && random.nextFloat() < 0.1F) {
         eggs++;
      }

      for (int index = 0; index < eggs; index++) {
         placeOnRandomFreeNestBlock(level, random, nest, Blocks.SNIFFER_EGG.defaultBlockState());
      }

      int plants = 4 + random.nextInt(5);

      for (int index = 0; index < plants; index++) {
         Block plant = NEST_PLANTS[random.nextInt(NEST_PLANTS.length)];
         placeOnRandomFreeNestBlock(level, random, nest, plant.defaultBlockState());
      }
   }

   private static void placeOnRandomFreeNestBlock(WorldGenLevel level, RandomSource random, List<BlockPos> nest, BlockState state) {
      for (int attempt = 0; attempt < 18; attempt++) {
         BlockPos pos = nest.get(random.nextInt(nest.size())).above();
         if (level.isEmptyBlock(pos) && state.canSurvive(level, pos)) {
            level.setBlock(pos, state, 2);
            return;
         }
      }
   }

   private static BlockPos findFloor(WorldGenLevel level, int x, int z, int aroundY) {
      for (int y = aroundY + 3; y >= aroundY - 7; y--) {
         BlockPos air = new BlockPos(x, y, z);
         BlockPos floor = air.below();
         if (level.getBlockState(air).isAir() && isSiftTerrain(level.getBlockState(floor))) {
            return floor;
         }
      }

      return null;
   }

   private static boolean isCarvableVegetation(BlockState state) {
      return state.is(ModBlocks.CORAL_BUSH)
         || state.is(ModBlocks.BRIGHT_CORAL_BUSH)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.SIFTSLATE_HANGING_ROOTS)
         || state.is(ModBlocks.OVERGROWN_HANGING_ROOTS);
   }

   private static boolean isSiftTerrain(BlockState state) {
      return state.is(ModBlocks.SOULSHALE)
         || state.is(ModBlocks.CORAL_SOIL)
         || state.is(ModBlocks.CORAL_GROWTH)
         || state.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || state.is(ModBlocks.SIFTSLATE)
         || state.is(ModBlocks.SIFTSLATE_GROWTH)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   private static int distanceSquared(BlockPos first, BlockPos second) {
      int dx = first.getX() - second.getX();
      int dy = first.getY() - second.getY();
      int dz = first.getZ() - second.getZ();
      return dx * dx + dy * dy + dz * dz;
   }

   private static double square(double value) {
      return value * value;
   }

   private record CaveEntrance(BlockPos pos) {
   }

   private record DirectionScore(Direction direction, int score) {
   }

   private record IchorBasin(Set<BlockPos> fluid, Set<BlockPos> lining, Set<BlockPos> clearance) {
   }

   private record IchorPoolPlan(
      BlockPos chamberCenter, Direction outward, int variant, double chamberOutwardRadius, double chamberVerticalRadius, double chamberCrossRadius
   ) {
   }
}
