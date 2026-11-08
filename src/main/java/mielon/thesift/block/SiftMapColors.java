package mielon.thesift.block;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import mielon.thesift.map.SiftMapColorConstruction;
import mielon.thesift.mixin.MapColorInvoker;
import net.minecraft.world.level.material.MapColor;

public final class SiftMapColors {
   private static final Set<MapColor> EXTENDED = Collections.newSetFromMap(new IdentityHashMap<>());
   public static final MapColor CORAL = register(62, 16160930, MapColor.COLOR_PINK);
   public static final MapColor SOULSHALE = register(63, 2974278, MapColor.WARPED_WART_BLOCK);
   public static final MapColor SIFTSLATE = custom(11155255, MapColor.COLOR_RED);
   public static final MapColor SOULSHALE_GROWTH = custom(13053229, MapColor.CRIMSON_NYLIUM);
   public static final MapColor HEALTHY_SCULK = custom(13912907, MapColor.CRIMSON_NYLIUM);
   public static final MapColor CORAL_SOIL = custom(11553110, MapColor.TERRACOTTA_PINK);
   public static final MapColor CORAL_GROWTH = custom(15559800, MapColor.COLOR_PINK);
   public static final MapColor CARTILAGE = custom(15063738, MapColor.QUARTZ);
   public static final MapColor PORES = custom(13618504, MapColor.COLOR_YELLOW);
   public static final MapColor DRY_SCULK = custom(16295289, MapColor.RAW_IRON);
   public static final MapColor OVERGROWN = custom(7653541, MapColor.GLOW_LICHEN);
   public static final MapColor OVERGROWN_PLANTS = custom(7062436, MapColor.GLOW_LICHEN);
   public static final MapColor WILLOW_PLANKS = custom(8900501, MapColor.GLOW_LICHEN);
   public static final MapColor WILLOW_LOG_END = custom(6396531, MapColor.GLOW_LICHEN);
   public static final MapColor WILLOW_BARK = custom(922143, MapColor.COLOR_BLACK);
   public static final MapColor WILLOW_VINES = custom(8045233, MapColor.GLOW_LICHEN);
   public static final MapColor WILLOW_FOLIAGE = custom(9104621, MapColor.DIAMOND);
   public static final MapColor ASH_FOLIAGE = custom(11388363, MapColor.CLAY);
   public static final MapColor ASH_VINES = custom(9939117, MapColor.CLAY);
   public static final MapColor ICHOR_SNOW = custom(14794205, MapColor.WOOL);
   public static final MapColor SOUL = custom(11399903, MapColor.DIAMOND);
   public static final MapColor GREEN_BLOOM = custom(8641434, MapColor.GLOW_LICHEN);
   public static final MapColor WHISPERBLOOM = custom(16443069, MapColor.SAND);
   public static final MapColor DARK_SCULK = custom(1189162, MapColor.COLOR_BLACK);

   private SiftMapColors() {
   }

   public static boolean isExtended(MapColor color) {
      return EXTENDED.contains(color);
   }

   private static MapColor custom(int rgb, MapColor fallback) {
      MapColor color = SiftMapColorConstruction.create(rgb, fallback);
      EXTENDED.add(color);
      return color;
   }

   private static MapColor register(int id, int rgb, MapColor fallback) {
      MapColor existing = MapColor.byId(id);
      if (existing != MapColor.NONE) {
         return existing.col == rgb ? existing : fallback;
      } else {
         return MapColorInvoker.theSift$create(id, rgb);
      }
   }
}
