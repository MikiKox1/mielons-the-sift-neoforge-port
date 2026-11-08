package mielon.thesift.entity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import mielon.thesift.advancement.ModAdvancements;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class SiftiteReturnEntity extends Entity implements ItemSupplier {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(SiftiteReturnEntity.class, EntityDataSerializers.ITEM_STACK);
   private UUID ownerId;
   private long deliveryOrder;
   private static final Map<ServerLevel, SiftiteReturnEntity.DeliveryIndex> DELIVERIES = new WeakHashMap<>();
   private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

   public static void clearDeliveryIndex() {
      DELIVERIES.clear();
   }

   @Override
   public InterpolationHandler getInterpolation() {
      return this.interpolation;
   }

   public SiftiteReturnEntity(EntityType<? extends SiftiteReturnEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
      this.setInvulnerable(true);
   }

   @Override
   protected void defineSynchedData(Builder builder) {
      builder.define(DATA_ITEM, ItemStack.EMPTY);
   }

   public void configure(UUID ownerId, ItemStack stack, long deliveryOrder) {
      this.ownerId = ownerId;
      this.deliveryOrder = deliveryOrder;
      this.entityData.set(DATA_ITEM, stack.copy());
   }

   @Override
   public ItemStack getItem() {
      return this.entityData.get(DATA_ITEM);
   }

   @Override
   public void tick() {
      super.tick();
      this.noPhysics = true;
      if (this.level() instanceof ServerLevel level) {
         ServerPlayer owner = this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
         if (owner != null && owner.isAlive()) {
            if (owner.level() != level) {
               ServerLevel destination = owner.level();
               SiftiteReturnEntity moved = new SiftiteReturnEntity(ModEntities.SIFTITE_RETURN, destination);
               moved.configure(this.ownerId, this.getItem(), this.deliveryOrder);
               moved.setPos(owner.position().add(0.0, 0.75, 0.0));
               if (destination.addFreshEntity(moved)) {
                  this.entityData.set(DATA_ITEM, ItemStack.EMPTY);
                  this.discard();
               }
            } else {
               Vec3 center = owner.position().add(0.0, owner.getBbHeight() * 0.62, 0.0);
               boolean waitingForEarlierItem = this.hasEarlierDelivery(level);
               ItemStack remaining = this.getItem().copy();
               if (!waitingForEarlierItem && this.distanceToSqr(center) <= 0.42250000000000004) {
                  int countBeforeDelivery = remaining.getCount();
                  owner.getInventory().add(remaining);
                  if (remaining.getCount() < countBeforeDelivery) {
                     ModAdvancements.award(owner, "the_sift/keep_inventory");
                  }

                  if (remaining.isEmpty()) {
                     this.discard();
                     return;
                  }

                  if (remaining.getCount() != this.getItem().getCount()) {
                     this.entityData.set(DATA_ITEM, remaining);
                  }
               }

               boolean inventoryFull = waitingForEarlierItem || !canFitAny(owner, this.getItem());
               Vec3 destination;
               if (inventoryFull) {
                  double angle = this.tickCount * 0.17 + (this.getId() & 15) * 0.39;
                  destination = center.add(Math.cos(angle) * 1.65, 0.35 + Math.sin(angle * 1.7) * 0.35, Math.sin(angle) * 1.65);
               } else {
                  destination = center;
               }

               Vec3 delta = destination.subtract(this.position());
               Vec3 desired = delta.scale(inventoryFull ? 0.3 : 0.38);
               Vec3 velocity = this.getDeltaMovement().scale(0.65).add(desired.scale(0.35));
               double maxSpeed = inventoryFull ? 0.45 : 1.25;
               if (velocity.lengthSqr() > maxSpeed * maxSpeed) {
                  velocity = velocity.normalize().scale(maxSpeed);
               }

               this.setDeltaMovement(velocity);
               this.move(MoverType.SELF, velocity);
               if ((this.tickCount & 1) == 0) {
                  level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 1, 0.03, 0.03, 0.03, 0.005);
               }
            }
         } else {
            this.setDeltaMovement(Vec3.ZERO);
         }
      } else {
         this.interpolation.interpolate();
      }
   }

   private boolean hasEarlierDelivery(ServerLevel level) {
      SiftiteReturnEntity.DeliveryIndex index = DELIVERIES.get(level);
      if (index == null || index.tick != level.getGameTime()) {
         index = new SiftiteReturnEntity.DeliveryIndex();
         index.tick = level.getGameTime();

         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SiftiteReturnEntity item && item.ownerId != null && !item.isRemoved()) {
               index.owners
                  .computeIfAbsent(
                     item.ownerId, k -> new TreeSet<>(Comparator.<SiftiteReturnEntity>comparingLong(e -> e.deliveryOrder).thenComparingInt(Entity::getId))
                  )
                  .add(item);
            }
         }

         DELIVERIES.put(level, index);
      }

      TreeSet<SiftiteReturnEntity> items = index.owners
         .computeIfAbsent(this.ownerId, k -> new TreeSet<>(Comparator.<SiftiteReturnEntity>comparingLong(e -> e.deliveryOrder).thenComparingInt(Entity::getId)));
      items.add(this);

      while (!items.isEmpty() && (items.first().isRemoved() || items.first().getItem().isEmpty())) {
         items.pollFirst();
      }

      return !items.isEmpty() && items.first().deliveryOrder < this.deliveryOrder;
   }

   private static boolean canFitAny(ServerPlayer player, ItemStack incoming) {
      for (int slot = 0; slot < 36; slot++) {
         ItemStack existing = player.getInventory().getItem(slot);
         if (existing.isEmpty()) {
            return true;
         }

         if (ItemStack.isSameItemSameComponents(existing, incoming) && existing.getCount() < existing.getMaxStackSize()) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected void readAdditionalSaveData(ValueInput input) {
      this.ownerId = parseUuid(input.getStringOr("Owner", ""));
      this.deliveryOrder = input.getLongOr("DeliveryOrder", 0L);
      input.read("Item", ItemStack.CODEC).ifPresent(stack -> this.entityData.set(DATA_ITEM, stack));
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput output) {
      if (this.ownerId != null) {
         output.putString("Owner", this.ownerId.toString());
      }

      output.putLong("DeliveryOrder", this.deliveryOrder);
      if (!this.getItem().isEmpty()) {
         output.store("Item", ItemStack.CODEC, this.getItem());
      }
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

   private static final class DeliveryIndex {
      long tick;
      final Map<UUID, TreeSet<SiftiteReturnEntity>> owners = new HashMap<>();
   }
}
