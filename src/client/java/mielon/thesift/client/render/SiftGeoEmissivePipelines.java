package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SiftGeoEmissivePipelines {
   public static final OitPipelineSet EMISSIVE = OitPipelineSet.builder(
            "the_sift/geckolib_emissive",
            RenderPipeline.builder(new Snippet[]{RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET})
               .withVertexShader("core/entity")
               .withFragmentShader("core/entity")
               .withShaderDefine("EMISSIVE")
               .withShaderDefine("NO_OVERLAY")
               .withShaderDefine("NO_CARDINAL_LIGHTING")
               .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
               .withVertexBinding(0, DefaultVertexFormat.ENTITY)
               .withPrimitiveTopology(PrimitiveTopology.QUADS)
               .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
               .withCull(false)
         )
         .build();

   private SiftGeoEmissivePipelines() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerOitPipelineSet(EMISSIVE);
   }
}
