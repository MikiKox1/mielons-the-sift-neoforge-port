package mielon.thesift.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import mielon.thesift.client.compat.SiftShaderCompat;
import mielon.thesift.client.render.RiftRenderTypes;
import mielon.thesift.entity.MiniRiftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public final class MiniRiftRenderer extends EntityRenderer<MiniRiftEntity, MiniRiftRenderer.State> {
   private static final RiftMesh CUBE = new RiftMesh(List.of(new RiftAssetModel.Box(-0.4F, -0.4F, -0.4F, 0.4F, 0.4F, 0.4F)));
   private static final Identifier ENERGY_TEXTURE = id("textures/misc/rift_energy_shaderpack.png");

   public MiniRiftRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public MiniRiftRenderer.State createRenderState() {
      return new MiniRiftRenderer.State();
   }

   public void extractRenderState(MiniRiftEntity entity, MiniRiftRenderer.State state, float partial) {
      super.extractRenderState(entity, state, partial);
      state.open = entity.getOpenScale(partial);
      state.backgroundVariant = entity.getBackgroundVariant();
   }

   protected int getBlockLightLevel(MiniRiftEntity entity, BlockPos pos) {
      return 15;
   }

   @Override
   protected AABB getBoundingBoxForCulling(MiniRiftEntity entity, float partialTicks) {
      return entity.getInterpolatedBoundingBox(partialTicks).inflate(1.0);
   }

   public void submit(MiniRiftRenderer.State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      if (!(state.open <= 0.0F)) {
         poses.pushPose();
         float size = state.open * state.open * (3.0F - 2.0F * state.open);
         poses.scale(size, size, size);
         if (!SiftShaderCompat.isShaderPackInUse()) {
            collector.submitCustomGeometry(
               poses, RiftRenderTypes.surface(true, state.backgroundVariant), (pose, buffer) -> CUBE.draw(buffer, pose, true, false)
            );
            collector.submitCustomGeometry(poses, RiftRenderTypes.GLOW, (pose, buffer) -> CUBE.draw(buffer, pose, true, true));
         } else {
            float seconds = shaderSeconds();
            float destinationAlpha = destinationAlpha(state.distanceToCameraSq);
            collector.submitCustomGeometry(poses, RenderTypes.beaconBeam(ENERGY_TEXTURE, false), (pose, buffer) -> {
               CUBE.drawShaderpack(buffer, pose, true, RiftMesh.ShaderpackPass.ENERGY, 1.0F, seconds);
               CUBE.drawShaderpack(buffer, pose, true, RiftMesh.ShaderpackPass.FRAME, 1.0F, seconds);
            });
            if (destinationAlpha > 0.001F) {
               collector.submitCustomGeometry(
                  poses,
                  RenderTypes.beaconBeam(RiftRenderTypes.texture(true, state.backgroundVariant), true),
                  (pose, buffer) -> CUBE.drawShaderpack(buffer, pose, true, RiftMesh.ShaderpackPass.DESTINATION, destinationAlpha, seconds)
               );
            }

            collector.submitCustomGeometry(
               poses,
               RenderTypes.beaconBeam(ENERGY_TEXTURE, true),
               (pose, buffer) -> CUBE.drawShaderpack(buffer, pose, true, RiftMesh.ShaderpackPass.GLOW, 0.4F, seconds)
            );
         }

         poses.popPose();
      }
   }

   private static float shaderSeconds() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         return 0.0F;
      }

      float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
      return ((float)minecraft.level.getGameTime() + partial) / 20.0F;
   }

   private static float destinationAlpha(double distanceSquared) {
      float distance = (float)Math.sqrt(Math.max(0.0, distanceSquared));
      float t = Math.max(0.0F, Math.min(1.0F, (distance - 3.0F) / 12.0F));
      float smooth = t * t * (3.0F - 2.0F * t);
      return (1.0F - smooth) * 0.78F;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   public static final class State extends EntityRenderState {
      float open;
      int backgroundVariant;
   }
}
