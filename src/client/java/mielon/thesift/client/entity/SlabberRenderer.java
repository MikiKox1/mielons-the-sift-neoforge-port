package mielon.thesift.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.SlabberEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class SlabberRenderer extends GeoEntityRenderer<SlabberEntity, EntityRenderState> {
   private static final DataTicket<Float> STRIDE = DataTicket.create("the_sift_slabber_stride", Float.class);
   private static final DataTicket<Boolean> LICKING = DataTicket.create("the_sift_slabber_licking", Boolean.class);
   private static final DataTicket<Boolean> BABY = DataTicket.create("the_sift_slabber_baby", Boolean.class);

   public SlabberRenderer(Context context) {
      super(context, new SlabberModel());
      this.withScale(1.0F);
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.95F;
   }

   @Override
   protected float getShadowRadius(EntityRenderState state) {
      return 0.95F * state.boundingBoxWidth / 1.5F;
   }

   public void extractRenderState(SlabberEntity entity, EntityRenderState renderState, float partialTick) {
      super.extractRenderState(entity, renderState, partialTick);
      if (entity.isCurrentlyGlowing()) {
         renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }

   public void addRenderData(SlabberEntity entity, Void related, EntityRenderState state, float partialTick) {
      super.addRenderData(entity, related, state, partialTick);
      state.addGeckolibData(BABY, entity.isBaby());
      LivingEntityRenderState living = (LivingEntityRenderState)state;
      float stride = Mth.cos(living.walkAnimationPos * 0.6662F) * 1.4F * Math.min(living.walkAnimationSpeed, 1.0F);
      state.addGeckolibData(STRIDE, stride);
      state.addGeckolibData(LICKING, entity.isLicking());
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
      if (!renderPassInfo.getOrDefaultGeckolibData(LICKING, false)) {
         float stride = renderPassInfo.getOrDefaultGeckolibData(STRIDE, 0.0F);
         snapshots.ifPresent("FrontRight", bone -> bone.setRotX(stride));
         snapshots.ifPresent("BackLeft", bone -> bone.setRotX(stride));
         snapshots.ifPresent("FrontLeft", bone -> bone.setRotX(-stride));
         snapshots.ifPresent("BackRight", bone -> bone.setRotX(-stride));
      }

      float childScale = renderPassInfo.getOrDefaultGeckolibData(BABY, false) ? 0.5F : 1.0F;
      snapshots.ifPresent("bb_main", root -> {
         root.setScaleX(childScale);
         root.setScaleY(childScale);
         root.setScaleZ(childScale);
      });
   }
}
