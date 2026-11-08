package mielon.thesift.client;

import mielon.thesift.client.entity.MiniRiftRenderer;
import mielon.thesift.client.entity.RiftRenderer;
import mielon.thesift.client.loading.SiftClientPortalTransition;
import mielon.thesift.client.map.SiftMapClientColors;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.network.MapColorsPayload;
import mielon.thesift.network.RiftLoadingPayload;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class RiftClientInitializer {
   private RiftClientInitializer() {
   }

   public static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
      event.register(RiftLoadingPayload.TYPE, (payload, context) -> SiftClientPortalTransition.handle(payload));
      event.register(MapColorsPayload.TYPE, (payload, context) -> SiftMapClientColors.accept(payload));
   }

   public static void clearMapColors() {
      SiftMapClientColors.clear();
   }

   public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.RIFT, RiftRenderer::new);
      event.registerEntityRenderer(ModEntities.MINI_RIFT, MiniRiftRenderer::new);
      event.registerEntityRenderer(ModEntities.SIFTITE_RETURN, context -> new ThrownItemRenderer<>(context, 1.0F, true));
   }
}
