package mielon.thesift.item;

import java.util.function.Consumer;
import mielon.thesift.fluid.IchorRegeneration;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.platform.NeoEvents.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class IchorBottleItem extends Item {
   private static boolean interactionsRegistered;

   public IchorBottleItem(Properties properties) {
      super(properties);
   }

   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
      IchorRegeneration.drink(entity);
      return super.finishUsingItem(stack, level, entity);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
      addIchorTooltip(lines);
   }

   private static void addIchorTooltip(Consumer<Component> lines) {
      lines.accept(Component.translatable("tooltip.the_sift.ichor_bottle.unprotected").withStyle(ChatFormatting.RED));
      lines.accept(Component.translatable("tooltip.the_sift.ichor_bottle.absorption_one").withStyle(ChatFormatting.GRAY));
      lines.accept(Component.translatable("tooltip.the_sift.ichor_bottle.absorption_two").withStyle(ChatFormatting.BLUE));
   }

   public static void registerInteractions() {
      if (!interactionsRegistered) {
         interactionsRegistered = true;
         UseItemCallback.EVENT.register(IchorBottleItem::fillFromSource);
      }
   }

   private static InteractionResult fillFromSource(Player player, Level level, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.is(Items.GLASS_BOTTLE) && !player.isSpectator()) {
         BlockHitResult hit = getPlayerPOVHitResult(level, player, Fluid.SOURCE_ONLY);
         if (hit.getType() != Type.BLOCK) {
            return InteractionResult.PASS;
         }

         BlockPos pos = hit.getBlockPos();
         FluidState fluid = level.getFluidState(pos);
         if (!fluid.isSource() || !fluid.getType().isSame(ModFluids.ICHOR)) {
            return InteractionResult.PASS;
         }

         if (level.mayInteract(player, pos) && player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            if (!level.isClientSide()) {
               ItemStack filled = new ItemStack(ModItems.ICHOR_BOTTLE);
               player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
               player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
               level.playSound(null, pos, ModSounds.ICHOR_BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
               level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   public static InteractionResult fillFromCauldron(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
      int levelValue = state.getValue(LayeredCauldronBlock.LEVEL);
      if (levelValue <= 0) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!level.isClientSide()) {
         ItemStack filled = new ItemStack(ModItems.ICHOR_BOTTLE);
         player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, filled));
         player.awardStat(Stats.USE_CAULDRON);
         player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
         LayeredCauldronBlock.lowerFillLevel(state, level, pos);
         level.playSound(null, pos, ModSounds.ICHOR_BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
      }

      return InteractionResult.SUCCESS;
   }
}
