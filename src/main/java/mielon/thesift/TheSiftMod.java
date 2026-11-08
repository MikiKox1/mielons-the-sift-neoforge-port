package mielon.thesift;

import mielon.thesift.advancement.ModAdvancements;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPlantInteractions;
import mielon.thesift.entity.BlubEntity;
import mielon.thesift.entity.DarkSnifferEntity;
import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.ModAttachments;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.MonarchEntity;
import mielon.thesift.entity.NesterEntity;
import mielon.thesift.entity.NuzzleEntity;
import mielon.thesift.entity.PollinatorEntity;
import mielon.thesift.entity.ScavengerEntity;
import mielon.thesift.entity.SeedlingEntity;
import mielon.thesift.entity.SingerEntity;
import mielon.thesift.entity.SingerSummoner;
import mielon.thesift.entity.SlabberEntity;
import mielon.thesift.entity.StalkerEntity;
import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.item.BlubItems;
import mielon.thesift.item.ModFuels;
import mielon.thesift.item.ModItems;
import mielon.thesift.loot.AncientCitySoulshaleLoot;
import mielon.thesift.map.SiftExplorationMaps;
import mielon.thesift.network.MapColorsPayload;
import mielon.thesift.network.MonarchFlightPayload;
import mielon.thesift.network.RiftLoadingPayload;
import mielon.thesift.network.SiftIntroReadyPayload;
import mielon.thesift.particle.ModParticles;
import mielon.thesift.platform.ModRegistration;
import mielon.thesift.platform.NeoEvents;
import mielon.thesift.platform.NeoEvents.ServerLifecycleEvents;
import mielon.thesift.platform.NeoEvents.ServerLifecycleEvents.ServerStopped;
import mielon.thesift.platform.NeoEvents.ServerTickEvents;
import mielon.thesift.platform.NeoEvents.ServerTickEvents.EndTick;
import mielon.thesift.portal.SonorousAutoplay;
import mielon.thesift.portal.SonorousConsoles;
import mielon.thesift.portal.SonorousIgnition;
import mielon.thesift.sound.ModSounds;
import mielon.thesift.world.RiftDirectory;
import mielon.thesift.world.RiftManager;
import mielon.thesift.world.SiftLocateCommand;
import mielon.thesift.world.SiftPortalStructure;
import mielon.thesift.world.SiftTeleportManager;
import mielon.thesift.world.SiftTideResumeMigration;
import mielon.thesift.world.SiftWeatherCommand;
import mielon.thesift.world.SiftiteRecoveryManager;
import mielon.thesift.worldgen.ModWorldgen;
import mielon.thesift.worldgen.SiftFeaturePlacementGuard;
import mielon.thesift.worldgen.SiftLandmarkTracker;
import mielon.thesift.worldgen.SiftTerrainFeatureMemory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.RegisterCauldronFluidContentEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TheSiftMod.MOD_ID)
public final class TheSiftMod {
   public static final String MOD_ID = "the_sift";
   public static final Logger LOGGER = LoggerFactory.getLogger("the_sift");

   public TheSiftMod(IEventBus modBus) {
      NeoEvents.bootstrap();
      // Map/loot codecs do not construct blocks and can be queued before events.
      SiftExplorationMaps.initialize();
      AncientCitySoulshaleLoot.register();
      ModFuels.register();
      modBus.addListener(this::registerContent);
      modBus.addListener(this::commonSetup);
      modBus.addListener(TheSiftMod::registerEntityAttributes);
      modBus.addListener(TheSiftMod::registerSpawnPlacements);
      modBus.addListener(TheSiftMod::modifyDefaultComponents);
      modBus.addListener(TheSiftMod::addVanillaBlockEntityBlocks);
      modBus.addListener(TheSiftMod::registerPayloads);
      modBus.addListener(TheSiftMod::registerCauldronFluidContent);
      modBus.addListener(TheSiftMod::registerCapabilities);
   }

   private void registerContent(RegisterEvent event) {
      if (event.getRegistryKey().equals(BuiltInRegistries.GAME_RULE.key())) {
         RiftManager.registerGameRules();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.PARTICLE_TYPE.key())) {
         ModParticles.initialize();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.SOUND_EVENT.key())) {
         ModSounds.initialize();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.FLUID.key())) {
         ModFluids.initialize();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.BLOCK.key())) {
         ModBlocks.bootstrap();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.ENTITY_TYPE.key())) {
         ModEntities.initialize();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.ITEM.key())) {
         ModItems.bootstrap();
         BlubItems.initialize();
      } else if (event.getRegistryKey().equals(BuiltInRegistries.FEATURE.key())) {
         // Features capture block objects. NeoForge now has unlocked registries
         // and registered blocks; structure types/processors follow FEATURE.
         ModWorldgen.initialize();
      } else if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)) {
         ModAttachments.initialize();
      }

      ModRegistration.onRegister(event);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(this::onInitialize);
   }

   private void onInitialize() {
      ModBlocks.initialize();
      ModFluids.initialize();
      ModFluids.registerNeoForgeInteractions();
      SiftPlantInteractions.register();
      SiftWeatherCommand.register();
      ModItems.initialize();
      ModAdvancements.register();
      SiftiteRecoveryManager.register();
      ServerTickEvents.START_SERVER_TICK.register(RiftDirectory::tick);
      SonorousIgnition.register();
      SingerSummoner.register();
      SonorousAutoplay.register();
      registerServerLifecycle();
      LOGGER.info("The Sift initialized.");
   }

   private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
      event.put(ModEntities.SINGER, SingerEntity.createAttributes().build());
      event.put(ModEntities.ECHO_GOLEM, EchoGolemEntity.createAttributes().build());
      event.put(ModEntities.DARK_SNIFFER, DarkSnifferEntity.createAttributes().build());
      event.put(ModEntities.BLUB, BlubEntity.createAttributes().build());
      event.put(ModEntities.SEEDLING, SeedlingEntity.createAttributes().build());
      event.put(ModEntities.NESTER, NesterEntity.createAttributes().build());
      event.put(ModEntities.SLABBER, SlabberEntity.createAttributes().build());
      event.put(ModEntities.NUZZLE, NuzzleEntity.createAttributes().build());
      event.put(ModEntities.STALKER, StalkerEntity.createAttributes().build());
      event.put(ModEntities.SCAVENGER, ScavengerEntity.createAttributes().build());
      event.put(ModEntities.MONARCH, MonarchEntity.createAttributes().build());
      event.put(ModEntities.MONARCH_ECHO, MonarchEntity.createEchoAttributes().build());
      event.put(ModEntities.POLLINATOR, PollinatorEntity.createAttributes().build());
   }

   private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
      // The Sift uses natural vanilla Sniffer spawning; NeoForge requires its predicate.
      event.register(
         EntityType.SNIFFER, SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, Operation.REPLACE
      );
      event.register(
         ModEntities.ECHO_GOLEM,
         SpawnPlacementTypes.ON_GROUND,
         Types.MOTION_BLOCKING_NO_LEAVES,
         EchoGolemEntity::checkEchoGolemSpawnRules,
         Operation.REPLACE
      );
      event.register(ModEntities.DARK_SNIFFER, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, DarkSnifferEntity::checkDarkSnifferSpawnRules, Operation.REPLACE);
      event.register(ModEntities.BLUB, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, BlubEntity::checkBlubSpawnRules, Operation.REPLACE);
      event.register(ModEntities.SEEDLING, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, SeedlingEntity::checkSeedlingSpawnRules, Operation.REPLACE);
      event.register(ModEntities.NESTER, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, NesterEntity::checkNesterSpawnRules, Operation.REPLACE);
      event.register(ModEntities.SLABBER, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, SlabberEntity::checkSlabberSpawnRules, Operation.REPLACE);
      event.register(ModEntities.NUZZLE, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, NuzzleEntity::checkNuzzleSpawnRules, Operation.REPLACE);
      event.register(ModEntities.STALKER, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, StalkerEntity::checkStalkerSpawnRules, Operation.REPLACE);
      event.register(ModEntities.SCAVENGER, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, ScavengerEntity::checkScavengerSpawnRules, Operation.REPLACE);
      event.register(ModEntities.POLLINATOR, SpawnPlacementTypes.ON_GROUND, Types.MOTION_BLOCKING_NO_LEAVES, PollinatorEntity::checkPollinatorSpawnRules, Operation.REPLACE);
   }

   private static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
      ModBlocks.modifyDefaultComponents(event);
      ModItems.modifyDefaultComponents(event);
   }

   private static void addVanillaBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
      ModBlocks.addVanillaBlockEntityBlocks(event);
   }

   private static void registerPayloads(RegisterPayloadHandlersEvent event) {
      RiftLoadingPayload.register(event);
      SiftIntroReadyPayload.register(event);
      MapColorsPayload.register(event);
      MonarchFlightPayload.register(event);
   }

   private static void registerCauldronFluidContent(RegisterCauldronFluidContentEvent event) {
      ModFluids.registerCauldronFluidContent(event);
   }

   private static void registerCapabilities(RegisterCapabilitiesEvent event) {
      ModFluids.registerCapabilities(event);
   }

   private static void registerServerLifecycle() {
      ServerLifecycleEvents.SERVER_STARTED.register(SiftPortalStructure::prepare);
      ServerLifecycleEvents.SERVER_STARTED.register(SiftTideResumeMigration::resumeRequestedClock);
      ServerTickEvents.END_SERVER_TICK.register((EndTick)server -> {
         ModAdvancements.tick(server);
         SiftTeleportManager.tick(server);
         RiftManager.tick(server);
         SiftiteRecoveryManager.tick(server);
         IchorWaterlogging.tick(server);
         SiftTerrainFeatureMemory.tick(server);
      });
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> {
         SonorousConsoles.clearTransientBuffers();
         SiftTeleportManager.clearTransientState();
         RiftManager.clearTransientState();
         SiftiteRecoveryManager.clearTransientState();
         SiftFeaturePlacementGuard.clear();
         SiftLocateCommand.clearPendingSearches();
         EchoGolemEntity.clearTransientState();
         IchorWaterlogging.clear();
      });
      ServerLifecycleEvents.SERVER_STOPPING.register(SiftTerrainFeatureMemory::flush);
      ServerLifecycleEvents.SERVER_STOPPING.register(SiftLandmarkTracker::flush);
      ServerLifecycleEvents.SERVER_STOPPING.register(IchorWaterlogging::flush);
   }
}
