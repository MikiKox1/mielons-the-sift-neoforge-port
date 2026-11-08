package mielon.thesift.portal;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class SonorousBeams {
   private static final int RETRACT_DELAY_TICKS = 60;

   private SonorousBeams() {
   }

   public static void start(ServerLevel level, BlockPos notePos, int soundIndex1to8) {
      BlockPos deepslatePos = notePos.below();
      if (level.getBlockState(deepslatePos).is(ModBlocks.SONOROUS_DEEPSLATE)) {
         if (level.getBlockEntity(deepslatePos) instanceof SonorousDeepslateBlockEntity beam) {
            beam.startBeam(SonorousColors.packedRGB(soundIndex1to8));
         }
      }
   }

   public static void clearGroup(ServerLevel level, BlockPos anyBlockInGroup) {
      for (BlockPos deepslatePos : SonorousConsoles.findGroup(level, anyBlockInGroup)) {
         if (level.getBlockEntity(deepslatePos) instanceof SonorousDeepslateBlockEntity beam) {
            beam.clearBeam();
         }
      }
   }

   public static void failGroup(ServerLevel level, BlockPos anyBlockInGroup) {
      for (BlockPos pos : SonorousConsoles.findGroup(level, anyBlockInGroup)) {
         if (level.getBlockEntity(pos) instanceof SonorousDeepslateBlockEntity beam) {
            beam.failBeam();
         }
      }
   }

   public static void scheduleRetract(ServerLevel level, BlockPos anyBlockInGroup) {
      for (BlockPos deepslatePos : SonorousConsoles.findGroup(level, anyBlockInGroup)) {
         if (level.getBlockEntity(deepslatePos) instanceof SonorousDeepslateBlockEntity beam) {
            beam.scheduleShrink(60);
         }
      }
   }
}
