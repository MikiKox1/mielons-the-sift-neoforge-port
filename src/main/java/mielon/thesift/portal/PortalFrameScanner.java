package mielon.thesift.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PortalFrameScanner {
   private static final int MIN_TOTAL_SIZE = 3;
   private static final int MAX_SCAN = 23;

   public static Optional<PortalFrameScanner.Frame> scan(Level level, BlockPos clicked) {
      Optional<PortalFrameScanner.Frame> onX = scanAxis(level, clicked, Axis.X, false);
      return onX.isPresent() ? onX : scanAxis(level, clicked, Axis.Z, false);
   }

   public static Optional<PortalFrameScanner.Frame> scanForClosing(Level level, BlockPos clicked) {
      Optional<PortalFrameScanner.Frame> onX = scanAxis(level, clicked, Axis.X, true);
      return onX.isPresent() ? onX : scanAxis(level, clicked, Axis.Z, true);
   }

   private static boolean isFrame(Level level, BlockPos pos) {
      return level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.REINFORCED_DEEPSLATE);
   }

   public static boolean isAirLikeInterior(Level level, BlockPos pos) {
      if (!level.hasChunkAt(pos)) {
         return false;
      }

      BlockState state = level.getBlockState(pos);
      return state.is(ModBlocks.SIFT_PORTAL) && !state.getValue(SiftPortalBlock.ACTIVE)
         || state.isAir()
         || state.is(Blocks.SCULK_VEIN)
         || state.is(Blocks.GLOW_LICHEN);
   }

   private static boolean isOpenInterior(Level level, BlockPos pos, boolean closing) {
      return isAirLikeInterior(level, pos) ? true : closing && level.hasChunkAt(pos) && level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL);
   }

   private static Optional<PortalFrameScanner.Frame> scanAxis(Level level, BlockPos clicked, Axis axis, boolean closing) {
      Direction positiveDir = axis == Axis.X ? Direction.EAST : Direction.SOUTH;
      Direction negativeDir = positiveDir.getOpposite();
      BlockPos anchor = clicked;
      int steps = 0;

      while (isFrame(level, anchor.below())) {
         anchor = anchor.below();
         if (++steps > 23) {
            return Optional.empty();
         }
      }

      steps = 0;

      while (isFrame(level, anchor.relative(negativeDir))) {
         anchor = anchor.relative(negativeDir);
         if (++steps > 23) {
            return Optional.empty();
         }
      }

      int width = 1;

      while (isFrame(level, anchor.relative(positiveDir, width))) {
         if (++width > 23) {
            return Optional.empty();
         }
      }

      int height = 1;

      while (isFrame(level, anchor.above(height))) {
         if (++height > 23) {
            return Optional.empty();
         }
      }

      if (width >= 3 && height >= 3) {
         List<BlockPos> interior = new ArrayList<>();
         boolean hasPortalBlock = false;

         for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
               BlockPos pos = anchor.relative(positiveDir, w).above(h);
               boolean border = w == 0 || w == width - 1 || h == 0 || h == height - 1;
               if (border) {
                  if (!isFrame(level, pos)) {
                     return Optional.empty();
                  }
               } else {
                  if (!isOpenInterior(level, pos, closing)) {
                     return Optional.empty();
                  }

                  interior.add(pos);
                  if (level.getBlockState(pos).is(ModBlocks.SIFT_PORTAL)) {
                     hasPortalBlock = true;
                  }
               }
            }
         }

         if (closing && !hasPortalBlock) {
            return Optional.empty();
         }

         double middle = (width - 1) * 0.5;
         Vec3 bottomCenter = new Vec3(
            anchor.getX() + 0.5 + positiveDir.getStepX() * middle, anchor.getY() + 1.0, anchor.getZ() + 0.5 + positiveDir.getStepZ() * middle
         );
         return Optional.of(new PortalFrameScanner.Frame(interior, axis, bottomCenter));
      } else {
         return Optional.empty();
      }
   }

   public record Frame(List<BlockPos> interior, Axis axis, Vec3 bottomCenter) {
   }
}
