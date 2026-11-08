package mielon.thesift.mixin;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootPool.class)
public interface LootPoolEntriesAccessor {
   @Accessor("entries")
   List<LootPoolEntryContainer> theSift$getEntries();

   @Mutable
   @Accessor("entries")
   void theSift$setEntries(List<LootPoolEntryContainer> entries);
}
