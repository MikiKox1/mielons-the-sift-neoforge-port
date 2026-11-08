package mielon.thesift.client.particle;

import mielon.thesift.fluid.IchorSoulMotion;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.FacingCameraMode;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class SoulParticle extends SingleQuadParticle {
   private final boolean canyonSoul;
   private final float startSize;
   private final double swayPhase;
   private final double startY;
   private final FacingCameraMode facingMode;

   private SoulParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean canyonSoul) {
      super(level, x, y, z, xd, yd, zd, sprites.get(level.getRandom()));
      this.canyonSoul = canyonSoul;
      this.swayPhase = this.random.nextDouble() * Math.PI * 2.0;
      this.facingMode = canyonSoul ? FacingCameraMode.LOOKAT_Y : FacingCameraMode.LOOKAT_XYZ;
      this.startY = y;
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = canyonSoul ? 0.995F : 0.94F;
      this.alpha = canyonSoul ? 0.86F : 0.92F;
      if (canyonSoul) {
         this.xd = (this.random.nextDouble() - 0.5) * 0.018;
         this.yd = 0.18 + this.random.nextDouble() * 0.08;
         this.zd = (this.random.nextDouble() - 0.5) * 0.018;
         this.startSize = 0.92F + this.random.nextFloat() * 0.62F;
         this.lifetime = 90;
      } else {
         this.xd = xd + (this.random.nextDouble() - 0.5) * 0.055;
         this.yd = Math.abs(yd) + 0.035 + this.random.nextDouble() * 0.065;
         this.zd = zd + (this.random.nextDouble() - 0.5) * 0.055;
         this.startSize = 0.075F + this.random.nextFloat() * 0.095F;
         this.lifetime = 18 + this.random.nextInt(15);
      }

      this.quadSize = this.startSize;
      this.setSize(this.startSize, this.startSize);
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.canyonSoul) {
         double sway = Math.sin(this.swayPhase + this.age * 0.22) * 0.007;
         this.move(this.xd + sway, this.yd, this.zd - sway * 0.65);
         this.yd = Math.min(0.31, this.yd + 0.0016);
         double rise = this.y - this.startY;
         this.alpha = IchorSoulMotion.canyonAlpha(rise);
         if (rise >= 16.0) {
            this.remove();
            return;
         }

         this.quadSize = this.startSize * (1.0F + Math.min(0.28F, this.age / 180.0F));
      } else {
         this.move(this.xd, this.yd, this.zd);
         this.xd = this.xd * this.friction;
         this.yd *= 0.965;
         this.zd = this.zd * this.friction;
         this.alpha = 0.92F * Math.max(0.0F, (this.lifetime - this.age) / 9.0F);
      }

      this.age++;
      if (this.age >= this.lifetime || this.alpha <= 0.01F) {
         this.remove();
      }
   }

   @Override
   public int getLightCoords(float partialTickTime) {
      return 15728880;
   }

   @Override
   public FacingCameraMode getFacingCameraMode() {
      return this.facingMode;
   }

   @Override
   protected Layer getLayer() {
      return Layer.bySprite(this.sprite);
   }

   public static final class FragmentProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public FragmentProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SoulParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new SoulParticle(level, x, y, z, xd, yd, zd, this.sprites, false);
      }
   }

   public static final class RiseProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public RiseProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SoulParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new SoulParticle(level, x, y, z, xd, yd, zd, this.sprites, true);
      }
   }
}
