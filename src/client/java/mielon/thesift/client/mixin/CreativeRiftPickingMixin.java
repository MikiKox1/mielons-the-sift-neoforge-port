package mielon.thesift.client.mixin;

import mielon.thesift.entity.RiftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RiftEntity.class, remap = false)
public abstract class CreativeRiftPickingMixin {
   @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true, remap = false)
   private void theSift$targetRifts(CallbackInfoReturnable<Boolean> info) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && player.isCreative() && minecraft.getCameraEntity() == player
         && ((RiftEntity)(Object)this).isAttackable()) {
         info.setReturnValue(true);
      }
   }
}
