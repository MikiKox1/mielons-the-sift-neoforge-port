package mielon.thesift.client.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class MonarchBossBarMixin {
   @Unique
   private int theSift$monarchTint = -1;

   @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"))
   private void theSift$barColor(GuiGraphicsExtractor graphics, int x, int y, BossEvent event, CallbackInfo ci) {
      this.theSift$monarchTint = -1;
      if (event.getName().getContents() instanceof TranslatableContents translated) {
         this.theSift$monarchTint = switch (translated.getKey()) {
            case "entity.the_sift.monarch" -> -11556983;
            case "entity.the_sift.monarch_echo" -> -7345180;
            default -> -1;
         };
      }
   }

   @Redirect(
      method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/Identifier;[Lnet/minecraft/resources/Identifier;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"
      )
   )
   private void theSift$tintNativeSprite(
      GuiGraphicsExtractor graphics,
      RenderPipeline pipeline,
      Identifier texture,
      int textureWidth,
      int textureHeight,
      int u,
      int v,
      int x,
      int y,
      int width,
      int height
   ) {
      graphics.blitSprite(pipeline, texture, textureWidth, textureHeight, u, v, x, y, width, height, this.theSift$monarchTint);
   }
}
