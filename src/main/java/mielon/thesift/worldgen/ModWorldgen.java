package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class ModWorldgen {
   public static final StructureType<SculkersLairStructure> SCULKERS_LAIR = ModRegistration.register(
      BuiltInRegistries.STRUCTURE_TYPE, Identifier.fromNamespaceAndPath("the_sift", "sculkers_lair"), () -> SculkersLairStructure.CODEC
   );
   public static final MapCodec<SculkersLairProcessor> SCULKERS_LAIR_PROCESSOR = ModRegistration.register(
      BuiltInRegistries.STRUCTURE_PROCESSOR, Identifier.fromNamespaceAndPath("the_sift", "sculkers_lair"), SculkersLairProcessor.CODEC
   );
   public static final MapCodec<CampCoralVinesProcessor> CAMP_CORAL_VINES = ModRegistration.register(
      BuiltInRegistries.STRUCTURE_PROCESSOR, Identifier.fromNamespaceAndPath("the_sift", "camp_coral_vines"), CampCoralVinesProcessor.CODEC
   );
   public static final Feature<NoneFeatureConfiguration> SOULSHALE_TOWER = register("soulshale_tower", new SoulshaleTowerFeature());
   public static final Feature<NoneFeatureConfiguration> SOULSHALE_SHELTER = register("soulshale_shelter", new SoulshaleShelterFeature());
   public static final Feature<NoneFeatureConfiguration> ORGANIC_DESERT_SURFACE = register("organic_desert_surface", new OrganicDesertFeature(0));
   public static final Feature<NoneFeatureConfiguration> ORGANIC_DESERT_CHANNELS = register("organic_desert_channels", new OrganicDesertFeature(1));
   public static final Feature<NoneFeatureConfiguration> ORGANIC_DESERT_FINISH = register("organic_desert_finish", new OrganicDesertFeature(2));
   public static final Feature<NoneFeatureConfiguration> ORGANIC_RIB_FOSSIL = register("organic_rib_fossil", new OrganicRibFossilFeature());
   public static final Feature<NoneFeatureConfiguration> ASH_WILLOW_TREE = register("ash_willow_tree", new AshWillowTreeFeature());
   public static final Feature<NoneFeatureConfiguration> CORAL_SURFACE = register("coral_surface", new CoralSurfaceFeature(false));
   public static final Feature<NoneFeatureConfiguration> CORAL_VEGETATION = register("coral_vegetation", new CoralSurfaceFeature(true));
   public static final Feature<NoneFeatureConfiguration> CAVE_ICHOR_LAKE = register("cave_ichor_lake", new SiftCaveIchorLakeFeature());
   public static final Feature<NoneFeatureConfiguration> CAVE_VEGETATION = register("cave_vegetation", new SiftCaveVegetationFeature());
   public static final Feature<NoneFeatureConfiguration> CAVE_GEOLOGY = register("cave_geology", new SiftCaveGeologyFeature());
   public static final Feature<NoneFeatureConfiguration> CAVE_WILLOW = register("cave_willow", new SiftCaveWillowFeature());
   public static final Feature<NoneFeatureConfiguration> ICHOR_RAVINE_FINISH = register("ichor_ravine_finish", new SiftIchorRavineFinishFeature());
   public static final Feature<NoneFeatureConfiguration> CAVE_ENTRANCE = register("cave_entrance", new SiftCaveEntranceFeature());
   public static final Feature<NoneFeatureConfiguration> STRATA_SUPPORTS = register("strata_supports", new SiftStrataSupportFeature());
   public static final Feature<NoneFeatureConfiguration> STRATA_RIMS = register("strata_rims", new SiftStrataRimFeature());
   public static final Feature<NoneFeatureConfiguration> STRATA_UNDERGROWTH = register("strata_undergrowth", new SiftStrataUndergrowthFeature());
   public static final Feature<NoneFeatureConfiguration> ICHOR_TERRACES = register("ichor_terraces", new SiftIchorTerracesFeature());
   public static final Feature<NoneFeatureConfiguration> LUSH_MONOLITH = register("lush_monolith", new SiftMonolithFeature(true));
   public static final Feature<NoneFeatureConfiguration> BARE_MONOLITH = register("bare_monolith", new SiftMonolithFeature(false));
   public static final Feature<NoneFeatureConfiguration> CLIFF_ARCH = register("cliff_arch", new SiftArchFeature());
   public static final Feature<NoneFeatureConfiguration> DRY_SCULK_SPIKES = register("dry_sculk_spikes", new SiftDrySpikeFeature());
   public static final Feature<NoneFeatureConfiguration> OVERGROWN_WILLOW_TREE = register("overgrown_willow_tree", new OvergrownWillowTreeFeature());
   public static final Feature<NoneFeatureConfiguration> COVERED_GROWTH_CLEANUP = register("covered_growth_cleanup", new CoveredGrowthCleanupFeature());
   public static final Feature<NoneFeatureConfiguration> ABANDONED_MAIN_PORTAL = register("abandoned_main_portal", new AbandonedMainPortalFeature());
   public static final Feature<NoneFeatureConfiguration> SNIFFER_CAVE_OVERGROWN = register(
      "sniffer_cave_overgrown", new SnifferCaveFeature(12, 6002815919507592773L, 8)
   );
   public static final Feature<NoneFeatureConfiguration> SNIFFER_CAVE_WASTES = register(
      "sniffer_cave_wastes", new SnifferCaveFeature(20, 6002815919508111699L, 5)
   );
   public static final Feature<NoneFeatureConfiguration> SNIFFER_PLANT_PATCH = register("sniffer_plant_patch", new SnifferPlantPatchFeature());
   public static final Feature<NoneFeatureConfiguration> SIFT_FLOWER_PATCH = register("sift_flower_patch", new SiftFlowerPatchFeature());
   public static final Feature<NoneFeatureConfiguration> SAUCER_BLOOM_PATCH = register("saucer_bloom_patch", new SiftFlowerPatchFeature(true));
   public static final Feature<NoneFeatureConfiguration> SIFTSLATE_PLANT_PATCH = register("siftslate_plant_patch", new SiftslatePlantPatchFeature());
   public static final Feature<NoneFeatureConfiguration> SOUL_CANYON = register("soul_canyon", new SoulCanyonFeature());
   public static final Feature<NoneFeatureConfiguration> ICHOR_LAKE = register("ichor_lake", new IchorLakeFeature());
   public static final Feature<NoneFeatureConfiguration> LAVA_FLOOR = register("lava_floor", new SiftLavaFloorFeature());
   public static final Feature<NoneFeatureConfiguration> SURFACE_SCULK_REGION = register("surface_sculk_region", new SiftSurfaceSculkRegionFeature());
   public static final Feature<NoneFeatureConfiguration> ICHOR_SNOW = register("ichor_snow", new SiftIchorSnowFeature());
   public static final Feature<SpringConfiguration> ICHOR_CAVE_SPRING = register("ichor_cave_spring", new SiftIchorCaveSpringFeature());

   private ModWorldgen() {
   }

   private static <C extends FeatureConfiguration> Feature<C> register(String name, Feature<C> feature) {
      return ModRegistration.register(BuiltInRegistries.FEATURE, Identifier.fromNamespaceAndPath("the_sift", name), feature);
   }

   public static void initialize() {
   }
}
