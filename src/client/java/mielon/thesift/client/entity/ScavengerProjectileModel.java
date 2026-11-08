package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.ScavengerProjectileEntity;
import net.minecraft.resources.Identifier;

public final class ScavengerProjectileModel extends GeoModel<ScavengerProjectileEntity> {
   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator_projectile");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "textures/entity/scavenger/scavenger_projectile.png");
   }

   public Identifier getAnimationResource(ScavengerProjectileEntity animatable) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator_projectile");
   }
}
