package mielon.thesift.client.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import mielon.thesift.client.light.SingerHeadLightBridge;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SingerRenderer extends GeoEntityRenderer<SingerEntity, EntityRenderState> {
   // Use the resting GeoModel head_light height for one stable source.
   // A deferred locator callback cannot guarantee dynamic light registration.
   private static final double LIGHT_HEIGHT = 34.0 / 16.0;

   public SingerRenderer(Context context) {
      super(context, new SingerModel());
      this.withRenderLayer(new SingerSoulBlockLayer(context, this));
      this.withRenderLayer(new SingerGlowingGeoLayer(this));
      this.withRenderLayer(new SiftGlowingOutlineLayer(this));
      this.shadowRadius = 0.0F;
   }

   @Override
   public boolean shouldRender(SingerEntity singer, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
      SingerHeadLightBridge.update(singer, singer.getPosition(partialTick).add(0.0, LIGHT_HEIGHT, 0.0));
      return super.shouldRender(singer, frustum, cameraX, cameraY, cameraZ, partialTick);
   }
}
