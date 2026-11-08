package mielon.thesift.client.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;

public final class SiftGlowingOutlineLayer<T extends Entity & GeoAnimatable> extends GeoRenderLayer<T, Void, EntityRenderState> {
   public SiftGlowingOutlineLayer(GeoRenderer<T, Void, EntityRenderState> renderer) {
      super(renderer);
   }

   @Override
   public void submitRenderTask(RenderPassInfo<EntityRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
      EntityRenderState state = renderPassInfo.renderState();
      if (renderPassInfo.willRender() && state.appearsGlowing()) {
         int previousColor = renderPassInfo.getOrDefaultGeckolibData(DataTickets.RENDER_COLOR, -1);
         int outlineColor = state.outlineColor == 0 ? -1 : state.outlineColor;
         state.addGeckolibData(DataTickets.RENDER_COLOR, outlineColor);

         try {
            this.renderer.submitRenderTasks(renderPassInfo, renderTasks.order(3), RenderTypes.outline(this.getTextureResource(state)));
         } finally {
            state.addGeckolibData(DataTickets.RENDER_COLOR, previousColor);
         }
      }
   }
}
