package mielon.thesift.enchantment;

import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public final class SoulAbsorption {
   public static final ResourceKey<Enchantment> KEY = ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("the_sift", "soul_absorption"));

   private SoulAbsorption() {
   }

   public static int level(LivingEntity entity) {
      Reference<Enchantment> enchantment = entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
      return EnchantmentHelper.getItemEnchantmentLevel(enchantment, entity.getItemBySlot(EquipmentSlot.CHEST));
   }

   public static ItemStack book(Provider registries, int level) {
      Reference<Enchantment> enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(KEY);
      return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
   }
}
