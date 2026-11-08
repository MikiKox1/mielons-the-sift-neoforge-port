"""Static coverage gates for the exact 26.2 upstream, not gameplay assertions.

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
ORIGINAL = THE_SIFT / "reference/upstream-2.0.0-20261107/mielon's-the-sift-2.0.0-mc26.2-fabric.jar"
ORIGINAL_SHA256 = "ac6809cc012e52f727ca6d6342c27528d57e4d59c3527e3d164e1b43ca42e240"
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

    def test_original_is_the_pinned_immutable_26_2_release(self):
        with ORIGINAL.open("rb") as stream:
            self.assertEqual(hashlib.file_digest(stream, "sha256").hexdigest(), ORIGINAL_SHA256)

    def test_all_upstream_top_level_game_classes_have_source(self):
        expected = sorted(
            name[:-6] + ".java" for name in self.original_names
            if name.startswith("mielon/thesift/") and name.endswith(".class") and "$" not in name
        )
        missing = [name for name in expected if not any((base / name).is_file() for base in JAVA_ROOTS)]
        self.assertEqual(missing, [], f"Missing original 2.0 classes ({len(missing)}): {missing[:30]}")

    def test_original_26_2_resources_with_only_verified_2_0_1_overrides(self):
        delta = json.loads((ROOT / "UPSTREAM-2.0.1-DELTA.json").read_text(encoding="utf-8"))
        overrides = delta["resource_overrides"]
        self.assertEqual(set(overrides), {
            "assets/the_sift/lang/en_us.json",
            "data/the_sift/structure/ichor_dome/organic_desert_2.nbt",
        })
        lair = json.loads((ROOT / "SCULKERS-LAIR-MC26.2-PALETTE-FIX.json").read_text(encoding="utf-8"))
        lair_overrides = lair["resource_sha256"]
        target = json.loads((ROOT / "TARGET-2612-PROVENANCE.json").read_text(encoding="utf8"))
        target_overrides = target["target_resource_changes_from_accepted_base"]
        removals = target.get("target_resource_removals", {})
        predicate_delta = json.loads((ROOT / "PREDICATE-2612-ADAPTATIONS.json").read_text(encoding="utf8"))
        self.assertEqual(len(predicate_delta["resources"]), 11)
        self.assertTrue({"assets/the_sift/shaders/core/restore_tide_sky.vsh", *predicate_delta["resources"]} <= set(target_overrides))
        self.assertEqual(len(lair_overrides), 79)
        self.assertTrue(all(path.startswith("data/the_sift/structure/sculkers_lair/parts/")
                            and path.endswith(".nbt") for path in lair_overrides))
        failures = []
        for name in sorted(self.original_names):
            if name.endswith("/") or not name.startswith(("assets/", "data/")):
                continue
            candidates = [base / name for base in RESOURCE_ROOTS if (base / name).is_file()]
            if name in removals:
                self.assertEqual(candidates, [], name)
                self.assertTrue(removals[name].get("reason"), name)
                continue
            if len(candidates) != 1:
                failures.append((name, f"expected one source resource, found {len(candidates)}"))
            elif name in target_overrides:
                # Before hashes are independently checked against the accepted
                # 2.0.1 base in the final artifact audit, not older 2.0.0 bytes.
                self.assertEqual(hashlib.sha256(candidates[0].read_bytes()).hexdigest(), target_overrides[name]["after_sha256"])
                self.assertTrue(target_overrides[name].get("reason"), name)
            elif name in overrides:
                if hashlib.sha256(candidates[0].read_bytes()).hexdigest() != overrides[name]:
                    failures.append((name, "different bytes from the verified 2.0.1 target-version adaptation"))
            elif name in lair_overrides:
                if hashlib.sha256(candidates[0].read_bytes()).hexdigest() != lair_overrides[name]:
                    failures.append((name, "different bytes from verified community lair palette repair"))
            elif candidates[0].read_bytes() != self.original.read(name):
                current = candidates[0].read_bytes()
                reference = self.original.read(name)
                if not (name.endswith((".json", ".mcmeta", ".txt"))
                        and current.replace(b"\r\n", b"\n") == reference.replace(b"\r\n", b"\n")):
                    failures.append((name, "difference beyond normal checkout line endings"))
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
        self.assertEqual(props["minecraft_version"], "26.1.2")
        self.assertEqual(props["minecraft_version_range"], "[26.1.2]")
        self.assertEqual(props["neo_min_version"], "26.1.2.100")
        self.assertEqual(props["mod_version"], "2.0.1-neoforge.1-mc26.1.2")


if __name__ == "__main__":
    unittest.main()
