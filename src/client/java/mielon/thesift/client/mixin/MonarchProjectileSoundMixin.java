package mielon.thesift.client.mixin;

import mielon.thesift.client.sound.MonarchProjectileSound;
import mielon.thesift.entity.MonarchProjectileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MonarchProjectileSoundMixin {
   @Unique
   private MonarchProjectileSound theSift$projectileSound;

   @Inject(method = "tick", at = @At("TAIL"))
   private void theSift$tickProjectileSound(CallbackInfo ci) {
      if ((Object)this instanceof MonarchProjectileEntity projectile && !projectile.isRemoved() && !projectile.isSilent()) {
         Minecraft client = Minecraft.getInstance();
         if (client.level == projectile.level()) {
            Entity camera = client.getCameraEntity();
            if (camera != null && !(camera.distanceToSqr(projectile) > 256.0)) {
               if (this.theSift$projectileSound == null || projectile.tickCount % 20 == 0) {
                  if (this.theSift$projectileSound == null
                     || this.theSift$projectileSound.isStopped()
                     || !client.getSoundManager().isActive(this.theSift$projectileSound)) {
                     this.theSift$projectileSound = new MonarchProjectileSound(projectile);
                     client.getSoundManager().play(this.theSift$projectileSound);
                  }
               }
            }
         }
      }
   }
}
