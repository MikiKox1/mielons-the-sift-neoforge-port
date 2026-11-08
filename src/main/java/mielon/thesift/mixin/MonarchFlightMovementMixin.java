package mielon.thesift.mixin;

import mielon.thesift.item.MonarchFlightAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class MonarchFlightMovementMixin {
   @ModifyArg(
      method = "travelFallFlying",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
      ),
      index = 1
   )
   private Vec3 theSift$lift(Vec3 vanilla) {
      return (Object)this instanceof Player player ? ((MonarchFlightAccess)player).theSift$monarchFlight().movement(player, vanilla) : vanilla;
   }
}
