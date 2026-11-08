package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.OvergrownWillowVinesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class OvergrownWillowTreeFeature extends Feature<NoneFeatureConfiguration> {
   private static final OvergrownWillowTreeFeature.TemplateChoice[] VARIANTS = new OvergrownWillowTreeFeature.TemplateChoice[]{
      template("overgrown_willow_1", 5, 3),
      template("overgrown_willow_2", 5, 3),
      template("overgrown_willow_3", 2, 3),
      template("overgrown_willow_4", 2, 3),
      template("overgrown_willow_5", 3, 3),
      template("overgrown_willow_6", 3, 3),
      template("overgrown_willow_7", 2, 2)
   };
   private static final OvergrownWillowTreeFeature.TemplateChoice SMALL = VARIANTS[6];
   private static final Direction[] ROOT_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

   public OvergrownWillowTreeFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos placementOrigin = context.origin();
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(placementOrigin);
      int minX = placementOrigin.getX() & -16;
      int minZ = placementOrigin.getZ() & -16;
      int rootX = minX + random.nextInt(16);
      int rootZ = minZ + random.nextInt(16);
      int rootY = SiftCaveHabitat.rockTop(level, rootX, rootZ) + 1;
      BlockPos root = new BlockPos(rootX, rootY, rootZ);
      if (SiftCaveHabitat.surfaceTreeFloor(level, root.below())
         && !SiftFeaturePlacementGuard.intersectsPortal(root, 12)
         && isTreeGround(level.getBlockState(root.below()))) {
         OvergrownWillowTreeFeature.TemplateChoice choice = random.nextFloat() < 0.38F ? VARIANTS[random.nextInt(6)] : SMALL;
         return placeAt(level, random, root, bounds, choice);
      } else {
         return false;
      }
   }

   public static boolean hasSaplingGroup(LevelReader level, BlockPos pos) {
      return saplingSquare(level, pos) != null;
   }

   private static OvergrownWillowTreeFeature.SaplingSquare saplingSquare(LevelReader level, BlockPos pos) {
      for (int size : new int[]{3, 2}) {
         for (int dx = 1 - size; dx <= 0; dx++) {
            for (int dz = 1 - size; dz <= 0; dz++) {
               BlockPos corner = pos.offset(dx, 0, dz);
               boolean complete = true;

               for (int x = 0; x < size && complete; x++) {
                  for (int z = 0; z < size; z++) {
                     BlockPos candidate = corner.offset(x, 0, z);
                     BlockState state = level.getBlockState(candidate);
                     if (!state.is(ModBlocks.OVERGROWN_WILLOW_SAPLING) || !state.canSurvive(level, candidate)) {
                        complete = false;
                        break;
                     }
                  }
               }

               if (complete) {
                  return new OvergrownWillowTreeFeature.SaplingSquare(corner, size);
               }
            }
         }
      }

      return null;
   }

   public static boolean growFromSapling(ServerLevel level, RandomSource random, BlockPos pos) {
      OvergrownWillowTreeFeature.SaplingSquare square = saplingSquare(level, pos);
      if (square == null) {
         return false;
      }

      List<BlockState> original = new ArrayList<>();

      for (int x = 0; x < square.size(); x++) {
         for (int z = 0; z < square.size(); z++) {
            BlockPos sapling = square.corner().offset(x, 0, z);
            original.add(level.getBlockState(sapling));
            level.removeBlock(sapling, false);
         }
      }

      OvergrownWillowTreeFeature.TemplateChoice choice = square.size() == 2 ? SMALL : VARIANTS[random.nextInt(6)];
      boolean grown = placeAt(level, random, square.corner(), SiftWorldgenBounds.aroundWithNeighbourMargin(square.corner()), choice);
      if (!grown) {
         int index = 0;

         for (int x = 0; x < square.size(); x++) {
            for (int z = 0; z < square.size(); z++) {
               level.setBlock(square.corner().offset(x, 0, z), original.get(index++), 3);
            }
         }
      }

      return grown;
   }

   static boolean placeAt(WorldGenLevel level, RandomSource random, BlockPos root, SiftWorldgenBounds bounds, boolean large) {
      return !SiftFeaturePlacementGuard.intersectsPortal(root, 12) && isTreeGround(level.getBlockState(root.below()))
         ? placeAt(level, random, root, bounds, large ? VARIANTS[random.nextInt(6)] : SMALL)
         : false;
   }

   private static boolean placeAt(
      WorldGenLevel level, RandomSource random, BlockPos root, SiftWorldgenBounds bounds, OvergrownWillowTreeFeature.TemplateChoice choice
   ) {
      Optional<StructureTemplate> optional = level.getLevel().getStructureManager().get(choice.id());
      if (optional.isEmpty()) {
         return false;
      }

      StructureTemplate template = optional.get();
      Rotation rotation = Rotation.getRandom(random);
      Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.LEFT_RIGHT;
      BlockPos pivot = choice.root();
      BlockPos templateOrigin = root.offset(-pivot.getX(), 0, -pivot.getZ());
      StructurePlaceSettings settings = new StructurePlaceSettings()
         .setMirror(mirror)
         .setRotation(rotation)
         .setRotationPivot(pivot)
         .setIgnoreEntities(true)
         .setKnownShape(false)
         .setRandom(random);
      List<StructureBlockInfo> logs = template.filterBlocks(templateOrigin, settings, ModBlocks.OVERGROWN_WILLOW_LOG);
      if (logs.isEmpty()) {
         return false;
      }

      int baseY = logs.stream().mapToInt(info -> info.pos().getY()).min().orElse(root.getY());
      int baseX = logs.stream().filter(info -> info.pos().getY() == baseY).mapToInt(info -> info.pos().getX()).min().orElse(root.getX());
      int baseZ = logs.stream().filter(info -> info.pos().getY() == baseY).mapToInt(info -> info.pos().getZ()).min().orElse(root.getZ());
      templateOrigin = templateOrigin.offset(root.getX() - baseX, root.getY() - baseY, root.getZ() - baseZ);
      if (SiftCampProtection.intersects(level, template.getBoundingBox(settings, templateOrigin))) {
         return false;
      }

      logs = template.filterBlocks(templateOrigin, settings, ModBlocks.OVERGROWN_WILLOW_LOG);
      List<StructureBlockInfo> wood = template.filterBlocks(templateOrigin, settings, ModBlocks.OVERGROWN_WILLOW_WOOD);
      List<StructureBlockInfo> foliage = template.filterBlocks(templateOrigin, settings, ModBlocks.OVERGROWN_WILLOW_FOLIAGE);
      List<StructureBlockInfo> vines = template.filterBlocks(templateOrigin, settings, ModBlocks.OVERGROWN_WILLOW_VINES);
      vines = mirrorVineFaces(vines, mirror, rotation);
      if (allInside(bounds, logs) && allInside(bounds, wood) && allInside(bounds, foliage) && allInside(bounds, vines)) {
         BlockPos actualBase = findActualBase(logs, root);
         if (actualBase == null
            || !isTreeGround(level.getBlockState(actualBase.below()))
            || hasNearbyTrunk(level, bounds, actualBase, 3)
            || !trunkFits(level, bounds, logs)
            || !trunkFits(level, bounds, wood)) {
            return false;
         }

         if (SiftCaveHabitat.lush(level, root.below()) && !trunkFits(level, bounds, foliage)) {
            return false;
         }

         List<BlockPos> trunkExtensions = WillowTrunkGrounding.plan(level, bounds, logs);
         if (trunkExtensions == null) {
            return false;
         }

         int placed = WillowTrunkGrounding.place(level, trunkExtensions, ModBlocks.OVERGROWN_WILLOW_LOG);
         placed += placeTemplateBlocks(level, bounds, logs);
         placed += placeTemplateBlocks(level, bounds, wood);
         placed += placeTemplateBlocks(level, bounds, foliage);
         placed += placeVineTemplateBlocksTopDown(level, bounds, vines);
         alignTemplateVinesToTree(level, bounds, vines);
         varyHangingVines(level, bounds, random, vines);
         return placed > 0;
      } else {
         return false;
      }
   }

   private static BlockPos findActualBase(List<StructureBlockInfo> logs, BlockPos requestedRoot) {
      BlockPos best = null;
      int lowestY = Integer.MAX_VALUE;
      int bestDistance = Integer.MAX_VALUE;

      for (StructureBlockInfo info : logs) {
         BlockPos pos = info.pos();
         int y = pos.getY();
         int distance = horizontalDistanceSquared(pos, requestedRoot);
         if (y < lowestY || y == lowestY && distance < bestDistance) {
            lowestY = y;
            bestDistance = distance;
            best = pos;
         }
      }

      return best;
   }

   private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
      int dx = first.getX() - second.getX();
      int dz = first.getZ() - second.getZ();
      return dx * dx + dz * dz;
   }

   private static boolean trunkFits(WorldGenLevel level, SiftWorldgenBounds bounds, List<StructureBlockInfo> blocks) {
      for (StructureBlockInfo info : blocks) {
         if (!bounds.contains(info.pos()) || !level.ensureCanWrite(info.pos()) || !canReplaceTreeBlock(level.getBlockState(info.pos()))) {
            return false;
         }
      }

      return true;
   }

   private static int placeTemplateBlocks(WorldGenLevel level, SiftWorldgenBounds bounds, List<StructureBlockInfo> blocks) {
      int placed = 0;

      for (StructureBlockInfo info : blocks) {
         if (bounds.contains(info.pos()) && level.ensureCanWrite(info.pos()) && canReplaceTreeBlock(level.getBlockState(info.pos()))) {
            level.setBlock(info.pos(), info.state(), 2);
            placed++;
         }
      }

      return placed;
   }

   private static List<StructureBlockInfo> mirrorVineFaces(List<StructureBlockInfo> vines, Mirror mirror, Rotation rotation) {
      if (mirror == Mirror.NONE) {
         return vines;
      }

      Rotation inverse = switch (rotation) {
         case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
         case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
         default -> rotation;
      };
      List<StructureBlockInfo> result = new ArrayList<>(vines.size());

      for (StructureBlockInfo info : vines) {
         result.add(new StructureBlockInfo(info.pos(), info.state().rotate(inverse).mirror(mirror).rotate(rotation), info.nbt()));
      }

      return result;
   }

   private static int placeVineTemplateBlocksTopDown(WorldGenLevel level, SiftWorldgenBounds bounds, List<StructureBlockInfo> vines) {
      List<StructureBlockInfo> topDown = new ArrayList<>(vines);
      topDown.sort((first, second) -> Integer.compare(second.pos().getY(), first.pos().getY()));
      int placed = 0;

      for (StructureBlockInfo info : topDown) {
         if (bounds.contains(info.pos()) && level.ensureCanWrite(info.pos()) && canReplaceTreeBlock(level.getBlockState(info.pos()))) {
            BlockState placedState = info.state();
            BlockState above = level.getBlockState(info.pos().above());
            boolean roof = OvergrownWillowVinesBlock.acceptsSupport(level, info.pos().above(), Direction.DOWN);
            boolean hanging = isWillowVine(above);

            for (Direction direction : ROOT_DIRECTIONS) {
               BooleanProperty face = VineBlock.getPropertyForFace(direction);
               boolean active = placedState.getValue(face);
               placedState = placedState.setValue(face, active);
            }

            placedState = placedState.setValue(VineBlock.UP, roof);
            level.setBlock(info.pos(), placedState, 18);
            placed++;
         }
      }

      return placed;
   }

   private static void varyHangingVines(WorldGenLevel level, SiftWorldgenBounds bounds, RandomSource random, List<StructureBlockInfo> vines) {
      for (StructureBlockInfo info : vines) {
         BlockState tipState = level.getBlockState(info.pos());
         if (isWillowVine(tipState)) {
            for (Direction direction : ROOT_DIRECTIONS) {
               BooleanProperty face = VineBlock.getPropertyForFace(direction);
               BooleanProperty bottom = OvergrownWillowVinesBlock.getBottomProperty(direction);
               tipState = level.getBlockState(info.pos());
               if (tipState.getValue(face) && tipState.getValue(bottom) && !(random.nextFloat() >= 0.34F)) {
                  BlockPos current = info.pos();
                  int extension = 1 + random.nextInt(3);

                  for (int step = 0; step < extension; step++) {
                     BlockPos below = current.below();
                     if (!bounds.contains(below) || !level.ensureCanWrite(below)) {
                        break;
                     }

                     BlockState belowState = level.getBlockState(below);
                     if (belowState.isAir()) {
                        belowState = ModBlocks.OVERGROWN_WILLOW_VINES.defaultBlockState().setValue(face, true);
                     } else {
                        if (!isWillowVine(belowState)) {
                           break;
                        }

                        belowState = belowState.setValue(face, true);
                     }

                     belowState = OvergrownWillowVinesBlock.refreshBottomFaces(belowState, level.getBlockState(below.below()));
                     level.setBlock(below, belowState, 18);
                     refreshVineAt(level, bounds, current);
                     current = below;
                  }

                  refreshVineAt(level, bounds, current);
               }
            }
         }
      }
   }

   private static void alignTemplateVinesToTree(WorldGenLevel level, SiftWorldgenBounds bounds, List<StructureBlockInfo> vines) {
      List<StructureBlockInfo> topDown = new ArrayList<>(vines);
      topDown.sort((first, second) -> Integer.compare(second.pos().getY(), first.pos().getY()));

      for (StructureBlockInfo info : topDown) {
         BlockPos pos = info.pos();
         if (bounds.contains(pos)) {
            BlockState state = level.getBlockState(pos);
            if (isWillowVine(state)) {
               BlockState above = level.getBlockState(pos.above());
               boolean upSupport = OvergrownWillowVinesBlock.acceptsSupport(level, pos.above(), Direction.DOWN);
               boolean hasWallFace = false;

               for (Direction direction : ROOT_DIRECTIONS) {
                  hasWallFace |= state.getValue(VineBlock.getPropertyForFace(direction));
               }

               state = state.setValue(VineBlock.UP, upSupport);
               if (!hasWallFace && !upSupport) {
                  level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
               } else {
                  level.setBlock(pos, state, 18);
               }
            }
         }
      }

      for (StructureBlockInfo info : topDown) {
         refreshVineAt(level, bounds, info.pos());
      }
   }

   private static void refreshVineAt(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos pos) {
      if (bounds.contains(pos) && bounds.contains(pos.below())) {
         BlockState state = level.getBlockState(pos);
         if (isWillowVine(state)) {
            level.setBlock(pos, OvergrownWillowVinesBlock.refreshBottomFaces(state, level.getBlockState(pos.below())), 18);
         }
      }
   }

   private static boolean isWillowVine(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_WILLOW_VINES) || state.is(ModBlocks.OVERGROWN_WILLOW_VINES_PLANT);
   }

   private static boolean hasNearbyTrunk(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos root, int radius) {
      MutableBlockPos cursor = new MutableBlockPos();
      OvergrownWillowTreeFeature.TrunkSectionCache sectionCache = new OvergrownWillowTreeFeature.TrunkSectionCache(level, bounds);

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            for (int dy = -1; dy <= 10; dy++) {
               cursor.set(root.getX() + dx, root.getY() + dy, root.getZ() + dz);
               if (bounds.contains(cursor) && sectionCache.mayContainTrunk(cursor)) {
                  BlockState state = level.getBlockState(cursor);
                  if (isWillowTrunk(state)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean isWillowTrunk(BlockState state) {
      return state.is(ModBlocks.OVERGROWN_WILLOW_LOG) || state.is(ModBlocks.OVERGROWN_WILLOW_WOOD);
   }

   private static boolean allInside(SiftWorldgenBounds bounds, List<StructureBlockInfo> blocks) {
      for (StructureBlockInfo info : blocks) {
         if (!bounds.contains(info.pos())) {
            return false;
         }
      }

      return true;
   }

   private static boolean isTreeGround(BlockState state) {
      return state.is(BlockTags.SUPPORTS_VEGETATION)
         || state.is(ModBlocks.SIFTSLATE_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   private static boolean canReplaceTreeBlock(BlockState state) {
      return state.isAir()
         || state.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE)
         || state.is(ModBlocks.OVERGROWN_WILLOW_VINES)
         || state.is(ModBlocks.OVERGROWN_WILLOW_VINES_PLANT)
         || state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS);
   }

   private static OvergrownWillowTreeFeature.TemplateChoice template(String path, int rootX, int rootZ) {
      return new OvergrownWillowTreeFeature.TemplateChoice(Identifier.fromNamespaceAndPath("the_sift", path), new BlockPos(rootX, 0, rootZ));
   }

   private record SaplingSquare(BlockPos corner, int size) {
   }

   private record TemplateChoice(Identifier id, BlockPos root) {
   }

   private static final class TrunkSectionCache {
      private final WorldGenLevel level;
      private final SiftWorldgenBounds bounds;
      private long[] keys = new long[8];
      private boolean[] values = new boolean[8];
      private int size;

      private TrunkSectionCache(WorldGenLevel level, SiftWorldgenBounds bounds) {
         this.level = level;
         this.bounds = bounds;
      }

      private boolean mayContainTrunk(BlockPos pos) {
         if (this.bounds.contains(pos) && this.level.isInsideBuildHeight(pos)) {
            long key = SectionPos.asLong(pos);

            for (int index = 0; index < this.size; index++) {
               if (this.keys[index] == key) {
                  return this.values[index];
               }
            }

            int sectionX = SectionPos.x(key);
            int sectionY = SectionPos.y(key);
            int sectionZ = SectionPos.z(key);
            ChunkAccess chunk = this.level.getChunk(sectionX, sectionZ);
            int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
            boolean result = sectionIndex >= 0 && sectionIndex < chunk.getSections().length && sectionMayContainTrunk(chunk.getSection(sectionIndex));
            this.remember(key, result);
            return result;
         } else {
            return false;
         }
      }

      private static boolean sectionMayContainTrunk(LevelChunkSection section) {
         return !section.hasOnlyAir() && section.maybeHas(OvergrownWillowTreeFeature::isWillowTrunk);
      }

      private void remember(long key, boolean value) {
         if (this.size == this.keys.length) {
            this.keys = Arrays.copyOf(this.keys, this.size * 2);
            this.values = Arrays.copyOf(this.values, this.size * 2);
         }

         this.keys[this.size] = key;
         this.values[this.size] = value;
         this.size++;
      }
   }
}
