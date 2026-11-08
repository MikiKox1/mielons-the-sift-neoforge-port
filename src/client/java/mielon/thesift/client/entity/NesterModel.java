package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.NesterEntity;
import net.minecraft.resources.Identifier;

public final class NesterModel extends GeoModel<NesterEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/nester");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/nester/nester.png");
   }

   public Identifier getAnimationResource(NesterEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/nester");
   }
}
