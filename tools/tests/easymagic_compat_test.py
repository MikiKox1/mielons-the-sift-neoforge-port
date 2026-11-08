from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[2]
EASY_MAGIC_JAR = Path(
    os.environ.get(
        "THE_SIFT_EASY_MAGIC_JAR",
        str(ROOT / "run" / "mods" / "easymagic-v26.3.0-mc26.3.x+neoforge.jar"),
    )
).expanduser()
EXPECTED_EASY_MAGIC_SHA256 = "130afa86e341fed660320560ecabc32b9297ad58fdbae06cacece4af22fc6144"


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def find_javap() -> str:
    candidates = [
        shutil.which("javap"),
        str(Path(os.environ.get("JAVA_HOME", "")) / "bin" / "javap.exe") if os.environ.get("JAVA_HOME") else None,
    ]
    for candidate in candidates:
        if candidate and Path(candidate).is_file():
            return candidate
    raise RuntimeError("javap was not found; JDK 25 is required for the Easy Magic bytecode contract test")


def javap(class_name: str) -> str:
    completed = subprocess.run(
        [find_javap(), "-classpath", str(EASY_MAGIC_JAR), "-c", "-p", class_name],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return completed.stdout


class EasyMagicCompatContractTest(unittest.TestCase):
    def test_exact_easy_magic_binary_contract(self) -> None:
        self.assertTrue(EASY_MAGIC_JAR.is_file(), f"missing exact Easy Magic staging copy: {EASY_MAGIC_JAR}")
        digest = hashlib.sha256(EASY_MAGIC_JAR.read_bytes()).hexdigest()
        self.assertEqual(EXPECTED_EASY_MAGIC_SHA256, digest)

        menu = javap("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu")
        catalyst_slot = javap("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu$3")
        reroll_slot = javap("fuzs.easymagic.common.world.inventory.ModEnchantmentMenu$1")

        self.assertIn("Field enchantSlots:Lnet/minecraft/world/Container;", menu)
        self.assertIn("private void updateLevelsAndClues(net.minecraft.core.RegistryAccess, net.minecraft.world.item.ItemStack, int);", menu)
        self.assertRegex(
            menu,
            r"Method net/minecraft/world/inventory/EnchantmentMenu\.clickMenuButton:\(Lnet/minecraft/world/entity/player/Player;I\)Z",
        )
        self.assertIn("ENCHANTING_CATALYSTS_ITEM_TAG", catalyst_slot)
        self.assertIn("REROLL_CATALYSTS_ITEM_TAG", reroll_slot)

        with zipfile.ZipFile(EASY_MAGIC_JAR) as archive:
            access_transformer = archive.read("META-INF/accesstransformer.cfg").decode("utf-8")
        self.assertIn("public-f net.minecraft.world.inventory.EnchantmentMenu enchantSlots", access_transformer)
        self.assertIn("net.minecraft.world.inventory.EnchantmentMenu getEnchantmentList", access_transformer)

    def test_easy_magic_base_and_sift_overlay_tags(self) -> None:
        with zipfile.ZipFile(EASY_MAGIC_JAR) as archive:
            base = json.loads(archive.read("data/easymagic/tags/item/enchanting_catalysts.json"))
        self.assertIn("minecraft:lapis_lazuli", base["values"])

        overlay = json.loads(read("src/main/resources/data/easymagic/tags/item/enchanting_catalysts.json"))
        self.assertFalse(overlay.get("replace", False))
        self.assertIn("the_sift:charoite", overlay["values"])

    def test_vanilla_mixin_uses_active_menu_slots(self) -> None:
        source = read("src/main/java/mielon/thesift/mixin/EnchantmentMenuMixin.java")
        self.assertIn("implements CharoiteEnchantmentMenuBridge", source)
        self.assertIn("getSlot(0).getItem()", source)
        self.assertIn("getSlot(1).getItem().is(ModItems.CHAROITE)", source)
        self.assertIn("theSift$setCharoiteCostsActive", source)

    def test_optional_easy_magic_mixin_has_no_binary_link(self) -> None:
        source = read("src/main/java/mielon/thesift/mixin/compat/EasyMagicEnchantmentMenuMixin.java")
        self.assertIn("@Pseudo", source)
        self.assertIn('targets = "fuzs.easymagic.common.world.inventory.ModEnchantmentMenu"', source)
        self.assertNotRegex(source, re.compile(r"^import\s+fuzs\.easymagic", re.MULTILINE))
        self.assertIn('method = "updateLevelsAndClues"', source)
        self.assertIn('method = "resetLevelsAndClues"', source)
        self.assertIn("require = 0", source)
        self.assertIn("menu.costs[slot] += 20", source)
        self.assertIn("enchantment.level() + 1", source)
        self.assertIn("theSift$setCharoiteCostsActive(false)", source)
        self.assertIn("theSift$setCharoiteCostsActive(true)", source)

        mixins = json.loads(read("src/main/resources/the_sift.mixins.json"))
        self.assertIn("compat.EasyMagicEnchantmentMenuMixin", mixins["mixins"])


if __name__ == "__main__":
    unittest.main()
