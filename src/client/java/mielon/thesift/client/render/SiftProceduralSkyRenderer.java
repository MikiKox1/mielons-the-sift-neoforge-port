package mielon.thesift.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import java.util.Optional;
import java.util.OptionalDouble;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.compat.SiftSkyShaderCompat;
import mielon.thesift.world.SiftTides;
import mielon.thesift.world.ThriveSkyColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
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
   private static final RenderPipeline SKY_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/procedural_sky"))
         .withCull(false)
         .build();
   private static boolean restoringSky;
   private static final RenderPipeline RESTORE_PIPELINE = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("the_sift", "pipeline/restore_tide_sky"))
         .withVertexShader(Identifier.fromNamespaceAndPath("the_sift", "core/restore_tide_sky"))
         .withFragmentShader(Identifier.fromNamespaceAndPath("the_sift", "core/restore_tide_sky"))
         .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
         .withCull(false)
         .build();
   private static final float[][] DAY_PALETTE = new float[][]{
      {1.0F, 0.61F, 0.76F}, {0.3F, 0.94F, 0.97F}, {0.52F, 1.0F, 0.8F}, {0.91F, 0.73F, 1.0F}, {1.0F, 0.82F, 0.64F}
   };
   private static final float[][] NIGHT_PALETTE = new float[][]{
      {0.22F, 0.76F, 1.0F}, {0.13F, 0.96F, 1.0F}, {0.36F, 0.47F, 1.0F}, {0.72F, 0.35F, 1.0F}, {0.92F, 0.38F, 0.88F}
   };

   public static void restoreAfterShaders(Matrix4fc view) {
      if (SiftSkyShaderCompat.restoreSky()) {
         Minecraft client = Minecraft.getInstance();
         RenderTarget target = client.gameRenderer.mainRenderTarget();
         PoseStack poses = new PoseStack();
         poses.mulPose(new Matrix4f(view));
         AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
         prepareBuffers(indices);
         restoringSky = true;

         try {
            RenderPass pass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(
                  () -> "The Sift tide sky after shaders", target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty()
               );

            try {
               render(poses, client.level, pass, indices);
            } catch (Throwable var13) {
               if (pass != null) {
                  try {
                     pass.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (pass != null) {
               pass.close();
            }
         } finally {
            restoringSky = false;
         }
      }
   }

   private SiftProceduralSkyRenderer() {
   }

   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      event.registerPipeline(SKY_PIPELINE);
      event.registerPipeline(RESTORE_PIPELINE);
      SiftShaderSkyRenderer.registerPipelines(event);
   }

   // SkyRenderer opens its RenderPass before renderSunMoonAndStars. Both vertex
   // and sequential index GPU buffers must exist before that pass begins.
   public static void prepareBuffers(AutoStorageIndexBuffer quadIndices) {
      ensureBuffers();
      quadIndices.getBuffer(PrimitiveTopology.QUADS.indexCount(VERTEX_COUNT));
   }

   public static void initialize() {
      SiftShaderCompat.assignPipeline(SKY_PIPELINE, "SKY_BASIC");
   }

   public static void render(PoseStack poseStack, ClientLevel level, RenderPass pass, AutoStorageIndexBuffer quadIndices) {
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
      int indexCount = PrimitiveTopology.QUADS.indexCount(86016);
      GpuBuffer indexBuffer = quadIndices.getBuffer(indexCount);
      pass.setPipeline(
         RenderSystem.getCompiledPipeline(restoringSky ? RESTORE_PIPELINE : (SiftShaderCompat.isShaderPackInUse() ? SKY_PIPELINE : RenderPipelines.DEBUG_QUADS))
      );
      RenderSystem.bindDefaultUniforms(pass);
      pass.setIndexBuffer(indexBuffer, quadIndices.type());

      for (int layer : restoringSky ? new int[]{0, 1, 6, 2, 3, 4, 5, 7} : new int[]{0, 1, 6, 2, 3, 4, 5}) {
         float alpha = layer == 0 ? 1.0F : (layer == 1 ? night : (layer >= 6 ? thrive : (layer % 2 == 0 ? day : night)));
         if (!(alpha <= 0.001F)) {
            pass.setUniform("DynamicTransforms", transforms[layer]);
            pass.setVertexBuffer(0, BUFFERS[layer].slice());
            pass.drawIndexed(indexCount, 1, 0, 0, 0);
         }
      }
   }

   public static void close() {
      SiftShaderSkyRenderer.close();
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
      ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(byteCount);

      GpuBuffer var16;
      try {
         BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);

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

         MeshData mesh = builder.buildOrThrow();

         try {
            var16 = RenderSystem.getDevice().createBuffer(() -> "The Sift procedural sky layer", 32, mesh.vertexBuffer());
         } catch (Throwable var13) {
            if (mesh != null) {
               try {
                  mesh.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (mesh != null) {
            mesh.close();
         }
      } catch (Throwable var14) {
         if (bytes != null) {
            try {
               bytes.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }
         }

         throw var14;
      }

      if (bytes != null) {
         bytes.close();
      }

      return var16;
   }

   private static void addVertex(BufferBuilder builder, float u, float v, boolean night, int layer) {
      float azimuth = u * (float) (Math.PI * 2);
      float elevationDegrees = -90.0F + v * 180.0F;
      float elevation = (float)Math.toRadians(elevationDegrees);
      float horizontal = (float)Math.cos(elevation) * 99.0F;
      float px = (float)Math.sin(azimuth) * horizontal;
      float py = (float)Math.sin(elevation) * 99.0F;
      float pz = (float)Math.cos(azimuth) * horizontal;
      float artV = clamp((elevationDegrees - -14.0F) / 104.0F);
      float lowerConvergence = smooth(clamp((-14.0F - elevationDegrees) / 68.0F));
      float ribbonFade = 1.0F - smooth(clamp((-14.0F - elevationDegrees) / 16.0F));
      float[] rgba = layer == 4
         ? ThriveSkyColors.sunrise(elevationDegrees)
         : (
            layer >= 3
               ? ThriveSkyColors.color(u, elevationDegrees)
               : (layer == 0 ? baseColor(u, artV, night, lowerConvergence) : ribbonColor(u, artV, night, layer - 1, ribbonFade))
         );
      builder.addVertex(px, py, pz).setColor(channel(rgba[0]), channel(rgba[1]), channel(rgba[2]), channel(rgba[3]));
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

   static GpuBufferSlice transform(PoseStack poseStack, float yaw, float roll, float brightness, float alpha) {
      Matrix4fStack modelView = RenderSystem.getModelViewStack();
      modelView.pushMatrix();
      modelView.mul(poseStack.last().pose());
      modelView.rotateY(yaw);
      modelView.rotateZ(roll);
      GpuBufferSlice result = RenderSystem.getDynamicUniforms()
         .writeTransform(new Matrix4f(modelView), new Vector4f(brightness, brightness, brightness, alpha));
      modelView.popMatrix();
      return result;
   }

   private static int channel(float value) {
      return Math.round(clamp(value) * 255.0F);
   }

   /** Same 2.0.1 colors as addVertex(), sampled per-pixel only for shaderpacks. */
   static float[] shaderLayerColor(int textureLayer, float u, float v) {
      if (textureLayer < 0 || textureLayer >= 8) throw new IllegalArgumentException("Unknown sky layer");
      boolean night = textureLayer == 1 || textureLayer == 3 || textureLayer == 5;
      int layer = textureLayer < 6 ? textureLayer / 2 : textureLayer - 3;
      float elevationDegrees = -90.0F + v * 180.0F;
      float artV = clamp((elevationDegrees - -14.0F) / 104.0F);
      float lowerConvergence = smooth(clamp((-14.0F - elevationDegrees) / 68.0F));
      float ribbonFade = 1.0F - smooth(clamp((-14.0F - elevationDegrees) / 16.0F));
      return layer == 4 ? ThriveSkyColors.sunrise(elevationDegrees)
         : (layer >= 3 ? ThriveSkyColors.color(u, elevationDegrees)
            : (layer == 0 ? baseColor(u, artV, night, lowerConvergence)
               : ribbonColor(u, artV, night, layer - 1, ribbonFade)));
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
