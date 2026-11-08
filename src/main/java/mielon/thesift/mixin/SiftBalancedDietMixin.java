package mielon.thesift.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mielon.thesift.advancement.SiftBalancedDiet;
import mielon.thesift.advancement.SiftMonsterAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerAdvancementManager.class)
public abstract class SiftBalancedDietMixin {
   @ModifyVariable(
      method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
      at = @At("HEAD"),
      argsOnly = true,
      require = 1
   )
   private Map<Identifier, Advancement> theSift$addFoods(Map<Identifier, Advancement> input) {
      HashMap<Identifier, Advancement> output = new HashMap<>(input);
      Identifier id = Identifier.fromNamespaceAndPath("minecraft", "husbandry/balanced_diet");
      if (output.containsKey(id)) {
         output.put(id, SiftBalancedDiet.extend(output.get(id)));
      }

      for (String path : List.of("adventure/kill_a_mob", "adventure/kill_all_mobs")) {
         Identifier monsterId = Identifier.fromNamespaceAndPath("minecraft", path);
         if (output.containsKey(monsterId)) {
            output.put(monsterId, SiftMonsterAdvancements.extend(output.get(monsterId), path.endsWith("kill_all_mobs")));
         }
      }

      return output;
   }
}
