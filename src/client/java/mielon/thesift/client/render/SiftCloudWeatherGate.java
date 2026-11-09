package mielon.thesift.client.render;

import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

/** Apply rain-only clouds before vanilla schedules a depth or OIT cloud pass. */
public final class SiftCloudWeatherGate {
   private SiftCloudWeatherGate() {
   }

   public static void onExtract(ExtractLevelRenderStateEvent event) {
      ClientLevel level = event.getLevel();
      if (level == null || !level.dimension().equals(TheSiftDimension.LEVEL_KEY) || SiftShaderCompat.isShaderPackInUse()) {
         return;
      }

      event.getRenderState().cloudColor = SiftCloudOpacity.rainOnly(
         event.getRenderState().cloudColor, level.getRainLevel(1.0F), SiftTides.nightBlend(level.getDefaultClockTime())
      );
   }
}
