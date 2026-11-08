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
public abstract class SiftBiomeMigrationMixin<T> {
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
   private Identifier theSift$upgradeBiomeId(Identifier id) {
      return this.theSift$survivingBiome(id);
   }

   @ModifyVariable(
      method = {
            "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;",
            "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;",
            "getOrCreateHolderOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;",
            "containsKey(Lnet/minecraft/resources/ResourceKey;)Z"
      },
      at = @At("HEAD"),
      argsOnly = true,
      require = 1
   )
   private ResourceKey<T> theSift$upgradeBiomeKey(ResourceKey<T> key) {
      Identifier id = this.theSift$survivingBiome(key.identifier());
      return id.equals(key.identifier()) ? key : ResourceKey.create(this.key(), id);
   }

   @Unique
   private Identifier theSift$survivingBiome(Identifier id) {
      if (id != null && this.key().equals(Registries.BIOME) && id.getNamespace().equals("the_sift")) {
         String target = switch (id.getPath()) {
            case "overgrown_slopes", "overgrown_forest_slopes", "overgrown_peaks" -> "overgrown_clearing";
            case "siftslate_slopes", "siftslate_peaks" -> "sift_wastes";
            case "sift_deep_dark" -> "corrupted_sift";
            default -> null;
         };
         return target == null ? id : Identifier.fromNamespaceAndPath("the_sift", target);
      } else {
         return id;
      }
   }
}
