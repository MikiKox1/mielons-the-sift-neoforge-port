package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.ScavengerEntity;
import net.minecraft.resources.Identifier;

public final class ScavengerModel extends GeoModel<ScavengerEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/scavenger");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/scavenger/scavenger.png");
   }

   public Identifier getAnimationResource(ScavengerEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/scavenger");
   }
}
