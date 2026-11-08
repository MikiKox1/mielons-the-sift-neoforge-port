package mielon.thesift.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftBiomeTransition;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Blender.class)
public abstract class SiftBlenderSamplingMixin {
   @Unique
   private boolean theSift$sift;
   @Shadow
   @Final
   private Long2ObjectOpenHashMap<BlendingData> heightAndBiomeBlendingData;

   @Shadow
   private static double heightToOffset(double height) {
      throw new AssertionError();
   }

   @Inject(method = "of", at = @At("RETURN"), require = 1)
   private static void theSift$identifyDimension(WorldGenRegion region, CallbackInfoReturnable<Blender> cir) {
      if (region != null && region.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY) && cir.getReturnValue() != Blender.empty()) {
         ((SiftBlenderSamplingMixin)(Object)cir.getReturnValue()).theSift$sift = true;
      }
   }

   @Redirect(
      method = "blendOffsetAndFactor(II)Lnet/minecraft/world/level/levelgen/blending/Blender$BlendingOutput;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/blending/Blender;heightToOffset(D)D"),
      require = 1
   )
   private double theSift$linearSurfaceOffset(double height) {
      return this.theSift$sift ? (height + 0.5 - 128.0) / 128.0 : heightToOffset(height);
   }

   @Inject(method = "blendDensity", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$resampleDensity(int x, int y, int z, float input, CallbackInfoReturnable<Float> cir) {
      if (this.theSift$sift && (y & 7) != 0) {
         int lower = Math.floorDiv(y, 8) * 8;
         Blender self = (Blender)(Object)this;
         float low = self.blendDensity(x, lower, z, input);
         float high = self.blendDensity(x, lower + 8, z, input);
         cir.setReturnValue(low + (high - low) * (y - lower) / 8.0F);
      }
   }

   @Inject(method = "blendBiome", at = @At("HEAD"), cancellable = true, require = 1)
   private void theSift$organicBiomeBoundary(int x, int y, int z, CallbackInfoReturnable<Holder<Biome>> cir) {
      if (this.theSift$sift) {
         cir.setReturnValue(SiftBiomeTransition.sample(this.heightAndBiomeBlendingData, x, y, z));
      }
   }
}
