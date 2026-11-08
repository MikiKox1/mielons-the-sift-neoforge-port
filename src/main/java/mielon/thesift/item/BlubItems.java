package mielon.thesift.item;

import mielon.thesift.entity.ModEntities;
import mielon.thesift.platform.ModRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item.Properties;

public final class BlubItems {
   public static final ResourceKey<Item> BLUB_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "blub_spawn_egg")
   );
   public static final Item BLUB_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, BLUB_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(BLUB_SPAWN_EGG_KEY).spawnEgg(ModEntities.BLUB))
   );

   private BlubItems() {
   }

   public static void initialize() {
   }
}
