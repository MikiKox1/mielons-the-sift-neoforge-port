package mielon.thesift.client.mixin;

import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class SiftMusicManagerMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private int nextSongDelay;
   @Unique
   private boolean theSift$wasInSift;

   @Shadow
   public abstract void stopPlaying();

   @Inject(method = "tick", at = @At("HEAD"))
   private void theSift$changeDimensionMusic(CallbackInfo ci) {
      boolean inSift = this.minecraft.level != null && this.minecraft.level.dimension().equals(TheSiftDimension.LEVEL_KEY);
      if (inSift != this.theSift$wasInSift) {
         this.theSift$wasInSift = inSift;
         this.stopPlaying();
         this.nextSongDelay = 100;
      }
   }
}
