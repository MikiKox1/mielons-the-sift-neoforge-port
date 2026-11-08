package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.SlabberEntity;
import net.minecraft.resources.Identifier;

public final class SlabberModel extends GeoModel<SlabberEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/slabber");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/slabber/slabber.png");
   }

   public Identifier getAnimationResource(SlabberEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/slabber");
   }
}
