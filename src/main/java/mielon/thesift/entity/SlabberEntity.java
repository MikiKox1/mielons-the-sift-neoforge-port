package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;
import java.util.List;
import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftSculkBlobResidents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public final class SlabberEntity extends Animal implements GeoEntity {
   public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("the_sift", "slabber_food"));
   public static final float MODEL_SCALE = 1.0F;
   public static final float WIDTH = 1.5F;
   public static final float HEIGHT = 1.6875F;
   public static final float EYE_HEIGHT = 1.3125F;
   private static final EntityDataAccessor<Boolean> DATA_LICKING = SynchedEntityData.defineId(SlabberEntity.class, EntityDataSerializers.BOOLEAN);
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
   private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
   private static final RawAnimation LICK = RawAnimation.begin().thenLoop("lick");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private long nextSocialTick;

   public SlabberEntity(EntityType<? extends Animal> type, Level level) {
      super(type, level);
      this.lookControl = new LookControl(this) {
         @Override
         public void tick() {
            if (this.lookAtCooldown > 0) {
               this.lookAtCooldown--;
               if (SlabberEntity.this.getNavigation().isDone() && !SlabberEntity.this.getMoveControl().hasWanted()) {
                  this.getYRotD()
                     .ifPresent(yaw -> SlabberEntity.this.setYRot(this.rotateTowards(SlabberEntity.this.getYRot(), yaw, Math.min(this.yMaxRotSpeed, 10.0F))));
               }
            }

            SlabberEntity.this.setXRot(0.0F);
            SlabberEntity.this.yHeadRot = SlabberEntity.this.getYRot();
         }
      };
      this.getNavigation().setCanFloat(true);
      this.nextSocialTick = level.getGameTime() + 100L + this.random.nextInt(141);
   }

   public static Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 10.0)
         .add(Attributes.STEP_HEIGHT, 1.0)
         .add(Attributes.MOVEMENT_SPEED, 0.2)
         .add(Attributes.FOLLOW_RANGE, 16.0)
         .add(Attributes.TEMPT_RANGE, 10.0);
   }

   public static boolean checkSlabberSpawnRules(
      EntityType<SlabberEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      BlockPos below = pos.below();
      BlockState floor = level.getBlockState(below);
      return reason == EntitySpawnReason.NATURAL && SiftSculkBlobResidents.blocksNaturalSpawn(level, pos)
         ? false
         : level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)
            && Mob.checkMobSpawnRules(type, level, reason, pos, random)
            && floor.isFaceSturdy(level, below, Direction.UP)
            && (
               SeedlingEntity.canSpawnOn(floor)
                  || floor.is(Blocks.SCULK)
                  || floor.is(Blocks.BONE_BLOCK)
                  || floor.is(ModBlocks.ICHOR_SNOW_BLOCK)
                  || floor.is(ModBlocks.SIFT_RUBBLE)
            );
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_LICKING, false);
   }

   @Override
   public int getMaxHeadYRot() {
      return 0;
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new SlabberEntity.SlabberNavigation(this, level);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.5));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, stack -> this.isFood(stack), false));
      this.goalSelector.addGoal(4, new SlabberEntity.LickGoal());
      this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.05));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 7.0F));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Mob.class, 6.0F, 0.02F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
   }

   @Override
   public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
      return ModEntities.SLABBER.create(level, EntitySpawnReason.BREEDING);
   }

   @Override
   public boolean isFood(ItemStack stack) {
      return stack.is(FOOD);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 4;
   }

   public boolean isLicking() {
      return this.entityData.get(DATA_LICKING);
   }

   public boolean canLick(LivingEntity other) {
      if (other == this
         || other instanceof SlabberEntity
         || !other.isAlive()
         || other.isSpectator()
         || other.level() != this.level()
         || this.isInLove()
         || this.isOnFire()
         || other.isInvisible()
         || Math.abs(other.getY() - this.getY()) > 1.25) {
         return false;
      } else if (this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < 200) {
         return false;
      } else {
         return !(
               other instanceof Mob mob
                  && (mob.getTarget() == this || mob.isAggressive() || mob.getLastHurtByMob() == this && mob.tickCount - mob.getLastHurtByMobTimestamp() < 200)
            )
            ? other != this.getLastHurtByMob() || this.tickCount - this.getLastHurtByMobTimestamp() >= 200
            : false;
      }
   }

   private double lickReach(LivingEntity other) {
      return 1.35 * (this.isBaby() ? 0.5 : 1.0) + other.getBbWidth() * 0.5 + 0.5;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.SLABBER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource damage) {
      return ModSounds.SLABBER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.SLABBER_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.SLABBER_STEP, 0.15F, 1.0F);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("movement", 5, state -> {
         AnimationController<SlabberEntity> controller = state.controller();
         if (this.isLicking()) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(LICK);
         } else if (state.isMoving()) {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(WALK);
         } else {
            controller.setAnimationSpeed(1.0);
            controller.setAnimation(IDLE);
         }

         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private final class LickGoal extends Goal {
      private LivingEntity recipient;
      private Path approach;
      private int elapsed;
      private int contactTicks;
      private int repathTicks;

      LickGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public boolean canUse() {
         if (SlabberEntity.this.level().getGameTime() >= SlabberEntity.this.nextSocialTick
            && !SlabberEntity.this.isPassenger()
            && !SlabberEntity.this.isLeashed()
            && !SlabberEntity.this.isInLove()
            && (SlabberEntity.this.getLastHurtByMob() == null || SlabberEntity.this.tickCount - SlabberEntity.this.getLastHurtByMobTimestamp() >= 200)) {
            SlabberEntity.this.nextSocialTick = SlabberEntity.this.level().getGameTime() + 40L + SlabberEntity.this.random.nextInt(61);
            List<LivingEntity> nearby = SlabberEntity.this.level()
               .getEntitiesOfClass(LivingEntity.class, SlabberEntity.this.getBoundingBox().inflate(6.0, 1.25, 6.0), SlabberEntity.this::canLick);

            while (!nearby.isEmpty()) {
               LivingEntity candidate = nearby.remove(SlabberEntity.this.random.nextInt(nearby.size()));
               if (SlabberEntity.this.getSensing().hasLineOfSight(candidate)) {
                  double reach = SlabberEntity.this.lickReach(candidate);
                  Path path = SlabberEntity.this.getNavigation().createPath(candidate, 0);
                  if (!(SlabberEntity.this.distanceToSqr(candidate) > reach * reach) || path != null && path.canReach()) {
                     this.recipient = candidate;
                     this.approach = path;
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      }

      @Override
      public void start() {
         this.elapsed = this.contactTicks = this.repathTicks = 0;
         double reach = SlabberEntity.this.lickReach(this.recipient);
         if (this.approach != null && SlabberEntity.this.distanceToSqr(this.recipient) > reach * reach) {
            SlabberEntity.this.getNavigation().moveTo(this.approach, 0.95);
         }
      }

      @Override
      public boolean canContinueToUse() {
         return this.recipient != null
            && SlabberEntity.this.canLick(this.recipient)
            && this.elapsed < 160
            && this.contactTicks < 56
            && SlabberEntity.this.distanceToSqr(this.recipient) < 64.0
            && SlabberEntity.this.getSensing().hasLineOfSight(this.recipient);
      }

      @Override
      public void tick() {
         this.elapsed++;
         SlabberEntity.this.getLookControl()
            .setLookAt(this.recipient.getX(), this.recipient.getY() + Math.min(this.recipient.getBbHeight() * 0.45, 1.15), this.recipient.getZ(), 25.0F, 15.0F);
         double reach = SlabberEntity.this.lickReach(this.recipient);
         if (SlabberEntity.this.distanceToSqr(this.recipient) <= reach * reach) {
            SlabberEntity.this.getNavigation().stop();
            float facing = (float)(
                  Math.atan2(this.recipient.getZ() - SlabberEntity.this.getZ(), this.recipient.getX() - SlabberEntity.this.getX()) * 180.0 / Math.PI
               )
               - 90.0F;
            boolean facingRecipient = Math.abs(Mth.wrapDegrees(facing - SlabberEntity.this.getYRot())) <= 20.0F;
            SlabberEntity.this.entityData.set(SlabberEntity.DATA_LICKING, facingRecipient);
            if (facingRecipient) {
               this.contactTicks++;
               if (this.contactTicks == 1 && this.recipient instanceof ServerPlayer player) {
                  ModAdvancements.award(player, "the_sift/awkward_moment");
               }
            }
         } else {
            SlabberEntity.this.entityData.set(SlabberEntity.DATA_LICKING, false);
            if (++this.repathTicks >= 10) {
               this.repathTicks = 0;
               SlabberEntity.this.getNavigation().moveTo(this.recipient, 0.95);
            }
         }
      }

      @Override
      public void stop() {
         SlabberEntity.this.entityData.set(SlabberEntity.DATA_LICKING, false);
         SlabberEntity.this.getNavigation().stop();
         this.recipient = null;
         this.approach = null;
         SlabberEntity.this.nextSocialTick = SlabberEntity.this.level().getGameTime() + 240L + SlabberEntity.this.random.nextInt(361);
      }
   }

   private static final class SlabberNavigation extends GroundPathNavigation {
      SlabberNavigation(SlabberEntity mob, Level level) {
         super(mob, level);
      }

      @Override
      protected void followThePath() {
         Vec3 position = this.getTempMobPos();
         Vec3 waypoint = this.path.getNextEntityPos(this.mob);
         this.maxDistanceToWaypoint = Math.max(0.3F, Math.min(0.55F, this.mob.getBbWidth() * 0.5F));
         double rise = this.mob.getY() - waypoint.y;
         if (Math.abs(this.mob.getX() - waypoint.x) <= this.maxDistanceToWaypoint
            && Math.abs(this.mob.getZ() - waypoint.z) <= this.maxDistanceToWaypoint
            && rise > -0.65
            && rise < Math.max(1.25, this.mob.maxUpStep() + 0.25)) {
            this.path.advance();
         }

         this.doStuckDetection(position);
      }
   }
}
