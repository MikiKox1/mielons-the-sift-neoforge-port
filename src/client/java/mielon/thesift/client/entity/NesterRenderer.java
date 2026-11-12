package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.NesterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class NesterRenderer extends GeoEntityRenderer<NesterEntity, EntityRenderState> {
   private static final DataTicket<Boolean> ACTION = DataTicket.create("the_sift_nester_action", Boolean.class);
   private static final DataTicket<Float> HEAD_YAW = DataTicket.create("the_sift_nester_head_yaw", Float.class);

   public NesterRenderer(Context context) {
      super(context, new NesterModel());
      this.withScale(2.0F);
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.475F;
   }

   public void extractRenderState(NesterEntity entity, EntityRenderState renderState, float partialTick) {
      super.extractRenderState(entity, renderState, partialTick);
      if (entity.isCurrentlyGlowing()) {
         renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }

   public void addRenderData(NesterEntity entity, Void related, EntityRenderState state, float partialTick) {
      super.addRenderData(entity, related, state, partialTick);
      state.addGeckolibData(ACTION, entity.isPerformingAction());
      float head = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
      float body = this.calculateYRot(entity, 0.0F, partialTick);
      state.addGeckolibData(HEAD_YAW, NesterHeadRotation.relativeYaw(head, body));
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
      boolean action = renderPassInfo.getOrDefaultGeckolibData(ACTION, false);
      float yaw = renderPassInfo.getOrDefaultGeckolibData(HEAD_YAW, 0.0F);
      float pitch = action ? 0.0F : renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F);
      float headYaw = action ? Mth.clamp(yaw, -15.0F, 15.0F) : yaw;
      snapshots.ifPresent("LowerHead", head -> {
         head.setRotX(head.getRotX() - pitch * (float) (Math.PI / 180.0));
         head.setRotY(head.getRotY() + headYaw * (float) (Math.PI / 180.0));
      });
   }
}
