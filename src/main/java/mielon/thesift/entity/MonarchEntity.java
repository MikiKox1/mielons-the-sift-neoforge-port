package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import mielon.thesift.entity.ai.MonarchNavigation;
import mielon.thesift.fluid.IchorRegeneration;
import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MonarchEntity extends Monster implements GeoEntity {
   public static final float MODEL_SCALE = 2.0F;
   public static final float WIDTH = 1.45F;
   public static final float HEIGHT = 4.8125F;
   public static final float EYE_HEIGHT = 4.5F;
   public static final int EXPERIENCE = 441;
   public static final int BOSS_BAR_COLOR = 5220233;
   private static final double MOVEMENT_SPEED = 0.27;
   private static final double CHASE_SPEED = 1.1;
   private static final double DESPERATE_CHASE_SPEED = 1.3;
   public static final int OPENING_RISE = 0;
   public static final int OPENING_VOLLEY = 1;
   public static final int DIVE_WINDUP = 2;
   public static final int DIVE = 3;
   public static final int MELEE = 4;
   public static final int SPLIT_RISE = 5;
   public static final int ECHO_HOVER = 6;
   public static final int ECHO_WAIT = 7;
   public static final int CYCLE_RISE = 8;
   public static final int CYCLE_VOLLEY = 9;
   public static final int RANGED = 10;
   public static final int LAND = 11;
   public static final int SPLITTING = 12;
   public static final int ECHO_FORM = 13;
   public static final int ESCAPE = 14;
   private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(MonarchEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DASHING = SynchedEntityData.defineId(MonarchEntity.class, EntityDataSerializers.BOOLEAN);
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
   private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
   private static final RawAnimation DASH = RawAnimation.begin().thenLoop("dash");
   private static final RawAnimation DIVE_ANIM = RawAnimation.begin().thenPlayAndHold("dive");
   private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("dive_windup");
   private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("claw");
   private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("shoot");
   private static final RawAnimation SPLIT = RawAnimation.begin().thenPlay("split");
   private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("emerge");
   private static final RawAnimation WING_GUARD = RawAnimation.begin().thenPlay("wing_guard");
   private static final Identifier DEAD_OWNERS = Identifier.fromNamespaceAndPath("the_sift", "monarch_echo_links");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final ServerBossEvent boss = new ServerBossEvent(
      UUID.randomUUID(), Component.translatable("entity.the_sift.monarch"), BossBarColor.WHITE, BossBarOverlay.PROGRESS
   );
   private int actionTicks;
   private int phaseSeen;
   private int shots;
   private int attackCooldown;
   private int clawTick;
   private int dashTicks;
   private int dashCooldown = 60;
   private int shotCooldown;
   private boolean splitOnce;
   private boolean echoDefeated;
   private boolean firstClawConnected;
   private LivingEntity clawVictim;
   private UUID echoId;
   private UUID ownerId;
   private double flightY;
   private Vec3 splitOrigin = Vec3.ZERO;
   private Vec3 splitDestination = Vec3.ZERO;
   private int repathTicks;
   private int shelterCheckTicks;
   private int withdrawalTicks;
   private boolean shelteredTarget;
   private Vec3 withdrawalPoint;
   private Vec3 approachPoint;
   private Path approachPath;
   private int unreachableChecks;
   private int reachableChecks;
   private int orbitDirection = 1;
   private Vec3 committedDive = Vec3.ZERO;
   private Vec3 dashVelocity = Vec3.ZERO;
   private BlockPos echoPosition = BlockPos.ZERO;
   private int loadGrace = 60;
   private int stuckTicks;
   private int escapeCooldown;
   private int escapeStage;
   private int escapeReturnAction = 4;
   private int basinTicks;
   private double bestEscapeGap = Double.MAX_VALUE;
   private Vec3 escapeProgress;
   private Vec3 escapeLanding = Vec3.ZERO;
   private double escapeLiftY;
   private final List<MonarchEntity.SummonSlot> summons = new ArrayList<>();

   public MonarchEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = type == ModEntities.MONARCH_ECHO ? 0 : 441;
      if (type == ModEntities.MONARCH_ECHO) {
         this.entityData.set(ACTION, 4);
      }

      this.setPersistenceRequired();
      this.moveControl = new MoveControl(this) {
         @Override
         public void tick() {
            if (this.operation == Operation.MOVE_TO) {
               this.operation = Operation.WAIT;
               double dx = this.wantedX - MonarchEntity.this.getX();
               double dz = this.wantedZ - MonarchEntity.this.getZ();
               if (dx * dx + dz * dz > 0.015) {
                  float yaw = (float)(Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                  MonarchEntity.this.setYRot(this.rotlerp(MonarchEntity.this.getYRot(), yaw, 35.0F));
                  MonarchEntity.this.setSpeed((float)(this.speedModifier * MonarchEntity.this.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                  return;
               }
            }

            MonarchEntity.this.setSpeed(0.0F);
         }
      };
      this.getNavigation().setCanFloat(true);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
      this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
      this.setPathfindingMalus(PathType.LAVA, 0.0F);
   }

   public static Builder createAttributes() {
      return attributes(400.0, 8.0);
   }

   public static Builder createEchoAttributes() {
      return attributes(100.0, 5.0);
   }

   private static Builder attributes(double health, double damage) {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, health)
         .add(Attributes.ATTACK_DAMAGE, damage)
         .add(Attributes.MOVEMENT_SPEED, 0.27)
         .add(Attributes.FOLLOW_RANGE, 64.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ACTION, 0);
      builder.define(DASHING, false);
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new MonarchNavigation(this, level);
   }

   @Override
   public int getMaxFallDistance() {
      return 32;
   }

   @Override
   public void travel(Vec3 input) {
      if (!this.isNoGravity() && IchorRegeneration.isTouching(this)) {
         this.moveRelative(0.1F, input);
         this.move(MoverType.SELF, this.getDeltaMovement());
         Vec3 velocity = this.getDeltaMovement();
         boolean advancing = this.getSpeed() > 0.01F || !this.getNavigation().isDone();
         boolean swimUp = advancing && (this.getMoveControl().getWantedY() > this.getY() + 0.05 || this.horizontalCollision)
            || this.isEyeInFluid(FluidTags.WATER);
         double vertical = swimUp ? Math.min(0.3, velocity.y * 0.8 + 0.09) : velocity.y * 0.8 - 0.02;
         this.setDeltaMovement(velocity.x * 0.9, vertical, velocity.z * 0.9);
         this.resetFallDistance();
      } else {
         super.travel(input);
      }
   }

   @Override
   protected void registerGoals() {
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.MONARCH_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.MONARCH_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.MONARCH_DEATH;
   }

   @Override
   public float getVoicePitch() {
      return super.getVoicePitch() * 0.8F;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      if (!this.isFlying()) {
         this.playSound(ModSounds.MONARCH_STEP, 0.5F, 0.8F);
      }
   }

   @Override
   public boolean fireImmune() {
      return true;
   }

   @Override
   public boolean isAffectedByPotions() {
      return false;
   }

   @Override
   public boolean canBeAffected(MobEffectInstance effect) {
      return false;
   }

   @Override
   public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
      return !source.is(DamageTypeTags.IS_FIRE)
            && !source.is(DamageTypeTags.IS_FALL)
            && !source.is(DamageTypes.MAGIC)
            && !source.is(DamageTypes.INDIRECT_MAGIC)
         ? super.hurtServer(server, source, amount)
         : false;
   }

   public boolean blockIncomingArrow() {
      if (this.isAlive() && this.random.nextBoolean()) {
         this.triggerAnim("guard", "wing_guard");
         this.playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0F, 0.7F);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public ProjectileDeflection deflection(Projectile projectile) {
      return projectile instanceof AbstractArrow && this.blockIncomingArrow() ? ProjectileDeflection.REVERSE : super.deflection(projectile);
   }

   public boolean isEcho() {
      return this.getType() == ModEntities.MONARCH_ECHO;
   }

   public int action() {
      return this.entityData.get(ACTION);
   }

   public int phase() {
      if (this.isEcho()) {
         return 0;
      }

      float fraction = this.getHealth() / this.getMaxHealth();
      return fraction <= 0.1F ? 4 : (fraction <= 0.25F ? 3 : (fraction <= 0.5F ? 2 : (fraction <= 0.75F ? 1 : 0)));
   }

   public boolean isFlying() {
      int a = this.action();
      return a != 4 && a != 10 || a == 10 && !this.onGround();
   }

   public int activeSummons() {
      return this.summons.size();
   }

   public UUID echoUuid() {
      return this.echoId;
   }

   @Override
   public boolean canAttack(LivingEntity other) {
      return !SculkerFamily.contains(other) && super.canAttack(other);
   }

   @Override
   protected boolean considersEntityAsAlly(Entity other) {
      return SculkerFamily.contains(other) || super.considersEntityAsAlly(other);
   }

   @Override
   protected boolean isAlwaysExperienceDropper() {
      return !this.isEcho();
   }

   @Override
   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      this.updateBoss();
      this.boss.addPlayer(player);
   }

   @Override
   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      this.boss.removePlayer(player);
   }

   private void updateBoss() {
      this.boss.setName(Component.translatable(this.isEcho() ? "entity.the_sift.monarch_echo" : "entity.the_sift.monarch"));
      this.boss.setColor(BossBarColor.WHITE);
      this.boss.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
      this.boss.setVisible(this.isAlive());
   }

   private boolean valid(LivingEntity target) {
      return target != null
         && target.isAlive()
         && target.level() == this.level()
         && !target.isSpectator()
         && !(target instanceof Player p && p.isCreative())
         && !SculkerFamily.contains(target);
   }

   private void changeAction(int value) {
      this.entityData.set(ACTION, value);
      this.actionTicks = 0;
      this.shots = 0;
      this.shotCooldown = 0;
      this.shelterCheckTicks = 0;
      this.shelteredTarget = false;
      this.withdrawalPoint = null;
      this.withdrawalTicks = 0;
      this.unreachableChecks = 0;
      this.reachableChecks = 0;
      this.approachPoint = null;
      this.approachPath = null;
      this.dashTicks = 0;
      this.entityData.set(DASHING, false);
      this.clawTick = 0;
      this.clawVictim = null;
      this.firstClawConnected = false;
      this.getNavigation().stop();
      this.setDeltaMovement(Vec3.ZERO);
      this.setNoGravity(value != 4 && value != 10);
      this.resetFallDistance();
   }

   private void rise(int action) {
      this.flightY = this.clearFlightHeight(10.0);
      this.changeAction(action);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel server) {
         this.updateBoss();
         if (this.isAlive() && !this.isNoAi()) {
            if (this.loadGrace > 0) {
               this.loadGrace--;
            }

            if (this.isEcho() && this.ownerId != null && this.ownerDied(server)) {
               this.dieWithOwner(server);
            } else {
               this.refreshSummons(server);
               if (!this.valid(this.getTarget())) {
                  this.setTarget(
                     server.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 64.0, p -> p instanceof LivingEntity livingx && this.valid(livingx))
                  );
               }

               LivingEntity target = this.getTarget();
               int phase = this.phase();
               if (!this.isEcho() && phase > this.phaseSeen && this.action() != 14) {
                  this.phaseSeen = phase;
                  if (phase == 1 && !this.splitOnce) {
                     this.rise(5);
                  } else if (phase == 2) {
                     this.rise(8);
                  } else if (phase == 3) {
                     this.changeAction(10);
                     this.setNoGravity(!this.onGround());
                  } else if (phase == 4) {
                     if (!this.onGround()) {
                        this.beginDive(target);
                     } else {
                        this.changeAction(4);
                     }
                  }
               }

               if (this.attackCooldown > 0) {
                  this.attackCooldown--;
               }

               if (this.dashCooldown > 0) {
                  this.dashCooldown--;
               }

               this.actionTicks++;
               if (this.isFlying()) {
                  this.resetFallDistance();
               }

               this.setAggressive(this.valid(target));
               if (this.valid(target)) {
                  this.getLookControl().setLookAt(target, 35.0F, 30.0F);
                  if (this.isFlying() && this.action() != 3) {
                     this.face(target.position());
                  }
               } else if (this.action() == 4) {
                  this.getNavigation().stop();
               }

               switch (this.action()) {
                  case 0:
                  case 5:
                  case 8:
                     this.ascend(server, target);
                     break;
                  case 1:
                     this.hover();
                     if (this.valid(target) && this.shots < 3 && this.actionTicks >= 15 + this.shots * 18 && this.shoot(server, target, true)) {
                        this.shots++;
                     }

                     if (this.shots >= 3 && this.summons.stream().noneMatch(s -> s.projectile)) {
                        this.beginDive(target);
                     }
                     break;
                  case 2:
                     this.setDeltaMovement(Vec3.ZERO);
                     if (this.actionTicks >= 6) {
                        this.changeAction(3);
                        this.dashVelocity = this.committedDive.subtract(this.position()).normalize().scale(1.9);
                     }
                     break;
                  case 3:
                     this.face(this.committedDive);
                     this.setDeltaMovement(this.dashVelocity);
                     this.dashTrail(server, this.dashVelocity);
                     if (this.position().distanceToSqr(this.committedDive) < 2.4
                        || this.verticalCollision
                        || this.horizontalCollision
                        || this.actionTicks >= 35) {
                        this.impact(server);
                        this.changeAction(4);
                        this.attackCooldown = 12;
                     }
                     break;
                  case 4:
                     if (this.valid(target)) {
                        this.melee(server, target);
                     }

                     if (!this.isEcho() && phase == 2 && !this.shelteredTarget && this.actionTicks >= 170) {
                        this.rise(8);
                     }
                     break;
                  case 6:
                     this.hover();
                     Entity echo = this.echoId == null ? null : server.getEntity(this.echoId);
                     if (echo != null) {
                        this.echoPosition = echo.blockPosition();
                     }

                     if (!this.echoDefeated
                        && (
                           echo instanceof LivingEntity living && !living.isAlive()
                              || echo == null && this.loadGrace == 0 && server.hasChunkAt(this.echoPosition)
                        )) {
                        this.echoDefeated = true;
                     }

                     if (this.echoDefeated) {
                        this.changeAction(7);
                     } else if (this.valid(target) && --this.shotCooldown <= 0 && this.summons.size() < 2) {
                        this.shoot(server, target, true);
                        this.shotCooldown = 25;
                     }
                     break;
                  case 7:
                     this.hover();
                     if (this.actionTicks >= 60) {
                        this.beginDive(target);
                     }
                     break;
                  case 9:
                     this.hover();
                     if (this.valid(target) && this.shots < 5 && --this.shotCooldown <= 0) {
                        boolean summon = this.shots == 2 && this.summons.size() < 4;
                        this.shoot(server, target, summon);
                        this.shots++;
                        this.shotCooldown = 24;
                     }

                     if (this.shots >= 5 && this.shotCooldown-- <= 8) {
                        this.beginDive(target);
                     }
                     break;
                  case 10:
                     this.ranged(server, target);
                     break;
                  case 11:
                     Vec3 delta = this.committedDive.subtract(this.position());
                     this.setDeltaMovement(delta.normalize().scale(Math.min(0.5, delta.length())));
                     if (delta.lengthSqr() < 0.5 || this.verticalCollision || this.actionTicks > 60) {
                        this.changeAction(10);
                        this.setNoGravity(false);
                        this.attackCooldown = 12;
                     }
                     break;
                  case 12:
                     this.split(server, target);
                     break;
                  case 13:
                     this.separate(28);
                     if (this.actionTicks >= 28) {
                        this.beginDive(target);
                     }
                     break;
                  case 14:
                     this.terrainEscape();
                     break;
                  default:
                     this.changeAction(4);
               }

               this.watchForTerrainTrap(target);
            }
         }
      }
   }

   private double clearFlightHeight(double rise) {
      double desired = this.getY() + rise;
      AABB flightBox = new AABB(
         this.getX() - this.getBbWidth() / 2.0,
         this.getY(),
         this.getZ() - this.getBbWidth() / 2.0,
         this.getX() + this.getBbWidth() / 2.0,
         this.getY() + 5.6875,
         this.getZ() + this.getBbWidth() / 2.0
      );
      AABB probe = flightBox.expandTowards(0.0, rise + 3.0, 0.0);

      for (VoxelShape shape : this.level().getBlockCollisions(this, probe)) {
         for (AABB box : shape.toAabbs()) {
            if (!(box.maxX <= flightBox.minX)
               && !(box.minX >= flightBox.maxX)
               && !(box.maxZ <= flightBox.minZ)
               && !(box.minZ >= flightBox.maxZ)
               && box.minY >= this.getY() + 0.1) {
               desired = Math.min(desired, box.minY - 3.0 - 5.6875);
            }
         }
      }

      return Math.max(this.getY(), desired);
   }

   private void ascend(ServerLevel server, LivingEntity target) {
      if (this.actionTicks == 1 && this.flightY < this.getY() + 0.01) {
         this.flightY = this.clearFlightHeight(10.0);
      }

      this.flightY = Math.min(this.flightY, this.clearFlightHeight(Math.max(0.0, this.flightY - this.getY())));
      this.getNavigation().stop();
      this.setNoGravity(true);
      double dy = this.flightY - this.getY();
      this.setDeltaMovement(0.0, Math.min(0.6, Math.max(0.0, dy * 0.24)), 0.0);
      if (dy <= 0.12 || this.actionTicks >= 70) {
         int previous = this.action();
         this.setDeltaMovement(Vec3.ZERO);
         if (previous == 5) {
            this.splitOrigin = this.position();
            this.splitDestination = this.position();
            this.changeAction(12);
         } else {
            this.changeAction(previous == 0 ? 1 : 9);
         }
      }
   }

   private void hover() {
      this.getNavigation().stop();
      this.setNoGravity(true);
      this.setDeltaMovement(Vec3.ZERO);
   }

   private void beginDive(LivingEntity target) {
      this.committedDive = this.valid(target) ? target.position() : this.position().add(0.0, -10.0, 0.0);
      this.changeAction(2);
   }

   private void impact(ServerLevel server) {
      server.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.15, this.getZ(), 50, 2.0, 0.15, 2.0, 0.08);
      server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.1, this.getZ(), 35, 1.8, 0.1, 1.8, 0.04);

      for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0, 2.0, 5.0), this::valid)) {
         double distance = victim.position().subtract(this.position()).horizontalDistance();
         if (!(distance > 6.0) && this.getSensing().hasLineOfSight(victim)) {
            float strength = (float)(1.0 - distance / 6.0);
            victim.hurtServer(server, this.damageSources().mobAttack(this), (this.isEcho() ? 4.0F : 6.0F) + 4.0F * strength);
            Vec3 away = victim.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 0.001) {
               away = new Vec3(1.0, 0.0, 0.0);
            }

            victim.knockback(0.6 + 3.6 * strength, -away.x, -away.z, this.damageSources().mobAttack(this), 0.0F);
            Vec3 launched = victim.getDeltaMovement();
            victim.setDeltaMovement(launched.x, Math.max(launched.y, 0.3 + 0.65 * strength), launched.z);
            victim.needsSync = true;
            victim.hurtMarked = true;
         }
      }

      this.playSound(ModSounds.MONARCH_PROJECTILE_EXPLODE, 1.2F, 0.65F);
   }

   private void melee(ServerLevel server, LivingEntity target) {
      this.setNoGravity(false);
      if (!this.withdrawFromShelter(target)) {
         if (this.clawTick > 0) {
            if (this.valid(this.clawVictim)) {
               this.pursue(this.clawVictim, true);
            }

            if (++this.clawTick == 5) {
               this.clawContact(server, false);
            } else if (this.clawTick == 9) {
               this.clawContact(server, true);
            }

            if (this.clawTick >= 15) {
               this.clawTick = 0;
               this.clawVictim = null;
               this.firstClawConnected = false;
            }
         } else if (this.dashTicks > 0) {
            if ((this.isEcho() || this.phase() != 2) && this.canStartClaws(target) && this.attackCooldown == 0) {
               this.dashTicks = 0;
               this.entityData.set(DASHING, false);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.35, 1.0, 0.35));
               this.beginClaws(target, 24);
            } else {
               this.setDeltaMovement(this.dashVelocity.x, this.getDeltaMovement().y, this.dashVelocity.z);
               this.dashTrail(server, this.dashVelocity);
               this.face(this.position().add(this.dashVelocity));
               if (--this.dashTicks == 0 || this.horizontalCollision) {
                  this.dashTicks = 0;
                  this.entityData.set(DASHING, false);
               }
            }
         } else if (this.canStartClaws(target) && this.attackCooldown == 0) {
            this.beginClaws(target, this.isEcho() ? 30 : 28);
         } else {
            boolean retreat = !this.isEcho() && this.phase() == 2;
            double distance = this.distanceToSqr(target);
            if (this.dashCooldown == 0 && this.onGround() && (retreat ? distance < 49.0 : distance > 10.0 && distance < 225.0)) {
               Vec3 direction = (retreat ? this.position().subtract(target.position()) : target.position().subtract(this.position()))
                  .multiply(1.0, 0.0, 1.0)
                  .normalize();
               Vec3 destination = this.position().add(direction.scale((retreat ? 0.85 : 1.05) * 9.0));
               if (this.canWalkDirectly(this.position(), destination)) {
                  this.getNavigation().stop();
                  this.dashVelocity = direction.scale(retreat ? 0.85 : 1.05);
                  this.dashTicks = 9;
                  this.dashCooldown = retreat ? 130 : (this.phase() == 4 ? 85 : 120);
                  this.entityData.set(DASHING, true);
                  this.playSound(ModSounds.MONARCH_DASH, 1.0F, 0.7F);
                  return;
               }

               this.dashCooldown = 15;
            }

            this.pursue(target, false);
         }
      }
   }

   public boolean canWalkDirectly(Vec3 from, Vec3 to) {
      double length = to.subtract(from).horizontalDistance();
      if (!(length > 64.0) && !(to.y - from.y > 12.0)) {
         int samples = Math.max(1, (int)Math.ceil(length / 0.45));
         Vec3 previous = from;

         for (int i = 1; i <= samples; i++) {
            double t = (double)i / samples;
            Vec3 next = this.standingAt(Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.z, to.z), previous.y, 24.0);
            if (next == null || next.y - previous.y > 1.05) {
               return false;
            }

            Vec3 upper = new Vec3(previous.x, Math.max(previous.y, next.y), previous.z);
            Vec3 horizontal = new Vec3(next.x - upper.x, 0.0, next.z - upper.z);
            if (!this.level().noCollision(this, this.bodyAt(upper).expandTowards(horizontal).deflate(0.015))) {
               return false;
            }

            previous = next;
         }

         return Math.abs(previous.y - to.y) < 1.25;
      } else {
         return false;
      }
   }

   private AABB bodyAt(Vec3 feet) {
      return this.getBoundingBox().move(feet.subtract(this.position()));
   }

   private Vec3 standingAt(double x, double z, double referenceY, double drop) {
      double half = this.getBbWidth() * 0.5;
      double top = referenceY + 1.01;
      double bottom = referenceY - drop;

      for (int y = Mth.floor(top); y >= Mth.floor(bottom); y--) {
         double best = -Double.MAX_VALUE;

         for (int bx = Mth.floor(x - half); bx <= Mth.floor(x + half); bx++) {
            for (int bz = Mth.floor(z - half); bz <= Mth.floor(z + half); bz++) {
               BlockPos p = new BlockPos(bx, y, bz);

               for (AABB box : this.level().getBlockState(p).getCollisionShape(this.level(), p).toAabbs()) {
                  double height = y + box.maxY;
                  if (height <= top + 0.001
                     && height >= bottom
                     && box.maxX + bx > x - half
                     && box.minX + bx < x + half
                     && box.maxZ + bz > z - half
                     && box.minZ + bz < z + half) {
                     best = Math.max(best, height);
                  }
               }
            }
         }

         if (best > -Double.MAX_VALUE) {
            Vec3 point = new Vec3(x, best, z);
            if (this.level().noCollision(this, this.bodyAt(point).deflate(0.015))) {
               return point;
            }
         }
      }

      return null;
   }

   private boolean bodyFitsPath(Path path) {
      if (path != null && path.canReach()) {
         for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); i++) {
            Vec3 point = path.getEntityPosAtNode(this, i);
            if (!this.level().noCollision(this, this.bodyAt(point).deflate(0.025))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean findMeleeApproach(LivingEntity target) {
      this.approachPoint = null;
      this.approachPath = null;
      if (this.canSwimTowards(target)) {
         this.approachPoint = target.position();
         return true;
      }

      Vec3 towards = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
      Vec3 near = target.position().subtract(towards.scale(1.4));
      Vec3[] candidates = new Vec3[]{
         near,
         target.position(),
         target.position().add(1.4, 0.0, 0.0),
         target.position().add(-1.4, 0.0, 0.0),
         target.position().add(0.0, 0.0, 1.4),
         target.position().add(0.0, 0.0, -1.4)
      };

      for (Vec3 candidate : candidates) {
         Vec3 point = this.standingAt(candidate.x, candidate.z, target.getY(), 1.4);
         if (point != null && Math.abs(point.y - target.getY()) < 1.5 && this.canWalkDirectly(this.position(), point)) {
            this.approachPoint = point;
            return true;
         }
      }

      for (Vec3 candidate : candidates) {
         Vec3 point = this.standingAt(candidate.x, candidate.z, target.getY(), 1.4);
         if (point != null && !(Math.abs(point.y - target.getY()) >= 1.5)) {
            Path path = this.getNavigation().createPath(BlockPos.containing(point), 0);
            if (this.bodyFitsPath(path)) {
               this.approachPoint = point;
               this.approachPath = path;
               return true;
            }
         }
      }

      return false;
   }

   private boolean withdrawFromShelter(LivingEntity target) {
      if (--this.shelterCheckTicks <= 0) {
         this.shelterCheckTicks = 12;
         boolean reachable = this.canStartClaws(target) || this.findMeleeApproach(target);
         if (reachable) {
            this.unreachableChecks = 0;
            this.reachableChecks++;
            if (!this.shelteredTarget || this.reachableChecks >= 2 || this.approachPoint != null && this.approachPath == null) {
               this.shelteredTarget = false;
               this.withdrawalPoint = null;
               this.withdrawalTicks = 0;
            }
         } else {
            this.reachableChecks = 0;
            if (++this.unreachableChecks >= 2 && !this.shelteredTarget) {
               this.shelteredTarget = true;
               this.withdrawalTicks = 0;
               this.shotCooldown = 16;
            }
         }
      }

      if (!this.shelteredTarget) {
         return false;
      }

      this.dashTicks = 0;
      this.entityData.set(DASHING, false);
      this.clawTick = 0;
      this.clawVictim = null;
      this.firstClawConnected = false;
      this.face(target.position());
      this.tacticalMovement(target);
      if (--this.shotCooldown <= 0 && this.position().subtract(target.position()).horizontalDistance() > 6.0) {
         Vec3 start = this.position().add(0.0, 4.5, 0.0);
         BlockHitResult hit = this.level().clip(new ClipContext(start, target.getBoundingBox().getCenter(), Block.COLLIDER, Fluid.NONE, this));
         if (!this.getSensing().hasLineOfSight(target)
            && !(hit.getLocation().distanceToSqr(target.position()) < 25.0)
            && !(hit.getLocation().distanceToSqr(start) > 9.0)) {
            this.shotCooldown = 12;
         } else {
            this.shoot((ServerLevel)this.level(), target, false);
            this.shotCooldown = 38 + this.random.nextInt(15);
         }
      }

      this.dashCooldown = Math.max(this.dashCooldown, 40);
      return true;
   }

   private void tacticalMovement(LivingEntity target) {
      if (this.withdrawalPoint != null && this.position().distanceToSqr(this.withdrawalPoint) < 1.2) {
         this.withdrawalTicks = 0;
      }

      if (--this.withdrawalTicks > 0 && this.withdrawalPoint != null) {
         if (this.getNavigation().isDone()) {
            this.getMoveControl().setWantedPosition(this.withdrawalPoint.x, this.withdrawalPoint.y, this.withdrawalPoint.z, 1.15);
         }
      } else {
         this.withdrawalTicks = 28 + this.random.nextInt(13);
         this.withdrawalPoint = null;
         Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
         if (away.lengthSqr() < 0.01) {
            away = new Vec3(1.0, 0.0, 0.0);
         }

         double distance = this.position().subtract(target.position()).horizontalDistance();
         double best = -Double.MAX_VALUE;
         Path chosenPath = null;

         for (float angle : new float[]{
            0.55F * this.orbitDirection, 1.05F * this.orbitDirection, 0.0F, -0.55F * this.orbitDirection, 1.6F * this.orbitDirection
         }) {
            for (double radius : new double[]{10.0, 8.0, 12.0}) {
               Vec3 candidate = target.position().add(away.yRot(angle).scale(radius));
               Vec3 point = this.standingAt(candidate.x, candidate.z, this.getY(), 6.0);
               if (point != null && !(point.distanceToSqr(this.position()) < 2.0)) {
                  double safety = point.subtract(target.position()).horizontalDistance();
                  if (!(safety < 7.0) && (!(distance < 7.0) || !(safety < distance + 1.0))) {
                     boolean direct = this.canWalkDirectly(this.position(), point);
                     Path path = direct ? null : this.getNavigation().createPath(BlockPos.containing(point), 0);
                     if (direct || this.bodyFitsPath(path)) {
                        BlockHitResult ray = this.level()
                           .clip(new ClipContext(point.add(0.0, 4.5, 0.0), target.getEyePosition(), Block.COLLIDER, Fluid.NONE, this));
                        double score = (ray.getType() == Type.MISS ? 4.0 : 0.0)
                           - Math.abs(safety - 10.0) * 0.6
                           - Math.abs(angle - 0.55 * this.orbitDirection) * 2.0
                           + Math.min(6.0, point.distanceTo(this.position())) * 0.12;
                        if (score > best) {
                           best = score;
                           this.withdrawalPoint = point;
                           chosenPath = path;
                        }
                     }
                  }
               }
            }
         }

         if (this.withdrawalPoint != null) {
            if (chosenPath != null) {
               this.getNavigation().moveTo(chosenPath, 1.15);
            } else {
               this.getNavigation().stop();
               this.getMoveControl().setWantedPosition(this.withdrawalPoint.x, this.withdrawalPoint.y, this.withdrawalPoint.z, 1.15);
            }
         } else {
            this.orbitDirection = -this.orbitDirection;
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
         }
      }
   }

   private boolean canSwimTowards(LivingEntity target) {
      if (!IchorRegeneration.isTouching(this)) {
         return false;
      }

      BlockHitResult hit = this.level()
         .clip(new ClipContext(this.position().add(0.0, this.getBbHeight() + 2.0, 0.0), this.position().add(0.0, -1.0, 0.0), Block.COLLIDER, Fluid.ANY, this));
      if (hit.getType() != Type.BLOCK) {
         return false;
      }

      FluidState fluid = this.level().getFluidState(hit.getBlockPos());
      if (!IchorWaterlogging.isIchor(fluid.getType())) {
         return false;
      }

      double surface = hit.getBlockPos().getY() + fluid.getHeight(this.level(), hit.getBlockPos());
      if (target.getY() > surface + 0.75) {
         return false;
      }

      double height = Math.max(this.getY(), target.getY() + 0.3);
      Vec3 lifted = new Vec3(this.getX(), height, this.getZ());
      if (!this.level().noCollision(this, this.bodyAt(this.position()).expandTowards(0.0, height - this.getY(), 0.0).deflate(0.015))) {
         return false;
      }

      Vec3 direction = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
      double stop = (this.getBbWidth() + target.getBbWidth()) * 0.5 + 0.35;
      Vec3 end = target.position().subtract(direction.normalize().scale(stop));
      int count = Math.max(1, (int)Math.ceil(direction.horizontalDistance() / 0.45));

      for (int i = 1; i <= count; i++) {
         Vec3 point = lifted.lerp(new Vec3(end.x, height, end.z), (double)i / count);
         if (!this.level().noCollision(this, this.bodyAt(point).deflate(0.015))) {
            return false;
         }
      }

      return true;
   }

   private void watchForTerrainTrap(LivingEntity target) {
      if (this.escapeCooldown > 0) {
         this.escapeCooldown--;
      }

      if ((this.action() == 4 || this.action() == 10 && !this.isNoGravity()) && this.valid(target) && !this.canStartClaws(target)) {
         if (this.escapeProgress != null
            && !(this.position().subtract(this.escapeProgress).horizontalDistanceSqr() > 0.36)
            && !(this.getY() > this.escapeProgress.y + 0.6)) {
            this.stuckTicks++;
         } else {
            this.escapeProgress = this.position();
            this.stuckTicks = 0;
         }

         boolean belowBank = target.getY() > this.getY() + 1.05 || IchorRegeneration.isTouching(this);
         double gap = this.distanceTo(target);
         if (belowBank && !(gap < this.bestEscapeGap - 2.0)) {
            this.basinTicks++;
         } else {
            this.bestEscapeGap = gap;
            this.basinTicks = 0;
         }

         if (this.escapeCooldown <= 0 && (this.stuckTicks >= 40 || this.basinTicks >= 80)) {
            this.stuckTicks = 0;
            this.basinTicks = 0;
            this.escapeCooldown = 40;
            this.tryTerrainEscape(target);
         }
      } else {
         this.escapeProgress = this.position();
         this.stuckTicks = 0;
         this.basinTicks = 0;
         this.bestEscapeGap = Double.MAX_VALUE;
      }
   }

   private AABB escapeBodyAt(Vec3 feet) {
      return new AABB(
         feet.x - this.getBbWidth() * 0.5,
         feet.y,
         feet.z - this.getBbWidth() * 0.5,
         feet.x + this.getBbWidth() * 0.5,
         feet.y + 5.6875 + 0.35,
         feet.z + this.getBbWidth() * 0.5
      );
   }

   private boolean clearEscapeSegment(Vec3 start, Vec3 end) {
      int count = Math.max(1, (int)Math.ceil(start.distanceTo(end) / 0.35));

      for (int i = 0; i <= count; i++) {
         if (!this.level().noCollision(this, this.escapeBodyAt(start.lerp(end, (double)i / count)).deflate(0.015))) {
            return false;
         }
      }

      return true;
   }

   private boolean roomyLanding(Vec3 point) {
      int neighbours = 0;

      for (Vec3 direction : new Vec3[]{new Vec3(1.5, 0.0, 0.0), new Vec3(-1.5, 0.0, 0.0), new Vec3(0.0, 0.0, 1.5), new Vec3(0.0, 0.0, -1.5)}) {
         Vec3 next = this.standingAt(point.x + direction.x, point.z + direction.z, point.y, 0.6);
         if (next != null && Math.abs(next.y - point.y) < 0.6) {
            neighbours++;
         }
      }

      return neighbours >= 3;
   }

   private boolean tryTerrainEscape(LivingEntity target) {
      double lift = 0.0;

      for (double height = 0.5; height <= 12.0 && this.clearEscapeSegment(this.position(), this.position().add(0.0, height, 0.0)); height += 0.5) {
         lift = height;
      }

      if (lift < 2.0) {
         return false;
      }

      Vec3 best = null;
      double bestScore = -Double.MAX_VALUE;
      Set<Integer> raisedSides = new HashSet<>();
      boolean fluid = IchorRegeneration.isTouching(this);

      for (int side = 0; side < 16; side++) {
         double angle = side * Math.PI / 8.0;

         for (double radius : new double[]{2.0, 3.0, 4.0, 6.0, 8.0}) {
            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            Vec3 point = this.standingAt(x, z, this.getY() + lift - 1.01, lift - 0.75);
            if (point != null
               && !(point.y <= this.getY() + 1.05)
               && !(point.y + 0.7 > this.getY() + lift)
               && this.roomyLanding(point)
               && !this.canWalkDirectly(this.position(), point)) {
               Vec3 above = point.add(0.0, 0.7, 0.0);
               Vec3 takeoff = new Vec3(this.getX(), above.y, this.getZ());
               if (this.clearEscapeSegment(this.position(), takeoff) && this.clearEscapeSegment(takeoff, above) && this.clearEscapeSegment(above, point)) {
                  raisedSides.add(side / 2);
                  double score = -point.distanceTo(this.position()) + (point.y - this.getY()) * 0.15;
                  if (this.valid(target)) {
                     score -= point.distanceTo(target.position()) * 0.2;
                  }

                  if (score > bestScore) {
                     best = point;
                     bestScore = score;
                  }
               }
            }
         }
      }

      if (best != null && (fluid || this.valid(target) && !(target.getY() < this.getY() + 1.0) || raisedSides.size() >= 3)) {
         int resume = this.action() == 10 ? 10 : 4;
         this.changeAction(14);
         this.escapeReturnAction = resume;
         this.escapeLanding = best;
         this.escapeLiftY = best.y + 0.7;
         this.escapeStage = 0;
         this.escapeCooldown = 160;
         this.escapeProgress = null;
         return true;
      } else {
         return false;
      }
   }

   private void terrainEscape() {
      this.getNavigation().stop();
      this.setNoGravity(true);
      if (this.actionTicks > 120) {
         this.finishTerrainEscape();
      } else {
         Vec3 destination;
         if (this.escapeStage == 0) {
            destination = new Vec3(this.getX(), this.escapeLiftY, this.getZ());
            if (this.getY() >= this.escapeLiftY - 0.12) {
               this.escapeStage = 1;
               this.setDeltaMovement(Vec3.ZERO);
               return;
            }
         } else if (this.escapeStage == 1) {
            destination = new Vec3(this.escapeLanding.x, this.escapeLiftY, this.escapeLanding.z);
            if (this.position().distanceToSqr(destination) < 0.06) {
               this.escapeStage = 2;
               this.setDeltaMovement(Vec3.ZERO);
               return;
            }
         } else {
            destination = this.escapeLanding;
            if (this.position().distanceToSqr(destination) < 0.03) {
               this.finishTerrainEscape();
               return;
            }
         }

         Vec3 delta = destination.subtract(this.position());
         Vec3 motion = delta.normalize().scale(Math.min(this.escapeStage == 2 ? 0.25 : 0.45, delta.length() * 0.35));
         if (!this.clearEscapeSegment(this.position(), this.position().add(motion))) {
            this.finishTerrainEscape();
         } else {
            this.setDeltaMovement(motion);
            this.resetFallDistance();
         }
      }
   }

   private void finishTerrainEscape() {
      int resume = this.escapeReturnAction == 10 ? 10 : 4;
      this.changeAction(resume);
      this.setNoGravity(false);
      this.escapeProgress = this.position();
      this.stuckTicks = 0;
      this.escapeCooldown = 160;
      this.attackCooldown = Math.max(this.attackCooldown, 10);
   }

   private void pursue(LivingEntity target, boolean attacking) {
      double speed = this.phase() == 4 ? 1.3 : 1.1;
      if (this.canSwimTowards(target)) {
         Vec3 direction = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
         double stop = (this.getBbWidth() + target.getBbWidth()) * 0.5 + 0.35;
         Vec3 point = target.position().subtract(direction.scale(stop));
         this.getNavigation().stop();
         this.getMoveControl().setWantedPosition(point.x, target.getY() + 0.3, point.z, speed);
      } else {
         Vec3 towards = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
         double gap = towards.horizontalDistance();
         double stop = (this.getBbWidth() + target.getBbWidth()) * 0.5 + 0.35;
         if (gap <= stop && Math.abs(target.getY() - this.getY()) < 1.5 && this.getSensing().hasLineOfSight(target)) {
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
            this.face(target.position());
         } else if (--this.repathTicks <= 0 || this.getNavigation().isDone()) {
            this.repathTicks = attacking ? 4 : 10;
            Vec3 destination = target.position().subtract(towards.normalize().scale(stop));
            Vec3 point = this.standingAt(destination.x, destination.z, target.getY(), 1.4);
            if (point != null && this.canWalkDirectly(this.position(), point)) {
               this.getNavigation().stop();
               this.getMoveControl().setWantedPosition(point.x, point.y, point.z, speed);
            } else {
               if (this.approachPoint != null && this.approachPath != null && this.bodyFitsPath(this.approachPath)) {
                  this.getNavigation().moveTo(this.approachPath, speed);
               } else {
                  this.getNavigation().moveTo(target, speed);
               }
            }
         }
      }
   }

   private void dashTrail(ServerLevel server, Vec3 velocity) {
      Vec3 behind = this.position().subtract(velocity.normalize().scale(0.65)).add(0.0, 2.0, 0.0);
      server.sendParticles(ParticleTypes.SCULK_SOUL, behind.x, behind.y, behind.z, 3, 0.2, 0.6, 0.2, 0.012);
   }

   private boolean canStartClaws(LivingEntity target) {
      return this.getBoundingBox().inflate(0.75, 0.3, 0.75).intersects(target.getBoundingBox()) && this.getSensing().hasLineOfSight(target);
   }

   private void beginClaws(LivingEntity target, int cooldown) {
      this.clawTick = 1;
      this.clawVictim = target;
      this.firstClawConnected = false;
      this.attackCooldown = cooldown;
      this.triggerAnim("combat", "claw");
   }

   private void clawContact(ServerLevel server, boolean followup) {
      this.playSound(ModSounds.MONARCH_ATTACK, 0.95F, 0.7F);
      if (this.valid(this.clawVictim) && this.inClawRange(this.clawVictim)) {
         DamageSource source = followup && this.firstClawConnected ? StalkerEntity.followupDamage(server, this) : this.damageSources().mobAttack(this);
         boolean connected = StalkerEntity.clawContact(this, this.clawVictim, server, source, (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE), followup);
         if (!followup) {
            this.firstClawConnected = connected;
         }
      }
   }

   private boolean inClawRange(LivingEntity target) {
      return this.getBoundingBox().inflate(1.25, 0.3, 1.25).intersects(target.getBoundingBox()) && this.getSensing().hasLineOfSight(target);
   }

   private boolean safeFloor(Vec3 point) {
      BlockPos p = BlockPos.containing(point.x, this.getY() - 0.25, point.z);
      return this.level().getBlockState(p).isFaceSturdy(this.level(), p, Direction.UP)
         && this.level().noCollision(this, this.getBoundingBox().move(point.x - this.getX(), 0.0, point.z - this.getZ()));
   }

   private void ranged(ServerLevel server, LivingEntity target) {
      if (this.clawTick > 0) {
         if (++this.clawTick == 5) {
            this.clawContact(server, false);
         } else if (this.clawTick == 9) {
            this.clawContact(server, true);
         }

         if (this.clawTick >= 15) {
            this.clawTick = 0;
            this.clawVictim = null;
            this.firstClawConnected = false;
         }
      }

      if (!this.valid(target)) {
         if (this.isNoGravity()) {
            this.hover();
         }
      } else {
         if (this.isNoGravity()) {
            this.getNavigation().stop();
            Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
            double distance = this.position().subtract(target.position()).horizontalDistance();
            Vec3 movement = away.scale(distance < 10.0 ? 0.2 : (distance > 20.0 ? -0.15 : 0.0));
            this.setDeltaMovement(movement.x, 0.0, movement.z);
            if (this.actionTicks >= 160) {
               Vec3 landing = this.findLanding(target);
               if (landing != null) {
                  this.committedDive = landing;
                  this.changeAction(11);
               } else {
                  this.actionTicks = 80;
               }
            }
         } else {
            if (this.actionTicks % 10 == 0) {
               if (this.distanceToSqr(target) < 81.0) {
                  Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
                  Vec3 escape = this.position().add(away.scale(5.0));
                  if (this.safeFloor(escape)) {
                     this.getNavigation().moveTo(escape.x, escape.y, escape.z, 1.35);
                  }
               } else if (!(this.distanceToSqr(target) > 400.0) && this.getSensing().hasLineOfSight(target)) {
                  this.getNavigation().stop();
               } else {
                  this.getNavigation().moveTo(target, 1.0);
               }
            }

            if (this.actionTicks >= 160) {
               this.flightY = this.clearFlightHeight(8.0);
               this.changeAction(10);
               this.setNoGravity(this.flightY > this.getY() + 0.1);
            }
         }

         if (this.attackCooldown == 0 && this.clawTick == 0 && this.inClawRange(target) && this.clawTick == 0) {
            this.beginClaws(target, 30);
         }

         if (this.clawTick == 0 && --this.shotCooldown <= 0 && this.getSensing().hasLineOfSight(target)) {
            this.shoot(server, target, false);
            this.shotCooldown = 32;
         }

         if (this.isNoGravity() && this.flightY > this.getY() + 0.01 && this.actionTicks < 35) {
            this.flightY = Math.min(this.flightY, this.clearFlightHeight(this.flightY - this.getY()));
            this.setDeltaMovement(this.getDeltaMovement().x, Math.min(0.4, (this.flightY - this.getY()) * 0.24), this.getDeltaMovement().z);
         }
      }
   }

   private Vec3 findLanding(LivingEntity target) {
      Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
      if (away.lengthSqr() < 0.01) {
         away = new Vec3(1.0, 0.0, 0.0);
      }

      for (float angle : new float[]{0.0F, 0.8F, -0.8F, 1.6F, -1.6F}) {
         Vec3 point = target.position().add(away.yRot(angle).scale(10.0));

         for (int y = (int)Math.min(this.getY(), target.getY() + 8.0); y >= target.getY() - 6.0; y--) {
            BlockPos floor = BlockPos.containing(point.x, y, point.z);
            Vec3 feet = new Vec3(point.x, y + 1.0, point.z);
            if (this.serverFloor(floor) && this.level().noCollision(this, this.getBoundingBox().move(feet.subtract(this.position())))) {
               return feet;
            }
         }
      }

      return null;
   }

   private boolean serverFloor(BlockPos floor) {
      return this.level().getBlockState(floor).isFaceSturdy(this.level(), floor, Direction.UP);
   }

   private void face(Vec3 point) {
      Vec3 d = point.subtract(this.position());
      if (!(d.horizontalDistanceSqr() < 0.001)) {
         float yaw = (float)(Mth.atan2(d.z, d.x) * 180.0 / Math.PI) - 90.0F;
         float turn = Mth.approachDegrees(this.getYRot(), yaw, this.isFlying() ? 8.0F : 18.0F);
         this.setYRot(turn);
         this.setYBodyRot(turn);
      }
   }

   private boolean shoot(ServerLevel server, LivingEntity target, boolean summoning) {
      MonarchProjectileEntity projectile = (summoning ? ModEntities.MONARCH_SUMMON_PROJECTILE : ModEntities.MONARCH_EXPLOSION_PROJECTILE)
         .create(server, EntitySpawnReason.MOB_SUMMONED);
      if (projectile == null) {
         return false;
      }

      projectile.setPos(this.getX(), this.getY() + 4.5, this.getZ());
      Vec3 aim = target.getBoundingBox().getCenter();
      if (summoning) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double radius = 3.0 + this.random.nextDouble() * 4.0;
         aim = target.position().add(Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius);
      }

      projectile.launch(this, aim);
      if (!server.addFreshEntity(projectile)) {
         return false;
      }

      if (summoning) {
         this.summons.add(new MonarchEntity.SummonSlot(projectile.getUUID(), projectile.blockPosition(), true));
      }

      this.triggerAnim("combat", "shoot");
      return true;
   }

   public void resolveSummon(UUID projectile, Mob summoned) {
      for (MonarchEntity.SummonSlot slot : this.summons) {
         if (slot.id.equals(projectile)) {
            if (summoned == null) {
               this.summons.remove(slot);
            } else {
               slot.id = summoned.getUUID();
               slot.pos = summoned.blockPosition();
               slot.projectile = false;
            }

            return;
         }
      }
   }

   private void refreshSummons(ServerLevel server) {
      this.summons.removeIf(slot -> {
         Entity entity = server.getEntity(slot.id);
         if (entity != null) {
            slot.pos = entity.blockPosition();
            return !entity.isAlive();
         } else {
            return this.loadGrace == 0 && server.hasChunkAt(slot.pos);
         }
      });
   }

   private void trimSummons(ServerLevel server, int count) {
      while (this.summons.size() > count) {
         MonarchEntity.SummonSlot slot = this.summons.remove(this.summons.size() - 1);
         Entity entity = server.getEntity(slot.id);
         if (entity != null) {
            server.sendParticles(ParticleTypes.SCULK_SOUL, entity.getX(), entity.getY() + 0.5, entity.getZ(), 20, 0.4, 0.5, 0.4, 0.04);
            entity.discard();
         }
      }
   }

   private void split(ServerLevel server, LivingEntity target) {
      if (this.actionTicks < 14) {
         this.hover();
         if (this.actionTicks % 2 == 0) {
            double angle = this.actionTicks * 0.7;
            server.sendParticles(
               ParticleTypes.SCULK_SOUL, this.getX() + Math.cos(angle) * 0.8, this.getY() + 2.5, this.getZ() + Math.sin(angle) * 0.8, 2, 0.08, 0.5, 0.08, 0.015
            );
         }
      } else {
         if (!this.splitOnce) {
            this.spawnEcho(server, target);
         }

         this.separate(28);
         if (this.actionTicks % 3 == 0 && this.echoId != null && server.getEntity(this.echoId) instanceof MonarchEntity echo) {
            Vec3 bridge = this.position().lerp(echo.position(), 0.5).add(0.0, 2.5, 0.0);
            server.sendParticles(ParticleTypes.SCULK_SOUL, bridge.x, bridge.y, bridge.z, 5, 0.3, 0.6, 0.3, 0.02);
         }

         if (this.actionTicks >= 42) {
            this.trimSummons(server, 2);
            this.changeAction(6);
         }
      }
   }

   private void separate(int duration) {
      this.getNavigation().stop();
      this.setNoGravity(true);
      int elapsed = this.action() == 12 ? this.actionTicks - 14 : this.actionTicks;
      double t = Mth.clamp((double)elapsed / duration, 0.0, 1.0);
      double ease = t * t * (3.0 - 2.0 * t);
      Vec3 wanted = this.splitOrigin.lerp(this.splitDestination, ease);
      Vec3 step = wanted.subtract(this.position());
      this.setDeltaMovement(this.level().noCollision(this, this.getBoundingBox().expandTowards(step).deflate(0.01)) ? step : Vec3.ZERO);
   }

   private Vec3 separationPoint(Vec3 origin, Vec3 direction, double distance) {
      Vec3 result = origin;

      for (double d = 0.2; d <= distance + 0.001; d += 0.2) {
         Vec3 candidate = origin.add(direction.scale(d));
         AABB volume = this.getBoundingBox().expandTowards(candidate.subtract(this.position()));
         if (!this.level().noCollision(this, volume)) {
            break;
         }

         result = candidate;
      }

      return result;
   }

   private void spawnEcho(ServerLevel server, LivingEntity target) {
      if (!this.splitOnce) {
         MonarchEntity echo = ModEntities.MONARCH_ECHO.create(server, EntitySpawnReason.MOB_SUMMONED);
         if (echo != null) {
            Vec3 forward = this.valid(target) ? target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize() : new Vec3(0.0, 0.0, 1.0);
            if (forward.lengthSqr() < 0.01) {
               forward = new Vec3(0.0, 0.0, 1.0);
            }

            Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
            Vec3 origin = this.position();
            Vec3 echoEnd = origin;
            Vec3 parentEnd = origin;
            double best = -1.0;

            for (Vec3 direction : new Vec3[]{side, side.scale(-1.0), forward, forward.scale(-1.0)}) {
               Vec3 e = this.separationPoint(origin, direction, 3.2);
               Vec3 p = this.separationPoint(origin, direction.scale(-1.0), 1.2);
               double space = e.distanceToSqr(p);
               if (space > best) {
                  best = space;
                  echoEnd = e;
                  parentEnd = p;
               }
            }

            echo.setPos(origin);
            echo.setYRot(this.getYRot());
            echo.setYBodyRot(this.yBodyRot);
            echo.setYHeadRot(this.getYHeadRot());
            echo.ownerId = this.getUUID();
            echo.setTarget(target);
            echo.changeAction(13);
            echo.splitOrigin = origin;
            echo.splitDestination = echoEnd;
            if (server.addFreshEntity(echo)) {
               this.splitOnce = true;
               this.echoId = echo.getUUID();
               this.echoPosition = echo.blockPosition();
               this.splitOrigin = origin;
               this.splitDestination = parentEnd;
               this.playSound(ModSounds.MONARCH_DASH, 1.1F, 0.5F);
               server.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.5, this.getZ(), 28, 0.5, 1.1, 0.5, 0.04);
            }
         }
      }
   }

   private boolean ownerDied(ServerLevel server) {
      CompoundTag data = server.getServer().getCommandStorage().get(DEAD_OWNERS);
      return data.getBooleanOr(this.ownerId.toString(), false) || server.getEntity(this.ownerId) instanceof MonarchEntity parent && !parent.isAlive();
   }

   private void dieWithOwner(ServerLevel server) {
      this.setHealth(0.0F);
      this.die(this.damageSources().genericKill());
      CompoundTag data = server.getServer().getCommandStorage().get(DEAD_OWNERS);
      data.remove(this.ownerId.toString());
      server.getServer().getCommandStorage().set(DEAD_OWNERS, data);
   }

   @Override
   public void die(DamageSource source) {
      if (this.isEcho() && this.ownerId != null && this.level() instanceof ServerLevel server && server.getEntity(this.ownerId) instanceof MonarchEntity parent
         )
       {
         parent.echoDefeated = true;
      }

      if (!this.isEcho() && this.level() instanceof ServerLevel server && this.echoId != null && !this.echoDefeated) {
         CompoundTag data = server.getServer().getCommandStorage().get(DEAD_OWNERS);
         data.putBoolean(this.getUUID().toString(), true);
         server.getServer().getCommandStorage().set(DEAD_OWNERS, data);
         if (server.getEntity(this.echoId) instanceof MonarchEntity echo && echo.isAlive()) {
            echo.dieWithOwner(server);
         }
      }

      this.boss.removeAllPlayers();
      super.die(source);
   }

   @Override
   public void remove(RemovalReason reason) {
      this.boss.removeAllPlayers();
      super.remove(reason);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController(
            "movement",
            3,
            state -> {
               state.controller()
                  .setAnimation(
                     this.action() == 12
                        ? SPLIT
                        : (
                           this.action() == 13
                              ? EMERGE
                              : (
                                 this.action() == 2
                                    ? WINDUP
                                    : (
                                       this.action() == 3
                                          ? DIVE_ANIM
                                          : (
                                             this.isFlying()
                                                ? FLY
                                                : (this.entityData.get(DASHING) ? DASH : (this.getDeltaMovement().horizontalDistanceSqr() > 0.002 ? RUN : IDLE))
                                          )
                                    )
                              )
                        )
                  );
               return PlayState.CONTINUE;
            }
         )
      );
      controllers.add(
         new AnimationController("combat", 0, state -> PlayState.STOP)
            .triggerableAnim("claw", CLAW)
            .triggerableAnim("shoot", SHOOT)
            .triggerableAnim("split", SPLIT)
      );
      controllers.add(new AnimationController("guard", 0, state -> PlayState.STOP).triggerableAnim("wing_guard", WING_GUARD));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putInt("MonarchAction", this.action());
      output.putInt("ActionTicks", this.actionTicks);
      output.putInt("PhaseSeen", this.phaseSeen);
      output.putInt("Shots", this.shots);
      output.putInt("ShotCooldown", this.shotCooldown);
      output.putDouble("FlightY", this.flightY);
      output.putBoolean("SplitOnce", this.splitOnce);
      output.putBoolean("EchoDefeated", this.echoDefeated);
      if (this.echoId != null) {
         output.putString("EchoId", this.echoId.toString());
      }

      if (this.ownerId != null) {
         output.putString("MonarchOwner", this.ownerId.toString());
      }

      output.putLong("EchoPosition", this.echoPosition.asLong());
      output.putDouble("DiveX", this.committedDive.x);
      output.putDouble("DiveY", this.committedDive.y);
      output.putDouble("DiveZ", this.committedDive.z);
      output.putDouble("DashX", this.dashVelocity.x);
      output.putDouble("DashY", this.dashVelocity.y);
      output.putDouble("DashZ", this.dashVelocity.z);
      output.putDouble("SplitOriginX", this.splitOrigin.x);
      output.putDouble("SplitOriginY", this.splitOrigin.y);
      output.putDouble("SplitOriginZ", this.splitOrigin.z);
      output.putDouble("SplitEndX", this.splitDestination.x);
      output.putDouble("SplitEndY", this.splitDestination.y);
      output.putDouble("SplitEndZ", this.splitDestination.z);
      output.putInt("EscapeStage", this.escapeStage);
      output.putInt("EscapeReturn", this.escapeReturnAction);
      output.putInt("EscapeCooldown", this.escapeCooldown);
      output.putDouble("EscapeX", this.escapeLanding.x);
      output.putDouble("EscapeY", this.escapeLanding.y);
      output.putDouble("EscapeZ", this.escapeLanding.z);
      output.putDouble("EscapeLift", this.escapeLiftY);
      output.putInt("SummonCount", this.summons.size());

      for (int i = 0; i < this.summons.size(); i++) {
         MonarchEntity.SummonSlot slot = this.summons.get(i);
         output.putString("Summon" + i, slot.id.toString());
         output.putLong("SummonPos" + i, slot.pos.asLong());
         output.putBoolean("SummonPending" + i, slot.projectile);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (movement != null && Math.abs(movement.getBaseValue() - 0.31) < 1.0E-6) {
         movement.setBaseValue(0.27);
      }

      int restored = input.getIntOr("MonarchAction", this.isEcho() ? 4 : 0);
      boolean savedFlight = this.isNoGravity();
      this.changeAction(restored >= 0 && restored <= 14 ? restored : 4);
      if (restored == 10) {
         this.setNoGravity(savedFlight);
      }

      this.actionTicks = Math.max(0, input.getIntOr("ActionTicks", 0));
      this.phaseSeen = Mth.clamp(input.getIntOr("PhaseSeen", 0), 0, 4);
      this.shots = Mth.clamp(input.getIntOr("Shots", 0), 0, 5);
      this.shotCooldown = input.getIntOr("ShotCooldown", 0);
      this.flightY = input.getDoubleOr("FlightY", this.getY() + 10.0);
      this.splitOnce = input.getBooleanOr("SplitOnce", false);
      this.echoDefeated = input.getBooleanOr("EchoDefeated", false);
      this.echoId = readUuid(input.getStringOr("EchoId", ""));
      this.ownerId = readUuid(input.getStringOr("MonarchOwner", ""));
      this.echoPosition = BlockPos.of(input.getLongOr("EchoPosition", 0L));
      this.committedDive = new Vec3(input.getDoubleOr("DiveX", this.getX()), input.getDoubleOr("DiveY", this.getY()), input.getDoubleOr("DiveZ", this.getZ()));
      this.dashVelocity = new Vec3(input.getDoubleOr("DashX", 0.0), input.getDoubleOr("DashY", 0.0), input.getDoubleOr("DashZ", 0.0));
      this.splitOrigin = new Vec3(
         input.getDoubleOr("SplitOriginX", this.getX()), input.getDoubleOr("SplitOriginY", this.getY()), input.getDoubleOr("SplitOriginZ", this.getZ())
      );
      this.splitDestination = new Vec3(
         input.getDoubleOr("SplitEndX", this.getX()), input.getDoubleOr("SplitEndY", this.getY()), input.getDoubleOr("SplitEndZ", this.getZ())
      );
      this.escapeStage = Mth.clamp(input.getIntOr("EscapeStage", 0), 0, 2);
      this.escapeReturnAction = input.getIntOr("EscapeReturn", 4) == 10 ? 10 : 4;
      this.escapeCooldown = Mth.clamp(input.getIntOr("EscapeCooldown", 0), 0, 160);
      this.escapeLanding = new Vec3(
         input.getDoubleOr("EscapeX", this.getX()), input.getDoubleOr("EscapeY", this.getY()), input.getDoubleOr("EscapeZ", this.getZ())
      );
      this.escapeLiftY = input.getDoubleOr("EscapeLift", this.getY());
      this.escapeProgress = null;
      this.stuckTicks = 0;
      this.summons.clear();

      for (int i = 0; i < Mth.clamp(input.getIntOr("SummonCount", 0), 0, 8); i++) {
         UUID id = readUuid(input.getStringOr("Summon" + i, ""));
         if (id != null) {
            this.summons
               .add(new MonarchEntity.SummonSlot(id, BlockPos.of(input.getLongOr("SummonPos" + i, 0L)), input.getBooleanOr("SummonPending" + i, false)));
         }
      }

      this.loadGrace = 60;
   }

   private static UUID readUuid(String text) {
      try {
         return UUID.fromString(text);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static final class SummonSlot {
      UUID id;
      BlockPos pos;
      boolean projectile;

      SummonSlot(UUID id, BlockPos pos, boolean projectile) {
         this.id = id;
         this.pos = pos;
         this.projectile = projectile;
      }
   }
}
