package mielon.thesift.mixin;

import mielon.thesift.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public abstract class SculkflowerChickenMixin {
   @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
   private void theSift$poisonChicken(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      Animal chicken = (Animal)(Object)this;
      ItemStack stack = player.getItemInHand(hand);
      if (chicken.getType() == EntityTypes.CHICKEN && stack.is(ModItems.SCULKFLOWER_SEEDS)) {
         if (chicken.level() instanceof ServerLevel serverLevel) {
            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }

            chicken.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
            if (player.isCreative() || !chicken.isInvulnerable()) {
               chicken.hurtServer(serverLevel, chicken.damageSources().playerAttack(player), Float.MAX_VALUE);
            }
         }

         cir.setReturnValue(InteractionResult.SUCCESS);
      }
   }
}
