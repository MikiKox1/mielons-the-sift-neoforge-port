package mielon.thesift.advancement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ConsumeItemTrigger.TriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class SiftBalancedDiet {
   private SiftBalancedDiet() {
   }

   public static Advancement extend(Advancement input) {
      Map<String, Criterion<?>> criteria = new LinkedHashMap<>(input.criteria());
      List<List<String>> requirements = new ArrayList<>(input.requirements().requirements());

      for (Item food : new Item[]{
         ModItems.RAW_SLABBER_MEAT,
         ModItems.COOKED_SLABBER_MEAT,
         ModItems.RAW_NUZZLE_MEAT,
         ModItems.COOKED_NUZZLE_MEAT,
         ModItems.RAW_SIFTER_MEAT,
         ModItems.COOKED_SIFTER_MEAT,
         ModItems.BLUBBY_BERRIES
      }) {
         String name = BuiltInRegistries.ITEM.getKey(food).toString();
         if (!criteria.containsKey(name)) {
            criteria.put(name, TriggerInstance.usedItem(BuiltInRegistries.ITEM, food));
            requirements.add(List.of(name));
         }
      }

      return new Advancement(
         input.parent(),
         input.display(),
         input.rewards(),
         Map.copyOf(criteria),
         new AdvancementRequirements(List.copyOf(requirements)),
         input.sendsTelemetryEvent()
      );
   }
}
