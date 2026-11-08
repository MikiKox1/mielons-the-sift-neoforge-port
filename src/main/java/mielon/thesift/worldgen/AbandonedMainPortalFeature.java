package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.SiftPortalVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class AbandonedMainPortalFeature extends Feature<NoneFeatureConfiguration> {
   private static final int MAX_RELIEF = 18;
   private static final int FOUNDATION_BLEND_RADIUS = 12;
   private static final int MAX_EARTHWORK = 8;

   public AbandonedMainPortalFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
      WorldGenLevel level = context.level();
      RandomSource random = context.random();
      BlockPos origin = context.origin();
      if (!SiftLandmarkPlacement.isSelectedChunk(level.getSeed(), origin, 20, 4702392765337521733L, 2)) {
         return false;
      }

      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
      int centerX = (origin.getX() & -16) + 8;
      int centerZ = (origin.getZ() & -16) + 8;
      int surfaceY = level.getHeight(Types.WORLD_SURFACE_WG, centerX, centerZ) - 1;
      BlockPos center = new BlockPos(centerX, surfaceY, centerZ);
      if (surfaceY > level.getMinY() + 4 && !SiftFeaturePlacementGuard.intersectsPortal(center, 28)) {
         Rotation[] rotations = Rotation.values();
         int firstRotation = random.nextInt(rotations.length);
         Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.LEFT_RIGHT;
         AbandonedMainPortalFeature.PortalPlan plan = null;
         String selectedBiome = "sift_wastes";
         int selectedCenterX = centerX;
         int selectedCenterZ = centerZ;

         for (int[] offset : new int[][]{{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
            int candidateX = centerX + offset[0];
            int candidateZ = centerZ + offset[1];
            List<Integer> surfaces = SiftLandmarkTerrain.exposedSurfaces(level, candidateX, candidateZ);
            int firstSurface = surfaces.isEmpty() ? 0 : random.nextInt(surfaces.size());

            for (int surfaceIndex = 0; surfaceIndex < surfaces.size(); surfaceIndex++) {
               int referenceY = surfaces.get((firstSurface + surfaceIndex) % surfaces.size());
               Identifier templateId = chooseTemplate(level, new BlockPos(candidateX, referenceY, candidateZ), random);
               Optional<StructureTemplate> optional = level.getLevel().getStructureManager().get(templateId);
               if (!optional.isEmpty()) {
                  StructureTemplate template = optional.get();

                  for (int attempt = 0; attempt < rotations.length; attempt++) {
                     StructurePlaceSettings settings = new StructurePlaceSettings()
                        .setMirror(mirror)
                        .setRotation(rotations[(firstRotation + attempt) % rotations.length])
                        .setIgnoreEntities(true)
                        .setKnownShape(false)
                        .setRandom(random)
                        .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
                     AbandonedMainPortalFeature.PortalPlan candidate = planPlacement(level, bounds, template, settings, candidateX, candidateZ, referenceY);
                     if (candidate != null && (plan == null || candidate.relief() < plan.relief())) {
                        plan = candidate;
                        selectedBiome = SiftPortalVariants.abandonedTemplateBiome(templateId.getPath());
                        selectedCenterX = candidateX;
                        selectedCenterZ = candidateZ;
                     }
                  }

                  if (plan != null) {
                     break;
                  }
               }
            }

            if (plan != null) {
               break;
            }
         }

         if (SiftCampProtection.near(level, selectedCenterX, selectedCenterZ, 28)) {
            return false;
         }

         if (SiftCampProtection.near(level, selectedCenterX, selectedCenterZ, 28)) {
            return false;
         }

         if (plan != null && SiftFeaturePlacementGuard.tryReserveSolid(selectedCenterX - 23, selectedCenterZ - 23, selectedCenterX + 22, selectedCenterZ + 22)) {
            List<StructureBlockInfo> naturalBlocks = plan.naturalBlocks();
            int baseY = naturalBlocks.getFirst().pos().getY();
            List<BlockPos> vegetationFloors = applyFoundation(level, plan.terrain());
            vegetationFloors.forEach(SiftPortalVegetation::register);
            if (!plan.template().placeInWorld(level, plan.origin(), plan.origin(), plan.settings(), random, 2)) {
               return false;
            }

            for (StructureBlockInfo info : naturalBlocks) {
               SiftPortalVegetation.register(info.pos());
               SiftTerrainFeatureMemory.protectTerrain(info.pos().getX(), info.pos().getZ(), info.pos().getY(), info.pos().getY());
            }

            SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.ABANDONED_MAIN_PORTAL, new BlockPos(selectedCenterX, baseY, selectedCenterZ));
            SiftLandmarkTracker.record(SiftPortalVariants.abandonedKind(selectedBiome), new BlockPos(selectedCenterX, baseY, selectedCenterZ));
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Identifier chooseTemplate(WorldGenLevel level, BlockPos center, RandomSource random) {
      String biomePath = SiftCaveHabitat.biome(level, center).unwrapKey().map(key -> key.identifier().getPath()).orElse("sift_wastes");
      return SiftPortalVariants.abandoned(biomePath, random.nextInt(SiftPortalVariants.abandonedCount(biomePath)));
   }

   private static AbandonedMainPortalFeature.PortalPlan planPlacement(
      WorldGenLevel level, SiftWorldgenBounds bounds, StructureTemplate template, StructurePlaceSettings settings, int centerX, int centerZ, int referenceY
   ) {
      List<StructureBlockInfo> relative = naturalBlocks(template, BlockPos.ZERO, settings);
      if (relative.isEmpty()) {
         return null;
      }

      int minX = relative.stream().mapToInt(info -> info.pos().getX()).min().orElseThrow();
      int maxX = relative.stream().mapToInt(info -> info.pos().getX()).max().orElseThrow();
      int minZ = relative.stream().mapToInt(info -> info.pos().getZ()).min().orElseThrow();
      int maxZ = relative.stream().mapToInt(info -> info.pos().getZ()).max().orElseThrow();
      int baseY = relative.getFirst().pos().getY();
      int originX = centerX - Math.floorDiv(minX + maxX + 1, 2);
      int originZ = centerZ - Math.floorDiv(minZ + maxZ + 1, 2);
      List<Integer> heights = new ArrayList<>();

      for (int x = originX + minX; x <= originX + maxX; x++) {
         for (int z = originZ + minZ; z <= originZ + maxZ; z++) {
            if (!bounds.contains(x, z)) {
               return null;
            }

            int height = SiftLandmarkTerrain.surfaceNearY(level, x, z, referenceY, 8);
            if (height <= level.getMinY() + 4) {
               return null;
            }

            heights.add(height);
         }
      }

      heights.sort(Integer::compareTo);
      int lowest = heights.getFirst();
      int relief = heights.get(heights.size() - 1) - lowest;
      if (relief > 18) {
         return null;
      }

      int groundY = heights.get(heights.size() / 2) + 1;
      BlockPos origin = new BlockPos(originX, groundY - baseY, originZ);
      List<StructureBlockInfo> natural = naturalBlocks(template, origin, settings);
      List<StructureBlockInfo> all = new ArrayList<>(natural);
      all.addAll(template.filterBlocks(origin, settings, Blocks.CHEST));
      if (!allBlocksWritable(level, bounds, all)) {
         return null;
      }

      List<AbandonedMainPortalFeature.TerrainColumn> terrain = planFoundation(
         level, bounds, originX + minX, originX + maxX, originZ + minZ, originZ + maxZ, groundY - 1, level.getSeed() ^ origin.asLong()
      );
      return terrain == null ? null : new AbandonedMainPortalFeature.PortalPlan(template, origin, settings, natural, terrain, relief);
   }

   private static List<StructureBlockInfo> naturalBlocks(StructureTemplate template, BlockPos origin, StructurePlaceSettings settings) {
      List<StructureBlockInfo> blocks = new ArrayList<>();
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.SIFTSLATE));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.SIFTSLATE_GROWTH));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.HEALTHY_SCULK));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.DRY_HEALTHY_SCULK));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.DRY_HEALTHY_SCULK_GROWTH));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.SOULSHALE));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.SOULSHALE_GROWTH));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.HEALTHY_SCULK_CARTILAGE));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.HEALTHY_SCULK_PORES));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.CORAL_SOIL));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.CORAL_GROWTH));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.BRIGHT_CORAL_GROWTH));
      blocks.addAll(template.filterBlocks(origin, settings, Blocks.SCULK));
      blocks.addAll(template.filterBlocks(origin, settings, ModBlocks.ICHOR_SNOW_BLOCK));
      blocks.sort(Comparator.comparingInt(info -> info.pos().getY()));
      return blocks;
   }

   private static boolean allBlocksWritable(WorldGenLevel level, SiftWorldgenBounds bounds, List<StructureBlockInfo> blocks) {
      for (StructureBlockInfo info : blocks) {
         if (!bounds.contains(info.pos()) || !level.ensureCanWrite(info.pos()) || !SiftLandmarkTerrain.canReplace(level.getBlockState(info.pos()))) {
            return false;
         }
      }

      return true;
   }

   private static List<AbandonedMainPortalFeature.TerrainColumn> planFoundation(
      WorldGenLevel level, SiftWorldgenBounds bounds, int minX, int maxX, int minZ, int maxZ, int floorY, long noiseSalt
   ) {
      List<AbandonedMainPortalFeature.TerrainColumn> terrain = new ArrayList<>();
      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = minX - 12; x <= maxX + 12; x++) {
         for (int z = minZ - 12; z <= maxZ + 12; z++) {
            if (bounds.contains(x, z)) {
               int dx = Math.max(0, Math.max(minX - x, x - maxX));
               int dz = Math.max(0, Math.max(minZ - z, z - maxZ));
               double distance = Math.sqrt(dx * dx + dz * dz);
               double edge = 12.0 + blendNoise(x, z, noiseSalt) * 1.35;
               if (!(distance > edge)) {
                  boolean core = dx == 0 && dz == 0;
                  int naturalTop = SiftLandmarkTerrain.surfaceNearY(level, x, z, floorY, 8);
                  if (naturalTop == Integer.MIN_VALUE) {
                     if (core) {
                        return null;
                     }
                  } else {
                     if (core
                        && (
                           !SiftLandmarkTerrain.isGround(level.getBlockState(new BlockPos(x, Math.min(floorY, naturalTop) - 3, z)))
                              || Math.abs(floorY - naturalTop) > 8
                        )) {
                        return null;
                     }

                     double t = Math.max(0.0, 1.0 - distance / edge);
                     double weight = t * t * (3.0 - 2.0 * t);
                     if (!core) {
                        weight *= Math.min(1.0, bounds.distanceToEdge(x, z) / 3.0);
                     }

                     int adjustment = Math.max(-8, Math.min(8, floorY - naturalTop));
                     int targetTop = core ? floorY : naturalTop + (int)Math.round(adjustment * weight);
                     if (core || naturalTop != targetTop) {
                        cursor.set(x, naturalTop, z);
                        BlockState originalTop = level.getBlockState(cursor);
                        int startY = Math.min(naturalTop, targetTop) - 2;
                        int endY = Math.max(naturalTop, targetTop) + 3;

                        for (int y = startY; y <= endY; y++) {
                           cursor.set(x, y, z);
                           if (!level.ensureCanWrite(cursor) || !SiftLandmarkTerrain.canReplace(level.getBlockState(cursor))) {
                              return null;
                           }
                        }

                        BlockState surface = originalTop.is(ModBlocks.SIFT_RUBBLE) ? ModBlocks.SIFTSLATE.defaultBlockState() : originalTop;
                        terrain.add(new AbandonedMainPortalFeature.TerrainColumn(x, z, naturalTop, targetTop, surface));
                     }
                  }
               }
            }
         }
      }

      return terrain;
   }

   private static List<BlockPos> applyFoundation(WorldGenLevel level, List<AbandonedMainPortalFeature.TerrainColumn> terrain) {
      List<BlockPos> surfaces = new ArrayList<>();
      MutableBlockPos cursor = new MutableBlockPos();

      for (AbandonedMainPortalFeature.TerrainColumn column : terrain) {
         int floorY = column.targetTop();

         for (int y = Math.min(column.naturalTop() + 1, floorY - 2); y <= floorY; y++) {
            cursor.set(column.x(), y, column.z());
            BlockState current = level.getBlockState(cursor);
            if (y > column.naturalTop() || !SiftLandmarkTerrain.isGround(current) || current.is(ModBlocks.SIFT_RUBBLE)) {
               level.setBlock(cursor, SiftLandmarkTerrain.stoneForBiome(SiftCaveHabitat.biome(level, cursor)).defaultBlockState(), 2);
            }
         }

         for (int y = floorY + 1; y <= Math.max(column.naturalTop(), floorY) + 3; y++) {
            cursor.set(column.x(), y, column.z());
            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
         }

         cursor.set(column.x(), floorY, column.z());
         level.setBlock(cursor, column.surface(), 2);
         surfaces.add(cursor.immutable());
      }

      return surfaces;
   }

   private static double blendNoise(int x, int z, long salt) {
      double phase = (salt & 65535L) * 1.91E-4;
      double broad = Math.sin(x * 0.087 + z * 0.039 + phase);
      double cross = Math.cos(x * 0.043 - z * 0.074 - phase * 0.71);
      double detail = Math.sin((x + z) * 0.023 + phase * 1.83);
      return broad * 0.5 + cross * 0.33 + detail * 0.17;
   }

   private record PortalPlan(
      StructureTemplate template,
      BlockPos origin,
      StructurePlaceSettings settings,
      List<StructureBlockInfo> naturalBlocks,
      List<AbandonedMainPortalFeature.TerrainColumn> terrain,
      int relief
   ) {
   }

   private record TerrainColumn(int x, int z, int naturalTop, int targetTop, BlockState surface) {
   }
}
