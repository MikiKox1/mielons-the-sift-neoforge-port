package mielon.thesift.client.entity;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.entity.NuzzleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

public final class NuzzleRenderer extends GeoEntityRenderer<NuzzleEntity, EntityRenderState> {
   static final DataTicket<Integer> WOOL_COLOR = DataTicket.create("the_sift_nuzzle_wool_color", Integer.class);
   private static final DataTicket<Boolean> SHEARED = DataTicket.create("the_sift_nuzzle_sheared", Boolean.class);
   private static final DataTicket<Boolean> BABY = DataTicket.create("the_sift_nuzzle_baby", Boolean.class);
   private static final DataTicket<Float> STRIDE = DataTicket.create("the_sift_nuzzle_stride", Float.class);
   private static final DataTicket<Float> GRAZE = DataTicket.create("the_sift_nuzzle_graze", Float.class);
   private static final DataTicket<Float> HEAD = DataTicket.create("the_sift_nuzzle_head", Float.class);

   public NuzzleRenderer(Context context) {
      super(context, new NuzzleModel());
      this.withRenderLayer(new NuzzleWoolLayer(this));
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.65F;
   }

   @Override
   protected float getShadowRadius(EntityRenderState state) {
      return NuzzlePose.shadowRadius(state.boundingBoxWidth);
   }

   public void addRenderData(NuzzleEntity entity, Void related, EntityRenderState state, float partialTick) {
      super.addRenderData(entity, related, state, partialTick);
      GeoRenderState geo = state;
      LivingEntityRenderState living = (LivingEntityRenderState)state;
      geo.addGeckolibData(SHEARED, entity.isSheared());
      geo.addGeckolibData(BABY, entity.isBaby());
      geo.addGeckolibData(STRIDE, Mth.cos(living.walkAnimationPos * 0.6662F) * 1.4F * Math.min(living.walkAnimationSpeed, 1.0F));
      geo.addGeckolibData(GRAZE, entity.getHeadEatPositionScale(partialTick));
      geo.addGeckolibData(HEAD, entity.getHeadEatAngleScale(partialTick));
      geo.addGeckolibData(WOOL_COLOR, NuzzleWoolColor.get(entity, partialTick));
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> pass, BoneSnapshots bones) {
      float stride = pass.getOrDefaultGeckolibData(STRIDE, 0.0F);
      bones.ifPresent("FrontRight", b -> b.setRotX(stride));
      bones.ifPresent("BackLeft", b -> b.setRotX(stride));
      bones.ifPresent("FrontLeft", b -> b.setRotX(-stride));
      bones.ifPresent("BackRight", b -> b.setRotX(-stride));
      bones.ifPresent("Wool", b -> b.skipRender(pass.getOrDefaultGeckolibData(SHEARED, false)));
      float scale = pass.getOrDefaultGeckolibData(BABY, false) ? 0.5F : 1.0F;
      bones.ifPresent("bb_main", b -> b.setScale(scale, scale, scale));
      float graze = pass.getOrDefaultGeckolibData(GRAZE, 0.0F);
      bones.ifPresent("Body", b -> b.setRotX(0.0F));
      float head = NuzzlePose.headPitch(pass.getOrDefaultGeckolibData(HEAD, 0.0F));
      bones.ifPresent("Head", b -> {
         b.setRotX(head);
         b.setTranslateY(NuzzlePose.headY(graze));
         b.setTranslateZ(NuzzlePose.headZ(head));
      });
   }
}
