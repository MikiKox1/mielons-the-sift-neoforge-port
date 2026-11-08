package mielon.thesift.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;

public final class SiftIchorCaveSpringFeature extends SpringFeature {
   private static final Direction[] POSSIBLE_OPENINGS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};

   public SiftIchorCaveSpringFeature() {
      super(SpringConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<SpringConfiguration> context) {
      WorldGenLevel level = context.level();
      BlockPos origin = context.origin();
      if (origin.getY() >= 14 && origin.getY() <= 30) {
         for (Direction direction : POSSIBLE_OPENINGS) {
            BlockPos opening = origin.relative(direction);
            if (level.isEmptyBlock(opening)) {
               int surfaceY = level.getHeight(Types.WORLD_SURFACE_WG, opening.getX(), opening.getZ());
               if (opening.getY() >= surfaceY - 1) {
                  return false;
               }
            }
         }

         return super.place(context);
      } else {
         return false;
      }
   }
}
