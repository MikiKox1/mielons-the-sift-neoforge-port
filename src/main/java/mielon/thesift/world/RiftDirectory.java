package mielon.thesift.world;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mielon.thesift.entity.RiftEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class RiftDirectory {
   private static final Identifier KEY = Identifier.fromNamespaceAndPath("the_sift", "rift_exits");
   private static final Map<UUID, RiftDirectory.Exit> exits = new HashMap<>();
   private static final Map<UUID, RiftEntity> loaded = new HashMap<>();
   private static boolean initialized;
   private static boolean dirty;

   public static void tick(MinecraftServer server) {
      if (!initialized) {
         initialized = true;

         for (Tag raw : server.getCommandStorage().get(KEY).getListOrEmpty("exits")) {
            if (raw instanceof CompoundTag t) {
               try {
                  UUID id = UUID.fromString(t.getStringOr("id", ""));
                  exits.put(
                     id,
                     new RiftDirectory.Exit(
                        id,
                        new BlockPos(t.getIntOr("x", 0), t.getIntOr("y", 0), t.getIntOr("z", 0)),
                        new BlockPos(t.getIntOr("ax", 0), t.getIntOr("ay", 0), t.getIntOr("az", 0)),
                        t.getLongOr("expires", 0L)
                     )
                  );
               } catch (IllegalArgumentException var6) {
               }
            }
         }
      }

      dirty = dirty | exits.values().removeIf(ex -> ex.expires <= server.overworld().getGameTime());
      if (dirty) {
         CompoundTag tag = new CompoundTag();
         ListTag list = new ListTag();

         for (RiftDirectory.Exit e : exits.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", e.id.toString());
            t.putInt("x", e.pos.getX());
            t.putInt("y", e.pos.getY());
            t.putInt("z", e.pos.getZ());
            t.putInt("ax", e.anchor.getX());
            t.putInt("ay", e.anchor.getY());
            t.putInt("az", e.anchor.getZ());
            t.putLong("expires", e.expires);
            list.add(t);
         }

         tag.put("exits", list);
         server.getCommandStorage().set(KEY, tag);
         dirty = false;
      }
   }

   public static void track(RiftEntity rift) {
      loaded.put(rift.getUUID(), rift);
      RiftDirectory.Exit e = exits.get(rift.getUUID());
      if (e != null) {
         rift.extendExpiresAt(e.expires);
      }
   }

   public static void removed(RiftEntity rift, boolean destroyed) {
      loaded.remove(rift.getUUID());
      if (destroyed && exits.remove(rift.getUUID()) != null) {
         dirty = true;
      }
   }

   public static List<RiftEntity> loaded() {
      return List.copyOf(loaded.values());
   }

   public static Optional<RiftDirectory.Exit> near(BlockPos anchor, long now) {
      return nearby(anchor, now).stream().findFirst();
   }

   public static List<RiftDirectory.Exit> nearby(BlockPos anchor, long now) {
      return exits.values()
         .stream()
         .filter(e -> e.expires - now > 20L && (horizontal(e.anchor, anchor) <= 4096.0 || horizontal(e.pos, anchor) <= 4096.0))
         .sorted(Comparator.comparingDouble(e -> horizontal(e.pos, anchor)))
         .toList();
   }

   private static double horizontal(BlockPos a, BlockPos b) {
      double x = (double)a.getX() - b.getX();
      double z = (double)a.getZ() - b.getZ();
      return x * x + z * z;
   }

   public static void add(RiftEntity rift, BlockPos anchor) {
      exits.put(rift.getUUID(), new RiftDirectory.Exit(rift.getUUID(), rift.getAnchorPos(), anchor, rift.getExpiresAt()));
      dirty = true;
      track(rift);
   }

   public static RiftDirectory.Exit extend(RiftDirectory.Exit e, long expiry) {
      if (expiry > e.expires) {
         e = new RiftDirectory.Exit(e.id, e.pos, e.anchor, expiry);
         exits.put(e.id, e);
         dirty = true;
      }

      RiftEntity rift = loaded.get(e.id);
      if (rift != null) {
         rift.extendExpiresAt(e.expires);
      }

      return e;
   }

   public static void clear() {
      exits.clear();
      loaded.clear();
      initialized = false;
      dirty = false;
   }

   public record Exit(UUID id, BlockPos pos, BlockPos anchor, long expires) {
   }
}
