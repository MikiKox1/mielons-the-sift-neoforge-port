package mielon.thesift.client.entity;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.CustomBoneTextureGeoLayer;
import mielon.thesift.entity.EchoGolemEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public final class EchoGolemSoulGlowLayer extends CustomBoneTextureGeoLayer<EchoGolemEntity, Void, EntityRenderState> {
   public EchoGolemSoulGlowLayer(GeoRenderer<EchoGolemEntity, Void, EntityRenderState> renderer) {
      super(renderer, "soul_block", Identifier.fromNamespaceAndPath("the_sift", "textures/entity/echo_golem/echo_golem.png"));
   }

   public boolean shouldRenderBone(EntityRenderState state) {
      EchoGolemEntity golem = state.getGeckolibData(EchoGolemRenderer.ECHO_GOLEM_ENTITY);
      return golem != null && golem.hasSoulBlock() && !state.isInvisible;
   }

   @Override
   protected void renderBone(RenderPassInfo<EntityRenderState> pass, GeoBone bone, SubmitNodeCollector tasks) {
      int light = pass.packedLight();
      pass.renderState().addGeckolibData(DataTickets.PACKED_LIGHT, 15728880);

      try {
         super.renderBone(pass, bone, tasks);
      } finally {
         pass.renderState().addGeckolibData(DataTickets.PACKED_LIGHT, light);
      }
   }
}
