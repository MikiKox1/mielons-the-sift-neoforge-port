package mielon.thesift.client.armor;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.GeoArmorRenderer.RenderData;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mielon.thesift.item.SifterCapItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.client.resources.palette.PalettedTextureManager.Handle;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public final class SifterCapRenderer extends GeoArmorRenderer<SifterCapItem, HumanoidRenderState> {
   private static final DataTicket<ItemStack> CAP_STACK = DataTicket.create("sifter_cap_stack", ItemStack.class);

   public SifterCapRenderer() {
      super(new SifterCapModel());
   }

   public RenderType getRenderType(HumanoidRenderState state, Identifier texture) {
      return Boolean.TRUE.equals(state.getGeckolibData(DataTickets.HAS_GLINT))
         ? RenderTypes.armorCutoutNoCullGlint(texture)
         : RenderTypes.armorCutoutNoCull(texture);
   }

   public void addRenderData(SifterCapItem item, RenderData data, HumanoidRenderState state, float partialTick) {
      super.addRenderData(item, data, state, partialTick);
      state.addGeckolibData(CAP_STACK, data.itemStack().copy());
   }

   public void postRenderPass(RenderPassInfo<HumanoidRenderState> pass, SubmitNodeCollector collector) {
      super.postRenderPass(pass, collector);
      if (pass.willRender()) {
         ItemStack stack = (ItemStack)pass.getGeckolibData(CAP_STACK);
         ArmorTrim trim = (ArmorTrim)stack.get(DataComponents.TRIM);
         if (trim != null) {
            Identifier texture = ((TrimPattern)trim.pattern().value()).assetId().withPath(path -> LayerType.HUMANOID.trimAssetPrefix() + "/" + path);
            Handle mapping = Minecraft.getInstance().getPalettedTextureManager().getOrPrepare(texture, ((TrimMaterial)trim.material().value()).paletteId());
            RenderType trimType = RenderTypes.armorTrim(mapping.textureLocation(), ((TrimPattern)trim.pattern().value()).decal());
            PoseStack trimPoses = pass.poseStack();
            trimPoses.pushPose();

            try {
               trimPoses.last().set(pass.getModelRenderMatrixPose());
               collector.order(1)
                  .submitCustomGeometry(
                     pass.poseStack(),
                     trimType,
                     (pose, buffer) -> {
                        PoseStack poses = pass.poseStack();
                        poses.pushPose();

                        try {
                           poses.last().set(pose);
                           VertexConsumer trimBuffer = mapping.wrap(buffer);
                           pass.renderPosed(
                              () -> pass.model()
                                 .getBone("armorHead")
                                 .ifPresent(bone -> bone.positionAndRender(pass, trimBuffer, pass.packedLight(), OverlayTexture.NO_OVERLAY, -1))
                           );
                        } finally {
                           poses.popPose();
                        }
                     }
                  );
            } finally {
               trimPoses.popPose();
            }
         }
      }
   }

   public static GeoRenderProvider provider() {
      return new GeoRenderProvider() {
         private final SifterCapRenderer renderer = new SifterCapRenderer();

         public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
            return this.renderer;
         }
      };
   }
}
