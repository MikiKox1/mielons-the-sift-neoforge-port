"""Check MC26.3 item components and block transformers against exact upstream."""
from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL_SOURCE = ROOT.parents[1] / "reports/research/port-26.3-2.0.1-20261108/decompiled-263/mielon/thesift"
DATA = ROOT / "src/main/resources/data/neoforge/data_maps"


class NativeDataMapContracts(unittest.TestCase):
    def test_compostable_components_match_all_22_original_chances(self):
        def values(root):
            result = {}
            for relative in ("block/ModBlocks.java", "item/ModItems.java"):
                text = (root / relative).read_text(encoding="utf-8")
                pattern = (r"\b(?:context|event)\.modify\((?:ModBlocks\.)?([A-Z_]+),[^;]*?"
                           r"DataComponents\.COMPOSTABLE,\s*(?:compostable\((\d+)\)|"
                           r"new Compostable\(new Constant\((\d+)\)\))")
                for field, helper_value, direct_value in re.findall(pattern, text):
                    self.assertNotIn(field, result)
                    result[field] = int(helper_value or direct_value)
            return result

        expected = values(ORIGINAL_SOURCE)
        self.assertEqual(len(expected), 22, "Exact MC26.3 source has 22 compostable items")
        self.assertEqual(expected["ASH_WILLOW_SAPLING_ITEM"], 30)
        actual = values(ROOT / "src/main/java/mielon/thesift")
        self.assertEqual(actual, expected)
        self.assertFalse((DATA / "item/compostables.json").exists(),
                         "Obsolete MC26.2 compostables data map must not shadow native item components")

    def test_stripping_matches_all_four_original_pairs(self):
        source = (ORIGINAL_SOURCE / "block/ModBlocks.java").read_text(encoding="utf-8")
        entries = re.findall(r"BlockTransformerHelper\.registerStripping\(([A-Z_]+), ([A-Z_]+)\)", source)
        self.assertEqual(len(entries), 4)
        expected = {"the_sift:" + first.lower(): "the_sift:" + second.lower() for first, second in entries}
        path = DATA / "block/transformables.json"
        self.assertTrue(path.is_file(), "NeoForge26.3 uses TRANSFORMABLES with the native axe transformer")
        actual = json.loads(path.read_text(encoding="utf-8"))
        self.assertFalse(actual.get("replace", False), "Do not remove vanilla stripping entries")
        self.assertEqual(set(actual["values"]), set(expected))
        for source_id, target in expected.items():
            transform = actual["values"][source_id]
            self.assertEqual(transform["transformer"], "minecraft:axe")
            entry = transform["transform_data"]
            self.assertEqual(entry["item_damage_per_use"], 1)
            self.assertEqual(entry["sound"], "minecraft:item.axe.strip")
            provider = entry["block_state_provider"]
            self.assertEqual(provider["type"], "minecraft:rule_based")
            self.assertEqual(len(provider["rules"]), 1)
            rule = provider["rules"][0]
            self.assertEqual(rule["if_true"], {"type": "minecraft:matching_blocks", "blocks": source_id})
            self.assertEqual(rule["then"]["type"], "minecraft:copy_properties")
            self.assertEqual(rule["then"]["source"], {"id": target, "properties": {"axis": "y"}})
        self.assertFalse((DATA / "block/strippables.json").exists())


if __name__ == "__main__":
    unittest.main()
