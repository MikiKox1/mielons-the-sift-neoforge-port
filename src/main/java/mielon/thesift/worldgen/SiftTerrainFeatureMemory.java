package mielon.thesift.worldgen;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class SiftTerrainFeatureMemory {
   private static Path storage;
   private static final Long2ObjectLinkedOpenHashMap<SiftTerrainFeatureMemory.Marks> CHUNKS = new Long2ObjectLinkedOpenHashMap<>(128);

   private SiftTerrainFeatureMemory() {
   }

   private static long key(int x, int z) {
      return (long)(x >> 4) << 32 ^ z >> 4 & 4294967295L;
   }

   private static int column(int x, int z) {
      return (x & 15) * 16 + (z & 15);
   }

   static synchronized void support(long salt, SiftStrataSupportShape.Support support) {
      int r = 8;
      SiftTerrainFeatureMemory.Volume volume = new SiftTerrainFeatureMemory.Volume(salt, support);

      for (int x = support.x() - r >> 4; x <= support.x() + r >> 4; x++) {
         for (int z = support.z() - r >> 4; z <= support.z() + r >> 4; z++) {
            writable(key(x << 4, z << 4)).supports.add(volume);
         }
      }
   }

   static synchronized boolean isSupport(int x, int y, int z) {
      SiftTerrainFeatureMemory.Marks marks = readable(key(x, z));
      if (marks == null) {
         return false;
      }

      for (SiftTerrainFeatureMemory.Volume volume : marks.supports) {
         SiftStrataSupportShape.Support s = volume.support();
         if (y >= s.floor() - 4 && y <= s.ceiling() + 14 && SiftStrataSupportShape.contains(volume.salt(), s.x(), s.z(), s.floor(), s.ceiling(), x, y, z)) {
            return true;
         }
      }

      return false;
   }

   static synchronized void canyon(int x, int z, int floor, int bank) {
      excludeRibs(x, z, floor - 2, bank + 1);
      protectTerrain(x, z, floor - 2, bank + 1);
   }

   static synchronized void ravine(int x, int z, int floor, int bank) {
      excludeRibs(x, z, floor - 2, bank + 1);
      readable(key(x, z)).ravineBanks.put(column(x, z), bank);
   }

   static synchronized int ravineBank(int x, int z) {
      SiftTerrainFeatureMemory.Marks marks = readable(key(x, z));
      return marks == null ? Integer.MIN_VALUE : marks.ravineBanks.getOrDefault(column(x, z), Integer.MIN_VALUE);
   }

   static synchronized void protectShelter(int x, int z, int bottom, int top) {
      protectTerrain(x, z, bottom, top);
      excludeRibs(x, z, bottom, top);
      writable(key(x, z)).shelters.computeIfAbsent(column(x, z), k -> new ArrayList<>()).add(new SiftTerrainFeatureMemory.Span(bottom, top));
   }

   static synchronized boolean isShelter(BlockPos pos) {
      SiftTerrainFeatureMemory.Marks marks = readable(key(pos.getX(), pos.getZ()));
      if (marks == null) {
         return false;
      }

      List<SiftTerrainFeatureMemory.Span> spans = marks.shelters.get(column(pos.getX(), pos.getZ()));
      if (spans != null) {
         for (SiftTerrainFeatureMemory.Span span : spans) {
            if (pos.getY() >= span.bottom() && pos.getY() <= span.top()) {
               return true;
            }
         }
      }

      return false;
   }

   static synchronized void protectTerrain(int x, int z, int bottom, int top) {
      writable(key(x, z)).protectedTerrain.computeIfAbsent(column(x, z), k -> new ArrayList<>()).add(new SiftTerrainFeatureMemory.Span(bottom, top));
   }

   static synchronized boolean isProtectedTerrain(int x, int y, int z) {
      if (SiftPortalVegetation.allowsFloor(new BlockPos(x, y, z))) {
         return false;
      }

      SiftTerrainFeatureMemory.Marks marks = readable(key(x, z));
      if (marks == null) {
         return false;
      }

      List<SiftTerrainFeatureMemory.Span> spans = marks.protectedTerrain.get(column(x, z));
      if (spans != null) {
         for (SiftTerrainFeatureMemory.Span span : spans) {
            if (y >= span.bottom() && y <= span.top()) {
               return true;
            }
         }
      }

      return false;
   }

   static synchronized void excludeRibs(int x, int z, int bottom, int top) {
      SiftTerrainFeatureMemory.Marks marks = writable(key(x, z));
      marks.canyons.computeIfAbsent(column(x, z), k -> new ArrayList<>()).add(new SiftTerrainFeatureMemory.Span(bottom, top));
   }

   static synchronized boolean isCanyonWall(int x, int y, int z) {
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            SiftTerrainFeatureMemory.Marks marks = readable(key(x + dx, z + dz));
            if (marks != null) {
               List<SiftTerrainFeatureMemory.Span> spans = marks.canyons.get(column(x + dx, z + dz));
               if (spans != null) {
                  for (SiftTerrainFeatureMemory.Span span : spans) {
                     if (y >= span.bottom() && y <= span.top()) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   static synchronized void organicPlant(BlockPos pos) {
      writable(key(pos.getX(), pos.getZ())).organicPlants.add(pos.asLong());
   }

   static synchronized boolean isOrganicPlant(BlockPos pos) {
      SiftTerrainFeatureMemory.Marks marks = readable(key(pos.getX(), pos.getZ()));
      return marks != null && marks.organicPlants.contains(pos.asLong());
   }

   static synchronized void clear() {
      CHUNKS.clear();
      storage = null;
   }

   public static synchronized void initialize(MinecraftServer server) {
      Path root = server.getWorldPath(LevelResource.ROOT).resolve("data/the_sift/worldgen_geometry").toAbsolutePath().normalize();
      if (storage != null && !storage.equals(root)) {
         CHUNKS.clear();
      }

      storage = root;
   }

   public static synchronized void flush(MinecraftServer server) {
      for (Entry<SiftTerrainFeatureMemory.Marks> entry : CHUNKS.long2ObjectEntrySet()) {
         save(entry.getLongKey(), entry.getValue());
      }
   }

   public static synchronized void tick(MinecraftServer server) {
      if (server.overworld().getGameTime() % 100L == 0L) {
         int budget = 8;

         for (Entry<SiftTerrainFeatureMemory.Marks> entry : CHUNKS.long2ObjectEntrySet()) {
            if (entry.getValue().dirty) {
               save(entry.getLongKey(), entry.getValue());
               if (--budget == 0) {
                  break;
               }
            }
         }
      }
   }

   private static SiftTerrainFeatureMemory.Marks writable(long key) {
      SiftTerrainFeatureMemory.Marks marks = readable(key);
      marks.dirty = true;
      return marks;
   }

   private static SiftTerrainFeatureMemory.Marks readable(long key) {
      SiftTerrainFeatureMemory.Marks marks = CHUNKS.getAndMoveToLast(key);
      if (marks != null) {
         return marks;
      }
      marks = load(key);
      CHUNKS.put(key, marks);
      if (CHUNKS.size() > 4096) {
         long oldest = CHUNKS.firstLongKey();
         save(oldest, CHUNKS.get(oldest));
         CHUNKS.removeFirst();
      }
      return marks;
   }

   private static Path file(long key) {
      return storage.resolve(Long.toUnsignedString(key, 16) + ".nbt");
   }

   private static SiftTerrainFeatureMemory.Marks load(long key) {
      SiftTerrainFeatureMemory.Marks marks = new SiftTerrainFeatureMemory.Marks();
      if (storage != null && Files.isRegularFile(file(key))) {
         try (InputStream input = Files.newInputStream(file(key))) {
            CompoundTag tag = NbtIo.readCompressed(input, NbtAccounter.create(8388608L));
            long[] values = tag.getLongArray("supports").orElse(new long[0]);

            for (int i = 0; i + 4 < values.length; i += 5) {
               marks.supports
                  .add(
                     new SiftTerrainFeatureMemory.Volume(
                        values[i], new SiftStrataSupportShape.Support((int)values[i + 1], (int)values[i + 2], (int)values[i + 3], (int)values[i + 4])
                     )
                  );
            }

            readSpans(marks.canyons, tag.getLongArray("canyons").orElse(new long[0]));
            readSpans(marks.protectedTerrain, tag.getLongArray("protected").orElse(new long[0]));
            readSpans(marks.shelters, tag.getLongArray("shelters").orElse(new long[0]));
            values = tag.getLongArray("banks").orElse(new long[0]);

            for (int i = 0; i + 1 < values.length; i += 2) {
               marks.ravineBanks.put((int)values[i], (int)values[i + 1]);
            }

            for (long value : tag.getLongArray("plants").orElse(new long[0])) {
               marks.organicPlants.add(value);
            }

            values = tag.getLongArray("reservations").orElse(new long[0]);

            for (int i = 0; i + 3 < values.length; i += 4) {
               marks.reservations.add(new int[]{(int)values[i], (int)values[i + 1], (int)values[i + 2], (int)values[i + 3]});
            }

            return marks;
         } catch (IOException error) {
            throw new UncheckedIOException("Cannot read worldgen geometry " + file(key), error);
         }
      } else {
         return marks;
      }
   }

   private static void readSpans(Map<Integer, List<SiftTerrainFeatureMemory.Span>> target, long[] values) {
      for (int i = 0; i + 2 < values.length; i += 3) {
         target.computeIfAbsent((int)values[i], k -> new ArrayList<>()).add(new SiftTerrainFeatureMemory.Span((int)values[i + 1], (int)values[i + 2]));
      }
   }

   private static long[] spans(Map<Integer, List<SiftTerrainFeatureMemory.Span>> source) {
      ArrayList<Long> values = new ArrayList<>();
      source.forEach((column, spans) -> spans.forEach(span -> {
         values.add((long)column.intValue());
         values.add((long)span.bottom());
         values.add((long)span.top());
      }));
      return values.stream().mapToLong(Long::longValue).toArray();
   }

   private static void save(long key, SiftTerrainFeatureMemory.Marks marks) {
      if (marks.dirty && storage != null) {
         CompoundTag tag = new CompoundTag();
         tag.putLongArray(
            "supports",
            marks.supports
               .stream()
               .flatMapToLong(v -> LongStream.of(v.salt(), v.support().x(), v.support().z(), v.support().floor(), v.support().ceiling()))
               .toArray()
         );
         tag.putLongArray("canyons", spans(marks.canyons));
         tag.putLongArray("protected", spans(marks.protectedTerrain));
         tag.putLongArray("shelters", spans(marks.shelters));
         tag.putLongArray(
            "banks", marks.ravineBanks.entrySet().stream().flatMapToLong(e -> LongStream.of(e.getKey().intValue(), e.getValue().intValue())).toArray()
         );
         tag.putLongArray("plants", marks.organicPlants.stream().mapToLong(Long::longValue).toArray());
         tag.putLongArray("reservations", marks.reservations.stream().flatMapToLong(r -> Arrays.stream(r).asLongStream()).toArray());

         try {
            Files.createDirectories(storage);
            Path temporary = storage.resolve(Long.toUnsignedString(key, 16) + ".tmp");

            try (OutputStream output = Files.newOutputStream(temporary)) {
               NbtIo.writeCompressed(tag, output);
            }

            try {
               Files.move(temporary, file(key), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
               Files.move(temporary, file(key), StandardCopyOption.REPLACE_EXISTING);
            }

            marks.dirty = false;
         } catch (IOException error) {
            throw new UncheckedIOException("Cannot save worldgen geometry " + file(key), error);
         }
      }
   }

   static synchronized boolean intersectsReservation(int minX, int minZ, int maxX, int maxZ) {
      for (int x = minX >> 4; x <= maxX >> 4; x++) {
         for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
            for (int[] r : readable(key(x << 4, z << 4)).reservations) {
               if (maxX >= r[0] && minX <= r[2] && maxZ >= r[1] && minZ <= r[3]) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   static synchronized boolean reserve(int minX, int minZ, int maxX, int maxZ) {
      if (intersectsReservation(minX, minZ, maxX, maxZ)) {
         return false;
      }

      for (int x = minX >> 4; x <= maxX >> 4; x++) {
         for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
            writable(key(x << 4, z << 4)).reservations.add(new int[]{minX, minZ, maxX, maxZ});
         }
      }

      return true;
   }

   private static final class Marks {
      boolean dirty;
      final List<int[]> reservations = new ArrayList<>();
      final Set<Long> organicPlants = new HashSet<>();
      final List<SiftTerrainFeatureMemory.Volume> supports = new ArrayList<>();
      final Map<Integer, List<SiftTerrainFeatureMemory.Span>> canyons = new HashMap<>();
      final Map<Integer, Integer> ravineBanks = new HashMap<>();
      final Map<Integer, List<SiftTerrainFeatureMemory.Span>> protectedTerrain = new HashMap<>();
      final Map<Integer, List<SiftTerrainFeatureMemory.Span>> shelters = new HashMap<>();
   }

   private record Span(int bottom, int top) {
   }

   private record Volume(long salt, SiftStrataSupportShape.Support support) {
   }
}
