package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import mielon.thesift.block.entity.SiftPortalBlockEntity;
import mielon.thesift.client.compat.SiftShaderCompat;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SiftPortalRenderer implements BlockEntityRenderer<SiftPortalBlockEntity, SiftPortalRenderer.State> {
   // The opaque shader-only backing has the same color as the original base
   // surface. It replaces the invisible depth-only BEACON pass under Iris.
   private static final int SHADER_BACKING_COLOR = 0xFF26C0C5;

   public SiftPortalRenderer.State createRenderState() {
      return new SiftPortalRenderer.State();
   }

   @Override
   public int getViewDistance() {
      return 256;
   }

   public void extractRenderState(SiftPortalBlockEntity entity, SiftPortalRenderer.State state, float partial, Vec3 camera, CrumblingOverlay overlay) {
      BlockEntityRenderState.extractBase(entity, state, overlay);
      BlockPos pos = entity.getBlockPos();
      state.axisX = entity.getBlockState().getValue(SiftPortalBlock.AXIS) == Axis.X;
      state.worldU = state.axisX ? pos.getX() : pos.getZ();
      state.worldY = pos.getY();
      state.positive = state.axisX ? camera.z >= pos.getZ() + 0.5 : camera.x >= pos.getX() + 0.5;
      state.active = entity.getBlockState().getValue(SiftPortalBlock.ACTIVE);
      state.edges = edges(entity);
      double distance = camera.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      state.quality = distance < 2304.0 ? 8 : (distance < 12544.0 ? 4 : 2);
      state.seconds = entity.getLevel() == null ? 0.0 : ((double)entity.getLevel().getGameTime() + partial) / 20.0;
   }

   public void submit(SiftPortalRenderer.State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      boolean axisX = state.axisX;
      boolean positive = state.positive;
      int worldU = state.worldU;
      int worldY = state.worldY;
      int edges = state.edges;
      int quality = state.quality;
      double seconds = state.seconds;
      boolean shaderPack = SiftShaderCompat.isShaderPackInUse();
      if (state.active) {
         collector.submitCustomGeometry(
            poses,
            shaderPack ? SiftPortalRenderTypes.SHADER_BACKING : SiftPortalRenderTypes.DEPTH,
            (pose, buffer) -> PixelPortalSurface.drawDepthMask(
               axisX, (x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color) -> quad(pose, buffer, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, shaderPack ? SHADER_BACKING_COLOR : color)
            )
         );
      }

      if (state.active) {
         collector.submitCustomGeometry(
            poses,
            SiftPortalRenderTypes.surface(shaderPack),
            (pose, buffer) -> PixelPortalSurface.drawSurface(
               axisX,
               worldU,
               worldY,
               positive,
               seconds,
               quality,
               (x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color) -> quad(pose, buffer, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color)
            )
         );
      }

      collector.submitCustomGeometry(
         poses,
         SiftPortalRenderTypes.rim(shaderPack),
         (pose, buffer) -> {
            PixelPortalSurface.QuadSink sink =
               (x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color) -> quad(pose, buffer, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color);
            if (shaderPack) {
               PixelPortalSurface.drawShaderRimGlow(axisX, edges, positive, seconds, sink);
            } else {
               PixelPortalSurface.drawRimGlow(axisX, edges, positive, seconds, sink);
            }
         }
      );
      collector.submitCustomGeometry(
         poses,
         SiftPortalRenderTypes.core(shaderPack),
         (pose, buffer) -> {
            PixelPortalSurface.QuadSink sink =
               (x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color) -> quad(pose, buffer, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color);
            if (shaderPack) {
               PixelPortalSurface.drawShaderRimCore(axisX, edges, positive, sink);
            } else {
               PixelPortalSurface.drawRimCore(axisX, edges, positive, sink);
            }
         }
      );
   }

   private static void quad(
      Pose pose,
      VertexConsumer buffer,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      int color
   ) {
      float nx = Math.signum((y1 - y0) * (z2 - z0) - (z1 - z0) * (y2 - y0));
      float ny = Math.signum((z1 - z0) * (x2 - x0) - (x1 - x0) * (z2 - z0));
      float nz = Math.signum((x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0));
      vertex(pose, buffer, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz, color);
      vertex(pose, buffer, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz, color);
      vertex(pose, buffer, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz, color);
      vertex(pose, buffer, x3, y3, z3, 0.0F, 1.0F, nx, ny, nz, color);
   }

   private static void vertex(Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v, float nx, float ny, float nz, int color) {
      buffer.addVertex(pose.pose(), x, y, z)
         .setColor(color >>> 16 & 0xFF, color >>> 8 & 0xFF, color & 0xFF, color >>> 24 & 0xFF)
         .setUv(u, v)
         .setOverlay(655360)
         .setLight(15728880)
         .setNormal(nx, ny, nz);
   }

   private static int edges(SiftPortalBlockEntity entity) {
      Level level = entity.getLevel();
      BlockPos pos = entity.getBlockPos();
      Axis axis = entity.getBlockState().getValue(SiftPortalBlock.AXIS);
      Direction along = axis == Axis.X ? Direction.EAST : Direction.SOUTH;
      int mask = 0;
      Direction[] directions = new Direction[]{along.getOpposite(), along, Direction.DOWN, Direction.UP};

      for (int i = 0; i < directions.length; i++) {
         BlockState other = level == null ? null : level.getBlockState(pos.relative(directions[i]));
         if (!entity.getBlockState().getValue(SiftPortalBlock.ACTIVE)) {
            if (other != null && other.is(Blocks.REINFORCED_DEEPSLATE)) {
               mask |= 1 << i;
            }
         } else if (other == null || !other.is(ModBlocks.SIFT_PORTAL) || other.getValue(SiftPortalBlock.AXIS) != axis) {
            mask |= 1 << i;
         }
      }

      return mask;
   }

   public static final class State extends BlockEntityRenderState {
      boolean axisX;
      boolean positive;
      boolean active;
      int worldU;
      int worldY;
      int edges;
      int quality;
      double seconds;
   }
}
