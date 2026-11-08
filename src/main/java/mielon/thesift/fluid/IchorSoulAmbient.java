package mielon.thesift.fluid;

import mielon.thesift.particle.ModParticles;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;

public final class IchorSoulAmbient {
   private IchorSoulAmbient() {
   }

   public static void animate(Level level, BlockPos pos, FluidState state, RandomSource random) {
      if (state.isSource() && level.getBlockState(pos.above()).isAir()) {
         double surface = pos.getY() + state.getHeight(level, pos);
         if (random.nextInt(48) == 0) {
            level.addParticle(
               ModParticles.ICHOR_SOUL_SURFACE,
               pos.getX() + random.nextDouble(),
               surface - 0.1 - random.nextDouble() * 0.5,
               pos.getZ() + 0.25 + random.nextDouble() * 0.5,
               0.0,
               0.0,
               0.0
            );
         }

         if (random.nextInt(18) == 0 && canRise(level, pos)) {
            level.addParticle(
               ModParticles.ICHOR_SOUL_RISING,
               pos.getX() + 0.2 + random.nextDouble() * 0.6,
               surface + 0.05,
               pos.getZ() + 0.2 + random.nextDouble() * 0.6,
               0.0,
               0.0,
               0.0
            );
         }
      }
   }

   public static boolean canRise(Level level, BlockPos pos) {
      boolean roof = level.getHeight(Types.WORLD_SURFACE, pos.getX(), pos.getZ()) > pos.getY() + 1;
      if (!roof && !isSoulNight(level)) {
         return false;
      }

      for (int y = 1; y <= 12; y++) {
         if (!level.getBlockState(pos.above(y)).isAir()) {
            return false;
         }
      }

      return true;
   }

   public static boolean isSoulNight(Level level) {
      if (level.dimension() == Level.NETHER || level.dimension() == Level.END) {
         return true;
      }

      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return SiftTides.nightBlend(level.getDefaultClockTime()) >= 0.5F;
      }

      long tick = Math.floorMod(level.getDefaultClockTime(), 24000L);
      return level.dimension() == Level.OVERWORLD && tick >= 13000L && tick < 23000L;
   }
}
