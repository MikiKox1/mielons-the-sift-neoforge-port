package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.NuzzleEntity;
import net.minecraft.resources.Identifier;

public final class NuzzleModel extends GeoModel<NuzzleEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/nuzzle");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/nuzzle/nuzzle.png");
   }

   public Identifier getAnimationResource(NuzzleEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/nuzzle");
   }
}
