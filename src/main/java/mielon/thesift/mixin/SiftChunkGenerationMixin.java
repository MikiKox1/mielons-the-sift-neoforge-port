package mielon.thesift.mixin;

import mielon.thesift.worldgen.SiftChunkGenerationAccess;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public abstract class SiftChunkGenerationMixin implements SiftChunkGenerationAccess {
   @Unique
   private volatile int theSift$generation = 2;
   @Unique
   private volatile double[] theSift$edgeProfile;
   @Unique
   private int theSift$surfaceRevision;
   @Shadow
   @Final
   @Mutable
   protected BlendingData blendingData;

   @Override
   public double[] theSift$edgeProfile() {
      return this.theSift$edgeProfile;
   }

   @Override
   public void theSift$edgeProfile(double[] value) {
      this.theSift$edgeProfile = value;
   }

   @Override
   public int theSift$surfaceRevision() {
      return this.theSift$surfaceRevision;
   }

   @Override
   public void theSift$surfaceRevision(int value) {
      this.theSift$surfaceRevision = value;
   }

   @Override
   public int theSift$generation() {
      return this.theSift$generation;
   }

   @Override
   public void theSift$generation(int value) {
      this.theSift$generation = value;
   }

   @Override
   public void theSift$legacyBlending(BlendingData data) {
      this.blendingData = data;
   }
}
