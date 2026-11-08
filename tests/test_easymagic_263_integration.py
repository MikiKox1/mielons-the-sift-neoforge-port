"""Static, pinned Easy Magic 26.3 optional integration contract.

Runtime menu behavior is a separate QA gate. Do not infer it from static tests.
"""
import hashlib
import json
import re
import subprocess
import unittest
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
SIFT = ROOT.parents[1]
DEPS = SIFT / "reference/optional-mc26.3-20261108"
EASY = DEPS / "easymagic-v26.3.0-mc26.3.x+neoforge.jar"
PUZZLES = DEPS / "puzzleslib-v26.3.17-mc26.3.x+neoforge.jar"
EASY_SHA = "130afa86e341fed660320560ecabc32b9297ad58fdbae06cacece4af22fc6144"
PUZZLES_SHA = "ec5d2107373e72b6d8b382c53cf4fea837fb938ee55bf594ca8eb0594e3f8c0b"
JAVAP = Path(r"C:\Program Files\Java\jdk-25.0.4.1\bin\javap.exe")
ADAPTER = "src/main/java/mielon/thesift/mixin/compat/EasyMagicEnchantmentMenuMixin.java"
BRIDGE = "src/main/java/mielon/thesift/compat/easymagic/CharoiteEnchantmentMenuBridge.java"
VANILLA = "src/main/java/mielon/thesift/mixin/EnchantmentMenuMixin.java"
TAG = "src/main/resources/data/easymagic/tags/item/enchanting_catalysts.json"


def source(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def javap(class_name: str) -> str:
    assert JAVAP.is_file()
    output = subprocess.run(
        [str(JAVAP), "-p", "-c", "-classpath", str(EASY), class_name],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
        encoding="utf-8", errors="replace", check=True,
    )
    return output.stdout


class EasyMagic263Integration(unittest.TestCase):
    def test_exact_optional_binaries(self):
        self.assertEqual(hashlib.sha256(EASY.read_bytes()).hexdigest(), EASY_SHA)
        self.assertEqual(hashlib.sha256(PUZZLES.read_bytes()).hexdigest(), PUZZLES_SHA)

    def test_upstream_slot_and_recalculation_abi(self):
        menu = javap("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu")
        fuel = javap("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu$3")
        reroll = javap("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu$1")
        self.assertIn("extends net.minecraft.world.inventory.EnchantmentMenu", menu)
        self.assertIn("private void updateLevelsAndClues(net.minecraft.core.RegistryAccess, net.minecraft.world.item.ItemStack, int)", menu)
        self.assertIn("private void resetLevelsAndClues()", menu)
        self.assertIn("public final java.util.List<java.util.List<net.minecraft.world.item.enchantment.EnchantmentInstance>> clues", menu)
        self.assertIn("ENCHANTING_CATALYSTS_ITEM_TAG", fuel)
        self.assertIn("REROLL_CATALYSTS_ITEM_TAG", reroll)
        self.assertIn("EnchantmentMenu.clickMenuButton", menu)

    def test_overlay_keeps_lapis_and_adds_charoite(self):
        with ZipFile(EASY) as jar:
            base = json.loads(jar.read("data/easymagic/tags/item/enchanting_catalysts.json"))
        self.assertIn("minecraft:lapis_lazuli", base["values"])
        overlay = json.loads(source(TAG))
        self.assertIs(overlay.get("replace", False), False)
        self.assertEqual(overlay["values"], ["the_sift:charoite"])

    def test_reused_vanilla_enchanting_hooks_and_bridge(self):
        vanilla = source(VANILLA)
        self.assertIn("implements CharoiteEnchantmentMenuBridge", vanilla)
        self.assertIn("theSift$setCharoiteCostsActive(boolean active)", vanilla)
        self.assertIn("getSlot(0).getItem()", vanilla)
        self.assertIn("getSlot(1).getItem().is(ModItems.CHAROITE)", vanilla)
        self.assertIn("CharoiteEnchanting.markUsed(", vanilla)
        self.assertIn("ModAdvancements.award(", vanilla)
        self.assertIn("theSift$charoiteCosts ? Math.max(1, displayedCost - 20)", vanilla)
        self.assertIn("enchantment.level() + 1", vanilla)
        self.assertIn("void theSift$setCharoiteCostsActive(boolean active);", source(BRIDGE))

    def test_optional_adapter_no_loader_linkage(self):
        adapter = source(ADAPTER)
        self.assertIn("@Pseudo", adapter)
        self.assertIn('targets = "fuzs.easymagic.common.world.inventory.ModEnchantmentMenu"', adapter)
        self.assertNotRegex(adapter, r"(?m)^import\s+fuzs\.")
        self.assertIn('method = "updateLevelsAndClues"', adapter)
        self.assertIn('method = "resetLevelsAndClues"', adapter)
        self.assertIn("require = 0", adapter)
        self.assertIn("menu.costs[slot] += 20", adapter)
        self.assertIn("theSift$setCharoiteCostsActive(true)", adapter)
        self.assertIn("theSift$setCharoiteCostsActive(false)", adapter)
        mixins = json.loads(source("src/main/resources/the_sift.mixins.json"))
        self.assertIn("compat.EasyMagicEnchantmentMenuMixin", mixins["mixins"])
        # Easy Magic and Puzzles Lib must remain optional, not loader-required.
        template = source("src/main/templates/META-INF/neoforge.mods.toml")
        self.assertNotIn('modId="easymagic"', template)
        self.assertNotIn('modId="puzzleslib"', template)


if __name__ == "__main__":
    unittest.main()
