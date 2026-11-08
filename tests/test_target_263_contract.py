"""Exact-target port contracts; runtime acceptance is a separate gate."""
import hashlib
import json
from pathlib import Path
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SIFT = ROOT.parents[1]
UPSTREAM = SIFT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
PIN = "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed"


class Target263Contract(unittest.TestCase):
    def test_reference_identity(self):
        with UPSTREAM.open("rb") as f:
            self.assertEqual(hashlib.file_digest(f, "sha256").hexdigest(), PIN)

    def test_target_metadata_and_independent_minimum(self):
        props = dict(line.split("=", 1) for line in
                     (ROOT / "gradle.properties").read_text(encoding="utf8").splitlines()
                     if "=" in line and not line.startswith("#"))
        self.assertEqual(props["minecraft_version"], "26.3")
        self.assertEqual(props["minecraft_version_range"], "[26.3]")
        self.assertEqual(props["neo_min_version"], "26.3.0.64-beta")
        self.assertEqual(props["geckolib_version"], "5.5.7")
        self.assertEqual(props["mod_version"], "2.0.1-neoforge.1-mc26.3-alpha.4")
        meta = (ROOT / "src/main/templates/META-INF/neoforge.mods.toml").read_text(encoding="utf8")
        self.assertIn('versionRange="[${neo_min_version},)"', meta)
        self.assertIn('loaderVersion="[12,)"', meta)

    def test_all_original_target_resources_are_present_and_byte_exact(self):
        dirs = [ROOT / name for name in ("src/main/resources", "src/client/resources", "src/generated/resources")]
        with ZipFile(UPSTREAM) as jar:
            names = sorted(n for n in jar.namelist()
                           if n.startswith(("assets/", "data/")) and not n.endswith("/"))
            self.assertEqual(len(names), 2573)
            problems = []
            for name in names:
                found = [base / name for base in dirs if (base / name).is_file()]
                if len(found) != 1:
                    problems.append((name, "missing or ambiguous"))
                elif found[0].read_bytes() != jar.read(name):
                    problems.append((name, "does not match exact MC26.3 original"))
            self.assertEqual(problems, [], str(problems[:12]))

    def test_no_experimental_geometry_cache_import(self):
        text = (ROOT / "src/main/java/mielon/thesift/worldgen/SiftTerrainFeatureMemory.java").read_text(encoding="utf8")
        self.assertIn("LinkedHashMap<Long,", text)
        self.assertNotIn("Long2ObjectLinkedOpenHashMap", text)

    def test_community_route_and_optional_catalyst_are_retained(self):
        arrival = (ROOT / "src/main/java/mielon/thesift/world/SafePortalArrival.java").read_text(encoding="utf8")
        storage = (ROOT / "src/main/java/mielon/thesift/world/SiftWorldStorage.java").read_text(encoding="utf8")
        self.assertIn("findNearRecordedPortal", arrival)
        self.assertIn("main_portal_entrances", storage)
        tag = json.loads((ROOT / "src/main/resources/data/easymagic/tags/item/enchanting_catalysts.json").read_text(encoding="utf8"))
        self.assertFalse(tag.get("replace", False))
        self.assertIn("the_sift:charoite", tag["values"])


if __name__ == "__main__":
    unittest.main()
