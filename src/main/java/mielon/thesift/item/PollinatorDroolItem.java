package mielon.thesift.item;

import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.PollinatorProjectileEntity;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ProjectileItem.DispenseConfig;
import net.minecraft.world.level.Level;

public final class PollinatorDroolItem extends Item implements ProjectileItem {
   public PollinatorDroolItem(Properties properties) {
      super(properties);
   }

   @Override
   public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction facing) {
      PollinatorProjectileEntity projectile = new PollinatorProjectileEntity(ModEntities.POLLINATOR_PROJECTILE, level);
      projectile.setPlayerThrown();
      projectile.setPos(position.x(), position.y(), position.z());
      return projectile;
   }

   @Override
   public DispenseConfig createDispenseConfig() {
      return DispenseConfig.builder().power(0.8F).build();
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(stack)) {
         return InteractionResult.FAIL;
      }

      player.getCooldowns().addCooldown(stack, 10);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.POLLINATOR_DROOL_FIRE, SoundSource.PLAYERS, 0.7F, 1.0F);
      if (level instanceof ServerLevel server) {
         PollinatorProjectileEntity projectile = new PollinatorProjectileEntity(ModEntities.POLLINATOR_PROJECTILE, server);
         projectile.setOwner(player);
         projectile.setPlayerThrown();
         projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
         projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.8F, 0.0F);
         server.addFreshEntity(projectile);
         player.awardStat(Stats.ITEM_USED.get(this));
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }
      }

      return InteractionResult.SUCCESS;
   }
}
