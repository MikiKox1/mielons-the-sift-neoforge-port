package mielon.thesift.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import mielon.thesift.block.entity.IchorCauldronBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.Vec3;

public final class IchorCauldronRenderer implements BlockEntityRenderer<IchorCauldronBlockEntity, IchorCauldronRenderer.State> {
   public IchorCauldronRenderer.State createRenderState() {
      return new IchorCauldronRenderer.State();
   }

   public void extractRenderState(IchorCauldronBlockEntity entity, IchorCauldronRenderer.State state, float partial, Vec3 camera, CrumblingOverlay overlay) {
      BlockEntityRenderState.extractBase(entity, state, overlay);
      state.x = entity.getBlockPos().getX();
      state.z = entity.getBlockPos().getZ();
      state.height = (6 + 3 * entity.getBlockState().getValue(LayeredCauldronBlock.LEVEL)) / 16.0F;
      state.light = state.lightCoords;
   }

   public void submit(IchorCauldronRenderer.State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
      int x = state.x;
      int z = state.z;
      int light = state.light;
      float height = state.height;
      collector.submitCustomGeometry(
         poses, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> draw(pose, buffer, x, z, height, light)
      );
   }

   private static void draw(Pose pose, VertexConsumer buffer, int worldX, int worldZ, float height, int light) {
      TextureAtlasSprite sprite = Minecraft.getInstance()
         .getAtlasManager()
         .get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.fromNamespaceAndPath("the_sift", "block/ichor_still")));
      IchorQuadUv uv = new IchorQuadUv(worldX, 0, worldZ);
      float[] xs = new float[]{0.125F, 0.125F, 0.875F, 0.875F};
      float[] zs = new float[]{0.125F, 0.875F, 0.875F, 0.125F};

      for (int i = 0; i < 4; i++) {
         uv.blockVertex(i, xs[i], height, zs[i]);
      }

      for (int i = 0; i < 4; i++) {
         float u = sprite.getU(uv.u(i));
         float v = sprite.getV(uv.v(i));
         buffer.addVertex(pose.pose(), xs[i], height, zs[i])
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0.0F, 1.0F, 0.0F);
      }
   }

   public static final class State extends BlockEntityRenderState {
      int x;
      int z;
      int light;
      float height;
   }
}
