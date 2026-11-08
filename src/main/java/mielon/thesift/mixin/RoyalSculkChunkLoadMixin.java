package mielon.thesift.mixin;

import mielon.thesift.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class RoyalSculkChunkLoadMixin {
   @Inject(method = "setLoaded", at = @At("TAIL"), require = 1)
   private void theSift$restorePedestalState(boolean loaded, CallbackInfo ci) {
      LevelChunk chunk = (LevelChunk)(Object)this;
      if (loaded && chunk.getLevel() instanceof ServerLevel level) {
         LevelChunkSection[] var12 = chunk.getSections();

         for (int index = 0; index < var12.length; index++) {
            LevelChunkSection section = var12[index];
            if (section.maybeHas(state -> state.is(ModBlocks.ROYAL_SCULK))) {
               int minY = level.getMinY() + index * 16;

               for (int x = 0; x < 16; x++) {
                  for (int y = 0; y < 16; y++) {
                     for (int z = 0; z < 16; z++) {
                        if (section.getBlockState(x, y, z).is(ModBlocks.ROYAL_SCULK)) {
                           chunk.getBlockTicks()
                              .schedule(
                                 new ScheduledTick<>(
                                    ModBlocks.ROYAL_SCULK,
                                    new BlockPos(chunk.getPos().getMinBlockX() + x, minY + y, chunk.getPos().getMinBlockZ() + z),
                                    level.getGameTime() + 1L,
                                    TickPriority.NORMAL,
                                    level.nextSubTickCount()
                                 )
                              );
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
