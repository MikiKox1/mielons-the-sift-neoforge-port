package mielon.thesift.advancement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.entity.ModEntities;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.predicates.entity.EntityPredicate.Builder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.KilledTrigger.TriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public final class SiftMonsterAdvancements {
   private SiftMonsterAdvancements() {
   }

   public static Advancement extend(Advancement input, boolean all) {
      Map<String, Criterion<?>> criteria = new LinkedHashMap<>(input.criteria());
      List<List<String>> requirements = new ArrayList<>();

      for (List<String> group : input.requirements().requirements()) {
         requirements.add(new ArrayList<>(group));
      }

      for (EntityType<?> type : new EntityType[]{
         ModEntities.DARK_SNIFFER, ModEntities.SEEDLING, ModEntities.NESTER, ModEntities.POLLINATOR, ModEntities.STALKER, ModEntities.SCAVENGER
      }) {
         String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
         if (!criteria.containsKey(name)) {
            criteria.put(name, TriggerInstance.playerKilledEntity(Builder.entity().of(BuiltInRegistries.ENTITY_TYPE, type)));
            if (all) {
               requirements.add(new ArrayList<>(List.of(name)));
            } else {
               for (List<String> group : requirements) {
                  group.add(name);
               }
            }
         }
      }

      return new Advancement(
         input.parent(),
         input.display(),
         input.rewards(),
         Map.copyOf(criteria),
         new AdvancementRequirements(requirements.stream().map(List::copyOf).toList()),
         input.sendsTelemetryEvent()
      );
   }
}
