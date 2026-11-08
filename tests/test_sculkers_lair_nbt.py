"""Fail-first MC26.2 Sculkers' Lair template palette compatibility checks."""
from pathlib import Path
from zipfile import ZipFile
import gzip
import hashlib
import io
import unittest

import nbtlib


ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
ORIGINAL_SHA = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
PREFIX = "data/the_sift/structure/sculkers_lair/parts/"


def parse(data):
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(data) if data.startswith(b"\x1f\x8b") else data))


def normalized(template):
    template["DataVersion"] = nbtlib.Int(4903)
    for entry in template["palette"]:
        if "id" in entry:
            entry["Name"] = entry.pop("id")
        if "properties" in entry:
            entry["Properties"] = entry.pop("properties")
    return template


class LairPaletteCompatibility(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.archive = ZipFile(ORIGINAL)
        with ORIGINAL.open("rb") as stream:
            assert hashlib.file_digest(stream, "sha256").hexdigest() == ORIGINAL_SHA
        cls.names = sorted(name for name in cls.archive.namelist()
                           if name.startswith(PREFIX) and name.endswith(".nbt"))

    @classmethod
    def tearDownClass(cls):
        cls.archive.close()

    def test_exact_upstream_defect_is_reproducible(self):
        self.assertEqual(len(self.names), 79)
        modern = sum("id" in parse(self.archive.read(name))["palette"][0] for name in self.names)
        traditional = sum("Name" in parse(self.archive.read(name))["palette"][0] for name in self.names)
        self.assertEqual((modern, traditional), (77, 2))

    def test_every_template_uses_minecraft_26_2_name_properties_and_version(self):
        for name in self.names:
            with self.subTest(template=name):
                template = parse((ROOT / "src/main/resources" / name).read_bytes())
                self.assertEqual(int(template["DataVersion"]), 4903)
                self.assertGreater(len(template["blocks"]), 0)
                for block in template["palette"]:
                    self.assertIn("Name", block)
                    self.assertNotIn("id", block)
                    self.assertNotIn("properties", block)

    def test_only_expected_nbt_format_changes_compared_to_official_upstream(self):
        for name in self.names:
            with self.subTest(template=name):
                expected = normalized(parse(self.archive.read(name)))
                actual = parse((ROOT / "src/main/resources" / name).read_bytes())
                self.assertEqual(actual, expected)


if __name__ == "__main__":
    unittest.main()
