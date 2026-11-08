package mielon.thesift.client.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import mielon.thesift.entity.SeedlingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.ARGB;

public final class SeedlingRenderer extends GeoEntityRenderer<SeedlingEntity, EntityRenderState> {
   public SeedlingRenderer(Context context) {
      super(context, new SeedlingModel());
      this.withScale(0.8F);
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
      this.shadowRadius = 0.336F;
   }

   public void extractRenderState(SeedlingEntity entity, EntityRenderState renderState, float partialTick) {
      super.extractRenderState(entity, renderState, partialTick);
      if (entity.isCurrentlyGlowing()) {
         renderState.outlineColor = ARGB.opaque(entity.getTeamColor());
      }
   }
}
