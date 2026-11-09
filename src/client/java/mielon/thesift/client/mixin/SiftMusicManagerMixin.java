package mielon.thesift.client.mixin;

import net.minecraft.client.player.LocalPlayer;

import net.minecraft.client.multiplayer.ClientLevel;

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
      ClientLevel level = this.minecraft.level;
      LocalPlayer player = this.minecraft.player;
      if (level == null || player != null && player.level() == level) {
         if (level != null || player == null) {
            boolean inSift = level != null && level.dimension().equals(TheSiftDimension.LEVEL_KEY);
            if (inSift != this.theSift$wasInSift) {
               this.theSift$wasInSift = inSift;
               this.stopPlaying();
               this.nextSongDelay = 100;
            }
         }
      }
   }
}
