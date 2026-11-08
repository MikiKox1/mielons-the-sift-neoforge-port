package mielon.thesift.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class RiftRenderTypes {
   private static final RenderPipeline SURFACE_PIPELINE = createPipeline(false);
   private static final RenderPipeline GLOW_PIPELINE = createPipeline(true);
   private static final Identifier[] SIFT_TEXTURES = textures("sift");
   private static final Identifier[] OVERWORLD_TEXTURES = textures("overworld");
   private static final RenderType[] SIFT_SURFACES = surfaces(SIFT_TEXTURES);
   private static final RenderType[] OVERWORLD_SURFACES = surfaces(OVERWORLD_TEXTURES);
   public static final Identifier SIFT_TEXTURE = SIFT_TEXTURES[0];
   public static final RenderType SIFT = SIFT_SURFACES[0];
   public static final RenderType OVERWORLD = OVERWORLD_SURFACES[0];
   public static final RenderType GLOW = createType(SIFT_TEXTURE, true);

   public static Identifier texture(boolean targetSift, int variant) {
      return (targetSift ? SIFT_TEXTURES : OVERWORLD_TEXTURES)[Math.floorMod(variant, 8)];
   }

   public static RenderType surface(boolean targetSift, int variant) {
      return (targetSift ? SIFT_SURFACES : OVERWORLD_SURFACES)[Math.floorMod(variant, 8)];
   }

   private static Identifier[] textures(String destination) {
      Identifier[] textures = new Identifier[8];

      for (int i = 0; i < textures.length; i++) {
         String path = "textures/entity/rift/" + destination + "_" + (i + 1) + ".png";
         textures[i] = id(path);
      }

      return textures;
   }

   private static RenderType[] surfaces(Identifier[] textures) {
      RenderType[] surfaces = new RenderType[textures.length];

      for (int i = 0; i < textures.length; i++) {
         surfaces[i] = createType(textures[i], false);
      }

      return surfaces;
   }

   private RiftRenderTypes() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(SURFACE_PIPELINE);
      event.registerPipeline(GLOW_PIPELINE);
   }

   private static RenderPipeline createPipeline(boolean glow) {
      Builder builder = RenderPipeline.builder(new Snippet[]{RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
         .withLocation(id(glow ? "pipeline/rift_glow" : "pipeline/rift_surface"))
         .withVertexShader(id("core/rift"))
         .withFragmentShader(id("core/rift"))
         .withSampler("Sampler0")
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
         .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), !glow))
         .withCull(true);
      if (glow) {
         builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
      }

      return builder.build();
   }

   private static RenderType createType(Identifier texture, boolean glow) {
      return RenderType.create(
         "sift_rift_" + texture.getPath() + "_" + glow,
         RenderSetup.builder(glow ? GLOW_PIPELINE : SURFACE_PIPELINE).withTexture("Sampler0", texture).createRenderSetup()
      );
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }
}
