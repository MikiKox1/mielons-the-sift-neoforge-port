package mielon.thesift.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mielon.thesift.world.SiftTeleportManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SiftPortalMovementMixin {
   @Shadow
   public ServerPlayer player;

   @WrapMethod(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V")
   private void theSift$recordPortalCrossing(ServerboundMovePlayerPacket packet, Operation<Void> original) {
      ServerLevel level = this.player.level();
      if (!level.getServer().isSameThread()) {
         original.call(new Object[]{packet});
      } else {
         Vec3 from = this.player.position();
         original.call(new Object[]{packet});
         if (this.player.level() == level) {
            SiftTeleportManager.onPlayerMovement(this.player, from);
         }
      }
   }
}
