package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class MonarchProjectileEntity extends Projectile implements GeoEntity {
   public static final float MODEL_SCALE = 1.5F;
   private static final EntityDataAccessor<Boolean> FALLING = SynchedEntityData.defineId(MonarchProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int age;

   public MonarchProjectileEntity(EntityType<? extends MonarchProjectileEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public boolean isSummoning() {
      return this.getType() == ModEntities.MONARCH_SUMMON_PROJECTILE;
   }

   public boolean isFalling() {
      return this.entityData.get(FALLING);
   }

   public void launch(MonarchEntity owner, Vec3 point) {
      this.setOwner(owner);
      this.setDeltaMovement(point.subtract(this.position()).normalize().scale(this.isSummoning() ? 0.7 : 0.9));
      this.playSound(ModSounds.MONARCH_PROJECTILE_LAUNCH, 1.0F, 0.68F);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(FALLING, false);
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return !this.isSummoning() && !SculkerFamily.contains(entity) && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server) {
         if (++this.age > 300 || this.level().getDifficulty() == Difficulty.PEACEFUL || this.getOwner() instanceof MonarchEntity monarch && !monarch.isAlive()) {
            this.finish(null);
            return;
         }

         HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
         if (hit.getType() != Type.MISS) {
            this.onHit(hit);
            return;
         }

         if (this.age % 3 == 0) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 1, 0.03, 0.03, 0.03, 0.0);
         }
      }

      this.setPos(this.position().add(this.getDeltaMovement()));
      this.updateRotation();
      if (this.isFalling()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.045, 0.0));
      }
   }

   @Override
   protected void onHit(HitResult hit) {
      if (this.level() instanceof ServerLevel server && !this.isRemoved()) {
         this.setPos(hit.getLocation());
         if (!this.isSummoning()) {
            this.explode(server);
         } else {
            if (hit instanceof BlockHitResult blockHit) {
               if (blockHit.getDirection() == Direction.UP) {
                  this.summon(server, blockHit);
                  return;
               }

               Vec3 velocity = this.getDeltaMovement();
               Direction face = blockHit.getDirection();
               double x = face.getStepX();
               double y = face.getStepY();
               double z = face.getStepZ();
               double dot = velocity.x * x + velocity.y * y + velocity.z * z;
               Vec3 reflected = velocity.subtract(new Vec3(x, y, z).scale(2.0 * dot)).scale(0.35);
               this.setDeltaMovement(reflected.x, Math.min(-0.12, reflected.y), reflected.z);
               this.setPos(this.position().add(x * 0.06, y * 0.06, z * 0.06));
               this.entityData.set(FALLING, true);
               this.needsSync = true;
               this.hurtMarked = true;
            }
         }
      }
   }

   private void summon(ServerLevel server, BlockHitResult hit) {
      Mob minion = (this.random.nextBoolean() ? ModEntities.STALKER : ModEntities.SCAVENGER).create(server, EntitySpawnReason.MOB_SUMMONED);
      if (minion == null) {
         this.finish(null);
      } else {
         Vec3 point = hit.getLocation().add(0.0, 0.03, 0.0);
         boolean placed = false;

         for (int radius = 0; radius <= 3 && !placed; radius++) {
            for (int dx = -radius; dx <= radius && !placed; dx++) {
               for (int dz = -radius; dz <= radius && !placed; dz++) {
                  if (radius == 0 || Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                     BlockPos floor = hit.getBlockPos().offset(dx, 0, dz);
                     if (server.getBlockState(floor).isFaceSturdy(server, floor, Direction.UP)) {
                        minion.setPos(point.x + dx, point.y, point.z + dz);
                        if (server.noCollision(minion) && server.getFluidState(minion.blockPosition()).isEmpty()) {
                           placed = true;
                        }
                     }
                  }
               }
            }
         }

         if (!placed) {
            this.setPos(point.add(0.0, 0.08, 0.0));
            this.entityData.set(FALLING, true);
            this.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.4, 0.15, (this.random.nextDouble() - 0.5) * 0.4);
            this.needsSync = true;
            this.hurtMarked = true;
         } else {
            minion.setPersistenceRequired();
            if (this.getOwner() instanceof MonarchEntity owner) {
               minion.addTag("the_sift.monarch_owner=" + owner.getUUID());
               if (owner.getTarget() != null) {
                  minion.setTarget(owner.getTarget());
               }
            }

            if (!server.addFreshEntity(minion)) {
               this.finish(null);
            } else {
               this.burst(server);
               this.finish(minion);
            }
         }
      }
   }

   private void explode(ServerLevel server) {
      for (LivingEntity victim : server.getEntitiesOfClass(
         LivingEntity.class, this.getBoundingBox().inflate(3.0), e -> e.isAlive() && !SculkerFamily.contains(e)
      )) {
         double distance = victim.getBoundingBox().getCenter().distanceTo(this.position());
         if (!(distance > 3.0) && this.clearPath(victim)) {
            victim.hurtServer(server, this.damageSources().indirectMagic(this, this.getOwner()), (float)(4.0 + 8.0 * (1.0 - distance / 3.0)));
            Vec3 away = victim.position().subtract(this.position());
            victim.knockback(0.6, -away.x, -away.z, this.damageSources().indirectMagic(this, this.getOwner()), 0.0F);
         }
      }

      this.burst(server);
      this.finish(null);
   }

   private boolean clearPath(LivingEntity victim) {
      Vec3 start = this.position().subtract(this.getDeltaMovement().normalize().scale(0.08));
      return this.level().clip(new ClipContext(start, victim.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, this)).getType() == Type.MISS;
   }

   private void burst(ServerLevel server) {
      server.sendParticles(
         this.isSummoning() ? ParticleTypes.SCULK_CHARGE_POP : ParticleTypes.SOUL_FIRE_FLAME,
         this.getX(),
         this.getY() + 0.1,
         this.getZ(),
         45,
         0.5,
         0.5,
         0.5,
         0.08
      );
      this.playSound(ModSounds.MONARCH_PROJECTILE_EXPLODE, 1.0F, 0.75F);
   }

   private void finish(Mob summoned) {
      if (this.isSummoning() && this.getOwner() instanceof MonarchEntity owner) {
         owner.resolveSummon(this.getUUID(), summoned);
      }

      this.discard();
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("FlightAge", this.age);
      output.putBoolean("Falling", this.isFalling());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.age = input.getIntOr("FlightAge", 0);
      this.entityData.set(FALLING, input.getBooleanOr("Falling", false));
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("spin", 0, state -> {
         state.controller().setAnimation(SPIN);
         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
