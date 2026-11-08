package mielon.thesift.mixin;

import java.util.Optional;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.blending.BlendingData.BiomeConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlendingData.class)
public interface SiftBlendingDataInvoker {
   @Invoker("<init>")
   static BlendingData theSift$create(int minSection, int maxSection, Optional<float[]> heights) {
      throw new AssertionError();
   }

   @Invoker("iterateBiomes")
   void theSift$biomes(int var1, int var2, int var3, BiomeConsumer var4);
}
