package mielon.thesift.loot;

import mielon.thesift.block.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mielon.thesift.mixin.LootPoolEntriesAccessor;
import mielon.thesift.mixin.LootTablePoolsAccessor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class AncientCitySoulshaleLoot {
   private AncientCitySoulshaleLoot() {
   }

   public static void register() {
      NeoForge.EVENT_BUS.addListener(AncientCitySoulshaleLoot::onLootTableLoad);
   }

   private static void onLootTableLoad(LootTableLoadEvent event) {
      if (!BuiltInLootTables.ANCIENT_CITY.equals(event.getKey())) {
         return;
      }

      List<LootPool> pools = ((LootTablePoolsAccessor)event.getTable()).theSift$getPools();
      if (!pools.isEmpty()) {
         // The Fabric source inserts a weighted entry into the first existing pool.
         LootPoolEntriesAccessor pool = (LootPoolEntriesAccessor)pools.getFirst();
         List<LootPoolEntryContainer> entries = new ArrayList<>(pool.theSift$getEntries());
         entries.add(LootItem.lootTableItem(ModBlocks.CHISELED_SOULSHALE_ITEM).setWeight(3).build());
         pool.theSift$setEntries(List.copyOf(entries));
      }
   }
}
