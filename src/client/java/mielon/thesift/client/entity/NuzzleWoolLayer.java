package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import mielon.thesift.entity.NuzzleEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class NuzzleWoolLayer extends GeoRenderLayer<NuzzleEntity, Void, EntityRenderState> {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/nuzzle/nuzzle_wool.png");

   public NuzzleWoolLayer(NuzzleRenderer renderer) {
      super(renderer);
   }

   @Override
   public void submitRenderTask(RenderPassInfo<EntityRenderState> pass, SubmitNodeCollector tasks) {
      if (pass.willRender() && !pass.renderState().isInvisible) {
         int previous = pass.getOrDefaultGeckolibData(DataTickets.RENDER_COLOR, -1);
         pass.renderState().addGeckolibData(DataTickets.RENDER_COLOR, pass.getOrDefaultGeckolibData(NuzzleRenderer.WOOL_COLOR, -1));

         try {
            this.renderer.submitRenderTasks(pass, tasks.order(2), RenderTypes.entityCutoutZOffset(TEXTURE));
         } finally {
            pass.renderState().addGeckolibData(DataTickets.RENDER_COLOR, previous);
         }
      }
   }
}
