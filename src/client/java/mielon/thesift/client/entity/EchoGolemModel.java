package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.EchoGolemEntity;
import net.minecraft.resources.Identifier;

public final class EchoGolemModel extends GeoModel<EchoGolemEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/echo_golem");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/echo_golem/echo_golem.png");
   }

   public Identifier getAnimationResource(EchoGolemEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/echo_golem");
   }
}
