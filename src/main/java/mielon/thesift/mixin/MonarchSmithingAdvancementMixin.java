package mielon.thesift.mixin;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class MonarchSmithingAdvancementMixin {
   @Inject(method = "onTake", at = @At("TAIL"), require = 1)
   private void theSift$crafted(Player player, ItemStack stack, CallbackInfo ci) {
      if (player instanceof ServerPlayer serverPlayer && stack.is(ModItems.MONARCH_ELYTRA)) {
         ModAdvancements.award(serverPlayer, "the_sift/skys_no_longer_a_limit");
      }
   }
}
