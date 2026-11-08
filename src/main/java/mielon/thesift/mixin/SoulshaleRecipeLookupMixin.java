package mielon.thesift.mixin;

import mielon.thesift.compat.SoulshaleIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
public abstract class SoulshaleRecipeLookupMixin {
   @ModifyVariable(method = "byKey", at = @At("HEAD"), argsOnly = true, require = 1)
   private ResourceKey<?> theSift$upgrade(ResourceKey<?> id) {
      return ResourceKey.create(id.registryKey(), SoulshaleIds.upgrade(id.identifier()));
   }
}
