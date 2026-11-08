package mielon.thesift.map;

import mielon.thesift.world.TheSiftDimension;
import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class SiftExplorationMaps {
   private static boolean initialized;
   private static boolean itemsInitialized;

   private SiftExplorationMaps() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   public static SiftExplorationMaps.Kind byId(String id) {
      for (SiftExplorationMaps.Kind kind : SiftExplorationMaps.Kind.values()) {
         if (kind.structureId.equals(id)) {
            return kind;
         }
      }

      throw new IllegalArgumentException("Unknown Sift explorer map destination: " + id);
   }

   public static SiftExplorationMaps.Kind fromMarkerId(String id) {
      for (SiftExplorationMaps.Kind kind : SiftExplorationMaps.Kind.values()) {
         if (kind.markerId().equals(id)) {
            return kind;
         }
      }

      return null;
   }

   public static void initialize() {
      if (!initialized) {
         initialized = true;

         for (SiftExplorationMaps.Kind kind : SiftExplorationMaps.Kind.values()) {
            kind.decoration = ModRegistration.register(
               BuiltInRegistries.MAP_DECORATION_TYPE, id(kind.structureId), new MapDecorationType(id(kind.structureId), true, true)
            );
         }

         ModRegistration.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, id("exploration_map"), SiftExplorationMapFunction.CODEC);
      }
   }

   public static void initializeItems() {
      if (!itemsInitialized) {
         itemsInitialized = true;

         for (SiftExplorationMaps.Kind kind : SiftExplorationMaps.Kind.values()) {
            kind.item = ModRegistration.register(
               BuiltInRegistries.ITEM,
               id(kind.itemId),
               new MapItem(new Properties().setId(ResourceKey.create(Registries.ITEM, id(kind.itemId))))
            );
         }
      }
   }

   public static ItemStack create(ServerLevel level, SiftExplorationMaps.Kind kind, BlockPos origin) {
      if (!level.dimension().equals(TheSiftDimension.LEVEL_KEY)) {
         return ItemStack.EMPTY;
      }

      Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(kind.structureKey()).value();
      StructureStart source = level.structureManager().getStructureAt(origin, structure);
      if (source.isValid() && source.canBeReferenced()) {
         level.structureManager().addReference(source);
      }

      BlockPos located = level.findNearestMapStructure(kind.destination(), origin, 100, true);
      if (located == null) {
         return ItemStack.EMPTY;
      }

      BlockPos target = located;
      if (kind == SiftExplorationMaps.Kind.LAIR) {
         ChunkAccess chunk = level.getChunk(located.getX() >> 4, located.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS);
         target = firstCorridorTarget(chunk.getStartForStructure(structure));
         if (target == null) {
            return ItemStack.EMPTY;
         }
      }

      return createAt(level, kind, target);
   }

   public static BlockPos firstCorridorTarget(StructureStart start) {
      return start != null && start.isValid() && !start.getPieces().isEmpty() && start.getPieces().get(0) instanceof PoolElementStructurePiece root
         ? root.getBoundingBox().getCenter()
         : null;
   }

   public static ItemStack createAt(ServerLevel level, SiftExplorationMaps.Kind kind, BlockPos target) {
      ItemStack stack = new ItemStack(kind.item());
      MapItem.applyNewSavedData(level, stack, target.getX(), target.getZ(), (byte)2, true, true);
      MapItem.renderBiomePreviewMap(level, stack);
      MapItemSavedData.addTargetDecoration(stack, target, kind.markerId(), kind.decoration());
      stack.set(DataComponents.ITEM_NAME, Component.translatable(kind.translationKey()));
      return stack;
   }

   public enum Kind {
      WASTES("abandoned_camp_wastes", "abandoned_camp_wastes_map", 12071980),
      OVERGROWN("abandoned_camp_overgrown", "abandoned_camp_overgrown_map", 10024164),
      CORAL("abandoned_camp_coral", "abandoned_camp_coral_map", 12242900),
      LAIR("sculkers_lair", "buried_sculkers_lair_map", 4021061);

      public final String structureId;
      public final String itemId;
      public final int color;
      private MapDecorationType decoration;
      private Item item;

      Kind(String structureId, String itemId, int color) {
         this.structureId = structureId;
         this.itemId = itemId;
         this.color = color;
      }

      public String translationKey() {
         return "item.the_sift." + this.itemId;
      }

      public String markerId() {
         return "the_sift:" + this.structureId;
      }

      public TagKey<Structure> destination() {
         return TagKey.create(Registries.STRUCTURE, SiftExplorationMaps.id("on_" + this.structureId + "_maps"));
      }

      public ResourceKey<Structure> structureKey() {
         return ResourceKey.create(Registries.STRUCTURE, SiftExplorationMaps.id(this.structureId));
      }

      public Holder<MapDecorationType> decoration() {
         return BuiltInRegistries.MAP_DECORATION_TYPE.wrapAsHolder(this.decoration);
      }

      public Item item() {
         return this.item;
      }
   }
}
