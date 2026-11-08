"""Target data-layout and shader regressions independent of gameplay smoke tests."""
from collections import Counter
import gzip
import io
import json
from pathlib import Path
import subprocess
import sys
import unittest
from zipfile import ZipFile

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
RESOURCES = ROOT / "src/main/resources"


class RenderPearlAndTargetData(unittest.TestCase):
    def test_original_shader_set_has_target_syntax_and_exact_bytes(self):
        with ZipFile(ORIGINAL) as jar:
            names = [n for n in jar.namelist() if n.startswith("assets/the_sift/shaders/core/")
                     and n.endswith((".vsh", ".fsh"))]
            self.assertEqual(len(names), 11)
            for name in names:
                with self.subTest(shader=name):
                    data = (RESOURCES / name).read_bytes()
                    self.assertEqual(data, jar.read(name))
                    text = data.decode("utf8")
                    self.assertNotIn("#moj_import", text)
                    self.assertIn("#extension GL_ARB_separate_shader_objects : require", text)
                    self.assertIn("layout(location", text.replace("layout (", "layout("))

    def test_target_feature_registry_paths_not_old_configured_paths(self):
        feature = RESOURCES / "data/the_sift/worldgen/feature"
        self.assertEqual(len(list(feature.glob("*.json"))), 67)
        self.assertEqual(len(list((RESOURCES / "data/the_sift/worldgen/carver").glob("*.json"))), 3)
        self.assertEqual(list((RESOURCES / "data/the_sift/worldgen/configured_feature").glob("*.json")), [])
        self.assertEqual(list((RESOURCES / "data/the_sift/worldgen/configured_carver").glob("*.json")), [])
        for path in (RESOURCES / "data/the_sift/worldgen/placed_feature").glob("*.json"):
            data = json.loads(path.read_text(encoding="utf8"))
            value = data.get("feature")
            if isinstance(value, str) and value.startswith("the_sift:"):
                self.assertTrue((feature / (value.split(":", 1)[1] + ".json")).is_file(), str(path))

    def test_all_299_templates_preserve_both_native_and_dfu_legacy_formats(self):
        # Old templates are legitimate inputs to TemplateSource's DFU path.
        # Never bulk-normalize them or infer AIR absence solely from raw Name/id.
        with ZipFile(ORIGINAL) as jar:
            names = [n for n in jar.namelist() if n.startswith("data/")
                     and "/structure/" in n and n.endswith(".nbt")]
            self.assertEqual(len(names), 299)
            versions = Counter()
            for name in names:
                payload = (RESOURCES / name).read_bytes()
                self.assertEqual(payload, jar.read(name), name)
                raw = gzip.decompress(payload) if payload.startswith(b"\x1f\x8b") else payload
                root = nbtlib.File.parse(io.BytesIO(raw))
                version = int(root["DataVersion"])
                versions[version] += 1
                for palette in root.get("palettes", [root.get("palette", [])]):
                    for entry in palette:
                        self.assertIn("id" if version == 5023 else "Name", entry, name)
            self.assertEqual(versions, {4903: 137, 5023: 162})

    def test_historical_mc262_repair_refuses_target263_before_writing(self):
        result = subprocess.run(
            [sys.executable, "-B", str(ROOT / "tools/repair_sculkers_lair_nbt.py"), "--apply"],
            capture_output=True, text=True, encoding="utf8", check=False,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Historical repair applies only to MC26.2", result.stderr)


if __name__ == "__main__":
    unittest.main()
