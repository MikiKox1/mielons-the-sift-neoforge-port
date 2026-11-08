package mielon.thesift.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.item.ModItems;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.worldgen.SiftSculkBlobResidents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BlubEntity extends TamableAnimal implements GeoEntity {
   public static final float WIDTH = 0.665F;
   public static final float HEIGHT = 0.875F;
   public static final float EYE_HEIGHT = 0.4375F;
   private static final double PACK_ALERT_RANGE = 16.0;
   private static final int PACK_ANGER_TICKS = 600;
   private static final int FLAPPING_TICKS = 20;
   private static final int MAX_TOWER_SIZE = 5;
   private static final double TOWER_STEP_HEIGHT = 0.546875;
   private static final String BODY_CONTROLLER = "body";
   private static final String HAPPY_TRIGGER = "happy";
   private static final EntityDataAccessor<Boolean> DATA_FLAPPING = SynchedEntityData.defineId(BlubEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(BlubEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(BlubEntity.class, EntityDataSerializers.INT);
   private static final RawAnimation HOPPING = RawAnimation.begin().thenLoop("hopping");
   private static final RawAnimation HAPPY = RawAnimation.begin().thenPlay("happy");
   private static final RawAnimation FLAPPING = RawAnimation.begin().thenPlay("flapping");
   private static final RawAnimation ANTENNAE_IDLE = RawAnimation.begin().thenLoop("antennae_idle");
   private static final RawAnimation ANTENNAE_HOPPING = RawAnimation.begin().thenLoop("antennae_hopping");
   private static final RawAnimation ANTENNAE_FLAPPING = RawAnimation.begin().thenPlay("antennae_flapping");
   private static final RawAnimation VARIANT_IDLE = RawAnimation.begin().thenLoop("variant_idle");
   private static final RawAnimation HOPPING_EARS = RawAnimation.begin().thenLoop("hopping_ears");
   private static final RawAnimation HAPPY_EARS = RawAnimation.begin().thenPlay("happy_ears");
   private static final RawAnimation STACK_SUPPORT = RawAnimation.begin().thenLoop("stack_support");
   private static final RawAnimation SITTING = RawAnimation.begin().thenPlayAndHold("sitting");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int angerTicks;
   private UUID angerTargetId;
   private int flappingTicks;
   private int nextFlappingTick;
   private int jumpTicks;
   private int jumpDuration;
   private int jumpDelayTicks;
   private int nextIdleSoundTick;
   private boolean wasOnGround;
   private int stackCheckTicks;
   private int stackCooldownTicks;
   private int stackWaitTicks;
   private int towerStayTicks;
   private BlubEntity towerJoiner;
   private boolean approachingTower;

   public BlubVariant getVariant() {
      return BlubVariant.fromId(this.entityData.get(DATA_VARIANT));
   }

   public void setVariant(BlubVariant variant) {
      this.entityData.set(DATA_VARIANT, variant.id());
   }

   public boolean isMielonAppearance() {
      if (this.getCustomName() == null) {
         return false;
      }

      String name = this.getCustomName().getString();
      return "mielon".equals(name) || "Mielon".equals(name) || "MIELON".equals(name);
   }

   public BlubVariant getVisualVariant() {
      return this.isMielonAppearance() ? BlubVariant.DEFAULT : this.getVariant();
   }

   public BlubEntity(EntityType<? extends TamableAnimal> type, Level level) {
      super(type, level);
      this.jumpControl = new BlubEntity.BlubJumpControl(this);
      this.moveControl = new BlubEntity.BlubMoveControl(this);
      this.setSpeedModifier(0.0);
      this.nextFlappingTick = 100 + this.random.nextInt(301);
      this.nextIdleSoundTick = 160 + this.random.nextInt(321);
      this.stackCheckTicks = 20 * (15 + this.random.nextInt(31));
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 3.0)
         .add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.TEMPT_RANGE, 10.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   public static boolean checkBlubSpawnRules(
      EntityType<BlubEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return reason == EntitySpawnReason.NATURAL && SiftSculkBlobResidents.blocksNaturalSpawn(level, pos)
         ? false
         : Mob.checkMobSpawnRules(type, level, reason, pos, random) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_FLAPPING, false);
      builder.define(DATA_SITTING, false);
      builder.define(DATA_VARIANT, BlubVariant.DEFAULT.id());
   }

   @Override
   protected void updateControlFlags() {
      super.updateControlFlags();
      if (this.isInBlubTower()) {
         this.goalSelector.setControlFlag(Flag.LOOK, true);
         this.goalSelector.setControlFlag(Flag.MOVE, false);
         this.goalSelector.setControlFlag(Flag.JUMP, false);
      }
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> stack.getItem() == ModItems.BLUBBY_BERRIES, false));
      this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 10.0F, 2.0F));
      this.goalSelector.addGoal(5, new BlubEntity.BlubStackGoal());
      this.goalSelector.addGoal(6, new BlubEatBerriesGoal(this));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 7.0F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
      this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel) {
         if (this.flappingTicks > 0 && --this.flappingTicks == 0) {
            this.entityData.set(DATA_FLAPPING, false);
         }

         LivingEntity target = this.getTarget();
         boolean orderedSitting = this.isOrderedToSit();
         if (this.entityData.get(DATA_SITTING) != orderedSitting) {
            this.entityData.set(DATA_SITTING, orderedSitting);
         }

         if (this.isInSittingPose() != orderedSitting) {
            this.setInSittingPose(orderedSitting);
         }

         if (--this.nextIdleSoundTick <= 0) {
            if (target == null && this.isAlive()) {
               this.playBlubIdleSound(0.75F, 0.92F + this.random.nextFloat() * 0.22F);
            }

            this.nextIdleSoundTick = 180 + this.random.nextInt(361);
         }

         if (!this.isTame() && this.angerTicks > 0) {
            if (target == null && this.angerTargetId != null && this.level() instanceof ServerLevel level) {
               Player restoredTarget = level.getServer().getPlayerList().getPlayer(this.angerTargetId);
               if (restoredTarget != null
                  && restoredTarget.isAlive()
                  && restoredTarget.level() == this.level()
                  && !restoredTarget.isCreative()
                  && !restoredTarget.isSpectator()) {
                  this.setTarget(restoredTarget);
                  this.setAggressive(true);
                  target = restoredTarget;
               }
            }

            this.angerTicks--;
            if (this.angerTicks == 0 || target != null && !target.isAlive() || target instanceof Player player && (player.isCreative() || player.isSpectator())
               )
             {
               this.setTarget(null);
               this.setAggressive(false);
               this.angerTargetId = null;
            }
         }

         this.updateBlubTower();
         boolean canFlap = this.flappingTicks == 0 && this.getTarget() == null && this.getDeltaMovement().horizontalDistanceSqr() < 0.001;
         if (canFlap && --this.nextFlappingTick <= 0) {
            this.flappingTicks = 20;
            this.entityData.set(DATA_FLAPPING, true);
            this.nextFlappingTick = 140 + this.random.nextInt(401);
         } else if (!canFlap && this.flappingTicks == 0) {
            this.nextFlappingTick = Math.max(this.nextFlappingTick, 40);
         }
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
      this.setVariant(BlubVariant.forBiome(level.getBiome(this.blockPosition()), this.random));
      if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && this.random.nextFloat() < 0.08F) {
         this.setAge(0);
         BlubEntity top = this;

         for (int layer = 1; layer < 5; layer++) {
            BlubEntity passenger = ModEntities.BLUB.create(level.getLevel(), EntitySpawnReason.JOCKEY);
            if (passenger == null) {
               break;
            }

            passenger.snapTo(top.getX(), top.getY() + 0.546875, top.getZ(), this.getYRot(), 0.0F);
            passenger.finalizeSpawn(level, difficulty, EntitySpawnReason.JOCKEY, null);
            passenger.setAge(0);
            if (level.getBlockCollisions(passenger, passenger.getBoundingBox()).iterator().hasNext() || !passenger.startRiding(top)) {
               passenger.discard();
               break;
            }

            top.positionRider(passenger);
            passenger.towerStayTicks = 20 * (35 + passenger.random.nextInt(36));
            top = passenger;
            if (!this.random.nextBoolean()) {
               break;
            }
         }

         if (this.isInBlubTower()) {
            this.towerStayTicks = 20 * (35 + this.random.nextInt(36));
         }
      }

      return result;
   }

   private Player nearbyBlubbyBerriesHolder() {
      return this.level()
         .getNearestPlayer(
            this.getX(),
            this.getY(),
            this.getZ(),
            10.0,
            entity -> entity instanceof Player player
               && player.isAlive()
               && !player.isSpectator()
               && (player.getMainHandItem().is(ModItems.BLUBBY_BERRIES) || player.getOffhandItem().is(ModItems.BLUBBY_BERRIES))
               && this.hasLineOfSight(player)
         );
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      boolean blubbyBerries = held.getItem() == ModItems.BLUBBY_BERRIES;
      boolean edible = held.get(DataComponents.FOOD) != null;
      if (this.isTame() && (blubbyBerries || edible) && this.getHealth() < this.getMaxHealth()) {
         if (this.level() instanceof ServerLevel) {
            this.feed(player, hand, held, 1.0F, 4.0F);
            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else if (!this.isTame() && blubbyBerries && this.getTarget() == null && this.angerTicks <= 0) {
         if (this.level() instanceof ServerLevel level) {
            held.consume(1, player);
            if (this.random.nextInt(3) == 0) {
               if (this.isInBlubTower()) {
                  disperseBlubTower(this.getBlubTower());
               }

               this.tame(player);
               this.stackCooldownTicks = Math.max(this.stackCooldownTicks, 100);
               if (player instanceof ServerPlayer serverPlayer) {
                  ModAdvancements.award(serverPlayer, "the_sift/blub");
               }

               this.setPersistenceRequired();
               this.getNavigation().stop();
               this.setTarget(null);
               this.setAggressive(false);
               this.angerTicks = 0;
               this.angerTargetId = null;
               this.setBlubSitting(false);
               this.setJumping(false);
               this.facePoint(player.getX(), player.getZ());
               this.setYBodyRot(this.getYRot());
               this.setYHeadRot(this.getYRot());
               this.playBlubIdleSound(0.95F, 1.08F);
               this.triggerAnim("body", "happy");
               this.triggerAnim("ears", "happy");
               level.broadcastEntityEvent(this, (byte)7);
            } else {
               level.broadcastEntityEvent(this, (byte)6);
            }

            return InteractionResult.SUCCESS_SERVER;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else if (!this.isTame() || !this.isOwnedBy(player)) {
         return super.mobInteract(player, hand);
      } else if (this.level() instanceof ServerLevel) {
         this.setBlubSitting(!this.isBlubSitting());
         this.setJumping(false);
         this.getNavigation().stop();
         this.setTarget(null);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      List<BlubEntity> tower = this.isInBlubTower() ? this.getBlubTower() : List.of();
      boolean hurt = super.hurtServer(level, source, amount);
      if (hurt && !tower.isEmpty()) {
         disperseBlubTower(tower);
      }

      if (hurt && !this.isTame() && source.getEntity() instanceof Player player && !player.isCreative() && !player.isSpectator()) {
         AABB alertArea = this.getBoundingBox().inflate(16.0);

         for (BlubEntity blub : level.getEntitiesOfClass(BlubEntity.class, alertArea, candidate -> !candidate.isTame() && candidate.isAlive())) {
            blub.setBlubSitting(false);
            blub.setTarget(player);
            blub.setAggressive(true);
            blub.angerTicks = 600;
            blub.angerTargetId = player.getUUID();
            blub.getNavigation().moveTo(player, 1.2);
         }

         return true;
      } else {
         return hurt;
      }
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putString("TheSiftBlubVariant", this.getVariant().serializedName());
      output.putInt("TheSiftStackCooldown", this.stackCooldownTicks);
      output.putInt("TheSiftTowerStay", this.towerStayTicks);
      if (!this.isTame() && this.angerTicks > 0) {
         output.putInt("TheSiftAngerTicks", this.angerTicks);
         if (this.angerTargetId != null) {
            output.putString("TheSiftAngryAt", this.angerTargetId.toString());
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.setVariant(BlubVariant.fromName(input.getStringOr("TheSiftBlubVariant", "default")));
      this.stackCooldownTicks = Mth.clamp(input.getIntOr("TheSiftStackCooldown", 0), 0, 2400);
      this.towerStayTicks = Mth.clamp(input.getIntOr("TheSiftTowerStay", 0), 0, 1400);
      this.angerTicks = Math.max(0, input.getIntOr("TheSiftAngerTicks", 0));
      this.angerTargetId = input.getString("TheSiftAngryAt").flatMap(value -> {
         try {
            return Optional.of(UUID.fromString(value));
         } catch (IllegalArgumentException ignored) {
            return Optional.empty();
         }
      }).orElse(null);
      if (this.isTame()) {
         this.angerTicks = 0;
         this.angerTargetId = null;
      }
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      if (target instanceof BlubEntity) {
         return false;
      } else if (target instanceof OwnableEntity pet && pet.getOwner() != null) {
         return false;
      } else {
         return !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
            ? !this.isOwnedBy(target) && super.canAttack(target)
            : false;
      }
   }

   @Override
   public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
      if (target instanceof BlubEntity) {
         return false;
      } else {
         return target instanceof OwnableEntity pet && pet.getOwner() != null ? false : super.wantsToAttack(target, owner);
      }
   }

   private boolean isMovingForAnimation() {
      return this.isInBlubTower() ? false : this.isJumping() || !this.onGround() || this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
   }

   public boolean isBlubSitting() {
      return this.entityData.get(DATA_SITTING);
   }

   private void setBlubSitting(boolean sitting) {
      if (sitting && this.isInBlubTower()) {
         disperseBlubTower(this.getBlubTower());
      }

      this.setOrderedToSit(sitting);
      this.setInSittingPose(sitting);
      this.entityData.set(DATA_SITTING, sitting);
      if (sitting) {
         this.setJumping(false);
         this.getNavigation().stop();
         this.moveControl.setWait();
         this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
      }
   }

   public boolean isInBlubTower() {
      return this.getVehicle() instanceof BlubEntity || this.getFirstPassenger() instanceof BlubEntity;
   }

   private BlubEntity getBlubTowerBase() {
      BlubEntity base = this;

      while (base.getVehicle() instanceof BlubEntity below) {
         base = below;
      }

      return base;
   }

   private List<BlubEntity> getBlubTower() {
      List<BlubEntity> tower = new ArrayList<>();
      BlubEntity current = this.getBlubTowerBase();
      tower.add(current);

      while (current.getFirstPassenger() instanceof BlubEntity above) {
         tower.add(above);
         current = above;
      }

      return tower;
   }

   private boolean canPlayInTower() {
      LivingEntity owner = this.getOwner();
      return this.isAlive()
         && !this.isBaby()
         && this.hurtTime == 0
         && this.getTarget() == null
         && this.angerTicks <= 0
         && !this.isOrderedToSit()
         && !this.isInWater()
         && !this.isInLava()
         && !this.isOnFire()
         && !this.isLeashed()
         && (owner == null || this.distanceToSqr(owner) <= 144.0);
   }

   private boolean canAcceptTowerJoiner(BlubEntity joiner) {
      if (this != joiner
         && !this.isPassenger()
         && !this.approachingTower
         && this.onGround()
         && this.canPlayInTower()
         && this.stackCooldownTicks <= 0
         && this.nearbyBlubbyBerriesHolder() == null
         && (this.towerJoiner == null || this.towerJoiner == joiner)) {
         List<BlubEntity> tower = this.getBlubTower();
         if (tower.size() < 5 && !tower.contains(joiner)) {
            for (BlubEntity blub : tower) {
               if (!blub.canPlayInTower()) {
                  return false;
               }
            }

            BlubEntity top = tower.get(tower.size() - 1);
            AABB landing = joiner.getBoundingBox().move(top.getX() - joiner.getX(), top.getY() + 0.546875 - joiner.getY(), top.getZ() - joiner.getZ());
            return !this.level().getBlockCollisions(joiner, landing).iterator().hasNext();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void holdForBlubTower() {
      ((BlubEntity.BlubJumpControl)this.jumpControl).cancelJump();
      this.getNavigation().stop();
      this.stopTowerMovement();
      this.setJumping(false);
      this.jumpTicks = 0;
      this.jumpDuration = 0;
      this.setDeltaMovement(0.0, this.isPassenger() ? 0.0 : this.getDeltaMovement().y, 0.0);
      if (this.isPassenger()) {
         this.fallDistance = 0.0;
      }
   }

   private void updateBlubTower() {
      if (this.stackCooldownTicks > 0) {
         this.stackCooldownTicks--;
      }

      if (this.stackCheckTicks > 0) {
         this.stackCheckTicks--;
      }

      if (this.towerJoiner != null
         && (--this.stackWaitTicks <= 0 || !this.towerJoiner.isAlive() || this.towerJoiner.isPassenger() || this.distanceToSqr(this.towerJoiner) > 81.0)) {
         this.towerJoiner = null;
         this.stackWaitTicks = 0;
      }

      if (!this.isInBlubTower()) {
         this.towerStayTicks = 0;
      } else {
         List<BlubEntity> tower = this.getBlubTower();
         if (!(this.getVehicle() instanceof BlubEntity)
            && this.tickCount % 20 == 0
            && tower.size() >= 3
            && tower.stream().map(BlubEntity::getVariant).distinct().count() >= 3L) {
            for (ServerPlayer player : ((ServerLevel)this.level())
               .getEntitiesOfClass(
                  ServerPlayer.class,
                  this.getBoundingBox().inflate(16.0),
                  observer -> observer.isAlive() && !observer.isSpectator() && observer.hasLineOfSight(this)
               )) {
               ModAdvancements.award(player, "the_sift/now_thats_a_stack");
            }
         }

         if (this == tower.get(0) && this.tickCount % 5 == 0) {
            Player temptingPlayer = this.nearbyBlubbyBerriesHolder();
            if (temptingPlayer != null) {
               disperseBlubTower(tower);

               for (BlubEntity member : tower) {
                  if (!member.isOrderedToSit()) {
                     member.getNavigation().moveTo(temptingPlayer, 1.1);
                  }
               }

               return;
            }
         }

         if (tower.size() <= 5 && !tower.get(0).isPassenger() && !tower.stream().anyMatch(blub -> !blub.canPlayInTower())) {
            if (this.towerStayTicks <= 0) {
               this.towerStayTicks = 20 * (35 + this.random.nextInt(36));
            } else if (--this.towerStayTicks == 0) {
               int index = tower.indexOf(this);
               disperseBlubTower(new ArrayList<>(tower.subList(index, tower.size())));
               if (index > 0) {
                  tower.get(0).stackCooldownTicks = Math.max(tower.get(0).stackCooldownTicks, 300);
               }

               return;
            }

            this.holdForBlubTower();
         } else {
            disperseBlubTower(tower);
         }
      }
   }

   private static void disperseBlubTower(List<BlubEntity> tower) {
      if (!tower.isEmpty()) {
         double startAngle = tower.get(0).random.nextDouble() * Math.PI * 2.0;

         for (int i = tower.size() - 1; i >= 0; i--) {
            BlubEntity blub = tower.get(i);
            boolean mounted = blub.getVehicle() instanceof BlubEntity;
            if (mounted) {
               blub.stopRiding();
            }

            blub.towerJoiner = null;
            blub.stackWaitTicks = 0;
            blub.towerStayTicks = 0;
            blub.stackCooldownTicks = 20 * (60 + blub.random.nextInt(61));
            blub.stackCheckTicks = 20 * (8 + blub.random.nextInt(13));
            blub.setJumping(false);
            blub.jumpTicks = 0;
            blub.jumpDuration = 0;
            blub.getNavigation().stop();
            blub.stopTowerMovement();
            if (mounted && blub.isAlive()) {
               double angle = startAngle + i * 2.399963229728653;
               blub.setDeltaMovement(Math.cos(angle) * 0.18, 0.22, Math.sin(angle) * 0.18);
               blub.setOnGround(false);
               blub.fallDistance = 0.0;
            }
         }
      }
   }

   @Override
   protected boolean canAddPassenger(Entity passenger) {
      if (passenger instanceof BlubEntity blub && blub != this && this.isAlive() && this.getPassengers().isEmpty()) {
         List<BlubEntity> below = this.getBlubTower();
         List<BlubEntity> above = blub.getBlubTower();
         return !below.contains(blub) && !above.contains(this) && below.size() + above.size() <= 5;
      } else {
         return false;
      }
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction move) {
      if (passenger instanceof BlubEntity && this.hasPassenger(passenger)) {
         move.accept(passenger, this.getX(), this.getY() + 0.546875, this.getZ());
         passenger.fallDistance = 0.0;
      } else {
         super.positionRider(passenger, move);
      }
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
      return passenger instanceof BlubEntity ? passenger.position() : super.getDismountLocationForPassenger(passenger);
   }

   private void stopTowerMovement() {
      this.moveControl.setWait();
   }

   @Override
   public boolean startRiding(Entity vehicle, boolean force, boolean sendEvent) {
      return vehicle instanceof BlubEntity below && !below.canAddPassenger(this) ? false : super.startRiding(vehicle, force, sendEvent);
   }

   private double hoppingAnimationSpeed() {
      double horizontalSpeed = Math.sqrt(this.getDeltaMovement().horizontalDistanceSqr());
      return Mth.clamp(1.0 + horizontalSpeed * 0.5, 1.0, 1.2);
   }

   @Override
   public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
      return null;
   }

   @Override
   public boolean canMate(Animal other) {
      return false;
   }

   @Override
   public boolean isFood(ItemStack stack) {
      return false;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isInWater() ? ModSounds.BLUB_IDLE_WATER : ModSounds.BLUB_IDLE_AIR;
   }

   private void playBlubIdleSound(float volume, float pitch) {
      if (!this.level().isClientSide()) {
         this.level()
            .playSound(null, this.blockPosition(), this.isInWater() ? ModSounds.BLUB_IDLE_WATER : ModSounds.BLUB_IDLE_AIR, this.getSoundSource(), volume, pitch);
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.BLUB_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.BLUB_DEATH;
   }

   @Override
   protected SoundEvent getSwimSound() {
      return ModSounds.BLUB_SWIM;
   }

   @Override
   protected SoundEvent getSwimSplashSound() {
      return ModSounds.BLUB_SPLASH;
   }

   @Override
   public boolean doHurtTarget(ServerLevel level, Entity target) {
      boolean hit = super.doHurtTarget(level, target);
      if (hit) {
         this.playSound(ModSounds.BLUB_ATTACK, 0.7F, 0.95F + this.random.nextFloat() * 0.15F);
      }

      return hit;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
   }

   @Override
   protected float getJumpPower() {
      float height = this.moveControl.getSpeedModifier() > 1.1 ? 0.36F : 0.3F;
      Path path = this.navigation.getPath();
      if (path != null && !path.isDone() && path.getNextEntityPos(this).y > this.getY() + 0.5) {
         height = 0.5F;
      }

      if (this.horizontalCollision || this.moveControl.getWantedY() > this.getY() + 0.5) {
         height = 0.5F;
      }

      return super.getJumpPower(height / 0.42F);
   }

   @Override
   public void jumpFromGround() {
      super.jumpFromGround();
      if (this.level() instanceof ServerLevel && this.isAlive()) {
         this.playSound(ModSounds.BLUB_HOP, 0.6F, 0.95F + this.random.nextFloat() * 0.1F);
      }

      if (this.moveControl.getSpeedModifier() > 0.0 && this.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
         this.moveRelative(0.1F, new Vec3(0.0, 0.0, 1.0));
      }
   }

   @Override
   public void setJumping(boolean jumping) {
      super.setJumping(jumping);
   }

   private void startJumping() {
      this.setJumping(true);
      this.jumpDuration = 15;
      this.jumpTicks = 0;
   }

   private void setSpeedModifier(double speed) {
      this.navigation.setSpeedModifier(speed);
      this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), speed);
   }

   private void facePoint(double x, double z) {
      this.setYRot((float)(Mth.atan2(z - this.getZ(), x - this.getX()) * (180.0 / Math.PI)) - 90.0F);
   }

   private void setLandingDelay() {
      double speed = this.moveControl.getSpeedModifier();
      this.jumpDelayTicks = speed >= 1.15 ? 4 : (speed >= 1.05 ? 6 : 8);
      ((BlubEntity.BlubJumpControl)this.jumpControl).setCanJump(false);
   }

   @Override
   protected void customServerAiStep(ServerLevel level) {
      super.customServerAiStep(level);
      if (this.jumpDelayTicks > 0) {
         this.jumpDelayTicks--;
      }

      if (this.isInBlubTower() || this.towerJoiner != null) {
         this.holdForBlubTower();
         this.wasOnGround = this.onGround();
      } else if (this.isBlubSitting()) {
         this.setJumping(false);
         this.moveControl.setWait();
         this.wasOnGround = this.onGround();
      } else {
         if (this.onGround()) {
            if (!this.wasOnGround) {
               this.setJumping(false);
               this.setLandingDelay();
            }

            BlubEntity.BlubJumpControl jumps = (BlubEntity.BlubJumpControl)this.jumpControl;
            if (!jumps.wantJump() && this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
               Vec3 destination = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
               Path path = this.navigation.getPath();
               if (path != null && !path.isDone()) {
                  destination = path.getNextEntityPos(this);
               }

               this.facePoint(destination.x, destination.z);
               this.startJumping();
            } else if (jumps.wantJump() && !jumps.canJump()) {
               jumps.setCanJump(true);
            }
         }

         this.wasOnGround = this.onGround();
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.jumpTicks != this.jumpDuration) {
         this.jumpTicks++;
      } else if (this.jumpDuration != 0) {
         this.jumpTicks = 0;
         this.jumpDuration = 0;
         this.setJumping(false);
      }
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return false;
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("body", 2, state -> {
         BlubEntity blub = (BlubEntity)state.animatable();
         AnimationController<BlubEntity> controller = state.controller();
         if (controller.isTriggeredAnimation("happy")) {
            controller.setAnimationSpeed(1.0);
            if (controller.getCurrentTimelineTime() != -2.0) {
               return PlayState.CONTINUE;
            }

            controller.stopTriggeredAnimation();
         }

         if (blub.isBlubSitting()) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(SITTING);
            return PlayState.CONTINUE;
         } else if (blub.isMovingForAnimation()) {
            controller.setAnimationSpeed(blub.hoppingAnimationSpeed());
            controller.setAnimation(HOPPING);
            return PlayState.CONTINUE;
         } else {
            controller.setAnimationSpeed(1.0);
            controller.reset();
            return PlayState.STOP;
         }
      }).triggerableAnim("happy", HAPPY).receiveTriggeredAnimations());
      controllers.add(new AnimationController("ears", 1, state -> {
         BlubEntity blub = (BlubEntity)state.animatable();
         AnimationController<GeoAnimatable> controller = state.controller();
         if (controller.isTriggeredAnimation("happy")) {
            controller.setAnimationSpeed(1.0);
            if (controller.getCurrentTimelineTime() != -2.0) {
               return PlayState.CONTINUE;
            }

            controller.stopTriggeredAnimation();
         }

         if (blub.getFirstPassenger() instanceof BlubEntity) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(STACK_SUPPORT);
            return PlayState.CONTINUE;
         } else if (blub.entityData.get(DATA_FLAPPING) && blub.getVisualVariant() != BlubVariant.MEADOWS) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(FLAPPING);
            return PlayState.CONTINUE;
         } else if (!blub.isBlubSitting() && blub.isMovingForAnimation()) {
            controller.setAnimationSpeed(blub.hoppingAnimationSpeed());
            controller.setAnimation(HOPPING_EARS);
            return PlayState.CONTINUE;
         } else if (blub.getVisualVariant() != BlubVariant.DEFAULT) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(VARIANT_IDLE);
            return PlayState.CONTINUE;
         } else {
            controller.setAnimationSpeed(1.0);
            controller.reset();
            return PlayState.STOP;
         }
      }).triggerableAnim("happy", HAPPY_EARS).receiveTriggeredAnimations());
      controllers.add(new AnimationController("antennae", 2, state -> {
         BlubEntity blub = (BlubEntity)state.animatable();
         AnimationController<BlubEntity> controller = state.controller();
         if (blub.getVisualVariant() == BlubVariant.DEFAULT) {
            controller.reset();
            return PlayState.STOP;
         } else if (!blub.isBlubSitting() && blub.isMovingForAnimation()) {
            controller.setAnimationSpeed(blub.hoppingAnimationSpeed());
            controller.setAnimation(ANTENNAE_HOPPING);
            return PlayState.CONTINUE;
         } else {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(blub.entityData.get(DATA_FLAPPING) ? ANTENNAE_FLAPPING : ANTENNAE_IDLE);
            return PlayState.CONTINUE;
         }
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private static final class BlubJumpControl extends JumpControl {
      private final BlubEntity blub;
      private boolean canJump;

      private BlubJumpControl(BlubEntity blub) {
         super(blub);
         this.blub = blub;
      }

      private boolean wantJump() {
         return this.jump;
      }

      private boolean canJump() {
         return this.canJump;
      }

      private void setCanJump(boolean canJump) {
         this.canJump = canJump;
      }

      private void cancelJump() {
         this.jump = false;
         this.canJump = false;
      }

      @Override
      public void tick() {
         if (this.jump) {
            this.blub.startJumping();
            this.jump = false;
         }
      }
   }

   private static final class BlubMoveControl extends MoveControl {
      private final BlubEntity blub;
      private double nextJumpSpeed;

      private BlubMoveControl(BlubEntity blub) {
         super(blub);
         this.blub = blub;
      }

      @Override
      public void tick() {
         BlubEntity.BlubJumpControl jumps = (BlubEntity.BlubJumpControl)this.blub.getJumpControl();
         if (this.blub.onGround() && !this.blub.isJumping() && !jumps.wantJump()) {
            this.blub.setSpeedModifier(0.0);
         } else if (this.hasWanted() || this.operation == Operation.JUMPING) {
            this.blub.setSpeedModifier(this.nextJumpSpeed);
         }

         super.tick();
      }

      @Override
      public void setWantedPosition(double x, double y, double z, double speed) {
         if (this.blub.isInWater()) {
            speed = 1.5;
         }

         super.setWantedPosition(x, y, z, speed);
         if (speed > 0.0) {
            this.nextJumpSpeed = speed;
         }
      }
   }

   private final class BlubStackGoal extends Goal {
      private BlubEntity base;
      private int elapsedTicks;
      private int launchTick;
      private boolean jumped;
      private boolean finished;

      private BlubStackGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
      }

      @Override
      public boolean canUse() {
         if (!BlubEntity.this.isInBlubTower()
            && !BlubEntity.this.isPassenger()
            && BlubEntity.this.towerJoiner == null
            && BlubEntity.this.onGround()
            && BlubEntity.this.canPlayInTower()
            && BlubEntity.this.stackCooldownTicks <= 0
            && BlubEntity.this.stackCheckTicks <= 0) {
            BlubEntity.this.stackCheckTicks = 20 * (8 + BlubEntity.this.random.nextInt(13));
            List<BlubEntity> nearby = BlubEntity.this.level()
               .getEntitiesOfClass(
                  BlubEntity.class, BlubEntity.this.getBoundingBox().inflate(6.0, 3.0, 6.0), blubx -> blubx != BlubEntity.this && blubx.isAlive()
               );
            long stacked = nearby.stream().filter(BlubEntity::isInBlubTower).count();
            List<BlubEntity> candidates = new ArrayList<>();

            for (BlubEntity blub : nearby) {
               if (blub.canAcceptTowerJoiner(BlubEntity.this)
                  && BlubEntity.this.hasLineOfSight(blub)
                  && (blub.isInBlubTower() || stacked < Math.max(2, (nearby.size() + 1) / 2))) {
                  candidates.add(blub);
               }
            }

            if (candidates.isEmpty()) {
               return false;
            } else {
               this.base = candidates.get(BlubEntity.this.random.nextInt(candidates.size()));
               int chance = this.base.isInBlubTower() ? 2 : 4;
               if (BlubEntity.this.random.nextInt(chance) != 0) {
                  this.base = null;
                  return false;
               } else {
                  return true;
               }
            }
         } else {
            return false;
         }
      }

      @Override
      public void start() {
         this.elapsedTicks = 0;
         this.jumped = false;
         this.finished = false;
         BlubEntity.this.approachingTower = true;
         this.base.towerJoiner = BlubEntity.this;
         this.base.stackWaitTicks = 120;
         this.base.holdForBlubTower();
         BlubEntity.this.getNavigation().moveTo(this.base, 1.0);
      }

      @Override
      public boolean canContinueToUse() {
         return !this.finished
            && this.elapsedTicks < 120
            && this.base != null
            && !BlubEntity.this.isPassenger()
            && !BlubEntity.this.isVehicle()
            && BlubEntity.this.canPlayInTower()
            && BlubEntity.this.stackCooldownTicks == 0
            && this.base.canAcceptTowerJoiner(BlubEntity.this)
            && this.base.towerJoiner == BlubEntity.this;
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void tick() {
         this.elapsedTicks++;
         List<BlubEntity> tower = this.base.getBlubTower();
         BlubEntity top = tower.get(tower.size() - 1);
         double dx = top.getX() - BlubEntity.this.getX();
         double dz = top.getZ() - BlubEntity.this.getZ();
         double horizontalDistance = dx * dx + dz * dz;
         double landingY = top.getY() + 0.546875;
         if (this.jumped) {
            Vec3 motion = BlubEntity.this.getDeltaMovement();
            BlubEntity.this.setDeltaMovement(Mth.clamp(dx * 0.18, -0.2, 0.2), motion.y, Mth.clamp(dz * 0.18, -0.2, 0.2));
            if (motion.y <= 0.0 && horizontalDistance < 0.0625 && Math.abs(BlubEntity.this.getY() - landingY) < 0.22) {
               if (BlubEntity.this.startRiding(top)) {
                  top.positionRider(BlubEntity.this);
                  BlubEntity.this.holdForBlubTower();

                  for (BlubEntity member : this.base.getBlubTower()) {
                     if (member.towerStayTicks <= 0) {
                        member.towerStayTicks = 20 * (35 + member.random.nextInt(36));
                     }
                  }
               }

               this.finished = true;
            } else if (this.elapsedTicks - this.launchTick > 40 || BlubEntity.this.onGround() && this.elapsedTicks - this.launchTick > 4) {
               this.finished = true;
            }
         } else {
            if (horizontalDistance <= 1.5625 && BlubEntity.this.onGround()) {
               double rise = landingY - BlubEntity.this.getY();
               AABB flight = BlubEntity.this.getBoundingBox().expandTowards(dx, rise + 1.1, dz);
               if (BlubEntity.this.level().getBlockCollisions(BlubEntity.this, flight).iterator().hasNext()) {
                  this.finished = true;
                  return;
               }

               BlubEntity.this.getNavigation().stop();
               BlubEntity.this.stopTowerMovement();
               BlubEntity.this.facePoint(top.getX(), top.getZ());
               BlubEntity.this.startJumping();
               double verticalSpeed = Math.sqrt(0.16 * (Math.max(0.0, rise) + 0.55)) + 0.04;
               BlubEntity.this.setDeltaMovement(dx * 0.1, verticalSpeed, dz * 0.1);
               BlubEntity.this.setOnGround(false);
               this.launchTick = this.elapsedTicks;
               this.jumped = true;
            } else if (this.elapsedTicks % 10 == 0) {
               BlubEntity.this.getNavigation().moveTo(this.base, 1.0);
            }
         }
      }

      @Override
      public void stop() {
         if (this.base != null && this.base.towerJoiner == BlubEntity.this) {
            this.base.towerJoiner = null;
            this.base.stackWaitTicks = 0;
         }

         BlubEntity.this.getNavigation().stop();
         BlubEntity.this.approachingTower = false;
         this.base = null;
      }
   }
}
