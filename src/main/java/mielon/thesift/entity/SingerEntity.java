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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousDeepslateBlock;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class SingerEntity extends PathfinderMob implements GeoEntity {
   private static final String CONTROLLER = "singer_sequence";
   private static final EntityDataAccessor<Boolean> DATA_SEQUENCE_STARTED = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_SEQUENCE_TICK = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_SOUL_EVENT = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_SOUL_EVENT_PHASE = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SOUL_EVENT_TICK = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.INT);
   private static final RawAnimation EVENT_SING = RawAnimation.begin().thenPlay("sing");
   private static final RawAnimation EVENT_APPEAR = RawAnimation.begin().thenPlay("appear");
   private static final RawAnimation EVENT_WALK = RawAnimation.begin().thenLoop("walk");
   private static final RawAnimation EVENT_IDLE = RawAnimation.begin().thenLoop("idle");
   private static final EntityDataAccessor<Boolean> DATA_HOLDING_SOUL = SynchedEntityData.defineId(SingerEntity.class, EntityDataSerializers.BOOLEAN);
   private static final RawAnimation BARTER_HOLD = RawAnimation.begin().thenLoop("barter_hold");
   private static final int BARTER_HIDE_TICK = 40;
   private static final RawAnimation EVENT_RECEIVE = RawAnimation.begin().thenPlay("receive_soul");
   private static final RawAnimation EVENT_DISAPPEAR = RawAnimation.begin().thenPlay("disappear");
   private static final int SOUL_TRANSFER_TICK = 22;
   private static final int WARDEN_DIGGING_PARTICLES_PER_TICK = 30;
   private static final int WARDEN_DIGGING_PARTICLE_DURATION_TICKS = 90;
   private static final float WARDEN_DIGGING_PARTICLE_OFFSET = 0.7F;
   private static final int HORN_SEARCH_RADIUS = 20;
   private static final int MAX_HORN_TARGETS = 8;
   private static final double SOUND_WAVE_SPEED = 0.6;
   private static final double SINGER_HEAD_Y_OFFSET = 3.0;
   private static final int[] SOUND_WAVE_TICKS = new int[]{
      secondsToTicks(0.16666666666666666),
      secondsToTicks(1.25),
      secondsToTicks(2.0833333333333335),
      secondsToTicks(2.8333333333333335),
      secondsToTicks(3.75),
      secondsToTicks(4.583333333333333),
      secondsToTicks(4.916666666666667),
      secondsToTicks(5.833333333333333)
   };
   private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
   private int sequenceSyncClientTick;
   private int soulSyncClientTick;
   private SingerAnimationTimings timings;
   private int sequenceTick;
   private boolean started;
   private boolean singSoundPlayed;
   private boolean hornTargetsPrepared;
   private boolean disappearSoundPlayed;
   private boolean restoreLegacyHornTargets;
   private List<BlockPos> hornTargets = List.of();
   private final List<SingerEntity.PendingHornActivation> pendingHornActivations = new ArrayList<>();
   private BlockPos portalCenter;
   private UUID soulEventGolem;
   private int soulEventTick;
   private int roamMoveCooldown;
   private boolean soulRoamWalking;
   private int canyonAwarenessCooldown;
   private BlockPos avoidedSoulCanyon;
   private boolean permanentSoulEvent;
   private UUID barterPlayer;
   private Vec3 barterTarget;
   private ItemEntity barterPickupTarget;

   public SingerEntity(EntityType<? extends SingerEntity> type, Level level) {
      super(type, level);
      if (this.getNavigation() instanceof GroundPathNavigation groundNavigation) {
         groundNavigation.setCanPathToTargetsBelowSurface(true);
      }

      this.noPhysics = true;
      this.setNoGravity(true);
      this.setNoAi(true);
      this.setPermanentlyInvulnerable(true);
      this.timings = SingerAnimationTimings.load();
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(DATA_SEQUENCE_STARTED, false);
      builder.define(DATA_SEQUENCE_TICK, 0);
      builder.define(DATA_SOUL_EVENT, false);
      builder.define(DATA_SOUL_EVENT_PHASE, SingerEntity.SoulEventPhase.APPEAR.ordinal());
      builder.define(DATA_SOUL_EVENT_TICK, 0);
      builder.define(DATA_HOLDING_SOUL, false);
   }

   public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.27)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
         .add(Attributes.SCALE, 1.2);
   }

   public void setPortalCenter(BlockPos portalCenter) {
      this.portalCenter = portalCenter == null ? null : portalCenter.immutable();
   }

   public BlockPos getPortalCenter() {
      return this.portalCenter;
   }

   public void beginSequence() {
      if (!this.started) {
         this.started = true;
         this.entityData.set(DATA_SEQUENCE_STARTED, true);
         this.entityData.set(DATA_SEQUENCE_TICK, 0);
         this.timings = SingerAnimationTimings.load();
         this.sequenceTick = 0;
         this.singSoundPlayed = false;
         this.hornTargetsPrepared = false;
         this.disappearSoundPlayed = false;
         this.hornTargets = List.of();
         this.pendingHornActivations.clear();
         if (!this.level().isClientSide()) {
            this.playAppearEffects();
         }
      }
   }

   public void beginSoulEvent(UUID golemId) {
      this.permanentSoulEvent = false;
      this.entityData.set(DATA_SOUL_EVENT, true);
      this.started = false;
      this.soulEventGolem = golemId;
      this.soulEventTick = 0;
      this.roamMoveCooldown = 0;
      this.setSoulEventPhase(SingerEntity.SoulEventPhase.APPEAR);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setNoAi(true);
      this.setPermanentlyInvulnerable(false);
      this.setHealth(this.getMaxHealth());
   }

   private void beginPermanentSoulEvent() {
      this.beginSoulEvent(null);
      this.permanentSoulEvent = true;
      this.setPersistenceRequired();
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
      if ((reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.COMMAND) && !this.isSoulEvent() && this.portalCenter == null) {
         this.beginPermanentSoulEvent();
      }

      return result;
   }

   public boolean isSoulEvent() {
      return this.entityData.get(DATA_SOUL_EVENT);
   }

   public boolean isHoldingSoulBlock() {
      return this.entityData.get(DATA_HOLDING_SOUL);
   }

   public boolean canBarter() {
      return this.isAlive() && this.isSoulEvent() && this.soulEventPhase() == SingerEntity.SoulEventPhase.ROAMING;
   }

   public void beginBarter(ServerPlayer player, Vec3 target) {
      if (this.canBarter()) {
         this.barterPlayer = player == null ? null : player.getUUID();
         this.barterTarget = target;
         this.barterPickupTarget = null;
         this.getNavigation().stop();
         this.setSoulEventPhase(SingerEntity.SoulEventPhase.BARTER);
         this.entityData.set(DATA_HOLDING_SOUL, true);
         if (player != null) {
            this.faceSoulPartner(player);
         }

         this.playSound(ModSounds.SINGER_HAPPY, 1.0F, 1.0F);
      }
   }

   private void tickBarter(ServerLevel level) {
      this.getNavigation().stop();
      ServerPlayer player = this.barterPlayer == null ? null : level.getServer().getPlayerList().getPlayer(this.barterPlayer);
      if (player != null && player.level() == level && this.distanceToSqr(player) <= 256.0) {
         this.barterTarget = player.position();
         this.faceSoulPartner(player);
      }

      if (this.soulEventTick >= 40) {
         this.entityData.set(DATA_HOLDING_SOUL, false);
         SingerBarter.reward(this, level, player, this.barterTarget == null ? this.position().add(this.getLookAngle()) : this.barterTarget);
         this.barterPlayer = null;
         this.barterTarget = null;
         this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
      }
   }

   private boolean seekBarterPayment(ServerLevel level) {
      if (this.barterPickupTarget != null
         && (
            !this.barterPickupTarget.isAlive()
               || !this.barterPickupTarget.getItem().is(ModBlocks.SOUL_BLOCK.asItem())
               || this.distanceToSqr(this.barterPickupTarget) > 64.0
         )) {
         this.barterPickupTarget = null;
      }

      if (this.barterPickupTarget == null && this.tickCount % 10 == 0) {
         this.barterPickupTarget = level.getEntitiesOfClass(
               ItemEntity.class, this.getBoundingBox().inflate(8.0, 3.0, 8.0), item -> item.isAlive() && item.getItem().is(ModBlocks.SOUL_BLOCK.asItem())
            )
            .stream()
            .min(Comparator.comparingDouble(this::distanceToSqr))
            .orElse(null);
      }

      if (this.barterPickupTarget == null) {
         return false;
      } else if (this.getBoundingBox().inflate(0.8).intersects(this.barterPickupTarget.getBoundingBox())) {
         SingerBarter.pickUp(this, this.barterPickupTarget);
         return true;
      } else if ((this.getNavigation().isDone() || this.tickCount % 10 == 0)
         && !this.getNavigation().moveTo(this.barterPickupTarget.getX(), this.barterPickupTarget.getY(), this.barterPickupTarget.getZ(), 1.0)) {
         this.barterPickupTarget = null;
         return false;
      } else {
         return true;
      }
   }

   private void enterPersistentRoaming() {
      this.permanentSoulEvent = true;
      this.setPersistenceRequired();
      this.started = false;
      this.entityData.set(DATA_SEQUENCE_STARTED, false);
      this.entityData.set(DATA_SOUL_EVENT, true);
      this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
      this.noPhysics = false;
      this.setNoGravity(false);
      this.setNoAi(false);
      this.setPermanentlyInvulnerable(false);
   }

   public void offerSoulGolem(EchoGolemEntity golem) {
      if (this.isSoulEvent() && golem.hasSoulBlock() && golem.canInteractWithSinger()) {
         SingerEntity.SoulEventPhase phase = this.soulEventPhase();
         if (phase == SingerEntity.SoulEventPhase.ROAMING
            || this.soulEventGolem != null && this.level() instanceof ServerLevel level && level.getEntityInAnyDimension(this.soulEventGolem) == null) {
            this.soulEventGolem = golem.getUUID();
         }
      }
   }

   @Override
   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      InteractionResult result = SingerBarter.interact(this, player, hand);
      return result == InteractionResult.PASS ? super.mobInteract(player, hand) : result;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         this.tickClientDiggingParticles();
      } else {
         if (!this.started && !this.isSoulEvent() && this.portalCenter == null) {
            this.beginPermanentSoulEvent();
         }

         if (this.hasCustomName()) {
            this.permanentSoulEvent = true;
            this.setPersistenceRequired();
            if (this.isSoulEvent() && this.soulEventPhase() == SingerEntity.SoulEventPhase.DISAPPEAR) {
               this.enterPersistentRoaming();
            }
         }

         this.tickPendingHornActivations();
         if (this.isSoulEvent()) {
            this.tickSoulEvent((ServerLevel)this.level());
         } else if (this.started) {
            if (this.sequenceTick >= this.timings.appearTicks() && !this.singSoundPlayed) {
               this.singSoundPlayed = true;
               this.prepareHornTargets();
               this.playSingSoundFromSinger();
            }

            if (this.restoreLegacyHornTargets) {
               this.restoreLegacyHornTargets = false;
               this.hornTargetsPrepared = false;
               this.prepareHornTargets(true);
            }

            this.tickSoundWaveLaunches();
            int disappearStart = this.timings.appearTicks() + this.timings.singTicks();
            if (this.sequenceTick >= disappearStart) {
               if (this.hasCustomName()) {
                  this.enterPersistentRoaming();
                  return;
               }

               if (!this.disappearSoundPlayed) {
                  this.disappearSoundPlayed = true;
                  this.playDisappearEffects();
               }
            }

            this.sequenceTick++;
            this.entityData.set(DATA_SEQUENCE_TICK, this.sequenceTick);
            int total = this.timings.appearTicks() + this.timings.singTicks() + this.timings.disappearTicks();
            if (this.sequenceTick >= total) {
               this.discard();
            }
         }
      }
   }

   private void tickSoulEvent(ServerLevel level) {
      SingerEntity.SoulEventPhase phase = this.soulEventPhase();
      this.soulEventTick++;
      this.entityData.set(DATA_SOUL_EVENT_TICK, this.soulEventTick);
      switch (phase) {
         case APPEAR:
            if (this.soulEventTick == 1) {
               this.playAppearEffects();
            }

            if (this.soulEventTick >= this.timings.appearTicks()) {
               this.noPhysics = false;
               this.setNoGravity(false);
               this.setNoAi(false);
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
            }
            break;
         case APPROACH:
            this.tickSoulApproach(level);
            break;
         case EXCHANGE:
            this.tickSoulExchange(level);
            break;
         case ROAMING:
            this.tickSoulRoaming(level);
            break;
         case DISAPPEAR:
            this.getNavigation().stop();
            this.noPhysics = true;
            this.setNoGravity(true);
            this.setNoAi(true);
            if (this.soulEventTick == 1) {
               this.playDisappearEffects();
            }

            if (this.soulEventTick >= this.timings.disappearTicks()) {
               this.discard();
            }
            break;
         case BARTER:
            this.tickBarter(level);
      }
   }

   private void tickSoulApproach(ServerLevel level) {
      EchoGolemEntity golem = this.resolveSoulGolem(level);
      if (golem != null && golem.hasSoulBlock() && golem.canInteractWithSinger()) {
         this.faceSoulPartner(golem);
         if (this.soulEventTick % 15 == 1 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(golem, 0.92);
         }

         if (this.distanceToSqr(golem) > 144.0) {
            this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
         } else {
            if (this.distanceToSqr(golem) <= 6.25) {
               this.getNavigation().stop();
               golem.beginBow();
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.EXCHANGE);
            }
         }
      } else {
         this.soulEventGolem = null;
         this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
      }
   }

   private void tickSoulExchange(ServerLevel level) {
      EchoGolemEntity golem = this.resolveSoulGolem(level);
      this.getNavigation().stop();
      if (golem == null) {
         this.soulEventGolem = null;
         this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
      } else {
         this.faceSoulPartner(golem);
         golem.faceEntity(this);
         if (this.soulEventTick < 22 && !golem.hasSoulBlock()) {
            golem.finishBow();
            this.soulEventGolem = null;
            this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
         } else {
            this.getLookControl().setLookAt(golem, 12.0F, 8.0F);
            if (this.soulEventTick == 22) {
               if (!golem.transferSoulToSinger()) {
                  golem.finishBow();
                  this.soulEventGolem = null;
                  this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
                  return;
               }

               this.entityData.set(DATA_HOLDING_SOUL, true);
               this.playSound(ModSounds.SINGER_HAPPY, 1.0F, 0.96F + this.random.nextFloat() * 0.08F);
            }

            if (this.soulEventTick >= 32) {
               this.entityData.set(DATA_HOLDING_SOUL, false);
            }

            if (this.soulEventTick >= 48) {
               golem.finishBow();
               this.soulEventGolem = null;
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.ROAMING);
            }
         }
      }
   }

   private void tickSoulRoaming(ServerLevel level) {
      if (!this.seekBarterPayment(level)) {
         if (!this.gentlyAvoidActiveSoulCanyon(level)) {
            EchoGolemEntity assigned = this.resolveSoulGolem(level);
            if (assigned != null && assigned.hasSoulBlock() && assigned.canInteractWithSinger() && this.distanceToSqr(assigned) <= 100.0) {
               this.setSoulEventPhase(SingerEntity.SoulEventPhase.APPROACH);
            } else {
               if (this.soulEventTick % 40 == 1) {
                  EchoGolemEntity nearby = level.getEntitiesOfClass(
                        EchoGolemEntity.class, this.getBoundingBox().inflate(48.0, 28.0, 48.0), golem -> golem.hasSoulBlock() && golem.canInteractWithSinger()
                     )
                     .stream()
                     .min(Comparator.comparingDouble(this::distanceToSqr))
                     .orElse(null);
                  if (nearby != null) {
                     this.soulEventGolem = nearby.getUUID();
                     if (this.distanceToSqr(nearby) <= 100.0) {
                        this.setSoulEventPhase(SingerEntity.SoulEventPhase.APPROACH);
                        return;
                     }
                  }
               }

               if (this.soulRoamWalking) {
                  if (this.getNavigation().isDone() || --this.roamMoveCooldown <= 0) {
                     this.getNavigation().stop();
                     this.soulRoamWalking = false;
                     this.roamMoveCooldown = 40 + this.random.nextInt(81);
                  }
               } else if (--this.roamMoveCooldown <= 0) {
                  Vec3 destination = this.chooseSoulRoamDestination(level);
                  this.soulRoamWalking = destination != null && this.getNavigation().moveTo(destination.x, destination.y, destination.z, 0.78);
                  this.roamMoveCooldown = this.soulRoamWalking ? 200 : 40 + this.random.nextInt(81);
               }

               if (!this.permanentSoulEvent && !this.hasCustomName() && this.soulEventTick >= 600) {
                  this.setSoulEventPhase(SingerEntity.SoulEventPhase.DISAPPEAR);
               }
            }
         }
      }
   }

   private boolean gentlyAvoidActiveSoulCanyon(ServerLevel level) {
      if (--this.canyonAwarenessCooldown <= 0) {
         this.canyonAwarenessCooldown = 40;
         this.avoidedSoulCanyon = SiftLandmarkTracker.nearestActiveSoulCanyon(level, this.blockPosition(), 36.0).orElse(null);
      }

      if (this.avoidedSoulCanyon == null) {
         return false;
      }

      double horizontal = horizontalDistanceSqr(this.blockPosition(), this.avoidedSoulCanyon);
      boolean belowRim = this.getY() <= this.avoidedSoulCanyon.getY() + 10.0;
      if (belowRim && !(horizontal > 289.0)) {
         if (this.getNavigation().isDone() || this.soulEventTick % 40 == 1) {
            Vec3 away = DefaultRandomPos.getPosAway(this, 15, 8, Vec3.atCenterOf(this.avoidedSoulCanyon));
            if (away != null) {
               this.getNavigation().moveTo(away.x, away.y, away.z, 0.66);
            } else if (this.getNavigation().isDone()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private Vec3 chooseSoulRoamDestination(ServerLevel level) {
      for (int attempt = 0; attempt < 5; attempt++) {
         Vec3 destination = DefaultRandomPos.getPos(this, 12, 6);
         if (destination != null) {
            BlockPos candidate = BlockPos.containing(destination);
            BlockPos canyon = this.avoidedSoulCanyon;
            if (canyon == null || horizontalDistanceSqr(candidate, canyon) >= 324.0) {
               return destination;
            }
         }
      }

      return null;
   }

   private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
      double dx = first.getX() - second.getX();
      double dz = first.getZ() - second.getZ();
      return dx * dx + dz * dz;
   }

   private void faceSoulPartner(Entity golem) {
      double dx = golem.getX() - this.getX();
      double dz = golem.getZ() - this.getZ();
      float yaw = (float)(Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      this.setYRot(yaw);
      this.setYHeadRot(yaw);
      this.setYBodyRot(yaw);
   }

   private EchoGolemEntity resolveSoulGolem(ServerLevel level) {
      if (this.soulEventGolem == null) {
         return null;
      } else {
         return level.getEntityInAnyDimension(this.soulEventGolem) instanceof EchoGolemEntity golem && golem.isAlive() && golem.canInteractWithSinger()
            ? golem
            : null;
      }
   }

   private SingerEntity.SoulEventPhase soulEventPhase() {
      int index = Mth.clamp(this.entityData.get(DATA_SOUL_EVENT_PHASE), 0, SingerEntity.SoulEventPhase.values().length - 1);
      return SingerEntity.SoulEventPhase.values()[index];
   }

   private boolean isSoulEventWalkingForAnimation() {
      return this.getDeltaMovement().horizontalDistanceSqr() > 9.0E-4 || this.getNavigation().isInProgress();
   }

   private void setSoulEventPhase(SingerEntity.SoulEventPhase phase) {
      if (phase == SingerEntity.SoulEventPhase.ROAMING) {
         this.getNavigation().stop();
         this.soulRoamWalking = false;
         this.roamMoveCooldown = 40 + this.random.nextInt(81);
      }

      if (phase != SingerEntity.SoulEventPhase.EXCHANGE && phase != SingerEntity.SoulEventPhase.BARTER) {
         this.entityData.set(DATA_HOLDING_SOUL, false);
      }

      this.entityData.set(DATA_SOUL_EVENT_PHASE, phase.ordinal());
      this.soulEventTick = 0;
      this.entityData.set(DATA_SOUL_EVENT_TICK, 0);
   }

   private void playSingSoundFromSinger() {
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SINGER_SING, SoundSource.NEUTRAL, 1000.0F, 1.0F);
      }
   }

   private static int secondsToTicks(double seconds) {
      return Math.max(0, (int)Math.round(seconds * 20.0));
   }

   private void prepareHornTargets() {
      this.prepareHornTargets(false);
   }

   private void prepareHornTargets(boolean includeNoteModeForMigration) {
      if (!this.hornTargetsPrepared) {
         this.hornTargetsPrepared = true;
         if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 var19 = this.position();
            Vec3 forward = this.getLookAngle();
            forward = new Vec3(forward.x, 0.0, forward.z);
            if (forward.lengthSqr() < 1.0E-6) {
               forward = new Vec3(0.0, 0.0, 1.0);
            } else {
               forward = forward.normalize();
            }

            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            if (right.lengthSqr() < 1.0E-6) {
               right = new Vec3(1.0, 0.0, 0.0);
            } else {
               right = right.normalize();
            }

            Vec3 finalRight = right;
            List<BlockPos> candidates = new ArrayList<>();
            MutableBlockPos cursor = new MutableBlockPos();
            int originX = this.blockPosition().getX();
            int originY = this.blockPosition().getY();
            int originZ = this.blockPosition().getZ();

            for (int dx = -20; dx <= 20; dx++) {
               for (int dy = -20; dy <= 20; dy++) {
                  for (int dz = -20; dz <= 20; dz++) {
                     double distanceSquared = (double)dx * dx + (double)dy * dy + (double)dz * dz;
                     if (!(distanceSquared > 400.0)) {
                        cursor.set(originX + dx, originY + dy, originZ + dz);
                        BlockState state = serverLevel.getBlockState(cursor);
                        if (state.is(ModBlocks.SONOROUS_DEEPSLATE)) {
                           SonorousDeepslateBlock.Mode mode = state.getValue(SonorousDeepslateBlock.MODE);
                           if (mode == SonorousDeepslateBlock.Mode.HORN || includeNoteModeForMigration && mode == SonorousDeepslateBlock.Mode.NOTE) {
                              candidates.add(cursor.immutable());
                           }
                        }
                     }
                  }
               }
            }

            candidates.sort((a, b) -> {
               Vec3 aOffset = Vec3.atCenterOf(a).subtract(var19);
               Vec3 bOffset = Vec3.atCenterOf(b).subtract(var19);
               double aRight = aOffset.dot(finalRight);
               double bRight = bOffset.dot(finalRight);
               return Double.compare(bRight, aRight);
            });
            if (candidates.size() > 8) {
               candidates = new ArrayList<>(candidates.subList(0, 8));
            }

            int splitIndex = (candidates.size() + 1) / 2;
            List<BlockPos> firstHalf = new ArrayList<>(candidates.subList(0, splitIndex));
            List<BlockPos> secondHalf = new ArrayList<>(candidates.subList(splitIndex, candidates.size()));
            firstHalf.sort(Comparator.<BlockPos>comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(var19)).reversed());
            secondHalf.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(var19)));
            List<BlockPos> result = new ArrayList<>(candidates.size());
            result.addAll(firstHalf);
            result.addAll(secondHalf);
            this.hornTargets = List.copyOf(result);
         }
      }
   }

   private void tickSoundWaveLaunches() {
      if (this.singSoundPlayed && !this.hornTargets.isEmpty()) {
         int singStartTick = this.timings.appearTicks();
         int singRelativeTick = this.sequenceTick - singStartTick;

         for (int i = 0; i < SOUND_WAVE_TICKS.length && i < this.hornTargets.size(); i++) {
            if (singRelativeTick == SOUND_WAVE_TICKS[i]) {
               this.launchSoundWave(this.hornTargets.get(i));
            }
         }
      }
   }

   private void launchSoundWave(BlockPos target) {
      if (this.level() instanceof ServerLevel serverLevel) {
         BlockState targetState = serverLevel.getBlockState(target);
         if (targetState.is(ModBlocks.SONOROUS_DEEPSLATE)) {
            if (targetState.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.HORN) {
               Vec3 start = new Vec3(this.getX(), this.getY() + 3.0, this.getZ());
               Vec3 end = Vec3.atCenterOf(target);
               Vec3 delta = end.subtract(start);
               double distance = delta.length();
               if (distance < 0.001) {
                  this.convertHornToNote(serverLevel, target);
               } else {
                  Vec3 direction = delta.scale(1.0 / distance);
                  int travelTicks = Math.max(1, (int)Math.ceil(distance / 0.6));
                  double encodedMagnitude = 1.0 + travelTicks / 100.0;
                  serverLevel.sendParticles(
                     ModParticles.SINGER_SOUND_WAVE,
                     start.x,
                     start.y,
                     start.z,
                     0,
                     direction.x * encodedMagnitude,
                     direction.y * encodedMagnitude,
                     direction.z * encodedMagnitude,
                     1.0
                  );
                  this.pendingHornActivations.add(new SingerEntity.PendingHornActivation(target.immutable(), travelTicks));
               }
            }
         }
      }
   }

   private void tickPendingHornActivations() {
      if (!this.pendingHornActivations.isEmpty()) {
         Iterator<SingerEntity.PendingHornActivation> iterator = this.pendingHornActivations.iterator();

         while (iterator.hasNext()) {
            SingerEntity.PendingHornActivation pending = iterator.next();
            pending.ticksLeft--;
            if (pending.ticksLeft <= 0) {
               if (this.level() instanceof ServerLevel serverLevel) {
                  this.convertHornToNote(serverLevel, pending.pos);
               }

               iterator.remove();
            }
         }
      }
   }

   private void convertHornToNote(ServerLevel level, BlockPos pos) {
      BlockState hornState = level.getBlockState(pos);
      if (hornState.is(ModBlocks.SONOROUS_DEEPSLATE)) {
         if (hornState.getValue(SonorousDeepslateBlock.MODE) == SonorousDeepslateBlock.Mode.HORN) {
            level.setBlock(pos, hornState.setValue(SonorousDeepslateBlock.MODE, SonorousDeepslateBlock.Mode.NOTE), 3);
            level.levelEvent(2001, pos, Block.getId(hornState));
         }
      }
   }

   private void playAppearEffects() {
      float volume = this.portalCenter == null ? 1.0F : 4.0F;
      if (this.portalCenter == null) {
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.THE_SIFT_PORTAL_OPEN, SoundSource.BLOCKS, volume, 1.0F);
      }
   }

   private void playDisappearEffects() {
      float volume = this.portalCenter == null ? 1.0F : 4.0F;
      if (this.portalCenter == null) {
         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.THE_SIFT_PORTAL_CLOSE, SoundSource.BLOCKS, volume, 1.0F);
      }
   }

   public int transitionSoundPhase() {
      if (this.isSoulEvent()) {
         return switch (this.soulEventPhase()) {
            case APPEAR -> 1;
            case DISAPPEAR -> 2;
            default -> 0;
         };
      } else {
         if (!this.entityData.get(DATA_SEQUENCE_STARTED)) {
            return 0;
         }

         int tick = this.entityData.get(DATA_SEQUENCE_TICK);
         if (tick < this.timings.appearTicks()) {
            return 1;
         }

         int disappearStart = this.timings.appearTicks() + this.timings.singTicks();
         return tick >= disappearStart && tick < disappearStart + this.timings.disappearTicks() ? 2 : 0;
      }
   }

   public float transitionSoundVolume() {
      return this.isSoulEvent() ? 1.0F : 4.0F;
   }

   private void tickClientDiggingParticles() {
      if (this.isClientAppearOrDisappearPhase()) {
         RandomSource random = this.getRandom();
         boolean inSift = this.level().dimension().equals(TheSiftDimension.LEVEL_KEY);
         BlockParticleOption blockParticle = null;
         if (inSift) {
            BlockState stateBelow = this.getBlockStateOn();
            if (stateBelow.getRenderShape() == RenderShape.INVISIBLE) {
               return;
            }

            blockParticle = new BlockParticleOption(ParticleTypes.BLOCK, stateBelow);
         }

         for (int i = 0; i < 30; i++) {
            double x = this.getX() + Mth.randomBetween(random, -0.7F, 0.7F);
            double y = this.getY();
            double z = this.getZ() + Mth.randomBetween(random, -0.7F, 0.7F);
            if (inSift) {
               this.level().addParticle(blockParticle, x, y, z, 0.0, 0.0, 0.0);
            } else {
               this.level().addParticle(ModParticles.SIFT_PARALLAX, x, y, z, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private boolean isClientAppearOrDisappearPhase() {
      if (this.entityData.get(DATA_SOUL_EVENT)) {
         SingerEntity.SoulEventPhase phase = this.soulEventPhase();
         int phaseTick = this.entityData.get(DATA_SOUL_EVENT_TICK);
         return (phase == SingerEntity.SoulEventPhase.APPEAR || phase == SingerEntity.SoulEventPhase.DISAPPEAR) && phaseTick < 90;
      }

      if (!this.entityData.get(DATA_SEQUENCE_STARTED)) {
         return false;
      }

      int tick = this.entityData.get(DATA_SEQUENCE_TICK);
      int disappearStart = this.timings.appearTicks() + this.timings.singTicks();
      int total = disappearStart + this.timings.disappearTicks();
      boolean emerging = tick < this.timings.appearTicks() && tick < 90;
      boolean digging = tick >= disappearStart && tick < total && tick - disappearStart < 90;
      return emerging || digging;
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      super.addAdditionalSaveData(output);
      output.putBoolean("sequence_started", this.started);
      output.putInt("sequence_tick", this.sequenceTick);
      output.putBoolean("sing_sound_played", this.singSoundPlayed);
      output.putBoolean("horn_targets_prepared", this.hornTargetsPrepared);
      output.putBoolean("disappear_sound_played", this.disappearSoundPlayed);
      output.putBoolean("soul_event", this.isSoulEvent());
      output.putInt("soul_event_phase", this.soulEventPhase().ordinal());
      output.putInt("soul_event_tick", this.soulEventTick);
      output.putBoolean("permanent_soul_event", this.permanentSoulEvent);
      if (this.barterPlayer != null) {
         output.putString("barter_player", this.barterPlayer.toString());
      }

      if (this.barterTarget != null) {
         output.putDouble("barter_target_x", this.barterTarget.x);
         output.putDouble("barter_target_y", this.barterTarget.y);
         output.putDouble("barter_target_z", this.barterTarget.z);
      }

      if (this.soulEventGolem != null) {
         output.putString("soul_event_golem", this.soulEventGolem.toString());
      }

      output.putInt("horn_targets_version", 1);
      output.putInt("horn_target_count", this.hornTargets.size());

      for (int i = 0; i < this.hornTargets.size(); i++) {
         output.putLong("horn_target_" + i, this.hornTargets.get(i).asLong());
      }

      if (this.portalCenter != null) {
         output.putLong("portal_center", this.portalCenter.asLong());
      }

      output.putInt("pending_horn_count", this.pendingHornActivations.size());

      for (int i = 0; i < this.pendingHornActivations.size(); i++) {
         SingerEntity.PendingHornActivation pending = this.pendingHornActivations.get(i);
         output.putLong("pending_horn_pos_" + i, pending.pos.asLong());
         output.putInt("pending_horn_ticks_" + i, pending.ticksLeft);
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      super.readAdditionalSaveData(input);
      AttributeInstance resistance = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      if (resistance != null && resistance.getBaseValue() == 0.0) {
         resistance.setBaseValue(0.4);
      }

      this.started = input.getBooleanOr("sequence_started", false);
      this.sequenceTick = input.getIntOr("sequence_tick", 0);
      this.entityData.set(DATA_SEQUENCE_STARTED, this.started);
      this.entityData.set(DATA_SEQUENCE_TICK, this.sequenceTick);
      this.singSoundPlayed = input.getBooleanOr("sing_sound_played", false);
      this.hornTargetsPrepared = input.getBooleanOr("horn_targets_prepared", false);
      this.disappearSoundPlayed = input.getBooleanOr("disappear_sound_played", false);
      boolean soulEvent = input.getBooleanOr("soul_event", false);
      this.entityData.set(DATA_SOUL_EVENT, soulEvent);
      int eventPhase = Mth.clamp(input.getIntOr("soul_event_phase", 0), 0, SingerEntity.SoulEventPhase.values().length - 1);
      this.entityData.set(DATA_SOUL_EVENT_PHASE, eventPhase);
      this.soulEventTick = Math.max(0, input.getIntOr("soul_event_tick", 0));
      this.permanentSoulEvent = input.getBooleanOr("permanent_soul_event", false);
      this.entityData.set(DATA_SOUL_EVENT_TICK, this.soulEventTick);
      String barterId = input.getStringOr("barter_player", "");

      try {
         this.barterPlayer = barterId.isEmpty() ? null : UUID.fromString(barterId);
      } catch (IllegalArgumentException ignored) {
         this.barterPlayer = null;
      }

      this.barterTarget = new Vec3(
         input.getDoubleOr("barter_target_x", this.getX()),
         input.getDoubleOr("barter_target_y", this.getY()),
         input.getDoubleOr("barter_target_z", this.getZ())
      );
      this.entityData
         .set(
            DATA_HOLDING_SOUL,
            soulEvent
               && (
                  this.soulEventPhase() == SingerEntity.SoulEventPhase.BARTER && this.soulEventTick < 40
                     || this.soulEventPhase() == SingerEntity.SoulEventPhase.EXCHANGE && this.soulEventTick >= 22 && this.soulEventTick < 32
               )
         );
      String golemId = input.getStringOr("soul_event_golem", "");

      try {
         this.soulEventGolem = golemId.isEmpty() ? null : UUID.fromString(golemId);
      } catch (IllegalArgumentException ignored) {
         this.soulEventGolem = null;
      }

      if (soulEvent) {
         SingerEntity.SoulEventPhase loadedPhase = SingerEntity.SoulEventPhase.values()[eventPhase];
         boolean immaterial = loadedPhase == SingerEntity.SoulEventPhase.APPEAR || loadedPhase == SingerEntity.SoulEventPhase.DISAPPEAR;
         this.noPhysics = immaterial;
         this.setNoGravity(immaterial);
         this.setNoAi(immaterial);
         this.setPermanentlyInvulnerable(false);
      }

      int hornTargetsVersion = input.getIntOr("horn_targets_version", 0);
      int hornTargetCount = Math.max(0, Math.min(8, input.getIntOr("horn_target_count", 0)));
      List<BlockPos> loadedHornTargets = new ArrayList<>(hornTargetCount);

      for (int i = 0; i < hornTargetCount; i++) {
         long packed = input.getLong("horn_target_" + i).orElse(0L);
         loadedHornTargets.add(BlockPos.of(packed));
      }

      this.hornTargets = List.copyOf(loadedHornTargets);
      this.restoreLegacyHornTargets = hornTargetsVersion == 0
         && this.started
         && this.singSoundPlayed
         && this.sequenceTick < this.timings.appearTicks() + this.timings.singTicks();
      long packedPortalCenter = input.getLong("portal_center").orElse(Long.MIN_VALUE);
      this.portalCenter = packedPortalCenter == Long.MIN_VALUE ? null : BlockPos.of(packedPortalCenter);
      this.pendingHornActivations.clear();
      int pendingCount = Math.max(0, input.getIntOr("pending_horn_count", 0));

      for (int i = 0; i < pendingCount; i++) {
         long packedPos = input.getLong("pending_horn_pos_" + i).orElse(0L);
         int ticksLeft = Math.max(0, input.getIntOr("pending_horn_ticks_" + i, 0));
         this.pendingHornActivations.add(new SingerEntity.PendingHornActivation(BlockPos.of(packedPos), ticksLeft));
      }

      this.timings = SingerAnimationTimings.load();
   }

   @Override
   protected void registerGoals() {
   }

   @Override
   public boolean isPickable() {
      return this.isSoulEvent();
   }

   @Override
   public boolean isPushable() {
      return this.isSoulEvent();
   }

   @Override
   public boolean canBeCollidedWith(Entity entity) {
      return this.isSoulEvent() && super.canBeCollidedWith(entity);
   }

   @Override
   public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
      return this.isSoulEvent() ? super.isInvulnerableTo(level, source) : !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      if (this.isSoulEvent()) {
         return super.hurtServer(level, source, amount);
      } else {
         return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? super.hurtServer(level, source, amount) : false;
      }
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel level) {
      return this.isSoulEvent() ? 8 + this.random.nextInt(5) : 0;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.portalCenter == null && this.transitionSoundPhase() == 0 ? ModSounds.SINGER_IDLE : null;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return this.portalCenter != null ? null : ModSounds.SINGER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return this.portalCenter != null ? null : ModSounds.SINGER_DEATH;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (DATA_SEQUENCE_TICK.equals(key)) {
         this.sequenceSyncClientTick = this.tickCount;
      }

      if (DATA_SOUL_EVENT_TICK.equals(key) || DATA_SOUL_EVENT_PHASE.equals(key)) {
         this.soulSyncClientTick = this.tickCount;
      }
   }

   private int sequencePhaseStart() {
      int tick = this.entityData.get(DATA_SEQUENCE_TICK);
      int disappearStart = this.timings.appearTicks() + this.timings.singTicks();
      return tick >= disappearStart ? disappearStart : (tick >= this.timings.appearTicks() ? this.timings.appearTicks() : 0);
   }

   private double sequenceAnimationTick() {
      if (this.isSoulEvent()) {
         return switch (this.soulEventPhase()) {
            case APPEAR, EXCHANGE, DISAPPEAR -> this.entityData.get(DATA_SOUL_EVENT_TICK)
               + (this.level().isClientSide() ? Math.max(0, this.tickCount - this.soulSyncClientTick) : 0);
            default -> -1.0;
         };
      } else {
         return !this.entityData.get(DATA_SEQUENCE_STARTED)
            ? -1.0
            : this.entityData.get(DATA_SEQUENCE_TICK)
               - this.sequencePhaseStart()
               + (this.level().isClientSide() ? Math.max(0, this.tickCount - this.sequenceSyncClientTick) : 0);
      }
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new SingerAnimationController("singer_sequence", this::sequenceAnimationTick, state -> {
         SingerEntity singer = (SingerEntity)state.animatable();
         if (singer.entityData.get(DATA_SOUL_EVENT)) {
            SingerEntity.SoulEventPhase phase = singer.soulEventPhase();

            RawAnimation eventAnimation = switch (phase) {
               case APPEAR -> EVENT_APPEAR;
               case APPROACH -> singer.isSoulEventWalkingForAnimation() ? EVENT_WALK : EVENT_IDLE;
               case EXCHANGE -> EVENT_RECEIVE;
               case ROAMING -> singer.isSoulEventWalkingForAnimation() ? EVENT_WALK : EVENT_IDLE;
               case DISAPPEAR -> EVENT_DISAPPEAR;
               case BARTER -> singer.isHoldingSoulBlock() ? BARTER_HOLD : EVENT_IDLE;
            };
            state.controller().setAnimation(eventAnimation);
            return PlayState.CONTINUE;
         } else {
            if (!singer.entityData.get(DATA_SEQUENCE_STARTED)) {
               return PlayState.STOP;
            }

            AnimationController<GeoAnimatable> controller = state.controller();
            int phaseStart = singer.sequencePhaseStart();
            controller.setAnimation(phaseStart == 0 ? EVENT_APPEAR : (phaseStart == singer.timings.appearTicks() ? EVENT_SING : EVENT_DISAPPEAR));
            return PlayState.CONTINUE;
         }
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animatableInstanceCache;
   }

   private static final class PendingHornActivation {
      private final BlockPos pos;
      private int ticksLeft;

      private PendingHornActivation(BlockPos pos, int ticksLeft) {
         this.pos = pos;
         this.ticksLeft = ticksLeft;
      }
   }

   private enum SoulEventPhase {
      APPEAR,
      APPROACH,
      EXCHANGE,
      ROAMING,
      DISAPPEAR,
      BARTER;
   }
}
