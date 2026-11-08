package mielon.thesift.client.particle;

import java.util.Comparator;
import mielon.thesift.particle.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.phys.AABB;

public final class PollinatorDroolParticle extends SingleQuadParticle {
   private final SpriteSet sprites;
   private final AreaEffectCloud cloud;
   private final int fadeTicks;
   private boolean cloudExpired;
   private boolean hasHitGround;

   private PollinatorDroolParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z, xd, yd, zd, sprites.get(level.getRandom()));
      this.sprites = sprites;
      this.friction = 0.96F;
      this.xd = xd;
      this.yd = yd;
      this.zd = zd;
      this.rCol = Mth.nextFloat(this.random, 0.88F, 1.0F);
      this.gCol = Mth.nextFloat(this.random, 0.72F, 0.9F);
      this.bCol = Mth.nextFloat(this.random, 0.06F, 0.16F);
      this.quadSize *= 0.75F;
      this.lifetime = (int)(20.0 / (this.random.nextFloat() * 0.8 + 0.2));
      this.fadeTicks = 10 + this.random.nextInt(7);
      this.hasPhysics = false;
      this.setSpriteFromAge(sprites);
      this.cloud = level.getEntitiesOfClass(
            AreaEffectCloud.class,
            new AABB(x - 0.1, y - 0.1, z - 0.1, x + 0.1, y + 0.1, z + 0.1),
            candidate -> candidate.getParticle().getType() == ModParticles.POLLINATOR_DROOL
         )
         .stream()
         .min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(x, y, z)))
         .orElse(null);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (!this.cloudExpired && this.cloud != null && this.cloud.isRemoved()) {
         this.cloudExpired = true;
         this.lifetime = Math.min(this.lifetime, this.age + this.fadeTicks);
      }

      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float fade = Mth.clamp((float)(this.lifetime - this.age) / this.fadeTicks, 0.0F, 1.0F);
         this.alpha = fade * fade * (3.0F - 2.0F * fade);
         this.setSpriteFromAge(this.sprites);
         if (this.onGround) {
            this.yd = 0.0;
            this.hasHitGround = true;
         }

         if (this.hasHitGround) {
            this.yd += 0.002;
         }

         this.move(this.xd, this.yd, this.zd);
         if (this.y == this.yo) {
            this.xd *= 1.1;
            this.zd *= 1.1;
         }

         this.xd = this.xd * this.friction;
         this.zd = this.zd * this.friction;
         if (this.hasHitGround) {
            this.yd = this.yd * this.friction;
         }
      }
   }

   @Override
   public float getQuadSize(float partialTick) {
      return this.quadSize * Mth.clamp((this.age + partialTick) / this.lifetime * 32.0F, 0.0F, 1.0F);
   }

   @Override
   protected Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public PollinatorDroolParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new PollinatorDroolParticle(level, x, y, z, xd, yd, zd, this.sprites);
      }
   }
}
