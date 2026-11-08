package mielon.thesift.client.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import mielon.thesift.entity.ScavengerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;

public final class ScavengerRenderer extends GeoEntityRenderer<ScavengerEntity, EntityRenderState> {
   public ScavengerRenderer(Context context) {
      super(context, new ScavengerModel());
      this.withScale(2.0F);
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.8F;
   }

   public void extractRenderState(ScavengerEntity entity, EntityRenderState state, float partialTick) {
      super.extractRenderState(entity, state, partialTick);
      if (entity.isCurrentlyGlowing()) {
         state.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }
}
