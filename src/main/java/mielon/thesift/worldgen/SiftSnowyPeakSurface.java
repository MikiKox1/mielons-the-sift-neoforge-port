package mielon.thesift.worldgen;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

public final class SiftSnowyPeakSurface {
   private SiftSnowyPeakSurface() {
   }

   public static boolean rejectsSculk(WorldGenLevel level, BlockPos pos) {
      int top = SiftCaveHabitat.rockTop(level, pos.getX(), pos.getZ());
      return pos.getY() < top - 12
         ? false
         : SiftCaveHabitat.surfaceBiome(level, pos.getX(), pos.getZ()).is(TheSiftDimension.ICHOR_SNOWY_PEAKS) && !SiftCaveHabitat.underground(level, pos);
   }
}
