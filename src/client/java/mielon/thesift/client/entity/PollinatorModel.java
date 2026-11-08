package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.PollinatorEntity;
import net.minecraft.resources.Identifier;

public final class PollinatorModel extends GeoModel<PollinatorEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/pollinator/pollinator.png");
   }

   public Identifier getAnimationResource(PollinatorEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator");
   }
}
