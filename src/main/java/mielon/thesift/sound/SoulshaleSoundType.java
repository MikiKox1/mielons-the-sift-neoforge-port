package mielon.thesift.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

public final class SoulshaleSoundType extends SoundType {
   public static final SoulshaleSoundType INSTANCE = new SoulshaleSoundType();

   private SoulshaleSoundType() {
      super(
         1.0F,
         1.0F,
         SoundType.DEEPSLATE.getBreakSound(),
         SoundType.DEEPSLATE.getStepSound(),
         SoundType.DEEPSLATE.getPlaceSound(),
         SoundType.DEEPSLATE.getHitSound(),
         SoundType.DEEPSLATE.getFallSound()
      );
   }

   @Override
   public SoundEvent getBreakSound() {
      return ModSounds.SOULSHALE_BREAK;
   }

   @Override
   public SoundEvent getStepSound() {
      return ModSounds.SOULSHALE_STEP;
   }

   @Override
   public SoundEvent getPlaceSound() {
      return ModSounds.SOULSHALE_PLACE;
   }

   @Override
   public SoundEvent getHitSound() {
      return ModSounds.SOULSHALE_HIT;
   }

   @Override
   public SoundEvent getFallSound() {
      return ModSounds.SOULSHALE_STEP;
   }
}
