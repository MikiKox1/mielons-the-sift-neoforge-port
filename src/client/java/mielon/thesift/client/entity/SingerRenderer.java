package mielon.thesift.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.client.light.SingerSoulLightBridge;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class SingerRenderer extends GeoEntityRenderer<SingerEntity, EntityRenderState> {
   private static final DataTicket<SingerEntity> SINGER_ENTITY = DataTicket.create("the_sift_singer_entity", SingerEntity.class);

   public SingerRenderer(Context context) {
      super(context, new SingerModel());
      this.withRenderLayer(new SingerSoulBlockLayer(context, this));
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.0F;
   }

   public void addRenderData(SingerEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
      super.addRenderData(animatable, relatedObject, renderState, partialTick);
      renderState.addGeckolibData(SINGER_ENTITY, animatable);
   }

   @Override
   public void preRenderPass(RenderPassInfo<EntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
      SingerEntity singer = renderPassInfo.getGeckolibData(SINGER_ENTITY);
      if (singer != null) {
         renderPassInfo.addLocatorPositionListener("soul_light", (worldPos, modelPos, localPos) -> {
            if (worldPos != null) {
               SingerSoulLightBridge.update(singer, worldPos);
            }
         });
      }
   }
}
