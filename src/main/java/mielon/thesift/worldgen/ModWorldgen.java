package mielon.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
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
   public static final MapCodec<SoulshaleTowerFeature> SOULSHALE_TOWER = register("soulshale_tower", SoulshaleTowerFeature.CODEC);
   public static final MapCodec<SoulshaleShelterFeature> SOULSHALE_SHELTER = register("soulshale_shelter", SoulshaleShelterFeature.CODEC);
   public static final MapCodec<OrganicDesertFeature> ORGANIC_DESERT_SURFACE = register("organic_desert_surface", OrganicDesertFeature.SURFACE_CODEC);
   public static final MapCodec<OrganicDesertFeature> ORGANIC_DESERT_CHANNELS = register("organic_desert_channels", OrganicDesertFeature.CHANNELS_CODEC);
   public static final MapCodec<OrganicDesertFeature> ORGANIC_DESERT_FINISH = register("organic_desert_finish", OrganicDesertFeature.FINISH_CODEC);
   public static final MapCodec<OrganicRibFossilFeature> ORGANIC_RIB_FOSSIL = register("organic_rib_fossil", OrganicRibFossilFeature.CODEC);
   public static final MapCodec<AshWillowTreeFeature> ASH_WILLOW_TREE = register("ash_willow_tree", AshWillowTreeFeature.CODEC);
   public static final MapCodec<CoralSurfaceFeature> CORAL_SURFACE = register("coral_surface", CoralSurfaceFeature.SURFACE_CODEC);
   public static final MapCodec<CoralSurfaceFeature> CORAL_VEGETATION = register("coral_vegetation", CoralSurfaceFeature.VEGETATION_CODEC);
   public static final MapCodec<SiftCaveIchorLakeFeature> CAVE_ICHOR_LAKE = register("cave_ichor_lake", SiftCaveIchorLakeFeature.CODEC);
   public static final MapCodec<SiftCaveVegetationFeature> CAVE_VEGETATION = register("cave_vegetation", SiftCaveVegetationFeature.CODEC);
   public static final MapCodec<SiftCaveGeologyFeature> CAVE_GEOLOGY = register("cave_geology", SiftCaveGeologyFeature.CODEC);
   public static final MapCodec<SiftCaveWillowFeature> CAVE_WILLOW = register("cave_willow", SiftCaveWillowFeature.CODEC);
   public static final MapCodec<SiftIchorRavineFinishFeature> ICHOR_RAVINE_FINISH = register("ichor_ravine_finish", SiftIchorRavineFinishFeature.CODEC);
   public static final MapCodec<SiftCaveEntranceFeature> CAVE_ENTRANCE = register("cave_entrance", SiftCaveEntranceFeature.CODEC);
   public static final MapCodec<SiftStrataSupportFeature> STRATA_SUPPORTS = register("strata_supports", SiftStrataSupportFeature.CODEC);
   public static final MapCodec<SiftStrataRimFeature> STRATA_RIMS = register("strata_rims", SiftStrataRimFeature.CODEC);
   public static final MapCodec<SiftStrataUndergrowthFeature> STRATA_UNDERGROWTH = register("strata_undergrowth", SiftStrataUndergrowthFeature.CODEC);
   public static final MapCodec<SiftIchorTerracesFeature> ICHOR_TERRACES = register("ichor_terraces", SiftIchorTerracesFeature.CODEC);
   public static final MapCodec<SiftMonolithFeature> LUSH_MONOLITH = register("lush_monolith", SiftMonolithFeature.LUSH_CODEC);
   public static final MapCodec<SiftMonolithFeature> BARE_MONOLITH = register("bare_monolith", SiftMonolithFeature.BARE_CODEC);
   public static final MapCodec<SiftArchFeature> CLIFF_ARCH = register("cliff_arch", SiftArchFeature.CODEC);
   public static final MapCodec<SiftDrySpikeFeature> DRY_SCULK_SPIKES = register("dry_sculk_spikes", SiftDrySpikeFeature.CODEC);
   public static final MapCodec<OvergrownWillowTreeFeature> OVERGROWN_WILLOW_TREE = register("overgrown_willow_tree", OvergrownWillowTreeFeature.CODEC);
   public static final MapCodec<CoveredGrowthCleanupFeature> COVERED_GROWTH_CLEANUP = register("covered_growth_cleanup", CoveredGrowthCleanupFeature.CODEC);
   public static final MapCodec<AbandonedMainPortalFeature> ABANDONED_MAIN_PORTAL = register("abandoned_main_portal", AbandonedMainPortalFeature.CODEC);
   public static final MapCodec<SnifferCaveFeature> SNIFFER_CAVE_OVERGROWN = register("sniffer_cave_overgrown", SnifferCaveFeature.OVERGROWN_CODEC);
   public static final MapCodec<SnifferCaveFeature> SNIFFER_CAVE_WASTES = register("sniffer_cave_wastes", SnifferCaveFeature.WASTES_CODEC);
   public static final MapCodec<SnifferPlantPatchFeature> SNIFFER_PLANT_PATCH = register("sniffer_plant_patch", SnifferPlantPatchFeature.CODEC);
   public static final MapCodec<SiftFlowerPatchFeature> SIFT_FLOWER_PATCH = register("sift_flower_patch", SiftFlowerPatchFeature.CODEC);
   public static final MapCodec<SiftFlowerPatchFeature> SAUCER_BLOOM_PATCH = register("saucer_bloom_patch", SiftFlowerPatchFeature.SAUCER_BLOOM_CODEC);
   public static final MapCodec<SiftslatePlantPatchFeature> SIFTSLATE_PLANT_PATCH = register("siftslate_plant_patch", SiftslatePlantPatchFeature.CODEC);
   public static final MapCodec<SoulCanyonFeature> SOUL_CANYON = register("soul_canyon", SoulCanyonFeature.CODEC);
   public static final MapCodec<IchorLakeFeature> ICHOR_LAKE = register("ichor_lake", IchorLakeFeature.CODEC);
   public static final MapCodec<SiftLavaFloorFeature> LAVA_FLOOR = register("lava_floor", SiftLavaFloorFeature.CODEC);
   public static final MapCodec<SiftSurfaceSculkRegionFeature> SURFACE_SCULK_REGION = register("surface_sculk_region", SiftSurfaceSculkRegionFeature.CODEC);
   public static final MapCodec<SiftIchorSnowFeature> ICHOR_SNOW = register("ichor_snow", SiftIchorSnowFeature.CODEC);
   public static final MapCodec<SiftIchorCaveSpringFeature> ICHOR_CAVE_SPRING = register("ichor_cave_spring", SiftIchorCaveSpringFeature.CODEC);

   private ModWorldgen() {
   }

   private static <F extends Feature> MapCodec<F> register(String name, MapCodec<F> codec) {
      return ModRegistration.register(BuiltInRegistries.FEATURE_TYPE, Identifier.fromNamespaceAndPath("the_sift", name), codec);
   }

   public static void initialize() {
   }
}
