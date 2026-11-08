package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.SeedlingEntity;
import net.minecraft.resources.Identifier;

public final class SeedlingModel extends GeoModel<SeedlingEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/seedling");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/seedling/seedling.png");
   }

   public Identifier getAnimationResource(SeedlingEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/seedling");
   }
}
