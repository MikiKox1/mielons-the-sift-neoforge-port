package mielon.thesift.world;

import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import mielon.thesift.entity.MiniRiftEntity;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.SiftiteReturnEntity;
import mielon.thesift.item.ModItems;
import mielon.thesift.platform.NeoEvents.ServerPlayerEvents;
import mielon.thesift.platform.NeoEvents.ServerPlayerEvents.AfterRespawn;
import mielon.thesift.platform.NeoEvents.ServerPlayerEvents.Join;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class SiftiteRecoveryManager {
   private static final Map<UUID, List<ItemStack>> PENDING = new HashMap<>();
   private static final Map<UUID, Long> DUE_AT = new HashMap<>();
   private static boolean registered;

   private SiftiteRecoveryManager() {
   }

   public static void register() {
      if (!registered) {
         registered = true;
         ServerPlayerEvents.AFTER_RESPAWN.register((AfterRespawn)(oldPlayer, newPlayer, alive) -> {
            if (ensureLoaded(newPlayer).size() > 0) {
               DUE_AT.put(newPlayer.getUUID(), newPlayer.level().getGameTime() + 40L);
            }
         });
         ServerPlayerEvents.JOIN.register((Join)player -> {
            if (ensureLoaded(player).size() > 0) {
               DUE_AT.putIfAbsent(player.getUUID(), player.level().getGameTime() + 40L);
            }
         });
      }
   }

   public static boolean canRecover(ItemStack stack) {
      return !stack.isEmpty() && stack.is(ModItems.SIFTITE_ITEMS) && !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
   }

   public static void captureOnDeath(ServerPlayer player) {
      if (!player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
         List<ItemStack> protectedStacks = new ArrayList<>();
         List<Integer> protectedSlots = new ArrayList<>();

         for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (canRecover(stack)) {
               protectedStacks.add(stack.copy());
               protectedSlots.add(slot);
            }
         }

         if (!protectedStacks.isEmpty()) {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
               List<ItemStack> allPending = new ArrayList<>(loadPending(server, player.getUUID()));
               allPending.addAll(protectedStacks);
               savePending(server, player.getUUID(), allPending);
               PENDING.put(player.getUUID(), allPending);

               for (int slot : protectedSlots) {
                  player.getInventory().setItem(slot, ItemStack.EMPTY);
               }
            }
         }
      }
   }

   public static void tick(MinecraftServer server) {
      if (!DUE_AT.isEmpty()) {
         List<UUID> ready = new ArrayList<>();

         for (Entry<UUID, Long> entry : DUE_AT.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.isAlive() && player.level().getGameTime() >= entry.getValue()) {
               ready.add(entry.getKey());
            }
         }

         for (UUID playerId : ready) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            List<ItemStack> stacks = PENDING.get(playerId);
            if (player != null && stacks != null && !stacks.isEmpty() && spawnMiniRift(player, stacks)) {
               PENDING.remove(playerId);
               DUE_AT.remove(playerId);
               savePending(server, playerId, List.of());
            }
         }
      }
   }

   private static boolean spawnMiniRift(ServerPlayer player, List<ItemStack> stacks) {
      ServerLevel level = player.level();
      Vec3 origin = player.position();
      Vec3 position = origin.add(1.55, 0.75, 0.0);

      for (int i = 0; i < 8; i++) {
         double angle = i * Math.PI / 4.0;
         Vec3 candidate = origin.add(Math.cos(angle) * 1.55, 0.75, Math.sin(angle) * 1.55);
         BlockPos blockPos = BlockPos.containing(candidate);
         if (level.getBlockState(blockPos).isAir() && level.getBlockState(blockPos.above()).isAir()) {
            position = candidate;
            break;
         }
      }

      MiniRiftEntity rift = new MiniRiftEntity(ModEntities.MINI_RIFT, level);
      rift.configure(player.getUUID(), stacks, level.getGameTime());
      rift.setPos(position);
      return level.addFreshEntity(rift);
   }

   private static List<ItemStack> ensureLoaded(ServerPlayer player) {
      MinecraftServer server = player.level().getServer();
      return server == null ? List.of() : PENDING.computeIfAbsent(player.getUUID(), playerId -> loadPending(server, playerId));
   }

   private static List<ItemStack> loadPending(MinecraftServer server, UUID playerId) {
      CompoundTag stored = server.getCommandStorage().get(storageId(playerId));
      if (!stored.getBooleanOr("Valid", false)) {
         return new ArrayList<>();
      }

      DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
      List<ItemStack> result = new ArrayList<>();

      for (Tag raw : stored.getListOrEmpty("Items")) {
         if (raw instanceof CompoundTag entry) {
            entry.read("Stack", ItemStack.CODEC, ops).ifPresent(stack -> {
               if (canRecover(stack)) {
                  result.add(stack.copy());
               }
            });
         }
      }

      return result;
   }

   private static void savePending(MinecraftServer server, UUID playerId, List<ItemStack> stacks) {
      CompoundTag stored = new CompoundTag();
      if (!stacks.isEmpty()) {
         stored.putBoolean("Valid", true);
         DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
         ListTag items = new ListTag();

         for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
               CompoundTag entry = new CompoundTag();
               entry.store("Stack", ItemStack.CODEC, ops, stack);
               items.add(entry);
            }
         }

         stored.put("Items", items);
      }

      server.getCommandStorage().set(storageId(playerId), stored);
   }

   private static Identifier storageId(UUID playerId) {
      return Identifier.fromNamespaceAndPath("the_sift", "siftite_recovery/" + playerId);
   }

   public static void clearTransientState() {
      SiftiteReturnEntity.clearDeliveryIndex();
      PENDING.clear();
      DUE_AT.clear();
   }
}
