"""MC26.3 native-format Lair and legacy-template migration contracts."""
from pathlib import Path
from zipfile import ZipFile
import gzip
import hashlib
import io
import unittest

import nbtlib


ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
ORIGINAL_SHA = "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed"
PREFIX = "data/the_sift/structure/sculkers_lair/parts/"


def parse(data):
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(data) if data.startswith(b"\x1f\x8b") else data))


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

    def test_exact_target_lair_uses_new_palette_schema(self):
        self.assertEqual(len(self.names), 79)
        modern = sum("id" in parse(self.archive.read(name))["palette"][0] for name in self.names)
        traditional = sum("Name" in parse(self.archive.read(name))["palette"][0] for name in self.names)
        self.assertEqual((modern, traditional), (79, 0))

    def test_every_lair_template_uses_minecraft_26_3_id_properties(self):
        for name in self.names:
            with self.subTest(template=name):
                template = parse((ROOT / "src/main/resources" / name).read_bytes())
                self.assertEqual(int(template["DataVersion"]), 5023)
                self.assertGreater(len(template["blocks"]), 0)
                for block in template["palette"]:
                    self.assertIn("id", block)
                    self.assertNotIn("Name", block)
                    self.assertNotIn("Properties", block)

    def test_no_target_lair_template_has_26_2_normalization(self):
        for name in self.names:
            with self.subTest(template=name):
                expected = parse(self.archive.read(name))
                actual = parse((ROOT / "src/main/resources" / name).read_bytes())
                self.assertEqual(actual, expected)
                self.assertEqual((ROOT / "src/main/resources" / name).read_bytes(), self.archive.read(name))


if __name__ == "__main__":
    unittest.main()
