"""Check platform data maps against the actual original Fabric registrations."""
from pathlib import Path
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL_SOURCE = ROOT.parents[1] / "reports/research/upstream-2.0.0-20261107/decompiled/mc26.2-fabric-vineflower-port/mielon/thesift"
DATA = ROOT / "src/main/resources/data/neoforge/data_maps"


class NativeDataMapContracts(unittest.TestCase):
    def test_compostables_match_all_original_chances(self):
        expected = {}
        for relative in ("block/ModBlocks.java", "item/ModItems.java"):
            source = (ORIGINAL_SOURCE / relative).read_text(encoding="utf-8")
            entries = re.findall(r"CompostableRegistry\.INSTANCE\.add\((?:ModBlocks\.)?([A-Z_]+), ([0-9.]+)F\)", source)
            for field, chance in entries:
                name = "the_sift:" + field.removesuffix("_ITEM").lower()
                self.assertNotIn(name, expected)
                expected[name] = {"chance": float(chance)}
        self.assertEqual(len(expected), 21, "Revisit exact upstream registration coverage")
        path = DATA / "item/compostables.json"
        self.assertTrue(path.is_file(), "Native composting data map must replace Fabric callbacks")
        actual = json.loads(path.read_text(encoding="utf-8"))
        self.assertFalse(actual.get("replace", False), "Do not replace other mods/vanilla compostables")
        self.assertEqual(actual["values"], expected)

    def test_stripping_matches_all_four_original_pairs(self):
        source = (ORIGINAL_SOURCE / "block/ModBlocks.java").read_text(encoding="utf-8")
        entries = re.findall(r"StrippableBlockRegistry\.register\(([A-Z_]+), ([A-Z_]+)\)", source)
        self.assertEqual(len(entries), 4)
        expected = {"the_sift:" + first.lower(): {"stripped_block": "the_sift:" + second.lower()}
                    for first, second in entries}
        path = DATA / "block/strippables.json"
        self.assertTrue(path.is_file(), "NeoForge26.2 uses STRIPPABLES data map, not AxeItem's old static map")
        actual = json.loads(path.read_text(encoding="utf-8"))
        self.assertFalse(actual.get("replace", False), "Do not remove vanilla stripping entries")
        self.assertEqual(actual["values"], expected)


if __name__ == "__main__":
    unittest.main()
