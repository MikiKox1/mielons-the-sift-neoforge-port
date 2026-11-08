package mielon.thesift.client.mixin;

import mielon.thesift.item.MonarchFlightAccess;
import mielon.thesift.network.MonarchFlightPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MonarchFlightInputMixin {
   @Unique
   private boolean theSift$jumpHeld;
   @Unique
   private boolean theSift$jumpWasDown;
   @Unique
   private boolean theSift$pressedDuringFlight;

   @Inject(method = "tick", at = @At("HEAD"))
   private void theSift$readJump(CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      boolean down = client.screen == null && client.options.keyJump.isDown();
      LocalPlayer player = (LocalPlayer)(Object)this;
      if (!down) {
         this.theSift$pressedDuringFlight = false;
      } else if (!this.theSift$jumpWasDown) {
         this.theSift$pressedDuringFlight = player.isFallFlying();
      }

      this.theSift$jumpWasDown = down;
      boolean held = down && this.theSift$pressedDuringFlight;
      ((MonarchFlightAccess)this).theSift$monarchFlight().updateInput(player, held);
      if (held != this.theSift$jumpHeld) {
         this.theSift$jumpHeld = held;
         if (client.getConnection() != null && client.getConnection().hasChannel(MonarchFlightPayload.TYPE)) {
            ClientPacketDistributor.sendToServer(new MonarchFlightPayload(held));
         }
      }
   }
}
