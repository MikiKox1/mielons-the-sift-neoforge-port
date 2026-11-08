package mielon.thesift.client.loading;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import mielon.thesift.client.render.RiftRenderTypes;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class SiftLoadingBackgroundRenderer {
   private static final RenderPipeline RIFT_PIPELINE = createPipeline(true);

   private SiftLoadingBackgroundRenderer() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(RIFT_PIPELINE);
   }

   public static void draw(GuiGraphicsExtractor graphics, int width, int height) {
      if (!SiftLoadingScreenState.isRift()) {
         SiftPixelLoadingBackground.draw(graphics, width, height);
      } else {
         int clock = (int)(System.nanoTime() / 16666667L) & 16777215;
         boolean targetSift = !TheSiftDimension.LEVEL_KEY.equals(SiftLoadingScreenState.sourceDimension());
         Identifier texture = RiftRenderTypes.texture(targetSift, 0);
         RenderPipeline pipeline = RIFT_PIPELINE;
         graphics.blit(pipeline, texture, 0, 0, 0.0F, 0.0F, width, height, 320, 320, 320, 320, 0xFF000000 | clock);
      }
   }

   private static RenderPipeline createPipeline(boolean rift) {
      Builder builder = RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_TEXTURED_SNIPPET})
         .withLocation(id(rift ? "pipeline/rift_loading" : "pipeline/portal_loading"))
         .withFragmentShader(id("core/sift_loading"));
      if (rift) {
         builder.withShaderDefine("RIFT_LOADING");
      }

      return builder.build();
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }
}
