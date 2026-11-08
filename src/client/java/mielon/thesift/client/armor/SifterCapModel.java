package mielon.thesift.client.armor;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.item.SifterCapItem;
import net.minecraft.resources.Identifier;

public final class SifterCapModel extends GeoModel<SifterCapItem> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "armor/sifter_cap");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/equipment/humanoid/sifter_cap.png");
   }

   public Identifier getAnimationResource(SifterCapItem animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "armor/sifter_cap");
   }
}
