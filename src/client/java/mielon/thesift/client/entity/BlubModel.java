package mielon.thesift.client.entity;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import mielon.thesift.entity.BlubEntity;
import mielon.thesift.entity.BlubVariant;
import net.minecraft.resources.Identifier;

public final class BlubModel extends GeoModel<BlubEntity> {
   private static final Identifier NORMAL_TEXTURE = id("textures/entity/blub/blub.png");
   private static final Identifier TAMED_TEXTURE = id("textures/entity/blub/tamed_blub.png");
   private static final Identifier MIELON_TEXTURE = id("textures/entity/blub/blub_mielon.png");
   private static final Identifier MIELON_TAMED_TEXTURE = id("textures/entity/blub/blub_mielon_tamed.png");
   private static final Identifier CARAPACE_TEXTURE = id("textures/entity/blub/blub_carapace.png");
   private static final Identifier CARAPACE_TAMED_TEXTURE = id("textures/entity/blub/blub_carapace_tamed.png");
   private static final Identifier MEADOWS_TEXTURE = id("textures/entity/blub/blub_meadows.png");
   private static final Identifier MEADOWS_TAMED_TEXTURE = id("textures/entity/blub/blub_meadows_tamed.png");

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("the_sift", path);
   }

   @Override
   public Identifier getModelResource(GeoRenderState state) {
      BlubEntity blub = state.getGeckolibData(BlubRenderer.BLUB_ENTITY);
      return id("entity/blub" + suffix(blub));
   }

   @Override
   public Identifier getTextureResource(GeoRenderState state) {
      BlubEntity blub = state.getGeckolibData(BlubRenderer.BLUB_ENTITY);
      if (blub != null && blub.isMielonAppearance()) {
         return blub.isTame() ? MIELON_TAMED_TEXTURE : MIELON_TEXTURE;
      }

      BlubVariant variant = blub == null ? BlubVariant.DEFAULT : blub.getVariant();

      return switch (variant) {
         case CARAPACE -> blub.isTame() ? CARAPACE_TAMED_TEXTURE : CARAPACE_TEXTURE;
         case MEADOWS -> blub.isTame() ? MEADOWS_TAMED_TEXTURE : MEADOWS_TEXTURE;
         default -> blub != null && blub.isTame() ? TAMED_TEXTURE : NORMAL_TEXTURE;
      };
   }

   private static String suffix(BlubEntity blub) {
      return blub == null ? "" : blub.getVisualVariant().resourceSuffix();
   }

   public Identifier getAnimationResource(BlubEntity animatable) {
      return id("entity/blub" + suffix(animatable));
   }
}
