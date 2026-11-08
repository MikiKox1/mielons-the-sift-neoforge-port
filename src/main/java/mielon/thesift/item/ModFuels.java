package mielon.thesift.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import mielon.thesift.block.ModBlocks;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.neoforged.neoforge.event.DefaultDataComponentsBoundEvent;

public final class ModFuels {
   private ModFuels() {
   }

   public static void onDefaultDataComponentsBound(DefaultDataComponentsBoundEvent event) {
      // Vanilla item components are not readable during ModifyDefaultComponentsEvent.
      if (!event.shouldUpdateStaticData()) {
         return;
      }

      for (Entry<Item, Item> entry : counterparts().entrySet()) {
         CookingFuel fuel = entry.getValue().components().get(DataComponents.COOKING_FUEL);
         // Match the original: an absent vanilla fuel does not erase another
         // mod's explicit component on our item.
         if (fuel == null) {
            continue;
         }
         Item item = entry.getKey();
         DataComponentMap components = item.components();
         if (!Objects.equals(components.get(DataComponents.COOKING_FUEL), fuel)) {
            DataComponentMap.Builder updated = DataComponentMap.builder().addAll(components).set(DataComponents.COOKING_FUEL, fuel);

            item.builtInRegistryHolder().bindComponents(updated.build());
            item.resetDefaultResource();
         }
      }
   }

   public static Map<Item, Item> counterparts() {
      Map<Item, Item> result = new LinkedHashMap<>();
      result.put(ModBlocks.OVERGROWN_WILLOW_PLANKS_ITEM, Items.OAK_PLANKS);
      result.put(ModBlocks.OVERGROWN_WILLOW_LOG_ITEM, Items.OAK_LOG);
      result.put(ModBlocks.OVERGROWN_WILLOW_WOOD_ITEM, Items.OAK_WOOD);
      result.put(ModBlocks.OVERGROWN_WILLOW_STAIRS_ITEM, Items.OAK_STAIRS);
      result.put(ModBlocks.OVERGROWN_WILLOW_SLAB_ITEM, Items.OAK_SLAB);
      result.put(ModBlocks.OVERGROWN_WILLOW_FENCE_ITEM, Items.OAK_FENCE);
      result.put(ModBlocks.OVERGROWN_WILLOW_FENCE_GATE_ITEM, Items.OAK_FENCE_GATE);
      result.put(ModBlocks.OVERGROWN_WILLOW_DOOR_ITEM, Items.OAK_DOOR);
      result.put(ModBlocks.OVERGROWN_WILLOW_TRAPDOOR_ITEM, Items.OAK_TRAPDOOR);
      result.put(ModBlocks.OVERGROWN_WILLOW_PRESSURE_PLATE_ITEM, Items.OAK_PRESSURE_PLATE);
      result.put(ModBlocks.OVERGROWN_WILLOW_BUTTON_ITEM, Items.OAK_BUTTON);
      result.put(ModBlocks.OVERGROWN_WILLOW_SHELF_ITEM, Items.OAK_SHELF);
      result.put(ModBlocks.OVERGROWN_WILLOW_SAPLING_ITEM, Items.OAK_SAPLING);
      result.put(ModBlocks.ASH_WILLOW_SAPLING_ITEM, Items.OAK_SAPLING);
      result.put(ModBlocks.STRIPPED_OVERGROWN_WILLOW_LOG_ITEM, Items.STRIPPED_OAK_LOG);
      result.put(ModBlocks.STRIPPED_OVERGROWN_WILLOW_WOOD_ITEM, Items.STRIPPED_OAK_WOOD);
      result.put(ModBlocks.OVERGROWN_WILLOW_SIGN_ITEM, Items.OAK_SIGN);
      result.put(ModBlocks.OVERGROWN_WILLOW_HANGING_SIGN_ITEM, Items.OAK_HANGING_SIGN);
      result.put(ModItems.OVERGROWN_WILLOW_BOAT, Items.OAK_BOAT);
      result.put(ModItems.OVERGROWN_WILLOW_CHEST_BOAT, Items.OAK_CHEST_BOAT);
      result.put(ModBlocks.ASH_WILLOW_PLANKS_ITEM, Items.OAK_PLANKS);
      result.put(ModBlocks.ASH_WILLOW_LOG_ITEM, Items.OAK_LOG);
      result.put(ModBlocks.ASH_WILLOW_WOOD_ITEM, Items.OAK_WOOD);
      result.put(ModBlocks.ASH_WILLOW_STAIRS_ITEM, Items.OAK_STAIRS);
      result.put(ModBlocks.ASH_WILLOW_SLAB_ITEM, Items.OAK_SLAB);
      result.put(ModBlocks.ASH_WILLOW_FENCE_ITEM, Items.OAK_FENCE);
      result.put(ModBlocks.ASH_WILLOW_FENCE_GATE_ITEM, Items.OAK_FENCE_GATE);
      result.put(ModBlocks.ASH_WILLOW_DOOR_ITEM, Items.OAK_DOOR);
      result.put(ModBlocks.ASH_WILLOW_TRAPDOOR_ITEM, Items.OAK_TRAPDOOR);
      result.put(ModBlocks.ASH_WILLOW_PRESSURE_PLATE_ITEM, Items.OAK_PRESSURE_PLATE);
      result.put(ModBlocks.ASH_WILLOW_BUTTON_ITEM, Items.OAK_BUTTON);
      result.put(ModBlocks.ASH_WILLOW_SHELF_ITEM, Items.OAK_SHELF);
      result.put(ModBlocks.STRIPPED_ASH_WILLOW_LOG_ITEM, Items.STRIPPED_OAK_LOG);
      result.put(ModBlocks.STRIPPED_ASH_WILLOW_WOOD_ITEM, Items.STRIPPED_OAK_WOOD);
      result.put(ModBlocks.ASH_WILLOW_SIGN_ITEM, Items.OAK_SIGN);
      result.put(ModBlocks.ASH_WILLOW_HANGING_SIGN_ITEM, Items.OAK_HANGING_SIGN);
      result.put(ModItems.ASH_WILLOW_BOAT, Items.OAK_BOAT);
      result.put(ModItems.ASH_WILLOW_CHEST_BOAT, Items.OAK_CHEST_BOAT);
      return result;
   }
}
