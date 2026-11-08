package mielon.thesift.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mielon.thesift.entity.ScavengerEntity;
import mielon.thesift.fluid.IchorFluid;
import mielon.thesift.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class IchorEntitySplashMixin {
   @Unique
   private boolean theSift$ichorSplash;
   @Unique
   private double theSift$splashSurface;

   @Inject(method = "doWaterSplashEffect", at = @At("HEAD"), require = 1)
   private void theSift$findSplashFluid(CallbackInfo ci) {
      Entity entity = (Entity)(Object)this;
      Level level = entity.level();
      AABB box = entity.getBoundingBox().deflate(0.001);
      this.theSift$ichorSplash = false;
      this.theSift$splashSurface = entity.getY();

      for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.05, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
         if (level.hasChunkAt(pos)) {
            FluidState fluid = level.getFluidState(pos);
            double surface = pos.getY() + fluid.getHeight(level, pos);
            if (fluid.getType() instanceof IchorFluid && surface > box.minY - 0.05) {
               this.theSift$ichorSplash = true;
               this.theSift$splashSurface = Math.max(this.theSift$splashSurface, surface);
            }
         }
      }
   }

   @WrapOperation(
      method = "doWaterSplashEffect",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
      require = 2
   )
   private void theSift$fluidSplash(
      Level level, ParticleOptions particle, double x, double y, double z, double xd, double yd, double zd, Operation<Void> original
   ) {
      Entity entity = (Entity)(Object)this;
      if (this.theSift$ichorSplash) {
         boolean bubble = particle.getType() == ParticleTypes.BUBBLE;
         if (bubble || particle.getType() == ParticleTypes.SPLASH) {
            particle = bubble ? ModParticles.ICHOR_BUBBLE : ModParticles.ICHOR_SPLASH;
            y = this.theSift$splashSurface + (bubble ? -0.04 : 0.03);
         }
      }

      if (entity instanceof ScavengerEntity) {
         x = entity.getX() + (x - entity.getX()) * 0.4;
         z = entity.getZ() + (z - entity.getZ()) * 0.4;
         xd *= 0.35;
         zd *= 0.35;
      }

      original.call(level, particle, x, y, z, xd, yd, zd);
   }
}
