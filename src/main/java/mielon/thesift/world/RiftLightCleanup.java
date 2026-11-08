package mielon.thesift.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mielon.thesift.entity.RiftEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

public final class RiftLightCleanup {
   private static final Identifier KEY = Identifier.fromNamespaceAndPath("the_sift", "rift_lights");
   private static final Map<RiftLightCleanup.Entry, Long> lights = new HashMap<>();
   private static boolean initialized;

   private static void initialize(MinecraftServer server) {
      if (!initialized) {
         initialized = true;

         for (Tag raw : server.getCommandStorage().get(KEY).getListOrEmpty("lights")) {
            if (raw instanceof CompoundTag data) {
               try {
                  ResourceKey<Level> dimension = ResourceKey.create(
                     Registries.DIMENSION, Identifier.parse(data.getStringOr("dimension", "minecraft:overworld"))
                  );
                  BlockPos pos = new BlockPos(data.getIntOr("x", 0), data.getIntOr("y", 0), data.getIntOr("z", 0));
                  lights.put(new RiftLightCleanup.Entry(dimension, pos), data.getLongOr("expires", 0L));
               } catch (IllegalArgumentException var6) {
               }
            }
         }
      }
   }

   private static void save(MinecraftServer server) {
      CompoundTag root = new CompoundTag();
      ListTag entries = new ListTag();

      for (Map.Entry<RiftLightCleanup.Entry, Long> light : lights.entrySet()) {
         CompoundTag data = new CompoundTag();
         RiftLightCleanup.Entry entry = light.getKey();
         data.putString("dimension", entry.dimension.identifier().toString());
         data.putInt("x", entry.pos.getX());
         data.putInt("y", entry.pos.getY());
         data.putInt("z", entry.pos.getZ());
         data.putLong("expires", light.getValue());
         entries.add(data);
      }

      root.put("lights", entries);
      server.getCommandStorage().set(KEY, root);
   }

   public static void track(ServerLevel level, Collection<BlockPos> positions, long expires) {
      if (!positions.isEmpty()) {
         MinecraftServer server = level.getServer();
         initialize(server);
         boolean changed = false;

         for (BlockPos pos : positions) {
            Long old = lights.put(new RiftLightCleanup.Entry(level.dimension(), pos.immutable()), expires);
            changed |= old == null || old != expires;
         }

         if (changed) {
            save(server);
         }
      }
   }

   public static boolean isManaged(ServerLevel level, BlockPos pos) {
      initialize(level.getServer());
      return lights.containsKey(new RiftLightCleanup.Entry(level.dimension(), pos));
   }

   public static void enqueue(ServerLevel level, Collection<BlockPos> positions) {
      track(level, positions, 0L);
   }

   private static long activeExpiry(ServerLevel level, BlockPos pos) {
      long expires = 0L;

      for (RiftEntity rift : RiftDirectory.loaded()) {
         if (!rift.isRemoved() && rift.level() == level) {
            BlockPos relative = pos.subtract(rift.getAnchorPos());
            int along = rift.isLongAlongX() ? relative.getX() : relative.getZ();
            int across = rift.isLongAlongX() ? relative.getZ() : relative.getX();
            if (relative.getY() == 2 && across == 0 && (along == 0 || Math.abs(along) == 3)) {
               expires = Math.max(expires, rift.getExpiresAt());
            }
         }
      }

      return expires;
   }

   public static void tick(MinecraftServer server) {
      initialize(server);
      boolean changed = false;
      Iterator<Map.Entry<RiftLightCleanup.Entry, Long>> iterator = lights.entrySet().iterator();

      while (iterator.hasNext()) {
         Map.Entry<RiftLightCleanup.Entry, Long> light = iterator.next();
         RiftLightCleanup.Entry entry = light.getKey();
         ServerLevel level = server.getLevel(entry.dimension);
         if (level == null) {
            iterator.remove();
            changed = true;
         } else if (light.getValue() <= level.getGameTime()) {
            long activeUntil = activeExpiry(level, entry.pos);
            if (activeUntil > level.getGameTime()) {
               light.setValue(activeUntil);
               changed = true;
            } else {
               LevelChunk chunk = level.getChunkSource().getChunkNow(entry.pos.getX() >> 4, entry.pos.getZ() >> 4);
               if (chunk != null) {
                  BlockState state = chunk.getBlockState(entry.pos);
                  if (state.is(Blocks.LIGHT)) {
                     BlockState replacement = state.getValue(BlockStateProperties.WATERLOGGED)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
                     level.setBlock(entry.pos, replacement, 3);
                  }

                  level.getChunkSource().getLightEngine().checkBlock(entry.pos);
                  iterator.remove();
                  changed = true;
               }
            }
         }
      }

      if (changed) {
         save(server);
      }
   }

   public static void clear() {
      lights.clear();
      initialized = false;
   }

   private RiftLightCleanup() {
   }

   private record Entry(ResourceKey<Level> dimension, BlockPos pos) {
   }
}
