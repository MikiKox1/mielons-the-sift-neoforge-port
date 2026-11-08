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
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public final class SifterCapRenderer extends GeoArmorRenderer<SifterCapItem, HumanoidRenderState> {
   private static final DataTicket<ItemStack> CAP_STACK = DataTicket.create("sifter_cap_stack", ItemStack.class);

   public SifterCapRenderer() {
      super(new SifterCapModel());
   }

   @Override
   public RenderType getRenderType(HumanoidRenderState state, Identifier texture) {
      return RenderTypes.armorCutoutNoCull(texture);
   }

   public void addRenderData(SifterCapItem item, RenderData data, HumanoidRenderState state, float partialTick) {
      super.addRenderData(item, data, state, partialTick);
      state.addGeckolibData(CAP_STACK, data.itemStack().copy());
   }

   @Override
   public void submitRenderTasks(RenderPassInfo<HumanoidRenderState> pass, OrderedSubmitNodeCollector collector, RenderType renderType) {
      super.submitRenderTasks(pass, collector, renderType);
      if (renderType != null) {
         if (Boolean.TRUE.equals(pass.getGeckolibData(DataTickets.HAS_GLINT))) {
            super.submitRenderTasks(pass, collector, RenderTypes.armorEntityGlint());
         }
      }
   }

   @Override
   public void postRenderPass(RenderPassInfo<HumanoidRenderState> pass, SubmitNodeCollector collector) {
      super.postRenderPass(pass, collector);
      if (pass.willRender()) {
         ItemStack stack = pass.getGeckolibData(CAP_STACK);
         ArmorTrim trim = stack.get(DataComponents.TRIM);
         if (trim != null) {
            Identifier spriteId = trim.layerAssetId(LayerType.HUMANOID.trimAssetPrefix(), stack.get(DataComponents.EQUIPPABLE).assetId().orElseThrow());
            TextureAtlasSprite mapping = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(Sheets.ARMOR_TRIMS_SHEET).getSprite(spriteId);
            RenderType trimType = Sheets.armorTrimsSheet(trim.pattern().value().decal());
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

         @Override
         public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
            return this.renderer;
         }
      };
   }
}
