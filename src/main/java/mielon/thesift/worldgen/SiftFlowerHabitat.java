package mielon.thesift.worldgen;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;

public final class SiftFlowerHabitat {
   private static final SiftLandmarkTracker.Kind[] KINDS = new SiftLandmarkTracker.Kind[]{
      SiftLandmarkTracker.Kind.FLOWER_BLUE_SAUCER,
      SiftLandmarkTracker.Kind.FLOWER_GREEN_SAUCER,
      SiftLandmarkTracker.Kind.FLOWER_SUNBURST,
      SiftLandmarkTracker.Kind.FLOWER_WHISPERBLOOM,
      SiftLandmarkTracker.Kind.FLOWER_TORCHFLOWER,
      SiftLandmarkTracker.Kind.FLOWER_PITCHERFLOWER,
      SiftLandmarkTracker.Kind.FLOWER_WILDFLOWERS
   };

   private SiftFlowerHabitat() {
   }

   private static Block block(SiftLandmarkTracker.Kind kind) {
      return switch (kind) {
         case FLOWER_BLUE_SAUCER -> ModBlocks.BLUE_SAUCER_BLOOM;
         case FLOWER_GREEN_SAUCER -> ModBlocks.GREEN_SAUCER_BLOOM;
         case FLOWER_SUNBURST -> ModBlocks.SUNBURST_PLANT;
         case FLOWER_WHISPERBLOOM -> ModBlocks.WHISPERBLOOM;
         case FLOWER_TORCHFLOWER -> Blocks.TORCHFLOWER;
         case FLOWER_PITCHERFLOWER -> Blocks.PITCHER_CROP;
         case FLOWER_WILDFLOWERS -> Blocks.WILDFLOWERS;
         default -> Blocks.AIR;
      };
   }

   public static boolean isFlower(BlockState state) {
      for (SiftLandmarkTracker.Kind kind : KINDS) {
         if (state.is(block(kind))) {
            return true;
         }
      }

      return false;
   }

   public static void record(WorldGenLevel level, BlockPos pos, BlockState state) {
      if (level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         record(pos, state);
      }
   }

   private static void record(BlockPos pos, BlockState state) {
      if (!state.hasProperty(DoublePlantBlock.HALF) || state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.UPPER) {
         for (SiftLandmarkTracker.Kind kind : KINDS) {
            if (state.is(block(kind))) {
               SiftLandmarkTracker.record(kind, pos.below());
               return;
            }
         }
      }
   }

   public static Block flowerAt(ServerLevel level, BlockPos soil) {
      if (!level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return null;
      }

      SiftLandmarkTracker.initialize(level.getServer());
      Block result = null;
      double best = Double.POSITIVE_INFINITY;

      for (SiftLandmarkTracker.Kind kind : KINDS) {
         BlockPos site = SiftLandmarkTracker.nearestInVolume(kind, soil, 2, 1);
         if (site != null) {
            double distance = site.distSqr(soil);
            if (distance < best) {
               best = distance;
               result = block(kind);
            }
         }
      }

      return result;
   }

   public static void survey(LevelChunk chunk) {
      if (chunk.getLevel() instanceof ServerLevel level) {
         boolean sift = level.dimension().equals(TheSiftDimension.LEVEL_KEY);
         if (sift) {
            SiftLandmarkTracker.initialize(level.getServer());
         }

         BlockPos mark = new BlockPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ());
         boolean flowers = sift && !SiftLandmarkTracker.hasRecorded(SiftLandmarkTracker.Kind.FLOWER_SURVEYED_CHUNK, mark);
         LevelChunkSection[] sections = chunk.getSections();

         for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.maybeHas(s -> s.is(ModBlocks.ICHOR_CAULDRON) || flowers && isFlower(s))) {
               int minY = chunk.getSectionYFromSectionIndex(sectionIndex) * 16;

               for (int x = 0; x < 16; x++) {
                  for (int z = 0; z < 16; z++) {
                     for (int y = 0; y < 16; y++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.is(ModBlocks.ICHOR_CAULDRON) || flowers && isFlower(state)) {
                           BlockPos pos = new BlockPos(mark.getX() + x, minY + y, mark.getZ() + z);
                           if (state.is(ModBlocks.ICHOR_CAULDRON)) {
                              chunk.getBlockEntity(pos, EntityCreationType.IMMEDIATE);
                           } else {
                              record(pos, state);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (flowers) {
            SiftLandmarkTracker.record(SiftLandmarkTracker.Kind.FLOWER_SURVEYED_CHUNK, mark);
         }
      }
   }
}
