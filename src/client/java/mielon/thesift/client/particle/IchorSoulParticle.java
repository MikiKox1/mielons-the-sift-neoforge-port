package mielon.thesift.client.particle;

import mielon.thesift.fluid.IchorFluid;
import mielon.thesift.fluid.IchorSoulMotion;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

public final class IchorSoulParticle extends SingleQuadParticle {
   private final int mode;
   private final Entity target;
   private final double phase;
   private final double phaseZ;
   private final double offsetX;
   private final double offsetY;
   private final double offsetZ;
   private double west;
   private double east;
   private double depth;
   private int fluidY;
   private final float size;
   private final double launchX;
   private final double launchZ;
   private final double startY;
   private final double aimX;
   private final double aimY;
   private final double aimZ;
   private float absorptionFade = 1.0F;

   private IchorSoulParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, int mode, Entity target, boolean absorbed) {
      super(level, x, y, z, sprites.get(level.getRandom()));
      this.mode = mode;
      this.startY = y;
      this.target = absorbed ? target : null;
      this.aimX = (this.random.nextDouble() - 0.5) * 0.5;
      this.aimY = 0.3 + this.random.nextDouble() * 0.45;
      this.aimZ = (this.random.nextDouble() - 0.5) * 0.5;
      this.phase = this.random.nextDouble() * Math.PI * 2.0;
      this.phaseZ = this.random.nextDouble() * Math.PI * 2.0;
      this.offsetX = Math.cos(this.phase) * (1.6 + this.random.nextDouble());
      this.offsetZ = Math.sin(this.phase) * (1.6 + this.random.nextDouble());
      this.offsetY = (this.random.nextDouble() - 0.5) * 1.6;
      this.launchX = (this.random.nextDouble() - 0.5) * (mode == 2 ? 0.16 : 0.06);
      this.launchZ = (this.random.nextDouble() - 0.5) * (mode == 2 ? 0.16 : 0.06);
      this.size = mode == 2 ? 0.15F + this.random.nextFloat() * 0.09F : 0.24F + this.random.nextFloat() * 0.1F;
      this.quadSize = this.size;
      this.hasPhysics = mode != 0 && !absorbed;
      this.gravity = 0.0F;
      this.friction = 1.0F;
      this.alpha = 0.0F;
      this.lifetime = 60;
      this.xd = 0.065 + this.random.nextDouble() * 0.025;
      this.yd = mode == 1 ? (12.0 + this.random.nextDouble() * 4.0) / 60.0 : 0.12 + this.random.nextDouble() * 0.045;
      if (mode == 0) {
         this.fluidY = (int)Math.floor(y);
         BlockPos pos = BlockPos.containing(x, this.fluidY, z);
         this.depth = pos.getY() + level.getFluidState(pos).getHeight(level, pos) - y;
         int westBlock = pos.getX();
         int eastBlock = pos.getX();

         for (int i = 0; i < 64 && this.source(westBlock - 1, z); i++) {
            westBlock--;
         }

         for (int i = 0; i < 64 && this.source(eastBlock + 1, z); i++) {
            eastBlock++;
         }

         this.west = westBlock + this.size + 0.02;
         this.east = eastBlock + 1.0 - this.size - 0.02;
         this.setPos(this.west, y, z);
         this.lifetime = Math.min(900, Math.max(12, (int)Math.ceil((this.east - this.west) / this.xd)));
      } else if (this.target != null) {
         this.xd = this.yd = this.zd = 0.0;
         this.setPos(
            target.getX() + this.offsetX * (1.0 + target.getBbWidth() * 0.3),
            target.getY() + target.getBbHeight() * this.aimY + this.offsetY,
            target.getZ() + this.offsetZ * (1.0 + target.getBbWidth() * 0.3)
         );
      }

      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
   }

   private boolean source(int x, double z) {
      BlockPos pos = BlockPos.containing(x, this.fluidY, z);
      if (this.level.hasChunkAt(pos) && this.level.getBlockState(pos.above()).isAir()) {
         FluidState fluid = this.level.getFluidState(pos);
         return fluid.getType() instanceof IchorFluid && fluid.isSource();
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (++this.age >= this.lifetime) {
         this.remove();
      } else {
         if (this.mode == 0) {
            double nextX = Math.min(this.east, this.x + this.xd);
            if (!this.source((int)Math.floor(nextX + this.size + this.xd * 14.0), this.z)) {
               this.lifetime = Math.min(this.lifetime, this.age + 14);
            }

            if (!this.source((int)Math.floor(nextX), this.z)) {
               this.remove();
               return;
            }

            BlockPos pos = BlockPos.containing(nextX, this.fluidY, this.z);
            this.setPos(nextX, this.fluidY + this.level.getFluidState(pos).getHeight(this.level, pos) - this.depth, this.z);
            float bankFade = IchorSoulMotion.smooth((this.x - this.west) / 0.65) * IchorSoulMotion.smooth((this.east - this.x) / 0.9);
            this.alpha = 0.85F * bankFade * IchorSoulMotion.smooth((this.lifetime - this.age) / 14.0);
         } else if (this.target != null) {
            if (this.target.isRemoved()) {
               this.lifetime = Math.min(this.lifetime, this.age + 10);
            } else {
               double dx = this.target.getX() + this.aimX * this.target.getBbWidth() - this.x;
               double dy = this.target.getY() + this.target.getBbHeight() * this.aimY - this.y;
               double dz = this.target.getZ() + this.aimZ * this.target.getBbWidth() - this.z;
               double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
               double speed = Math.min(0.14, distance * 0.12);
               double factor = speed / Math.max(0.001, distance);
               double curl = (1.0 - IchorSoulMotion.smooth(this.age / 40.0)) * 0.018;
               this.xd = this.xd + (dx * factor + Math.sin(this.phase + this.age * 0.12) * curl - this.xd) * 0.25;
               this.yd = this.yd + (dy * factor - this.yd) * 0.25;
               this.zd = this.zd + (dz * factor + Math.cos(this.phaseZ + this.age * 0.1) * curl - this.zd) * 0.25;
               AABB body = this.target.getBoundingBox();
               double edgeX = Math.max(0.0, Math.max(body.minX - this.x, this.x - body.maxX));
               double edgeY = Math.max(0.0, Math.max(body.minY - this.y, this.y - body.maxY));
               double edgeZ = Math.max(0.0, Math.max(body.minZ - this.z, this.z - body.maxZ));
               double clearance = Math.sqrt(edgeX * edgeX + edgeY * edgeY + edgeZ * edgeZ);
               this.absorptionFade = Math.min(this.absorptionFade, IchorSoulMotion.smooth((clearance - 0.12) / 0.9));
               if (this.absorptionFade <= 0.002F) {
                  this.remove();
                  return;
               }
            }

            this.move(this.xd, this.yd, this.zd);
            this.alpha = IchorSoulMotion.airAlpha(this.age, this.lifetime) * this.absorptionFade;
         } else {
            double riseStep = this.mode == 1 ? Math.min(this.yd, Math.max(0.0, this.startY + 16.0 - this.y)) : this.yd;
            this.move(this.launchX + IchorSoulMotion.wander(this.age, this.phase), riseStep, this.launchZ + IchorSoulMotion.wander(this.age, this.phaseZ));
            if (this.y == this.yo || this.onGround) {
               this.lifetime = Math.min(this.lifetime, this.age + 12);
            }

            this.alpha = IchorSoulMotion.airAlpha(this.age, this.lifetime);
            if (this.mode == 1) {
               this.alpha = this.alpha * IchorSoulMotion.smooth((16.0 - (this.y - this.startY)) / 2.5);
            }
         }

         this.quadSize = this.size * (1.0F + 0.06F * (float)Math.sin(this.phase + this.age * 0.13));
      }
   }

   @Override
   public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
      if (this.mode == 0) {
         this.extractRotatedQuad(state, camera, new Quaternionf().rotationY((float) (-Math.PI / 2)).rotateX((float) (-Math.PI / 2)), partialTick);
      } else {
         super.extract(state, camera, partialTick);
      }
   }

   @Override
   public int getLightCoords(float partialTick) {
      return 15728880;
   }

   @Override
   protected Layer getLayer() {
      return Layer.TRANSLUCENT;
   }

   public static final class BodyProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public BodyProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public IchorSoulParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new IchorSoulParticle(level, x, y, z, this.sprites, 2, level.getEntity((int)xd), yd > 0.0);
      }
   }

   public static final class RisingProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public RisingProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public IchorSoulParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new IchorSoulParticle(level, x, y, z, this.sprites, 1, null, false);
      }
   }

   public static final class SurfaceProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public SurfaceProvider(SpriteSet sprites) {
         this.sprites = sprites;
      }

      public IchorSoulParticle createParticle(
         SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random
      ) {
         return new IchorSoulParticle(level, x, y, z, this.sprites, 0, null, false);
      }
   }
}
