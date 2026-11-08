package mielon.thesift.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import mielon.thesift.client.map.SiftMapClientColors;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public abstract class SiftMapTextureMixin {
   @Unique
   private int theSift$mapId;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void theSift$rememberMap(MapTextureManager manager, int id, MapItemSavedData data, CallbackInfo callback) {
      this.theSift$mapId = id;
   }

   @Redirect(method = "updateTextureIfNeeded", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;setPixel(III)V"))
   private void theSift$renderBlockColors(NativeImage image, int x, int y, int fallback) {
      image.setPixel(x, y, SiftMapClientColors.get(this.theSift$mapId, x, y, fallback));
   }
}
