package mielon.thesift.block;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import mielon.thesift.block.entity.IchorCauldronBlockEntity;
import mielon.thesift.block.entity.SiftPortalBlockEntity;
import mielon.thesift.block.entity.SonorousDeepslateBlockEntity;
import mielon.thesift.platform.ModRegistration;
import mielon.thesift.sound.SoulshaleSoundType;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.Constant;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

public class ModBlocks {
   public static final Block CHAROITE_BLOCK = registerBlock(
      "charoite_block", Block::new, Properties.ofFullCopy(Blocks.LAPIS_BLOCK).mapColor(MapColor.COLOR_PURPLE)
   );
   public static final BlockItem CHAROITE_BLOCK_ITEM = registerBlockItem("charoite_block", CHAROITE_BLOCK);
   public static final Block SIFTITE_BLOCK = registerBlock(
      "siftite_block", Block::new, Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).mapColor(MapColor.DIAMOND)
   );
   public static final BlockItem SIFTITE_BLOCK_ITEM = registerBlockItem("siftite_block", SIFTITE_BLOCK);
   public static final ResourceKey<Block> SIFT_PORTAL_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "sift_portal")
   );
   public static final Block SIFT_PORTAL = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SIFT_PORTAL_KEY,
      new SiftPortalBlock(
         Properties.ofFullCopy(Blocks.NETHER_PORTAL)
            .mapColor(MapColor.DIAMOND)
            .setId(SIFT_PORTAL_KEY)
            .noCollision()
            .noOcclusion()
            .strength(-1.0F)
            .lightLevel(state -> 12)
      )
   );
   public static final ResourceKey<BlockEntityType<?>> SIFT_PORTAL_BLOCK_ENTITY_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), Identifier.fromNamespaceAndPath("the_sift", "sift_portal")
   );
   public static final BlockEntityType<SiftPortalBlockEntity> SIFT_PORTAL_BLOCK_ENTITY = ModRegistration.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, SIFT_PORTAL_BLOCK_ENTITY_KEY, new BlockEntityType<>(SiftPortalBlockEntity::new, Set.of(SIFT_PORTAL))
   );
   public static final ResourceKey<Block> SONOROUS_DEEPSLATE_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "sonorous_deepslate")
   );
   public static final Block SONOROUS_DEEPSLATE = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SONOROUS_DEEPSLATE_KEY,
      new SonorousDeepslateBlock(Properties.ofFullCopy(Blocks.REINFORCED_DEEPSLATE).mapColor(MapColor.COLOR_GRAY).setId(SONOROUS_DEEPSLATE_KEY))
   );
   public static final ResourceKey<Item> SONOROUS_DEEPSLATE_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "sonorous_deepslate")
   );
   public static final BlockItem SONOROUS_DEEPSLATE_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      SONOROUS_DEEPSLATE_ITEM_KEY,
      new SonorousDeepslateBlockItem(SONOROUS_DEEPSLATE, new net.minecraft.world.item.Item.Properties().setId(SONOROUS_DEEPSLATE_ITEM_KEY))
   );
   public static final ResourceKey<BlockEntityType<?>> SONOROUS_DEEPSLATE_BLOCK_ENTITY_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK_ENTITY_TYPE.key(), Identifier.fromNamespaceAndPath("the_sift", "sonorous_deepslate")
   );
   public static final BlockEntityType<SonorousDeepslateBlockEntity> SONOROUS_DEEPSLATE_BLOCK_ENTITY = ModRegistration.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      SONOROUS_DEEPSLATE_BLOCK_ENTITY_KEY,
      new BlockEntityType<>(SonorousDeepslateBlockEntity::new, Set.of(SONOROUS_DEEPSLATE))
   );
   public static final ResourceKey<Block> SIFTSLATE_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "siftslate")
   );
   public static final Block SIFTSLATE = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SIFTSLATE_KEY,
      new SiftSpreadingBaseBlock(Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(SiftMapColors.SIFTSLATE).setId(SIFTSLATE_KEY))
   );
   public static final ResourceKey<Item> SIFTSLATE_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "siftslate")
   );
   public static final BlockItem SIFTSLATE_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM, SIFTSLATE_ITEM_KEY, new BlockItem(SIFTSLATE, new net.minecraft.world.item.Item.Properties().setId(SIFTSLATE_ITEM_KEY))
   );
   public static final ResourceKey<Block> SIFTSLATE_GROWTH_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "siftslate_growth")
   );
   public static final Block SIFTSLATE_GROWTH = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SIFTSLATE_GROWTH_KEY,
      new SiftslateGrowthBlock(
         Properties.ofFullCopy(Blocks.STONE)
            .mapColor(state -> state.getValue(SiftslateGrowthBlock.SNOWY) ? SiftMapColors.ICHOR_SNOW : SiftMapColors.OVERGROWN)
            .sound(SoundType.DEEPSLATE)
            .randomTicks()
            .setId(SIFTSLATE_GROWTH_KEY)
      )
   );
   public static final ResourceKey<Item> SIFTSLATE_GROWTH_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "siftslate_growth")
   );
   public static final BlockItem SIFTSLATE_GROWTH_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      SIFTSLATE_GROWTH_ITEM_KEY,
      new BlockItem(SIFTSLATE_GROWTH, new net.minecraft.world.item.Item.Properties().setId(SIFTSLATE_GROWTH_ITEM_KEY))
   );
   public static final Block SOULSHALE = registerBlock(
      "soulshale",
      SiftSpreadingBaseBlock::new,
      Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_ITEM = registerBlockItem("soulshale", SOULSHALE);
   public static final Block SOULSHALE_GROWTH = registerBlock(
      "soulshale_growth",
      SoulshaleGrowthBlock::new,
      Properties.ofFullCopy(SOULSHALE)
         .mapColor(state -> state.getValue(SiftslateGrowthBlock.SNOWY) ? SiftMapColors.ICHOR_SNOW : SiftMapColors.SOULSHALE_GROWTH)
         .randomTicks()
   );
   public static final BlockItem SOULSHALE_GROWTH_ITEM = registerBlockItem("soulshale_growth", SOULSHALE_GROWTH);
   public static final Block CORAL_SOIL = registerBlock(
      "coral_soil", Block::new, Properties.ofFullCopy(Blocks.DIRT).mapColor(SiftMapColors.CORAL_SOIL).strength(0.6F).sound(SoundType.MUD)
   );
   public static final BlockItem CORAL_SOIL_ITEM = registerBlockItem("coral_soil", CORAL_SOIL);
   public static final Block CORAL_GROWTH = registerBlock(
      "coral_growth", properties -> new CoralGrowthBlock(false, properties), Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(SiftMapColors.CORAL_GROWTH)
   );
   public static final BlockItem CORAL_GROWTH_ITEM = registerBlockItem("coral_growth", CORAL_GROWTH);
   public static final Block BRIGHT_CORAL_GROWTH = registerBlock(
      "bright_coral_growth", properties -> new CoralGrowthBlock(true, properties), Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(SiftMapColors.CORAL)
   );
   public static final BlockItem BRIGHT_CORAL_GROWTH_ITEM = registerBlockItem("bright_coral_growth", BRIGHT_CORAL_GROWTH);
   public static final Block BLUBBY_BERRIES_BUSH = registerBlock(
      "blubby_berries_bush", BlubbyBerriesBushBlock::new, Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).strength(0.0F).randomTicks()
   );
   public static final Block CORAL_BUSH = registerBlock(
      "coral_bush", CoralBushBlock::new, Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.CORAL_GROWTH).sound(SoundType.GRASS)
   );
   public static final BlockItem CORAL_BUSH_ITEM = registerBlockItem("coral_bush", CORAL_BUSH);
   public static final Block BRIGHT_CORAL_BUSH = registerBlock(
      "bright_coral_bush", CoralBushBlock::new, Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.CORAL).sound(SoundType.GRASS)
   );
   public static final BlockItem BRIGHT_CORAL_BUSH_ITEM = registerBlockItem("bright_coral_bush", BRIGHT_CORAL_BUSH);
   public static final Block GRIM_BUSH = registerBlock(
      "grim_bush", GrimBushBlock::new, Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(MapColor.COLOR_RED)
   );
   public static final BlockItem GRIM_BUSH_ITEM = registerBlockItem("grim_bush", GRIM_BUSH);
   public static final Block SOULSHALE_STAIRS = registerBlock(
      "soulshale_stairs",
      properties -> new StairBlock(SOULSHALE.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE_STAIRS).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_STAIRS_ITEM = registerBlockItem("soulshale_stairs", SOULSHALE_STAIRS);
   public static final Block SOULSHALE_SLAB = registerBlock(
      "soulshale_slab",
      SlabBlock::new,
      Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE_SLAB).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_SLAB_ITEM = registerBlockItem("soulshale_slab", SOULSHALE_SLAB);
   public static final Block SOULSHALE_WALL = registerBlock(
      "soulshale_wall",
      WallBlock::new,
      Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE_WALL).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_WALL_ITEM = registerBlockItem("soulshale_wall", SOULSHALE_WALL);
   public static final Block POLISHED_SOULSHALE = registerBlock(
      "polished_soulshale", Block::new, Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem POLISHED_SOULSHALE_ITEM = registerBlockItem("polished_soulshale", POLISHED_SOULSHALE);
   public static final Block POLISHED_SOULSHALE_STAIRS = registerBlock(
      "polished_soulshale_stairs",
      properties -> new StairBlock(POLISHED_SOULSHALE.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE_STAIRS).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem POLISHED_SOULSHALE_STAIRS_ITEM = registerBlockItem("polished_soulshale_stairs", POLISHED_SOULSHALE_STAIRS);
   public static final Block POLISHED_SOULSHALE_SLAB = registerBlock(
      "polished_soulshale_slab",
      SlabBlock::new,
      Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE_SLAB).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem POLISHED_SOULSHALE_SLAB_ITEM = registerBlockItem("polished_soulshale_slab", POLISHED_SOULSHALE_SLAB);
   public static final Block POLISHED_SOULSHALE_WALL = registerBlock(
      "polished_soulshale_wall",
      WallBlock::new,
      Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE_WALL).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem POLISHED_SOULSHALE_WALL_ITEM = registerBlockItem("polished_soulshale_wall", POLISHED_SOULSHALE_WALL);
   public static final Block SOULSHALE_BRICKS = registerBlock(
      "soulshale_bricks", Block::new, Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_BRICKS_ITEM = registerBlockItem("soulshale_bricks", SOULSHALE_BRICKS);
   public static final Block SOULSHALE_BRICK_STAIRS = registerBlock(
      "soulshale_brick_stairs",
      properties -> new StairBlock(SOULSHALE_BRICKS.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_STAIRS).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_BRICK_STAIRS_ITEM = registerBlockItem("soulshale_brick_stairs", SOULSHALE_BRICK_STAIRS);
   public static final Block SOULSHALE_BRICK_SLAB = registerBlock(
      "soulshale_brick_slab",
      SlabBlock::new,
      Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_SLAB).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_BRICK_SLAB_ITEM = registerBlockItem("soulshale_brick_slab", SOULSHALE_BRICK_SLAB);
   public static final Block SOULSHALE_BRICK_WALL = registerBlock(
      "soulshale_brick_wall",
      WallBlock::new,
      Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_WALL).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_BRICK_WALL_ITEM = registerBlockItem("soulshale_brick_wall", SOULSHALE_BRICK_WALL);
   public static final Block CHISELED_SOULSHALE = registerBlock(
      "chiseled_soulshale", Block::new, Properties.ofFullCopy(Blocks.CHISELED_DEEPSLATE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem CHISELED_SOULSHALE_ITEM = registerBlockItem("chiseled_soulshale", CHISELED_SOULSHALE);
   public static final Block SOULSHALE_TOTEM = registerBlock(
      "soulshale_totem", Block::new, Properties.ofFullCopy(Blocks.CHISELED_DEEPSLATE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_TOTEM_ITEM = registerBlockItem("soulshale_totem", SOULSHALE_TOTEM);
   public static final DropExperienceBlock SOULSHALE_COAL_ORE = registerBlock(
      "soulshale_coal_ore",
      properties -> new DropExperienceBlock(UniformInt.of(0, 2), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_COAL_ORE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_COAL_ORE_ITEM = registerBlockItem("soulshale_coal_ore", SOULSHALE_COAL_ORE);
   public static final DropExperienceBlock SOULSHALE_DIAMOND_ORE = registerBlock(
      "soulshale_diamond_ore",
      properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_DIAMOND_ORE_ITEM = registerBlockItem("soulshale_diamond_ore", SOULSHALE_DIAMOND_ORE);
   public static final DropExperienceBlock SOULSHALE_EMERALD_ORE = registerBlock(
      "soulshale_emerald_ore",
      properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_EMERALD_ORE_ITEM = registerBlockItem("soulshale_emerald_ore", SOULSHALE_EMERALD_ORE);
   public static final DropExperienceBlock SOULSHALE_CHAROITE_ORE = registerBlock(
      "soulshale_charoite_ore",
      properties -> new DropExperienceBlock(UniformInt.of(2, 5), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_LAPIS_ORE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_CHAROITE_ORE_ITEM = registerBlockItem("soulshale_charoite_ore", SOULSHALE_CHAROITE_ORE);
   public static final DropExperienceBlock SOULSHALE_SIFTITE_ORE = registerBlock(
      "soulshale_siftite_ore",
      properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(SiftMapColors.SOULSHALE).sound(SoulshaleSoundType.INSTANCE)
   );
   public static final BlockItem SOULSHALE_SIFTITE_ORE_ITEM = registerBlockItem("soulshale_siftite_ore", SOULSHALE_SIFTITE_ORE);
   public static final Block COBBLED_SIFTSLATE = registerBlock(
      "cobbled_siftslate", Block::new, Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem COBBLED_SIFTSLATE_ITEM = registerBlockItem("cobbled_siftslate", COBBLED_SIFTSLATE);
   public static final Block COBBLED_SIFTSLATE_STAIRS = registerBlock(
      "cobbled_siftslate_stairs",
      properties -> new StairBlock(COBBLED_SIFTSLATE.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE_STAIRS).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem COBBLED_SIFTSLATE_STAIRS_ITEM = registerBlockItem("cobbled_siftslate_stairs", COBBLED_SIFTSLATE_STAIRS);
   public static final Block COBBLED_SIFTSLATE_SLAB = registerBlock(
      "cobbled_siftslate_slab", SlabBlock::new, Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE_SLAB).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem COBBLED_SIFTSLATE_SLAB_ITEM = registerBlockItem("cobbled_siftslate_slab", COBBLED_SIFTSLATE_SLAB);
   public static final Block COBBLED_SIFTSLATE_WALL = registerBlock(
      "cobbled_siftslate_wall", WallBlock::new, Properties.ofFullCopy(Blocks.COBBLED_DEEPSLATE_WALL).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem COBBLED_SIFTSLATE_WALL_ITEM = registerBlockItem("cobbled_siftslate_wall", COBBLED_SIFTSLATE_WALL);
   public static final Block SIFTSLATE_BRICKS = registerBlock(
      "siftslate_bricks", Block::new, Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_BRICKS_ITEM = registerBlockItem("siftslate_bricks", SIFTSLATE_BRICKS);
   public static final Block SIFTSLATE_BRICK_STAIRS = registerBlock(
      "siftslate_brick_stairs",
      properties -> new StairBlock(SIFTSLATE_BRICKS.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_STAIRS).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_BRICK_STAIRS_ITEM = registerBlockItem("siftslate_brick_stairs", SIFTSLATE_BRICK_STAIRS);
   public static final Block SIFTSLATE_BRICK_SLAB = registerBlock(
      "siftslate_brick_slab", SlabBlock::new, Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_SLAB).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_BRICK_SLAB_ITEM = registerBlockItem("siftslate_brick_slab", SIFTSLATE_BRICK_SLAB);
   public static final Block SIFTSLATE_BRICK_WALL = registerBlock(
      "siftslate_brick_wall", WallBlock::new, Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_WALL).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_BRICK_WALL_ITEM = registerBlockItem("siftslate_brick_wall", SIFTSLATE_BRICK_WALL);
   public static final Block CHISELED_SIFTSLATE = registerBlock(
      "chiseled_siftslate", Block::new, Properties.ofFullCopy(Blocks.CHISELED_DEEPSLATE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem CHISELED_SIFTSLATE_ITEM = registerBlockItem("chiseled_siftslate", CHISELED_SIFTSLATE);
   public static final Block SIFT_RUBBLE = registerBlock(
      "sift_rubble", SiftRubbleBlock::new, Properties.ofFullCopy(Blocks.GRAVEL).mapColor(SiftMapColors.SIFTSLATE).sound(SoundType.SUSPICIOUS_GRAVEL)
   );
   public static final BlockItem SIFT_RUBBLE_ITEM = registerBlockItem("sift_rubble", SIFT_RUBBLE);
   public static final Block ICHOR_GLASS = registerBlock("ichor_glass", SiftGlassBlock::new, Properties.ofFullCopy(Blocks.GLASS).mapColor(MapColor.DIAMOND));
   public static final BlockItem ICHOR_GLASS_ITEM = registerBlockItem("ichor_glass", ICHOR_GLASS);
   public static final Block ICHOR_GLASS_PANE = registerBlock(
      "ichor_glass_pane", IronBarsBlock::new, Properties.ofFullCopy(Blocks.GLASS_PANE).mapColor(MapColor.DIAMOND)
   );
   public static final BlockItem ICHOR_GLASS_PANE_ITEM = registerBlockItem("ichor_glass_pane", ICHOR_GLASS_PANE);
   public static final Block ICHOR_SNOW_BLOCK = registerBlock(
      "ichor_snow_block", Block::new, Properties.ofFullCopy(Blocks.SNOW_BLOCK).mapColor(SiftMapColors.ICHOR_SNOW)
   );
   public static final BlockItem ICHOR_SNOW_BLOCK_ITEM = registerBlockItem("ichor_snow_block", ICHOR_SNOW_BLOCK);
   public static final SnowLayerBlock ICHOR_SNOW = registerBlock(
      "ichor_snow", SnowLayerBlock::new, Properties.ofFullCopy(Blocks.SNOW).mapColor(SiftMapColors.ICHOR_SNOW)
   );
   public static final BlockItem ICHOR_SNOW_ITEM = registerBlockItem("ichor_snow", ICHOR_SNOW);
   public static final SculkflowerCropBlock SCULKFLOWER_CROP = registerBlock(
      "sculkflower_crop", SculkflowerCropBlock::new, Properties.of().mapColor(MapColor.DIAMOND).noCollision().randomTicks().instabreak().sound(SoundType.CROP)
   );
   public static final SculkflowerBlock SCULKFLOWER = registerBlock(
      "sculkflower", SculkflowerBlock::new, Properties.ofFullCopy(Blocks.CACTUS_FLOWER).mapColor(MapColor.DIAMOND)
   );
   public static final BlockItem SCULKFLOWER_ITEM = registerBlockItem("sculkflower", SCULKFLOWER);
   public static final Block ROYAL_SCULK = registerBlock(
      "royal_sculk",
      RoyalSculkBlock::new,
      dropsLike(
         Properties.ofFullCopy(Blocks.SCULK).mapColor(SiftMapColors.DARK_SCULK).destroyTime(Blocks.SCULK.defaultDestroyTime() * 16.0F / 9.0F), "royal_sculk"
      )
   );
   public static final BlockItem ROYAL_SCULK_ITEM = registerBlockItem(
      "royal_sculk", ROYAL_SCULK, new net.minecraft.world.item.Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final ResourceKey<Block> HEALTHY_SCULK_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "healthy_sculk")
   );
   public static final Block HEALTHY_SCULK = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      HEALTHY_SCULK_KEY,
      new HealthySculkSoilBlock(Properties.ofFullCopy(Blocks.SCULK).mapColor(SiftMapColors.HEALTHY_SCULK).setId(HEALTHY_SCULK_KEY))
   );
   public static final ResourceKey<Item> HEALTHY_SCULK_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "healthy_sculk")
   );
   public static final BlockItem HEALTHY_SCULK_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      HEALTHY_SCULK_ITEM_KEY,
      new BlockItem(HEALTHY_SCULK, new net.minecraft.world.item.Item.Properties().setId(HEALTHY_SCULK_ITEM_KEY))
   );
   public static final Block HEALTHY_SCULK_CARTILAGE = registerBlock(
      "healthy_sculk_cartilage", HealthySculkSoilBlock::new, Properties.ofFullCopy(HEALTHY_SCULK).mapColor(SiftMapColors.CARTILAGE)
   );
   public static final BlockItem HEALTHY_SCULK_CARTILAGE_ITEM = registerBlockItem("healthy_sculk_cartilage", HEALTHY_SCULK_CARTILAGE);
   public static final Block HEALTHY_SCULK_PORES = registerBlock(
      "healthy_sculk_pores", HealthySculkSoilBlock::new, Properties.ofFullCopy(HEALTHY_SCULK).mapColor(SiftMapColors.PORES)
   );
   public static final BlockItem HEALTHY_SCULK_PORES_ITEM = registerBlockItem("healthy_sculk_pores", HEALTHY_SCULK_PORES);
   public static final ResourceKey<Block> DRY_HEALTHY_SCULK_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "dry_healthy_sculk")
   );
   public static final Block DRY_HEALTHY_SCULK = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      DRY_HEALTHY_SCULK_KEY,
      new HealthySculkSoilBlock(Properties.ofFullCopy(Blocks.SCULK).mapColor(SiftMapColors.DRY_SCULK).setId(DRY_HEALTHY_SCULK_KEY))
   );
   public static final ResourceKey<Item> DRY_HEALTHY_SCULK_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "dry_healthy_sculk")
   );
   public static final BlockItem DRY_HEALTHY_SCULK_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      DRY_HEALTHY_SCULK_ITEM_KEY,
      new BlockItem(DRY_HEALTHY_SCULK, new net.minecraft.world.item.Item.Properties().setId(DRY_HEALTHY_SCULK_ITEM_KEY))
   );
   public static final ResourceKey<Block> DRY_HEALTHY_SCULK_GROWTH_KEY = blockKey("dry_healthy_sculk_growth");
   public static final Block DRY_HEALTHY_SCULK_GROWTH = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      DRY_HEALTHY_SCULK_GROWTH_KEY,
      new DryHealthySculkGrowthBlock(Properties.ofFullCopy(Blocks.SCULK).mapColor(SiftMapColors.OVERGROWN).randomTicks().setId(DRY_HEALTHY_SCULK_GROWTH_KEY))
   );
   public static final ResourceKey<Item> DRY_HEALTHY_SCULK_GROWTH_ITEM_KEY = itemKey("dry_healthy_sculk_growth");
   public static final BlockItem DRY_HEALTHY_SCULK_GROWTH_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      DRY_HEALTHY_SCULK_GROWTH_ITEM_KEY,
      new BlockItem(DRY_HEALTHY_SCULK_GROWTH, new net.minecraft.world.item.Item.Properties().setId(DRY_HEALTHY_SCULK_GROWTH_ITEM_KEY))
   );
   public static final ResourceKey<Block> ICHOR_CAULDRON_KEY = blockKey("ichor_cauldron");
   public static final Block ICHOR_CAULDRON = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      ICHOR_CAULDRON_KEY,
      new IchorCauldronBlock(
         Properties.ofFullCopy(Blocks.WATER_CAULDRON)
            .mapColor(MapColor.DIAMOND)
            .setId(ICHOR_CAULDRON_KEY)
            .lightLevel(state -> state.getValue(LayeredCauldronBlock.LEVEL) * 5)
      )
   );
   public static final ResourceKey<Block> SOUL_BLOCK_KEY = blockKey("soul_block");
   public static final Block SOUL_BLOCK = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SOUL_BLOCK_KEY,
      new SoulBlock(
         Properties.ofFullCopy(Blocks.SOUL_SOIL)
            .mapColor(SiftMapColors.SOUL)
            .strength(0.35F, 0.35F)
            .sound(SoundType.SOUL_SOIL)
            .lightLevel(state -> 14)
            .noLootTable()
            .setId(SOUL_BLOCK_KEY)
      )
   );
   public static final ResourceKey<Item> SOUL_BLOCK_ITEM_KEY = itemKey("soul_block");
   public static final BlockItem SOUL_BLOCK_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      SOUL_BLOCK_ITEM_KEY,
      new BlockItem(SOUL_BLOCK, new net.minecraft.world.item.Item.Properties().rarity(Rarity.RARE).setId(SOUL_BLOCK_ITEM_KEY))
   );
   public static final ResourceKey<Block> OVERGROWN_CHARD_KEY = blockKey("overgrown_chard");
   public static final Block OVERGROWN_CHARD = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      OVERGROWN_CHARD_KEY,
      new SiftPlantBlock(Properties.ofFullCopy(Blocks.BEETROOTS).mapColor(SiftMapColors.OVERGROWN_PLANTS).replaceable().setId(OVERGROWN_CHARD_KEY))
   );
   public static final ResourceKey<Item> OVERGROWN_CHARD_ITEM_KEY = itemKey("overgrown_chard");
   public static final BlockItem OVERGROWN_CHARD_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      OVERGROWN_CHARD_ITEM_KEY,
      new BlockItem(OVERGROWN_CHARD, new net.minecraft.world.item.Item.Properties().setId(OVERGROWN_CHARD_ITEM_KEY))
   );
   public static final ResourceKey<Block> OVERGROWN_STALKS_KEY = blockKey("overgrown_stalks");
   public static final Block OVERGROWN_STALKS = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      OVERGROWN_STALKS_KEY,
      new SiftPlantBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.OVERGROWN_PLANTS).setId(OVERGROWN_STALKS_KEY))
   );
   public static final ResourceKey<Item> OVERGROWN_STALKS_ITEM_KEY = itemKey("overgrown_stalks");
   public static final BlockItem OVERGROWN_STALKS_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      OVERGROWN_STALKS_ITEM_KEY,
      new BlockItem(OVERGROWN_STALKS, new net.minecraft.world.item.Item.Properties().setId(OVERGROWN_STALKS_ITEM_KEY))
   );
   public static final ResourceKey<Block> OVERGROWN_FRONDS_KEY = blockKey("overgrown_fronds");
   public static final Block OVERGROWN_FRONDS = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      OVERGROWN_FRONDS_KEY,
      new SiftPlantBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.OVERGROWN_PLANTS).setId(OVERGROWN_FRONDS_KEY))
   );
   public static final ResourceKey<Item> OVERGROWN_FRONDS_ITEM_KEY = itemKey("overgrown_fronds");
   public static final BlockItem OVERGROWN_FRONDS_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      OVERGROWN_FRONDS_ITEM_KEY,
      new BlockItem(OVERGROWN_FRONDS, new net.minecraft.world.item.Item.Properties().setId(OVERGROWN_FRONDS_ITEM_KEY))
   );
   public static final ResourceKey<Block> BLUE_SAUCER_BLOOM_KEY = blockKey("blue_saucer_bloom");
   public static final Block BLUE_SAUCER_BLOOM = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      BLUE_SAUCER_BLOOM_KEY,
      new SaucerBloomBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(MapColor.COLOR_LIGHT_BLUE).setId(BLUE_SAUCER_BLOOM_KEY))
   );
   public static final ResourceKey<Item> BLUE_SAUCER_BLOOM_ITEM_KEY = itemKey("blue_saucer_bloom");
   public static final BlockItem BLUE_SAUCER_BLOOM_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      BLUE_SAUCER_BLOOM_ITEM_KEY,
      new BlockItem(BLUE_SAUCER_BLOOM, new net.minecraft.world.item.Item.Properties().setId(BLUE_SAUCER_BLOOM_ITEM_KEY))
   );
   public static final ResourceKey<Block> GREEN_SAUCER_BLOOM_KEY = blockKey("green_saucer_bloom");
   public static final Block GREEN_SAUCER_BLOOM = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      GREEN_SAUCER_BLOOM_KEY,
      new SaucerBloomBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.GREEN_BLOOM).setId(GREEN_SAUCER_BLOOM_KEY))
   );
   public static final ResourceKey<Item> GREEN_SAUCER_BLOOM_ITEM_KEY = itemKey("green_saucer_bloom");
   public static final BlockItem GREEN_SAUCER_BLOOM_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      GREEN_SAUCER_BLOOM_ITEM_KEY,
      new BlockItem(GREEN_SAUCER_BLOOM, new net.minecraft.world.item.Item.Properties().setId(GREEN_SAUCER_BLOOM_ITEM_KEY))
   );
   public static final ResourceKey<Block> SUNBURST_PLANT_KEY = blockKey("sunburst_plant");
   public static final Block SUNBURST_PLANT = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SUNBURST_PLANT_KEY,
      new SiftPlantBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(MapColor.GOLD).setId(SUNBURST_PLANT_KEY), true)
   );
   public static final ResourceKey<Item> SUNBURST_PLANT_ITEM_KEY = itemKey("sunburst_plant");
   public static final BlockItem SUNBURST_PLANT_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      SUNBURST_PLANT_ITEM_KEY,
      new BlockItem(SUNBURST_PLANT, new net.minecraft.world.item.Item.Properties().setId(SUNBURST_PLANT_ITEM_KEY))
   );
   public static final ResourceKey<Block> WHISPERBLOOM_KEY = blockKey("whisperbloom");
   public static final Block WHISPERBLOOM = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      WHISPERBLOOM_KEY,
      new SiftPlantBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.WHISPERBLOOM).setId(WHISPERBLOOM_KEY), true)
   );
   public static final ResourceKey<Item> WHISPERBLOOM_ITEM_KEY = itemKey("whisperbloom");
   public static final BlockItem WHISPERBLOOM_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM, WHISPERBLOOM_ITEM_KEY, new BlockItem(WHISPERBLOOM, new net.minecraft.world.item.Item.Properties().setId(WHISPERBLOOM_ITEM_KEY))
   );
   public static final ResourceKey<Block> SIFTSLATE_STALKS_KEY = blockKey("siftslate_stalks");
   public static final Block SIFTSLATE_STALKS = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      SIFTSLATE_STALKS_KEY,
      new SiftPlantBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.SIFTSLATE).setId(SIFTSLATE_STALKS_KEY))
   );
   public static final ResourceKey<Item> SIFTSLATE_STALKS_ITEM_KEY = itemKey("siftslate_stalks");
   public static final BlockItem SIFTSLATE_STALKS_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      SIFTSLATE_STALKS_ITEM_KEY,
      new BlockItem(SIFTSLATE_STALKS, new net.minecraft.world.item.Item.Properties().setId(SIFTSLATE_STALKS_ITEM_KEY))
   );
   public static final ResourceKey<Block> HEALTHY_SCULK_SPROUTS_KEY = blockKey("healthy_sculk_sprouts");
   public static final Block HEALTHY_SCULK_SPROUTS = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      HEALTHY_SCULK_SPROUTS_KEY,
      new SiftSproutBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.HEALTHY_SCULK).setId(HEALTHY_SCULK_SPROUTS_KEY))
   );
   public static final ResourceKey<Item> HEALTHY_SCULK_SPROUTS_ITEM_KEY = itemKey("healthy_sculk_sprouts");
   public static final BlockItem HEALTHY_SCULK_SPROUTS_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      HEALTHY_SCULK_SPROUTS_ITEM_KEY,
      new BlockItem(HEALTHY_SCULK_SPROUTS, new net.minecraft.world.item.Item.Properties().setId(HEALTHY_SCULK_SPROUTS_ITEM_KEY))
   );
   public static final ResourceKey<Block> HEALTHY_SCULK_CARTILAGE_SPROUT_KEY = blockKey("healthy_sculk_cartilage_sprout");
   public static final Block HEALTHY_SCULK_CARTILAGE_SPROUT = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      HEALTHY_SCULK_CARTILAGE_SPROUT_KEY,
      new HealthySculkCartilageSproutBlock(
         Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.CARTILAGE).setId(HEALTHY_SCULK_CARTILAGE_SPROUT_KEY)
      )
   );
   public static final ResourceKey<Item> HEALTHY_SCULK_CARTILAGE_SPROUT_ITEM_KEY = itemKey("healthy_sculk_cartilage_sprout");
   public static final BlockItem HEALTHY_SCULK_CARTILAGE_SPROUT_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      HEALTHY_SCULK_CARTILAGE_SPROUT_ITEM_KEY,
      new BlockItem(HEALTHY_SCULK_CARTILAGE_SPROUT, new net.minecraft.world.item.Item.Properties().setId(HEALTHY_SCULK_CARTILAGE_SPROUT_ITEM_KEY))
   );
   public static final ResourceKey<Block> DRY_HEALTHY_SCULK_SPROUTS_KEY = blockKey("dry_healthy_sculk_sprouts");
   public static final Block DRY_HEALTHY_SCULK_SPROUTS = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      DRY_HEALTHY_SCULK_SPROUTS_KEY,
      new SiftSproutBlock(Properties.ofFullCopy(Blocks.NETHER_SPROUTS).mapColor(SiftMapColors.DRY_SCULK).setId(DRY_HEALTHY_SCULK_SPROUTS_KEY))
   );
   public static final ResourceKey<Item> DRY_HEALTHY_SCULK_SPROUTS_ITEM_KEY = itemKey("dry_healthy_sculk_sprouts");
   public static final BlockItem DRY_HEALTHY_SCULK_SPROUTS_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      DRY_HEALTHY_SCULK_SPROUTS_ITEM_KEY,
      new BlockItem(DRY_HEALTHY_SCULK_SPROUTS, new net.minecraft.world.item.Item.Properties().setId(DRY_HEALTHY_SCULK_SPROUTS_ITEM_KEY))
   );
   public static final Block SIFTSLATE_HANGING_ROOTS = registerBlock(
      "siftslate_hanging_roots",
      HangingRootsBlock::new,
      Properties.ofFullCopy(Blocks.HANGING_ROOTS).mapColor(SiftMapColors.SIFTSLATE).sound(SoundType.NETHER_SPROUTS)
   );
   public static final BlockItem SIFTSLATE_HANGING_ROOTS_ITEM = registerBlockItem("siftslate_hanging_roots", SIFTSLATE_HANGING_ROOTS);
   public static final Block OVERGROWN_HANGING_ROOTS = registerBlock(
      "overgrown_hanging_roots",
      HangingRootsBlock::new,
      Properties.ofFullCopy(Blocks.HANGING_ROOTS).mapColor(SiftMapColors.OVERGROWN_PLANTS).sound(SoundType.NETHER_SPROUTS)
   );
   public static final BlockItem OVERGROWN_HANGING_ROOTS_ITEM = registerBlockItem("overgrown_hanging_roots", OVERGROWN_HANGING_ROOTS);
   public static final Block OVERGROWN_WILLOW_PLANKS = registerBlock(
      "overgrown_willow_planks", Block::new, Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_PLANKS_ITEM = registerBlockItem("overgrown_willow_planks", OVERGROWN_WILLOW_PLANKS);
   public static final Block OVERGROWN_WILLOW_LOG = registerBlock(
      "overgrown_willow_log",
      RotatedPillarBlock::new,
      Properties.ofFullCopy(Blocks.OAK_LOG)
         .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Axis.Y ? SiftMapColors.WILLOW_LOG_END : SiftMapColors.WILLOW_BARK)
   );
   public static final BlockItem OVERGROWN_WILLOW_LOG_ITEM = registerBlockItem("overgrown_willow_log", OVERGROWN_WILLOW_LOG);
   public static final Block OVERGROWN_WILLOW_WOOD = registerBlock(
      "overgrown_willow_wood", RotatedPillarBlock::new, Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(SiftMapColors.WILLOW_BARK)
   );
   public static final BlockItem OVERGROWN_WILLOW_WOOD_ITEM = registerBlockItem("overgrown_willow_wood", OVERGROWN_WILLOW_WOOD);
   public static final Block STRIPPED_OVERGROWN_WILLOW_LOG = registerBlock(
      "stripped_overgrown_willow_log", RotatedPillarBlock::new, Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem STRIPPED_OVERGROWN_WILLOW_LOG_ITEM = registerBlockItem("stripped_overgrown_willow_log", STRIPPED_OVERGROWN_WILLOW_LOG);
   public static final Block STRIPPED_OVERGROWN_WILLOW_WOOD = registerBlock(
      "stripped_overgrown_willow_wood", RotatedPillarBlock::new, Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem STRIPPED_OVERGROWN_WILLOW_WOOD_ITEM = registerBlockItem("stripped_overgrown_willow_wood", STRIPPED_OVERGROWN_WILLOW_WOOD);
   public static final Block OVERGROWN_WILLOW_STAIRS = registerBlock(
      "overgrown_willow_stairs",
      properties -> new StairBlock(OVERGROWN_WILLOW_PLANKS.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_STAIRS_ITEM = registerBlockItem("overgrown_willow_stairs", OVERGROWN_WILLOW_STAIRS);
   public static final Block OVERGROWN_WILLOW_SLAB = registerBlock(
      "overgrown_willow_slab", SlabBlock::new, Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_SLAB_ITEM = registerBlockItem("overgrown_willow_slab", OVERGROWN_WILLOW_SLAB);
   public static final Block OVERGROWN_WILLOW_FENCE = registerBlock(
      "overgrown_willow_fence", FenceBlock::new, Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_FENCE_ITEM = registerBlockItem("overgrown_willow_fence", OVERGROWN_WILLOW_FENCE);
   public static final Block OVERGROWN_WILLOW_FENCE_GATE = registerBlock(
      "overgrown_willow_fence_gate",
      properties -> new FenceGateBlock(WoodType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_FENCE_GATE_ITEM = registerBlockItem("overgrown_willow_fence_gate", OVERGROWN_WILLOW_FENCE_GATE);
   public static final Block OVERGROWN_WILLOW_DOOR = registerBlock(
      "overgrown_willow_door",
      properties -> new DoorBlock(BlockSetType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_DOOR_ITEM = registerBlockItem("overgrown_willow_door", OVERGROWN_WILLOW_DOOR);
   public static final Block OVERGROWN_WILLOW_TRAPDOOR = registerBlock(
      "overgrown_willow_trapdoor",
      properties -> new TrapDoorBlock(BlockSetType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_TRAPDOOR_ITEM = registerBlockItem("overgrown_willow_trapdoor", OVERGROWN_WILLOW_TRAPDOOR);
   public static final Block OVERGROWN_WILLOW_PRESSURE_PLATE = registerBlock(
      "overgrown_willow_pressure_plate",
      properties -> new PressurePlateBlock(BlockSetType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_PRESSURE_PLATE_ITEM = registerBlockItem("overgrown_willow_pressure_plate", OVERGROWN_WILLOW_PRESSURE_PLATE);
   public static final Block OVERGROWN_WILLOW_BUTTON = registerBlock(
      "overgrown_willow_button",
      properties -> new ButtonBlock(BlockSetType.OAK, 30, properties),
      Properties.ofFullCopy(Blocks.OAK_BUTTON).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_BUTTON_ITEM = registerBlockItem("overgrown_willow_button", OVERGROWN_WILLOW_BUTTON);
   public static final Block OVERGROWN_WILLOW_SHELF = registerBlock(
      "overgrown_willow_shelf", ShelfBlock::new, Properties.ofFullCopy(Blocks.OAK_SHELF).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final BlockItem OVERGROWN_WILLOW_SHELF_ITEM = registerBlockItem("overgrown_willow_shelf", OVERGROWN_WILLOW_SHELF);
   public static final Block OVERGROWN_WILLOW_SIGN = registerBlock(
      "overgrown_willow_sign",
      properties -> new StandingSignBlock(WoodType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_SIGN).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final Block OVERGROWN_WILLOW_WALL_SIGN = registerBlock(
      "overgrown_willow_wall_sign",
      properties -> new WallSignBlock(WoodType.OAK, properties),
      dropsLike(Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).mapColor(SiftMapColors.WILLOW_PLANKS), "overgrown_willow_sign")
   );
   public static final Item OVERGROWN_WILLOW_SIGN_ITEM = registerItem(
      "overgrown_willow_sign", properties -> new StandingAndWallBlockItem(OVERGROWN_WILLOW_SIGN, OVERGROWN_WILLOW_WALL_SIGN, Direction.DOWN, properties)
   );
   public static final Block OVERGROWN_WILLOW_HANGING_SIGN = registerBlock(
      "overgrown_willow_hanging_sign",
      properties -> new CeilingHangingSignBlock(WoodType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN).mapColor(SiftMapColors.WILLOW_PLANKS)
   );
   public static final Block OVERGROWN_WILLOW_WALL_HANGING_SIGN = registerBlock(
      "overgrown_willow_wall_hanging_sign",
      properties -> new WallHangingSignBlock(WoodType.OAK, properties),
      dropsLike(Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN).mapColor(SiftMapColors.WILLOW_PLANKS), "overgrown_willow_hanging_sign")
   );
   public static final Item OVERGROWN_WILLOW_HANGING_SIGN_ITEM = registerItem(
      "overgrown_willow_hanging_sign", properties -> new HangingSignItem(OVERGROWN_WILLOW_HANGING_SIGN, OVERGROWN_WILLOW_WALL_HANGING_SIGN, properties)
   );
   public static final Block OVERGROWN_WILLOW_FOLIAGE = registerBlock(
      "overgrown_willow_foliage",
      properties -> new OvergrownWillowFoliageBlock(0.01F, properties),
      Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(SiftMapColors.WILLOW_FOLIAGE).destroyTime(0.7F).sound(SoundType.NETHER_WART)
   );
   public static final BlockItem OVERGROWN_WILLOW_FOLIAGE_ITEM = registerBlockItem("overgrown_willow_foliage", OVERGROWN_WILLOW_FOLIAGE);
   public static final Block OVERGROWN_WILLOW_SAPLING = registerBlock(
      "overgrown_willow_sapling",
      OvergrownWillowSaplingBlock::new,
      Properties.ofFullCopy(Blocks.OAK_SAPLING).mapColor(SiftMapColors.WILLOW_FOLIAGE).randomTicks().noCollision()
   );
   public static final BlockItem OVERGROWN_WILLOW_SAPLING_ITEM = registerBlockItem("overgrown_willow_sapling", OVERGROWN_WILLOW_SAPLING);
   public static final Block ASH_WILLOW_SAPLING = registerBlock(
      "ash_willow_sapling",
      properties -> new OvergrownWillowSaplingBlock(properties, true),
      Properties.ofFullCopy(Blocks.OAK_SAPLING).mapColor(MapColor.COLOR_BLACK).randomTicks().noCollision()
   );
   public static final BlockItem ASH_WILLOW_SAPLING_ITEM = registerBlockItem("ash_willow_sapling", ASH_WILLOW_SAPLING);
   public static final Block OVERGROWN_WILLOW_VINES = registerBlock(
      "overgrown_willow_vines", OvergrownWillowVinesBlock::new, Properties.ofFullCopy(Blocks.VINE).mapColor(SiftMapColors.WILLOW_VINES)
   );
   public static final BlockItem OVERGROWN_WILLOW_VINES_ITEM = registerBlockItem("overgrown_willow_vines", OVERGROWN_WILLOW_VINES);
   public static final Block OVERGROWN_WILLOW_VINES_PLANT = registerBlock(
      "overgrown_willow_vines_plant", OvergrownWillowVinesPlantBlock::new, Properties.ofFullCopy(Blocks.VINE).mapColor(SiftMapColors.WILLOW_VINES)
   );
   public static final Block ASH_WILLOW_PLANKS = registerBlock(
      "ash_willow_planks", Block::new, Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_PLANKS_ITEM = registerBlockItem("ash_willow_planks", ASH_WILLOW_PLANKS);
   public static final Block ASH_WILLOW_STAIRS = registerBlock(
      "ash_willow_stairs",
      properties -> new StairBlock(ASH_WILLOW_PLANKS.defaultBlockState(), properties),
      Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_STAIRS_ITEM = registerBlockItem("ash_willow_stairs", ASH_WILLOW_STAIRS);
   public static final Block ASH_WILLOW_SLAB = registerBlock(
      "ash_willow_slab", SlabBlock::new, Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_SLAB_ITEM = registerBlockItem("ash_willow_slab", ASH_WILLOW_SLAB);
   public static final Block ASH_WILLOW_FENCE = registerBlock(
      "ash_willow_fence", FenceBlock::new, Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_FENCE_ITEM = registerBlockItem("ash_willow_fence", ASH_WILLOW_FENCE);
   public static final Block ASH_WILLOW_FENCE_GATE = registerBlock(
      "ash_willow_fence_gate",
      properties -> new FenceGateBlock(WoodType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_FENCE_GATE_ITEM = registerBlockItem("ash_willow_fence_gate", ASH_WILLOW_FENCE_GATE);
   public static final Block ASH_WILLOW_DOOR = registerBlock(
      "ash_willow_door", properties -> new DoorBlock(BlockSetType.OAK, properties), Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_DOOR_ITEM = registerBlockItem("ash_willow_door", ASH_WILLOW_DOOR);
   public static final Block ASH_WILLOW_TRAPDOOR = registerBlock(
      "ash_willow_trapdoor",
      properties -> new TrapDoorBlock(BlockSetType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_TRAPDOOR_ITEM = registerBlockItem("ash_willow_trapdoor", ASH_WILLOW_TRAPDOOR);
   public static final Block ASH_WILLOW_PRESSURE_PLATE = registerBlock(
      "ash_willow_pressure_plate",
      properties -> new PressurePlateBlock(BlockSetType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_PRESSURE_PLATE_ITEM = registerBlockItem("ash_willow_pressure_plate", ASH_WILLOW_PRESSURE_PLATE);
   public static final Block ASH_WILLOW_BUTTON = registerBlock(
      "ash_willow_button",
      properties -> new ButtonBlock(BlockSetType.OAK, 30, properties),
      Properties.ofFullCopy(Blocks.OAK_BUTTON).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_BUTTON_ITEM = registerBlockItem("ash_willow_button", ASH_WILLOW_BUTTON);
   public static final Block ASH_WILLOW_SHELF = registerBlock(
      "ash_willow_shelf", ShelfBlock::new, Properties.ofFullCopy(Blocks.OAK_SHELF).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_SHELF_ITEM = registerBlockItem("ash_willow_shelf", ASH_WILLOW_SHELF);
   public static final Block ASH_WILLOW_SIGN = registerBlock(
      "ash_willow_sign", properties -> new StandingSignBlock(WoodType.OAK, properties), Properties.ofFullCopy(Blocks.OAK_SIGN).mapColor(MapColor.COLOR_BLACK)
   );
   public static final Block ASH_WILLOW_WALL_SIGN = registerBlock(
      "ash_willow_wall_sign",
      properties -> new WallSignBlock(WoodType.OAK, properties),
      dropsLike(Properties.ofFullCopy(Blocks.OAK_WALL_SIGN).mapColor(MapColor.COLOR_BLACK), "ash_willow_sign")
   );
   public static final Item ASH_WILLOW_SIGN_ITEM = registerItem(
      "ash_willow_sign", properties -> new StandingAndWallBlockItem(ASH_WILLOW_SIGN, ASH_WILLOW_WALL_SIGN, Direction.DOWN, properties)
   );
   public static final Block ASH_WILLOW_HANGING_SIGN = registerBlock(
      "ash_willow_hanging_sign",
      properties -> new CeilingHangingSignBlock(WoodType.OAK, properties),
      Properties.ofFullCopy(Blocks.OAK_HANGING_SIGN).mapColor(MapColor.COLOR_BLACK)
   );
   public static final Block ASH_WILLOW_WALL_HANGING_SIGN = registerBlock(
      "ash_willow_wall_hanging_sign",
      properties -> new WallHangingSignBlock(WoodType.OAK, properties),
      dropsLike(Properties.ofFullCopy(Blocks.OAK_WALL_HANGING_SIGN).mapColor(MapColor.COLOR_BLACK), "ash_willow_hanging_sign")
   );
   public static final Item ASH_WILLOW_HANGING_SIGN_ITEM = registerItem(
      "ash_willow_hanging_sign", properties -> new HangingSignItem(ASH_WILLOW_HANGING_SIGN, ASH_WILLOW_WALL_HANGING_SIGN, properties)
   );
   public static final Block ASH_WILLOW_LOG = registerBlock(
      "ash_willow_log", RotatedPillarBlock::new, Properties.ofFullCopy(OVERGROWN_WILLOW_LOG).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_LOG_ITEM = registerBlockItem("ash_willow_log", ASH_WILLOW_LOG);
   public static final Block ASH_WILLOW_WOOD = registerBlock(
      "ash_willow_wood", RotatedPillarBlock::new, Properties.ofFullCopy(OVERGROWN_WILLOW_WOOD).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem ASH_WILLOW_WOOD_ITEM = registerBlockItem("ash_willow_wood", ASH_WILLOW_WOOD);
   public static final Block STRIPPED_ASH_WILLOW_LOG = registerBlock(
      "stripped_ash_willow_log", RotatedPillarBlock::new, Properties.ofFullCopy(STRIPPED_OVERGROWN_WILLOW_LOG).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem STRIPPED_ASH_WILLOW_LOG_ITEM = registerBlockItem("stripped_ash_willow_log", STRIPPED_ASH_WILLOW_LOG);
   public static final Block STRIPPED_ASH_WILLOW_WOOD = registerBlock(
      "stripped_ash_willow_wood", RotatedPillarBlock::new, Properties.ofFullCopy(STRIPPED_OVERGROWN_WILLOW_WOOD).mapColor(MapColor.COLOR_BLACK)
   );
   public static final BlockItem STRIPPED_ASH_WILLOW_WOOD_ITEM = registerBlockItem("stripped_ash_willow_wood", STRIPPED_ASH_WILLOW_WOOD);
   public static final Block ASH_WILLOW_FOLIAGE = registerBlock(
      "ash_willow_foliage",
      properties -> new OvergrownWillowFoliageBlock(0.01F, properties),
      Properties.ofFullCopy(OVERGROWN_WILLOW_FOLIAGE).mapColor(SiftMapColors.ASH_FOLIAGE)
   );
   public static final BlockItem ASH_WILLOW_FOLIAGE_ITEM = registerBlockItem("ash_willow_foliage", ASH_WILLOW_FOLIAGE);
   public static final Block ASH_WILLOW_VINES = registerBlock(
      "ash_willow_vines", OvergrownWillowVinesBlock::new, Properties.ofFullCopy(OVERGROWN_WILLOW_VINES).mapColor(SiftMapColors.ASH_VINES)
   );
   public static final BlockItem ASH_WILLOW_VINES_ITEM = registerBlockItem("ash_willow_vines", ASH_WILLOW_VINES);
   public static final Block ASH_WILLOW_VINES_PLANT = registerBlock(
      "ash_willow_vines_plant", OvergrownWillowVinesPlantBlock::new, Properties.ofFullCopy(OVERGROWN_WILLOW_VINES_PLANT).mapColor(SiftMapColors.ASH_VINES)
   );
   public static final DropExperienceBlock SIFTSLATE_COAL_ORE = registerBlock(
      "siftslate_coal_ore",
      properties -> new DropExperienceBlock(UniformInt.of(0, 2), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_COAL_ORE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_COAL_ORE_ITEM = registerBlockItem("siftslate_coal_ore", SIFTSLATE_COAL_ORE);
   public static final DropExperienceBlock SIFTSLATE_DIAMOND_ORE = registerBlock(
      "siftslate_diamond_ore",
      properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_DIAMOND_ORE_ITEM = registerBlockItem("siftslate_diamond_ore", SIFTSLATE_DIAMOND_ORE);
   public static final DropExperienceBlock SIFTSLATE_EMERALD_ORE = registerBlock(
      "siftslate_emerald_ore",
      properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_EMERALD_ORE_ITEM = registerBlockItem("siftslate_emerald_ore", SIFTSLATE_EMERALD_ORE);
   public static final DropExperienceBlock SIFTSLATE_CHAROITE_ORE = registerBlock(
      "siftslate_charoite_ore",
      properties -> new DropExperienceBlock(UniformInt.of(2, 5), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_LAPIS_ORE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_CHAROITE_ORE_ITEM = registerBlockItem("siftslate_charoite_ore", SIFTSLATE_CHAROITE_ORE);
   public static final DropExperienceBlock SIFTSLATE_SIFTITE_ORE = registerBlock(
      "siftslate_siftite_ore",
      properties -> new DropExperienceBlock(UniformInt.of(3, 7), properties),
      Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).mapColor(SiftMapColors.SIFTSLATE)
   );
   public static final BlockItem SIFTSLATE_SIFTITE_ORE_ITEM = registerBlockItem("siftslate_siftite_ore", SIFTSLATE_SIFTITE_ORE);
   public static final ResourceKey<Block> REINFORCED_SIFTSLATE_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "reinforced_siftslate")
   );
   public static final Block REINFORCED_SIFTSLATE = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      REINFORCED_SIFTSLATE_KEY,
      new Block(
         Properties.ofFullCopy(Blocks.REINFORCED_DEEPSLATE).mapColor(SiftMapColors.SIFTSLATE).strength(-1.0F, 3600000.0F).setId(REINFORCED_SIFTSLATE_KEY)
      )
   );
   public static final ResourceKey<Item> REINFORCED_SIFTSLATE_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "reinforced_siftslate")
   );
   public static final BlockItem REINFORCED_SIFTSLATE_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      REINFORCED_SIFTSLATE_ITEM_KEY,
      new BlockItem(REINFORCED_SIFTSLATE, new net.minecraft.world.item.Item.Properties().setId(REINFORCED_SIFTSLATE_ITEM_KEY).useBlockDescriptionPrefix())
   );
   public static final ResourceKey<Block> REINFORCED_SOULSHALE_KEY = ResourceKey.create(
      BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", "reinforced_soulshale")
   );
   public static final Block REINFORCED_SOULSHALE = ModRegistration.register(
      BuiltInRegistries.BLOCK,
      REINFORCED_SOULSHALE_KEY,
      new Block(
         Properties.ofFullCopy(Blocks.REINFORCED_DEEPSLATE)
            .mapColor(SiftMapColors.SOULSHALE)
            .sound(SoulshaleSoundType.INSTANCE)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .setId(REINFORCED_SOULSHALE_KEY)
      )
   );
   public static final ResourceKey<Item> REINFORCED_SOULSHALE_ITEM_KEY = ResourceKey.create(
      BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", "reinforced_soulshale")
   );
   public static final BlockItem REINFORCED_SOULSHALE_ITEM = ModRegistration.register(
      BuiltInRegistries.ITEM,
      REINFORCED_SOULSHALE_ITEM_KEY,
      new BlockItem(REINFORCED_SOULSHALE, new net.minecraft.world.item.Item.Properties().setId(REINFORCED_SOULSHALE_ITEM_KEY).useBlockDescriptionPrefix())
   );
   public static final BlockEntityType<IchorCauldronBlockEntity> ICHOR_CAULDRON_BLOCK_ENTITY = ModRegistration.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE,
      Identifier.fromNamespaceAndPath("the_sift", "ichor_cauldron"),
      new BlockEntityType<>(IchorCauldronBlockEntity::new, Set.of(ICHOR_CAULDRON))
   );

   private static ResourceKey<Block> blockKey(String name) {
      return ResourceKey.create(BuiltInRegistries.BLOCK.key(), Identifier.fromNamespaceAndPath("the_sift", name));
   }

   private static ResourceKey<Item> itemKey(String name) {
      return ResourceKey.create(BuiltInRegistries.ITEM.key(), Identifier.fromNamespaceAndPath("the_sift", name));
   }

   private static <T extends Block> T registerBlock(String name, Function<Properties, T> factory, Properties properties) {
      ResourceKey<Block> key = blockKey(name);
      return ModRegistration.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
   }

   private static BlockItem registerBlockItem(String name, Block block) {
      return registerBlockItem(name, block, new net.minecraft.world.item.Item.Properties());
   }

   private static BlockItem registerBlockItem(String name, Block block, net.minecraft.world.item.Item.Properties properties) {
      ResourceKey<Item> key = itemKey(name);
      return ModRegistration.register(BuiltInRegistries.ITEM, key, new BlockItem(block, properties.setId(key).useBlockDescriptionPrefix()));
   }

   private static <T extends Item> T registerItem(String name, Function<net.minecraft.world.item.Item.Properties, T> factory) {
      ResourceKey<Item> key = itemKey(name);
      return ModRegistration.register(BuiltInRegistries.ITEM, key, factory.apply(new net.minecraft.world.item.Item.Properties().setId(key)));
   }

   private static Properties dropsLike(Properties properties, String lootTableName) {
      ResourceKey<LootTable> lootTable = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("the_sift", "blocks/" + lootTableName));
      return properties.overrideLootTable(Optional.of(lootTable));
   }

   private static Compostable compostable(int chancePercent) {
      return new Compostable(new Constant(chancePercent));
   }

   public static void bootstrap() {
   }

   public static void modifyDefaultComponents(ModifyDefaultComponentsEvent event) {
      event.modify(OVERGROWN_CHARD_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(OVERGROWN_STALKS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(OVERGROWN_FRONDS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(BLUE_SAUCER_BLOOM_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(65)));
      event.modify(GREEN_SAUCER_BLOOM_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(65)));
      event.modify(SUNBURST_PLANT_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(65)));
      event.modify(WHISPERBLOOM_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(65)));
      event.modify(GRIM_BUSH_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(SIFTSLATE_STALKS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(HEALTHY_SCULK_SPROUTS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(HEALTHY_SCULK_CARTILAGE_SPROUT_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(DRY_HEALTHY_SCULK_SPROUTS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(SIFTSLATE_HANGING_ROOTS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(OVERGROWN_HANGING_ROOTS_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(OVERGROWN_WILLOW_FOLIAGE_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(OVERGROWN_WILLOW_SAPLING_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(ASH_WILLOW_SAPLING_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(OVERGROWN_WILLOW_VINES_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(50)));
      event.modify(ASH_WILLOW_FOLIAGE_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(30)));
      event.modify(ASH_WILLOW_VINES_ITEM, (builder, context, item) -> builder.set(DataComponents.COMPOSTABLE, compostable(50)));
   }

   public static void addVanillaBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
      event.modify(BlockEntityTypes.SIGN, OVERGROWN_WILLOW_SIGN, ASH_WILLOW_SIGN, OVERGROWN_WILLOW_WALL_SIGN, ASH_WILLOW_WALL_SIGN);
      event.modify(BlockEntityTypes.HANGING_SIGN, OVERGROWN_WILLOW_HANGING_SIGN, ASH_WILLOW_HANGING_SIGN, OVERGROWN_WILLOW_WALL_HANGING_SIGN, ASH_WILLOW_WALL_HANGING_SIGN);
      event.modify(BlockEntityTypes.SHELF, OVERGROWN_WILLOW_SHELF, ASH_WILLOW_SHELF);
   }

   public static void initialize() {
      IchorCauldronInteractions.register();
      FireBlock flammable = (FireBlock)Blocks.FIRE;
      flammable.setFlammable(OVERGROWN_WILLOW_LOG, 5, 5);
      flammable.setFlammable(OVERGROWN_WILLOW_WOOD, 5, 5);
      flammable.setFlammable(STRIPPED_OVERGROWN_WILLOW_LOG, 5, 5);
      flammable.setFlammable(STRIPPED_OVERGROWN_WILLOW_WOOD, 5, 5);
      flammable.setFlammable(OVERGROWN_WILLOW_PLANKS, 5, 20);
      flammable.setFlammable(ASH_WILLOW_PLANKS, 5, 20);
      flammable.setFlammable(OVERGROWN_WILLOW_STAIRS, 5, 20);
      flammable.setFlammable(ASH_WILLOW_STAIRS, 5, 20);
      flammable.setFlammable(OVERGROWN_WILLOW_SLAB, 5, 20);
      flammable.setFlammable(ASH_WILLOW_SLAB, 5, 20);
      flammable.setFlammable(OVERGROWN_WILLOW_FENCE, 5, 20);
      flammable.setFlammable(ASH_WILLOW_FENCE, 5, 20);
      flammable.setFlammable(OVERGROWN_WILLOW_FENCE_GATE, 5, 20);
      flammable.setFlammable(ASH_WILLOW_FENCE_GATE, 5, 20);
      flammable.setFlammable(OVERGROWN_WILLOW_SHELF, 30, 20);
      flammable.setFlammable(ASH_WILLOW_SHELF, 30, 20);
      flammable.setFlammable(OVERGROWN_WILLOW_FOLIAGE, 30, 60);
      flammable.setFlammable(ASH_WILLOW_LOG, 5, 5);
      flammable.setFlammable(ASH_WILLOW_WOOD, 5, 5);
      flammable.setFlammable(STRIPPED_ASH_WILLOW_LOG, 5, 5);
      flammable.setFlammable(STRIPPED_ASH_WILLOW_WOOD, 5, 5);
      flammable.setFlammable(ASH_WILLOW_FOLIAGE, 30, 60);
      flammable.setFlammable(OVERGROWN_WILLOW_SAPLING, 60, 100);
      flammable.setFlammable(ASH_WILLOW_SAPLING, 60, 100);
   }
}
