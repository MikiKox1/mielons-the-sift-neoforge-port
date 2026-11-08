package mielon.thesift.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mielon.thesift.mixin.SnifferStateAccess;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.sniffer.Sniffer.State;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DarkSnifferEntity extends Sniffer {
   private static final double AGGRO_RANGE = 32.0;
   private static final double ATTACK_REACH = 3.15;
   private static final int ATTACK_COOLDOWN_TICKS = 24;
   private static final int SONIC_WINDUP_TICKS = 10;
   private static final int SONIC_COOLDOWN_TICKS = 100;
   private static final int SONIC_MIN_OUT_OF_REACH_TICKS = 8;
   private static final double SONIC_MIN_DISTANCE = 5.0;
   private static final double SONIC_WAVE_SPEED = 1.5;
   private static final double SONIC_HIT_RADIUS = 0.8;
   private static final double SONIC_OVERSHOOT_DISTANCE = 7.5;
   private static final double SONIC_MAX_LEAD_TICKS = 20.0;
   private static final double SONIC_PULL_STRENGTH = 0.3;
   private static final double NORMAL_SPEED = 0.1;
   private static final double WARDEN_CHASE_SPEED = 1.2;
   private int targetRefreshCooldown;
   private int pathRefreshCooldown;
   private int attackCooldown;
   private Warden wardenPetter;
   private int wardenPettingTicks;
   private int sonicCooldown;
   private int sonicWindupTicks;
   private int sonicFlightTicks;
   private int sonicFlightAge;
   private int ticksWithoutMeleeContact;
   private LivingEntity sonicVictim;
   private LivingEntity observedCombatTarget;
   private Vec3 sonicOrigin = Vec3.ZERO;
   private Vec3 sonicDirection = Vec3.ZERO;
   private final Set<UUID> sonicPulledEntityUuids = new HashSet<>();
   private double homeX;
   private double homeY;
   private double homeZ;
   private boolean homeSet;

   public DarkSnifferEntity(EntityType<? extends Animal> type, Level level) {
      super(type, level);
      this.targetRefreshCooldown = level.getRandom().nextInt(10);
      this.pathRefreshCooldown = level.getRandom().nextInt(6);
      this.setPathfindingMalus(PathType.LEAVES, 0.0F);
      this.setPathfindingMalus(PathType.COCOA, 0.0F);
      this.setPathfindingMalus(PathType.DAMAGE_CAUTIOUS, 0.0F);
      if (this.getNavigation() instanceof GroundPathNavigation navigation) {
         navigation.setCanPathToTargetsBelowSurface(true);
      }
   }

   public static boolean checkDarkSnifferSpawnRules(
      EntityType<DarkSnifferEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
   ) {
      return level.getDifficulty() != Difficulty.PEACEFUL
         && level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT)
         && (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION || level.getBrightness(LightLayer.BLOCK, pos) == 0)
         && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
         && (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION || SculkerPopulation.canSpawn(level, pos, false))
         && level.getFluidState(pos).isEmpty();
   }

   @Override
   public float getWalkTargetValue(BlockPos pos, LevelReader level) {
      if (level.getBlockState(pos.below()).is(Blocks.SCULK)) {
         return 10.0F;
      } else {
         return level.getBiome(pos).is(TheSiftDimension.CORRUPTED_SIFT) ? -level.getBrightness(LightLayer.BLOCK, pos) : super.getWalkTargetValue(pos, level);
      }
   }

   public static Builder createAttributes() {
      return Sniffer.createAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.1)
         .add(Attributes.ATTACK_DAMAGE, 7.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.STEP_HEIGHT, 1.25)
         .add(Attributes.MOVEMENT_EFFICIENCY, 1.0)
         .add(Attributes.ATTACK_KNOCKBACK, 0.75);
   }

   @Override
   protected boolean canBeABaby() {
      return false;
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
   public boolean canSniff() {
      return this.wardenPettingTicks <= 0 && !this.hasCombatTarget() && super.canSniff();
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
         return false;
      }

      boolean validPlayer = target instanceof Player player && !player.isCreative() && !player.isSpectator();
      boolean ordinarySniffer = target instanceof Sniffer && !(target instanceof DarkSnifferEntity);
      return (validPlayer || ordinarySniffer || SifterFamily.isMember(target)) && super.canAttack(target);
   }

   @Override
   public void checkDespawn() {
      if (!SculkerPopulation.despawnCrowded(this)) {
         super.checkDespawn();
      }
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel serverLevel) {
         if (this.attackCooldown > 0) {
            this.attackCooldown--;
         }

         if (this.sonicCooldown > 0) {
            this.sonicCooldown--;
         }

         if (!this.homeSet) {
            this.homeX = this.getX();
            this.homeY = this.getY();
            this.homeZ = this.getZ();
            this.homeSet = true;
         }

         if (this.wardenPettingTicks > 0) {
            this.cancelSonicSniff();
            this.wardenPettingTicks--;
            this.clearCombatTarget();
            this.transitionTo(State.IDLING);
            if (this.wardenPetter != null && this.wardenPetter.isAlive() && this.distanceToSqr(this.wardenPetter) <= 576.0) {
               this.getLookControl().setLookAt(this.wardenPetter, 20.0F, 20.0F);
            } else {
               this.wardenPetter = null;
               this.wardenPettingTicks = 0;
            }
         } else if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.cancelSonicSniff();
            this.clearCombatTarget();
         } else if (this.sonicWindupTicks <= 0 && this.sonicFlightTicks <= 0) {
            LivingEntity target = this.getTarget();
            if (!this.isValidTarget(target)) {
               target = null;
               this.setTarget(null);
            }

            if (--this.targetRefreshCooldown <= 0) {
               this.targetRefreshCooldown = 10;
               Player nearestPlayer = this.level()
                  .getNearestPlayer(
                     this.getX(),
                     this.getY(),
                     this.getZ(),
                     32.0,
                     entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator() && player.isAlive()
                  );
               LivingEntity nearestPrey = this.findNearestPrey(serverLevel);
               target = nearestPlayer;
               if (nearestPrey != null && (target == null || this.distanceToSqr(nearestPrey) < this.distanceToSqr(target))) {
                  target = nearestPrey;
               }

               this.setTarget(target);
            }

            if (target == null) {
               this.observedCombatTarget = null;
               this.ticksWithoutMeleeContact = 0;
               this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1);
               this.setAggressive(false);
            } else {
               this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
               this.setAggressive(true);
               if (this.observedCombatTarget != target) {
                  this.observedCombatTarget = target;
                  this.ticksWithoutMeleeContact = 0;
               }

               double targetDistanceSqr = this.distanceToSqr(target);
               if (targetDistanceSqr > 9.9225) {
                  this.ticksWithoutMeleeContact = Math.min(8, this.ticksWithoutMeleeContact + 1);
               } else {
                  this.ticksWithoutMeleeContact = 0;
               }

               boolean validSonicRange = targetDistanceSqr >= 25.0 && this.getSensing().hasLineOfSight(target);
               if (this.sonicCooldown <= 0 && this.ticksWithoutMeleeContact >= 8 && validSonicRange) {
                  this.beginSonicSniff(target);
               } else {
                  this.transitionTo(State.IDLING);
                  this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                  if (--this.pathRefreshCooldown <= 0) {
                     this.pathRefreshCooldown = 6;
                     this.getNavigation().moveTo(target, 1.2);
                  }

                  if (this.horizontalCollision && this.onGround() && Math.abs(target.getY() - this.getY()) <= 3.0) {
                     this.getJumpControl().jump();
                  }

                  if (this.attackCooldown <= 0 && this.distanceToSqr(target) <= 9.9225 && this.doHurtTarget(serverLevel, target)) {
                     this.attackCooldown = 24;
                     this.ticksWithoutMeleeContact = 0;
                  }
               }
            }
         } else {
            this.tickSonicSniff(serverLevel);
         }
      }
   }

   private void pauseRunningForSonicSniff() {
      this.getNavigation().stop();
      this.setSpeed(0.0F);
      this.setXxa(0.0F);
      this.setZza(0.0F);
      this.setJumping(false);
   }

   private void beginSonicSniff(LivingEntity target) {
      this.sonicVictim = target;
      this.sonicWindupTicks = 10;
      this.sonicFlightTicks = 0;
      this.sonicFlightAge = 0;
      this.sonicCooldown = 100;
      this.ticksWithoutMeleeContact = 0;
      this.pauseRunningForSonicSniff();
      this.getLookControl().setLookAt(target, 30.0F, 30.0F);
      this.setSnifferState(State.SNIFFING);
   }

   private void tickSonicSniff(ServerLevel serverLevel) {
      LivingEntity victim = this.sonicVictim;
      if (victim != null && victim.isAlive() && this.canAttack(victim)) {
         this.pauseRunningForSonicSniff();
         this.getLookControl().setLookAt(victim, 30.0F, 30.0F);
         if (this.sonicWindupTicks > 0) {
            this.sonicWindupTicks--;
            this.setSnifferState(State.SNIFFING);
            if (this.sonicWindupTicks == 0) {
               this.launchSonicWave(serverLevel, victim);
            }
         } else {
            this.setSnifferState(State.SNIFFING);
            Vec3 previousWavePosition = this.sonicOrigin.add(this.sonicDirection.scale(1.5 * this.sonicFlightAge));
            this.sonicFlightAge++;
            Vec3 wavePosition = this.sonicOrigin.add(this.sonicDirection.scale(1.5 * this.sonicFlightAge));
            this.pullMobsAlongWave(serverLevel, previousWavePosition, wavePosition);
            if (victim.getBoundingBox().inflate(0.8).intersects(previousWavePosition, wavePosition)) {
               this.pullEntityOnce(victim);
            }

            if (this.sonicFlightAge >= this.sonicFlightTicks) {
               this.cancelSonicSniff();
               this.transitionTo(State.IDLING);
            }
         }
      } else {
         this.cancelSonicSniff();
         this.transitionTo(State.IDLING);
      }
   }

   private void launchSonicWave(ServerLevel serverLevel, LivingEntity victim) {
      Vec3 start = this.getEyePosition();
      Vec3 targetPosition = victim.getEyePosition();
      Vec3 directDelta = targetPosition.subtract(start);
      double directDistance = directDelta.length();
      if (!(directDistance < 0.001) && this.getSensing().hasLineOfSight(victim)) {
         double leadTicks = Math.min(20.0, directDistance / 1.5);
         Vec3 victimVelocity = victim.getDeltaMovement();
         Vec3 predictedTarget = targetPosition.add(victimVelocity.x() * leadTicks, 0.0, victimVelocity.z() * leadTicks);
         Vec3 delta = predictedTarget.subtract(start);
         double predictedDistance = delta.length();
         if (predictedDistance < 0.001) {
            delta = directDelta;
            predictedDistance = directDistance;
         }

         this.sonicOrigin = start;
         this.sonicDirection = delta.scale(1.0 / predictedDistance);
         double travelDistance = predictedDistance + 7.5;
         this.sonicFlightTicks = Math.max(1, (int)Math.ceil(travelDistance / 1.5));
         this.sonicFlightAge = 0;
         this.setSnifferState(State.SNIFFING);
         this.playSound(ModSounds.DARK_SNIFFER_SNIFF, 2.5F, 0.78F);
         double encodedMagnitude = 1.0 + this.sonicFlightTicks / 100.0;
         serverLevel.sendParticles(
            ModParticles.SOUND_WAVE,
            start.x(),
            start.y(),
            start.z(),
            0,
            this.sonicDirection.x() * encodedMagnitude,
            this.sonicDirection.y() * encodedMagnitude,
            this.sonicDirection.z() * encodedMagnitude,
            1.0
         );
      } else {
         this.cancelSonicSniff();
         this.transitionTo(State.IDLING);
      }
   }

   private void pullMobsAlongWave(ServerLevel serverLevel, Vec3 previousWavePosition, Vec3 wavePosition) {
      AABB searchBox = new AABB(previousWavePosition, wavePosition).inflate(0.8);

      for (LivingEntity mob : serverLevel.getEntitiesOfClass(
         LivingEntity.class, searchBox, mobx -> mobx != this && mobx.isAlive() && !this.sonicPulledEntityUuids.contains(mobx.getUUID())
      )) {
         if (mob.getBoundingBox().inflate(0.8).intersects(previousWavePosition, wavePosition)) {
            this.pullEntityOnce(mob);
         }
      }
   }

   private void pullEntityOnce(LivingEntity victim) {
      if (victim != this && victim.isAlive() && this.sonicPulledEntityUuids.add(victim.getUUID())) {
         Vec3 pull = new Vec3(this.getX() - victim.getX(), this.getY() - victim.getY(), this.getZ() - victim.getZ()).scale(0.3);
         victim.setDeltaMovement(victim.getDeltaMovement().add(pull));
         if (victim instanceof Player) {
            ((SonicSniffMotionAccess)victim).theSift$markSonicSniffMotion();
         }

         victim.hurtMarked = true;
         if (victim instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(victim));
         }
      }
   }

   private void cancelSonicSniff() {
      this.sonicWindupTicks = 0;
      this.sonicFlightTicks = 0;
      this.sonicFlightAge = 0;
      this.sonicVictim = null;
      this.sonicOrigin = Vec3.ZERO;
      this.sonicDirection = Vec3.ZERO;
      this.sonicPulledEntityUuids.clear();
      this.ticksWithoutMeleeContact = 0;
   }

   private void setSnifferState(State state) {
      ((SnifferStateAccess)(Object)this).theSift$setState(state);
   }

   private boolean hasCombatTarget() {
      return this.isValidTarget(this.getTarget());
   }

   private boolean isValidTarget(LivingEntity target) {
      return target != null && target.isAlive() && this.canAttack(target) && this.distanceToSqr(target) <= 1024.0;
   }

   private LivingEntity findNearestPrey(ServerLevel level) {
      LivingEntity nearest = null;
      double nearestDistance = 1024.0;

      for (LivingEntity candidate : level.getEntitiesOfClass(
         LivingEntity.class,
         this.getBoundingBox().inflate(32.0),
         candidatex -> (candidatex instanceof Sniffer && !(candidatex instanceof DarkSnifferEntity) || SifterFamily.isMember(candidatex))
            && candidatex.isAlive()
            && this.canAttack(candidatex)
      )) {
         double distance = this.distanceToSqr(candidate);
         if (distance < nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
         }
      }

      return nearest;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("TheSiftHomeSet", this.homeSet);
      if (this.homeSet) {
         output.putDouble("TheSiftHomeX", this.homeX);
         output.putDouble("TheSiftHomeY", this.homeY);
         output.putDouble("TheSiftHomeZ", this.homeZ);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      this.homeSet = input.getBooleanOr("TheSiftHomeSet", false);
      if (this.homeSet) {
         this.homeX = input.getDoubleOr("TheSiftHomeX", this.getX());
         this.homeY = input.getDoubleOr("TheSiftHomeY", this.getY());
         this.homeZ = input.getDoubleOr("TheSiftHomeZ", this.getZ());
      }
   }

   public boolean isAvailableForWardenPetting() {
      return this.isAlive() && !this.isAggressive() && !this.hasCombatTarget() && this.wardenPettingTicks <= 0;
   }

   public void beginWardenPetting(Warden warden, int durationTicks) {
      if (this.isAlive()) {
         this.wardenPetter = warden;
         this.wardenPettingTicks = Math.max(this.wardenPettingTicks, durationTicks);
         this.clearCombatTarget();
      }
   }

   public void endWardenPetting(Warden warden) {
      if (this.wardenPetter == warden) {
         this.wardenPetter = null;
         this.wardenPettingTicks = 0;
      }
   }

   private void clearCombatTarget() {
      this.setTarget(null);
      this.setAggressive(false);
      this.getNavigation().stop();
   }
}
