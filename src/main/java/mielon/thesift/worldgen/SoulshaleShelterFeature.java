package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.OvergrownWillowVinesBlock;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class SoulshaleShelterFeature implements Feature {
   public static final MapCodec<SoulshaleShelterFeature> CODEC = MapCodec.unit(SoulshaleShelterFeature::new);
   public static final int CELL_CHUNKS = 8;
   public static final int CANDIDATES = 1;
   public static final long SALT = 7670146607783890456L;
   public static final int MIN_SEPARATION = 96;
   private static final int[] DOOR_Z = new int[]{4, 3, 5, 4, 3, 5, 4, 3, 5, 4, 3, 5};
   private static final int MAX_BURIAL = 3;

   public MapCodec<SoulshaleShelterFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (!SiftLandmarkPlacement.isSelectedChunk(level.getSeed(), origin, 8, 7670146607783890456L, 1)) {
         return false;
      }

      int variant = random.nextInt(DOOR_Z.length);
      Optional<StructureTemplate> optional = level.getLevel()
         .getStructureTemplateManager()
         .get(Identifier.fromNamespaceAndPath("the_sift", "soulshale_shelter_" + (variant + 1)));
      if (optional.isEmpty()) {
         return false;
      }

      StructureTemplate template = optional.get();
      if (template.getSize().getX() == 9 && template.getSize().getY() == 5 && template.getSize().getZ() == 9) {
         SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
         int chunkX = origin.getX() & -16;
         int chunkZ = origin.getZ() & -16;
         int firstColumn = random.nextInt(256);
         int firstRotation = random.nextInt(4);
         Rotation[] rotations = Rotation.values();

         for (int index = 0; index < 256; index++) {
            int column = firstColumn + index * 73 & 0xFF;
            int x = chunkX + (column & 15);
            int z = chunkZ + (column >> 4);
            int top = Math.min(level.getMaxY() - 8, SiftCaveHabitat.surfaceRockBottom(level, x, z) - 6);

            for (int y = top; y >= Math.max(12, level.getMinY() + 4); y--) {
               BlockPos entrance = new BlockPos(x, y, z);
               if (clear(level, entrance)
                  && clear(level, entrance.above())
                  && rock(level, entrance.below())
                  && SiftCaveHabitat.biome(level, entrance).is(TheSiftDimension.SOULSHALE_CAVES)
                  && !SiftLandmarkTracker.nearestOther(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, entrance, 96.0, null).isPresent()) {
                  for (int turn = 0; turn < 4; turn++) {
                     Rotation rotation = rotations[(firstRotation + turn) % 4];

                     for (int burial = 0; burial <= 3; burial++) {
                        BlockPos base = entrance.subtract(transform(-burial, 1, DOOR_Z[variant], rotation));
                        if (fitsWall(level, bounds, base, rotation, DOOR_Z[variant], burial, wide(variant))
                           && decorationsHaveSupport(level, template, base, rotation)) {
                           BlockPos min = at(base, rotation, -burial - 3, -1, -1);
                           BlockPos max = at(base, rotation, 9, 5, 9);
                           if (!SiftLandmarkTracker.tryClaimShelter(entrance, 96.0)) {
                              return false;
                           }

                           if (SiftFeaturePlacementGuard.tryReserveSolid(
                              Math.min(min.getX(), max.getX()),
                              Math.min(min.getZ(), max.getZ()),
                              Math.max(min.getX(), max.getX()),
                              Math.max(min.getZ(), max.getZ())
                           )) {
                              for (int dx = -burial - 3; dx <= 9; dx++) {
                                 for (int dz = -1; dz <= 9; dz++) {
                                    BlockPos p = at(base, rotation, dx, 0, dz);
                                    SiftTerrainFeatureMemory.protectShelter(p.getX(), p.getZ(), base.getY() - 1, base.getY() + 5);
                                 }
                              }

                              if (!placeRoom(level, template, base, rotation, random, variant, burial)) {
                                 SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, entrance);
                                 return false;
                              }

                              return true;
                           }

                           SiftLandmarkTracker.forget(SiftLandmarkTracker.Kind.SOULSHALE_SHELTER, entrance);
                        }
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean fitsWall(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos base, Rotation rotation, int doorZ, int burial, boolean wide) {
      for (int x : new int[]{-burial - 3, 9}) {
         for (int z : new int[]{-1, 9}) {
            BlockPos p = at(base, rotation, x, 0, z);
            if (!bounds.contains(p) || SiftFeaturePlacementGuard.intersectsPortal(p, 1)) {
               return false;
            }
         }
      }

      BlockPos room = at(base, rotation, 5, 2, 4);
      if (SiftCaveHabitat.grim(level, room) && !SiftFeaturePlacementGuard.intersectsReservedLandmark(room, 7)) {
         for (int x = -burial - 2; x <= -burial; x++) {
            BlockPos feet = at(base, rotation, x, 1, doorZ);
            if (!clear(level, feet)
               || !clear(level, feet.above())
               || !rock(level, feet.below()) && (!clear(level, feet.below()) || !rock(level, feet.below(2)))) {
               return false;
            }
         }

         if (!rock(level, at(base, rotation, 8, 2, doorZ))) {
            return false;
         }

         for (int z : new int[]{2, 6}) {
            for (int y = 0; y <= 3; y++) {
               if (!rock(level, at(base, rotation, 2, y, z))) {
                  return false;
               }
            }
         }

         for (int z = 3; z <= 5; z++) {
            if (!rock(level, at(base, rotation, 2, 0, z))) {
               return false;
            }
         }

         for (int x = 3; x <= 9; x++) {
            for (int z = -1; z <= 9; z++) {
               for (int y = -1; y <= 5; y++) {
                  BlockPos p = at(base, rotation, x, y, z);
                  if (!rock(level, p) || !writable(level, p)) {
                     return false;
                  }
               }
            }
         }

         for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 8; z++) {
               for (int y = 0; y <= 4; y++) {
                  BlockPos p = at(base, rotation, x, y, z);
                  if (!writable(level, p) || !rock(level, p) && !clear(level, p)) {
                     return false;
                  }
               }
            }
         }

         for (int x = -burial; x <= 3; x++) {
            for (int z = doorZ; z <= doorZ + (wide ? 1 : 0); z++) {
               if (!rock(level, at(base, rotation, x, 0, z))) {
                  return false;
               }

               for (int y = 1; y <= 2; y++) {
                  BlockPos p = at(base, rotation, x, y, z);
                  if (!writable(level, p) || !rock(level, p) && !clear(level, p)) {
                     return false;
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean placeRoom(
      WorldGenLevel level, StructureTemplate template, BlockPos base, Rotation rotation, RandomSource random, int variant, int burial
   ) {
      List<Block> decorations = decorationBlocks();
      List<Block> ignored = new ArrayList<>(decorations);
      ignored.add(Blocks.STRUCTURE_BLOCK);
      ignored.add(Blocks.AIR);
      ignored.add(ModBlocks.SOULSHALE);
      StructurePlaceSettings settings = new StructurePlaceSettings()
         .setRotation(rotation)
         .setMirror(Mirror.NONE)
         .setIgnoreEntities(true)
         .setKnownShape(false)
         .setRandom(random)
         .addProcessor(new BlockIgnoreProcessor(ignored));
      List<StructureBlockInfo> plants = decorationInfos(template, base, settings);
      Set<BlockPos> authoredAir = new HashSet<>();

      for (StructureBlockInfo info : template.filterBlocks(base, settings, Blocks.AIR)) {
         authoredAir.add(info.pos());
      }

      for (StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.AIR)) {
         BlockPos p = info.pos();
         if (p.getX() >= 3) {
            level.setBlock(at(base, rotation, p.getX(), p.getY(), p.getZ()), Blocks.AIR.defaultBlockState(), 2);
         }
      }

      for (int x = -burial; x <= 3; x++) {
         for (int z = DOOR_Z[variant]; z <= DOOR_Z[variant] + (wide(variant) ? 1 : 0); z++) {
            for (int y = 1; y <= 2; y++) {
               level.setBlock(at(base, rotation, x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
         }
      }

      for (StructureBlockInfo info : plants) {
         level.setBlock(info.pos(), Blocks.AIR.defaultBlockState(), 2);
         if (!info.state().is(ModBlocks.ASH_WILLOW_VINES) && authoredAir.contains(info.pos().above())) {
            level.setBlock(info.pos().above(), Blocks.AIR.defaultBlockState(), 2);
         }
      }

      if (!template.placeInWorld(level, base, base, settings, random, 2)) {
         return false;
      }

      Map<BlockPos, BlockState> authoredStone = authoredStone(template, base, settings);

      for (StructureBlockInfo info : plants) {
         if (!info.state().is(ModBlocks.ASH_WILLOW_VINES)) {
            for (int depth = 1; depth <= 3; depth++) {
               BlockPos support = info.pos().below(depth);
               BlockState stone = authoredStone.get(support);
               if (stone == null) {
                  break;
               }

               boolean grounded = rock(level, support);
               level.setBlock(support, stone, 2);
               if (grounded) {
                  break;
               }
            }
         } else {
            for (Direction face : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP}) {
               BooleanProperty property = VineBlock.getPropertyForFace(face);
               if (info.state().hasProperty(property) && info.state().getValue(property)) {
                  BlockPos support = info.pos().relative(face);
                  BlockState stone = authoredStone.get(support);
                  if (stone != null) {
                     level.setBlock(support, stone, 2);
                  }
               }
            }
         }
      }

      plants.sort(Comparator.<StructureBlockInfo>comparingInt(infox -> infox.pos().getY()).reversed());

      for (StructureBlockInfo info : plants) {
         if (level.isEmptyBlock(info.pos()) && info.state().canSurvive(level, info.pos())) {
            level.setBlock(info.pos(), info.state(), 2);
         }
      }

      for (StructureBlockInfo info : plants) {
         BlockState state = level.getBlockState(info.pos());
         if (state.is(ModBlocks.ASH_WILLOW_VINES) || state.is(ModBlocks.ASH_WILLOW_VINES_PLANT)) {
            level.setBlock(info.pos(), OvergrownWillowVinesBlock.refreshBottomFaces(state, level.getBlockState(info.pos().below())), 18);
         }
      }

      return true;
   }

   private static List<Block> decorationBlocks() {
      return List.of(ModBlocks.GRIM_BUSH, ModBlocks.ASH_WILLOW_VINES, Blocks.DYED_CANDLE.black(), Blocks.DYED_CANDLE.white());
   }

   private static List<StructureBlockInfo> decorationInfos(StructureTemplate template, BlockPos base, StructurePlaceSettings settings) {
      List<StructureBlockInfo> result = new ArrayList<>();

      for (Block block : decorationBlocks()) {
         result.addAll(template.filterBlocks(base, settings, block));
      }

      return result;
   }

   private static Map<BlockPos, BlockState> authoredStone(StructureTemplate template, BlockPos base, StructurePlaceSettings settings) {
      Map<BlockPos, BlockState> result = new HashMap<>();

      for (StructureBlockInfo info : template.filterBlocks(base, settings, ModBlocks.SOULSHALE)) {
         result.put(info.pos(), info.state());
      }

      return result;
   }

   private static boolean decorationsHaveSupport(WorldGenLevel level, StructureTemplate template, BlockPos base, Rotation rotation) {
      StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
      Map<BlockPos, BlockState> stone = authoredStone(template, base, settings);

      for (StructureBlockInfo info : decorationInfos(template, base, settings)) {
         if (!info.state().is(ModBlocks.ASH_WILLOW_VINES)) {
            boolean grounded = false;

            for (int depth = 1; depth <= 3; depth++) {
               BlockPos support = info.pos().below(depth);
               if (rock(level, support)) {
                  grounded = true;
                  break;
               }

               if (!stone.containsKey(support)) {
                  break;
               }
            }

            if (!grounded) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean wide(int variant) {
      return variant % 6 == 3 || variant % 6 == 5;
   }

   private static boolean writable(WorldGenLevel level, BlockPos p) {
      BlockState state = level.getBlockState(p);
      return level.ensureCanWrite(p)
         && !state.hasBlockEntity()
         && state.getFluidState().isEmpty()
         && !SiftTerrainFeatureMemory.isProtectedTerrain(p.getX(), p.getY(), p.getZ());
   }

   private static boolean rock(WorldGenLevel level, BlockPos p) {
      BlockState state = level.getBlockState(p);
      return SiftLandmarkTerrain.isGround(state) && state.getFluidState().isEmpty() && !state.hasBlockEntity();
   }

   private static boolean clear(WorldGenLevel level, BlockPos p) {
      BlockState state = level.getBlockState(p);
      return state.getFluidState().isEmpty()
         && !state.hasBlockEntity()
         && (
            state.isAir()
               || !SiftLandmarkTerrain.isGround(state)
                  && (
                     SiftLandmarkTerrain.canReplace(state)
                        || state.is(ModBlocks.GRIM_BUSH)
                        || state.is(ModBlocks.WHISPERBLOOM)
                        || state.is(ModBlocks.ASH_WILLOW_VINES)
                  )
         );
   }

   private static BlockPos transform(int x, int y, int z, Rotation rotation) {
      return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, rotation, BlockPos.ZERO);
   }

   private static BlockPos at(BlockPos base, Rotation rotation, int x, int y, int z) {
      return transform(x, y, z, rotation).offset(base);
   }
}
