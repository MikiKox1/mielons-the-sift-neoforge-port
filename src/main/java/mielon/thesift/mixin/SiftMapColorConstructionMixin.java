package mielon.thesift.mixin;

import mielon.thesift.map.SiftMapColorConstruction;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MapColor.class)
public abstract class SiftMapColorConstructionMixin {
   @Shadow
   @Final
   private static MapColor[] MATERIAL_COLORS;

   @Redirect(
      method = "<init>",
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/level/material/MapColor;MATERIAL_COLORS:[Lnet/minecraft/world/level/material/MapColor;",
         opcode = 178
      )
   )
   private MapColor[] theSift$keepPrivateColorsOutOfVanillaPalette() {
      return SiftMapColorConstruction.isPrivate() ? new MapColor[64] : MATERIAL_COLORS;
   }
}
