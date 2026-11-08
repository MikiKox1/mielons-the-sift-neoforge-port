package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.StalkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class StalkerRenderer extends GeoEntityRenderer<StalkerEntity, EntityRenderState> {
   public StalkerRenderer(Context context) {
      super(context, new StalkerModel());
      this.withScale(2.0F);
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.8F;
   }

   public void extractRenderState(StalkerEntity entity, EntityRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      if (entity.isCurrentlyGlowing()) {
         state.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> pass, BoneSnapshots bones) {
      float yaw = pass.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0.0F);
      float pitch = pass.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F);
      bones.ifPresent("Head", head -> {
         head.setRotX(head.getRotX() - Mth.clamp(pitch, -25.0F, 25.0F) * (float) (Math.PI / 180.0));
         head.setRotY(head.getRotY() + Mth.clamp(yaw, -40.0F, 40.0F) * (float) (Math.PI / 180.0));
      });
   }
}
