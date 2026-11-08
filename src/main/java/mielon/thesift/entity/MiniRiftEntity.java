package mielon.thesift.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.UUID;
import mielon.thesift.world.RiftBackgrounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueOutput.TypedOutputList;
import net.minecraft.world.phys.Vec3;

public final class MiniRiftEntity extends Entity {
   public static final int ANIMATION_TICKS = 12;
   private static final int EMIT_INTERVAL = 5;
   private static final EntityDataAccessor<Boolean> DATA_CLOSING = SynchedEntityData.defineId(MiniRiftEntity.class, EntityDataSerializers.BOOLEAN);
   private final Deque<ItemStack> pending = new ArrayDeque<>();
   private UUID ownerId;
   private long batchOrder;
   private int nextDeliveryIndex;
   private int closingTicks;

   public MiniRiftEntity(EntityType<? extends MiniRiftEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setPermanentlyInvulnerable(true);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_CLOSING, false);
   }

   public void configure(UUID ownerId, Collection<ItemStack> stacks, long batchOrder) {
      this.ownerId = ownerId;
      this.batchOrder = Math.max(0L, batchOrder);
      this.nextDeliveryIndex = 0;

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            this.pending.addLast(stack.copy());
         }
      }
   }

   public int getBackgroundVariant() {
      return RiftBackgrounds.variant(this.getUUID());
   }

   public boolean isClosing() {
      return this.entityData.get(DATA_CLOSING);
   }

   public float getOpenScale(float partialTick) {
      return this.isClosing() ? Math.max(0.0F, 1.0F - (this.closingTicks + partialTick) / 12.0F) : Math.min(1.0F, (this.tickCount + partialTick) / 12.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.noPhysics = true;
      this.setDeltaMovement(Vec3.ZERO);
      if (this.level().isClientSide()) {
         if (this.isClosing()) {
            this.closingTicks++;
         }
      } else if (this.isClosing()) {
         if (++this.closingTicks > 12) {
            this.discard();
         }
      } else {
         if (this.level() instanceof ServerLevel level && this.ownerId != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.ownerId);
            if (owner != null && owner.isAlive() && owner.level() != level) {
               MiniRiftEntity moved = new MiniRiftEntity(ModEntities.MINI_RIFT, owner.level());
               moved.configure(this.ownerId, new ArrayList<>(this.pending), this.batchOrder);
               moved.batchOrder = this.batchOrder;
               moved.nextDeliveryIndex = this.nextDeliveryIndex;
               moved.setPos(owner.position().add(1.55, 0.75, 0.0));
               if (owner.level().addFreshEntity(moved)) {
                  this.pending.clear();
                  this.discard();
               }

               return;
            }
         }

         if (this.tickCount >= 12 && this.tickCount % 5 == 0 && !this.pending.isEmpty()) {
            ItemStack stack = this.pending.getFirst();
            SiftiteReturnEntity returning = new SiftiteReturnEntity(ModEntities.SIFTITE_RETURN, this.level());
            long deliveryOrder = (this.batchOrder << 16) + Integer.toUnsignedLong(this.nextDeliveryIndex);
            returning.configure(this.ownerId, stack, deliveryOrder);
            returning.setPos(this.getX(), this.getY(), this.getZ());
            if (this.level().addFreshEntity(returning)) {
               this.pending.removeFirst();
               this.nextDeliveryIndex++;
            }
         }

         if (this.pending.isEmpty() && this.tickCount >= 12) {
            this.entityData.set(DATA_CLOSING, true);
            this.closingTicks = 0;
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      this.ownerId = parseUuid(input.getStringOr("Owner", ""));
      this.entityData.set(DATA_CLOSING, input.getBooleanOr("Closing", false));
      this.closingTicks = input.getIntOr("ClosingTicks", 0);
      this.batchOrder = input.getLongOr("BatchOrder", Math.max(0L, this.level().getGameTime()));
      this.nextDeliveryIndex = Math.max(0, input.getIntOr("NextDeliveryIndex", 0));
      input.listOrEmpty("Pending", ItemStack.CODEC).forEach(stack -> {
         if (!stack.isEmpty()) {
            this.pending.addLast(stack);
         }
      });
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      if (this.ownerId != null) {
         output.putString("Owner", this.ownerId.toString());
      }

      output.putBoolean("Closing", this.isClosing());
      output.putInt("ClosingTicks", this.closingTicks);
      output.putLong("BatchOrder", this.batchOrder);
      output.putInt("NextDeliveryIndex", this.nextDeliveryIndex);
      TypedOutputList<ItemStack> list = output.list("Pending", ItemStack.CODEC);
      this.pending.forEach(list::add);
   }

   private static UUID parseUuid(String value) {
      try {
         return UUID.fromString(value);
      } catch (IllegalArgumentException ignored) {
         return null;
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
      return false;
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
      return true;
   }
}
