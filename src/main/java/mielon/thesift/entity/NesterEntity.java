package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

public final class NesterEntity extends Monster implements GeoEntity {
   public static final float MODEL_SCALE = 2.0F;
   public static final float WIDTH = 0.95F;
   public static final float HEIGHT = 2.25F;
   public static final float EYE_HEIGHT = 2.06F;
   private static final int WARNING_TICKS = 32;
   private static final int RAM_TICKS = 8;
   private static final int RAM_DAMAGE_TICK = 2;
   private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(NesterEntity.class, EntityDataSerializers.INT);
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
   private static final RawAnimation WARNING = RawAnimation.begin().thenPlayAndHold("warning");
   private static final RawAnimation RAM = RawAnimation.begin().thenPlayAndHold("ram");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int warningTicks;
   private int ramAge = -1;
   private LivingEntity ramTarget;
   private long ramStartedAt;
   private long nextRamTick;

   public NesterEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 7;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.MOVEMENT_SPEED, 0.33)
         .add(Attributes.ATTACK_DAMAGE, 6.0)
         .add(Attributes.ATTACK_KNOCKBACK, 0.35)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   public static boolean checkNesterSpawnRules(
      EntityType<NesterEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return SeedlingEntity.checkSeedlingSpawnRules(type, level, reason, pos, random);
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
      return SifterFamily.isDroolDamage(source) || super.isInvulnerableTo(level, source);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_ACTION, 0);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new NesterEntity.ActionGoal());
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.18, false) {
         @Override
         public boolean canUse() {
            return !NesterEntity.this.isWarning() && super.canUse();
         }

         @Override
         public boolean canContinueToUse() {
            return !NesterEntity.this.isWarning() && super.canContinueToUse();
         }
      });
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Mob.class, 6.0F, 0.02F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector
         .addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (target, level) -> !SifterFamily.isDisguised(target)));
      this.targetSelector
         .addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target, level) -> SculkerFamily.isSifterEnemy(target)));
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      return this.level().getDifficulty() != Difficulty.PEACEFUL && super.canAttack(target);
   }

   @Override
   public boolean isPreventingPlayerRest(ServerLevel level, Player player) {
      return this.level().getDifficulty() != Difficulty.PEACEFUL;
   }

   @Override
   public void setTarget(LivingEntity target) {
      LivingEntity previous = this.getTarget();
      super.setTarget(target);
      if (this.level() instanceof ServerLevel
         && this.getTarget() == target
         && target instanceof Player
         && target != previous
         && this.canAttack(target)
         && this.getSensing().hasLineOfSight(target)) {
         this.warningTicks = 32;
         this.ramAge = -1;
         this.ramTarget = null;
         this.entityData.set(DATA_ACTION, 1);
         this.pauseForWarning();
         this.playSound(ModSounds.NESTER_ENRAGED, 1.0F, 1.0F);
      }
   }

   public boolean isPerformingAction() {
      return this.entityData.get(DATA_ACTION) != 0;
   }

   private boolean isWarning() {
      return this.entityData.get(DATA_ACTION) == 1;
   }

   private void pauseForWarning() {
      this.getNavigation().stop();
      this.setSpeed(0.0F);
      this.setXxa(0.0F);
      this.setZza(0.0F);
      this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      this.setJumping(false);
   }

   @Override
   public boolean doHurtTarget(ServerLevel level, Entity target) {
      if (!this.isPerformingAction()
         && this.level().getGameTime() >= this.nextRamTick
         && target instanceof LivingEntity living
         && living.isAlive()
         && this.canAttack(living)) {
         this.ramTarget = living;
         this.ramAge = 0;
         this.ramStartedAt = this.level().getGameTime();
         this.nextRamTick = this.ramStartedAt + 20L;
         this.entityData.set(DATA_ACTION, 2);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         if (level.getDifficulty() == Difficulty.PEACEFUL) {
            this.setTarget(null);
            this.setAggressive(false);
            this.clearAction();
         } else if (this.warningTicks <= 0) {
            if (this.ramAge >= 0) {
               this.ramAge = (int)(level.getGameTime() - this.ramStartedAt);
               if (this.ramTarget != null && this.ramTarget.isAlive()) {
                  this.getLookControl().setLookAt(this.ramTarget, 30.0F, 30.0F);
                  if (this.ramAge == 1 && this.onGround()) {
                     Vec3 direction = this.ramTarget.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
                     this.setDeltaMovement(this.getDeltaMovement().add(direction.scale(0.06)));
                  }
               }

               if (this.ramAge == 2 && this.canLandRam()) {
                  this.playSound(ModSounds.NESTER_ATTACK, 1.0F, 0.9F + this.random.nextFloat() * 0.1F);
                  super.doHurtTarget(level, this.ramTarget);
               }

               if (this.ramAge >= 8) {
                  this.clearAction();
               }
            }
         } else {
            this.pauseForWarning();
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.canAttack(target)) {
               this.getLookControl().setLookAt(target, 30.0F, 30.0F);
               if (--this.warningTicks == 0) {
                  this.entityData.set(DATA_ACTION, 0);
               }
            } else {
               this.clearAction();
            }
         }
      }
   }

   private boolean canLandRam() {
      return this.ramTarget != null
         && this.ramTarget.isAlive()
         && this.ramTarget.level() == this.level()
         && this.canAttack(this.ramTarget)
         && this.isWithinMeleeAttackRange(this.ramTarget)
         && this.getSensing().hasLineOfSight(this.ramTarget);
   }

   private void clearAction() {
      this.warningTicks = 0;
      this.ramAge = -1;
      this.ramTarget = null;
      this.entityData.set(DATA_ACTION, 0);
   }

   @Override
   public void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null && (Math.abs(speed.getBaseValue() - 0.275) < 1.0E-6 || Math.abs(speed.getBaseValue() - 0.31) < 1.0E-6)) {
         speed.setBaseValue(0.33);
      }

      AttributeInstance resistance = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (resistance != null && (resistance.getBaseValue() == 0.0 || resistance.getBaseValue() == 0.4)) {
         resistance.setBaseValue(0.2);
      }
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return TheSiftDimension.isOvergrownBiome(this.level().getBiome(this.blockPosition())) ? 3 : 2;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.isPerformingAction()) {
         return null;
      } else {
         return this.getTarget() != null && this.getTarget().isAlive() ? ModSounds.NESTER_AGGRESSIVE : ModSounds.NESTER_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.NESTER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.NESTER_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.NESTER_STEP, 0.35F, 0.95F + this.random.nextFloat() * 0.1F);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("body", 3, state -> {
         AnimationController<NesterEntity> controller = state.controller();
         int action = this.entityData.get(DATA_ACTION);
         controller.setAnimationSpeed(1.0);
         if (action == 1) {
            controller.setAnimation(WARNING);
         } else if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            double speed = Math.sqrt(this.getDeltaMovement().horizontalDistanceSqr());
            controller.setAnimationSpeed(Mth.clamp(1.0 + speed * 3.0, 1.0, 1.8));
            controller.setAnimation(WALK);
         } else {
            controller.setAnimation(IDLE);
         }

         return PlayState.CONTINUE;
      }));
      controllers.add(new AnimationController("ram", 0, state -> {
         AnimationController<NesterEntity> controller = state.controller();
         if (this.entityData.get(DATA_ACTION) == 2) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(RAM);
            return PlayState.CONTINUE;
         } else {
            controller.reset();
            return PlayState.STOP;
         }
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private final class ActionGoal extends Goal {
      private ActionGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return NesterEntity.this.isWarning();
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         NesterEntity.this.pauseForWarning();
         LivingEntity target = NesterEntity.this.getTarget();
         if (target != null) {
            NesterEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
         }
      }
   }
}
