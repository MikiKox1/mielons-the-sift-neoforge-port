"""Check the published The Sift JAR's declared NeoForge minimum.

Gradle's compile-time NeoForge version is intentionally independent of the
NeoForge loader version range advertised in the produced mod descriptor.
"""

from __future__ import annotations

import os
from pathlib import Path
import tomllib
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[2]
MINIMUM_NEOFORGE = "26.3.0.64-beta"
MINECRAFT_VERSION = "26.3"


def production_jar() -> Path:
    supplied = os.environ.get("THE_SIFT_JAR")
    if supplied:
        return Path(supplied)

    candidates = sorted(
        jar
        for jar in (ROOT / "build" / "libs").glob("the_sift-*.jar")
        if not jar.name.endswith("-sources.jar")
    )
    if not candidates:
        raise AssertionError("No compiled The Sift production JAR in build/libs")
    return candidates[-1]


class LoaderRangeContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        jar_path = production_jar()
        assert jar_path.is_file(), f"Missing JAR: {jar_path}"
        with zipfile.ZipFile(jar_path) as archive:
            cls.metadata = tomllib.loads(
                archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
            )

    def dependencies(self) -> dict[str, dict[str, str]]:
        return {
            dep["modId"]: dep
            for dep in self.metadata["dependencies"]["the_sift"]
        }

    def test_neoforge_has_inclusive_minimum_without_upper_bound(self) -> None:
        neo = self.dependencies()["neoforge"]
        self.assertEqual(neo["type"], "required")
        self.assertEqual(neo["side"], "BOTH")
        self.assertEqual(neo["versionRange"], f"[{MINIMUM_NEOFORGE},)")

    def test_minecraft_version_remains_pinned(self) -> None:
        minecraft = self.dependencies()["minecraft"]
        self.assertEqual(minecraft["versionRange"], f"[{MINECRAFT_VERSION}]")
        self.assertEqual(minecraft["type"], "required")

    def test_optional_integrations_remain_optional(self) -> None:
        dependencies = self.dependencies()
        self.assertEqual(dependencies["lambdynlights"]["type"], "optional")
        self.assertNotIn("easymagic", dependencies)


if __name__ == "__main__":
    unittest.main()
