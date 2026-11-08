package mielon.thesift.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;

public final class MonarchElytraItem extends Item {
   public MonarchElytraItem(Properties properties) {
      super(properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, display, tooltip, flag);
      tooltip.accept(Component.translatable("item.the_sift.monarch_elytra.flight").withStyle(style -> style.withColor(5220233)));
   }
}
