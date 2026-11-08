package mielon.thesift.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.warden.Warden;

public final class SculkerFamily {
   public static final TagKey<EntityType<?>> SCULKERS = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("the_sift", "sculkers"));

   private SculkerFamily() {
   }

   public static boolean isSifterEnemy(Entity entity) {
      return entity instanceof ScavengerEntity || entity instanceof StalkerEntity || entity instanceof DarkSnifferEntity;
   }

   public static boolean contains(Entity entity) {
      return entity instanceof MonarchEntity
         || entity instanceof StalkerEntity
         || entity instanceof ScavengerEntity
         || entity instanceof DarkSnifferEntity
         || entity instanceof Warden
         || entity != null && entity.getType().builtInRegistryHolder().is(SCULKERS);
   }
}
