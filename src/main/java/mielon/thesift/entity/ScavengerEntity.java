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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ScavengerEntity extends Monster implements GeoEntity {
   public static final float MODEL_SCALE = 2.0F;
   public static final float WIDTH = 1.19F;
   public static final float HEIGHT = 1.2750001F;
   public static final float EYE_HEIGHT = 1.0625F;
   public static final double HOVER_HEIGHT = 0.8;
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("move");
   private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("shoot");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int shotWindup;
   private int impactDriftTicks;
   private Vec3 steeringTarget;
   private boolean movementDefaultsChecked;

   public ScavengerEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 5;
      this.setNoGravity(true);
      this.moveControl = new MoveControl(this) {
         @Override
         public void tick() {
            ScavengerEntity.this.steeringTarget = null;
            if (this.operation == Operation.MOVE_TO) {
               this.operation = Operation.WAIT;
               double dx = this.wantedX - ScavengerEntity.this.getX();
               double dz = this.wantedZ - ScavengerEntity.this.getZ();
               if (dx * dx + dz * dz > 0.025) {
                  ScavengerEntity.this.steeringTarget = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
                  float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                  ScavengerEntity.this.setYRot(this.rotlerp(ScavengerEntity.this.getYRot(), yaw, 60.0F));
                  double pace = ScavengerEntity.this.isAggressive() ? 1.0 : 0.5;
                  ScavengerEntity.this.setSpeed((float)(pace * this.speedModifier * ScavengerEntity.this.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                  return;
               }
            }

            ScavengerEntity.this.setSpeed(0.0F);
         }
      };
      this.getNavigation().setCanFloat(true);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
      this.setPathfindingMalus(PathType.LAVA, 0.0F);
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 35.0)
         .add(Attributes.MOVEMENT_SPEED, 0.32)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.STEP_HEIGHT, 0.0);
   }

   @Override
   public float getWalkTargetValue(BlockPos pos, LevelReader level) {
      return level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT) ? -level.getBrightness(LightLayer.BLOCK, pos) : super.getWalkTargetValue(pos, level);
   }

   public static boolean checkScavengerSpawnRules(
      EntityType<ScavengerEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return StalkerEntity.checkSculkerSpawnRules(level, reason, pos, random, true);
   }

   @Override
   public void checkDespawn() {
      if (!SculkerPopulation.despawnCrowded(this)) {
         super.checkDespawn();
      }
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      return !SculkerFamily.contains(target) && super.canAttack(target);
   }

   @Override
   protected boolean considersEntityAsAlly(Entity other) {
      return SculkerFamily.contains(other) || super.considersEntityAsAlly(other);
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new GroundPathNavigation(this, level) {
         @Override
         protected boolean canUpdatePath() {
            return !ScavengerEntity.this.isPassenger();
         }

         @Override
         protected Vec3 getTempMobPos() {
            return ScavengerEntity.this.position().add(0.0, -0.8, 0.0);
         }

         @Override
         protected void followThePath() {
            if (this.path != null && !this.path.isDone()) {
               Vec3 next = this.path.getNextEntityPos(ScavengerEntity.this);
               if (Math.abs(ScavengerEntity.this.getX() - next.x) < 0.65
                  && Math.abs(ScavengerEntity.this.getZ() - next.z) < 0.65
                  && Math.abs(ScavengerEntity.this.getY() - 0.8 - next.y) < 1.5) {
                  this.path.advance();
               }

               this.doStuckDetection(this.getTempMobPos());
            }
         }
      };
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(1, new ScavengerEntity.KeepDistanceGoal());
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      this.targetSelector
         .addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, (target, level) -> SifterFamily.isMember(target)));
   }

   @Override
   public void tick() {
      if (this.level() instanceof ServerLevel && !this.movementDefaultsChecked) {
         this.movementDefaultsChecked = true;
         AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
         if (health != null && Math.abs(health.getBaseValue() - 30.0) < 1.0E-5) {
            health.setBaseValue(35.0);
            if (this.getHealth() > 0.0F) {
               this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + 5.0F));
            }
         }

         AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null && Math.abs(speed.getBaseValue() - 0.27) < 1.0E-5) {
            speed.setBaseValue(0.32);
         }

         AttributeInstance step = this.getAttribute(Attributes.STEP_HEIGHT);
         if (step != null && Math.abs(step.getBaseValue() - 1.0) < 1.0E-5) {
            step.setBaseValue(0.0);
         }
      }

      super.tick();
      if (this.level() instanceof ServerLevel server && this.isAlive()) {
         if (this.shotWindup > 0 && --this.shotWindup == 0) {
            LivingEntity target = this.getTarget();
            if (this.validTarget(target) && this.getSensing().hasLineOfSight(target)) {
               ScavengerProjectileEntity projectile = new ScavengerProjectileEntity(ModEntities.SCAVENGER_PROJECTILE, server);
               Vec3 forward = target.getEyePosition().subtract(this.getEyePosition()).normalize();
               projectile.setPos(this.getEyePosition().add(forward.scale(0.8)));
               projectile.launch(this, target);
               server.addFreshEntity(projectile);
               this.playSound(ModSounds.SCAVENGER_PROJECTILE_LAUNCH, 0.8F, 0.75F + this.random.nextFloat() * 0.2F);
            }
         }
      }
   }

   @Override
   public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
      boolean damaged = super.hurtServer(server, source, amount);
      if (damaged && this.isAlive() && source.getDirectEntity() != null) {
         Entity direct = source.getDirectEntity();
         Vec3 direction;
         if (direct instanceof Projectile && direct.getDeltaMovement().lengthSqr() > 0.001) {
            direction = direct.getDeltaMovement().normalize();
         } else {
            Vec3 origin = direct instanceof LivingEntity living ? living.getEyePosition() : direct.position();
            direction = this.getBoundingBox().getCenter().subtract(origin).normalize();
         }

         double impulse = Mth.clamp(direction.y * 0.48, -0.45, 0.45);
         if (Math.abs(impulse) > 0.025) {
            this.setDeltaMovement(this.getDeltaMovement().x, Mth.clamp(this.getDeltaMovement().y + impulse, -0.55, 0.55), this.getDeltaMovement().z);
            this.impactDriftTicks = 14;
            this.needsSync = true;
            this.syncVelocity = true;
         }

         return damaged;
      } else {
         return damaged;
      }
   }

   @Override
   public boolean canStandOnFluid(FluidState state) {
      return !state.isEmpty();
   }

   private void hover() {
      if (this.impactDriftTicks > 0) {
         this.impactDriftTicks--;
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.9, 1.0));
         this.resetFallDistance();
      } else {
         Vec3 feet = this.position();
         BlockHitResult ground = this.level().clip(new ClipContext(feet.add(0.0, 0.2, 0.0), feet.add(0.0, -5.0, 0.0), Block.COLLIDER, Fluid.ANY, this));
         double groundY = ground.getType() == Type.MISS ? this.getY() - 3.0 : ground.getLocation().y;
         double desired = groundY + 0.8;
         if (this.steeringTarget != null) {
            Vec3 direction = this.steeringTarget.subtract(feet).multiply(1.0, 0.0, 1.0).normalize();

            for (double ahead : new double[]{0.9, 1.6, 2.35}) {
               Vec3 probe = feet.add(direction.scale(ahead));
               BlockHitResult step = this.level().clip(new ClipContext(probe.add(0.0, 1.3, 0.0), probe.add(0.0, -2.0, 0.0), Block.COLLIDER, Fluid.ANY, this));
               if (step.getType() != Type.MISS && step.getLocation().y <= groundY + 1.05) {
                  desired = Math.max(desired, step.getLocation().y + 0.8);
               }
            }

            double waypointY = this.steeringTarget.y + 0.8;
            if (this.steeringTarget.subtract(feet).horizontalDistanceSqr() < 4.0 && waypointY - this.getY() <= 1.15) {
               desired = Math.max(desired, waypointY);
            }
         }

         double rise = Math.max(0.0, desired - this.getY());
         if (rise > 0.0 && !this.level().noCollision(this, this.getBoundingBox().expandTowards(0.0, rise, 0.0).deflate(0.01))) {
            desired = this.getY();
         }

         double currentLift = this.getDeltaMovement().y;
         double acceleration = Mth.clamp((desired - this.getY()) * 0.04 - currentLift * 0.4, -0.014, 0.014);
         double lift = Mth.clamp(currentLift + acceleration, -0.14, 0.09);
         this.setDeltaMovement(this.getDeltaMovement().x, lift, this.getDeltaMovement().z);
         this.fallDistance = 0.0;
      }
   }

   @Override
   public void travel(Vec3 input) {
      if (this.isAlive() && !this.isNoAi() && !this.isPassenger() && this.level() instanceof ServerLevel) {
         this.hover();
      }

      this.moveRelative(0.2F, input);
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1.0, 0.8));
   }

   private boolean validTarget(LivingEntity target) {
      return target != null && target.isAlive() && this.canAttack(target) && !target.isSpectator() && !(target instanceof Player player && player.isCreative());
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.getDeltaMovement().horizontalDistanceSqr() > 0.001 && this.random.nextInt(3) == 0
         ? ModSounds.SCAVENGER_MOVEMENT
         : ModSounds.SCAVENGER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.SCAVENGER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.SCAVENGER_DEATH;
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("movement", 4, state -> {
         state.controller().setAnimation(this.getDeltaMovement().horizontalDistanceSqr() > 0.001 ? MOVE : IDLE);
         return PlayState.CONTINUE;
      }));
      controllers.add(new AnimationController("combat", 0, state -> PlayState.STOP).triggerableAnim("shoot", SHOOT));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private final class KeepDistanceGoal extends Goal {
      private int cooldown;
      private int visibleTicks;
      private int repath;

      KeepDistanceGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         return ScavengerEntity.this.validTarget(ScavengerEntity.this.getTarget());
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
         this.cooldown = 20;
         ScavengerEntity.this.setAggressive(true);
      }

      @Override
      public void stop() {
         ScavengerEntity.this.getNavigation().stop();
         ScavengerEntity.this.setAggressive(false);
         ScavengerEntity.this.shotWindup = 0;
         this.visibleTicks = 0;
         this.repath = 0;
      }

      @Override
      public void tick() {
         LivingEntity target = ScavengerEntity.this.getTarget();
         if (ScavengerEntity.this.validTarget(target)) {
            boolean visible = ScavengerEntity.this.getSensing().hasLineOfSight(target);
            this.visibleTicks = visible ? this.visibleTicks + 1 : 0;
            ScavengerEntity.this.getLookControl().setLookAt(target, 30.0F, 25.0F);
            if (this.cooldown > 0) {
               this.cooldown--;
            }

            if (--this.repath <= 0) {
               this.repath = 10;
               if (ScavengerEntity.this.distanceToSqr(target) < 36.0) {
                  this.retreatFrom(target);
               } else if (!(ScavengerEntity.this.distanceToSqr(target) > 196.0) && this.visibleTicks >= 8) {
                  ScavengerEntity.this.getNavigation().stop();
               } else {
                  ScavengerEntity.this.getNavigation().moveTo(target, 1.0);
               }
            }

            if (this.visibleTicks >= 8 && ScavengerEntity.this.distanceToSqr(target) <= 256.0 && this.cooldown == 0 && ScavengerEntity.this.shotWindup == 0) {
               ScavengerEntity.this.shotWindup = 6;
               this.cooldown = 55 + ScavengerEntity.this.random.nextInt(16);
               ScavengerEntity.this.triggerAnim("combat", "shoot");
            }
         }
      }

      private void retreatFrom(LivingEntity target) {
         Vec3 away = ScavengerEntity.this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();

         for (float angle : new float[]{0.0F, 1.0472F, -1.0472F}) {
            Vec3 escape = ScavengerEntity.this.position().add(away.yRot(angle).scale(4.5));
            BlockPos destination = BlockPos.containing(escape.x, ScavengerEntity.this.getY() - 0.8 + 0.2, escape.z);
            Path path = ScavengerEntity.this.getNavigation().createPath(destination, 0);
            if (path != null && path.canReach()) {
               ScavengerEntity.this.getNavigation().moveTo(path, 1.2);
               return;
            }
         }

         Vec3 escape = DefaultRandomPos.getPosAway(ScavengerEntity.this, 8, 2, target.position());
         if (escape != null) {
            ScavengerEntity.this.getNavigation().moveTo(escape.x, escape.y, escape.z, 1.2);
         }
      }
   }
}
