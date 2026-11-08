package mielon.thesift.painting;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModPaintings {
   public static final ResourceKey<PaintingVariant> MONARCH = ResourceKey.create(
      Registries.PAINTING_VARIANT, Identifier.fromNamespaceAndPath("the_sift", "monarch")
   );

   private ModPaintings() {
   }

   public static ItemStack monarch(Provider registries) {
      ItemStack stack = new ItemStack(Items.PAINTING);
      stack.set(DataComponents.PAINTING_VARIANT, registries.lookupOrThrow(Registries.PAINTING_VARIANT).getOrThrow(MONARCH));
      return stack;
   }
}
