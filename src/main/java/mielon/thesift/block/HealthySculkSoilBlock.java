package mielon.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class HealthySculkSoilBlock extends SiftBonemealBlock {
   public HealthySculkSoilBlock(Properties properties) {
      super(properties);
   }

   @Override
   public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
      super.fallOn(level, state, pos, entity, fallDistance);
      if (!level.isClientSide()
         && entity instanceof Player
         && fallDistance > 0.5
         && (
            level.getBlockState(pos.above()).getBlock() instanceof CropBlock
               || level.getBlockState(pos.above()).getBlock() instanceof StemBlock
               || level.getBlockState(pos.above()).getBlock() instanceof AttachedStemBlock
               || level.getBlockState(pos.above()).getBlock() instanceof PitcherCropBlock
               || level.getBlockState(pos.above()).getBlock() instanceof SculkflowerBlock
         )) {
         level.destroyBlock(pos.above(), true, entity);
      }
   }
}
