package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import mielon.thesift.client.compat.SiftShaderCompat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitPipelineSet.Builder;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SonorousBeamRenderTypes {
   private static final Identifier BEAM_TEXTURE = id("textures/block/sonorous_deepslate_beam.png");
   private static final RenderType PHOTON_BEAM = RenderTypes.entityTranslucent(BEAM_TEXTURE);
   private static final RenderPipeline BEAM_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/sonorous_beam_depth"))
      .withVertexShader(id("core/sonorous_beam"))
      .withFragmentShader(id("core/sonorous_beam"))
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), true))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   private static final RenderPipeline WATER_OVERLAY_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/sonorous_beam_water_overlay"))
      .withVertexShader(id("core/sonorous_beam"))
      .withFragmentShader(id("core/sonorous_beam"))
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
      .withBindGroupLayout(BindGroupLayouts.PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   private static final OitPipelineSet BEAM_OIT = createOit(false);
   private static final OitPipelineSet WATER_OVERLAY_OIT = createOit(true);
   public static final RenderType BEAM = RenderType.create(
      "the_sift_sonorous_beam", RenderSetup.builder(BEAM_PIPELINE).setOitPipelines(BEAM_OIT).withTexture("Sampler0", BEAM_TEXTURE).createRenderSetup()
   );
   public static final RenderType WATER_OVERLAY = RenderType.create(
      "the_sift_sonorous_beam_water_overlay",
      RenderSetup.builder(WATER_OVERLAY_PIPELINE).setOitPipelines(WATER_OVERLAY_OIT).withTexture("Sampler0", BEAM_TEXTURE).createRenderSetup()
   );

   private static OitPipelineSet createOit(boolean overlay) {
      Builder builder = OitPipelineSet.builder(
         overlay ? "the_sift/sonorous_water_overlay" : "the_sift/sonorous_beam",
         RenderPipeline.builder(new Snippet[0])
            .withVertexShader(id("core/sonorous_beam"))
            .withFragmentShader(id("core/sonorous_beam"))
            .withBindGroupLayout(BindGroupLayouts.GLOBALS)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withVertexBinding(0, DefaultVertexFormat.BLOCK)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
      );
      if (overlay) {
         builder.withoutDepthTest();
      }

      return builder.build();
   }

   private SonorousBeamRenderTypes() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(BEAM_PIPELINE);
      event.registerPipeline(WATER_OVERLAY_PIPELINE);
      event.registerOitPipelineSet(BEAM_OIT);
      event.registerOitPipelineSet(WATER_OVERLAY_OIT);
   }

   public static RenderType beam() {
      // Photon applies beacon-specific fog/alpha rules to its BEACON_BEAM program.
      // The standard entity path preserves this mod's colored, scrolling beam.
      return SiftShaderCompat.isShaderPackInUse() && SiftShaderCompat.isPhotonShaderPackSelected()
         ? PHOTON_BEAM : BEAM;
   }

   public static void initialize() {
      SiftShaderCompat.assignPipeline(BEAM_PIPELINE, "BEACON_BEAM");
      SiftShaderCompat.assignPipeline(WATER_OVERLAY_PIPELINE, "BEACON_BEAM");
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }
}
