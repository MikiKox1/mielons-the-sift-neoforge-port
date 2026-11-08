package mielon.thesift.client.sound;

import mielon.thesift.entity.SingerEntity;
import mielon.thesift.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class SingerTransitionSound extends AbstractTickableSoundInstance {
   private final SingerEntity singer;
   private final int phase;

   public SingerTransitionSound(SingerEntity singer, int phase) {
      super(phase == 1 ? ModSounds.SINGER_EMERGE : ModSounds.SINGER_DIG, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
      this.singer = singer;
      this.phase = phase;
      this.volume = singer.transitionSoundVolume();
      this.updatePosition();
   }

   @Override
   public boolean canPlaySound() {
      return !this.singer.isRemoved() && !this.singer.isSilent() && this.singer.transitionSoundPhase() == this.phase;
   }

   @Override
   public void tick() {
      if (this.canPlaySound() && Minecraft.getInstance().level == this.singer.level()) {
         this.updatePosition();
      } else {
         this.stop();
      }
   }

   private void updatePosition() {
      this.x = this.singer.getX();
      this.y = this.singer.getY();
      this.z = this.singer.getZ();
   }
}
