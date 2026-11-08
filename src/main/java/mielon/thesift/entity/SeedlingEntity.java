package mielon.thesift.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.storage.ValueInput;

public final class SeedlingEntity extends Monster implements GeoEntity {
   public static final float MODEL_SCALE = 0.8F;
   public static final float WIDTH = 0.71999997F;
   public static final float HEIGHT = 0.76F;
   public static final float EYE_HEIGHT = 0.57600003F;
   private static final String ATTACK_CONTROLLER = "attack_controller";
   private static final String BITE_TRIGGER = "bite";
   private static final int BITE_ANIMATION_TICKS = 14;
   private static final int BITE_SOUND_TICK = 8;
   private static final int BITE_DAMAGE_TICK = 13;
   private static final int BITE_DURATION_TICKS = 14;
   private static final double BITE_REACH = 2.5;
   private static final RawAnimation RUNNING = RawAnimation.begin().thenLoop("running");
   private static final RawAnimation BITE = RawAnimation.begin().thenPlay("attack");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private Entity biteTarget;
   private int biteAge = -1;

   public SeedlingEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 7;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 15.0)
         .add(Attributes.MOVEMENT_SPEED, 0.31)
         .add(Attributes.ATTACK_DAMAGE, 8.0)
         .add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.STEP_HEIGHT, 1.0);
   }

   public static boolean canSpawnOn(BlockState floor) {
      return floor.is(ModBlocks.CORAL_SOIL)
         || floor.is(ModBlocks.CORAL_GROWTH)
         || floor.is(ModBlocks.BRIGHT_CORAL_GROWTH)
         || floor.is(ModBlocks.SIFTSLATE)
         || floor.is(ModBlocks.SOULSHALE)
         || floor.is(ModBlocks.SOULSHALE_GROWTH)
         || floor.is(ModBlocks.COBBLED_SIFTSLATE)
         || floor.is(ModBlocks.SIFTSLATE_GROWTH)
         || floor.is(ModBlocks.HEALTHY_SCULK)
         || floor.is(ModBlocks.HEALTHY_SCULK_CARTILAGE)
         || floor.is(ModBlocks.HEALTHY_SCULK_PORES)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK)
         || floor.is(ModBlocks.DRY_HEALTHY_SCULK_GROWTH);
   }

   public static <T extends Mob> boolean checkSeedlingSpawnRules(
      EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)
         && level.getLevel().getDifficulty() != Difficulty.PEACEFUL
         && Mob.checkMobSpawnRules(type, level, reason, pos, random)
         && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
         && canSpawnOn(level.getBlockState(pos.below()))
         && (pos.getY() >= level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1 || random.nextFloat() < 0.2F);
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
      return SifterFamily.isDroolDamage(source) || super.isInvulnerableTo(level, source);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.18, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.95));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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
   public boolean doHurtTarget(ServerLevel level, Entity target) {
      if (this.biteAge < 0 && target instanceof LivingEntity living && living.isAlive() && this.canAttack(living)) {
         this.biteTarget = target;
         this.biteAge = 0;
         this.playSound(ModSounds.SEEDLING_OPEN_MOUTH, 0.8F, 1.0F);
         this.getNavigation().stop();
         this.getLookControl().setLookAt(target, 30.0F, 30.0F);
         this.triggerAnim("attack_controller", "bite");
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
         this.setTarget(null);
         this.setAggressive(false);
         this.biteAge = -1;
         this.biteTarget = null;
      } else if (this.level() instanceof ServerLevel level && this.biteAge >= 0) {
         this.biteAge++;
         this.getNavigation().stop();
         if (this.biteTarget != null && this.biteTarget.isAlive()) {
            this.getLookControl().setLookAt(this.biteTarget, 30.0F, 30.0F);
         }

         if (this.biteAge == 8) {
            this.playSound(ModSounds.SEEDLING_ATTACK, 0.9F, 0.95F + this.random.nextFloat() * 0.1F);
         }

         if (this.biteAge == 13) {
            this.applyBiteDamage(level);
         }

         if (this.biteAge >= 14) {
            this.biteAge = -1;
            this.biteTarget = null;
         }
      }
   }

   private void applyBiteDamage(ServerLevel level) {
      if (this.biteTarget instanceof LivingEntity living
         && living.level() == this.level()
         && living.isAlive()
         && this.canAttack(living)
         && !(this.distanceToSqr(living) > 6.25)
         && this.getSensing().hasLineOfSight(living)) {
         super.doHurtTarget(level, living);
      }
   }

   @Override
   public void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
      if (health != null && health.getBaseValue() == 40.0) {
         health.setBaseValue(15.0);
         this.setHealth(Math.min(this.getHealth(), 15.0F));
      }

      AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null && attack.getBaseValue() == 10.0) {
         attack.setBaseValue(8.0);
      }
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return TheSiftDimension.isOvergrownBiome(this.level().getBiome(this.blockPosition())) ? 3 : 2;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      if (this.biteAge >= 0) {
         return null;
      } else if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
         return ModSounds.SEEDLING_RUN;
      } else {
         return this.getTarget() != null && this.getTarget().isAlive() ? ModSounds.SEEDLING_AGGRESSIVE : ModSounds.SEEDLING_AMBIENT;
      }
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.SEEDLING_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.SEEDLING_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(ModSounds.SEEDLING_STEP, 0.12F, 0.9F + this.random.nextFloat() * 0.2F);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController("attack_controller", 3, state -> {
         AnimationController<SeedlingEntity> controller = state.controller();
         if (controller.isTriggeredAnimation("bite")) {
            controller.setAnimationSpeed(1.0);
            if (controller.getCurrentTimelineTime() != -2.0) {
               return PlayState.CONTINUE;
            }

            controller.stopTriggeredAnimation();
         }

         if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            controller.setAnimationSpeed(2.5);
            controller.setAnimation(RUNNING);
            return PlayState.CONTINUE;
         } else {
            controller.setAnimationSpeed(1.0);
            controller.reset();
            return PlayState.STOP;
         }
      }).triggerableAnim("bite", BITE).receiveTriggeredAnimations());
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
