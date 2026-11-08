package mielon.thesift.mixin;

import mielon.thesift.block.OvergrownWillowVinesBlock;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public abstract class WillowVinesShearsMixin {
   @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
   private void theSift$clipWillow(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof OvergrownWillowVinesBlock && !state.getValue(OvergrownWillowVinesBlock.CLIPPED)) {
         Player player = context.getPlayer();
         ItemStack stack = context.getItemInHand();
         if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
               CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
            }

            BlockState clipped = state.setValue(OvergrownWillowVinesBlock.CLIPPED, true);
            level.setBlockAndUpdate(pos, clipped);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, Context.of(player, clipped));
            if (player != null) {
               stack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }
         }

         level.playSound(player, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
         cir.setReturnValue(InteractionResult.SUCCESS);
      }
   }
}
