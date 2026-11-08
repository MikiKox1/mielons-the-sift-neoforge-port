package mielon.thesift.client.render;

import mielon.thesift.client.compat.SiftShaderCompat;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SiftPortalRenderTypes {
   private static final Identifier WHITE = Identifier.fromNamespaceAndPath("the_sift", "textures/misc/portal_white.png");
   public static final RenderType SURFACE = RenderTypes.beaconBeam(WHITE, true);
   public static final RenderType RIM = SURFACE;
   public static final RenderType CORE = RenderTypes.beaconBeam(WHITE, false);
   // A full opaque entity pass gives Iris a real color/depth surface behind the
   // animated translucent tiles; a depth-only mask can self-occlude in packs.
   public static final RenderType BACKING = RenderTypes.entitySolid(WHITE);
   private static final RenderType SHADER_SURFACE = RenderTypes.entityTranslucent(WHITE);
   private static final RenderPipeline DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/portal_depth_mask"))
      .withColorTargetState(new ColorTargetState(Optional.empty(), ColorTargetState.DEFAULT.format(), 0))
      .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), true))
      .withCull(false)
      .build();
   public static final RenderType DEPTH = RenderType.create(
      "the_sift_portal_depth", RenderSetup.builder(DEPTH_PIPELINE).withTexture("Sampler0", WHITE).createRenderSetup()
   );

   public static RenderType surface(boolean shaderPack) {
      return shaderPack ? SHADER_SURFACE : SURFACE;
   }

   public static RenderType rim(boolean shaderPack) {
      return shaderPack ? SHADER_SURFACE : RIM;
   }

   public static RenderType core(boolean shaderPack) {
      return shaderPack ? SHADER_SURFACE : CORE;
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(DEPTH_PIPELINE);
   }

   public static void initialize() {
      SiftShaderCompat.assignPipeline(DEPTH_PIPELINE, "BEACON_BEAM");
   }

   private SiftPortalRenderTypes() {
   }
}
