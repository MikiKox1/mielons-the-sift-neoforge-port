package mielon.thesift.client.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SiftCloudRenderPipelines {
   private static final Identifier SHADER = Identifier.fromNamespaceAndPath("the_sift", "core/rendertype_ichor_clouds");
   public static final RenderPipeline FANCY = RenderPipeline.builder(new Snippet[]{RenderPipelines.CLOUDS_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/ichor_clouds"))
         .withVertexShader(SHADER)
         .withFragmentShader(SHADER)
         .build();
   public static final RenderPipeline FLAT = RenderPipeline.builder(new Snippet[]{RenderPipelines.CLOUDS_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/flat_ichor_clouds"))
         .withVertexShader(SHADER)
         .withFragmentShader(SHADER)
         .withCull(false)
         .build();
   public static final OitPipelineSet OIT_FANCY = OitPipelineSet.builder(
            "the_sift/ichor_clouds",
            RenderPipeline.builder(new Snippet[]{RenderPipelines.OIT_CLOUDS_SNIPPET}).withVertexShader(SHADER).withFragmentShader(SHADER)
         )
         .build();
   public static final OitPipelineSet OIT_FLAT = OitPipelineSet.builder(
            "the_sift/flat_ichor_clouds",
            RenderPipeline.builder(new Snippet[]{RenderPipelines.OIT_CLOUDS_SNIPPET}).withVertexShader(SHADER).withFragmentShader(SHADER).withCull(false)
         )
         .build();

   private SiftCloudRenderPipelines() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(FANCY);
      event.registerPipeline(FLAT);
      event.registerOitPipelineSet(OIT_FANCY);
      event.registerOitPipelineSet(OIT_FLAT);
   }

   public static void initialize() {
   }
}
