package mielon.thesift.client;

import mielon.thesift.TheSiftMod;
import mielon.thesift.client.loading.SiftLoadingBackgroundRenderer;
import mielon.thesift.client.render.IchorFluidRendering;
import mielon.thesift.client.render.RiftRenderTypes;
import mielon.thesift.client.render.SiftCloudRenderPipelines;
import mielon.thesift.client.render.SiftPortalRenderTypes;
import mielon.thesift.client.render.SiftProceduralSkyRenderer;
import mielon.thesift.client.render.SonorousBeamRenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = TheSiftMod.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
   private ClientModEvents() {
   }

   // NeoForge 26.2 keeps RenderPipelines.register private. Register every
   // mod-specific Blaze3D pipeline through the official client mod event,
   // including the source's own pipelines which Fabric could register directly.
   @SubscribeEvent
   private static void registerCustomPipelines(RegisterRenderPipelinesEvent event) {
      RiftRenderTypes.registerPipelines(event);
      SiftPortalRenderTypes.registerPipelines(event);
      SonorousBeamRenderTypes.registerPipelines(event);
      SiftCloudRenderPipelines.registerPipelines(event);
      SiftProceduralSkyRenderer.registerPipelines(event);
      SiftLoadingBackgroundRenderer.registerPipelines(event);
   }

   @SubscribeEvent
   private static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(TheSiftClient::initialize);
   }

   @SubscribeEvent
   private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
      TheSiftClient.registerLayerDefinitions(event);
   }

   @SubscribeEvent
   private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      TheSiftClient.registerRenderers(event);
      RiftClientInitializer.registerRenderers(event);
   }

   @SubscribeEvent
   private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      TheSiftClient.registerParticleProviders(event);
   }

   @SubscribeEvent
   private static void clientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
      RiftClientInitializer.clearMapColors();
   }

   @SubscribeEvent
   private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
      RiftClientInitializer.registerPayloadHandlers(event);
   }

   @SubscribeEvent
   private static void registerFluidModels(RegisterFluidModelsEvent event) {
      IchorFluidRendering.register(event);
   }
}
