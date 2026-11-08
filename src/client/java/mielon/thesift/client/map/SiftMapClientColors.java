package mielon.thesift.client.map;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import mielon.thesift.TheSiftMod;
import mielon.thesift.map.SiftMapEncoding;
import mielon.thesift.network.MapColorsPayload;

public final class SiftMapClientColors {
   private static final Map<Integer, int[]> MAPS = new HashMap<>();

   private SiftMapClientColors() {
   }

   public static void accept(MapColorsPayload payload) {
      try {
         int[] colors = SiftMapEncoding.decode(payload.colors());
         if (colors.length == 0) {
            MAPS.remove(payload.mapId());
         } else {
            MAPS.put(payload.mapId(), colors);
         }
      } catch (IOException failure) {
         TheSiftMod.LOGGER.warn("Ignoring invalid map color data for map {}", payload.mapId(), failure);
      }
   }

   public static int get(int id, int x, int y, int fallback) {
      int[] pixels = MAPS.get(id);
      if (pixels == null) {
         return fallback;
      }

      int color = pixels[x + y * 128];
      return color == 0 ? fallback : color;
   }

   public static void clear() {
      MAPS.clear();
   }
}
