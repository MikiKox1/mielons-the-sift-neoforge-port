package mielon.thesift.worldgen;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import mielon.thesift.mixin.SiftBlendingDataInvoker;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class SiftTerrainUpgrade {
   public static final String GENERATION_TAG = "the_sift:terrain_generation";
   public static final int LEGACY = 1;
   public static final int CURRENT = 2;

   private SiftTerrainUpgrade() {
   }

   public static ChunkAccess unwrap(ChunkAccess chunk) {
      return chunk instanceof ImposterProtoChunk wrapper ? wrapper.getWrapped() : chunk;
   }

   public static SiftChunkGenerationAccess data(ChunkAccess chunk) {
      return (SiftChunkGenerationAccess)unwrap(chunk);
   }

   public static boolean legacy(ChunkAccess chunk) {
      return data(chunk).theSift$generation() == 1;
   }

   public static ProtoChunk load(ServerLevel level, CompoundTag tag, ProtoChunk chunk) {
      int saved = tag.getIntOr("the_sift:terrain_generation", 1);
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)
         && saved <= 1
         && chunk.getPersistedStatus().isOrAfter(ChunkStatus.NOISE)
         && !chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)
         && chunk.getBlockEntitiesPos().isEmpty()
         && chunk.getEntities().isEmpty()) {
         ProtoChunk fresh = new ProtoChunk(chunk.getPos(), UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
         fresh.setAllStarts(new HashMap<>(chunk.getAllStarts()));
         fresh.setAllReferences(new HashMap<>(chunk.getAllReferences()));
         fresh.setPersistedStatus(ChunkStatus.STRUCTURE_REFERENCES);
         read(level, new CompoundTag(), fresh);
         fresh.markUnsaved();
         return fresh;
      } else {
         read(level, tag, chunk);
         return chunk;
      }
   }

   public static void read(ServerLevel level, CompoundTag tag, ChunkAccess chunk) {
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         chunk = unwrap(chunk);
         int version = chunk.getPersistedStatus().isOrAfter(ChunkStatus.NOISE) ? tag.getIntOr("the_sift:terrain_generation", 1) : 2;
         if (version <= 0) {
            version = 1;
         }

         boolean changed = !tag.contains("the_sift:terrain_generation");
         data(chunk).theSift$generation(version);
         data(chunk).theSift$surfaceRevision(tag.getIntOr("the_sift:surface_transition", 0));
         if (version == 1 && chunk.getBlendingData() == null) {
            data(chunk).theSift$legacyBlending(SiftBlendingDataInvoker.theSift$create(0, 16, Optional.empty()));
            changed = true;
         }

         if (changed) {
            chunk.markUnsaved();
         }
      }
   }

   public static void write(ServerLevel level, CompoundTag tag, ChunkAccess chunk) {
      if (level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         tag.putInt("the_sift:terrain_generation", data(chunk).theSift$generation());
         if (data(chunk).theSift$surfaceRevision() > 0) {
            tag.putInt("the_sift:surface_transition", data(chunk).theSift$surfaceRevision());
         }
      }
   }

   public static boolean hasLegacyAround(WorldGenRegion region, ChunkPos center, int range) {
      for (int z = center.z() - range; z <= center.z() + range; z++) {
         for (int x = center.x() - range; x <= center.x() + range; x++) {
            if (region.hasChunk(x, z) && legacy(region.getChunk(x, z))) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean protects(WorldGenRegion region, BlockPos pos, BlockState replacement) {
      if (region.getLevel().dimension().equals(TheSiftDimension.LEVEL_KEY) && region.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
         ChunkAccess chunk = region.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
         if (legacy(chunk) && chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)) {
            return true;
         } else {
            BlockState previous = chunk.getBlockState(pos);
            boolean removesGround = isTerrain(previous) && replacement.isAir();
            boolean fillsAir = previous.isAir() && isTerrain(replacement);
            if (!removesGround && !fillsAir) {
               return false;
            } else {
               return legacy(chunk) ? true : SiftSurfaceTransition.protects(region, chunk, pos, removesGround);
            }
         }
      } else {
         return false;
      }
   }

   public static boolean isTerrain(BlockState state) {
      return SiftTerrainUpgrade.TerrainBlocks.VALUES.contains(state.getBlock());
   }

   public static int surface(ChunkAccess chunk, int x, int z) {
      MutableBlockPos pos = new MutableBlockPos();

      for (int y = 255; y >= 0; y--) {
         if (isTerrain(chunk.getBlockState(pos.set(x, y, z)))) {
            return y;
         }
      }

      return 0;
   }

   private static final class TerrainBlocks {
      private static final Set<Block> VALUES = create();

      private static Set<Block> create() {
         Set<Block> result = Collections.newSetFromMap(new IdentityHashMap<>());
         Set<String> vanilla = Set.of(
            "stone",
            "deepslate",
            "tuff",
            "granite",
            "diorite",
            "andesite",
            "dirt",
            "coarse_dirt",
            "rooted_dirt",
            "grass_block",
            "podzol",
            "mycelium",
            "gravel",
            "sand",
            "red_sand",
            "terracotta",
            "soul_sand",
            "soul_soil",
            "snow_block",
            "packed_ice",
            "ice",
            "blue_ice",
            "sculk",
            "clay",
            "bedrock"
         );
         Set<String> sift = Set.of(
            "siftslate",
            "cobbled_siftslate",
            "siftslate_growth",
            "soulshale",
            "soulshale_growth",
            "coral_soil",
            "coral_growth",
            "bright_coral_growth",
            "healthy_sculk",
            "dry_healthy_sculk",
            "soul_block",
            "snowy_sculk",
            "snowy_healthy_sculk",
            "ichor_snow_block",
            "sift_rubble"
         );

         for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id.getNamespace().equals("minecraft") && (vanilla.contains(id.getPath()) || id.getPath().endsWith("_ore"))
               || id.getNamespace().equals("the_sift") && (sift.contains(id.getPath()) || id.getPath().endsWith("_ore"))) {
               result.add(block);
            }
         }

         return result;
      }
   }
}
