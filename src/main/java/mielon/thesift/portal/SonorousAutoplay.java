package mielon.thesift.portal;

import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

public final class SonorousAutoplay {
   private SonorousAutoplay() {
   }

   public static void register() {
   }

   public static void start(ServerLevel level, SonorousConsoles.MatchedSequence matched) {
      if (!matched.notes().isEmpty()) {
         BlockPos anchor = SonorousConsoles.findAnchor(level, matched.notes().get(0).notePos());
         if (anchor != null) {
            if (level.getBlockEntity(anchor) instanceof SonorousDeepslateBlockEntity blockEntity) {
               SonorousBeams.clearGroup(level, anchor);
               blockEntity.startAutoplay(matched);
            }
         }
      }
   }

   public static boolean interrupt(ServerLevel level, BlockPos anyBlockInGroup) {
      BlockPos anchor = SonorousConsoles.findAnchor(level, anyBlockInGroup);
      if (anchor == null) {
         return false;
      } else if (level.getBlockEntity(anchor) instanceof SonorousDeepslateBlockEntity blockEntity && blockEntity.stopAutoplay()) {
         SonorousBeams.clearGroup(level, anchor);
         level.playSound(null, anyBlockInGroup, ModSounds.SONOROUS_AUTOPLAY_GLITCH, SoundSource.RECORDS, 3.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }
}
