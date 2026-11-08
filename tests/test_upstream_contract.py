"""Static coverage gates for the exact MC26.3 2.0.1 upstream, not gameplay assertions.

Run with the workshop Python: python -m unittest discover -s tests -v
The accepted old port must fail the 2.0 presence/data gates before migration.
"""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import json
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
THE_SIFT = ROOT.parents[1]
ORIGINAL = THE_SIFT / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
ORIGINAL_SHA256 = "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed"
JAVA_ROOTS = (ROOT / "src/main/java", ROOT / "src/client/java")
RESOURCE_ROOTS = (ROOT / "src/main/resources", ROOT / "src/client/resources")


class UpstreamContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = ZipFile(ORIGINAL)
        cls.original_names = set(cls.original.namelist())

    @classmethod
    def tearDownClass(cls):
        cls.original.close()

    def test_original_is_the_pinned_immutable_26_3_release(self):
        with ORIGINAL.open("rb") as stream:
            self.assertEqual(hashlib.file_digest(stream, "sha256").hexdigest(), ORIGINAL_SHA256)

    def test_all_upstream_top_level_game_classes_have_source(self):
        expected = sorted(
            name[:-6] + ".java" for name in self.original_names
            if name.startswith("mielon/thesift/") and name.endswith(".class") and "$" not in name
        )
        missing = [name for name in expected if not any((base / name).is_file() for base in JAVA_ROOTS)]
        self.assertEqual(missing, [], f"Missing original 2.0 classes ({len(missing)}): {missing[:30]}")

    def test_original_26_3_resources_are_target_version_exact(self):
        manifest = json.loads((ROOT / "UPSTREAM-26.3-RESOURCE-MANIFEST.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["source_jar_sha256"], ORIGINAL_SHA256)
        self.assertEqual(manifest["source_resource_count"], 2573)
        failures = []
        for name in sorted(self.original_names):
            if name.endswith("/") or not name.startswith(("assets/", "data/")):
                continue
            candidates = [base / name for base in RESOURCE_ROOTS if (base / name).is_file()]
            if len(candidates) != 1:
                failures.append((name, f"expected one source resource, found {len(candidates)}"))
            elif candidates[0].read_bytes() != self.original.read(name):
                failures.append((name, "different bytes from exact MC26.3 release"))
        self.assertEqual(failures, [], f"Missing/changed/duplicate resources ({len(failures)}): {failures[:25]}")

    def test_no_fabric_loader_imports_in_port_java(self):
        failures = []
        for base in JAVA_ROOTS:
            for path in base.rglob("*.java"):
                if re.search(r"(?m)^import net\.fabricmc\.", path.read_text(encoding="utf-8-sig")):
                    failures.append(str(path.relative_to(ROOT)))
        self.assertEqual(failures, [], f"Unported Fabric API imports ({len(failures)}): {failures[:30]}")

    def test_no_decompiler_failure_bodies(self):
        markers = ("Decompilation failed", "Couldn't be decompiled", "This method has failed to decompile", "$VF: Couldn't be decompiled")
        failures = []
        for base in JAVA_ROOTS:
            for path in base.rglob("*.java"):
                text = path.read_text(encoding="utf-8-sig")
                if any(marker in text for marker in markers):
                    failures.append(str(path.relative_to(ROOT)))
        self.assertEqual(failures, [], f"Synthetic decompiler failures: {failures}")

    def test_2_0_entities_are_registered(self):
        source = (JAVA_ROOTS[0] / "mielon/thesift/entity/ModEntities.java").read_text(encoding="utf-8-sig")
        expected = ("seedling", "nester", "pollinator", "stalker", "scavenger", "slabber", "nuzzle", "monarch", "monarch_echo")
        for entity_id in expected:
            with self.subTest(entity_id=entity_id):
                self.assertIn('"' + entity_id + '"', source)

    def test_exact_minecraft_and_preserved_neoforge_minimum(self):
        text = (ROOT / "gradle.properties").read_text(encoding="utf-8-sig")
        props = dict(line.split("=", 1) for line in text.splitlines() if "=" in line and not line.lstrip().startswith("#"))
        self.assertEqual(props["minecraft_version"], "26.3")
        self.assertEqual(props["minecraft_version_range"], "[26.3]")
        self.assertEqual(props["neo_min_version"], "26.3.0.64-beta")
        self.assertEqual(props["mod_version"], "2.0.1-neoforge.1-mc26.3")


if __name__ == "__main__":
    unittest.main()
