package mielon.thesift.mixin;

import mielon.thesift.compat.SoulshaleIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StructureTemplateManager.class)
public abstract class SoulshaleTemplateLookupMixin {
   @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true, require = 1)
   private Identifier theSift$upgrade(Identifier id) {
      return SoulshaleIds.upgrade(id);
   }
}
