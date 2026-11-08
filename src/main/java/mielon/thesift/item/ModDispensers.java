package mielon.thesift.item;

import mielon.thesift.entity.ModEntities;
import mielon.thesift.fluid.ModFluids;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;

public final class ModDispensers {
   private ModDispensers() {
   }

   public static void register() {
      DispenserBlock.registerProjectileBehavior(ModItems.POLLINATOR_DROOL);
      DispenserBlock.registerProjectileBehavior(ModItems.ICHOR_SNOWBALL);
      DispenserBlock.registerBehavior(ModFluids.ICHOR_BUCKET, DispenserBlock.DISPENSER_REGISTRY.get(Items.WATER_BUCKET));
      DispenserBlock.registerBehavior(ModItems.OVERGROWN_WILLOW_BOAT, new BoatDispenseItemBehavior(ModEntities.OVERGROWN_WILLOW_BOAT));
      DispenserBlock.registerBehavior(ModItems.OVERGROWN_WILLOW_CHEST_BOAT, new BoatDispenseItemBehavior(ModEntities.OVERGROWN_WILLOW_CHEST_BOAT));
      DispenserBlock.registerBehavior(ModItems.ASH_WILLOW_BOAT, new BoatDispenseItemBehavior(ModEntities.ASH_WILLOW_BOAT));
      DispenserBlock.registerBehavior(ModItems.ASH_WILLOW_CHEST_BOAT, new BoatDispenseItemBehavior(ModEntities.ASH_WILLOW_CHEST_BOAT));
   }
}
