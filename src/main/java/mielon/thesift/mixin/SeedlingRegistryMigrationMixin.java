package mielon.thesift.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MappedRegistry.class)
public abstract class SeedlingRegistryMigrationMixin<T> {
   @Shadow
   public abstract ResourceKey<? extends Registry<T>> key();

   @ModifyVariable(
      method = {
            "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;",
            "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;",
            "containsKey(Lnet/minecraft/resources/Identifier;)Z"
      },
      at = @At("HEAD"),
      argsOnly = true,
      require = 1
   )
   private Identifier theSift$upgradeIdentifier(Identifier id) {
      return this.theSift$remapLegacyId(id);
   }

   @ModifyVariable(
      method = {
            "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
      },
      at = @At("HEAD"),
      argsOnly = true,
      require = 1
   )
   private ResourceKey<T> theSift$upgradeKey(ResourceKey<T> key) {
      Identifier id = this.theSift$remapLegacyId(key.identifier());
      return id.equals(key.identifier()) ? key : ResourceKey.create(this.key(), id);
   }

   @Unique
   private Identifier theSift$remapLegacyId(Identifier id) {
      if (id != null && id.getNamespace().equals("the_sift")) {
         String path = id.getPath();
         if ((this.key().equals(Registries.BLOCK) || this.key().equals(Registries.ITEM)) && path.equals("overgrown_lotus")) {
            return Identifier.fromNamespaceAndPath("the_sift", "blue_saucer_bloom");
         } else if (this.key().equals(Registries.ENTITY_TYPE) && path.equals("sifter")) {
            return Identifier.fromNamespaceAndPath("the_sift", "seedling");
         } else if (this.key().equals(Registries.ITEM) && path.equals("sifter_spawn_egg")) {
            return Identifier.fromNamespaceAndPath("the_sift", "seedling_spawn_egg");
         } else {
            return this.key().equals(Registries.SOUND_EVENT) && path.startsWith("entity.sifter.")
               ? Identifier.fromNamespaceAndPath("the_sift", "entity.seedling." + path.substring("entity.sifter.".length()))
               : id;
         }
      } else {
         return id;
      }
   }
}
