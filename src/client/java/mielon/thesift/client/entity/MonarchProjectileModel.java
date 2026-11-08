package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.MonarchProjectileEntity;
import net.minecraft.resources.Identifier;

public final class MonarchProjectileModel extends GeoModel<MonarchProjectileEntity> {
   private final boolean variant;

   public MonarchProjectileModel(boolean variant) {
      this.variant = variant;
   }

   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator_projectile");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return this.variant
         ? Identifier.fromNamespaceAndPath("the_sift", "textures/entity/monarch/monarch_summon_projectile.png")
         : Identifier.fromNamespaceAndPath("the_sift", "textures/entity/monarch/monarch_explosion_projectile.png");
   }

   public Identifier getAnimationResource(MonarchProjectileEntity entity) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/pollinator_projectile");
   }
}
