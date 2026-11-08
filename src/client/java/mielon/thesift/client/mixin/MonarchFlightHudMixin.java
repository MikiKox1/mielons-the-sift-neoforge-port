package mielon.thesift.client.mixin;

import mielon.thesift.item.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class MonarchFlightHudMixin {
   private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("the_sift", "textures/gui/sprites/hud/monarch_elytra_indicator_background.png");
   private static final Identifier PROGRESS = Identifier.fromNamespaceAndPath("the_sift", "textures/gui/sprites/hud/monarch_elytra_indicator_progress.png");

   @Inject(method = "extractCrosshair", at = @At("RETURN"))
   private void theSift$cooldown(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null
         && client.options.getCameraType().isFirstPerson()
         && !player.isSpectator()
         && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MONARCH_ELYTRA)) {
         float cooldown = player.getCooldowns().getCooldownPercent(new ItemStack(ModItems.MONARCH_ELYTRA), 0.0F);
         if (!(cooldown <= 0.0F)) {
            int x = graphics.guiWidth() / 2 - 8;
            int y = graphics.guiHeight() / 2 - 12;
            graphics.blit(RenderPipelines.CROSSHAIR, BACKGROUND, x, y, 0.0F, 0.0F, 16, 4, 16, 4);
            int width = Math.min(16, (int)((1.0F - cooldown) * 16.0F));
            if (width > 0) {
               graphics.blit(RenderPipelines.CROSSHAIR, PROGRESS, x, y, 0.0F, 0.0F, width, 4, 16, 4);
            }
         }
      }
   }
}
