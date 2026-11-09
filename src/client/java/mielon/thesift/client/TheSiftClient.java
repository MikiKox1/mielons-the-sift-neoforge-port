package mielon.thesift.client;

import mielon.thesift.block.ModBlocks;
import mielon.thesift.client.armor.SifterCapRenderer;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.entity.BlubRenderer;
import mielon.thesift.client.entity.DarkSnifferRenderer;
import mielon.thesift.client.entity.EchoGolemRenderer;
import mielon.thesift.client.entity.MonarchProjectileRenderer;
import mielon.thesift.client.entity.MonarchRenderer;
import mielon.thesift.client.entity.NesterRenderer;
import mielon.thesift.client.entity.NuzzleRenderer;
import mielon.thesift.client.entity.PollinatorProjectileRenderer;
import mielon.thesift.client.entity.PollinatorRenderer;
import mielon.thesift.client.entity.ScavengerProjectileRenderer;
import mielon.thesift.client.entity.ScavengerRenderer;
import mielon.thesift.client.entity.SeedlingRenderer;
import mielon.thesift.client.entity.SingerRenderer;
import mielon.thesift.client.entity.SlabberRenderer;
import mielon.thesift.client.entity.StalkerRenderer;
import mielon.thesift.client.particle.IchorFluidParticle;
import mielon.thesift.client.particle.IchorSoulParticle;
import mielon.thesift.client.particle.IchorSurfaceParticle;
import mielon.thesift.client.particle.PollinatorDroolParticle;
import mielon.thesift.client.particle.SiftNoteParticle;
import mielon.thesift.client.particle.SiftParallaxParticle;
import mielon.thesift.client.particle.SingerSoundWaveParticle;
import mielon.thesift.client.particle.SoulParticle;
import mielon.thesift.client.particle.SoundWaveParticle;
import mielon.thesift.client.render.IchorCauldronRenderer;
import mielon.thesift.client.render.IchorFluidRendering;
import mielon.thesift.client.render.SiftCloudRenderPipelines;
import mielon.thesift.client.render.SiftCloudWeatherGate;
import mielon.thesift.client.render.SiftPortalRenderer;
import mielon.thesift.client.render.SiftPortalRenderTypes;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.client.render.SonorousBeamRenderTypes;
import mielon.thesift.client.render.SonorousBeamRenderer;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.item.SifterCapItem;
import mielon.thesift.particle.ModParticles;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class TheSiftClient {
   private static final ModelLayerLocation ASH_WILLOW_BOAT_LAYER = new ModelLayerLocation(
      Identifier.fromNamespaceAndPath("the_sift", "boat/ash_willow"), "main"
   );
   private static final ModelLayerLocation ASH_WILLOW_CHEST_BOAT_LAYER = new ModelLayerLocation(
      Identifier.fromNamespaceAndPath("the_sift", "chest_boat/ash_willow"), "main"
   );
   private static final ModelLayerLocation OVERGROWN_WILLOW_BOAT_LAYER = new ModelLayerLocation(
      Identifier.fromNamespaceAndPath("the_sift", "boat/overgrown_willow"), "main"
   );
   private static final ModelLayerLocation OVERGROWN_WILLOW_CHEST_BOAT_LAYER = new ModelLayerLocation(
      Identifier.fromNamespaceAndPath("the_sift", "chest_boat/overgrown_willow"), "main"
   );

   private TheSiftClient() {
   }

   public static void initialize() {
      SifterCapItem.installRenderer(SifterCapRenderer::provider);
      SiftShaderCompat.initialize();
      SiftPortalRenderTypes.initialize();
      SonorousBeamRenderTypes.initialize();
      SiftProceduralSkyRenderer.initialize();
      SiftCloudRenderPipelines.initialize();
      NeoForge.EVENT_BUS.addListener(SiftCloudWeatherGate::onExtract);
   }

   public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(OVERGROWN_WILLOW_BOAT_LAYER, BoatModel::createBoatModel);
      event.registerLayerDefinition(ASH_WILLOW_BOAT_LAYER, BoatModel::createBoatModel);
      event.registerLayerDefinition(OVERGROWN_WILLOW_CHEST_BOAT_LAYER, BoatModel::createChestBoatModel);
      event.registerLayerDefinition(ASH_WILLOW_CHEST_BOAT_LAYER, BoatModel::createChestBoatModel);
   }

   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.OVERGROWN_WILLOW_BOAT, context -> new BoatRenderer(context, OVERGROWN_WILLOW_BOAT_LAYER));
      event.registerEntityRenderer(ModEntities.ASH_WILLOW_BOAT, context -> new BoatRenderer(context, ASH_WILLOW_BOAT_LAYER));
      event.registerEntityRenderer(ModEntities.OVERGROWN_WILLOW_CHEST_BOAT, context -> new BoatRenderer(context, OVERGROWN_WILLOW_CHEST_BOAT_LAYER));
      event.registerEntityRenderer(ModEntities.ASH_WILLOW_CHEST_BOAT, context -> new BoatRenderer(context, ASH_WILLOW_CHEST_BOAT_LAYER));
      event.registerBlockEntityRenderer(ModBlocks.ICHOR_CAULDRON_BLOCK_ENTITY, context -> new IchorCauldronRenderer());
      event.registerBlockEntityRenderer(ModBlocks.SIFT_PORTAL_BLOCK_ENTITY, context -> new SiftPortalRenderer());
      event.registerBlockEntityRenderer(ModBlocks.SONOROUS_DEEPSLATE_BLOCK_ENTITY, SonorousBeamRenderer::new);
      event.registerEntityRenderer(ModEntities.SINGER, SingerRenderer::new);
      event.registerEntityRenderer(ModEntities.ECHO_GOLEM, EchoGolemRenderer::new);
      event.registerEntityRenderer(ModEntities.DARK_SNIFFER, DarkSnifferRenderer::new);
      event.registerEntityRenderer(ModEntities.BLUB, BlubRenderer::new);
      event.registerEntityRenderer(ModEntities.SEEDLING, SeedlingRenderer::new);
      event.registerEntityRenderer(ModEntities.NESTER, NesterRenderer::new);
      event.registerEntityRenderer(ModEntities.SLABBER, SlabberRenderer::new);
      event.registerEntityRenderer(ModEntities.NUZZLE, NuzzleRenderer::new);
      event.registerEntityRenderer(ModEntities.STALKER, StalkerRenderer::new);
      event.registerEntityRenderer(ModEntities.SCAVENGER, ScavengerRenderer::new);
      event.registerEntityRenderer(ModEntities.SCAVENGER_PROJECTILE, ScavengerProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.MONARCH, context -> new MonarchRenderer(context, false));
      event.registerEntityRenderer(ModEntities.MONARCH_ECHO, context -> new MonarchRenderer(context, true));
      event.registerEntityRenderer(ModEntities.MONARCH_SUMMON_PROJECTILE, context -> new MonarchProjectileRenderer(context, true));
      event.registerEntityRenderer(ModEntities.MONARCH_EXPLOSION_PROJECTILE, context -> new MonarchProjectileRenderer(context, false));
      event.registerEntityRenderer(ModEntities.POLLINATOR, PollinatorRenderer::new);
      event.registerEntityRenderer(ModEntities.POLLINATOR_PROJECTILE, PollinatorProjectileRenderer::new);
   }

   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(ModParticles.ICHOR_SOUL_SURFACE, IchorSoulParticle.SurfaceProvider::new);
      event.registerSpriteSet(ModParticles.ICHOR_SOUL_RISING, IchorSoulParticle.RisingProvider::new);
      event.registerSpriteSet(ModParticles.ICHOR_SOUL_BODY, IchorSoulParticle.BodyProvider::new);
      event.registerSpriteSet(ModParticles.POLLINATOR_DROOL, PollinatorDroolParticle.Provider::new);
      event.registerSpriteSet(ModParticles.SIFT_PARALLAX, SiftParallaxParticle.Provider::new);
      event.registerSpriteSet(ModParticles.SIFT_NOTE, SiftNoteParticle.Provider::new);
      event.registerSpriteSet(ModParticles.SOUND_WAVE, SoundWaveParticle.Provider::new);
      event.registerSpriteSet(ModParticles.SINGER_SOUND_WAVE, SingerSoundWaveParticle.Provider::new);
      event.registerSpriteSet(ModParticles.CANYON_SOUL, SoulParticle.RiseProvider::new);
      event.registerSpriteSet(ModParticles.SOUL_FRAGMENT, SoulParticle.FragmentProvider::new);
      event.registerSpriteSet(ModParticles.ICHOR_SURFACE_MIST, IchorSurfaceParticle.Provider::new);
      event.registerSpriteSet(ModParticles.ICHOR_BUBBLE, IchorFluidParticle.BubbleProvider::new);
      event.registerSpriteSet(ModParticles.ICHOR_SPLASH, IchorFluidParticle.SplashProvider::new);
      event.registerSpriteSet(ModParticles.ICHOR_RAIN_SPLASH, IchorFluidParticle.RainSplashProvider::new);
   }
}
