package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import mielon.thesift.entity.ai.StalkerNavigation;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StalkerEntity extends Monster implements GeoEntity {
   public static final float MODEL_SCALE = 2.0F;
   public static final float WIDTH = 0.90999997F;
   public static final float HEIGHT = 2.375F;
   public static final float EYE_HEIGHT = 2.125F;
   private static final double MOVEMENT_SPEED = 0.35;
   private static final double CHASE_SPEED = 1.2;
   private static final double CLAW_REACH = 0.85;
   public static final float CLAW_DAMAGE = 4.0F;
   public static final float LOW_HEALTH = 12.0F;
   private static final int COMBO_LENGTH = 8;
   private static final int FIRST_CUT = 4;
   private static final int SECOND_CUT = 8;
   private static final ThreadLocal<LivingEntity> FIRST_CLAW_TARGET = new ThreadLocal<>();
   private static final EntityDataAccessor<Boolean> DASHING = SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Float> DASH_YAW = SynchedEntityData.defineId(StalkerEntity.class, EntityDataSerializers.FLOAT);
   private static final ResourceKey<DamageType> FOLLOWUP = ResourceKey.create(
      Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("the_sift", "stalker_followup")
   );
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
   private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
   private static final RawAnimation DASH = RawAnimation.begin().thenLoop("dash");
   private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int dashTicks;
   private boolean defaultsChecked;

   public StalkerEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 5;
      this.getNavigation().setCanFloat(true);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 35.0)
         .add(Attributes.ATTACK_DAMAGE, 4.0)
         .add(Attributes.MOVEMENT_SPEED, 0.35)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   public float getWalkTargetValue(BlockPos pos, LevelReader level) {
      return level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT) ? -level.getBrightness(LightLayer.BLOCK, pos) : super.getWalkTargetValue(pos, level);
   }

   public static boolean checkStalkerSpawnRules(
      EntityType<StalkerEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return checkSculkerSpawnRules(level, reason, pos, random, false);
   }

   static boolean checkSculkerSpawnRules(ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random, boolean scavenger) {
      BlockState floor = level.getBlockState(pos.below());
      boolean corrupted = level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT);
      boolean ambient = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
      boolean endure = !corrupted && SiftTides.tide(level.getLevel().getDefaultClockTime()) == SiftTides.Tide.ENDURE;
      return level.getDifficulty() != Difficulty.PEACEFUL
         && level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)
         && (corrupted ? level.getBrightness(LightLayer.BLOCK, pos) == 0 : !endure || level.getBrightness(LightLayer.BLOCK, pos) == 0)
         && (!ambient || corrupted || !floor.is(Blocks.SCULK))
         && (
            corrupted
               || floor.is(Blocks.SCULK) && !ambient
               || level.getBiome(pos).is(TheSiftDimension.ORGANIC_DESERT) && random.nextInt(8) == 0
               || endure && random.nextInt(32) == 0
         )
         && floor.isFaceSturdy(level, pos.below(), Direction.UP)
         && (
            reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION
               || (
                  endure
                     ? (scavenger ? SculkerPopulation.canSpawnScavenger(level, pos, true) : SculkerPopulation.canSpawnEndureStalker(level, pos))
                     : (scavenger ? SculkerPopulation.canSpawnScavenger(level, pos, false) : SculkerPopulation.canSpawn(level, pos, true))
               )
         )
         && level.getFluidState(pos).isEmpty()
         && level.getFluidState(pos.above()).isEmpty();
   }

   @Override
   public void checkDespawn() {
      if (!SculkerPopulation.despawnCrowded(this)) {
         super.checkDespawn();
      }
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DASHING, false);
      builder.define(DASH_YAW, 0.0F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new StalkerEntity.ClawAttackGoal());
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector
         .addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target, level) -> SifterFamily.isMember(target)));
   }

   @Override
   public boolean canAttack(LivingEntity other) {
      return !SculkerFamily.contains(other) && super.canAttack(other);
   }

   @Override
   protected boolean considersEntityAsAlly(Entity other) {
      return SculkerFamily.contains(other) || super.considersEntityAsAlly(other);
   }

   public boolean isDashing() {
      return this.entityData.get(DASHING);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel && !this.defaultsChecked) {
         this.defaultsChecked = true;
         AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
         if (health != null && Math.abs(health.getBaseValue() - 30.0) < 1.0E-5) {
            health.setBaseValue(35.0);
            if (this.getHealth() > 0.0F) {
               this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 5.0F));
            }
         }

         AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null && (Math.abs(speed.getBaseValue() - 0.28) < 1.0E-5 || Math.abs(speed.getBaseValue() - 0.38) < 1.0E-5)) {
            speed.setBaseValue(0.35);
         }

         AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
         if (damage != null && (Math.abs(damage.getBaseValue() - 1.75) < 1.0E-5 || Math.abs(damage.getBaseValue() - 2.0) < 1.0E-5)) {
            damage.setBaseValue(4.0);
         }

         AttributeInstance step = this.getAttribute(Attributes.STEP_HEIGHT);
         if (step != null && Math.abs(step.getBaseValue() - 0.6) < 1.0E-5) {
            step.setBaseValue(1.0);
         }
      }

      if (this.isDashing()) {
         this.faceDashDirection();
         if (this.level() instanceof ServerLevel && this.dashTicks > 0 && (--this.dashTicks == 0 || this.getDeltaMovement().horizontalDistanceSqr() < 0.01)) {
            this.dashTicks = 0;
            this.entityData.set(DASHING, false);
         }
      }
   }

   private void faceDashDirection() {
      float yaw = this.entityData.get(DASH_YAW);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
      this.yRotO = yaw;
      this.yBodyRotO = yaw;
      this.yHeadRotO = yaw;
      this.setXRot(0.0F);
   }

   public static double firstClawKnockbackStrength(LivingEntity recipient, double strength) {
      return FIRST_CLAW_TARGET.get() == recipient ? strength * 0.3 : strength;
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new StalkerNavigation(this, level);
   }

   public static boolean clawContact(LivingEntity attacker, LivingEntity victim, ServerLevel server, DamageSource source, float damage, boolean followup) {
      LivingEntity previous = FIRST_CLAW_TARGET.get();
      if (followup) {
         FIRST_CLAW_TARGET.remove();
      } else {
         FIRST_CLAW_TARGET.set(victim);
      }

      try {
         return victim.hurtServer(server, source, damage);
      } finally {
         if (previous == null) {
            FIRST_CLAW_TARGET.remove();
         } else {
            FIRST_CLAW_TARGET.set(previous);
         }
      }
   }

   public static DamageSource followupDamage(ServerLevel server, LivingEntity attacker) {
      return new DamageSource(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(FOLLOWUP), attacker);
   }

   private boolean withinClawReach(LivingEntity target) {
      return this.withinClawReach(target, 0.85);
   }

   private boolean withinClawReach(LivingEntity target, double reach) {
      AABB own = this.getBoundingBox();
      AABB other = target.getBoundingBox();
      if (!(other.minY > own.maxY + 0.35) && !(other.maxY < own.minY - 0.35)) {
         double dx = Math.max(0.0, Math.max(own.minX - other.maxX, other.minX - own.maxX));
         double dz = Math.max(0.0, Math.max(own.minZ - other.maxZ, other.minZ - own.maxZ));
         return dx * dx + dz * dz <= reach * reach;
      } else {
         return false;
      }
   }

   private void dash(LivingEntity target, boolean retreat) {
      if (this.onGround() && !this.isInWater() && !this.isPassenger()) {
         Vec3 direction = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
         if (direction.lengthSqr() < 0.001) {
            direction = this.getLookAngle().multiply(1.0, 0.0, 1.0);
         }

         direction = direction.normalize().scale(retreat ? -1.0 : 1.0);

         for (int step = 1; step <= 5; step++) {
            Vec3 offset = direction.scale(step * 0.5);
            BlockPos floor = BlockPos.containing(this.position().add(offset).add(0.0, -0.1, 0.0));
            if (!this.level().noCollision(this, this.getBoundingBox().move(offset))
               || !this.level().getBlockState(floor).isFaceSturdy(this.level(), floor, Direction.UP)) {
               return;
            }
         }

         this.getNavigation().stop();
         this.setDeltaMovement(direction.scale(retreat ? 0.95 : 0.85).add(0.0, 0.12, 0.0));
         this.needsSync = true;
         this.entityData.set(DASH_YAW, (float)(Math.atan2(direction.z, direction.x) * 180.0 / Math.PI) - 90.0F);
         this.faceDashDirection();
         this.dashTicks = 7;
         this.entityData.set(DASHING, true);
         this.playSound(ModSounds.STALKER_DASH, 0.8F, 1.0F);
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.STALKER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.STALKER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.STALKER_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.STALKER_STEP, 0.35F, 1.0F);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("movement", 3, state -> {
         state.controller().setAnimation(this.isDashing() ? DASH : (state.isMoving() ? (this.isAggressive() ? RUN : WALK) : IDLE));
         return PlayState.CONTINUE;
      }));
      controllers.add(new AnimationController("combat", 0, state -> PlayState.STOP).triggerableAnim("attack", ATTACK));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private final class ClawAttackGoal extends Goal {
      private LivingEntity victim;
      private int comboTick = -1;
      private int attackCooldown;
      private int dashCooldown;
      private int repathTicks;
      private boolean firstConnected;

      ClawAttackGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      private boolean valid(LivingEntity target) {
         return target != null
            && target.isAlive()
            && !target.isSpectator()
            && StalkerEntity.this.canAttack(target)
            && !(target instanceof Player player && player.isCreative());
      }

      @Override
      public boolean canUse() {
         return this.valid(StalkerEntity.this.getTarget());
      }

      @Override
      public boolean canContinueToUse() {
         return this.canUse();
      }

      @Override
      public void start() {
         StalkerEntity.this.setAggressive(true);
      }

      @Override
      public void stop() {
         StalkerEntity.this.setAggressive(false);
         StalkerEntity.this.getNavigation().stop();
         this.victim = null;
         this.comboTick = -1;
         this.firstConnected = false;
         this.repathTicks = 0;
      }

      @Override
      public void tick() {
         LivingEntity target = StalkerEntity.this.getTarget();
         if (this.valid(target)) {
            if (this.attackCooldown > 0) {
               this.attackCooldown--;
            }

            if (this.dashCooldown > 0) {
               this.dashCooldown--;
            }

            if (!StalkerEntity.this.isDashing()) {
               StalkerEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
               if (this.comboTick < 0) {
                  double distance = StalkerEntity.this.distanceToSqr(target);
                  boolean visible = StalkerEntity.this.getSensing().hasLineOfSight(target);
                  if (StalkerEntity.this.withinClawReach(target, 0.6) && visible && this.attackCooldown == 0) {
                     this.pursue(target, visible, 2);
                     this.victim = target;
                     this.comboTick = 0;
                     this.firstConnected = false;
                     this.attackCooldown = 30;
                     StalkerEntity.this.triggerAnim("combat", "attack");
                  } else if (visible && StalkerEntity.this.withinClawReach(target, 0.2)) {
                     StalkerEntity.this.getNavigation().stop();
                  } else {
                     this.pursue(target, visible, 6);
                     if (StalkerEntity.this.getHealth() > 12.0F && this.dashCooldown == 0 && distance >= 9.0 && distance <= 49.0 && visible) {
                        StalkerEntity.this.dash(target, false);
                        this.dashCooldown = 60;
                     }
                  }
               } else {
                  this.pursue(target, StalkerEntity.this.getSensing().hasLineOfSight(target), 2);
                  this.comboTick++;
                  if (this.comboTick == 4 || this.comboTick == 8) {
                     this.cut(this.comboTick == 8);
                  }

                  if (this.comboTick >= 8) {
                     this.comboTick = -1;
                     this.victim = null;
                     this.repathTicks = 0;
                     if (StalkerEntity.this.getHealth() <= 12.0F) {
                        StalkerEntity.this.dash(target, true);
                        this.dashCooldown = 45;
                     }
                  }
               }
            }
         }
      }

      private void pursue(LivingEntity target, boolean visible, int interval) {
         Vec3 step = target.position().subtract(StalkerEntity.this.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.35);
         BlockPos support = BlockPos.containing(StalkerEntity.this.position().add(step).add(0.0, -0.1, 0.0));
         if (!visible
            || !(StalkerEntity.this.distanceToSqr(target) < 64.0)
            || !(target.getY() - StalkerEntity.this.getY() < 0.5)
            || !(target.getY() - StalkerEntity.this.getY() > -1.5)
            || !StalkerEntity.this.level().noCollision(StalkerEntity.this, StalkerEntity.this.getBoundingBox().expandTowards(step).deflate(0.01))
            || !StalkerEntity.this.level().getBlockState(support).isFaceSturdy(StalkerEntity.this.level(), support, Direction.UP)
               && !StalkerEntity.this.level().getBlockState(support.below()).isFaceSturdy(StalkerEntity.this.level(), support.below(), Direction.UP)) {
            if (--this.repathTicks <= 0) {
               this.repathTicks = interval;
               StalkerEntity.this.getNavigation().moveTo(target, 1.2);
            }

            if (visible && StalkerEntity.this.distanceToSqr(target) < 16.0 && StalkerEntity.this.getNavigation().isDone()) {
               StalkerEntity.this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.2);
            }
         } else {
            StalkerEntity.this.getNavigation().stop();
            StalkerEntity.this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.2);
            this.repathTicks = 0;
         }
      }

      private void cut(boolean followup) {
         StalkerEntity.this.playSound(ModSounds.STALKER_ATTACK, 0.8F, 0.95F + StalkerEntity.this.random.nextFloat() * 0.1F);
         if (this.valid(this.victim) && this.victim == StalkerEntity.this.getTarget() && StalkerEntity.this.getSensing().hasLineOfSight(this.victim)) {
            if (StalkerEntity.this.withinClawReach(this.victim)) {
               ServerLevel server = (ServerLevel)StalkerEntity.this.level();
               DamageSource source = followup && this.firstConnected
                  ? new DamageSource(server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(StalkerEntity.FOLLOWUP), StalkerEntity.this)
                  : StalkerEntity.this.damageSources().mobAttack(StalkerEntity.this);
               LivingEntity previous = StalkerEntity.FIRST_CLAW_TARGET.get();
               if (followup) {
                  StalkerEntity.FIRST_CLAW_TARGET.remove();
               } else {
                  StalkerEntity.FIRST_CLAW_TARGET.set(this.victim);
               }

               boolean connected;
               try {
                  connected = this.victim.hurtServer(server, source, 4.0F);
               } finally {
                  if (previous == null) {
                     StalkerEntity.FIRST_CLAW_TARGET.remove();
                  } else {
                     StalkerEntity.FIRST_CLAW_TARGET.set(previous);
                  }
               }

               if (!followup) {
                  this.firstConnected = connected;
               }
            }
         }
      }
   }
}
