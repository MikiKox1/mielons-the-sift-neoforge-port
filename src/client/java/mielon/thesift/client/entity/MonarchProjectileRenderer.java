package mielon.thesift.client.entity;

import com.geckolib.renderer.GeoEntityRenderer;
import mielon.thesift.entity.MonarchProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class MonarchProjectileRenderer extends GeoEntityRenderer<MonarchProjectileEntity, EntityRenderState> {
   public MonarchProjectileRenderer(Context context, boolean variant) {
      super(context, new MonarchProjectileModel(variant));
      this.withScale(1.5F);
      this.shadowRadius = 0.0F;
      this.withRenderLayer(new SiftGlowingOutlineLayer<>(this));
   }

   @Override
   public RenderType getRenderType(EntityRenderState state, Identifier texture) {
      return state.isInvisible ? super.getRenderType(state, texture) : RenderTypes.entityTranslucentEmissive(texture);
   }
}
