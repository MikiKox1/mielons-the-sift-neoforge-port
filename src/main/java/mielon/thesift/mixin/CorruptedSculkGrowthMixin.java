package mielon.thesift.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SculkBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkBlock.class)
public abstract class CorruptedSculkGrowthMixin {
   @Inject(method = "getRandomGrowthState", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$veinsOnly(LevelAccessor level, BlockPos pos, RandomSource random, boolean worldgen, CallbackInfoReturnable<BlockState> cir) {
      if (!worldgen && level instanceof ServerLevel server) {
         Structure lair = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Identifier.fromNamespaceAndPath("the_sift", "sculkers_lair"));
         if (lair != null && server.structureManager().getStructureWithPieceAt(pos, lair).isValid()) {
            cir.setReturnValue(Blocks.SCULK_VEIN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
         }
      }
   }
}
