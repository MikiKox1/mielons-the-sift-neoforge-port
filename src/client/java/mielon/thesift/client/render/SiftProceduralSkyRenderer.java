package mielon.thesift.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.OptionalInt;
import java.util.OptionalDouble;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.compat.SiftSkyShaderCompat;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.ThriveSkyColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class SiftProceduralSkyRenderer {
   private static final int AZIMUTH_SEGMENTS = 192;
   private static final int ELEVATION_SEGMENTS = 112;
   private static final int VERTEX_COUNT = 86016;
   private static final float RADIUS = 99.0F;
   private static final float TAU = (float) (Math.PI * 2);
   private static final float ART_BOTTOM_DEGREES = -14.0F;
   private static final float ART_TOP_DEGREES = 90.0F;
   private static final float DOME_BOTTOM_DEGREES = -90.0F;
   private static final int BASE_THRIVE = 6;
   private static final int BASE_DAY = 0;
   private static final int BASE_NIGHT = 1;
   private static final int FAR_DAY = 2;
   private static final int FAR_NIGHT = 3;
   private static final int NEAR_DAY = 4;
   private static final int NEAR_NIGHT = 5;
   private static final GpuBuffer[] BUFFERS = new GpuBuffer[8];
   private static final int UV_WIDTH = 512;
   private static final int UV_HEIGHT = 256;
   private static final DynamicTexture[] SHADER_TEXTURES = new DynamicTexture[8];
   private static final Identifier[] SHADER_TEXTURE_IDS = new Identifier[8];
   private static final int[] NATIVE_ORDER = {0, 1, 6, 2, 3, 4, 5};
   private static final int[] SHADER_ORDER = {0, 1, 6, 2, 3, 4, 5, 7};
   private static GpuBuffer TEXTURED_DOME_BUFFER;
   private static boolean photonSkyQueued;
   private static ClientLevel photonSkyLevel;
   private static AutoStorageIndexBuffer photonSkyQuadIndices;
   private static String queuedShaderPack = "";
   private static final Matrix4f PHOTON_POSE = new Matrix4f();
   private static final Matrix4f PHOTON_MODELVIEW = new Matrix4f();
   private static final RenderPipeline TEXTURED_SKY_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/procedural_sky_uv_2612"))
         .withVertexShader("core/position_tex")
         .withFragmentShader("core/position_tex")
         .withSampler("Sampler0")
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
         .withCull(false)
         .build();
   private static final RenderPipeline PHOTON_POST_FINAL_SKY_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/photon_post_final_sky_2612"))
         .withVertexShader(Identifier.fromNamespaceAndPath("the_sift", "core/photon_sky_overlay"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("the_sift", "core/photon_sky_overlay"))
         .withSampler("Sampler0")
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
         .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
         .withCull(false)
         .build();
   private static final RenderPipeline COMPLEMENTARY_POST_FINAL_SKY_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/complementary_post_final_sky_2612"))
         .withVertexShader(Identifier.fromNamespaceAndPath("the_sift", "core/photon_sky_overlay"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("the_sift", "core/photon_sky_overlay"))
         .withSampler("Sampler0")
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
         .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build();
   private static final float[][] DAY_PALETTE = new float[][]{
      {1.0F, 0.61F, 0.76F}, {0.3F, 0.94F, 0.97F}, {0.52F, 1.0F, 0.8F}, {0.91F, 0.73F, 1.0F}, {1.0F, 0.82F, 0.64F}
   };
   private static final float[][] NIGHT_PALETTE = new float[][]{
      {0.22F, 0.76F, 1.0F}, {0.13F, 0.96F, 1.0F}, {0.36F, 0.47F, 1.0F}, {0.72F, 0.35F, 1.0F}, {0.92F, 0.38F, 0.88F}
   };

   public static void restoreAfterShaders(Matrix4fc view) {
      if (SiftSkyShaderCompat.restoreSky()
         && !SiftShaderCompat.isPhotonShaderPackSelected()
         && !SiftShaderCompat.isComplementaryShaderPackSelected()) {
         Minecraft client = Minecraft.getInstance();
         RenderTarget target = client.getMainRenderTarget();
         PoseStack poses = new PoseStack();
         poses.mulPose(new Matrix4f(view));
         AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
         renderTextured(poses, client.level, target, indices, true);
      }
   }

   public static void renderAfterPhotonFinalPass() {
      if (!photonSkyQueued) {
         return;
      }
      photonSkyQueued = false;
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null || level != photonSkyLevel || !SiftSkyShaderCompat.restoreSky()
         || !(SiftShaderCompat.isPhotonShaderPackSelected() || SiftShaderCompat.isComplementaryShaderPackSelected())
         || !queuedShaderPack.equals(SiftShaderCompat.activeShaderPackName())) {
         clearPhotonQueue();
         return;
      }

      PoseStack poses = new PoseStack();
      poses.last().pose().set(PHOTON_POSE);
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.set(PHOTON_MODELVIEW);
      try {
         renderTextured(poses, level, Minecraft.getInstance().getMainRenderTarget(), photonSkyQuadIndices, true);
      } finally {
         modelView.popMatrix();
         clearPhotonQueue();
      }
   }

   private static void clearPhotonQueue() {
      photonSkyQueued = false;
      photonSkyLevel = null;
      photonSkyQuadIndices = null;
      queuedShaderPack = "";
   }

   private static void queuePhoton(PoseStack poses, ClientLevel level, AutoStorageIndexBuffer quadIndices) {
      photonSkyQueued = true;
      photonSkyLevel = level;
      photonSkyQuadIndices = quadIndices;
      queuedShaderPack = SiftShaderCompat.activeShaderPackName();
      PHOTON_POSE.set(poses.last().pose());
      PHOTON_MODELVIEW.set(RenderSystem.getModelViewStack());
   }

   private SiftProceduralSkyRenderer() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(TEXTURED_SKY_PIPELINE);
      event.registerPipeline(PHOTON_POST_FINAL_SKY_PIPELINE);
      event.registerPipeline(COMPLEMENTARY_POST_FINAL_SKY_PIPELINE);
   }

   public static void initialize() {
      SiftShaderCompat.assignPipeline(TEXTURED_SKY_PIPELINE, "TEXTURED");
   }

   public static void render(PoseStack poseStack, ClientLevel level, RenderTarget renderTarget, AutoStorageIndexBuffer quadIndices) {
      if (SiftShaderCompat.isShaderPackInUse()) {
         if (!SiftSkyShaderCompat.restoreSky()) {
            return;
         }
         if (SiftShaderCompat.isPhotonShaderPackSelected() || SiftShaderCompat.isComplementaryShaderPackSelected()) {
            queuePhoton(poseStack, level, quadIndices);
         } else {
            renderTextured(poseStack, level, renderTarget, quadIndices, false);
         }
         return;
      }

      ensureBuffers();
      long ticks = level.getDefaultClockTime();
      float night = SiftTides.nightBlend(ticks);
      float day = SiftTides.flowBlend(ticks);
      float thrive = SiftTides.thriveBlend(ticks);
      float weather = 1.0F - level.getRainLevel(1.0F) * 0.16F;
      float time = (float)level.getGameTime() / 20.0F;
      GpuBufferSlice[] transforms = new GpuBufferSlice[8];
      transforms[7] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, thrive);
      transforms[6] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, thrive);
      transforms[0] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, 1.0F);
      transforms[1] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, thrive >= 0.99999F ? 0.0F : night / (1.0F - thrive));
      transforms[2] = transform(poseStack, time * 0.00135F, (float)Math.sin(time * 0.004F) * 0.01F, weather, day * 0.96F);
      transforms[3] = transform(poseStack, time * 0.00135F, (float)Math.sin(time * 0.004F) * 0.01F, weather, night * 0.96F);
      transforms[4] = transform(poseStack, -time * 0.00205F, (float)Math.sin(time * 0.0065F + 1.8F) * 0.016F, weather, day * 0.78F);
      transforms[5] = transform(poseStack, -time * 0.00205F, (float)Math.sin(time * 0.0065F + 1.8F) * 0.016F, weather, night * 0.82F);
      int indexCount = VertexFormat.Mode.QUADS.indexCount(VERTEX_COUNT);
      GpuBuffer indexBuffer = quadIndices.getBuffer(indexCount);

      try (RenderPass pass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
               () -> "The Sift procedural sky",
               renderTarget.getColorTextureView(),
               OptionalInt.empty(),
               renderTarget.getDepthTextureView(),
               OptionalDouble.empty()
            )) {
         pass.setPipeline(RenderPipelines.DEBUG_QUADS);
         RenderSystem.bindDefaultUniforms(pass);
         pass.setIndexBuffer(indexBuffer, quadIndices.type());

         for (int layer : NATIVE_ORDER) {
            float alpha = layer == 0 ? 1.0F : (layer == 1 ? night : (layer >= 6 ? thrive : (layer % 2 == 0 ? day : night)));
            if (!(alpha <= 0.001F)) {
               pass.setUniform("DynamicTransforms", transforms[layer]);
               pass.setVertexBuffer(0, BUFFERS[layer]);
               pass.drawIndexed(0, 0, indexCount, 1);
            }
         }
      }
   }

   private static void renderTextured(
      PoseStack poseStack, ClientLevel level, RenderTarget renderTarget, AutoStorageIndexBuffer quadIndices, boolean postFinal
   ) {
      if (TEXTURED_DOME_BUFFER == null) {
         TEXTURED_DOME_BUFFER = buildTexturedDome();
      }
      ensureShaderTextures();
      // Native MC26.1.2 uploads DynamicTexture synchronously; prepare views
      // and sampler outside the open render pass, including after F3+T.
      GpuTextureView[] textureViews = new GpuTextureView[SHADER_TEXTURES.length];
      for (int i = 0; i < textureViews.length; i++) {
         textureViews[i] = SHADER_TEXTURES[i].getTextureView();
      }
      GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
      int indexCount = VertexFormat.Mode.QUADS.indexCount(VERTEX_COUNT);
      GpuBuffer indexBuffer = quadIndices.getBuffer(indexCount);

      long ticks = level.getDefaultClockTime();
      float night = SiftTides.nightBlend(ticks);
      float day = SiftTides.flowBlend(ticks);
      float thrive = SiftTides.thriveBlend(ticks);
      float weather = 1.0F - level.getRainLevel(1.0F) * 0.16F;
      float time = (float)level.getGameTime() / 20.0F;
      // THRIVE previously painted two full-alpha layers on top of Photon,
      // eliminating its sky contribution. Keep FLOW/ENDURE unchanged.
      float photonBaseBlend = postFinal ? 0.72F - 0.20F * thrive : 1.0F;
      float photonThriveBlend = postFinal ? thrive * 0.48F : thrive;
      float photonSunriseBlend = postFinal ? thrive * 0.36F : thrive;
      boolean complementaryPost = postFinal && SiftShaderCompat.isComplementaryShaderPackSelected();
      GpuBufferSlice[] transforms = new GpuBufferSlice[8];
      transforms[7] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(7, night, day, thrive) : photonSunriseBlend);
      transforms[6] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(6, night, day, thrive) : photonThriveBlend);
      transforms[0] = transform(poseStack, time * 1.0E-4F, 0.0F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(0, night, day, thrive) : photonBaseBlend);
      transforms[1] = transform(poseStack, time * 1.0E-4F, 0.0F, weather,
         complementaryPost ? SiftComplementarySkyOpacity.forLayer(1, night, day, thrive) : (thrive >= 0.99999F ? 0.0F : night / (1.0F - thrive)) * photonBaseBlend);
      transforms[2] = transform(poseStack, time * 0.00135F, (float)Math.sin(time * 0.004F) * 0.01F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(2, night, day, thrive) : day * 0.96F);
      transforms[3] = transform(poseStack, time * 0.00135F, (float)Math.sin(time * 0.004F) * 0.01F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(3, night, day, thrive) : night * 0.96F);
      transforms[4] = transform(poseStack, -time * 0.00205F, (float)Math.sin(time * 0.0065F + 1.8F) * 0.016F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(4, night, day, thrive) : day * 0.78F);
      transforms[5] = transform(poseStack, -time * 0.00205F, (float)Math.sin(time * 0.0065F + 1.8F) * 0.016F, weather, complementaryPost ? SiftComplementarySkyOpacity.forLayer(5, night, day, thrive) : night * 0.82F);

      try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
         () -> "The Sift 2.0.1 shaderpack sky",
         renderTarget.getColorTextureView(), OptionalInt.empty(),
         renderTarget.getDepthTextureView(), OptionalDouble.empty()
      )) {
         pass.setPipeline(complementaryPost ? COMPLEMENTARY_POST_FINAL_SKY_PIPELINE : (postFinal ? PHOTON_POST_FINAL_SKY_PIPELINE : TEXTURED_SKY_PIPELINE));
         RenderSystem.bindDefaultUniforms(pass);
         pass.setIndexBuffer(indexBuffer, quadIndices.type());
         pass.setVertexBuffer(0, TEXTURED_DOME_BUFFER);
         for (int layer : SHADER_ORDER) {
            float alpha = layer == 0 ? 1.0F : (layer == 1 ? night : (layer >= 6 ? thrive : (layer % 2 == 0 ? day : night)));
            if (alpha > 0.001F) {
               pass.setUniform("DynamicTransforms", transforms[layer]);
               pass.bindTexture("Sampler0", textureViews[layer], sampler);
               pass.drawIndexed(0, 0, indexCount, 1);
            }
         }
      }
   }

   private static GpuBuffer buildTexturedDome() {
      int bytesPerVertex = DefaultVertexFormat.POSITION_TEX.getVertexSize();
      try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(VERTEX_COUNT * bytesPerVertex)) {
         BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         for (int y = 0; y < ELEVATION_SEGMENTS; y++) {
            float v0 = y / (float)ELEVATION_SEGMENTS;
            float v1 = (y + 1.0F) / ELEVATION_SEGMENTS;
            for (int x = 0; x < AZIMUTH_SEGMENTS; x++) {
               float u0 = x / (float)AZIMUTH_SEGMENTS;
               float u1 = (x + 1.0F) / AZIMUTH_SEGMENTS;
               addTexturedVertex(builder, u0, v0);
               addTexturedVertex(builder, u1, v0);
               addTexturedVertex(builder, u1, v1);
               addTexturedVertex(builder, u0, v1);
            }
         }
         try (MeshData mesh = builder.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> "The Sift 2.0.1 UV dome", 32, mesh.vertexBuffer());
         }
      }
   }

   private static void addTexturedVertex(BufferBuilder builder, float u, float v) {
      float azimuth = u * TAU;
      float elevation = (float)Math.toRadians(DOME_BOTTOM_DEGREES + v * 180.0F);
      float horizontal = (float)Math.cos(elevation) * RADIUS;
      builder.addVertex(
         (float)Math.sin(azimuth) * horizontal,
         (float)Math.sin(elevation) * RADIUS,
         (float)Math.cos(azimuth) * horizontal
      ).setUv(u, v);
   }

   private static void ensureShaderTextures() {
      if (SHADER_TEXTURES[0] != null) {
         return;
      }
      TextureManager textureManager = Minecraft.getInstance().getTextureManager();
      for (int layer = 0; layer < SHADER_TEXTURES.length; layer++) {
         if (SHADER_TEXTURES[layer] != null) {
            continue;
         }
         NativeImage image = new NativeImage(UV_WIDTH, UV_HEIGHT, true);
         for (int y = 0; y < UV_HEIGHT; y++) {
            float v = (y + 0.5F) / UV_HEIGHT;
            for (int x = 0; x < UV_WIDTH; x++) {
               float u = (x + 0.5F) / UV_WIDTH;
               boolean night = layer < 6 && (layer & 1) == 1;
               int originalLayer = layer < 2 ? 0 : (layer < 6 ? 1 + (layer - 2) / 2 : layer - 3);
               float[] rgba = sampleColor(u, v, night, originalLayer);
               image.setPixel(x, y, channel(rgba[3]) << 24 | channel(rgba[0]) << 16 | channel(rgba[1]) << 8 | channel(rgba[2]));
            }
         }
         DynamicTexture texture = new DynamicTexture(() -> "The Sift 2.0.1 sky layer", image);
         Identifier id = Identifier.fromNamespaceAndPath("the_sift", "dynamic/sky_uv_2612_" + layer);
         textureManager.register(id, texture);
         SHADER_TEXTURE_IDS[layer] = id;
         SHADER_TEXTURES[layer] = texture;
      }
   }

   public static void close() {
      clearPhotonQueue();
      TextureManager textureManager = Minecraft.getInstance().getTextureManager();
      for (int i = 0; i < SHADER_TEXTURES.length; i++) {
         if (SHADER_TEXTURES[i] != null) {
            textureManager.release(SHADER_TEXTURE_IDS[i]);
            SHADER_TEXTURES[i] = null;
            SHADER_TEXTURE_IDS[i] = null;
         }
      }
      if (TEXTURED_DOME_BUFFER != null) {
         TEXTURED_DOME_BUFFER.close();
         TEXTURED_DOME_BUFFER = null;
      }
      for (int i = 0; i < BUFFERS.length; i++) {
         if (BUFFERS[i] != null) {
            BUFFERS[i].close();
            BUFFERS[i] = null;
         }
      }
   }

   private static void ensureBuffers() {
      if (BUFFERS[0] == null) {
         BUFFERS[6] = buildDome(false, 3);
         BUFFERS[7] = buildDome(false, 4);
         BUFFERS[0] = buildDome(false, 0);
         BUFFERS[1] = buildDome(true, 0);
         BUFFERS[2] = buildDome(false, 1);
         BUFFERS[3] = buildDome(true, 1);
         BUFFERS[4] = buildDome(false, 2);
         BUFFERS[5] = buildDome(true, 2);
      }
   }

   private static GpuBuffer buildDome(boolean night, int layer) {
      int byteCount = 86016 * DefaultVertexFormat.POSITION_COLOR.getVertexSize();

      try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(byteCount)) {
         BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for (int y = 0; y < 112; y++) {
            float v0 = y / 112.0F;
            float v1 = (y + 1) / 112.0F;

            for (int x = 0; x < 192; x++) {
               float u0 = x / 192.0F;
               float u1 = (x + 1) / 192.0F;
               addVertex(builder, u0, v0, night, layer);
               addVertex(builder, u1, v0, night, layer);
               addVertex(builder, u1, v1, night, layer);
               addVertex(builder, u0, v1, night, layer);
            }
         }

         try (MeshData mesh = builder.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> "The Sift procedural sky layer", 32, mesh.vertexBuffer());
         }
      }
   }

   private static void addVertex(BufferBuilder builder, float u, float v, boolean night, int layer) {
      float azimuth = u * (float) (Math.PI * 2);
      float elevationDegrees = -90.0F + v * 180.0F;
      float elevation = (float)Math.toRadians(elevationDegrees);
      float horizontal = (float)Math.cos(elevation) * 99.0F;
      float px = (float)Math.sin(azimuth) * horizontal;
      float py = (float)Math.sin(elevation) * 99.0F;
      float pz = (float)Math.cos(azimuth) * horizontal;
      float[] rgba = sampleColor(u, v, night, layer);
      builder.addVertex(px, py, pz).setColor(channel(rgba[0]), channel(rgba[1]), channel(rgba[2]), channel(rgba[3]));
   }

   private static float[] sampleColor(float u, float v, boolean night, int layer) {
      float elevationDegrees = DOME_BOTTOM_DEGREES + v * 180.0F;
      float artV = clamp((elevationDegrees - ART_BOTTOM_DEGREES) / (ART_TOP_DEGREES - ART_BOTTOM_DEGREES));
      float lowerConvergence = smooth(clamp((ART_BOTTOM_DEGREES - elevationDegrees) / 68.0F));
      float ribbonFade = 1.0F - smooth(clamp((ART_BOTTOM_DEGREES - elevationDegrees) / 16.0F));
      float[] rgba = layer == 4
         ? ThriveSkyColors.sunrise(elevationDegrees)
         : (
            layer >= 3
               ? ThriveSkyColors.color(u, elevationDegrees)
               : (layer == 0 ? baseColor(u, artV, night, lowerConvergence) : ribbonColor(u, artV, night, layer - 1, ribbonFade))
         );
      return rgba;
   }

   private static float[] baseColor(float u, float v, boolean night, float lowerConvergence) {
      float horizon = 1.0F - smooth(clamp((v - 0.05F) / 0.82F));
      float wave = 0.5F + 0.5F * (float)Math.sin((float) (Math.PI * 2) * (u * 2.0F + 0.08F));
      float zenithBlend = smooth((v - 0.68F) / 0.27F);
      if (night) {
         float[] color = new float[]{
            lerp(horizon, 0.025F, 0.055F) + wave * 0.015F, lerp(horizon, 0.145F, 0.43F) + wave * 0.035F, lerp(horizon, 0.31F, 0.64F) + wave * 0.055F, 1.0F
         };
         float[] result = new float[]{lerp(zenithBlend, color[0], 0.035F), lerp(zenithBlend, color[1], 0.19F), lerp(zenithBlend, color[2], 0.43F), 1.0F};
         return convergeLowerHemisphere(result, lowerConvergence, 0.055F, 0.36F, 0.58F);
      } else {
         float r = lerp(horizon, 0.47F, 0.98F);
         float g = lerp(horizon, 0.82F, 0.76F);
         float b = lerp(horizon, 0.93F, 0.88F);
         float mint = 0.5F + 0.5F * (float)Math.sin((float) (Math.PI * 2) * (u * 3.0F - 0.21F));
         r -= mint * 0.07F * (1.0F - horizon * 0.35F);
         g += mint * 0.05F * (1.0F - horizon * 0.35F);
         float[] result = new float[]{lerp(zenithBlend, r, 0.43F), lerp(zenithBlend, g, 0.87F), lerp(zenithBlend, b, 0.94F), 1.0F};
         return convergeLowerHemisphere(result, lowerConvergence, 0.94F, 0.78F, 0.88F);
      }
   }

   private static float[] convergeLowerHemisphere(float[] color, float blend, float red, float green, float blue) {
      return new float[]{lerp(blend, color[0], red), lerp(blend, color[1], green), lerp(blend, color[2], blue), color[3]};
   }

   private static float[] ribbonColor(float u, float v, boolean night, int variant, float lowerFade) {
      return ribbonColor(u, v, night, variant, lowerFade, night ? NIGHT_PALETTE : DAY_PALETTE);
   }

   private static float[] ribbonColor(float u, float v, boolean night, int variant, float lowerFade, float[][] palette) {
      float red = 0.0F;
      float green = 0.0F;
      float blue = 0.0F;
      float total = 0.0F;
      float combined = 0.0F;

      for (int i = 0; i < palette.length; i++) {
         float frequency = 1.0F + i % 3;
         float phase = i * 0.173F + variant * 0.271F;
         float center = 0.13F
            + i * 0.17F
            + (0.115F - variant * 0.018F) * (float)Math.sin((float) (Math.PI * 2) * (u * frequency + phase))
            + 0.038F * (float)Math.sin((float) (Math.PI * 2) * (u * (frequency + 2.0F) - phase));
         float width = (variant == 0 ? 0.135F : 0.085F) + i % 2 * 0.018F;
         float band = smooth(clamp(1.0F - Math.abs(v - center) / width));
         float pulse = (float)Math.sin((float) (Math.PI * 2) * (u * 2.0F + phase));
         band *= 0.78F + 0.22F * pulse * pulse;
         band *= 1.0F - smooth((v - 0.7F) / 0.22F);
         red += palette[i][0] * band;
         green += palette[i][1] * band;
         blue += palette[i][2] * band;
         total += band;
         combined = 1.0F - (1.0F - combined) * (1.0F - band * 0.58F);
      }

      if (total <= 1.0E-4F) {
         return new float[]{1.0F, 1.0F, 1.0F, 0.0F};
      }

      float glow = variant == 0 ? (night ? 0.48F : 0.42F) : (night ? 0.37F : 0.3F);
      return new float[]{red / total, green / total, blue / total, clamp(combined * glow * lowerFade)};
   }

   private static GpuBufferSlice transform(PoseStack poseStack, float yaw, float roll, float brightness, float alpha) {
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.mul(poseStack.last().pose());
      modelView.rotateY(yaw);
      modelView.rotateZ(roll);
      GpuBufferSlice result = RenderSystem.getDynamicUniforms()
         .writeTransform(new Matrix4f(modelView), new Vector4f(brightness, brightness, brightness, alpha), new Vector3f(), new Matrix4f());
      modelView.popMatrix();
      return result;
   }

   private static int channel(float value) {
      return Math.round(clamp(value) * 255.0F);
   }

   private static float lerp(float delta, float start, float end) {
      return start + delta * (end - start);
   }

   private static float smooth(float value) {
      float clamped = clamp(value);
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
