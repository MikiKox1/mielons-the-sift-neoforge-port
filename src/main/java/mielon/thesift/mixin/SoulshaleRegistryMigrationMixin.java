package mielon.thesift.mixin;

import mielon.thesift.compat.SoulshaleIds;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MappedRegistry.class)
public abstract class SoulshaleRegistryMigrationMixin<T> {
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
   private Identifier theSift$upgradeSoulshaleId(Identifier id) {
      return this.theSift$soulshaleId(id);
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
   private ResourceKey<T> theSift$upgradeSoulshaleKey(ResourceKey<T> key) {
      Identifier id = this.theSift$soulshaleId(key.identifier());
      return id.equals(key.identifier()) ? key : ResourceKey.create(this.key(), id);
   }

   @Unique
   private Identifier theSift$soulshaleId(Identifier id) {
      return SoulshaleIds.upgrade(id);
   }
}
