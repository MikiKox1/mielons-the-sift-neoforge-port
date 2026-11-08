package mielon.thesift.item;

import mielon.thesift.world.RiftManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public final class SiftRiftItem extends Item {
   public SiftRiftItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && player.getAbilities().instabuild) {
         if (context.getLevel() instanceof ServerLevel level) {
            BlockPos var6 = context.getClickedPos().relative(context.getClickedFace());
            boolean longAlongX = RiftManager.axisFromFacing(context.getHorizontalDirection());
            if (RiftManager.tryPlaceCreativeRift(level, var6, longAlongX)) {
               return InteractionResult.SUCCESS_SERVER;
            }

            player.sendOverlayMessage(Component.translatable("message.the_sift.rift_no_space"));
            return InteractionResult.FAIL;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.FAIL;
      }
   }
}
