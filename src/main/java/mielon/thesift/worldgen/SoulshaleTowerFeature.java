package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SoulshaleTowerFeature implements Feature {
   public static final MapCodec<SoulshaleTowerFeature> CODEC = MapCodec.unit(SoulshaleTowerFeature::new);
   private static final int RADIUS = 8;

   public MapCodec<SoulshaleTowerFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (!level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return false;
      }

      int x = (origin.getX() & -16) + 7 + random.nextInt(2);
      int z = (origin.getZ() & -16) + 7 + random.nextInt(2);
      BlockPos base = findBase(level, random, x, z);
      if (base != null
         && clearColumn(level, base, 3)
         && buriedChestSite(level, base.below())
         && level.ensureCanWrite(base.below())
         && !SiftFeaturePlacementGuard.intersectsPortal(base, 8)
         && !SiftFeaturePlacementGuard.intersectsReservedLandmark(base, 8)
         && !SiftCampProtection.near(level, x, z, 8)) {
         int target = 3 + random.nextInt(3);
         List<BlockPos> candidates = new ArrayList<>();

         for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
               int distance = dx * dx + dz * dz;
               if (distance > 0 && distance <= 64) {
                  candidates.add(new BlockPos(dx, 0, dz));
               }
            }
         }

         List<BlockPos> scattered = new ArrayList<>();

         while (!candidates.isEmpty() && scattered.size() < target) {
            BlockPos offset = candidates.remove(random.nextInt(candidates.size()));
            BlockPos pos = nearbyGround(level, x + offset.getX(), base.getY(), z + offset.getZ());
            if (pos != null && Math.abs(pos.getY() - base.getY()) <= 4 && clearColumn(level, pos, 1)) {
               scattered.add(pos);
            }
         }

         if (scattered.size() == target && SiftFeaturePlacementGuard.tryReserveSolid(x - 8, z - 8, x + 8, z + 8)) {
            BlockPos chestPos = base.below();
            BlockState ground = level.getBlockState(chestPos);
            Direction facing = Direction.from2DDataValue(random.nextInt(4));
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
               chest.setLootTable(
                  ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("the_sift", "chests/soulshale_tower")), random.nextLong()
               );
               level.setBlock(base, ModBlocks.POLISHED_SOULSHALE.defaultBlockState(), 2);
               level.setBlock(base.above(), ModBlocks.POLISHED_SOULSHALE.defaultBlockState(), 2);
               level.setBlock(base.above(2), ModBlocks.CHISELED_SOULSHALE.defaultBlockState(), 2);
               protect(base, -1, 2);

               for (BlockPos pos : scattered) {
                  level.setBlock(pos, ModBlocks.POLISHED_SOULSHALE.defaultBlockState(), 2);
                  BlockPos above = pos.above();
                  BlockState plant = level.getBlockState(above);
                  if (!plant.isAir()
                     && plant.canBeReplaced()
                     && plant.getFluidState().isEmpty()
                     && !plant.canSurvive(level, above)
                     && level.ensureCanWrite(above)) {
                     level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
                  }

                  protect(pos, 0, 0);
               }

               return true;
            } else {
               level.setBlock(chestPos, ground, 2);
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void protect(BlockPos pos, int bottom, int top) {
      SiftTerrainFeatureMemory.protectTerrain(pos.getX(), pos.getZ(), pos.getY() + bottom, pos.getY() + top);
   }

   private static boolean clearColumn(WorldGenLevel level, BlockPos base, int height) {
      for (int dy = 0; dy < height; dy++) {
         BlockPos pos = base.above(dy);
         BlockState state = level.getBlockState(pos);
         if (!level.ensureCanWrite(pos) || !state.getFluidState().isEmpty() || !state.isAir() && !state.canBeReplaced() || level.getBlockEntity(pos) != null) {
            return false;
         }
      }

      return true;
   }

   private static BlockPos findBase(WorldGenLevel level, RandomSource random, int x, int z) {
      int top = Math.min(level.getHeight(Types.WORLD_SURFACE_WG, x, z), level.getMaxY() - 3);
      BlockPos selected = null;
      int candidates = 0;

      for (int y = top; y > level.getMinY() + 2; y--) {
         BlockPos pos = new BlockPos(x, y, z);
         if (naturalGround(level, pos.below()) && buriedChestSite(level, pos.below()) && clearColumn(level, pos, 3)) {
            if (random.nextInt(++candidates) == 0) {
               selected = pos;
            }
         }
      }

      return selected;
   }

   private static BlockPos nearbyGround(WorldGenLevel level, int x, int centerY, int z) {
      for (int distance = 0; distance <= 4; distance++) {
         for (int direction : new int[]{1, -1}) {
            if (distance != 0 || direction != -1) {
               BlockPos pos = new BlockPos(x, centerY + distance * direction, z);
               if (pos.getY() > level.getMinY() && pos.getY() < level.getMaxY() && naturalGround(level, pos.below()) && clearColumn(level, pos, 1)) {
                  return pos;
               }
            }
         }
      }

      return null;
   }

   private static boolean buriedChestSite(WorldGenLevel level, BlockPos pos) {
      if (!naturalGround(level, pos.below())) {
         return false;
      }

      for (Direction direction : Plane.HORIZONTAL) {
         if (!naturalGround(level, pos.relative(direction))) {
            return false;
         }
      }

      return true;
   }

   private static boolean naturalGround(WorldGenLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return (
            SiftMonolithFeature.isSiftTerrain(state) || state.is(ModBlocks.SOULSHALE_GROWTH) || state.is(ModBlocks.ICHOR_SNOW_BLOCK) || state.is(Blocks.SCULK)
         )
         && state.isFaceSturdy(level, pos, Direction.UP)
         && state.getFluidState().isEmpty()
         && level.getBlockEntity(pos) == null;
   }
}
