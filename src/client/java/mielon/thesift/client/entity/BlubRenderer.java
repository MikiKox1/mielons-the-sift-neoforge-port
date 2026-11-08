package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.BlubEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public final class BlubRenderer extends GeoEntityRenderer<BlubEntity, EntityRenderState> {
   static final DataTicket<BlubEntity> BLUB_ENTITY = DataTicket.create("the_sift_blub_entity", BlubEntity.class);

   public BlubRenderer(Context context) {
      super(context, new BlubModel());
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.3325F;
   }

   public void addRenderData(BlubEntity animatable, Void relatedObject, EntityRenderState renderState, float partialTick) {
      super.addRenderData(animatable, relatedObject, renderState, partialTick);
      GeoRenderState geoState = renderState;
      geoState.addGeckolibData(BLUB_ENTITY, animatable);
      if (animatable.getVehicle() instanceof BlubEntity) {
         float yaw = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
         if (renderState instanceof LivingEntityRenderState living) {
            living.bodyRot = yaw;
            living.yRot = 0.0F;
         }

         geoState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
      }
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
      BlubEntity blub = renderPassInfo.getGeckolibData(BLUB_ENTITY);
      if (blub != null) {
         float healthRatio = Mth.clamp(blub.getHealth() / blub.getMaxHealth(), 0.0F, 1.0F);
         switch (blub.getVisualVariant()) {
            case CARAPACE: {
               float strength = blub.getFirstPassenger() instanceof BlubEntity ? 1.0F : 0.12F + 0.88F * healthRatio;
               snapshots.ifPresent("left_ear", ear -> {
                  ear.setRotX(ear.getRotX() * strength);
                  ear.setRotY(ear.getRotY() * strength);
                  ear.setRotZ(ear.getRotZ() * strength);
               });
               snapshots.ifPresent("right_ear", ear -> {
                  ear.setRotX(ear.getRotX() * strength);
                  ear.setRotY(ear.getRotY() * strength);
                  ear.setRotZ(ear.getRotZ() * strength);
               });
               break;
            }
            case MEADOWS: {
               float droop = (1.0F - healthRatio) * 70.0F * (float) (Math.PI / 180.0);
               float strength = 0.35F + 0.65F * healthRatio;
               snapshots.ifPresent("tail", tail -> {
                  tail.setRotX(tail.getRotX() * strength + droop);
                  tail.setRotY(tail.getRotY() * strength);
               });
               break;
            }
            default: {
               float droop = blub.getFirstPassenger() instanceof BlubEntity ? 0.0F : (1.0F - healthRatio) * 72.0F * (float) (Math.PI / 180.0);
               snapshots.ifPresent("left_ear", ear -> ear.setRotX(ear.getRotX() + droop));
               snapshots.ifPresent("right_ear", ear -> ear.setRotX(ear.getRotX() + droop));
            }
         }
      }
   }
}
