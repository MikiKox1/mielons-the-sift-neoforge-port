#!/usr/bin/env python3
"""Release 1.1.2 versus user-accepted stage, fail-closed package parity.

No game launches, external files, third-party dependencies, or ModTest reads.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import tomllib
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
STAGE = SIFT / "artifacts/builds/TheSift-1.1.2-MC26.3-NeoForge-26.3.0.76-STAGEONLY.jar"
RELEASE = ROOT / "build/libs/the_sift-1.1.2-neoforge.1.jar"
SOURCES = ROOT / "build/libs/the_sift-1.1.2-neoforge.1-sources.jar"
EXPECTED_DELTAS = {"META-INF/neoforge.mods.toml", "the_sift.mixins.json"}


def check(condition: bool, label: str) -> None:
    if not condition:
        raise AssertionError(label)
    print("PASS " + label)


def main() -> None:
    check(all(p.is_file() for p in (STAGE, RELEASE, SOURCES)),
          "accepted stage and release binary/source JARs exist")
    with ZipFile(STAGE) as accepted, ZipFile(RELEASE) as official, ZipFile(SOURCES) as sources:
        check(all(a.testzip() is None for a in (accepted, official, sources)),
              "binary and source JAR CRC checks")
        old_names, new_names = set(accepted.namelist()), set(official.namelist())
        check(old_names == new_names, "release preserves every user-tested ZIP entry")
        differences = {name for name in new_names if accepted.read(name) != official.read(name)}
        check(differences == EXPECTED_DELTAS,
              "only release metadata and whitespace of mixin configuration differ")

        old_mixins = json.loads(accepted.read("the_sift.mixins.json"))
        new_mixins = json.loads(official.read("the_sift.mixins.json"))
        check(old_mixins == new_mixins, "mixin configuration equivalent, including required injection order")

        old_meta = tomllib.loads(accepted.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        new_meta = tomllib.loads(official.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        check(old_meta["mods"][0]["version"] == "1.1.2-neoforge.1-stage.1",
              "baseline is exact user-tested stage")
        check(new_meta["mods"][0]["version"] == "1.1.2-neoforge.1",
              "release mod version has NO staging suffix")
        check(new_meta["mods"][0]["modId"] == "the_sift",
              "original mod identifier preserved")
        check(new_meta.get("license") == "MIT", "MIT license correctly declared in metadata")
        deps = {d["modId"]: d for d in new_meta["dependencies"]["the_sift"]}
        check(deps["neoforge"]["versionRange"] == "[26.3.0.64-beta,)",
              "NeoForge minimum 26.3.0.64-beta with NO upper version restriction")
        check(deps["minecraft"]["versionRange"] == "[26.3]",
              "release is scoped to Minecraft 26.3")
        check(deps["geckolib"]["versionRange"] == "[5.5.7,6)",
              "GeckoLib 5.5.7+ (5.x) remains required")
        check(deps["lambdynlights"]["type"] == "optional"
              and "easymagic" not in deps,
              "optional integrations are not mandatory dependencies")
        check(new_meta["mods"][0]["displayName"] == "Mielon's The Sift",
              "original mod name preserved")

        license_entry = official.read("LICENSE_the_sift").decode("utf-8")
        project_license = (ROOT / "LICENSE").read_text(encoding="utf-8")
        check(license_entry.replace("\r\n", "\n").strip()
              == project_license.replace("\r\n", "\n").strip(),
              "MIT original copyright and notice embedded in JAR")
        check("assets/the_sift/textures/textures.zip" not in new_names,
              "redundant upstream nested textures ZIP excluded")
        check("fabric.mod.json" not in new_names,
              "native NeoForge release has no Fabric metadata")
        check(len([x for x in new_names if x.endswith(".class")]) > 150,
              "compiled gameplay classes present")
        check("mielon/thesift/fluid/IchorRegeneration.class" in new_names
              and "mielon/thesift/mixin/SonicSniffMotionMixin.class" in new_names,
              "both upstream 1.1.2 fix implementations present")
        print("RELEASE_SHA256=" + hashlib.sha256(RELEASE.read_bytes()).hexdigest().upper())
        print("SOURCES_SHA256=" + hashlib.sha256(SOURCES.read_bytes()).hexdigest().upper())
        print("RELEASE_112_PARITY_PASS")


if __name__ == "__main__":
    main()
