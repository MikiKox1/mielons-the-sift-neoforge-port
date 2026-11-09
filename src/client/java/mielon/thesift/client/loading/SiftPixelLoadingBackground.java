package mielon.thesift.client.loading;

import mielon.thesift.client.render.PixelPortalSurface;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SiftPixelLoadingBackground {
   private SiftPixelLoadingBackground() {
   }

   public static void draw(GuiGraphicsExtractor graphics, int width, int height) {
      graphics.fill(0, 0, width, height, -14237499);
      float scale = Math.max(1.0F, height / 6.0F);
      int columns = (int)Math.ceil(width / scale);
      double seconds = System.nanoTime() / 1.0E9;

      for (int row = 0; row < 6; row++) {
         for (int column = 0; column < columns; column++) {
            int u = column;
            int v = row;
            PixelPortalSurface.drawNativeSurface(true, column, 80 + row, true, seconds, (x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color) -> {
               float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
               float maxX = Math.max(Math.max(x0, x1), Math.max(x2, x3));
               float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
               float maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));
               int left = Math.max(0, Math.round((u + minX) * scale));
               int right = Math.min(width, Math.round((u + maxX) * scale));
               int top = Math.max(0, Math.round(height - (v + maxY) * scale));
               int bottom = Math.min(height, Math.round(height - (v + minY) * scale));
               if (right > left && bottom > top) {
                  graphics.fill(left, top, right, bottom, color);
               }
            });
         }
      }

      graphics.fill(0, 0, width, height, 1694498816);
   }
}
