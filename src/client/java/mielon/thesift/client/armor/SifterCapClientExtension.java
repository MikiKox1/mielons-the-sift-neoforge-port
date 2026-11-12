package mielon.thesift.client.armor;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Native NeoForge hook; the normal equipment renderer owns dye, trim and glint. */
public final class SifterCapClientExtension implements IClientItemExtensions {
   private final Map<HumanoidModel<?>, SifterCapRenderer> models = new IdentityHashMap<>();

   @Override
   public Model getGenericArmorModel(ItemStack stack, LayerType layerType, Model original) {
      return original instanceof HumanoidModel<?> humanoid
         ? models.computeIfAbsent(humanoid, SifterCapRenderer::new)
         : original;
   }
}
