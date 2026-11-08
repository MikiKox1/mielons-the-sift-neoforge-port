package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import mielon.thesift.item.ModItems;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class PollinatorEntity extends Monster implements GeoEntity {
   public static final float MODEL_SCALE = 1.0F;
   public static final float WIDTH = 0.675F;
   public static final float HEIGHT = 1.4375F;
   public static final float EYE_HEIGHT = 1.25F;
   private static final EntityDataAccessor<Boolean> SPITTING = SynchedEntityData.defineId(PollinatorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
   private static final RawAnimation SPIT = RawAnimation.begin().thenPlayAndHold("spit");
   private static final RawAnimation COMB_IDLE = RawAnimation.begin().thenLoop("comb_idle");
   private static final RawAnimation COMB_WALK = RawAnimation.begin().thenLoop("comb_walk");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int spitAge = -1;
   private long spitStartedAt;
   private LivingEntity spitTarget;
   private boolean exhalePending;

   public PollinatorEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 7;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 30.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   public static boolean checkPollinatorSpawnRules(
      EntityType<PollinatorEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
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
      builder.define(SPITTING, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PollinatorEntity.AvoidEnemyGoal());
      this.goalSelector.addGoal(2, new PollinatorEntity.RangedAttackGoal());
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Mob.class, 8.0F, 0.02F));
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
   protected BodyRotationControl createBodyControl() {
      return new BodyRotationControl(this) {
         @Override
         public void clientTick() {
            if (PollinatorEntity.this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
               super.clientTick();
            } else {
               float yaw = Mth.rotLerp(0.35F, PollinatorEntity.this.yBodyRot, PollinatorEntity.this.yHeadRot);
               PollinatorEntity.this.setYRot(yaw);
               PollinatorEntity.this.yBodyRot = yaw;
            }

            PollinatorEntity.this.yHeadRot = PollinatorEntity.this.yBodyRot;
         }
      };
   }

   public boolean isSpitting() {
      return this.entityData.get(SPITTING);
   }

   private void beginSpit(LivingEntity target) {
      this.spitTarget = target;
      this.spitAge = 0;
      this.spitStartedAt = this.level().getGameTime();
      this.entityData.set(SPITTING, true);
      this.playSound(ModSounds.POLLINATOR_INHALE, 0.9F, 1.0F);
   }

   private void cancelSpit() {
      this.spitAge = -1;
      this.spitTarget = null;
      this.entityData.set(SPITTING, false);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level) {
         if (!this.isAlive()) {
            this.cancelSpit();
            this.exhalePending = false;
         } else {
            if (this.exhalePending) {
               this.playSound(ModSounds.POLLINATOR_EXHALE, 0.9F, 1.0F);
               this.exhalePending = false;
            }

            if (level.getDifficulty() == Difficulty.PEACEFUL) {
               this.setTarget(null);
               this.setAggressive(false);
               this.cancelSpit();
            } else if (this.spitAge >= 0) {
               this.spitAge = (int)(level.getGameTime() - this.spitStartedAt);
               if (this.spitTarget != null
                  && this.spitTarget.isAlive()
                  && this.spitTarget.level() == this.level()
                  && this.canAttack(this.spitTarget)
                  && !(this.distanceToSqr(this.spitTarget) < 36.0)) {
                  if (this.spitAge == 6 && this.getSensing().hasLineOfSight(this.spitTarget)) {
                     Vec3 aim = this.spitTarget.position().add(0.0, 0.05, 0.0);
                     Vec3 forward = aim.subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
                     PollinatorProjectileEntity projectile = new PollinatorProjectileEntity(ModEntities.POLLINATOR_PROJECTILE, level);
                     projectile.setOwner(this);
                     projectile.setPos(this.getX() + forward.x * 0.44, this.getY() + 1.12, this.getZ() + forward.z * 0.44);
                     projectile.launchArc(aim);
                     level.addFreshEntity(projectile);
                     this.playSound(ModSounds.POLLINATOR_DROOL_FIRE, 1.0F, 0.95F + this.random.nextFloat() * 0.1F);
                     this.exhalePending = true;
                  }

                  if (this.spitAge >= 12) {
                     this.cancelSpit();
                  }
               } else {
                  this.cancelSpit();
               }
            }
         }
      }
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return TheSiftDimension.isOvergrownBiome(this.level().getBiome(this.blockPosition())) ? 3 : 4;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean playerKill) {
      super.dropCustomDeathLoot(level, source, playerKill);
      int looting = 0;
      if (source.getEntity() instanceof LivingEntity killer) {
         Reference<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
         looting = EnchantmentHelper.getItemEnchantmentLevel(enchantment, killer.getMainHandItem());
      }

      if (this.random.nextFloat() < Math.min(1.0F, 0.4F + 0.2F * looting)) {
         int count = 1 + this.random.nextInt(2) + Math.round(this.random.nextFloat() * Math.max(0, looting - 3));
         this.spawnAtLocation(level, new ItemStack(ModItems.POLLINATOR_DROOL, count));
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.isSpitting()) {
         return null;
      } else if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
         return ModSounds.POLLINATOR_RUN;
      } else {
         return this.getTarget() != null && this.getTarget().isAlive() ? ModSounds.POLLINATOR_AGGRESSIVE : ModSounds.POLLINATOR_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.POLLINATOR_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.POLLINATOR_DEATH;
   }

   @Override
   public void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.POLLINATOR_STEP, 0.3F, 0.95F + this.random.nextFloat() * 0.1F);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("body", 3, state -> {
         AnimationController<PollinatorEntity> controller = state.controller();
         boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
         controller.setAnimationSpeed(moving ? Mth.clamp(1.0 + this.getDeltaMovement().horizontalDistance() * 3.0, 1.0, 1.6) : 1.0);
         controller.setAnimation(moving ? WALK : IDLE);
         return PlayState.CONTINUE;
      }));
      controllers.add(new AnimationController("mouth", 3, state -> {
         AnimationController<PollinatorEntity> controller = state.controller();
         if (this.isSpitting()) {
            controller.setTransitionTicks(0);
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(SPIT);
         } else {
            controller.setTransitionTicks(3);
            boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            controller.setAnimationSpeed(moving ? 1.0 + this.getDeltaMovement().horizontalDistance() * 3.0 : 1.0);
            controller.setAnimation(moving ? COMB_WALK : COMB_IDLE);
         }

         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private final class AvoidEnemyGoal extends AvoidEntityGoal<LivingEntity> {
      private AvoidEnemyGoal() {
         super(
            PollinatorEntity.this,
            LivingEntity.class,
            6.0F,
            1.15,
            1.35,
            enemy -> (
                  enemy instanceof Player actualPlayer
                        && !actualPlayer.isCreative()
                        && !actualPlayer.isSpectator()
                        && (!SifterFamily.isDisguised(actualPlayer) || PollinatorEntity.this.getTarget() == actualPlayer)
                     || SculkerFamily.isSifterEnemy(enemy)
               )
               && PollinatorEntity.this.canAttack(enemy)
         );
      }

      @Override
      public void start() {
         PollinatorEntity.this.cancelSpit();
         super.start();
      }

      @Override
      public boolean canContinueToUse() {
         return this.toAvoid != null
            && this.toAvoid.isAlive()
            && PollinatorEntity.this.canAttack(this.toAvoid)
            && PollinatorEntity.this.distanceToSqr(this.toAvoid) < 64.0
            && super.canContinueToUse();
      }

      @Override
      public void stop() {
         super.stop();
         this.pathNav.stop();
      }
   }

   private final class RangedAttackGoal extends Goal {
      private int nextShot;
      private int visibleTicks;

      private RangedAttackGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         LivingEntity target = PollinatorEntity.this.getTarget();
         return target != null && target.isAlive() && PollinatorEntity.this.canAttack(target);
      }

      @Override
      public boolean canContinueToUse() {
         return this.canUse();
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void start() {
         this.nextShot = 12;
      }

      @Override
      public void stop() {
         PollinatorEntity.this.getNavigation().stop();
         PollinatorEntity.this.cancelSpit();
         this.visibleTicks = 0;
      }

      @Override
      public void tick() {
         LivingEntity target = PollinatorEntity.this.getTarget();
         if (target != null) {
            double distance = PollinatorEntity.this.distanceToSqr(target);
            boolean visible = PollinatorEntity.this.getSensing().hasLineOfSight(target);
            this.visibleTicks = visible ? this.visibleTicks + 1 : 0;
            PollinatorEntity.this.getLookControl().setLookAt(target, 30.0F, 20.0F);
            if (--this.nextShot < 0) {
               this.nextShot = 0;
            }

            if (distance < 36.0) {
               PollinatorEntity.this.cancelSpit();
               PollinatorEntity.this.getNavigation().stop();
            } else if (!(distance > 196.0) && this.visibleTicks >= 8) {
               PollinatorEntity.this.getNavigation().stop();
               if (!PollinatorEntity.this.isSpitting() && this.nextShot == 0 && visible) {
                  double dx = target.getX() - PollinatorEntity.this.getX();
                  double dz = target.getZ() - PollinatorEntity.this.getZ();
                  float yaw = (float)(Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                  if (Math.abs(Mth.wrapDegrees(yaw - PollinatorEntity.this.yBodyRot)) <= 25.0F) {
                     PollinatorEntity.this.beginSpit(target);
                     this.nextShot = 50 + PollinatorEntity.this.random.nextInt(21);
                  }
               }
            } else {
               PollinatorEntity.this.getNavigation().moveTo(target, 1.0);
            }
         }
      }
   }
}
