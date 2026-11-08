package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftCaveIchorLakeFeature implements Feature {
   public static final MapCodec<SiftCaveIchorLakeFeature> CODEC = MapCodec.unit(SiftCaveIchorLakeFeature::new);

   public MapCodec<SiftCaveIchorLakeFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      if (random.nextInt(5) != 0) {
         return false;
      }

      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;

      for (int attempt = 0; attempt < 16; attempt++) {
         int x = minX + 3 + random.nextInt(10);
         int z = minZ + 3 + random.nextInt(10);

         for (int y = Math.min(210, SiftCaveHabitat.rockTop(level, x, z) - 7); y >= 16; y--) {
            BlockPos floor = new BlockPos(x, y, z);
            if (level.getBiome(floor).is(TheSiftDimension.SOULSHALE_CAVES)
               && (
                  level.getBlockState(floor).is(ModBlocks.SOULSHALE)
                     || level.getBlockState(floor).is(ModBlocks.SOULSHALE_GROWTH)
                     || level.getBlockState(floor).is(ModBlocks.DRY_HEALTHY_SCULK)
               )
               && level.isEmptyBlock(floor.above(2))
               && placePond(level, random, floor, SiftWorldgenBounds.aroundWithNeighbourMargin(origin))) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean placePond(WorldGenLevel level, RandomSource random, BlockPos center, SiftWorldgenBounds bounds) {
      double rx = 4.0 + random.nextDouble() * 2.5;
      double rz = 3.5 + random.nextDouble() * 2.5;
      double phase = random.nextDouble() * Math.PI * 2.0;
      List<SiftCaveIchorLakeFeature.Column> columns = new ArrayList<>();
      int r = 8;
      int waterY = center.getY() - 1;

      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            double distance = Math.sqrt(dx * dx / (rx * rx) + dz * dz / (rz * rz));
            double edge = 1.0 + 0.08 * Math.sin(Math.atan2(dz, dx) * 3.0 + phase);
            if (!(distance > edge + 0.32)) {
               int x = center.getX() + dx;
               int z = center.getZ() + dz;
               if (!bounds.contains(x, z) || SiftFeaturePlacementGuard.intersectsReservedLandmark(new BlockPos(x, waterY, z), 1)) {
                  return false;
               }

               int top = floorNear(level, x, z, center.getY());
               if (top == Integer.MIN_VALUE || top < waterY || top > waterY + 3) {
                  return false;
               }

               boolean wet = distance < edge;
               int bed = waterY - (wet ? 1 + (int)((1.0 - distance / edge) * 2.2) : 0);

               for (int y = bed - 2; y <= top; y++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  if (!level.ensureCanWrite(pos)
                     || !SiftLandmarkTerrain.isGround(level.getBlockState(pos))
                     || !level.getBiome(pos).is(TheSiftDimension.SOULSHALE_CAVES)
                     || !SiftCaveHabitat.underground(level, pos)
                     || SiftTerrainFeatureMemory.isProtectedTerrain(x, y, z)) {
                     return false;
                  }
               }

               columns.add(new SiftCaveIchorLakeFeature.Column(x, z, top, bed, wet));
            }
         }
      }

      if (!SiftFeaturePlacementGuard.tryReserveCarver(center.getX() - r, center.getZ() - r, center.getX() + r, center.getZ() + r)) {
         return false;
      }

      int shore = random.nextInt(4);

      for (SiftCaveIchorLakeFeature.Column column : columns) {
         SiftTerrainFeatureMemory.excludeRibs(column.x(), column.z(), column.bed() - 2, column.top() + 3);
         SiftTerrainFeatureMemory.protectTerrain(column.x(), column.z(), column.bed() - 2, column.top() + 3);

         for (int y = column.bed() - 1; y <= column.top(); y++) {
            BlockPos pos = new BlockPos(column.x(), y, column.z());
            BlockState state = ModBlocks.SOULSHALE.defaultBlockState();
            if (column.wet() && y > column.bed()) {
               state = y <= waterY ? ModFluids.ICHOR_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
            } else if (!column.wet() && y == column.top() && shore != 0) {
               boolean dry = shore == 2 || shore == 3 && Math.sin(Math.atan2(column.z() - center.getZ(), column.x() - center.getX()) + phase) > 0.0;
               state = (dry ? ModBlocks.DRY_HEALTHY_SCULK : ModBlocks.SOULSHALE_GROWTH).defaultBlockState();
            }

            level.setBlock(pos, state, 2);
         }
      }

      return true;
   }

   private static int floorNear(WorldGenLevel level, int x, int z, int referenceY) {
      for (int y = referenceY + 2; y >= referenceY - 2; y--) {
         BlockPos pos = new BlockPos(x, y, z);
         if (SiftLandmarkTerrain.isGround(level.getBlockState(pos))) {
            boolean clear = true;

            for (int dy = 1; dy <= 5; dy++) {
               if (!level.isEmptyBlock(pos.above(dy))) {
                  clear = false;
                  break;
               }
            }

            if (clear) {
               return y;
            }
         }
      }

      return Integer.MIN_VALUE;
   }

   private record Column(int x, int z, int top, int bed, boolean wet) {
   }
}
