package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import mielon.thesift.fluid.ProjectileFluidEffects;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class PollinatorProjectileEntity extends Projectile implements GeoEntity {
   public static final double GRAVITY = 0.045;
   public static final double PLAYER_GRAVITY = 0.05;
   private static final EntityDataAccessor<Boolean> PLAYER_THROWN = SynchedEntityData.defineId(PollinatorProjectileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private boolean hitEntity;
   private boolean clientLanded;

   public PollinatorProjectileEntity(EntityType<? extends PollinatorProjectileEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(PLAYER_THROWN, false);
   }

   public void setPlayerThrown() {
      this.entityData.set(PLAYER_THROWN, true);
   }

   public void launchArc(Vec3 target) {
      Vec3 delta = target.subtract(this.position());
      double ticks = Math.max(16.0, Math.min(32.0, delta.horizontalDistance() / 0.5));
      this.setDeltaMovement(delta.x / ticks, delta.y / ticks + 0.045 * (ticks - 1.0) * 0.5, delta.z / ticks);
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return !this.hitEntity && !SifterFamily.isMember(entity) && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.clientLanded) {
         if (this.level() instanceof ServerLevel && this.getOwner() instanceof PollinatorEntity && this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
         } else {
            Vec3 velocity = this.getDeltaMovement();
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != Type.MISS) {
               this.onHit(hit);
            } else {
               this.setPos(this.position().add(velocity));
               this.updateRotation();
               boolean thrown = this.entityData.get(PLAYER_THROWN);
               FluidState fluid = this.level().getFluidState(this.blockPosition());
               boolean submerged = !fluid.isEmpty() && this.getY() < this.blockPosition().getY() + fluid.getHeight(this.level(), this.blockPosition());
               double drag = submerged ? 0.6 : (thrown ? 0.99 : 1.0);
               if (submerged) {
                  ProjectileFluidEffects.spawnTrail(this, velocity);
               }

               this.setDeltaMovement(velocity.scale(drag).add(0.0, -(thrown ? 0.05 : 0.045), 0.0));
               if (this.level() instanceof ServerLevel && this.tickCount >= 200) {
                  this.discard();
               }
            }
         }
      }
   }

   @Override
   protected void onHit(HitResult hit) {
      Vec3 impact = hit.getLocation();
      if (hit instanceof BlockHitResult block) {
         if (block.getDirection() == Direction.UP) {
            this.land(impact.add(0.0, 0.015, 0.0));
            return;
         }

         impact = impact.add(block.getDirection().getUnitVec3().scale(0.03));
      } else {
         this.hitEntity = true;
      }

      this.setPos(impact);
      Vec3 velocity = this.getDeltaMovement();
      this.setDeltaMovement(velocity.x * 0.2, Math.min(-0.08, velocity.y), velocity.z * 0.2);
      this.needsSync = true;
      this.hurtMarked = true;
      this.updateRotation();
   }

   private void land(Vec3 ground) {
      if (!this.isRemoved() && !this.clientLanded) {
         this.setPos(ground);
         if (this.level() instanceof ServerLevel level) {
            AreaEffectCloud var6 = new AreaEffectCloud(level, ground.x, ground.y, ground.z);
            if (this.getOwner() instanceof LivingEntity owner) {
               var6.setOwner(owner);
            }

            var6.setCustomParticle(ModParticles.POLLINATOR_DROOL);
            var6.setRadius(1.75F);
            var6.setRadiusOnUse(0.0F);
            var6.setRadiusPerTick(0.0F);
            var6.setDurationOnUse(0);
            var6.setWaitTime(0);
            var6.setDuration(60);
            var6.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1));
            level.addFreshEntity(var6);
            level.playSound(
               null,
               ground.x,
               ground.y,
               ground.z,
               ModSounds.POLLINATOR_DROOL_EXPLOSION,
               this.getOwner() instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE,
               1.0F,
               1.0F
            );
            level.sendParticles(ModParticles.POLLINATOR_DROOL, ground.x, ground.y, ground.z, 24, 0.6, 0.015, 0.6, 0.03);
            level.playSound(
               null,
               ground.x,
               ground.y,
               ground.z,
               ModSounds.POLLINATOR_DROOL_LAND,
               this.getOwner() instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE,
               1.0F,
               1.0F
            );
            this.discard();
         } else {
            this.clientLanded = true;
            this.setDeltaMovement(Vec3.ZERO);
            this.setInvisible(true);
            this.emitLandingParticles(ground);
         }
      }
   }

   private void emitLandingParticles(Vec3 ground) {
      for (int i = 0; i < 24; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double radius = Math.sqrt(this.random.nextDouble()) * 1.75;
         this.level()
            .addParticle(
               ModParticles.POLLINATOR_DROOL,
               ground.x + Math.cos(angle) * radius,
               ground.y,
               ground.z + Math.sin(angle) * radius,
               (this.random.nextDouble() - 0.5) * 0.15,
               0.01,
               (this.random.nextDouble() - 0.5) * 0.15
            );
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("PlayerThrown", this.entityData.get(PLAYER_THROWN));
      output.putBoolean("HitEntity", this.hitEntity);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.entityData.set(PLAYER_THROWN, input.getBooleanOr("PlayerThrown", input.getBooleanOr("Straight", false)));
      this.hitEntity = input.getBooleanOr("HitEntity", false);
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
