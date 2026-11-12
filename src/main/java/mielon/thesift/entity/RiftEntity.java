package mielon.thesift.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.RiftAnimationClock;
import mielon.thesift.world.RiftBackgrounds;
import mielon.thesift.world.RiftDirectory;
import mielon.thesift.world.RiftLightCleanup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueOutput.TypedOutputList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiftEntity extends Entity {
   public static final int LIFETIME_TICKS = 6000;
   public static final int ANIMATION_TICKS = 16;
   private static final EntityDataAccessor<Boolean> DATA_TARGET_SIFT = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_LONG_ALONG_X = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_CLOSING = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_APPEARING = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Long> DATA_BORN = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Long> DATA_END = SynchedEntityData.defineId(RiftEntity.class, EntityDataSerializers.LONG);
   private long expiresAt;
   private UUID pairId = UUID.randomUUID();
   private BlockPos linkedPos;
   private long linkedUntil;
   private boolean appearSoundPlayed;
   private int closingTicks;
   private final List<BlockPos> placedLights = new ArrayList<>(3);

   public RiftEntity(EntityType<? extends RiftEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setInvulnerable(true);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_TARGET_SIFT, false);
      builder.define(DATA_LONG_ALONG_X, true);
      builder.define(DATA_CLOSING, false);
      builder.define(DATA_APPEARING, false);
      builder.define(DATA_BORN, 0L);
      builder.define(DATA_END, 0L);
   }

   public void configure(boolean targetSift, boolean longAlongX, long expiresAt, UUID pairId) {
      this.entityData.set(DATA_TARGET_SIFT, targetSift);
      this.entityData.set(DATA_LONG_ALONG_X, longAlongX);
      this.entityData.set(DATA_APPEARING, true);
      this.expiresAt = expiresAt;
      this.pairId = pairId;
      this.entityData.set(DATA_BORN, this.level().getGameTime());
      this.entityData.set(DATA_END, expiresAt);
   }

   public int getBackgroundVariant() {
      return RiftBackgrounds.variant(this.getUUID());
   }

   public boolean targetsSift() {
      return this.entityData.get(DATA_TARGET_SIFT);
   }

   public boolean isLongAlongX() {
      return this.entityData.get(DATA_LONG_ALONG_X);
   }

   public boolean isClosing() {
      return this.entityData.get(DATA_CLOSING);
   }

   public long getExpiresAt() {
      return this.expiresAt;
   }

   public void extendExpiresAt(long expiry) {
      if (expiry > this.expiresAt) {
         this.expiresAt = expiry;
         this.entityData.set(DATA_END, expiry);
         if (this.level() instanceof ServerLevel serverLevel) {
            RiftLightCleanup.track(serverLevel, this.placedLights, expiry);
         }

         if (expiry - this.level().getGameTime() > 16L) {
            this.entityData.set(DATA_CLOSING, false);
            this.closingTicks = 0;
         }
      }
   }

   public UUID getPairId() {
      return this.pairId;
   }

   public BlockPos getLinkedPos() {
      return this.linkedUntil > 0L && this.level().getGameTime() >= this.linkedUntil ? null : this.linkedPos;
   }

   public void setLinkedPos(BlockPos linkedPos) {
      this.linkedPos = linkedPos == null ? null : linkedPos.immutable();
      this.linkedUntil = this.expiresAt;
   }

   public void setLinkedPos(BlockPos pos, long until) {
      this.setLinkedPos(pos);
      this.linkedUntil = until;
   }

   public BlockPos getAnchorPos() {
      return BlockPos.containing(this.getX(), this.getY(), this.getZ());
   }

   public AABB getPortalBounds() {
      return portalBoundsAt(this.position(), this.isLongAlongX());
   }

   private static AABB portalBoundsAt(Vec3 position, boolean longAlongX) {
      double halfX = longAlongX ? 4.5 : 0.5;
      double halfZ = longAlongX ? 0.5 : 4.5;
      return new AABB(position.x - halfX, position.y - 0.375, position.z - halfZ, position.x + halfX, position.y + 3.875, position.z + halfZ);
   }

   @Override
   protected AABB makeBoundingBox(Vec3 position) {
      return portalBoundsAt(position, this.isLongAlongX());
   }

   public float getOpenScale(float partialTick) {
      if (this.isClosing()) {
         return RiftAnimationClock.progress(true, this.closingTicks + partialTick);
      } else {
         return !this.entityData.get(DATA_APPEARING) ? 1.0F : RiftAnimationClock.progress(false, this.tickCount + partialTick);
      }
   }

   @Override
   public void tick() {
      this.setBoundingBox(this.getPortalBounds());
      super.tick();
      this.noPhysics = true;
      this.setDeltaMovement(Vec3.ZERO);
      this.setBoundingBox(this.getPortalBounds());
      if (this.level().isClientSide()) {
         if (this.isClosing()) {
            this.closingTicks++;
         } else {
            this.closingTicks = 0;
         }
      } else {
         ServerLevel level = (ServerLevel)this.level();
         RiftDirectory.track(this);
         if (this.expiresAt <= 0L) {
            this.expiresAt = level.getGameTime() + 6000L;
            this.entityData.set(DATA_BORN, level.getGameTime());
            this.entityData.set(DATA_END, this.expiresAt);
         }

         if (!this.appearSoundPlayed) {
            this.appearSoundPlayed = true;
            this.playSound(ModSounds.RIFT_APPEAR, 2.0F, 1.0F);
         }

         if (this.entityData.get(DATA_APPEARING) && this.tickCount >= 16) {
            this.entityData.set(DATA_APPEARING, false);
         }

         if (this.placedLights.isEmpty() || this.tickCount == 1 || this.tickCount % 20 == 0) {
            this.placeVanillaLights(level);
         }

         RiftLightCleanup.track(level, this.placedLights, this.expiresAt);
         long remaining = this.expiresAt - level.getGameTime();
         if (remaining <= 16L && !this.isClosing()) {
            this.entityData.set(DATA_CLOSING, true);
            this.closingTicks = 0;
         }

         if (this.isClosing()) {
            this.closingTicks++;
         }

         if (remaining <= 0L || this.closingTicks > 16) {
            this.discard();
         }
      }
   }

   private void placeVanillaLights(ServerLevel level) {
      BlockPos anchor = this.getAnchorPos();

      for (int offset : new int[]{-3, 0, 3}) {
         BlockPos lightPos = this.isLongAlongX() ? anchor.offset(offset, 2, 0) : anchor.offset(0, 2, offset);
         if (level.getChunkSource().getChunkNow(lightPos.getX() >> 4, lightPos.getZ() >> 4) != null) {
            if (level.getBlockState(lightPos).isAir()) {
               level.setBlock(lightPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
               if (!this.placedLights.contains(lightPos)) {
                  this.placedLights.add(lightPos.immutable());
               }
            } else if (level.getBlockState(lightPos).is(Blocks.LIGHT) && RiftLightCleanup.isManaged(level, lightPos) && !this.placedLights.contains(lightPos)) {
               this.placedLights.add(lightPos.immutable());
            }
         }
      }
   }

   private void removeVanillaLights() {
      if (this.level() instanceof ServerLevel level) {
         RiftLightCleanup.enqueue(level, this.placedLights);
         this.placedLights.clear();
      }
   }

   @Override
   public void onRemoval(RemovalReason reason) {
      this.removeVanillaLights();
      if (!this.level().isClientSide()) {
         RiftDirectory.removed(this, reason.shouldDestroy());
      }

      super.onRemoval(reason);
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      this.entityData.set(DATA_TARGET_SIFT, input.getBooleanOr("TargetSift", false));
      this.entityData.set(DATA_LONG_ALONG_X, input.getBooleanOr("LongAlongX", true));
      this.entityData.set(DATA_CLOSING, input.getBooleanOr("Closing", false));
      this.expiresAt = input.getLongOr("ExpiresAt", 0L);
      this.entityData.set(DATA_END, this.expiresAt);
      this.entityData.set(DATA_BORN, input.getLongOr("BornAt", this.level().getGameTime() - 16L));
      this.pairId = parseUuid(input.getStringOr("PairId", ""), UUID.randomUUID());
      input.read("LinkedPos", BlockPos.CODEC).ifPresent(this::setLinkedPos);
      this.linkedUntil = input.getLongOr("LinkedUntil", this.expiresAt);
      this.appearSoundPlayed = input.getBooleanOr("AppearSoundPlayed", true);
      this.closingTicks = input.getIntOr("ClosingTicks", 0);
      this.placedLights.clear();
      input.listOrEmpty("PlacedLights", BlockPos.CODEC).forEach(p -> this.placedLights.add(p.immutable()));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      output.putBoolean("TargetSift", this.targetsSift());
      output.putBoolean("LongAlongX", this.isLongAlongX());
      output.putBoolean("Closing", this.isClosing());
      output.putLong("ExpiresAt", this.expiresAt);
      output.putLong("BornAt", this.entityData.get(DATA_BORN));
      output.putString("PairId", this.pairId.toString());
      if (this.linkedPos != null) {
         output.store("LinkedPos", BlockPos.CODEC, this.linkedPos);
         output.putLong("LinkedUntil", this.linkedUntil);
      }

      output.putBoolean("AppearSoundPlayed", this.appearSoundPlayed);
      output.putInt("ClosingTicks", this.closingTicks);
      TypedOutputList<BlockPos> lights = output.list("PlacedLights", BlockPos.CODEC);
      this.placedLights.forEach(lights::add);
   }

   private static UUID parseUuid(String value, UUID fallback) {
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException ignored) {
         return fallback;
      }
   }

   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      return false;
   }

   @Override
   public boolean hurtClient(DamageSource source) {
      return false;
   }

   @Override
   public boolean isAttackable() {
      return !this.isRemoved() && !this.isClosing();
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean canBeHitByProjectile() {
      return false;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public boolean canCollideWith(Entity other) {
      return false;
   }

   @Override
   public boolean canBeCollidedWith(Entity other) {
      return false;
   }

   @Override
   public boolean skipAttackInteraction(Entity attacker) {
      if (this.level() instanceof ServerLevel level && attacker instanceof Player player && player.isCreative() && this.isAttackable()) {
         this.expiresAt = level.getGameTime() + 16L;
         this.entityData.set(DATA_END, this.expiresAt);
         this.entityData.set(DATA_CLOSING, true);
         this.closingTicks = 0;
         RiftDirectory.stopOfferingExit(this);
      }
      return true;
   }

   @Override
   protected void doWaterSplashEffect() {
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   public AABB getFluidInteractionBox() {
      return new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ());
   }
}
