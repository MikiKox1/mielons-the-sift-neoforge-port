package mielon.thesift.mixin;

import mielon.thesift.map.SiftMapSampling;
import mielon.thesift.map.SiftMapStorage;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MapItem.class)
public abstract class SiftMapItemMixin {
   @Redirect(
      method = "update",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/material/MapColor;getPackedId(Lnet/minecraft/world/level/material/MapColor$Brightness;)B"
      )
   )
   private byte theSift$sampleActualBlockColor(MapColor color, Brightness brightness) {
      return SiftMapSampling.sample(color, brightness);
   }

   @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;updateColor(IIB)Z"))
   private boolean theSift$storeActualBlockColor(MapItemSavedData map, int x, int y, byte fallback) {
      if (((SiftMapStorage)map).theSift$colors().set(x, y, SiftMapSampling.take())) {
         map.setColor(x, y, fallback);
         return true;
      } else {
         return map.updateColor(x, y, fallback);
      }
   }
}
