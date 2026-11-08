package mielon.thesift.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public final class IchorSnowballItem extends SnowballItem {
   public IchorSnowballItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      ItemStack cooldownStack = stack.copy();
      if (player.getCooldowns().isOnCooldown(cooldownStack)) {
         return InteractionResult.FAIL;
      }

      InteractionResult result = super.use(level, player, hand);
      player.getCooldowns().addCooldown(cooldownStack, 10);
      return result;
   }
}
