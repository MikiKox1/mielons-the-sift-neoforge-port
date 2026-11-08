"""Guard against losing 2.0 startup/cleanup while reusing the proven 1.x loader."""
from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL_SOURCE = ROOT.parents[1] / "reports/research/port-26.3-2.0.1-20261108/decompiled-263/mielon/thesift/TheSiftMod.java"


def source(path: Path) -> str:
    text = path.read_text(encoding="utf-8-sig")
    return re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)


class BootstrapCoverage(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = source(ORIGINAL_SOURCE)
        cls.port = source(ROOT / "src/main/java/mielon/thesift/TheSiftMod.java")

    def test_all_thirteen_original_attribute_factories_are_wired(self):
        entries = re.findall(r"FabricDefaultAttributeRegistry\.register\(ModEntities\.([A-Z_]+),\s*(\w+)\.(\w+)\(\)\)", self.original)
        self.assertEqual(len(entries), 13)
        for entity, implementation, method in entries:
            with self.subTest(entity=entity):
                self.assertRegex(self.port, rf"event\.put\(ModEntities\.{entity},\s*{implementation}\.{method}\(\)\.build\(\)\)")

    def test_all_ten_original_natural_spawn_predicates_are_wired(self):
        entries = re.findall(r"SpawnPlacements\.register\(\s*ModEntities\.([A-Z_]+),.*?(\w+)::(check\w+SpawnRules)\s*\)", self.original, re.S)
        self.assertEqual(len(entries), 10)
        for entity, implementation, predicate in entries:
            with self.subTest(entity=entity):
                self.assertRegex(self.port, rf"event\.register\(\s*ModEntities\.{entity},\s*SpawnPlacementTypes\.ON_GROUND,\s*Types\.MOTION_BLOCKING_NO_LEAVES,\s*{implementation}::{predicate},\s*Operation\.REPLACE\s*\)")

    def test_original_start_tick_flush_and_clear_hooks_are_retained(self):
        original_lifecycle = self.original.split("private static void registerServerLifecycle()", 1)[1]
        calls = set(re.findall(r"\b(\w+)(?:::|\.)(prepare|tick|resumeRequestedClock|flush|clear\w*)\b", original_lifecycle))
        self.assertGreaterEqual(len(calls), 15)
        for owner, method in sorted(calls):
            with self.subTest(hook=f"{owner}.{method}"):
                self.assertRegex(self.port, rf"\b{owner}(?:::|\.){method}\b")

    def test_four_native_network_protocols_are_registered(self):
        for name in ("RiftLoadingPayload", "MapColorsPayload", "SiftIntroReadyPayload", "MonarchFlightPayload"):
            with self.subTest(protocol=name):
                self.assertIn(f"{name}.register(event)", self.port)

    def test_worldgen_is_not_constructed_while_registries_are_frozen(self):
        constructor = self.port.split("public TheSiftMod(IEventBus modBus)", 1)[1].split("private void registerContent", 1)[0]
        self.assertNotIn("ModWorldgen.initialize()", constructor,
                         "SnifferCaveFeature constructs block references; defer until RegisterEvent after block registration")
        self.assertRegex(self.port, r"BuiltInRegistries\.FEATURE_TYPE\.key\(\)\)\)\s*\{\s*ModWorldgen\.initialize\(\)")

    def test_mc263_explorer_map_items_wait_for_item_registry_event(self):
        # Actual first MC26.3 boot failed in MapItem.<init> before registry unfreeze.
        maps = (ROOT / "src/main/java/mielon/thesift/map/SiftExplorationMaps.java").read_text(encoding="utf8")
        startup = maps.split("public static void initialize()", 1)[1].split("public static void initializeItems()", 1)[0]
        self.assertNotIn("new MapItem", startup)
        self.assertIn("public static void initializeItems()", maps)
        constructor = self.port.split("public TheSiftMod(IEventBus modBus)", 1)[1].split("private void registerContent", 1)[0]
        self.assertNotIn("SiftExplorationMaps.initializeItems()", constructor)
        item_branch = self.port.split("BuiltInRegistries.ITEM.key()", 1)[1].split("else if", 1)[0]
        self.assertIn("SiftExplorationMaps.initializeItems()", item_branch)

    def test_mc263_fuel_copy_runs_after_components_are_bound(self):
        fuels = (ROOT / "src/main/java/mielon/thesift/item/ModFuels.java").read_text(encoding="utf8")
        self.assertIn("onDefaultDataComponentsBound(DefaultDataComponentsBoundEvent event)", fuels)
        self.assertIn("event.shouldUpdateStaticData()", fuels)
        self.assertIn("entry.getValue().components().get(DataComponents.COOKING_FUEL)", fuels)
        self.assertRegex(fuels, r"if \(fuel == null\)\s*\{\s*continue;")
        self.assertIn("addAll(components).set(DataComponents.COOKING_FUEL, fuel)", fuels)
        self.assertIn("item.builtInRegistryHolder().bindComponents(updated.build())", fuels)
        self.assertIn("item.resetDefaultResource()", fuels)
        self.assertIn("NeoForge.EVENT_BUS.addListener(ModFuels::onDefaultDataComponentsBound)", self.port)
        self.assertNotIn("ModFuels.modifyDefaultComponents(event)", self.port)

    def test_after_death_adapter_covers_vanilla_player_override(self):
        config = json.loads((ROOT / "src/main/resources/the_sift.mixins.json").read_text(encoding="utf-8"))
        self.assertIn("SiftAfterDeathMixin", config["mixins"])
        self.assertIn("SiftAfterPlayerDeathMixin", config["mixins"])


if __name__ == "__main__":
    unittest.main()
