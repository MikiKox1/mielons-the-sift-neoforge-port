package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftStrataRimFeature implements Feature {
   public static final MapCodec<SiftStrataRimFeature> CODEC = MapCodec.unit(SiftStrataRimFeature::new);

   public MapCodec<SiftStrataRimFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      SiftWorldgenBounds bounds = SiftWorldgenBounds.aroundWithNeighbourMargin(origin);
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      if (SiftCampProtection.near(level, minX + 8, minZ + 8, 16)) {
         return false;
      }

      List<SiftStrataRimFeature.Rib> ribs = new ArrayList<>();
      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            if (!SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(x, 0, z), 8)) {
               int top = Math.min(220, level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1);

               for (int y = top; y >= 40; y--) {
                  cursor.set(x, y, z);
                  if (!mountainSurface(level, cursor)
                     && SiftLandmarkTerrain.isGround(level.getBlockState(cursor))
                     && clearAbove(level.getBlockState(cursor.above()))) {
                     BlockPos edge = cursor.immutable().below();
                     BlockState source = level.getBlockState(edge);
                     if (SiftLandmarkTerrain.isGround(source) && !SiftTerrainFeatureMemory.isCanyonWall(x, edge.getY(), z)) {
                        BlockState rock = ribMaterial(source);
                        if (level.getBiome(edge).is(TheSiftDimension.OVERGROWN_CAVES)
                           && (source.is(ModBlocks.DRY_HEALTHY_SCULK) || source.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH))) {
                           rock = ModBlocks.SIFTSLATE.defaultBlockState();
                        }

                        BlockState surface = level.getBlockState(cursor);
                        if (!surface.is(Blocks.SCULK) || !source.is(ModBlocks.SIFTSLATE) && !source.is(Blocks.SCULK)) {
                           if (source.is(ModBlocks.SIFTSLATE)
                              && surface.is(ModBlocks.SIFTSLATE_GROWTH)
                              && TheSiftDimension.isOvergrownBiome(level.getBiome(cursor))
                              && !level.getBiome(cursor).is(TheSiftDimension.OVERGROWN_CAVES)
                              && random.nextInt(3) == 0) {
                              rock = surface;
                           }
                        } else {
                           rock = Blocks.SCULK.defaultBlockState();
                        }

                        List<Direction> directions = new ArrayList<>();

                        for (Direction direction : Plane.HORIZONTAL) {
                           BlockPos outside = edge.relative(direction);
                           if (bounds.contains(outside) && level.isEmptyBlock(outside) && level.isEmptyBlock(outside.below(2))) {
                              directions.add(direction);
                           }
                        }

                        if (!directions.isEmpty() && random.nextInt(3) != 0) {
                           ribs.add(
                              new SiftStrataRimFeature.Rib(
                                 edge, rock, directions.get(random.nextInt(directions.size())), 3 + random.nextInt(4), random.nextBoolean()
                              )
                           );
                        }

                        y -= 5;
                     }
                  }
               }
            }
         }
      }

      boolean changed = false;

      for (SiftStrataRimFeature.Rib rib : ribs) {
         BlockPos previous = rib.base();
         int branchAt = 2 + random.nextInt(2);
         Direction sideways = rib.direction().getClockWise();
         if (rib.left()) {
            sideways = sideways.getOpposite();
         }

         for (int step = 1; step <= rib.length(); step++) {
            BlockPos next = previous.relative(rib.direction());
            if (!placeAttached(level, bounds, next, rib.rock())) {
               break;
            }

            changed = true;
            if (step == 1) {
               placeAttached(level, bounds, next.below(), rib.rock());
            }

            if (step == branchAt) {
               BlockPos branch = next.relative(sideways);
               if (placeAttached(level, bounds, branch, rib.rock()) && random.nextBoolean()) {
                  placeAttached(level, bounds, branch.relative(sideways), rib.rock());
               }
            }

            previous = next;
            if (step == 3 && random.nextInt(3) == 0) {
               BlockPos lower = previous.below();
               if (placeAttached(level, bounds, lower, rib.rock())) {
                  previous = lower;
               }
            }
         }
      }

      return changed;
   }

   private static boolean placeAttached(WorldGenLevel level, SiftWorldgenBounds bounds, BlockPos pos, BlockState rock) {
      if (bounds.contains(pos)
         && level.ensureCanWrite(pos)
         && !mountainSurface(level, pos)
         && level.isEmptyBlock(pos)
         && !SiftTerrainFeatureMemory.isCanyonWall(pos.getX(), pos.getY(), pos.getZ())) {
         level.setBlock(pos, SiftLandmarkTerrain.growthSurface(level, pos, rock), 2);
         return true;
      } else {
         return false;
      }
   }

   private static boolean mountainSurface(WorldGenLevel level, BlockPos pos) {
      return SiftCaveHabitat.biome(level, pos).is(TheSiftDimension.ICHOR_SNOWY_PEAKS)
         || SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ()).is(TheSiftDimension.ICHOR_SNOWY_PEAKS) && !SiftCaveHabitat.underground(level, pos);
   }

   private static boolean clearAbove(BlockState state) {
      return state.getFluidState().isEmpty() && !SiftLandmarkTerrain.isGround(state) && SiftLandmarkTerrain.canReplace(state);
   }

   private static BlockState ribMaterial(BlockState source) {
      if (source.is(ModBlocks.SOULSHALE_GROWTH)
         || source.is(ModBlocks.SOULSHALE_COAL_ORE)
         || source.is(ModBlocks.SOULSHALE_DIAMOND_ORE)
         || source.is(ModBlocks.SOULSHALE_EMERALD_ORE)
         || source.is(ModBlocks.SOULSHALE_CHAROITE_ORE)
         || source.is(ModBlocks.SOULSHALE_SIFTITE_ORE)) {
         return ModBlocks.SOULSHALE.defaultBlockState();
      } else {
         return !source.is(ModBlocks.CORAL_GROWTH)
               && !source.is(ModBlocks.BRIGHT_CORAL_GROWTH)
               && !source.is(ModBlocks.CORAL_SOIL)
               && !source.is(ModBlocks.SOULSHALE)
               && !source.is(ModBlocks.SIFTSLATE)
               && !source.is(ModBlocks.SIFTSLATE_GROWTH)
               && !source.is(ModBlocks.COBBLED_SIFTSLATE)
               && !source.is(ModBlocks.HEALTHY_SCULK)
               && !source.is(ModBlocks.DRY_HEALTHY_SCULK)
               && !source.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH)
               && !source.is(Blocks.SCULK)
               && !source.is(Blocks.BONE_BLOCK)
            ? ModBlocks.SIFTSLATE.defaultBlockState()
            : source;
      }
   }

   private record Rib(BlockPos base, BlockState rock, Direction direction, int length, boolean left) {
   }
}
