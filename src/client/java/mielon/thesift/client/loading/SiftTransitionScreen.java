package mielon.thesift.client.loading;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SiftTransitionScreen extends Screen {
   private final long createdAtNanos = System.nanoTime();

   public SiftTransitionScreen() {
      super(Component.translatable("multiplayer.downloadingTerrain"));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      SiftLoadingBackgroundRenderer.draw(graphics, this.width, this.height);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 50, -1);
   }

   @Override
   public void tick() {
      if (!SiftLoadingScreenState.isActive() || System.nanoTime() - this.createdAtNanos > 30000000000L) {
         SiftLoadingScreenState.clear();
         if (this.minecraft.gui.screen() == this) {
            this.minecraft.gui.setScreen(null);
         }
      }
   }
}
