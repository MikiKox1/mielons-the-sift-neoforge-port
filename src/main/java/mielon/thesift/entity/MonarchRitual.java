package mielon.thesift.entity;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.fluid.IchorWaterlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MonarchRitual {
   private MonarchRitual() {
   }

   public static boolean matches(ServerLevel level, BlockPos flower) {
      return level.getBlockState(flower).is(ModBlocks.SCULKFLOWER) && matchesPedestal(level, flower.below());
   }

   public static boolean matchesPedestal(ServerLevel level, BlockPos royal) {
      BlockPos base = royal.below(2);
      if (level.getBlockState(royal).is(ModBlocks.ROYAL_SCULK)
         && level.getBlockState(royal.below()).is(ModBlocks.SOULSHALE_TOTEM)
         && level.getBlockState(base).is(ModBlocks.SOUL_BLOCK)) {
         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || z != 0) {
                  BlockPos pos = base.offset(x, 0, z);
                  if (!level.hasChunkAt(pos) || !IchorWaterlogging.isIchor(level.getFluidState(pos).getType()) || !level.getFluidState(pos).isSource()) {
                     return false;
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean trySummon(ServerLevel level, BlockPos flower) {
      if (level.getDifficulty() != Difficulty.PEACEFUL && matches(level, flower)) {
         MonarchEntity monarch = ModEntities.MONARCH.create(level, EntitySpawnReason.MOB_SUMMONED);
         if (monarch == null) {
            return false;
         }

         BlockPos base = flower.below(3);
         monarch.setPos(base.getX() + 0.5, base.getY() + 0.05, base.getZ() + 0.5);
         monarch.setTarget(level.getNearestPlayer(monarch, 64.0));
         if (!level.addFreshEntity(monarch)) {
            return false;
         }

         ModAdvancements.awardNearby(level, monarch.position(), 16.0, "the_sift/thats_the_guy_from_the_painting");

         for (int y = 3; y >= 0; y--) {
            BlockPos pos = base.above(y);
            level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
         }

         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || z != 0) {
                  BlockPos pos = base.offset(x, 0, z);
                  BlockState state = level.getBlockState(pos);
                  level.setBlock(
                     pos, IchorWaterlogging.isIchorlogged(state) ? IchorWaterlogging.setFluidlogged(state, false) : Blocks.AIR.defaultBlockState(), 3
                  );
                  level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.025);
               }
            }
         }

         level.sendParticles(ParticleTypes.SCULK_SOUL, monarch.getX(), monarch.getY() + 1.5, monarch.getZ(), 100, 1.4, 1.5, 1.4, 0.15);
         return true;
      } else {
         return false;
      }
   }
}
