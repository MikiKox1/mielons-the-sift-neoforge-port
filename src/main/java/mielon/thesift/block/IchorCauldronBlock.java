package mielon.thesift.block;

import mielon.thesift.block.entity.IchorCauldronBlockEntity;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction.Dispatcher;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.Fluid;

public final class IchorCauldronBlock extends LayeredCauldronBlock implements EntityBlock {
   public static final Dispatcher INTERACTIONS = new Dispatcher();

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new IchorCauldronBlockEntity(pos, state);
   }

   public IchorCauldronBlock(Properties properties) {
      super(Precipitation.NONE, INTERACTIONS, properties);
   }

   @Override
   protected boolean canReceiveStalactiteDrip(Fluid fluid) {
      return fluid == ModFluids.ICHOR || fluid == ModFluids.FLOWING_ICHOR;
   }

   @Override
   protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
      if (this.canReceiveStalactiteDrip(fluid) && !this.isFull(state)) {
         BlockState next = state.setValue(LEVEL, state.getValue(LEVEL) + 1);
         level.setBlockAndUpdate(pos, next);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, Context.of(next));
         level.levelEvent(1047, pos, 0);
      }
   }
}
