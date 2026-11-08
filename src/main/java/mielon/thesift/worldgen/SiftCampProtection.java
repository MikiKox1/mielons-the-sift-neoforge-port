package mielon.thesift.worldgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class SiftCampProtection {
   private static final List<ResourceKey<Structure>> CAMPS = Stream.of(
         "abandoned_camp_coral", "abandoned_camp_wastes", "abandoned_camp_overgrown", "ichor_dome_coral", "ichor_dome_overgrown", "ichor_dome_organic_desert"
      )
      .map(family -> ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("the_sift", family)))
      .toList();
   private static final ThreadLocal<SiftCampProtection> DECORATION = new ThreadLocal<>();
   private final List<BoundingBox> pieces;
   private final List<BoundingBox> domes;

   private SiftCampProtection(List<BoundingBox> pieces) {
      this(pieces, List.of());
   }

   private SiftCampProtection(List<BoundingBox> pieces, List<BoundingBox> domes) {
      this.pieces = pieces;
      this.domes = domes;
   }

   public static SiftCampProtection around(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ) {
      Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      List<Structure> camps = CAMPS.stream().map(registry::getValue).filter(Objects::nonNull).toList();
      ResourceKey<Structure> lairKey = ResourceKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath("the_sift", "sculkers_lair"));
      Structure lair = registry.getValue(lairKey.identifier());
      if (camps.isEmpty() && lair == null) {
         return new SiftCampProtection(List.of());
      }

      Set<StructureStart> starts = new HashSet<>();
      List<BoundingBox> pieces = new ArrayList<>();
      List<BoundingBox> domes = new ArrayList<>();

      for (int cx = (minX >> 4) - 2; cx <= (maxX >> 4) + 2; cx++) {
         for (int cz = (minZ >> 4) - 2; cz <= (maxZ >> 4) + 2; cz++) {
            ChunkAccess chunk = availableChunk(level, cx, cz);
            if (chunk != null) {
               if (lair != null) {
                  StructureStart local = chunk.getStartForStructure(lair);
                  if (local != null) {
                     starts.add(local);
                  }

                  boolean unavailableOwner = false;

                  for (long reference : chunk.getReferencesForStructure(lair)) {
                     ChunkAccess owner = availableChunk(level, ChunkPos.getX(reference), ChunkPos.getZ(reference));
                     if (owner == null) {
                        unavailableOwner = true;
                     } else {
                        StructureStart start = owner.getStartForStructure(lair);
                        if (start != null) {
                           starts.add(start);
                        }
                     }
                  }

                  if (unavailableOwner) {
                     pieces.add(new BoundingBox(cx << 4, level.getMinY(), cz << 4, (cx << 4) + 15, level.getMaxY() - 1, (cz << 4) + 15));
                  }
               }

               for (Structure camp : camps) {
                  StructureStart start = chunk.getStartForStructure(camp);
                  if (start != null) {
                     starts.add(start);
                  }
               }
            }
         }
      }

      for (StructureStart start : starts) {
         if (start.isValid()) {
            for (StructurePiece piece : start.getPieces()) {
               BoundingBox box = piece.getBoundingBox();
               if (registry.getKey(start.getStructure()).getPath().startsWith("ichor_dome_")) {
                  domes.add(new BoundingBox(box.minX(), box.minY() - 5, box.minZ(), box.maxX(), box.maxY() + 2, box.maxZ()));
               } else {
                  int margin = start.getStructure() == lair ? 1 : 0;
                  pieces.add(
                     new BoundingBox(box.minX() - margin, box.minY() - 5, box.minZ() - margin, box.maxX() + margin, box.maxY() + 2, box.maxZ() + margin)
                  );
               }
            }
         }
      }

      return new SiftCampProtection(pieces, domes);
   }

   private static ChunkAccess availableChunk(WorldGenLevel level, int x, int z) {
      return !level.hasChunk(x, z) ? null : level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS, false);
   }

   public static SiftCampProtection beginDecoration(WorldGenLevel level, BlockPos origin) {
      SiftCampProtection previous = DECORATION.get();
      if (previous == null && level.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         int x = origin.getX() & -16;
         int z = origin.getZ() & -16;
         DECORATION.set(around(level, x - 16, z - 16, x + 31, z + 31));
      }

      return previous;
   }

   public static void endDecoration(SiftCampProtection previous) {
      if (previous == null) {
         DECORATION.remove();
      } else {
         DECORATION.set(previous);
      }
   }

   public static boolean protectsDecoration(BlockPos pos) {
      SiftCampProtection guard = DECORATION.get();
      return guard != null && guard.contains(pos);
   }

   public boolean contains(BlockPos pos) {
      for (BoundingBox box : this.pieces) {
         if (box.isInside(pos)) {
            return true;
         }
      }

      for (BoundingBox box : this.domes) {
         if (box.isInside(pos) && inDomeColumn(box, pos.getX(), pos.getZ())) {
            return true;
         }
      }

      return false;
   }

   public boolean intersects(BoundingBox candidate) {
      for (BoundingBox box : this.pieces) {
         if (box.intersects(candidate)) {
            return true;
         }
      }

      for (BoundingBox box : this.domes) {
         if (box.intersects(candidate)) {
            int x = Math.max(candidate.minX(), Math.min(candidate.maxX(), (box.minX() + box.maxX()) / 2));
            int z = Math.max(candidate.minZ(), Math.min(candidate.maxZ(), (box.minZ() + box.maxZ()) / 2));
            if (inDomeColumn(box, x, z)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean inDomeColumn(BoundingBox box, int x, int z) {
      int dx = 2 * x - box.minX() - box.maxX();
      int dz = 2 * z - box.minZ() - box.maxZ();
      int diameter = box.getXSpan();
      return dx * dx + dz * dz <= diameter * diameter;
   }

   public static boolean intersects(WorldGenLevel level, BoundingBox candidate) {
      return around(level, candidate.minX(), candidate.minZ(), candidate.maxX(), candidate.maxZ()).intersects(candidate);
   }

   public static boolean near(WorldGenLevel level, int x, int z, int radius) {
      return intersects(level, new BoundingBox(x - radius, level.getMinY(), z - radius, x + radius, level.getMaxY() - 1, z + radius));
   }
}
