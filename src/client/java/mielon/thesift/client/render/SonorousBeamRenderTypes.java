package mielon.thesift.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import mielon.thesift.client.compat.SiftShaderCompat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SonorousBeamRenderTypes {
   private static final Identifier BEAM_TEXTURE = id("textures/block/sonorous_deepslate_beam.png");
   private static final RenderType PHOTON_BEAM = RenderTypes.entityTranslucent(BEAM_TEXTURE);
   private static final RenderPipeline BEAM_PIPELINE = RenderPipeline.builder()
      .withLocation(id("pipeline/sonorous_beam_depth"))
      .withVertexShader(id("core/sonorous_beam"))
      .withFragmentShader(id("core/sonorous_beam"))
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), true))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   private static final RenderPipeline WATER_OVERLAY_PIPELINE = RenderPipeline.builder()
      .withLocation(id("pipeline/sonorous_beam_water_overlay"))
      .withVertexShader(id("core/sonorous_beam"))
      .withFragmentShader(id("core/sonorous_beam"))
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.BLOCK)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   public static final RenderType BEAM = RenderType.create(
      "the_sift_sonorous_beam", RenderSetup.builder(BEAM_PIPELINE).withTexture("Sampler0", BEAM_TEXTURE).createRenderSetup()
   );
   public static final RenderType WATER_OVERLAY = RenderType.create(
      "the_sift_sonorous_beam_water_overlay", RenderSetup.builder(WATER_OVERLAY_PIPELINE).withTexture("Sampler0", BEAM_TEXTURE).createRenderSetup()
   );

   private SonorousBeamRenderTypes() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(BEAM_PIPELINE);
      event.registerPipeline(WATER_OVERLAY_PIPELINE);
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
