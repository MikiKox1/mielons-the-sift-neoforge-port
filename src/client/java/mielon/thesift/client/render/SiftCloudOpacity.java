package mielon.thesift.client.render;

/** Original rain-only cloud appearance, independent of shaderpack rendering. */
public final class SiftCloudOpacity {
   private SiftCloudOpacity() {
   }

   public static int rainOnly(int originalColor, float rain, float night) {
      float wet = Math.max(0.0F, Math.min(1.0F, rain));
      float dark = Math.max(0.0F, Math.min(1.0F, night));
      int alpha = Math.round((originalColor >>> 24 & 0xFF) * wet * 0.66F);
      int brightness = Math.round(255.0F * (1.0F - dark * 0.24F));
      return alpha << 24 | brightness << 16 | brightness << 8 | brightness;
   }
}
