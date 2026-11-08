package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ScavengerProjectileEntity extends Projectile implements GeoEntity {
   public static final float DAMAGE = 6.0F;
   private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("spin");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private UUID targetId;
   private LivingEntity target;
   private int age;

   public ScavengerProjectileEntity(EntityType<? extends ScavengerProjectileEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public void launch(LivingEntity owner, LivingEntity victim) {
      this.setOwner(owner);
      this.target = victim;
      this.targetId = victim.getUUID();
      this.setDeltaMovement(victim.getEyePosition().subtract(this.position()).normalize().scale(0.22));
   }

   @Override
   protected void defineSynchedData(Builder builder) {
   }

   @Override
   public boolean isPickable() {
      return !this.isRemoved();
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      return !SculkerFamily.contains(entity) && super.canHitEntity(entity);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server) {
         if (++this.age > 200 || this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
         }

         if (this.target == null && this.targetId != null && server.getEntity(this.targetId) instanceof LivingEntity living) {
            this.target = living;
         }

         if (this.target != null
            && (
               !this.target.isAlive()
                  || this.target.isSpectator()
                  || this.target.level() != this.level()
                  || SculkerFamily.contains(this.target)
                  || this.target instanceof Player player && player.isCreative()
            )) {
            this.discard();
            return;
         }

         if (this.target != null) {
            Vec3 desired = this.target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(0.32);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.86).add(desired.scale(0.14)));
            this.needsSync = true;
            this.syncVelocity = true;
         }

         HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
         if (hit.getType() != Type.MISS) {
            this.onHit(hit);
            return;
         }

         if (this.age % 3 == 0) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 1, 0.025, 0.025, 0.025, 0.0);
         }
      }

      this.setPos(this.position().add(this.getDeltaMovement()));
      this.updateRotation();
   }

   @Override
   protected void onHit(HitResult hit) {
      if (this.level() instanceof ServerLevel server && !this.isRemoved()) {
         this.setPos(hit.getLocation());
         if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity victim && !SculkerFamily.contains(victim)) {
            victim.hurtServer(server, this.damageSources().indirectMagic(this, this.getOwner()), 6.0F);
         }

         server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 24, 0.3, 0.3, 0.3, 0.045);
         this.playSound(ModSounds.SCAVENGER_PROJECTILE_EXPLODE, 0.8F, 1.1F);
         this.discard();
      }
   }

   @Override
   public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
      if (!this.isRemoved() && !this.isInvulnerableToBase(source)) {
         if (this.level() instanceof ServerLevel particlesLevel) {
            particlesLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 12, 0.12, 0.12, 0.12, 0.02);
         }

         this.playSound(ModSounds.SCAVENGER_PROJECTILE_EXPLODE, 0.8F, 1.1F);
         this.discard();
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      if (this.targetId != null) {
         output.putString("HomingTarget", this.targetId.toString());
      }

      output.putInt("FlightAge", this.age);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      String saved = input.getStringOr("HomingTarget", "");

      try {
         this.targetId = saved.isEmpty() ? null : UUID.fromString(saved);
      } catch (IllegalArgumentException ignored) {
         this.targetId = null;
      }

      this.age = input.getIntOr("FlightAge", 0);
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
