package mielon.thesift.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import mielon.thesift.TheSiftMod;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.compat.SiftSkyShaderCompat;
import mielon.thesift.world.SiftTides;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;

/** Optional eight-layer UV adapter. It never replaces the native 2.0.1 sky. */
public final class SiftShaderSkyRenderer {
   private static final int WIDTH = 1024;
   private static final int HEIGHT = 512;
   private static final int VERTEX_COUNT = 192 * 112 * 4;
   private static final int[] LAYER_ORDER = {0, 1, 6, 2, 3, 4, 5, 7};
   private static final float PHOTON_SKY_COVERAGE = 0.72F;
   private static final DynamicTexture[] TEXTURES = new DynamicTexture[8];
   private static final GpuTextureView[] VIEWS = new GpuTextureView[8];
   private static GpuBuffer dome;
   private static GpuBuffer indexBuffer;
   private static GpuSampler sampler;
   private static AutoStorageIndexBuffer preparedIndices;
   private static ClientLevel queuedLevel;
   private static String queuedPack = "";
   private static final Matrix4f QUEUED_POSE = new Matrix4f();
   private static final Matrix4f QUEUED_MODELVIEW = new Matrix4f();
   private static boolean frameDrawn;
   private static String lastPath = "";
   private static final RenderPipeline TEXTURED_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
      .withLocation(id("pipeline/tide_sky_textured"))
      .withVertexShader("core/position_tex")
      .withFragmentShader("core/position_tex")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .build();
   private static final RenderPipeline PHOTON_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
      .withLocation(id("pipeline/photon_tide_sky"))
      .withVertexShader(id("core/photon_tide_sky"))
      .withFragmentShader(id("core/photon_tide_sky"))
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
      .withCull(false)
      .build();

   private SiftShaderSkyRenderer() {}

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   private static Identifier textureId(int layer) {
      return id("dynamic/tide_sky/layer_" + layer);
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(TEXTURED_PIPELINE);
      event.registerPipeline(PHOTON_PIPELINE);
   }

   public static void initialize() {
      SiftShaderCompat.assignPipeline(TEXTURED_PIPELINE, "TEXTURED");
      // Photon runs after Iris' final composite and must remain unmapped.
   }

   public static boolean isManagedPack() {
      return SiftShaderCompat.isShaderPackInUse()
         && (SiftShaderCompat.isPhotonShaderPackSelected() || SiftShaderCompat.isComplementaryShaderPackSelected());
   }

   /** Called before opening the target-native sky pass. */
   public static void prepare(AutoStorageIndexBuffer indices) {
      queuedLevel = null;
      queuedPack = "";
      frameDrawn = false;
      if (!isManagedPack() || !SiftSkyShaderCompat.restoreSky()) return;
      if (dome == null) dome = buildDome();
      var manager = Minecraft.getInstance().getTextureManager();
      for (int layer = 0; layer < TEXTURES.length; layer++) {
         if (TEXTURES[layer] == null) {
            NativeImage image = new NativeImage(WIDTH, HEIGHT, false);
            try {
               for (int y = 0; y < HEIGHT; y++) {
                  for (int x = 0; x < WIDTH; x++) {
                     float[] rgba = SiftProceduralSkyRenderer.shaderLayerColor(layer,
                        x / (float)(WIDTH - 1), y / (float)(HEIGHT - 1));
                     image.setPixelABGR(x, y, pixel(rgba));
                  }
               }
               final int currentLayer = layer;
               DynamicTexture texture = new DynamicTexture(() -> "The Sift 2.0.1 tide layer " + currentLayer, image);
               manager.register(textureId(layer), texture);
               TEXTURES[layer] = texture;
            } catch (RuntimeException | Error error) {
               image.close();
               throw error;
            }
         }
         // Dynamic textures survive pack reload; refreshing views avoids stale references.
         VIEWS[layer] = TEXTURES[layer].getTextureView();
      }
      sampler = RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
         FilterMode.LINEAR, FilterMode.LINEAR, false);
      indexBuffer = indices.getBuffer(PrimitiveTopology.QUADS.indexCount(VERTEX_COUNT));
      preparedIndices = indices;
   }

   public static boolean renderSky(PoseStack pose, ClientLevel level, RenderTarget target, AutoStorageIndexBuffer indices) {
      prepare(indices);
      if (!isManagedPack() || !SiftSkyShaderCompat.restoreSky() || !ready()) return false;
      // Complementary and Photon both need the final Iris composite.
      // The early opaque TEXTURED pass washed out Complementary's own sky.
      queuedLevel = level;
      queuedPack = SiftShaderCompat.currentPackName();
      QUEUED_POSE.set(pose.last().pose());
      QUEUED_MODELVIEW.set(RenderSystem.getModelViewStack());
      return true;
   }

   /** Optional Iris callback; all GPU uploads have already finished in prepare(). */
   public static void afterPhotonFinal() {
      Minecraft client = Minecraft.getInstance();
      if (queuedLevel == null) return;
      if (queuedLevel != client.level || !queuedPack.equals(SiftShaderCompat.currentPackName())
         || !isManagedPack() || !SiftSkyShaderCompat.restoreSky() || !ready()) {
         queuedLevel = null;
         return;
      }
      ClientLevel level = queuedLevel;
      queuedLevel = null;
      PoseStack pose = new PoseStack();
      pose.last().pose().set(QUEUED_POSE);
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.set(QUEUED_MODELVIEW);
      try {
         drawFinal(pose, level);
         frameDrawn = true;
      } finally {
         modelView.popMatrix();
      }
   }

   /** Do not consume the queued known-pack sky before the Iris final callback. */
   public static void finishFrame(Matrix4fc view) {
      // LevelRenderer TAIL may precede Iris finalization. Never consume
      // or clear a queued Photon/Complementary sky here.
      if (isManagedPack()) return;
      queuedLevel = null;
      queuedPack = "";
      frameDrawn = false;
   }

   private static boolean ready() {
      return dome != null && indexBuffer != null && preparedIndices != null && sampler != null && VIEWS[7] != null;
   }

   private static void drawFinal(PoseStack pose, ClientLevel level) {
      var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
      try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
         () -> "The Sift 2.0.1 shader tide sky", target.getColorTextureView(), Optional.empty(),
         target.getDepthTextureView(), OptionalDouble.empty())) {
         draw(pose, level, pass, true);
      }
   }

   private static void draw(PoseStack pose, ClientLevel level, RenderPass pass, boolean postFinal) {
      long ticks = level.getDefaultClockTime();
      float night = SiftTides.nightBlend(ticks);
      float day = SiftTides.flowBlend(ticks);
      float thrive = SiftTides.thriveBlend(ticks);
      float weather = 1.0F - level.getRainLevel(1.0F) * 0.16F;
      float time = (float)level.getGameTime() / 20.0F;
      boolean complementaryPost = postFinal && SiftShaderCompat.isComplementaryShaderPackSelected();
      // Budget ALL eight Photon layers (not just the base sky): alpha
      // compositing of nearly opaque far/near ribbons would otherwise erase
      // Photon's own sky during FLOW/ENDURE. Per-phase exponent weights sum
      // to one, including while transitioning among all three tides.
      GpuBufferSlice[] transforms = new GpuBufferSlice[8];
      transforms[7] = SiftProceduralSkyRenderer.transform(pose, time * 1.0E-4F, 0.0F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(7, night, day, thrive) : (postFinal ? photonCompositeOpacity(thrive * 0.18F) : thrive));
      transforms[6] = SiftProceduralSkyRenderer.transform(pose, time * 1.0E-4F, 0.0F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(6, night, day, thrive) : (postFinal ? photonCompositeOpacity(thrive * 0.42F) : thrive));
      transforms[0] = SiftProceduralSkyRenderer.transform(pose, time * 1.0E-4F, 0.0F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(0, night, day, thrive) : (postFinal ? photonCompositeOpacity(0.40F) : 1.0F));
      transforms[1] = SiftProceduralSkyRenderer.transform(pose, time * 1.0E-4F, 0.0F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(1, night, day, thrive) : (postFinal ? photonCompositeOpacity(night * 0.20F) : (thrive >= 0.99999F ? 0.0F : night / (1.0F - thrive))));
      transforms[2] = SiftProceduralSkyRenderer.transform(pose, time * 0.00135F, (float)Math.sin(time * 0.004F) * 0.01F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(2, night, day, thrive) : (postFinal ? 0.96F * photonCompositeOpacity(day * 0.36F) : day * 0.96F));
      transforms[3] = SiftProceduralSkyRenderer.transform(pose, time * 0.00135F, (float)Math.sin(time * 0.004F) * 0.01F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(3, night, day, thrive) : (postFinal ? 0.96F * photonCompositeOpacity(night * 0.24F) : night * 0.96F));
      transforms[4] = SiftProceduralSkyRenderer.transform(pose, -time * 0.00205F, (float)Math.sin(time * 0.0065F + 1.8F) * 0.016F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(4, night, day, thrive) : (postFinal ? 0.78F * photonCompositeOpacity(day * 0.24F) : day * 0.78F));
      transforms[5] = SiftProceduralSkyRenderer.transform(pose, -time * 0.00205F, (float)Math.sin(time * 0.0065F + 1.8F) * 0.016F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(5, night, day, thrive) : (postFinal ? 0.82F * photonCompositeOpacity(night * 0.16F) : night * 0.82F));
      pass.setPipeline(postFinal ? PHOTON_PIPELINE : TEXTURED_PIPELINE);
      RenderSystem.bindDefaultUniforms(pass);
      pass.setIndexBuffer(indexBuffer, preparedIndices.type());
      pass.setVertexBuffer(0, dome.slice());
      for (int layer : LAYER_ORDER) {
         float alpha = layer == 0 ? 1.0F : (layer == 1 ? night : (layer >= 6 ? thrive : (layer % 2 == 0 ? day : night)));
         if (alpha <= 0.001F) continue;
         pass.setUniform("DynamicTransforms", transforms[layer]);
         pass.bindTexture("Sampler0", VIEWS[layer], sampler);
         pass.drawIndexed(PrimitiveTopology.QUADS.indexCount(VERTEX_COUNT), 1, 0, 0, 0);
      }
      String path = postFinal ? "post-final depth-masked 8-layer UV" : "Complementary Iris TEXTURED 8-layer UV";
      if (!path.equals(lastPath)) {
         lastPath = path;
         TheSiftMod.LOGGER.info("The Sift 2.0.1 shader sky: {} (pack: {})", path, SiftShaderCompat.currentPackName());
      }
   }

   /** The layer weights sum to one: at least 28% of the Photon image survives
    * all eight composited layers, regardless of the current tide. */
   private static float photonCompositeOpacity(float phaseWeight) {
      return 1.0F - (float)Math.pow(1.0F - PHOTON_SKY_COVERAGE,
         Math.max(0.0F, Math.min(1.0F, phaseWeight)));
   }

   private static GpuBuffer buildDome() {
      try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(VERTEX_COUNT * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
         BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX);
         for (int y = 0; y < 112; y++) {
            float v0 = y / 112.0F, v1 = (y + 1) / 112.0F;
            for (int x = 0; x < 192; x++) {
               float u0 = x / 192.0F, u1 = (x + 1) / 192.0F;
               vertex(builder, u0, v0); vertex(builder, u1, v0);
               vertex(builder, u1, v1); vertex(builder, u0, v1);
            }
         }
         try (MeshData mesh = builder.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> "The Sift eight-layer UV sky dome", 32, mesh.vertexBuffer());
         }
      }
   }

   private static void vertex(BufferBuilder builder, float u, float v) {
      float azimuth = u * (float)(Math.PI * 2);
      float elevation = (float)Math.toRadians(-90.0F + v * 180.0F);
      float horizontal = (float)Math.cos(elevation) * 99.0F;
      builder.addVertex((float)Math.sin(azimuth) * horizontal, (float)Math.sin(elevation) * 99.0F,
         (float)Math.cos(azimuth) * horizontal).setUv(u, v);
   }

   private static int pixel(float[] rgba) {
      return channel(rgba[0]) | channel(rgba[1]) << 8 | channel(rgba[2]) << 16 | channel(rgba[3]) << 24;
   }

   private static int channel(float value) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, value)) * 255.0F);
   }

   public static void close() {
      queuedLevel = null;
      queuedPack = "";
      frameDrawn = false;
      lastPath = "";
      for (int layer = 0; layer < TEXTURES.length; layer++) {
         if (TEXTURES[layer] != null) {
            Minecraft.getInstance().getTextureManager().release(textureId(layer));
            TEXTURES[layer] = null;
         }
         VIEWS[layer] = null;
      }
      if (dome != null) { dome.close(); dome = null; }
      sampler = null;
      indexBuffer = null;
      preparedIndices = null;
   }
}
