package mielon.thesift.mixin;

import mielon.thesift.advancement.ModAdvancements;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ItemUsedOnLocationTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemUsedOnLocationTrigger.class)
public abstract class LivingSoilAdvancementMixin {
   @Inject(method = "trigger", at = @At("TAIL"), require = 1)
   private void theSift$plantedSeeds(ServerPlayer player, BlockPos pos, ItemInstance stack, CallbackInfo ci) {
      if ((Object)this == CriteriaTriggers.PLACED_BLOCK) {
         ModAdvancements.onSeedsPlanted(player, pos);
      }
   }
}
