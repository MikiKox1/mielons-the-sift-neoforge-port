package mielon.thesift.world;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class TheSiftDimension {
   public static final Identifier ID = Identifier.fromNamespaceAndPath("the_sift", "the_sift");
   public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, ID);
   public static final ResourceKey<Biome> SIFT_WASTES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "sift_wastes"));
   public static final ResourceKey<Biome> OVERGROWN_CLEARING = ResourceKey.create(
      Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_clearing")
   );
   public static final ResourceKey<Biome> OVERGROWN_FOREST = ResourceKey.create(
      Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_forest")
   );
   public static final ResourceKey<Biome> ICHOR_SNOWY_PEAKS = ResourceKey.create(
      Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "ichor_snowy_peaks")
   );
   public static final ResourceKey<Biome> SIFTSLATE_CAVES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "siftslate_caves"));
   public static final ResourceKey<Biome> SOULSHALE_CAVES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "soulshale_caves"));
   public static final ResourceKey<Biome> OVERGROWN_CAVES = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "overgrown_caves"));
   public static final ResourceKey<Biome> CORAL_MEADOW = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "coral_meadow"));
   public static final ResourceKey<Biome> CORAL_FOREST = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "coral_forest"));
   public static final ResourceKey<Biome> ORGANIC_DESERT = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "organic_desert"));
   public static final ResourceKey<Biome> CORRUPTED_SIFT = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("the_sift", "corrupted_sift"));

   public static boolean isOrganicDesert(Holder<Biome> biome) {
      return biome.is(ORGANIC_DESERT);
   }

   public static boolean isGrimSurfaceBiome(Holder<Biome> biome) {
      return isCoralBiome(biome) || isOrganicDesert(biome);
   }

   public static boolean isCoralBiome(Holder<Biome> biome) {
      return biome.is(CORAL_MEADOW) || biome.is(CORAL_FOREST);
   }

   public static boolean isOvergrownBiome(Holder<Biome> biome) {
      return biome.is(OVERGROWN_CAVES) || biome.is(OVERGROWN_CLEARING) || biome.is(OVERGROWN_FOREST);
   }

   public static boolean isCaveBiome(Holder<Biome> biome) {
      return biome.is(SIFTSLATE_CAVES) || biome.is(SOULSHALE_CAVES) || biome.is(OVERGROWN_CAVES) || biome.is(CORRUPTED_SIFT);
   }

   private TheSiftDimension() {
   }
}
