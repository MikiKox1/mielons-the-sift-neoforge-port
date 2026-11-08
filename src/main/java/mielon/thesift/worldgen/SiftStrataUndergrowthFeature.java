package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftStrataUndergrowthFeature implements Feature {
   public static final MapCodec<SiftStrataUndergrowthFeature> CODEC = MapCodec.unit(SiftStrataUndergrowthFeature::new);

   public MapCodec<SiftStrataUndergrowthFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      int minX = origin.getX() & -16;
      int minZ = origin.getZ() & -16;
      boolean changed = false;
      MutableBlockPos cursor = new MutableBlockPos();

      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            if (!(random.nextFloat() > 0.58F) && !SiftFeaturePlacementGuard.intersectsPortal(new BlockPos(x, 0, z), 1)) {
               int top = level.getHeight(Types.WORLD_SURFACE_WG, x, z) - 1;

               for (int y = Math.min(top, SiftCaveHabitat.surfaceRockBottom(level, x, z) - 1); y >= 16; y--) {
                  cursor.set(x, y, z);
                  if (SiftLandmarkTerrain.isGround(level.getBlockState(cursor))
                     && level.isEmptyBlock(cursor.above())
                     && level.isEmptyBlock(cursor.above(2))
                     && !level.getBiome(cursor).is(TheSiftDimension.OVERGROWN_CAVES)
                     && !level.getBiome(cursor).is(TheSiftDimension.SIFTSLATE_CAVES)) {
                     changed |= SiftSurfaceDecorator.decorate(level, random, cursor.immutable(), 1.35F);
                     y -= 4;
                  }
               }
            }
         }
      }

      return changed;
   }
}
