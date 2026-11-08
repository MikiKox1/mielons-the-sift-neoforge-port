package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class SiftIchorRavineFinishFeature implements Feature {
   public static final MapCodec<SiftIchorRavineFinishFeature> CODEC = MapCodec.unit(SiftIchorRavineFinishFeature::new);

   public MapCodec<SiftIchorRavineFinishFeature> codec() {
      return CODEC;
   }

   public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
      return SiftIchorTerracesFeature.fillRavines(level, origin);
   }
}
