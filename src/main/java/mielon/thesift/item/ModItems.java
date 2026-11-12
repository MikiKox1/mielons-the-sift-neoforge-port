package mielon.thesift.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousDeepslateBlock;
import mielon.thesift.enchantment.SoulAbsorption;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.painting.ModPaintings;
import mielon.thesift.platform.ModRegistration;
import mielon.thesift.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public final class ModItems {
   public static final ResourceKey<Item> SINGER_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "singer_spawn_egg")
   );
   public static final Item SINGER_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, SINGER_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(SINGER_SPAWN_EGG_KEY).spawnEgg(ModEntities.SINGER))
   );
   public static final ResourceKey<Item> ECHO_GOLEM_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "echo_golem_spawn_egg")
   );
   public static final Item ECHO_GOLEM_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, ECHO_GOLEM_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(ECHO_GOLEM_SPAWN_EGG_KEY).spawnEgg(ModEntities.ECHO_GOLEM))
   );
   public static final ResourceKey<Item> DARK_SNIFFER_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "dark_sniffer_spawn_egg")
   );
   public static final Item DARK_SNIFFER_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM,
      DARK_SNIFFER_SPAWN_EGG_KEY,
      new SpawnEggItem(new Properties().setId(DARK_SNIFFER_SPAWN_EGG_KEY).spawnEgg(ModEntities.DARK_SNIFFER))
   );
   public static final ResourceKey<Item> SEEDLING_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "seedling_spawn_egg")
   );
   public static final Item SEEDLING_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, SEEDLING_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(SEEDLING_SPAWN_EGG_KEY).spawnEgg(ModEntities.SEEDLING))
   );
   public static final ResourceKey<Item> SCAVENGER_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "scavenger_spawn_egg")
   );
   public static final Item SCAVENGER_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, SCAVENGER_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(SCAVENGER_SPAWN_EGG_KEY).spawnEgg(ModEntities.SCAVENGER))
   );
   public static final ResourceKey<Item> STALKER_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "stalker_spawn_egg")
   );
   public static final Item STALKER_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, STALKER_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(STALKER_SPAWN_EGG_KEY).spawnEgg(ModEntities.STALKER))
   );
   public static final ResourceKey<Item> NUZZLE_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "nuzzle_spawn_egg")
   );
   public static final Item NUZZLE_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, NUZZLE_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(NUZZLE_SPAWN_EGG_KEY).spawnEgg(ModEntities.NUZZLE))
   );
   public static final ResourceKey<Item> SLABBER_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "slabber_spawn_egg")
   );
   public static final Item SLABBER_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, SLABBER_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(SLABBER_SPAWN_EGG_KEY).spawnEgg(ModEntities.SLABBER))
   );
   public static final ResourceKey<Item> NESTER_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "nester_spawn_egg")
   );
   public static final Item NESTER_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, NESTER_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(NESTER_SPAWN_EGG_KEY).spawnEgg(ModEntities.NESTER))
   );
   public static final ResourceKey<Item> POLLINATOR_SPAWN_EGG_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "pollinator_spawn_egg")
   );
   public static final Item POLLINATOR_SPAWN_EGG = ModRegistration.register(
      BuiltInRegistries.ITEM, POLLINATOR_SPAWN_EGG_KEY, new SpawnEggItem(new Properties().setId(POLLINATOR_SPAWN_EGG_KEY).spawnEgg(ModEntities.POLLINATOR))
   );
   public static final ResourceKey<Item> ASH_WILLOW_BOAT_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "ash_willow_boat")
   );
   public static final Item ASH_WILLOW_BOAT = ModRegistration.register(
      BuiltInRegistries.ITEM, ASH_WILLOW_BOAT_KEY, new BoatItem(ModEntities.ASH_WILLOW_BOAT, new Properties().stacksTo(1).setId(ASH_WILLOW_BOAT_KEY))
   );
   public static final ResourceKey<Item> ASH_WILLOW_CHEST_BOAT_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "ash_willow_chest_boat")
   );
   public static final Item ASH_WILLOW_CHEST_BOAT = ModRegistration.register(
      BuiltInRegistries.ITEM,
      ASH_WILLOW_CHEST_BOAT_KEY,
      new BoatItem(ModEntities.ASH_WILLOW_CHEST_BOAT, new Properties().stacksTo(1).setId(ASH_WILLOW_CHEST_BOAT_KEY))
   );
   public static final ResourceKey<Item> OVERGROWN_WILLOW_BOAT_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "overgrown_willow_boat")
   );
   public static final Item OVERGROWN_WILLOW_BOAT = ModRegistration.register(
      BuiltInRegistries.ITEM,
      OVERGROWN_WILLOW_BOAT_KEY,
      new BoatItem(ModEntities.OVERGROWN_WILLOW_BOAT, new Properties().stacksTo(1).setId(OVERGROWN_WILLOW_BOAT_KEY))
   );
   public static final ResourceKey<Item> OVERGROWN_WILLOW_CHEST_BOAT_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "overgrown_willow_chest_boat")
   );
   public static final Item OVERGROWN_WILLOW_CHEST_BOAT = ModRegistration.register(
      BuiltInRegistries.ITEM,
      OVERGROWN_WILLOW_CHEST_BOAT_KEY,
      new BoatItem(ModEntities.OVERGROWN_WILLOW_CHEST_BOAT, new Properties().stacksTo(1).setId(OVERGROWN_WILLOW_CHEST_BOAT_KEY))
   );
   public static final TagKey<Item> SIFTITE_REPAIR_MATERIALS = TagKey.create(
      Registries.ITEM, Identifier.fromNamespaceAndPath("the_sift", "siftite_repair_materials")
   );
   public static final TagKey<Item> SIFTITE_ITEMS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("the_sift", "siftite_items"));
   public static final ToolMaterial SIFTITE_TOOL_MATERIAL = new ToolMaterial(
      BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
      ToolMaterial.NETHERITE.durability() + 470,
      ToolMaterial.NETHERITE.speed() * 1.2F,
      ToolMaterial.NETHERITE.attackDamageBonus() + 1.0F,
      ToolMaterial.NETHERITE.enchantmentValue(),
      SIFTITE_REPAIR_MATERIALS
   );
   public static final ResourceKey<EquipmentAsset> SIFTITE_EQUIPMENT_ASSET = ResourceKey.create(
      EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("the_sift", "siftite")
   );
   public static final ArmorMaterial SIFTITE_ARMOR_MATERIAL = new ArmorMaterial(
      ArmorMaterials.NETHERITE.durability() + 4,
      new EnumMap<>(Map.of(
         ArmorType.BOOTS, 3,
         ArmorType.LEGGINGS, 6,
         ArmorType.CHESTPLATE, 8,
         ArmorType.HELMET, 3,
         ArmorType.BODY, 19
      )),
      ArmorMaterials.NETHERITE.enchantmentValue(),
      BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.SIFTITE_ARMOR_EQUIP),
      3.0F,
      0.15F,
      SIFTITE_REPAIR_MATERIALS,
      SIFTITE_EQUIPMENT_ASSET
   );
   public static final Item ICHOR_SHARD = registerItem("ichor_shard", new Properties());
   public static final Item CHAROITE = registerItem("charoite", new Properties());
   public static final Item SIFTITE_NUGGET = registerItem("siftite_nugget", new Properties());
   public static final Item SIFTITE_INGOT = registerItem("siftite_ingot", new Properties());
   public static final Item POLLINATOR_DROOL = registerItem("pollinator_drool", PollinatorDroolItem::new, new Properties().stacksTo(16));
   public static final Item ICHOR_SNOWBALL = registerItem("ichor_snowball", IchorSnowballItem::new, new Properties().stacksTo(16));
   public static final Item ICHOR_BOTTLE = registerItem(
      "ichor_bottle",
      IchorBottleItem::new,
      new Properties()
         .stacksTo(1)
         .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.POTION_CONTENTS, true))
         .usingConvertsTo(Items.GLASS_BOTTLE)
         .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
         .component(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(5909123), List.of(), Optional.empty()))
   );
   public static final Item RAW_SLABBER_MEAT = registerItem("raw_slabber_meat", new Properties().food(Foods.BEEF));
   public static final Item COOKED_SLABBER_MEAT = registerItem("cooked_slabber_meat", new Properties().food(Foods.COOKED_BEEF));
   public static final Item BLUBBY_BERRIES = registerItem(
      "blubby_berries", properties -> new BlockItem(ModBlocks.BLUBBY_BERRIES_BUSH, properties), new Properties().food(new FoodProperties(3, 0.6F, false))
   );
   public static final Item RAW_NUZZLE_MEAT = registerItem("raw_nuzzle_meat", new Properties().food(Foods.MUTTON));
   public static final Item COOKED_NUZZLE_MEAT = registerItem("cooked_nuzzle_meat", new Properties().food(Foods.COOKED_MUTTON));
   public static final Item RAW_SIFTER_MEAT = registerItem("raw_sifter_meat", new Properties().food(Foods.PORKCHOP));
   public static final Item COOKED_SIFTER_MEAT = registerItem("cooked_sifter_meat", new Properties().food(Foods.COOKED_PORKCHOP));
   public static final Item SIFT_RIFT = registerItem("sift_rift", SiftRiftItem::new, new Properties().stacksTo(1).rarity(Rarity.EPIC));
   public static final ResourceKey<JukeboxSong> RIFT_JUKEBOX_SONG_KEY = ResourceKey.create(
      Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath("the_sift", "rift")
   );
   public static final Item MUSIC_DISC_RIFT = registerItem(
      "music_disc_rift", new Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(RIFT_JUKEBOX_SONG_KEY)
   );
   public static final ResourceKey<JukeboxSong> WELLSPRING_JUKEBOX_SONG_KEY = ResourceKey.create(
      Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath("the_sift", "wellspring")
   );
   public static final Item MUSIC_DISC_WELLSPRING = registerItem(
      "music_disc_wellspring", new Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(WELLSPRING_JUKEBOX_SONG_KEY)
   );
   public static final ResourceKey<Item> SCULKFLOWER_SEEDS_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "sculkflower_seeds")
   );
   public static final Item SCULKFLOWER_SEEDS = ModRegistration.register(
      BuiltInRegistries.ITEM, SCULKFLOWER_SEEDS_KEY, new BlockItem(ModBlocks.SCULKFLOWER_CROP, new Properties().setId(SCULKFLOWER_SEEDS_KEY))
   );
   public static final Item SIFTITE_SWORD = registerItem("siftite_sword", new Properties().sword(SIFTITE_TOOL_MATERIAL, 3.0F, -2.4F).fireResistant());
   public static final Item SIFTITE_SHOVEL = registerItem("siftite_shovel", new Properties().shovel(SIFTITE_TOOL_MATERIAL, 1.5F, -3.0F).fireResistant());
   public static final Item SIFTITE_PICKAXE = registerItem("siftite_pickaxe", new Properties().pickaxe(SIFTITE_TOOL_MATERIAL, 1.0F, -2.8F).fireResistant());
   public static final Item SIFTITE_AXE = registerItem("siftite_axe", new Properties().axe(SIFTITE_TOOL_MATERIAL, 5.0F, -3.0F).fireResistant());
   public static final Item SIFTITE_HOE = registerItem("siftite_hoe", new Properties().hoe(SIFTITE_TOOL_MATERIAL, -4.0F, 0.0F).fireResistant());
   public static final Item SIFTITE_SPEAR = registerItem(
      "siftite_spear", new Properties().spear(SIFTITE_TOOL_MATERIAL, 1.25F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F).fireResistant()
   );
   public static final Item SIFTER_HIDE = registerItem("sifter_hide", new Properties());
   public static final Item SIFTER_CAP = registerItem(
      "sifter_cap",
      new Properties()
         .humanoidArmor(ArmorMaterials.LEATHER, ArmorType.HELMET)
         .component(
            DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.HEAD)
               .setEquipSound(ArmorMaterials.LEATHER.equipSound())
               .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("the_sift", "sifter_cap")))
               .build()
         )
   );
   public static final Item SIFTITE_HELMET = registerItem(
      "siftite_helmet",
      new Properties()
         .humanoidArmor(SIFTITE_ARMOR_MATERIAL, ArmorType.HELMET)
         .durability(ArmorType.HELMET.getDurability(SIFTITE_ARMOR_MATERIAL.durability()))
         .fireResistant()
   );
   public static final Item SIFTITE_CHESTPLATE = registerItem(
      "siftite_chestplate",
      new Properties()
         .humanoidArmor(SIFTITE_ARMOR_MATERIAL, ArmorType.CHESTPLATE)
         .durability(ArmorType.CHESTPLATE.getDurability(SIFTITE_ARMOR_MATERIAL.durability()))
         .fireResistant()
   );
   public static final Item SIFTITE_LEGGINGS = registerItem(
      "siftite_leggings",
      new Properties()
         .humanoidArmor(SIFTITE_ARMOR_MATERIAL, ArmorType.LEGGINGS)
         .durability(ArmorType.LEGGINGS.getDurability(SIFTITE_ARMOR_MATERIAL.durability()))
         .fireResistant()
   );
   public static final Item SIFTITE_BOOTS = registerItem(
      "siftite_boots",
      new Properties()
         .humanoidArmor(SIFTITE_ARMOR_MATERIAL, ArmorType.BOOTS)
         .durability(ArmorType.BOOTS.getDurability(SIFTITE_ARMOR_MATERIAL.durability()))
         .fireResistant()
   );
   public static final Item SIFTITE_UPGRADE_SMITHING_TEMPLATE = registerItem(
      "siftite_upgrade_smithing_template",
      properties -> new SmithingTemplateItem(
         Component.translatable("item.the_sift.smithing_template.siftite_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
         Component.translatable("item.the_sift.smithing_template.siftite_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
         Component.translatable("item.the_sift.smithing_template.siftite_upgrade.base_slot_description"),
         Component.translatable("item.the_sift.smithing_template.siftite_upgrade.additions_slot_description"),
         List.of(
            Identifier.withDefaultNamespace("container/slot/helmet"),
            Identifier.withDefaultNamespace("container/slot/chestplate"),
            Identifier.withDefaultNamespace("container/slot/leggings"),
            Identifier.withDefaultNamespace("container/slot/boots"),
            Identifier.withDefaultNamespace("container/slot/sword"),
            Identifier.withDefaultNamespace("container/slot/pickaxe"),
            Identifier.withDefaultNamespace("container/slot/axe"),
            Identifier.withDefaultNamespace("container/slot/shovel"),
            Identifier.withDefaultNamespace("container/slot/hoe"),
            Identifier.withDefaultNamespace("container/slot/spear")
         ),
         List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
         properties
      ),
      new Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item MONARCH_MEMBRANE = registerItem("monarch_membrane", new Properties().rarity(Rarity.RARE));
   public static final Item MONARCH_ELYTRA = registerItem(
      "monarch_elytra",
      MonarchElytraItem::new,
      new Properties()
         .durability(432)
         .rarity(Rarity.EPIC)
         .component(DataComponents.GLIDER, Unit.INSTANCE)
         .component(
            DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.CHEST)
               .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
               .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("the_sift", "monarch_elytra")))
               .setDamageOnHurt(false)
               .build()
         )
         .repairable(Items.PHANTOM_MEMBRANE)
   );
   public static final Item MONARCH_UPGRADE_SMITHING_TEMPLATE = registerItem(
      "monarch_upgrade_smithing_template",
      properties -> new SmithingTemplateItem(
         Component.translatable("item.the_sift.smithing_template.monarch_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
         Component.translatable("item.the_sift.smithing_template.monarch_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
         Component.translatable("item.the_sift.smithing_template.monarch_upgrade.base_slot_description"),
         Component.translatable("item.the_sift.smithing_template.monarch_upgrade.additions_slot_description"),
         List.of(Identifier.fromNamespaceAndPath("the_sift", "container/slot/elytra")),
         List.of(Identifier.fromNamespaceAndPath("the_sift", "container/slot/monarch_membrane")),
         properties
      ),
      new Properties().rarity(Rarity.UNCOMMON)
   );
   public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(
      BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath("the_sift", "the_sift")
   );
   public static final CreativeModeTab CREATIVE_TAB = CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.the_sift"))
      .icon(() -> new ItemStack(ModBlocks.DRY_HEALTHY_SCULK_GROWTH_ITEM))
      .displayItems(
         (params, output) -> {
            ItemStack horn = new ItemStack(ModBlocks.SONOROUS_DEEPSLATE_ITEM);
            horn.set(
               DataComponents.BLOCK_STATE,
               new BlockItemStateProperties(Map.of(SonorousDeepslateBlock.MODE.getName(), SonorousDeepslateBlock.Mode.HORN.getSerializedName()))
            );
            output.accept(horn);
            ItemStack note = new ItemStack(ModBlocks.SONOROUS_DEEPSLATE_ITEM);
            note.set(
               DataComponents.BLOCK_STATE,
               new BlockItemStateProperties(Map.of(SonorousDeepslateBlock.MODE.getName(), SonorousDeepslateBlock.Mode.NOTE.getSerializedName()))
            );
            output.accept(note);
            output.accept(ModBlocks.ROYAL_SCULK_ITEM);
            output.accept(ModBlocks.REINFORCED_SOULSHALE_ITEM);
            output.accept(ModBlocks.SOULSHALE_ITEM);
            output.accept(ModBlocks.SOULSHALE_COAL_ORE_ITEM);
            output.accept(ModBlocks.SOULSHALE_DIAMOND_ORE_ITEM);
            output.accept(ModBlocks.SOULSHALE_EMERALD_ORE_ITEM);
            output.accept(ModBlocks.SOULSHALE_CHAROITE_ORE_ITEM);
            output.accept(ModBlocks.SOULSHALE_SIFTITE_ORE_ITEM);
            output.accept(ModBlocks.SOULSHALE_STAIRS_ITEM);
            output.accept(ModBlocks.SOULSHALE_SLAB_ITEM);
            output.accept(ModBlocks.SOULSHALE_WALL_ITEM);
            output.accept(ModBlocks.POLISHED_SOULSHALE_ITEM);
            output.accept(ModBlocks.POLISHED_SOULSHALE_STAIRS_ITEM);
            output.accept(ModBlocks.POLISHED_SOULSHALE_SLAB_ITEM);
            output.accept(ModBlocks.POLISHED_SOULSHALE_WALL_ITEM);
            output.accept(ModBlocks.SOULSHALE_BRICKS_ITEM);
            output.accept(ModBlocks.SOULSHALE_BRICK_STAIRS_ITEM);
            output.accept(ModBlocks.SOULSHALE_BRICK_SLAB_ITEM);
            output.accept(ModBlocks.SOULSHALE_BRICK_WALL_ITEM);
            output.accept(ModBlocks.CHISELED_SOULSHALE_ITEM);
            output.accept(ModBlocks.SOULSHALE_TOTEM_ITEM);
            output.accept(ModBlocks.SOULSHALE_GROWTH_ITEM);
            output.accept(ModBlocks.REINFORCED_SIFTSLATE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_COAL_ORE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_DIAMOND_ORE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_EMERALD_ORE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_CHAROITE_ORE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_SIFTITE_ORE_ITEM);
            output.accept(ModBlocks.COBBLED_SIFTSLATE_ITEM);
            output.accept(ModBlocks.COBBLED_SIFTSLATE_STAIRS_ITEM);
            output.accept(ModBlocks.COBBLED_SIFTSLATE_SLAB_ITEM);
            output.accept(ModBlocks.COBBLED_SIFTSLATE_WALL_ITEM);
            output.accept(ModBlocks.SIFTSLATE_BRICKS_ITEM);
            output.accept(ModBlocks.SIFTSLATE_BRICK_STAIRS_ITEM);
            output.accept(ModBlocks.SIFTSLATE_BRICK_SLAB_ITEM);
            output.accept(ModBlocks.SIFTSLATE_BRICK_WALL_ITEM);
            output.accept(ModBlocks.CHISELED_SIFTSLATE_ITEM);
            output.accept(ModBlocks.SIFTSLATE_GROWTH_ITEM);
            output.accept(ModBlocks.SIFT_RUBBLE_ITEM);
            output.accept(ModBlocks.HEALTHY_SCULK_PORES_ITEM);
            output.accept(ModBlocks.HEALTHY_SCULK_CARTILAGE_ITEM);
            output.accept(ModBlocks.HEALTHY_SCULK_ITEM);
            output.accept(ModBlocks.DRY_HEALTHY_SCULK_ITEM);
            output.accept(ModBlocks.DRY_HEALTHY_SCULK_GROWTH_ITEM);
            output.accept(ModBlocks.CORAL_SOIL_ITEM);
            output.accept(ModBlocks.CORAL_GROWTH_ITEM);
            output.accept(ModBlocks.BRIGHT_CORAL_GROWTH_ITEM);
            output.accept(ModBlocks.ICHOR_SNOW_BLOCK_ITEM);
            output.accept(ModBlocks.ICHOR_SNOW_ITEM);
            output.accept(ModBlocks.SOUL_BLOCK_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_LOG_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_WOOD_ITEM);
            output.accept(ModBlocks.STRIPPED_OVERGROWN_WILLOW_LOG_ITEM);
            output.accept(ModBlocks.STRIPPED_OVERGROWN_WILLOW_WOOD_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_PLANKS_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_STAIRS_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_SLAB_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_FENCE_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_FENCE_GATE_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_DOOR_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_TRAPDOOR_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_PRESSURE_PLATE_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_BUTTON_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_SHELF_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_SIGN_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_HANGING_SIGN_ITEM);
            output.accept(OVERGROWN_WILLOW_BOAT);
            output.accept(OVERGROWN_WILLOW_CHEST_BOAT);
            output.accept(ModBlocks.ASH_WILLOW_LOG_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_WOOD_ITEM);
            output.accept(ModBlocks.STRIPPED_ASH_WILLOW_LOG_ITEM);
            output.accept(ModBlocks.STRIPPED_ASH_WILLOW_WOOD_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_PLANKS_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_STAIRS_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_SLAB_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_FENCE_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_FENCE_GATE_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_DOOR_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_TRAPDOOR_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_PRESSURE_PLATE_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_BUTTON_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_SHELF_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_SIGN_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_HANGING_SIGN_ITEM);
            output.accept(ASH_WILLOW_BOAT);
            output.accept(ASH_WILLOW_CHEST_BOAT);
            output.accept(ModBlocks.OVERGROWN_WILLOW_SAPLING_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_FOLIAGE_ITEM);
            output.accept(ModBlocks.OVERGROWN_WILLOW_VINES_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_SAPLING_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_FOLIAGE_ITEM);
            output.accept(ModBlocks.ASH_WILLOW_VINES_ITEM);
            output.accept(ModBlocks.OVERGROWN_FRONDS_ITEM);
            output.accept(ModBlocks.OVERGROWN_CHARD_ITEM);
            output.accept(ModBlocks.OVERGROWN_STALKS_ITEM);
            output.accept(ModBlocks.OVERGROWN_HANGING_ROOTS_ITEM);
            output.accept(ModBlocks.SIFTSLATE_STALKS_ITEM);
            output.accept(ModBlocks.SIFTSLATE_HANGING_ROOTS_ITEM);
            output.accept(ModBlocks.HEALTHY_SCULK_CARTILAGE_SPROUT_ITEM);
            output.accept(ModBlocks.HEALTHY_SCULK_SPROUTS_ITEM);
            output.accept(ModBlocks.DRY_HEALTHY_SCULK_SPROUTS_ITEM);
            output.accept(ModBlocks.BLUE_SAUCER_BLOOM_ITEM);
            output.accept(ModBlocks.GREEN_SAUCER_BLOOM_ITEM);
            output.accept(ModBlocks.SUNBURST_PLANT_ITEM);
            output.accept(ModBlocks.WHISPERBLOOM_ITEM);
            output.accept(ModBlocks.GRIM_BUSH_ITEM);
            output.accept(ModBlocks.CORAL_BUSH_ITEM);
            output.accept(ModBlocks.BRIGHT_CORAL_BUSH_ITEM);
            output.accept(BLUBBY_BERRIES);
            output.accept(SCULKFLOWER_SEEDS);
            output.accept(ModBlocks.SCULKFLOWER_ITEM);
            output.accept(RAW_SLABBER_MEAT);
            output.accept(COOKED_SLABBER_MEAT);
            output.accept(RAW_SIFTER_MEAT);
            output.accept(COOKED_SIFTER_MEAT);
            output.accept(RAW_NUZZLE_MEAT);
            output.accept(COOKED_NUZZLE_MEAT);
            output.accept(POLLINATOR_DROOL);
            output.accept(ICHOR_SNOWBALL);
            output.accept(ModFluids.ICHOR_BUCKET);
            output.accept(ICHOR_BOTTLE);
            output.accept(ModBlocks.ICHOR_GLASS_ITEM);
            output.accept(ModBlocks.ICHOR_GLASS_PANE_ITEM);
            output.accept(ICHOR_SHARD);
            output.accept(ModBlocks.CHAROITE_BLOCK_ITEM);
            output.accept(ModBlocks.SIFTITE_BLOCK_ITEM);
            output.accept(CHAROITE);
            output.accept(SIFTITE_INGOT);
            output.accept(SIFTITE_NUGGET);
            output.accept(SIFTITE_SWORD);
            output.accept(SIFTITE_PICKAXE);
            output.accept(SIFTITE_AXE);
            output.accept(SIFTITE_SHOVEL);
            output.accept(SIFTITE_HOE);
            output.accept(SIFTITE_SPEAR);
            output.accept(SIFTITE_HELMET);
            output.accept(SIFTITE_CHESTPLATE);
            output.accept(SIFTITE_LEGGINGS);
            output.accept(SIFTITE_BOOTS);
            output.accept(SIFTITE_UPGRADE_SMITHING_TEMPLATE);
            output.accept(MONARCH_UPGRADE_SMITHING_TEMPLATE);
            output.accept(MONARCH_MEMBRANE);
            output.accept(MONARCH_ELYTRA);
            output.accept(SIFTER_HIDE);
            output.accept(SIFTER_CAP);
            output.accept(SoulAbsorption.book(params.holders(), 1));
            output.accept(SoulAbsorption.book(params.holders(), 2));
            output.accept(MUSIC_DISC_RIFT);
            output.accept(MUSIC_DISC_WELLSPRING);
            output.accept(ModPaintings.monarch(params.holders()));
            output.accept(SIFT_RIFT);
            output.accept(SINGER_SPAWN_EGG);
            output.accept(ECHO_GOLEM_SPAWN_EGG);
            output.accept(BlubItems.BLUB_SPAWN_EGG);
            output.accept(SEEDLING_SPAWN_EGG);
            output.accept(POLLINATOR_SPAWN_EGG);
            output.accept(NESTER_SPAWN_EGG);
            output.accept(NUZZLE_SPAWN_EGG);
            output.accept(SLABBER_SPAWN_EGG);
            output.accept(DARK_SNIFFER_SPAWN_EGG);
            output.accept(STALKER_SPAWN_EGG);
            output.accept(SCAVENGER_SPAWN_EGG);
         }
      )
      .build();

   static {
      ModRegistration.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY, CREATIVE_TAB);
   }

   private ModItems() {
   }

   public static void bootstrap() {
      // Keep pre-2.0 worlds containing the removed portal BlockItem loadable.
      BuiltInRegistries.ITEM.addAlias(Identifier.fromNamespaceAndPath("the_sift", "sift_portal"), Identifier.withDefaultNamespace("air"));
   }

   public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
      event.modify(SCULKFLOWER_SEEDS, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, new Compostable(new Constant(30))));
      event.modify(ModBlocks.SCULKFLOWER_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, new Compostable(new Constant(65))));
   }

   public static void initialize() {
      IchorBottleItem.registerInteractions();
      ModDispensers.register();
   }

   private static Item registerItem(String name, Properties properties) {
      ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", name));
      return ModRegistration.register(BuiltInRegistries.ITEM, key, new Item(properties.setId(key)));
   }

   private static <T extends Item> T registerItem(String name, Function<Properties, T> factory, Properties properties) {
      ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", name));
      return ModRegistration.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
   }
}
