package mielon.thesift.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SiftCloudRenderPipelines {
   private static final Identifier SHADER = Identifier.fromNamespaceAndPath("the_sift", "core/rendertype_ichor_clouds");
   public static final RenderPipeline FANCY = RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/ichor_clouds"))
         .withVertexShader(SHADER)
         .withFragmentShader(SHADER)
         .build();
   public static final RenderPipeline FLAT = RenderPipeline.builder(RenderPipelines.CLOUDS_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/flat_ichor_clouds"))
         .withVertexShader(SHADER)
         .withFragmentShader(SHADER)
         .withCull(false)
         .build();

   private SiftCloudRenderPipelines() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(FANCY);
      event.registerPipeline(FLAT);
   }

   public static void initialize() {
   }
}
