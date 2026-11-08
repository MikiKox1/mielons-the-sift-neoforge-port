package mielon.thesift.client.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import mielon.thesift.entity.ScavengerProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class ScavengerProjectileRenderer extends GeoEntityRenderer<ScavengerProjectileEntity, EntityRenderState> {
   public ScavengerProjectileRenderer(Context context) {
      super(context, new ScavengerProjectileModel());
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.0F;
   }

   @Override
   public RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
      return renderState.isInvisible ? super.getRenderType(renderState, texture) : RenderTypes.entityTranslucent(texture);
   }

   public void extractRenderState(ScavengerProjectileEntity entity, EntityRenderState renderState, float partialTick) {
      super.extractRenderState(entity, renderState, partialTick);
      if (entity.isCurrentlyGlowing()) {
         renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }
}
