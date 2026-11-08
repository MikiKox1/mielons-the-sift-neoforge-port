package mielon.thesift.gametest;

import java.util.List;
import java.util.Map;
import mielon.thesift.block.BlubbyBerriesBushBlock;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SculkflowerCropBlock;
import mielon.thesift.enchantment.SoulAbsorption;
import mielon.thesift.entity.EchoGolemEntity;
import mielon.thesift.entity.EchoGolemSummoning;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.MonarchEntity;
import mielon.thesift.entity.MonarchRitual;
import mielon.thesift.entity.SnifferInfectionAccess;
import mielon.thesift.fluid.IchorRegeneration;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.item.ModFuels;
import mielon.thesift.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Assertions against native Minecraft server entities, blocks, fluids and NeoForge data maps. */
final class SiftServerGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 4, 4);

    private SiftServerGameTests() {
    }

    static void mobRegistrationAttributes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<EntityType<? extends Mob>> types = List.of(
                ModEntities.SINGER, ModEntities.ECHO_GOLEM, ModEntities.DARK_SNIFFER,
                ModEntities.BLUB, ModEntities.SEEDLING, ModEntities.SCAVENGER,
                ModEntities.MONARCH, ModEntities.MONARCH_ECHO, ModEntities.STALKER,
                ModEntities.NUZZLE, ModEntities.SLABBER, ModEntities.NESTER, ModEntities.POLLINATOR);
        helper.assertValueEqual(types.size(), 13, "all custom mob types included");
        for (EntityType<? extends Mob> type : types) {
            assertRegistered(helper, type);
            Mob entity = type.create(level, EntitySpawnReason.COMMAND);
            helper.assertTrue(entity != null, "entity constructor: " + type);
            helper.assertTrue(entity.getAttribute(Attributes.MAX_HEALTH) != null, "missing max-health attribute: " + type);
            helper.assertTrue(entity.getMaxHealth() > 0 && entity.getHealth() > 0, "invalid health: " + type);
        }
        Sniffer sniffer = EntityTypes.SNIFFER.create(level, EntitySpawnReason.COMMAND);
        helper.assertTrue(sniffer instanceof SnifferInfectionAccess, "vanilla Sniffer missing port infection access");
        SnifferInfectionAccess infection = (SnifferInfectionAccess) sniffer;
        infection.theSift$setSculkInfectionTicks(37);
        helper.assertTrue(infection.theSift$isSculkInfected() && infection.theSift$getSculkInfectionTicks() == 37,
                "Sniffer infection failed to store/synchronize ticks");
        infection.theSift$setSculkInfectionTicks(0);
        helper.assertTrue(!infection.theSift$isSculkInfected() && infection.theSift$getSculkInfectionTicks() == 0,
                "Sniffer infection did not reset");
        helper.succeed();
    }

    static void utilityEntities(GameTestHelper helper) {
        List<EntityType<?>> types = List.of(
                ModEntities.SCAVENGER_PROJECTILE, ModEntities.MONARCH_SUMMON_PROJECTILE,
                ModEntities.MONARCH_EXPLOSION_PROJECTILE, ModEntities.POLLINATOR_PROJECTILE,
                ModEntities.RIFT, ModEntities.MINI_RIFT, ModEntities.SIFTITE_RETURN,
                ModEntities.ASH_WILLOW_BOAT, ModEntities.ASH_WILLOW_CHEST_BOAT,
                ModEntities.OVERGROWN_WILLOW_BOAT, ModEntities.OVERGROWN_WILLOW_CHEST_BOAT);
        helper.assertValueEqual(types.size(), 11, "all custom utility entity types included");
        for (EntityType<?> type : types) {
            assertRegistered(helper, type);
            Entity entity = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
            helper.assertTrue(entity != null && entity.getType() == type, "utility entity constructor: " + type);
        }
        helper.succeed();
    }

    private static void assertRegistered(GameTestHelper helper, EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        helper.assertTrue(id != null && "the_sift".equals(id.getNamespace()), "unregistered entity: " + type);
    }

    static void monarchEcho(GameTestHelper helper) {
        MonarchEntity echo = ModEntities.MONARCH_ECHO.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(echo != null && echo.isEcho() && echo.phase() == 0, "Monarch Echo must have its own mechanics");
        BlockPos pos = helper.absolutePos(CENTER);
        echo.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        helper.assertTrue(helper.getLevel().addFreshEntity(echo), "Monarch Echo was not added to the server world");
        helper.assertTrue(echo.getType() == ModEntities.MONARCH_ECHO, "Echo spawned with the regular Monarch type");
        helper.succeed();
    }

    static void ritualSourcesConsume(GameTestHelper helper) {
        BlockPos flower = placePedestal(helper, true, false);
        BlockPos base = flower.below(3);
        helper.assertTrue(monarchCount(helper.getLevel(), base) == 1, "placing the flower must summon exactly one Monarch");
        for (int y = 0; y <= 3; y++) {
            helper.assertTrue(helper.getLevel().getBlockState(base.above(y)).isAir(), "ritual must consume the vertical pedestal");
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x != 0 || z != 0) {
                    helper.assertTrue(!helper.getLevel().getFluidState(base.offset(x, 0, z)).isSource(),
                            "ritual must consume the Ichor sources");
                }
            }
        }
        helper.assertTrue(!MonarchRitual.trySummon(helper.getLevel(), flower), "a consumed ritual cannot run twice");
        helper.assertTrue(monarchCount(helper.getLevel(), base) == 1, "no duplicate Monarch");
        helper.succeed();
    }

    static void ritualMissingSource(GameTestHelper helper) {
        BlockPos flower = placePedestal(helper, false, false);
        helper.assertTrue(!MonarchRitual.matches(helper.getLevel(), flower), "one missing Ichor source must invalidate the ritual");
        helper.assertTrue(!MonarchRitual.trySummon(helper.getLevel(), flower), "incomplete ritual succeeded");
        helper.assertTrue(helper.getLevel().getBlockState(flower).is(ModBlocks.SCULKFLOWER), "failed ritual consumed its flower");
        helper.assertTrue(monarchCount(helper.getLevel(), flower.below(3)) == 0, "incomplete ritual spawned Monarch");
        helper.succeed();
    }

    static void ritualFlowingIchor(GameTestHelper helper) {
        BlockPos flower = placePedestal(helper, true, true);
        helper.assertTrue(!MonarchRitual.matches(helper.getLevel(), flower), "flowing Ichor must not count as a source");
        helper.assertTrue(!MonarchRitual.trySummon(helper.getLevel(), flower), "flowing Ichor summoned Monarch");
        helper.assertTrue(helper.getLevel().getBlockState(flower).is(ModBlocks.SCULKFLOWER), "flowing ritual consumed blocks");
        helper.succeed();
    }

    static void ritualPeaceful(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL,
                "GameTest Peaceful environment was not applied");
        BlockPos flower = placePedestal(helper, true, false);
        helper.assertTrue(MonarchRitual.matches(helper.getLevel(), flower), "valid pedestal setup in Peaceful");
        helper.assertTrue(!MonarchRitual.trySummon(helper.getLevel(), flower), "Peaceful ritual summoned Monarch");
        helper.assertTrue(monarchCount(helper.getLevel(), flower.below(3)) == 0, "Peaceful spawned Monarch");
        helper.assertTrue(helper.getLevel().getBlockState(flower).is(ModBlocks.SCULKFLOWER), "Peaceful consumed flower");
        helper.succeed();
    }

    private static BlockPos placePedestal(GameTestHelper helper, boolean allSources, boolean oneFlowing) {
        ServerLevel level = helper.getLevel();
        BlockPos flower = helper.absolutePos(CENTER.above(2));
        BlockPos base = flower.below(3);
        level.setBlock(base, ModBlocks.SOUL_BLOCK.defaultBlockState(), 3);
        level.setBlock(base.above(), ModBlocks.SOULSHALE_TOTEM.defaultBlockState(), 3);
        level.setBlock(base.above(2), ModBlocks.ROYAL_SCULK.defaultBlockState(), 3);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0 || x == 1 && z == 0 && !allSources) continue;
                BlockState ichor = ModFluids.ICHOR_BLOCK.defaultBlockState();
                if (x == 1 && z == 0 && oneFlowing) ichor = ichor.setValue(LiquidBlock.LEVEL, 1);
                level.setBlock(base.offset(x, 0, z), ichor, 3);
            }
        }
        // The flower's onPlace hook is the real ritual trigger.
        level.setBlock(flower, ModBlocks.SCULKFLOWER.defaultBlockState(), 3);
        return flower;
    }

    private static long monarchCount(ServerLevel level, BlockPos base) {
        return level.getEntitiesOfClass(MonarchEntity.class, new AABB(base).inflate(5.0)).stream()
                .filter(entity -> entity.getType() == ModEntities.MONARCH).count();
    }

    static void echoGolemRejectsIncorrectBase(GameTestHelper helper) {
        BlockPos pumpkin = helper.absolutePos(CENTER);
        helper.getLevel().setBlock(pumpkin.below(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pumpkin, Blocks.CARVED_PUMPKIN.defaultBlockState(), 3);
        helper.assertTrue(!EchoGolemSummoning.trySpawn(helper.getLevel(), pumpkin), "wrong base summoned an Echo Golem");
        helper.assertTrue(helper.getLevel().getBlockState(pumpkin).is(Blocks.CARVED_PUMPKIN), "rejected summon consumed pumpkin");
        helper.succeed();
    }

    static void echoGolemSingleSummon(GameTestHelper helper) {
        BlockPos pumpkin = helper.absolutePos(CENTER);
        helper.getLevel().setBlock(pumpkin.below(), ModBlocks.POLISHED_SOULSHALE.defaultBlockState(), 3);
        helper.getLevel().setBlock(pumpkin, Blocks.CARVED_PUMPKIN.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(pumpkin).isAir(), "automatic Echo Golem summon did not consume pumpkin");
        helper.assertTrue(helper.getLevel().getBlockState(pumpkin.below()).isAir(), "Echo Golem base was not consumed");
        List<EchoGolemEntity> golems = helper.getLevel().getEntitiesOfClass(EchoGolemEntity.class, new AABB(pumpkin).inflate(4.0));
        helper.assertValueEqual(golems.size(), 1, "exactly one real Echo Golem must be added to world");
        helper.assertTrue(golems.getFirst().getType() == ModEntities.ECHO_GOLEM, "incorrect Echo Golem type");
        helper.assertTrue(!EchoGolemSummoning.trySpawn(helper.getLevel(), pumpkin), "same base spawned Echo Golem twice");
        helper.succeed();
    }

    static void blubbyBerriesBonemealHarvest(GameTestHelper helper) {
        BlockPos lower = helper.absolutePos(CENTER);
        BlubbyBerriesBushBlock bush = (BlubbyBerriesBushBlock) ModBlocks.BLUBBY_BERRIES_BUSH;
        helper.getLevel().setBlock(lower.below(), Blocks.DIRT.defaultBlockState(), 3);
        helper.getLevel().setBlock(lower, bush.defaultBlockState(), 3);
        helper.getLevel().setBlock(lower.above(), bush.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 3);
        BlockState state = helper.getLevel().getBlockState(lower);
        helper.assertTrue(bush.isValidBonemealTarget(helper.getLevel(), lower, state, BonemealSource.INTERACTION),
                "fresh berry bush cannot be bonemealed");
        bush.performBonemeal(helper.getLevel(), helper.getLevel().getRandom(), lower, state, BonemealSource.INTERACTION);
        helper.assertTrue(helper.getLevel().getBlockState(lower).getValue(BlubbyBerriesBushBlock.GROWN), "bonemeal did not grow berries");
        bush.useWithoutItem(helper.getLevel().getBlockState(lower), helper.getLevel(), lower,
                helper.makeMockServerPlayer(GameType.SURVIVAL), new BlockHitResult(Vec3.atCenterOf(lower), Direction.UP, lower, false));
        helper.assertTrue(!helper.getLevel().getBlockState(lower).getValue(BlubbyBerriesBushBlock.GROWN), "harvest did not reset berries");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(lower).inflate(2.0));
        helper.assertValueEqual(drops.size(), 1, "harvest should drop exactly one stack");
        ItemStack stack = drops.getFirst().getItem();
        helper.assertTrue(stack.is(ModItems.BLUBBY_BERRIES) && stack.getCount() >= 1 && stack.getCount() <= 4,
                "berry harvest count or item is invalid");
        helper.succeed();
    }

    static void sculkflowerCropBonemeal(GameTestHelper helper) {
        BlockPos cropPos = helper.absolutePos(CENTER);
        SculkflowerCropBlock crop = ModBlocks.SCULKFLOWER_CROP;
        helper.getLevel().setBlock(cropPos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        BlockState ageOne = crop.getStateForAge(1);
        helper.getLevel().setBlock(cropPos, ageOne, 3);
        crop.performBonemeal(helper.getLevel(), helper.getLevel().getRandom(), cropPos, ageOne, BonemealSource.INTERACTION);
        helper.assertTrue(helper.getLevel().getBlockState(cropPos).is(ModBlocks.SCULKFLOWER),
                "age-2 sculkflower crop did not become a flower after bonemeal");
        helper.succeed();
    }

    static void ichorSoulAbsorption(GameTestHelper helper) {
        BlockPos ichor = helper.absolutePos(CENTER);
        helper.getLevel().setBlock(ichor, ModFluids.ICHOR_BLOCK.defaultBlockState(), 3);
        Cow cow = helper.spawn(EntityTypes.COW, CENTER, EntitySpawnReason.COMMAND);
        helper.assertTrue(IchorRegeneration.isTouching(cow), "real cow in Ichor is not detected as touching");
        helper.assertValueEqual(SoulAbsorption.level(cow), 0, "unarmored cow has absorption");
        float initialHealth = cow.getHealth();
        IchorRegeneration.drink(cow);
        helper.assertTrue(cow.getHealth() < initialHealth, "unenchanted cow must take Ichor drinking damage");

        Mob enchanted = helper.spawn(EntityTypes.PILLAGER, CENTER.offset(5, 0, 0), EntitySpawnReason.COMMAND);
        ItemStack armor = new ItemStack(Items.DIAMOND_CHESTPLATE);
        armor.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(SoulAbsorption.KEY), 2);
        enchanted.setItemSlot(EquipmentSlot.CHEST, armor);
        helper.assertValueEqual(SoulAbsorption.level(enchanted), 2, "equipped Soul Absorption II was not resolved");
        IchorRegeneration.drink(enchanted);
        helper.assertTrue(enchanted.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
                "Soul Absorption II did not grant regeneration after drinking Ichor");
        helper.succeed();
    }

    static void willowNativeStrippables(GameTestHelper helper) {
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        checkStrippable(helper, player, 0, ModBlocks.OVERGROWN_WILLOW_LOG, ModBlocks.STRIPPED_OVERGROWN_WILLOW_LOG);
        checkStrippable(helper, player, 2, ModBlocks.OVERGROWN_WILLOW_WOOD, ModBlocks.STRIPPED_OVERGROWN_WILLOW_WOOD);
        checkStrippable(helper, player, 4, ModBlocks.ASH_WILLOW_LOG, ModBlocks.STRIPPED_ASH_WILLOW_LOG);
        checkStrippable(helper, player, 6, ModBlocks.ASH_WILLOW_WOOD, ModBlocks.STRIPPED_ASH_WILLOW_WOOD);
        helper.succeed();
    }

    private static void checkStrippable(GameTestHelper helper, Player player, int offset, Block original, Block expected) {
        BlockPos pos = helper.absolutePos(CENTER.offset(offset, 0, 0));
        BlockState source = original.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        helper.getLevel().setBlock(pos, source, 3);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        axe.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, axe,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        BlockState result = helper.getLevel().getBlockState(pos);
        helper.assertTrue(result.is(expected), "native 26.3 axe failed to strip " + original);
        helper.assertTrue(result.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z,
                "axe stripping did not retain wood axis for " + original);
    }

    static void nativeCompostables(GameTestHelper helper) {
        checkCompostables(helper, 30,
                "overgrown_chard overgrown_stalks overgrown_fronds grim_bush siftslate_stalks " +
                "healthy_sculk_sprouts healthy_sculk_cartilage_sprout dry_healthy_sculk_sprouts " +
                "siftslate_hanging_roots overgrown_hanging_roots overgrown_willow_foliage " +
                "overgrown_willow_sapling ash_willow_sapling ash_willow_foliage sculkflower_seeds");
        checkCompostables(helper, 50, "overgrown_willow_vines ash_willow_vines");
        checkCompostables(helper, 65,
                "blue_saucer_bloom green_saucer_bloom sunburst_plant whisperbloom sculkflower");
        helper.succeed();
    }

    private static void checkCompostables(GameTestHelper helper, int expectedLayers, String names) {
        for (String name : names.split(" ")) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("the_sift", name));
            helper.assertTrue(item != null && item != Items.AIR, "missing compostable item " + name);
            Compostable value = item.components().get(DataComponents.COMPOSTABLE);
            helper.assertTrue(value != null && value.layers() instanceof ResolvableInt.Constant,
                    "missing native 26.3 constant compost component for " + name + ": " + value);
            ResolvableInt.Constant layers = (ResolvableInt.Constant) value.layers();
            helper.assertValueEqual(layers.value(), expectedLayers, "wrong compost percentage for " + name);
        }
    }

    static void furnaceFuelEvents(GameTestHelper helper) {
        Map<Item, Item> equivalents = ModFuels.counterparts();
        helper.assertValueEqual(equivalents.size(), 38, "all 38 custom wood fuels registered");
        for (Map.Entry<Item, Item> entry : equivalents.entrySet()) {
            CookingFuel expected = entry.getValue().components().get(DataComponents.COOKING_FUEL);
            CookingFuel actual = entry.getKey().components().get(DataComponents.COOKING_FUEL);
            helper.assertTrue(expected != null, "vanilla counterpart has no native cooking fuel: " + entry.getValue());
            helper.assertValueEqual(actual, expected, "26.3 cooking fuel component differs from vanilla for " + entry.getKey());
        }
        helper.succeed();
    }
}
