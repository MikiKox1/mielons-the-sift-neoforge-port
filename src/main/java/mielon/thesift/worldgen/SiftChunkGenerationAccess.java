package mielon.thesift.worldgen;

import net.minecraft.world.level.levelgen.blending.BlendingData;

public interface SiftChunkGenerationAccess {
   int theSift$generation();

   void theSift$generation(int var1);

   double[] theSift$edgeProfile();

   void theSift$edgeProfile(double[] var1);

   int theSift$surfaceRevision();

   void theSift$surfaceRevision(int var1);

   void theSift$legacyBlending(BlendingData var1);
}
