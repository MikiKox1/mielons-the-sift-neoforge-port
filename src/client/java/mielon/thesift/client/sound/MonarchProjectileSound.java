package mielon.thesift.client.sound;

import mielon.thesift.entity.MonarchProjectileEntity;
import mielon.thesift.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class MonarchProjectileSound extends AbstractTickableSoundInstance {
   private final MonarchProjectileEntity projectile;

   public MonarchProjectileSound(MonarchProjectileEntity projectile) {
      super(ModSounds.MONARCH_PROJECTILE_LOOP, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
      this.projectile = projectile;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.3F;
      this.pitch = 0.75F;
      this.updatePosition();
   }

   @Override
   public boolean canPlaySound() {
      return !this.projectile.isRemoved() && !this.projectile.isSilent();
   }

   @Override
   public void tick() {
      Minecraft client = Minecraft.getInstance();
      Entity camera = client.getCameraEntity();
      if (this.canPlaySound() && client.level == this.projectile.level() && camera != null && !(camera.distanceToSqr(this.projectile) > 400.0)) {
         this.updatePosition();
      } else {
         this.stop();
      }
   }

   private void updatePosition() {
      this.x = this.projectile.getX();
      this.y = this.projectile.getY();
      this.z = this.projectile.getZ();
   }
}
