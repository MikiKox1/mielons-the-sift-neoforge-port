package mielon.thesift.mixin;

import java.util.List;
import mielon.thesift.compat.SoulshaleIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerRecipeBook.class)
public abstract class SoulshaleRecipeBookMixin {
   @ModifyVariable(method = "loadRecipes", at = @At("HEAD"), argsOnly = true, require = 1)
   private List<ResourceKey<Recipe<?>>> theSift$upgrade(List<ResourceKey<Recipe<?>>> recipes) {
      return recipes.stream().map(key -> ResourceKey.create(Registries.RECIPE, SoulshaleIds.upgrade(key.identifier()))).toList();
   }
}
