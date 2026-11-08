package mielon.thesift.mixin.compat;

import java.util.ArrayList;
import java.util.List;
import mielon.thesift.compat.easymagic.CharoiteEnchantmentMenuBridge;
import mielon.thesift.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapts Charoite enchanting to Easy Magic's three-slot container and custom
 * preview recalculation path.
 *
 * <p>The target is optional and named as a string on purpose. The Sift therefore
 * keeps loading when Easy Magic is absent, while the verified 26.2 menu can still
 * share the vanilla Charoite enchant application and advancement hooks.</p>
 */
@Pseudo
@Mixin(targets = "fuzs.easymagic.common.world.inventory.ModEnchantmentMenu", remap = false)
public abstract class EasyMagicEnchantmentMenuMixin {
   @Shadow(remap = false)
   @Final
   public List<List<EnchantmentInstance>> clues;

   @Inject(method = "updateLevelsAndClues", at = @At("HEAD"), require = 0, remap = false)
   private void theSift$beginEasyMagicRecalculation(RegistryAccess registries, ItemStack stack, int enchantingPower, CallbackInfo ci) {
      ((CharoiteEnchantmentMenuBridge)(Object)this).theSift$setCharoiteCostsActive(false);
   }

   @Inject(method = "resetLevelsAndClues", at = @At("HEAD"), require = 0, remap = false)
   private void theSift$clearEasyMagicCharoitePreview(CallbackInfo ci) {
      ((CharoiteEnchantmentMenuBridge)(Object)this).theSift$setCharoiteCostsActive(false);
   }

   @Inject(method = "updateLevelsAndClues", at = @At("RETURN"), require = 0, remap = false)
   private void theSift$applyEasyMagicCharoitePreview(RegistryAccess registries, ItemStack stack, int enchantingPower, CallbackInfo ci) {
      EnchantmentMenu menu = (EnchantmentMenu)(Object)this;
      if (!menu.getSlot(1).getItem().is(ModItems.CHAROITE)) {
         return;
      }

      for (int slot = 0; slot < menu.costs.length; slot++) {
         if (menu.costs[slot] > 0) {
            menu.costs[slot] += 20;
         }

         List<EnchantmentInstance> clue = this.clues.get(slot);
         if (!clue.isEmpty()) {
            this.clues.set(slot, theSift$boostEnchantments(clue));
         }
      }

      ((CharoiteEnchantmentMenuBridge)(Object)this).theSift$setCharoiteCostsActive(true);
   }

   @Unique
   private static List<EnchantmentInstance> theSift$boostEnchantments(List<EnchantmentInstance> enchantments) {
      List<EnchantmentInstance> boosted = new ArrayList<>(enchantments.size());
      for (EnchantmentInstance enchantment : enchantments) {
         Enchantment value = enchantment.enchantment().value();
         int level = value.getMaxLevel() > 1 ? enchantment.level() + 1 : enchantment.level();
         boosted.add(new EnchantmentInstance(enchantment.enchantment(), level));
      }
      return boosted;
   }
}
