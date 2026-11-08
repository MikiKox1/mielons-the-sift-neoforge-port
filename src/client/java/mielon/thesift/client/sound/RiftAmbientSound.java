package mielon.thesift.client.sound;

import mielon.thesift.entity.RiftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public final class RiftAmbientSound extends AbstractTickableSoundInstance {
   private final RiftEntity rift;

   public RiftAmbientSound(RiftEntity rift) {
      super(SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
      this.rift = rift;
      this.looping = true;
      this.delay = 0;
      this.updatePositionAndVolume();
   }

   @Override
   public boolean canStartSilent() {
      return true;
   }

   @Override
   public boolean canPlaySound() {
      return !this.rift.isRemoved() && !this.rift.isSilent();
   }

   @Override
   public void tick() {
      Minecraft client = Minecraft.getInstance();
      Entity camera = client.getCameraEntity();
      if (this.canPlaySound() && client.level == this.rift.level() && camera != null && !(camera.distanceToSqr(this.rift) > 1600.0)) {
         this.updatePositionAndVolume();
      } else {
         this.stop();
      }
   }

   private void updatePositionAndVolume() {
      this.x = this.rift.getX();
      this.y = this.rift.getY() + 2.0;
      this.z = this.rift.getZ();
      this.volume = this.rift.getOpenScale(0.0F);
   }
}
