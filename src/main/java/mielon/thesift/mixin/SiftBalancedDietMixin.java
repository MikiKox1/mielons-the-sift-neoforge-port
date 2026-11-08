package mielon.thesift.mixin;

import mielon.thesift.advancement.SiftBalancedDiet;
import mielon.thesift.advancement.SiftMonsterAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MappedRegistry.class)
public abstract class SiftBalancedDietMixin<T> {
   @ModifyVariable(method = "register", at = @At("HEAD"), argsOnly = true, index = 2, require = 1)
   private T theSift$addFoods(T value, ResourceKey<T> key, T original, RegistrationInfo info) {
      if (key.isFor(Registries.ADVANCEMENT) && key.identifier().toString().equals("minecraft:husbandry/balanced_diet")) {
         return (T)SiftBalancedDiet.extend((Advancement)value);
      }

      if (key.isFor(Registries.ADVANCEMENT)) {
         String id = key.identifier().toString();
         if (id.equals("minecraft:adventure/kill_a_mob") || id.equals("minecraft:adventure/kill_all_mobs")) {
            return (T)SiftMonsterAdvancements.extend((Advancement)value, id.endsWith("kill_all_mobs"));
         }
      }

      return value;
   }
}
