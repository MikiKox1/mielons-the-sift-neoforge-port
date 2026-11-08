package mielon.thesift.mixin;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.fluid.IchorRegeneration;
import net.minecraft.advancements.triggers.UsedTotemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UsedTotemTrigger.class)
public abstract class SiftTotemAdvancementMixin {
   @Inject(method = "trigger", at = @At("TAIL"), require = 1)
   private void theSift$usedTotem(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
      if (IchorRegeneration.isTouching(player)) {
         ModAdvancements.award(player, "the_sift/stronger_than_gods");
      }
   }
}
