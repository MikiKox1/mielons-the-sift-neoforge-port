package mielon.thesift.world;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;

public final class SiftPortalVariants {
   private static final Set<String> BIOMES = Set.of(
      "sift_wastes",
      "overgrown_clearing",
      "overgrown_forest",
      "ichor_snowy_peaks",
      "siftslate_caves",
      "overgrown_caves",
      "soulshale_caves",
      "organic_desert",
      "coral_meadow",
      "coral_forest",
      "corrupted_sift"
   );
   private static final int[] ABANDONED_VARIANTS = new int[]{1, 3, 4, 7, 8, 9, 10};

   public static int abandonedCount(String biome) {
      return ABANDONED_VARIANTS.length;
   }

   private SiftPortalVariants() {
   }

   public static String biome(String path) {
      return BIOMES.contains(path) ? path : "sift_wastes";
   }

   public static Identifier main(String biome) {
      return id("main_portal_" + biome(biome));
   }

   public static Identifier abandoned(String biome, int variant) {
      return id("abandoned_portal_" + biome(biome) + "_" + ABANDONED_VARIANTS[Math.floorMod(variant, ABANDONED_VARIANTS.length)]);
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   public static List<String> abandonedBiomes() {
      return List.of(
         "sift_wastes",
         "overgrown_clearing",
         "overgrown_forest",
         "ichor_snowy_peaks",
         "siftslate_caves",
         "overgrown_caves",
         "soulshale_caves",
         "organic_desert",
         "coral_meadow",
         "coral_forest",
         "corrupted_sift"
      );
   }

   public static SiftLandmarkTracker.Kind abandonedKind(String biome) {
      return SiftLandmarkTracker.Kind.valueOf("ABANDONED_PORTAL_" + biome(biome).toUpperCase(Locale.ROOT));
   }

   public static String abandonedLocateId(String biome) {
      return "the_sift:abandoned_main_portal_" + biome(biome);
   }

   public static String abandonedLocateBiome(String id) {
      String prefix = "the_sift:abandoned_main_portal_";
      if (!id.startsWith(prefix)) {
         return null;
      }

      String biome = id.substring(prefix.length());
      return BIOMES.contains(biome) ? biome : null;
   }

   public static String abandonedTemplateBiome(String path) {
      return biome(path.substring("abandoned_portal_".length(), path.lastIndexOf(95)));
   }

   public static void indexLegacyAbandoned(ServerLevel level) {
      RandomState random = level.getChunkSource().randomState();
      BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
      BiomeResolver resolver = source.createUncachedResolver(random);

      for (BlockPos pos : SiftLandmarkTracker.snapshot(SiftLandmarkTracker.Kind.ABANDONED_MAIN_PORTAL)) {
         boolean indexed = false;

         for (String biome : BIOMES) {
            if (SiftLandmarkTracker.hasRecorded(abandonedKind(biome), pos)) {
               indexed = true;
               break;
            }
         }

         if (!indexed) {
            Holder<Biome> biome = resolver.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
            String path = biome.unwrapKey().orElseThrow().identifier().getPath();
            SiftLandmarkTracker.record(abandonedKind(path), pos);
         }
      }
   }
}
