package mielon.thesift.client.mixin;

import mielon.thesift.client.sound.SingerTransitionSound;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingerEntity.class)
public abstract class SingerTransitionSoundMixin {
   @Unique
   private int theSift$soundPhase;
   @Unique
   private boolean theSift$heardPhase;
   @Unique
   private SingerTransitionSound theSift$transitionSound;

   @Inject(method = "tick", at = @At("RETURN"))
   private void theSift$followSingerPhase(CallbackInfo ci) {
      SingerEntity singer = (SingerEntity)(Object)this;
      Minecraft client = Minecraft.getInstance();
      if (client.level == singer.level()) {
         int phase = singer.transitionSoundPhase();
         if (phase != this.theSift$soundPhase) {
            if (this.theSift$transitionSound != null) {
               client.getSoundManager().stop(this.theSift$transitionSound);
            }

            this.theSift$transitionSound = null;
            this.theSift$soundPhase = phase;
            this.theSift$heardPhase = false;
         }

         Entity camera = client.getCameraEntity();
         double radius = 16.0F * singer.transitionSoundVolume();
         if (phase != 0 && !this.theSift$heardPhase && !singer.isSilent() && camera != null && !(camera.distanceToSqr(singer) >= radius * radius)) {
            this.theSift$heardPhase = true;
            this.theSift$transitionSound = new SingerTransitionSound(singer, phase);
            client.getSoundManager().play(this.theSift$transitionSound);
         }
      }
   }
}
