package mielon.thesift.entity;

import mielon.thesift.block.ModBlocks;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class EchoGolemSummoning {
   private EchoGolemSummoning() {
   }

   public static boolean hasBase(LevelReader level, BlockPos pumpkinPos) {
      return level.getBlockState(pumpkinPos.below()).is(ModBlocks.POLISHED_SOULSHALE);
   }

   public static boolean trySpawn(Level level, BlockPos pumpkinPos) {
      if (level instanceof ServerLevel serverLevel && hasBase(level, pumpkinPos)) {
         BlockState pumpkin = level.getBlockState(pumpkinPos);
         if (!pumpkin.is(Blocks.CARVED_PUMPKIN) && !pumpkin.is(Blocks.JACK_O_LANTERN)) {
            return false;
         }

         BlockPos basePos = pumpkinPos.below();
         BlockState base = level.getBlockState(basePos);
         EchoGolemEntity golem = new EchoGolemEntity(ModEntities.ECHO_GOLEM, level);
         golem.snapTo(basePos.getX() + 0.5, basePos.getY() + 0.05, basePos.getZ() + 0.5, 0.0F, 0.0F);
         golem.setPersistenceRequired();
         if (!serverLevel.addFreshEntity(golem)) {
            return true;
         }

         level.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), 2);
         level.setBlock(basePos, Blocks.AIR.defaultBlockState(), 2);
         level.levelEvent(2001, pumpkinPos, Block.getId(pumpkin));
         level.levelEvent(2001, basePos, Block.getId(base));

         for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
         }

         level.updateNeighborsAt(pumpkinPos, Blocks.AIR);
         level.updateNeighborsAt(basePos, Blocks.AIR);
         return true;
      } else {
         return false;
      }
   }
}
