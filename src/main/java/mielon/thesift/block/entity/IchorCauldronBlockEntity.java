package mielon.thesift.block.entity;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IchorCauldronBlockEntity extends BlockEntity {
   public IchorCauldronBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlocks.ICHOR_CAULDRON_BLOCK_ENTITY, pos, state);
   }
}
