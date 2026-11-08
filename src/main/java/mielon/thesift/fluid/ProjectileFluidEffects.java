package mielon.thesift.fluid;

import mielon.thesift.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ProjectileFluidEffects {
   public static ParticleOptions trailParticle(FluidState fluid) {
      if (fluid.getType() instanceof IchorFluid) {
         return ModParticles.ICHOR_BUBBLE;
      } else if (fluid.is(FluidTags.WATER)) {
         return ParticleTypes.BUBBLE;
      } else {
         return fluid.is(FluidTags.LAVA) ? ParticleTypes.LAVA : null;
      }
   }

   public static boolean isTouchingIchor(Entity entity) {
      AABB box = entity.getBoundingBox().deflate(0.001);
      MutableBlockPos pos = new MutableBlockPos();

      for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX); x++) {
         for (int y = Mth.floor(box.minY); y <= Mth.floor(box.maxY); y++) {
            for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ); z++) {
               pos.set(x, y, z);
               if (entity.level().hasChunkAt(pos)) {
                  FluidState fluid = entity.level().getFluidState(pos);
                  if (fluid.getType() instanceof IchorFluid && box.minY < y + fluid.getHeight(entity.level(), pos)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public static void spawnTrail(Entity projectile, Vec3 velocity) {
      if (projectile.level().isClientSide() && !(velocity.lengthSqr() < 1.0E-7)) {
         for (int i = 0; i < 4; i++) {
            Vec3 sample = projectile.position().subtract(velocity.scale(i * 0.25));
            BlockPos pos = BlockPos.containing(sample);
            FluidState fluid = projectile.level().getFluidState(pos);
            if (!fluid.isEmpty() && !(sample.y >= pos.getY() + fluid.getHeight(projectile.level(), pos))) {
               ParticleOptions particle = trailParticle(fluid);
               if (particle != null) {
                  if (particle == ParticleTypes.LAVA) {
                     if (i == 0 && (projectile.tickCount & 3) == 0) {
                        projectile.level().addParticle(particle, sample.x, sample.y, sample.z, 0.0, 0.0, 0.0);
                     }
                  } else {
                     projectile.level().addParticle(particle, sample.x, sample.y, sample.z, velocity.x, velocity.y, velocity.z);
                  }
               }
            }
         }
      }
   }

   private ProjectileFluidEffects() {
   }
}
