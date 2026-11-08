package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.StalkerEntity;
import net.minecraft.resources.Identifier;

public final class StalkerModel extends GeoModel<StalkerEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/stalker");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/stalker/stalker.png");
   }

   public Identifier getAnimationResource(StalkerEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/stalker");
   }
}
