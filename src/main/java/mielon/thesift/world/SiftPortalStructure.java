package mielon.thesift.world;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

import mielon.thesift.worldgen.CoralSurfaceFeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mielon.thesift.TheSiftMod;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import mielon.thesift.block.SiftslateGrowthBlock;
import mielon.thesift.worldgen.SiftFeaturePlacementGuard;
import mielon.thesift.worldgen.SiftLandmarkTerrain;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import mielon.thesift.worldgen.SiftPortalVegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

public final class SiftPortalStructure {
   private static final Identifier TEMPLATE_ID = Identifier.fromNamespaceAndPath("the_sift", "main_portal");
   private static final int SIZE_X = 9;
   private static final int SIZE_Y = 19;
   private static final int SIZE_Z = 40;
   private static final BlockPos MARKER_RELATIVE = new BlockPos(3, 4, 19);
   private static final BlockPos PLAYER_RELATIVE = new BlockPos(3, 5, 19);
   private static final int STRUCTURE_GROUND_RELATIVE_Y = MARKER_RELATIVE.getY() - 4;
   private static final int[][] GROUND_PROFILE = new int[][]{
      {-1, -1, -1, -1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1},
      {-1, -1, -1, -1, 1, 7, 14, 1, 6, 4, 3, 3, 3, 2, -1, -1, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 4, -1, 1, 12, 16, 14, 2, -1, -1, -1, -1},
      {-1, -1, -1, -1, 0, 17, 17, 12, 18, 6, 5, 4, 3, 3, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 6, 7, 17, 17, 17, 14, 14, -1, -1, -1, -1},
      {-1, -1, -1, -1, 0, 17, 18, 18, 18, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 16, 17, 17, 15, 14, -1, -1, -1, -1},
      {-1, -1, -1, -1, 0, 17, 17, 18, 18, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 5, 5, 6, 16, 17, 17, 15, 15, -1, -1, -1, -1},
      {-1, -1, -1, -1, 0, 17, 17, 2, 6, 4, 3, 4, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 2, 3, 3, 2, 4, 6, 17, 16, 15, 15, -1, -1, -1, -1},
      {-1, -1, -1, -1, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 1, 0, 2, 2, 2, 3, 3, 5, 5, 5, 4, -1, -1, -1, -1},
      {-1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, -1, 1, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1},
      {
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            0,
            0,
            0,
            0,
            0,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1
      }
   };
   private static final int[][] CLIFF_PROFILE = new int[][]{
      {-1, -1, -1, -1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1},
      {
            -1,
            -1,
            -1,
            -1,
            15,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            16,
            16,
            16,
            17,
            16,
            16,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            16,
            16,
            17,
            17,
            17,
            17,
            17,
            16,
            14,
            14,
            -1,
            -1,
            -1,
            -1
      },
      {
            9,
            10,
            10,
            9,
            16,
            17,
            17,
            17,
            18,
            18,
            17,
            17,
            17,
            16,
            16,
            16,
            17,
            16,
            16,
            17,
            17,
            17,
            18,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            16,
            17,
            17,
            17,
            14,
            14,
            12,
            12,
            12,
            11
      },
      {
            10,
            10,
            10,
            10,
            17,
            17,
            18,
            18,
            18,
            18,
            18,
            17,
            17,
            16,
            17,
            16,
            17,
            17,
            16,
            17,
            17,
            18,
            18,
            18,
            18,
            18,
            17,
            17,
            17,
            16,
            16,
            16,
            17,
            17,
            15,
            14,
            12,
            12,
            12,
            12
      },
      {
            10,
            10,
            10,
            10,
            17,
            17,
            17,
            18,
            18,
            18,
            17,
            17,
            17,
            16,
            16,
            16,
            17,
            17,
            17,
            17,
            17,
            18,
            17,
            17,
            18,
            18,
            17,
            17,
            17,
            17,
            17,
            16,
            17,
            17,
            15,
            15,
            12,
            12,
            12,
            12
      },
      {
            -1,
            -1,
            -1,
            -1,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            16,
            16,
            16,
            17,
            17,
            16,
            16,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            17,
            16,
            15,
            15,
            3,
            -1,
            -1,
            -1
      },
      {-1, -1, -1, -1, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 1, 0, 2, 2, 2, 3, 3, 5, 5, 5, 4, -1, -1, -1, -1},
      {-1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, -1, 1, 2, 2, -1, -1, -1, -1, -1, -1, -1, -1},
      {
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            0,
            0,
            0,
            0,
            0,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1
      }
   };
   private static final SiftPortalStructure.ProfilePoint[] GROUND_POINTS = collectProfilePoints(GROUND_PROFILE);
   private static final SiftPortalStructure.ProfilePoint[] CLIFF_POINTS = collectProfilePoints(CLIFF_PROFILE);
   private static final int BLEND_RADIUS = 52;
   private static final int FOUNDATION_BLEND_RADIUS = 15;
   private static final int SEARCH_RADIUS = 96;
   private static final int COARSE_STEP = 16;
   private static final int REFINE_RADIUS = 16;
   private static final int REFINE_STEP = 4;
   private static final int OUTER_SAMPLE_MARGIN = 12;
   private static final int FEATURE_CLEAR_MARGIN = 15;
   private static final int PORTAL_BUILD_FLAGS = 48;
   private static final ThreadLocal<Map<Long, Integer>> HEIGHT_CACHE = new ThreadLocal<>();

   public static void prepare(MinecraftServer server) {
      ServerLevel level = server.getLevel(TheSiftDimension.LEVEL_KEY);
      if (level != null) {
         ensurePortal(server, level);
      }
   }

   public static void restoreReservation(MinecraftServer server) {
      SiftWorldStorage.getPortalAnchor(server)
         .ifPresent(
            anchor -> SiftFeaturePlacementGuard.reservePortal(anchor.offset(-PLAYER_RELATIVE.getX(), -PLAYER_RELATIVE.getY(), -PLAYER_RELATIVE.getZ()), 9, 40)
         );
   }

   private SiftPortalStructure() {
   }

   public static Optional<BlockPos> ensurePortal(MinecraftServer server, ServerLevel sift) {
      Optional<BlockPos> stored = SiftWorldStorage.getPortalAnchor(server);
      if (stored.isPresent()) {
         BlockPos storedOrigin = stored.get().offset(-PLAYER_RELATIVE.getX(), -PLAYER_RELATIVE.getY(), -PLAYER_RELATIVE.getZ());
         SiftFeaturePlacementGuard.reservePortal(storedOrigin, 9, 40);
         restorePortalVegetation(sift, storedOrigin, stored.get());
         if (!SiftWorldStorage.hasBlendedPortalCoral(server) && sift.getBlockState(stored.get()).is(ModBlocks.SIFT_PORTAL)) {
            blendPortalCoral(sift, storedOrigin);
            SiftWorldStorage.markPortalCoralBlended(server);
         }
         return stored;
      }

      TheSiftMod.LOGGER.info("Selecting a location for the main Sift portal.");
      HEIGHT_CACHE.set(new HashMap<>());

      SiftPortalStructure.PlacementCandidate candidate;
      try {
         candidate = findBestCandidate(sift);
      } finally {
         HEIGHT_CACHE.remove();
      }

      if (candidate == null) {
         TheSiftMod.LOGGER.error("No main portal location outside Corrupted Sift was found.");
         return Optional.empty();
      } else {
         BlockPos candidatePos = new BlockPos(candidate.anchorX(), candidate.groundY(), candidate.anchorZ());
         String biomePath = sift.getBiome(candidatePos).unwrapKey().map(key -> key.identifier().getPath()).orElse("sift_wastes");
         Identifier templateId = SiftPortalVariants.main(biomePath);
         Optional<StructureTemplate> templateOptional = server.getStructureTemplateManager().get(templateId);
         if (templateOptional.isEmpty()) {
            TheSiftMod.LOGGER.error("Missing portal structure template: {}", templateId);
            return Optional.empty();
         } else {
            StructureTemplate template = templateOptional.get();
            int naturalGroundY = candidate.groundY();
            int originY = naturalGroundY - STRUCTURE_GROUND_RELATIVE_Y;
            originY = Math.max(0, Math.min(237, originY));
            BlockPos origin = new BlockPos(candidate.anchorX() - MARKER_RELATIVE.getX(), originY, candidate.anchorZ() - MARKER_RELATIVE.getZ());
            SiftFeaturePlacementGuard.reservePortal(origin, 9, 40);
            int markerY = originY + MARKER_RELATIVE.getY();
            TheSiftMod.LOGGER
               .debug(
                  "Main portal candidate: anchor=({}, {}, {}), markerY={}, origin={}, roughness={}",
                  candidate.anchorX(),
                  naturalGroundY,
                  candidate.anchorZ(),
                  markerY,
                  origin,
                  candidate.roughness()
               );
            clearLargeFeaturesAroundPortal(sift, origin);
            blendTerrainIntoPortal(sift, origin);
            sealPortalFoundation(sift, origin);
            StructurePlaceSettings settings = new StructurePlaceSettings()
               .setMirror(Mirror.NONE)
               .setRotation(Rotation.NONE)
               .setIgnoreEntities(true)
               .setKnownShape(true)
               .setRandom(RandomSource.create(sift.getSeed() ^ 1397311060L));
            clearPortalEntities(sift, origin);
            boolean placed = template.placeInWorld(sift, origin, origin, settings, RandomSource.create(sift.getSeed() ^ 88301613891916L), 48);
            if (!placed) {
               TheSiftMod.LOGGER.error("Failed to place the main Sift portal at {}", origin);
               return Optional.empty();
            } else {
               BlockPos marker = origin.offset(MARKER_RELATIVE.getX(), MARKER_RELATIVE.getY(), MARKER_RELATIVE.getZ());
               Block reinforced = SiftLandmarkTerrain.stoneForBiome(sift.getBiome(candidatePos)) == ModBlocks.SOULSHALE
                  ? ModBlocks.REINFORCED_SOULSHALE
                  : ModBlocks.REINFORCED_SIFTSLATE;
               setDuringPortalBuild(sift, marker, reinforced.defaultBlockState());
               BlockPos playerAnchor = origin.offset(PLAYER_RELATIVE.getX(), PLAYER_RELATIVE.getY(), PLAYER_RELATIVE.getZ());
               normalizeCoveredGrowth(sift, origin);
               restorePortalVegetation(sift, origin, playerAnchor);
               clearPortalEntities(sift, origin);
               blendPortalCoral(sift, origin);
               SiftWorldStorage.setPortalAnchor(server, playerAnchor);
               SiftWorldStorage.markPortalCoralBlended(server);
               TheSiftMod.LOGGER.info("Main Sift portal ready at {}", playerAnchor);
               return Optional.of(playerAnchor);
            }
         }
      }
   }

   public static void repairMainPortalAxis(ServerLevel level, BlockPos pos, BlockState state) {
      if (state.is(ModBlocks.SIFT_PORTAL) && state.getValue(SiftPortalBlock.AXIS) != Axis.Z && level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         SiftWorldStorage.getPortalAnchor(level.getServer())
            .ifPresent(
               anchor -> {
                  if (pos.getX() == anchor.getX()
                     && pos.getY() >= anchor.getY()
                     && pos.getY() <= anchor.getY() + 5
                     && pos.getZ() >= anchor.getZ() - 9
                     && pos.getZ() <= anchor.getZ() + 10) {
                     level.setBlock(pos, state.setValue(SiftPortalBlock.AXIS, Axis.Z), 18);
                  }
               }
            );
      }
   }

   private static void normalizeCoveredGrowth(ServerLevel level, BlockPos origin) {
      MutableBlockPos cursor = new MutableBlockPos();
      MutableBlockPos above = new MutableBlockPos();

      for (int x = 0; x < 9; x++) {
         for (int z = 0; z < 40; z++) {
            for (int y = 0; y < 18; y++) {
               cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
               if (level.getBlockState(cursor).is(ModBlocks.SIFTSLATE_GROWTH)) {
                  above.set(cursor.getX(), cursor.getY() + 1, cursor.getZ());
                  BlockState growth = level.getBlockState(cursor);
                  BlockState cover = level.getBlockState(above);
                  boolean snowy = SiftslateGrowthBlock.isIchorSnowCover(cover);
                  if (!snowy && cover.isCollisionShapeFullBlock(level, above)) {
                     setDuringPortalBuild(level, cursor, SiftLandmarkTerrain.stoneForBiome(level.getBiome(cursor)).defaultBlockState());
                  } else if (growth.getValue(SiftslateGrowthBlock.SNOWY) != snowy) {
                     setDuringPortalBuild(level, cursor, growth.setValue(SiftslateGrowthBlock.SNOWY, snowy));
                  }
               }
            }
         }
      }
   }

   private static void clearLargeFeaturesAroundPortal(ServerLevel level, BlockPos origin) {
      int minX = origin.getX() - 15;
      int maxX = origin.getX() + 9 - 1 + 15;
      int minZ = origin.getZ() - 15;
      int maxZ = origin.getZ() + 40 - 1 + 15;
      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            if (isPortalBaseColumn(x - origin.getX(), z - origin.getZ())) {
               int generatedTop = generatedSurfaceBlockY(level, x, z);
               int actualTop = level.getHeight(Types.WORLD_SURFACE, x, z) - 1;

               for (int y = actualTop; y > generatedTop; y--) {
                  cursor.set(x, y, z);
                  BlockState state = level.getBlockState(cursor);
                  if (!state.isAir()) {
                     if (!isSiftTerrain(state) && !isSiftPlant(state)) {
                        break;
                     }

                     setDuringPortalBuild(level, cursor, Blocks.AIR.defaultBlockState());
                  }
               }
            }
         }
      }
   }

   private static SiftPortalStructure.PlacementCandidate findBestCandidate(ServerLevel level) {
      SiftPortalStructure.PlacementCandidate coarse = searchArea(level, -96, 96, -96, 96, 16);

      for (int radius = 192; coarse == null && radius <= 6144; radius *= 2) {
         coarse = searchArea(level, -radius, radius, -radius, radius, 64);
      }

      if (coarse == null) {
         return null;
      }

      SiftPortalStructure.PlacementCandidate refined = searchArea(
         level, coarse.anchorX() - 16, coarse.anchorX() + 16, coarse.anchorZ() - 16, coarse.anchorZ() + 16, 4
      );
      return refined != null ? refined : coarse;
   }

   private static SiftPortalStructure.PlacementCandidate searchArea(ServerLevel level, int minSearchX, int maxSearchX, int minSearchZ, int maxSearchZ, int step) {
      SiftPortalStructure.PlacementCandidate best = null;
      double bestScore = Double.POSITIVE_INFINITY;
      int x = minSearchX;

      while (x <= maxSearchX) {
         for (int z = minSearchZ; z <= maxSearchZ; z += step) {
            int centerY = generatedSurfaceBlockY(level, x, z);
            ServerChunkCache source = level.getChunkSource();
            if (!source.getGenerator()
               .getBiomeSource()
               .createUncachedResolver(source.randomState())
               .getNoiseBiome(x >> 2, centerY >> 2, z >> 2)
               .is(TheSiftDimension.CORRUPTED_SIFT)) {
               SiftPortalStructure.TerrainSample sample = sampleFootprintWithoutLoadingChunks(level, x, z);
               if (isPortalSiteAllowed(level, x, sample.medianHeight(), z)) {
                  SiftPortalStructure.PlacementCandidate candidate = new SiftPortalStructure.PlacementCandidate(x, z, sample.medianHeight(), sample.roughness());
                  double candidateScore = score(candidate, x, z) + sample.range() * 135.0 + sample.outerRange() * 75.0;
                  if (sample.medianHeight() < 12 || sample.medianHeight() > 232) {
                     candidateScore += 10000.0;
                  }

                  if (candidateScore < bestScore) {
                     bestScore = candidateScore;
                     best = candidate;
                  }
               }
            }
         }

         x += step;
      }

      return best;
   }

   private static boolean isPortalSiteAllowed(ServerLevel level, int anchorX, int groundY, int anchorZ) {
      ServerChunkCache chunkSource = level.getChunkSource();
      BiomeSource source = chunkSource.getGenerator().getBiomeSource();
      BiomeResolver resolver = source.createUncachedResolver(chunkSource.randomState());
      int minX = anchorX - MARKER_RELATIVE.getX() - 2 >> 2;
      int minZ = anchorZ - MARKER_RELATIVE.getZ() - 2 >> 2;
      int maxX = (anchorX - MARKER_RELATIVE.getX() + 9 - 3 >> 2) + 1;
      int maxZ = (anchorZ - MARKER_RELATIVE.getZ() + 40 - 3 >> 2) + 1;
      int minY = groundY - 2 >> 2;
      int maxY = (groundY + 19 - 3 >> 2) + 1;

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
               if (resolver.getNoiseBiome(x, y, z).is(TheSiftDimension.CORRUPTED_SIFT)) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private static void clearPortalEntities(ServerLevel level, BlockPos origin) {
      AABB structure = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + 9, origin.getY() + 19, origin.getZ() + 40);
      AABB affected = new AABB(origin.getX() - 52, level.getMinY(), origin.getZ() - 52, origin.getX() + 9 + 52, level.getMaxY(), origin.getZ() + 40 + 52);

      for (Entity entity : level.getEntities((Entity)null, affected, entityx -> !(entityx instanceof Player))) {
         if (entity.getBoundingBox().intersects(structure) || level.getBlockCollisions(entity, entity.getBoundingBox()).iterator().hasNext()) {
            entity.discard();
         }
      }
   }

   private static double score(SiftPortalStructure.PlacementCandidate candidate, int x, int z) {
      double distancePenalty = Math.sqrt((double)x * x + (double)z * z) * 0.8;
      return candidate.roughness() * 18.0 + distancePenalty;
   }

   private static SiftPortalStructure.TerrainSample sampleFootprintWithoutLoadingChunks(ServerLevel level, int anchorX, int anchorZ) {
      int minX = anchorX - MARKER_RELATIVE.getX();
      int minZ = anchorZ - MARKER_RELATIVE.getZ();
      int maxX = minX + 9 - 1;
      int maxZ = minZ + 40 - 1;
      int midX = (minX + maxX) / 2;
      int midZ = (minZ + maxZ) / 2;
      int[] core = new int[]{
         generatedSurfaceBlockY(level, minX, minZ),
         generatedSurfaceBlockY(level, midX, minZ),
         generatedSurfaceBlockY(level, maxX, minZ),
         generatedSurfaceBlockY(level, minX, midZ),
         generatedSurfaceBlockY(level, midX, midZ),
         generatedSurfaceBlockY(level, maxX, midZ),
         generatedSurfaceBlockY(level, minX, maxZ),
         generatedSurfaceBlockY(level, midX, maxZ),
         generatedSurfaceBlockY(level, maxX, maxZ)
      };
      int outerMinX = minX - 12;
      int outerMaxX = maxX + 12;
      int outerMinZ = minZ - 12;
      int outerMaxZ = maxZ + 12;
      int outerMidX = (outerMinX + outerMaxX) / 2;
      int outerMidZ = (outerMinZ + outerMaxZ) / 2;
      int[] outer = new int[]{
         generatedSurfaceBlockY(level, outerMinX, outerMinZ),
         generatedSurfaceBlockY(level, outerMidX, outerMinZ),
         generatedSurfaceBlockY(level, outerMaxX, outerMinZ),
         generatedSurfaceBlockY(level, outerMinX, outerMidZ),
         generatedSurfaceBlockY(level, outerMaxX, outerMidZ),
         generatedSurfaceBlockY(level, outerMinX, outerMaxZ),
         generatedSurfaceBlockY(level, outerMidX, outerMaxZ),
         generatedSurfaceBlockY(level, outerMaxX, outerMaxZ)
      };
      int[] sorted = (int[])core.clone();
      Arrays.sort(sorted);
      int median = sorted[sorted.length / 2];
      int coreMin = Arrays.stream(core).min().orElse(median);
      int coreMax = Arrays.stream(core).max().orElse(median);
      int outerMin = Arrays.stream(outer).min().orElse(median);
      int outerMax = Arrays.stream(outer).max().orElse(median);
      double roughness = 0.0;

      for (int height : core) {
         roughness += Math.abs(height - median);
      }

      for (int height : outer) {
         roughness += Math.abs(height - median) * 0.45;
      }

      roughness /= core.length + outer.length * 0.45;
      return new SiftPortalStructure.TerrainSample(median, coreMax - coreMin, Math.max(coreMax, outerMax) - Math.min(coreMin, outerMin), roughness);
   }

   private static void blendTerrainIntoPortal(ServerLevel level, BlockPos origin) {
      int structureMinX = origin.getX();
      int structureMaxX = origin.getX() + 9 - 1;
      int structureMinZ = origin.getZ();
      int structureMaxZ = origin.getZ() + 40 - 1;
      int minX = structureMinX - 52;
      int maxX = structureMaxX + 52;
      int minZ = structureMinZ - 52;
      int maxZ = structureMaxZ + 52;
      long seed = level.getSeed();
      buildNaturalPortalFoundation(level, origin, seed);

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            int localX = x - structureMinX;
            int localZ = z - structureMinZ;
            boolean inside = localX >= 0 && localX < 9 && localZ >= 0 && localZ < 40;
            int naturalY = findTerrainSurfaceBlockY(level, x, z);
            if (naturalY > 1 && naturalY < 254) {
               BlockState originalTop = level.getBlockState(new BlockPos(x, naturalY, z));
               if (isSiftTerrain(originalTop)) {
                  if (inside) {
                     int exactRelativeY = GROUND_PROFILE[localX][localZ];
                     if (exactRelativeY >= 0) {
                        int desiredY = Math.max(2, Math.min(253, origin.getY() + exactRelativeY));
                        if (naturalY > desiredY) {
                           lowerTerrainColumn(level, x, z, naturalY, desiredY, originalTop, false);
                        }
                     }
                  } else {
                     double rectangleDistance = distanceOutsideRectangle(x, z, structureMinX, structureMaxX, structureMinZ, structureMaxZ);
                     if (!(rectangleDistance > 52.0)) {
                        double envelopeRelativeY = portalTerrainEnvelope(localX, localZ, seed);
                        double approach = portalApproachMask(localX, localZ);
                        if (approach > 0.0) {
                           double lowForecourt = 3.6 + terrainBlendNoise(seed ^ -3335678366873096957L, localX, localZ) * 0.55;
                           envelopeRelativeY = envelopeRelativeY * (1.0 - approach) + Math.min(envelopeRelativeY, lowForecourt) * approach;
                        }

                        if (!(envelopeRelativeY <= 0.0)) {
                           int desiredY = origin.getY() + (int)Math.floor(envelopeRelativeY);
                           desiredY = Math.max(2, Math.min(253, desiredY));
                           if (desiredY > naturalY) {
                              raiseTerrainColumn(level, x, z, naturalY, desiredY, originalTop, true);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void sealPortalFoundation(ServerLevel level, BlockPos origin) {
      int foundationTopY = Math.max(2, origin.getY() - 1);

      for (int localX = 0; localX < 9; localX++) {
         for (int localZ = 0; localZ < 40; localZ++) {
            if (isPortalBaseColumn(localX, localZ)) {
               int x = origin.getX() + localX;
               int z = origin.getZ() + localZ;
               int terrainY = findTerrainSurfaceBlockY(level, x, z);
               if (terrainY < 1) {
                  terrainY = Math.max(1, generatedSurfaceBlockY(level, x, z));
               }

               BlockState surface = level.getBlockState(new BlockPos(x, terrainY, z));
               if (terrainY < foundationTopY) {
                  raiseTerrainColumn(level, x, z, terrainY, foundationTopY, surface, false);
               }

               MutableBlockPos cursor = new MutableBlockPos();

               for (int y = foundationTopY; y > terrainY; y--) {
                  cursor.set(x, y, z);
                  BlockState state = level.getBlockState(cursor);
                  if (state.isAir() || isSiftPlant(state)) {
                     setDuringPortalBuild(level, cursor, SiftLandmarkTerrain.stoneForBiome(level.getBiome(cursor)).defaultBlockState());
                  }
               }
            }
         }
      }
   }

   private static void buildNaturalPortalFoundation(ServerLevel level, BlockPos origin, long seed) {
      int foundationTopY = Math.max(2, origin.getY() - 1);
      int minX = origin.getX() - 15;
      int maxX = origin.getX() + 9 - 1 + 15;
      int minZ = origin.getZ() - 15;
      int maxZ = origin.getZ() + 40 - 1 + 15;

      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            int localX = x - origin.getX();
            int localZ = z - origin.getZ();
            boolean core = isPortalBaseColumn(localX, localZ);
            double broadNoise = terrainBlendNoise(seed ^ 7640891576956012809L, localX * 3 + 11, localZ * 3 - 17);
            double fineNoise = terrainBlendNoise(seed ^ -4942790177534073029L, localX * 9 - 23, localZ * 9 + 31);
            double distance = core ? 0.0 : distanceToPortalBase(localX + broadNoise * 1.45, localZ - broadNoise * 1.15);
            if (!(distance > 15.0)) {
               double erosion = distance * (0.7 + fineNoise * 0.08) + distance * distance * 0.016;
               int targetY = core ? foundationTopY : foundationTopY - Math.max(1, (int)Math.floor(erosion));
               int naturalY = findTerrainSurfaceBlockY(level, x, z);
               if (naturalY > 1 && naturalY < targetY) {
                  BlockState originalTop = level.getBlockState(new BlockPos(x, naturalY, z));
                  if (isSiftTerrain(originalTop)) {
                     boolean preserveBiomeSurface = !core && distance > 2.25;
                     raiseTerrainColumn(level, x, z, naturalY, targetY, originalTop, preserveBiomeSurface);
                  }
               }
            }
         }
      }
   }

   private static boolean isPortalBaseColumn(int localX, int localZ) {
      return localX >= 0 && localX < 9 && localZ >= 0 && localZ < 40 && GROUND_PROFILE[localX][localZ] >= 0;
   }

   private static double distanceToPortalBase(double localX, double localZ) {
      double bestSquared = Double.POSITIVE_INFINITY;

      for (SiftPortalStructure.ProfilePoint point : GROUND_POINTS) {
         double dx = localX - point.x();
         double dz = localZ - point.z();
         bestSquared = Math.min(bestSquared, dx * dx + dz * dz);
      }

      return Math.sqrt(bestSquared);
   }

   private static double portalTerrainEnvelope(int localX, int localZ, long seed) {
      double broadWarp = terrainBlendNoise(seed ^ 1375183133L, localX * 3 + 17, localZ * 3 - 29);
      double fineWarp = terrainBlendNoise(seed ^ 2082071971L, localX * 7 - 41, localZ * 7 + 13);
      double warpedX = localX + broadWarp * 1.65 + fineWarp * 0.35;
      double warpedZ = localZ - broadWarp * 1.1 + fineWarp * 0.45;
      double columnNoise = terrainBlendNoise(seed ^ -6752110988234923001L, localX * 5 + 31, localZ * 5 - 47);
      double erosionPerBlock = 0.405 + columnNoise * 0.022;
      double best = Double.NEGATIVE_INFINITY;

      for (SiftPortalStructure.ProfilePoint point : CLIFF_POINTS) {
         double dx = (warpedX - point.x()) * 0.76;
         double dz = (warpedZ - point.z()) * 1.08;
         double distance = Math.sqrt(dx * dx + dz * dz);
         double candidate = point.height() - distance * erosionPerBlock;
         if (candidate > best) {
            best = candidate;
         }
      }

      best += columnNoise * 0.55;
      return !Double.isFinite(best) ? -1.0 : best;
   }

   private static double portalApproachMask(int localX, int localZ) {
      int distanceZ;
      if (localZ < 9) {
         distanceZ = 9 - localZ;
      } else if (localZ > 30) {
         distanceZ = localZ - 30;
      } else {
         distanceZ = 0;
      }

      int distanceX;
      if (localX < 0) {
         distanceX = -localX;
      } else {
         if (localX < 9) {
            return 0.0;
         }

         distanceX = localX - 8;
      }

      double zMask = 1.0 - smootherstep(distanceZ / 6.0);
      double xMask = 1.0 - smootherstep(Math.max(0.0, distanceX - 2.0) / 18.0);
      return clamp01(zMask * xMask);
   }

   private static void lowerTerrainColumn(ServerLevel level, int x, int z, int naturalY, int desiredY, BlockState originalTop, boolean preserveSurface) {
      BlockPos oldPlantPos = new BlockPos(x, naturalY + 1, z);
      BlockState oldPlant = level.getBlockState(oldPlantPos);
      boolean preservePlant = preserveSurface
         && !oldPlant.isAir()
         && oldPlant.canBeReplaced()
         && oldPlant.getFluidState().isEmpty()
         && !oldPlant.hasBlockEntity();
      BlockState oldUpper = level.getBlockState(oldPlantPos.above());
      boolean preserveUpper = preservePlant && oldPlant.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && oldUpper.getBlock() == oldPlant.getBlock();
      if (preservePlant) {
         setDuringPortalBuild(level, oldPlantPos, Blocks.AIR.defaultBlockState());
         if (preserveUpper) {
            setDuringPortalBuild(level, oldPlantPos.above(), Blocks.AIR.defaultBlockState());
         }
      }

      MutableBlockPos cursor = new MutableBlockPos();

      for (int y = naturalY; y > desiredY; y--) {
         cursor.set(x, y, z);
         BlockState state = level.getBlockState(cursor);
         if (!isSiftTerrain(state)) {
            break;
         }

         setDuringPortalBuild(level, cursor, Blocks.AIR.defaultBlockState());
      }

      cursor.set(x, desiredY, z);
      BlockState exposed = level.getBlockState(cursor);
      if (!preserveSurface && isSiftTerrain(exposed)) {
         setDuringPortalBuild(level, cursor, SiftLandmarkTerrain.stoneForBiome(level.getBiome(cursor)).defaultBlockState());
      } else if (isSiftTerrain(exposed) && isSiftTerrain(originalTop)) {
         setDuringPortalBuild(level, cursor, originalTop);
      }

      SiftPortalVegetation.register(new BlockPos(x, desiredY, z));
      if (preservePlant) {
         BlockPos newPlantPos = new BlockPos(x, desiredY + 1, z);
         if (level.getBlockState(newPlantPos).isAir() && oldPlant.canSurvive(level, newPlantPos)) {
            setDuringPortalBuild(level, newPlantPos, oldPlant);
            if (preserveUpper && level.getBlockState(newPlantPos.above()).isAir()) {
               setDuringPortalBuild(level, newPlantPos.above(), oldUpper);
            }
         }
      }
   }

   private static void raiseTerrainColumn(ServerLevel level, int x, int z, int naturalY, int desiredY, BlockState originalTop, boolean preserveSurface) {
      BlockPos oldPlantPos = new BlockPos(x, naturalY + 1, z);
      BlockState oldPlant = level.getBlockState(oldPlantPos);
      boolean preservePlant = preserveSurface
         && !oldPlant.isAir()
         && oldPlant.canBeReplaced()
         && oldPlant.getFluidState().isEmpty()
         && !oldPlant.hasBlockEntity();
      BlockState oldUpper = level.getBlockState(oldPlantPos.above());
      boolean preserveUpper = preservePlant && oldPlant.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && oldUpper.getBlock() == oldPlant.getBlock();
      if (preservePlant) {
         setDuringPortalBuild(level, oldPlantPos, Blocks.AIR.defaultBlockState());
         if (preserveUpper) {
            setDuringPortalBuild(level, oldPlantPos.above(), Blocks.AIR.defaultBlockState());
         }
      }

      MutableBlockPos cursor = new MutableBlockPos();

      for (int y = naturalY + 1; y <= desiredY; y++) {
         cursor.set(x, y, z);
         setDuringPortalBuild(level, cursor, SiftLandmarkTerrain.stoneForBiome(level.getBiome(cursor)).defaultBlockState());
      }

      cursor.set(x, desiredY, z);
      if (!preserveSurface) {
         setDuringPortalBuild(level, cursor, SiftLandmarkTerrain.stoneForBiome(level.getBiome(cursor)).defaultBlockState());
      } else if (isSiftTerrain(originalTop)) {
         setDuringPortalBuild(level, cursor, originalTop);
      }

      SiftPortalVegetation.register(new BlockPos(x, desiredY, z));
      if (preservePlant) {
         BlockPos newPlantPos = new BlockPos(x, desiredY + 1, z);
         if (level.getBlockState(newPlantPos).isAir() && oldPlant.canSurvive(level, newPlantPos)) {
            setDuringPortalBuild(level, newPlantPos, oldPlant);
            if (preserveUpper && level.getBlockState(newPlantPos.above()).isAir()) {
               setDuringPortalBuild(level, newPlantPos.above(), oldUpper);
            }
         }
      }
   }

   private static void restorePortalVegetation(ServerLevel level, BlockPos origin, BlockPos playerAnchor) {
      SiftLandmarkTracker.Kind kind = SiftLandmarkTracker.Kind.PORTAL_VEGETATION_REPAIRED;
      if (!SiftLandmarkTracker.hasRecorded(kind, origin)) {
         List<BlockPos> floors = new ArrayList<>(SiftPortalVegetation.around(origin, 9, 40, 52));

         for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 40; z++) {
               for (int y = 0; y < 19; y++) {
                  BlockPos floor = origin.offset(x, y, z);
                  if (SiftPortalVegetation.soil(level.getBlockState(floor)) && level.getBlockState(floor.above()).isAir()) {
                     floors.add(floor);
                  }
               }
            }
         }

         floors.removeIf(floorx -> isPortalApproach(floorx, playerAnchor));
         SiftPortalVegetation.restore(level, floors);
         SiftLandmarkTracker.record(kind, origin);
      }
   }

   private static boolean setDuringPortalBuild(ServerLevel level, BlockPos pos, BlockState state) {
      return level.getBlockState(pos) == state ? false : level.setBlock(pos, state, 48);
   }

   private static SiftPortalStructure.ProfilePoint[] collectProfilePoints(int[][] profile) {
      SiftPortalStructure.ProfilePoint[] temporary = new SiftPortalStructure.ProfilePoint[360];
      int count = 0;

      for (int x = 0; x < 9; x++) {
         for (int z = 0; z < 40; z++) {
            int height = profile[x][z];
            if (height >= 0) {
               temporary[count++] = new SiftPortalStructure.ProfilePoint(x, z, height);
            }
         }
      }

      return Arrays.copyOf(temporary, count);
   }

   private static boolean isPortalApproach(BlockPos pos, BlockPos playerAnchor) {
      int dx = Math.abs(pos.getX() - playerAnchor.getX());
      int dz = Math.abs(pos.getZ() - playerAnchor.getZ());
      return dx <= 1 && dz <= 2 && Math.abs(pos.getY() - (playerAnchor.getY() - 1)) <= 1;
   }

   private static boolean isSiftTerrain(BlockState state) {
      return state.is(Blocks.SCULK)
         || state.is(ModBlocks.ICHOR_SNOW_BLOCK)
         || state.is(ModBlocks.SOULSHALE)
         || state.is(ModBlocks.SOULSHALE_GROWTH)
         || state.is(ModBlocks.CORAL_SOIL)
         || state.is(ModBlocks.CORAL_GROWTH)
         || state.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || state.is(ModBlocks.HEALTHY_SCULK_PORES)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
         || state.is(ModBlocks.SIFTSLATE)
         || state.is(ModBlocks.SIFTSLATE_GROWTH)
         || state.is(ModBlocks.HEALTHY_SCULK)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK);
   }

   private static boolean isSiftPlant(BlockState state) {
      return state.is(ModBlocks.CORAL_BUSH)
         || state.is(ModBlocks.BRIGHT_CORAL_BUSH)
         || state.is(ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT)
         || state.is(ModBlocks.GRIM_BUSH)
         || state.is(ModBlocks.ASH_WILLOW_FOLIAGE)
         || state.is(ModBlocks.ASH_WILLOW_VINES)
         || state.is(ModBlocks.ASH_WILLOW_VINES_PLANT)
         || state.is(ModBlocks.ASH_WILLOW_LOG)
         || state.is(ModBlocks.ASH_WILLOW_WOOD)
         || state.is(ModBlocks.OVERGROWN_CHARD)
         || state.is(ModBlocks.OVERGROWN_STALKS)
         || state.is(ModBlocks.OVERGROWN_FRONDS)
         || state.is(ModBlocks.SIFTSLATE_STALKS)
         || state.is(ModBlocks.HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS)
         || state.is(ModBlocks.OVERGROWN_WILLOW_FOLIAGE)
         || state.is(ModBlocks.OVERGROWN_WILLOW_VINES)
         || state.is(ModBlocks.OVERGROWN_WILLOW_VINES_PLANT)
         || state.is(ModBlocks.OVERGROWN_WILLOW_LOG)
         || state.is(ModBlocks.OVERGROWN_WILLOW_WOOD);
   }

   private static int findTerrainSurfaceBlockY(ServerLevel level, int x, int z) {
      int top = level.getHeight(Types.WORLD_SURFACE, x, z) - 1;
      int minimum = Math.max(level.getMinY() + 1, top - 48);
      MutableBlockPos cursor = new MutableBlockPos();

      for (int y = top; y >= minimum; y--) {
         cursor.set(x, y, z);
         if (isSiftTerrain(level.getBlockState(cursor))) {
            return y;
         }
      }

      return -1;
   }

   private static double distanceOutsideRectangle(int x, int z, int minX, int maxX, int minZ, int maxZ) {
      int dx = x < minX ? minX - x : Math.max(0, x - maxX);
      int dz = z < minZ ? minZ - z : Math.max(0, z - maxZ);
      return Math.sqrt((double)dx * dx + (double)dz * dz);
   }

   private static double terrainBlendNoise(long seed, int x, int z) {
      double phase = (seed & 65535L) * 1.73E-4;
      double a = Math.sin(x * 0.083 + z * 0.031 + phase);
      double b = Math.cos(x * 0.037 - z * 0.071 - phase * 0.73);
      double c = Math.sin((x + z) * 0.021 + phase * 1.91);
      return a * 0.5 + b * 0.33 + c * 0.17;
   }

   private static double smootherstep(double value) {
      double t = clamp01(value);
      return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
   }

   private static double clamp01(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }

   private static int generatedSurfaceBlockY(ServerLevel level, int x, int z) {
      Map<Long, Integer> cache = HEIGHT_CACHE.get();
      long key = (long)x << 32 ^ z & 4294967295L;
      if (cache != null && cache.containsKey(key)) {
         return cache.get(key);
      }

      ServerChunkCache chunkSource = level.getChunkSource();
      int firstFreeY = chunkSource.getGenerator().getBaseHeight(x, z, Types.WORLD_SURFACE_WG, level, chunkSource.randomState());
      if (cache != null) {
         cache.put(key, firstFreeY - 1);
      }

      return firstFreeY - 1;
   }

   private record PlacementCandidate(int anchorX, int anchorZ, int groundY, double roughness) {
   }

   private record ProfilePoint(int x, int z, int height) {
   }

   private record TerrainSample(int medianHeight, int range, int outerRange, double roughness) {
   }

   private static void blendPortalCoral(ServerLevel level, BlockPos origin) {
      BoundingBox bounds = SiftFeaturePlacementGuard.portalTerrainBounds(origin, 9, 40);
      MutableBlockPos floor = new MutableBlockPos();

      for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
         for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            if (CoralSurfaceFeature.brightPatch(level.getSeed(), x, z)) {
               for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
                  floor.set(x, y, z);
                  if (level.getBlockState(floor).is(ModBlocks.CORAL_GROWTH)) {
                     BlockPos above = floor.above();
                     BlockState cover = level.getBlockState(above);
                     if (cover.isAir() || cover.is(ModBlocks.CORAL_BUSH) || cover.is(ModBlocks.BRIGHT_CORAL_BUSH)) {
                        BlockState soil = level.getBlockState(floor.below());
                        if (soil.is(ModBlocks.CORAL_SOIL) || soil.is(ModBlocks.SOULSHALE)) {
                           setDuringPortalBuild(level, floor, ModBlocks.BRIGHT_CORAL_GROWTH.defaultBlockState());
                           if (cover.is(ModBlocks.CORAL_BUSH)) {
                              setDuringPortalBuild(level, above, ModBlocks.BRIGHT_CORAL_BUSH.defaultBlockState());
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
