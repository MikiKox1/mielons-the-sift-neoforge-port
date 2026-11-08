package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.MonarchEntity;
import net.minecraft.resources.Identifier;

public final class MonarchModel extends GeoModel<MonarchEntity> {
   private final boolean variant;

   public MonarchModel(boolean variant) {
      this.variant = variant;
   }

   @Override
   public Identifier getModelResource(GeoRenderState state) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/monarch");
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      return this.variant
         ? Identifier.fromNamespaceAndPath("the_sift", "textures/entity/monarch/monarch_echo.png")
         : Identifier.fromNamespaceAndPath("the_sift", "textures/entity/monarch/monarch.png");
   }

   public Identifier getAnimationResource(MonarchEntity entity) {
      return Identifier.fromNamespaceAndPath("the_sift", "entity/monarch");
   }
}
