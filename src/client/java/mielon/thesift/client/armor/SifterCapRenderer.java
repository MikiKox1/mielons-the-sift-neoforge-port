package mielon.thesift.client.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Uses the vanilla equipment layer for the original texture, trim and glint. */
public final class SifterCapRenderer extends Model<HumanoidRenderState> {
   private final HumanoidModel<HumanoidRenderState> armor;
   private final ModelPart head;

   @SuppressWarnings("unchecked")
   public SifterCapRenderer(HumanoidModel<?> armor) {
      super(SifterCapModel.bake(), armor.renderType());
      this.armor = (HumanoidModel<HumanoidRenderState>) armor;
      this.head = this.root().getChild("head");
   }

   @Override
   public void setupAnim(HumanoidRenderState state) {
      this.armor.setupAnim(state);
      ModelPart armorRoot = this.armor.root();
      ModelPart armorHead = this.armor.getHead();
      this.root().loadPose(armorRoot.storePose());
      this.root().visible = armorRoot.visible;
      this.head.loadPose(armorHead.storePose());
      this.head.visible = armorHead.visible;
   }
}
