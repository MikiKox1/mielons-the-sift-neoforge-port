"""Check exact conversion of reviewed original data, not permissive JSON parsing."""
import importlib.util
import json
from pathlib import Path
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("predicate_adapter", ROOT / "tools/adapt_predicates_2612.py")
adapter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(adapter)


class Predicate2612Compatibility(unittest.TestCase):
    def test_all_51_affected_conditions_retain_values_in_target_codec(self):
        total = 0
        with ZipFile(adapter.ORIGINAL) as original:
            for name in adapter.PATHS:
                raw = json.loads(original.read(name))
                expected, count = adapter.convert(raw)
                current = json.loads((ROOT / "src/main/resources" / name).read_text(encoding="utf8"))
                self.assertEqual(current, expected, name)
                total += count
                if "/advancement/" in name:
                    self.assertEqual(current["requirements"], raw["requirements"])
                    self.assertEqual(current.get("rewards"), raw.get("rewards"))
                    self.assertEqual(set(current["criteria"]), set(raw["criteria"]))
        self.assertEqual(total, 51)

    def test_nested_item_component_ids_are_not_renamed(self):
        sample = {"condition": "minecraft:entity_properties", "predicate": {
            "minecraft:equipment": {"mainhand": {"predicates": {"minecraft:enchantments": [
                {"enchantments": "#minecraft:smelts_loot"}]}}},
            "minecraft:components": {"minecraft:sheep/color": "red"},
            "minecraft:type_specific/sheep": {"sheared": False},
        }}
        actual, count = adapter.convert(sample)
        self.assertEqual(count, 1)
        self.assertEqual(actual["predicate"]["components"], {"minecraft:sheep/color": "red"})
        self.assertEqual(actual["predicate"]["equipment"]["mainhand"], sample["predicate"]["minecraft:equipment"]["mainhand"])
        self.assertEqual(actual["predicate"]["type_specific"], {"type": "minecraft:sheep", "sheared": False})
        self.assertIn("minecraft:type_specific/sheep", sample["predicate"])

    def test_unknown_fields_cannot_be_silently_dropped(self):
        with self.assertRaisesRegex(AssertionError, "Unreviewed"):
            adapter.convert({"condition": "minecraft:entity_properties", "predicate": {"minecraft:unknown": True}})


if __name__ == "__main__":
    unittest.main()
