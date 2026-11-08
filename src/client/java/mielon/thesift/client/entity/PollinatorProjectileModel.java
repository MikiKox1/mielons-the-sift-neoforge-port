package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.PollinatorProjectileEntity;
import net.minecraft.resources.Identifier;

public final class PollinatorProjectileModel extends GeoModel<PollinatorProjectileEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator_projectile");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/pollinator/pollinator_projectile.png");
   }

   public Identifier getAnimationResource(PollinatorProjectileEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator_projectile");
   }
}
