package mielon.thesift.fluid;

import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jspecify.annotations.Nullable;

public final class IchorBucketItem extends BucketItem {
   public IchorBucketItem(Fluid fluid, Properties properties) {
      super(fluid, properties);
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BlockHitResult hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.NONE);
      if (hit.getType() == Type.BLOCK) {
         BlockPos pos = hit.getBlockPos();
         BlockState state = level.getBlockState(pos);
         if (IchorWaterlogging.canFill(state)
            && level.mayInteract(player, pos)
            && player.mayUseItemAt(pos, hit.getDirection(), stack)
            && this.emptyContents(player, level, pos, hit)) {
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS.heldItemTransformedTo(BucketItem.getEmptySuccessItem(stack, player));
         }
      }

      return super.use(level, player, hand);
   }

   @Override
   @Deprecated
   public boolean emptyContents(@Nullable LivingEntity user, Level level, BlockPos pos, @Nullable BlockHitResult hitResult) {
      return this.emptyContents(user, level, pos, hitResult, null);
   }

   @Override
   public boolean emptyContents(
      @Nullable LivingEntity user, Level level, BlockPos pos, @Nullable BlockHitResult hitResult, @Nullable ItemStack containerItem
   ) {
      BlockState state = level.getBlockState(pos);
      if (IchorWaterlogging.fill(level, pos, state)) {
         this.playEmptySound(user, level, pos);
         return true;
      } else {
         return super.emptyContents(user, level, pos, hitResult, containerItem);
      }
   }

   @Override
   protected void playEmptySound(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos) {
      level.playSound(null, pos, ModSounds.ICHOR_BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
   }
}
