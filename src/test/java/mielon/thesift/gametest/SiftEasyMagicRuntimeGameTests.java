package mielon.thesift.gametest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import mielon.thesift.item.CharoiteEnchanting;
import mielon.thesift.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.fml.ModList;

/** Test-only isolated runtime: exercises real enchanted-menu mixins and item tags. */
final class SiftEasyMagicRuntimeGameTests {
    private SiftEasyMagicRuntimeGameTests() {}

    static void actualMenuContract(GameTestHelper helper) {
        try {
            BlockPos table = helper.absolutePos(new BlockPos(4, 4, 4));
            helper.getLevel().setBlock(table.below(), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(table, Blocks.ENCHANTING_TABLE.defaultBlockState(), 3);
            for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                helper.getLevel().setBlock(table.offset(offset), Blocks.BOOKSHELF.defaultBlockState(), 3);
            }
            // MC26.1.2 supplies a survival-mode mock Player, not makeMockServerPlayer(GameType).
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.giveExperienceLevels(100);
            ContainerLevelAccess access = ContainerLevelAccess.create(helper.getLevel(), table);
            boolean optional = ModList.get().isLoaded("easymagic");
            EnchantmentMenu menu = optional
                ? createEasyMagicMenu(player.getInventory(), access)
                : new EnchantmentMenu(31, player.getInventory(), access);

            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(Items.LAPIS_LAZULI)),
                "Normal Lapis must remain accepted in enchantment catalyst slot");
            helper.assertTrue(menu.getSlot(1).mayPlace(new ItemStack(ModItems.CHAROITE)),
                "Charoite must be accepted in enchantment catalyst slot");
            menu.getSlot(0).set(sword);
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));
            Container container = menu.getSlot(0).container;
            menu.slotsChanged(container);
            int[] baseline = menu.costs.clone();
            helper.assertTrue(Arrays.stream(baseline).anyMatch(cost -> cost > 0), "No normal enchantment costs to compare");

            menu.getSlot(1).set(new ItemStack(ModItems.CHAROITE, 64));
            menu.slotsChanged(container);
            int[] charoite = menu.costs.clone();
            for (int i = 0; i < baseline.length; i++) {
                helper.assertTrue(charoite[i] == (baseline[i] > 0 ? baseline[i] + 20 : baseline[i]),
                    "Charoite cost is not base +20 in offer #" + i + " baseline=" +
                        Arrays.toString(baseline) + " actual=" + Arrays.toString(charoite));
            }

            if (optional) {
                helper.assertTrue(menu.slots.size() > 2,
                    "Easy Magic did not add separate reroll catalyst slot");
                Class<?> target = menu.getClass();
                // Recalculate by the real menu entry point rather than
                // passing an arbitrary enchanting power that may differ
                // from the world's actual bookshelf arrangement.
                menu.slotsChanged(container);
                System.out.println("PRIME2_EASYMAGIC_RECALC_ONE baseline=" +
                    Arrays.toString(baseline) + " initialCharoite=" +
                    Arrays.toString(charoite) + " recalc=" +
                    Arrays.toString(menu.costs));
                helper.assertTrue(Arrays.equals(charoite, menu.costs),
                    "Easy Magic recalc accumulated the Charoite +20 bonus");
                menu.slotsChanged(container);
                helper.assertTrue(Arrays.equals(charoite, menu.costs),
                    "Easy Magic second recalc accumulated bonus or changed enchantment costs");
                Field clueField = target.getField("clues");
                List<?> clues = (List<?>) clueField.get(menu);
                helper.assertTrue(clues.size() == 3, "Easy Magic clues not synchronized for three offers");
                // Real Easy Magic reroll action (button 4). Its default
                // configuration uses the enchanting catalyst as reroll fuel.
                int rerollCatalystBefore = menu.getSlot(1).getItem().getCount();
                helper.assertTrue(menu.clickMenuButton(player, 4),
                    "Easy Magic refused a reroll with sufficient XP and Charoite catalyst");
                helper.assertTrue(menu.getSlot(1).getItem().getCount() == rerollCatalystBefore - 1,
                    "Easy Magic reroll did not consume exactly one catalyst");
                int[] rerolledCharoite = menu.costs.clone();
                menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));
                menu.slotsChanged(container);
                int[] rerolledLapis = menu.costs.clone();
                menu.getSlot(1).set(new ItemStack(ModItems.CHAROITE, 64));
                menu.slotsChanged(container);
                for (int i = 0; i < rerolledLapis.length; i++) {
                    helper.assertTrue(menu.costs[i] == (rerolledLapis[i] > 0 ? rerolledLapis[i] + 20 : rerolledLapis[i]),
                        "Reroll or catalyst swap cumulatively increased Charoite costs #" + i);
                }
                helper.assertTrue(Arrays.equals(menu.costs, rerolledCharoite),
                    "Rerolled offer changed again even though the enchantment seed remained the same");
                System.out.println("PRIME2_EASYMAGIC_REROLL_PASS costs=" +
                    Arrays.toString(rerolledCharoite) + " catalyst=" +
                    rerollCatalystBefore + "->" + (rerollCatalystBefore - 1));
                System.out.println("PRIME2_EASYMAGIC_REAL_MENU_PASS costs=" + Arrays.toString(charoite));
            } else {
                System.out.println("PRIME2_VANILLA_ENCHANT_MENU_PASS costs=" + Arrays.toString(charoite));
            }
            int levelsBefore = player.experienceLevel;
            int catalystBefore = menu.getSlot(1).getItem().getCount();
            boolean clicked = menu.clickMenuButton(player, 0);
            helper.assertTrue(clicked, "The menu refused a valid Charoite enchantment with 100 XP levels");
            ItemStack result = menu.getSlot(0).getItem();
            helper.assertTrue(CharoiteEnchanting.wasUsed(result),
                "A successful Charoite enchantment did not mark the output item");
            helper.assertTrue(menu.getSlot(1).getItem().getCount() == catalystBefore - 1,
                "Charoite catalyst consumption was not exactly one item");
            helper.assertTrue(player.experienceLevel == levelsBefore - 1,
                "XP consumption for the first enchanted offer was not exactly one level");
            System.out.println("PRIME2_" + (optional ? "EASYMAGIC" : "VANILLA") +
                "_REAL_ENCHANT_PASS xp=" + levelsBefore + "->" + player.experienceLevel +
                " catalyst=" + catalystBefore + "->" + menu.getSlot(1).getItem().getCount());
            helper.succeed();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw helper.assertionException("Actual enchantment-menu runtime test failed: " + ex);
        }
    }

    private static EnchantmentMenu createEasyMagicMenu(Inventory inventory, ContainerLevelAccess access)
        throws ReflectiveOperationException {
        Class<?> cls = Class.forName("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu");
        Constructor<?> constructor = cls.getConstructor(int.class, Inventory.class, Container.class,
            ContainerLevelAccess.class);
        return (EnchantmentMenu) constructor.newInstance(31, inventory, new SimpleContainer(3), access);
    }
}
