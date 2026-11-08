package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.PollinatorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class PollinatorRenderer extends GeoEntityRenderer<PollinatorEntity, EntityRenderState> {
   public PollinatorRenderer(Context context) {
      super(context, new PollinatorModel());
      this.withScale(1.0F);
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.3375F;
   }

   public void extractRenderState(PollinatorEntity entity, EntityRenderState renderState, float partialTick) {
      super.extractRenderState(entity, renderState, partialTick);
      if (entity.isCurrentlyGlowing()) {
         renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
      float pitch = Mth.clamp(renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F), -10.0F, 10.0F);
      snapshots.ifPresent("Body", body -> body.setRotX(body.getRotX() - pitch * (float) (Math.PI / 180.0)));
   }
}
