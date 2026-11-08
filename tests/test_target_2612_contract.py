"""MC26.1.2 target pins and accepted mechanism preservation, not gameplay proof."""
from pathlib import Path
import hashlib
import json
import unittest

ROOT = Path(__file__).resolve().parents[1]
SIFT = ROOT.parents[1]
REF = SIFT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc1.21.1-neo.jar"
PIN = "0da27285ae442fc6bb27e6f4c703800c2ce90ea93ef6bc64c40da6d135ef23fb"


class Target2612Contract(unittest.TestCase):
    def test_original_neoforge_1211_is_pinned_reference(self):
        with REF.open("rb") as f:
            self.assertEqual(hashlib.file_digest(f, "sha256").hexdigest(), PIN)

    def test_target_metadata_preserves_independent_minimum(self):
        props = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text(encoding="utf8").splitlines()
                     if "=" in line and not line.startswith("#"))
        self.assertEqual(props["minecraft_version"], "26.1.2")
        self.assertEqual(props["minecraft_version_range"], "[26.1.2]")
        self.assertEqual(props["neo_version"], "26.1.2.126")
        self.assertEqual(props["neo_min_version"], "26.1.2.100")
        self.assertEqual(props["geckolib_version"], "5.5.2")
        self.assertEqual(props["mod_version"], "2.0.1-neoforge.1-mc26.1.2")
        meta = (ROOT / "src/main/templates/META-INF/neoforge.mods.toml").read_text(encoding="utf8")
        self.assertIn('loaderVersion="[11,)"', meta)
        self.assertIn('versionRange="[${neo_min_version},)"', meta)

    def test_accepted_portal_route_fixes_are_present(self):
        folder = ROOT / "src/main/java/mielon/thesift/world"
        self.assertIn("findNearRecordedPortal", (folder / "SafePortalArrival.java").read_text(encoding="utf8"))
        self.assertIn("main_portal_entrances", (folder / "SiftWorldStorage.java").read_text(encoding="utf8"))
        self.assertIn("linkedRiftExists", (folder / "RiftManager.java").read_text(encoding="utf8"))

    def test_experimental_worldgen_cache_is_not_imported(self):
        text = (ROOT / "src/main/java/mielon/thesift/worldgen/SiftTerrainFeatureMemory.java").read_text(encoding="utf8")
        self.assertIn("LinkedHashMap<Long,", text)
        self.assertNotIn("Long2ObjectLinkedOpenHashMap", text)

    def test_optional_charoite_overlay_does_not_replace_vanilla(self):
        value = json.loads((ROOT / "src/main/resources/data/easymagic/tags/item/enchanting_catalysts.json").read_text(encoding="utf8"))
        self.assertFalse(value.get("replace", False))
        self.assertEqual(value["values"], ["the_sift:charoite"])


if __name__ == "__main__":
    unittest.main()
