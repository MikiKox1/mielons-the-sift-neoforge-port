package mielon.thesift.block;

import mielon.thesift.entity.MonarchRitual;
import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SculkBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;

public final class RoyalSculkBlock extends SculkBlock {
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

   public RoyalSculkBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(ACTIVE);
   }

   @Override
   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
      super.onPlace(state, level, pos, previous, moving);
      if (level instanceof ServerLevel server && !previous.is(this)) {
         server.scheduleTick(pos, this, 1);
      }
   }

   @Override
   public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      refresh(level, pos);
   }

   @Override
   public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
   }

   public static void refresh(ServerLevel level, BlockPos pos) {
      LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
      if (chunk != null) {
         BlockState state = chunk.getBlockState(pos);
         if (state.is(ModBlocks.ROYAL_SCULK)) {
            for (int x = -1; x <= 1; x++) {
               for (int z = -1; z <= 1; z++) {
                  if (level.getChunkSource().getChunkNow(pos.getX() + x >> 4, pos.getZ() + z >> 4) == null) {
                     level.scheduleTick(pos, state.getBlock(), 20);
                     return;
                  }
               }
            }

            boolean active = MonarchRitual.matchesPedestal(level, pos);
            if (state.getValue(ACTIVE) != active) {
               level.setBlock(pos, state.setValue(ACTIVE, active), 2);
               playTransition(level, pos, active);
            }
         }
      }
   }

   private static void playTransition(ServerLevel level, BlockPos pos, boolean active) {
      level.playSound(
         null,
         pos,
         active ? ModSounds.ROYAL_SCULK_ACTIVATE : ModSounds.ROYAL_SCULK_DEACTIVATE,
         SoundSource.BLOCKS,
         2.0F,
         0.6F + level.getRandom().nextFloat() * 0.4F
      );
   }

   private static boolean ichorSource(BlockState state) {
      FluidState fluid = state.getFluidState();
      return IchorWaterlogging.isIchor(fluid.getType()) && fluid.isSource();
   }

   public static void onBlockChanged(ServerLevel level, BlockPos pos, BlockState previous, BlockState current) {
      if (level.getServer().isSameThread()) {
         boolean oldSource = ichorSource(previous);
         boolean newSource = ichorSource(current);
         if (previous.getBlock() != current.getBlock() || oldSource != newSource) {
            boolean oldRoyal = previous.is(ModBlocks.ROYAL_SCULK);
            boolean royal = current.is(ModBlocks.ROYAL_SCULK);
            boolean totem = previous.is(ModBlocks.SOULSHALE_TOTEM) || current.is(ModBlocks.SOULSHALE_TOTEM);
            boolean base = previous.is(ModBlocks.SOUL_BLOCK) || current.is(ModBlocks.SOUL_BLOCK);
            if (oldRoyal || royal || totem || base || oldSource != newSource) {
               if (oldRoyal && !royal && previous.getValue(ACTIVE)) {
                  playTransition(level, pos, false);
               }

               if (royal) {
                  refresh(level, pos);
               }

               if (totem) {
                  refresh(level, pos.above());
               }

               if (base || oldSource != newSource) {
                  for (int x = -1; x <= 1; x++) {
                     for (int z = -1; z <= 1; z++) {
                        refresh(level, pos.offset(x, 2, z));
                     }
                  }
               }
            }
         }
      }
   }
}
