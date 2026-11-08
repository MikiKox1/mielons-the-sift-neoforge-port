package mielon.thesift.client.mixin;

import java.util.List;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.client.render.WillowFoliageTint;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public abstract class WillowFoliageColorsMixin {
   @Inject(method = "createDefault", at = @At("RETURN"))
   private static void theSift$northSouthFoliage(CallbackInfoReturnable<BlockColors> cir) {
      cir.getReturnValue()
         .register(List.of(BlockTintSources.constant(-1), new WillowFoliageTint()), ModBlocks.OVERGROWN_WILLOW_FOLIAGE, ModBlocks.ASH_WILLOW_FOLIAGE);
   }
}
