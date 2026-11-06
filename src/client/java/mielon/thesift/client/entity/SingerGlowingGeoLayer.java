package mielon.thesift.client.entity;

import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.RenderPassInfo;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.entity.SingerEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Restores the original 1.21.1 SingerGlowLayer's entity-cutout mask under NeoForge 26.3. */
public final class SingerGlowingGeoLayer extends AutoGlowingGeoLayer<SingerEntity, Void, EntityRenderState> {
   private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath("the_sift", "textures/entity/singer_glowmask.png");
   private static final int FULL_BRIGHT = 15728880;
   private static final int SHADER_GLOW_TINT = 0x80808080;

   public SingerGlowingGeoLayer(SingerRenderer renderer) {
      super(renderer);
   }

   @Override
   protected RenderType getRenderType(EntityRenderState state) {
      if (state.isInvisible) {
         return super.getRenderType(state);
      }

      return SiftShaderCompat.isShaderPackInUse()
         ? RenderTypes.entityTranslucentEmissive(GLOW_TEXTURE)
         : RenderTypes.entityCutout(GLOW_TEXTURE);
   }

   @Override
   protected int getBrightness(EntityRenderState state) {
      return FULL_BRIGHT;
   }

   @Override
   public void submitRenderTask(RenderPassInfo<EntityRenderState> pass, SubmitNodeCollector collector) {
      if (pass.renderState().isInvisible || !SiftShaderCompat.isShaderPackInUse()) {
         super.submitRenderTask(pass, collector);
         return;
      }

      // Iris treats the built-in emissive renderer as bright additive eyes;
      // tint only Singer's glow and restore the shared render state after submission.
      int previous = pass.renderColor();
      pass.renderState().addGeckolibData(DataTickets.RENDER_COLOR, ARGB.multiply(previous, SHADER_GLOW_TINT));
      try {
         super.submitRenderTask(pass, collector);
      } finally {
         pass.renderState().addGeckolibData(DataTickets.RENDER_COLOR, previous);
      }
   }
}
