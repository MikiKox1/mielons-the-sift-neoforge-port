package mielon.thesift.client.particle;

import mielon.thesift.client.render.PixelPortalSurface;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class SiftParallaxParticle extends SingleQuadParticle {
   private final Layer layer;

   private SiftParallaxParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
      super(level, x, y, z, xd, yd, zd, sprites.get(level.getRandom()));
      this.quadSize /= 2.0F;
      this.gravity = 1.0F;
      this.friction = 0.98F;
      this.hasPhysics = true;
      this.alpha = 1.0F;
      this.layer = Layer.TRANSLUCENT;
      int color = this.random.nextFloat() < 0.12F ? PixelPortalSurface.RIM_CORE_COLOR : PixelPortalSurface.surfaceColor(this.random.nextFloat());
      this.setColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.alpha = Math.min(1.0F, Math.max(0.0F, (this.lifetime - this.age) * 3.0F / this.lifetime));
   }

   @Override
   public int getLightCoords(float partialTick) {
      return 15728880;
   }

   @Override
   protected Layer getLayer() {
      return this.layer;
   }

   public static final class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public SiftParallaxParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new SiftParallaxParticle(level, x, y, z, xd, yd, zd, this.sprites);
      }
   }
}
