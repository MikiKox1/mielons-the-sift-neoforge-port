package mielon.thesift.client.render;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.state.BlockState;

public final class WillowFoliageTint implements BlockTintSource {
   @Override
   public int color(BlockState state) {
      return -3355444;
   }

   @Override
   public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      CardinalLighting lighting = level.cardinalLighting();
      int channel = Math.clamp(Math.round(255.0F * lighting.north() / lighting.up()), 0, 255);
      return 0xFF000000 | channel << 16 | channel << 8 | channel;
   }

   @Override
   public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
      return -1;
   }
}
