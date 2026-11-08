package mielon.thesift.client.entity;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import mielon.thesift.entity.MonarchEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class MonarchRenderer extends GeoEntityRenderer<MonarchEntity, EntityRenderState> {
   private final boolean echoVariant;

   public MonarchRenderer(Context context, boolean variant) {
      super(context, new MonarchModel(variant));
      this.echoVariant = variant;
      this.withScale(2.0F);
      this.shadowRadius = 0.85F;
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      if (variant) {
         this.withRenderLayer(new AutoGlowingGeoLayer<MonarchEntity, Void, EntityRenderState>(this) {
            protected Identifier getTextureResource(EntityRenderState state) {
               return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/monarch/monarch_echo_emissive.png");
            }
         });
      }
   }

   @Override
   public RenderType getRenderType(EntityRenderState state, Identifier texture) {
      return this.echoVariant && !state.isInvisible ? RenderTypes.entityTranslucent(texture) : super.getRenderType(state, texture);
   }

   @Override
   public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> pass, BoneSnapshots bones) {
      float yaw = pass.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0.0F);
      float pitch = pass.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F);
      bones.ifPresent("Head", head -> {
         head.setRotX(head.getRotX() - Mth.clamp(pitch, -35.0F, 35.0F) * (float) (Math.PI / 180.0));
         head.setRotY(head.getRotY() + Mth.clamp(yaw, -50.0F, 50.0F) * (float) (Math.PI / 180.0));
      });
   }
}
