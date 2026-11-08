package mielon.thesift.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

public final class IchorSurfaceParticle extends SingleQuadParticle {
   private final SpriteSet sprites;
   private final float surfaceYaw;

   private IchorSurfaceParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
      super(level, x, y, z, sprites.get(level.getRandom()));
      this.sprites = sprites;
      this.surfaceYaw = this.random.nextFloat() * (float) (Math.PI * 2);
      this.quadSize = 0.12F + this.random.nextFloat() * 0.12F;
      this.lifetime = 40 + this.random.nextInt(31);
      this.hasPhysics = false;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.xd = 0.0;
      this.yd = 0.0;
      this.zd = 0.0;
      this.setAlpha(0.0F);
      this.setSpriteFromAge(sprites);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.removed) {
         float progress = (float)this.age / this.lifetime;
         this.setAlpha((float)Math.sin(Math.PI * progress) * 0.55F);
         this.setSpriteFromAge(this.sprites);
      }
   }

   @Override
   public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
      Quaternionf rotation = new Quaternionf().rotationY(this.surfaceYaw).rotateX((float) (-Math.PI / 2));
      this.extractRotatedQuad(renderState, camera, rotation, partialTick);
   }

   @Override
   public int getLightCoords(float partialTickTime) {
      return 15728880;
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

      public IchorSurfaceParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new IchorSurfaceParticle(level, x, y, z, this.sprites);
      }
   }
}
