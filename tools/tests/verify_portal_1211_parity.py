#!/usr/bin/env python3
"""Validate the MC26.1.2/MC26.2 original 1.21.1 portal backport."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import re
import sys
import tomllib

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
ASSETS = ROOT / "src/main/resources"
OFFICIAL = SIFT / "backports/26.1.2-1.1.2-prep/reference/mielon's-the-sift-1.1.2-mc1.21.1-neo.jar"
BASELINES = {
    "26.1.2": ("Port-1.1.2-NeoForge-26.1.2-FINAL/mielon's-the-sift-1.1.2-mc26.1.2-neoforge-port.jar",
               "3a008f77999717047885bce0954a2b95bd7b219189faff11bc1ce5dae3cfb16a"),
    "26.2": ("Port-1.1.2-SkyFix-Dual-26.2-26.3/mielon's-the-sift-1.1.2-mc26.2-neoforge-port.jar",
             "9740da26b007c909b60716c78c90691409e30db7bff3a6fef9321462c58a4a7d"),
}
VERSION = next(s.split("=", 1)[1].strip() for s in
               (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
               if s.startswith("minecraft_version="))


def check(ok, message):
    if not ok:
        raise AssertionError(message)
    print("PASS", message)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def norm(text):
    return re.sub(r"\s+", "", re.sub(r"//[^\n]*", "", text))


check(VERSION in BASELINES, "MC26.1.2 or MC26.2 targeted")
with ZipFile(OFFICIAL) as official:
    check(official.testzip() is None, "Original 1.21.1 JAR CRC")
    shared = "assets/the_sift/shaders/include/portal_fields.glsl"
    check(norm(official.read(shared).decode()) ==
          norm((ASSETS / shared).read_text(encoding="utf-8")), "Original shared noise GLSL")
    for filename in ("sift_portal.fsh", "sift_portal.vsh"):
        name = "assets/the_sift/shaders/core/" + filename
        before = official.read(name).decode()
        after = (ASSETS / name).read_text(encoding="utf-8")
        check(norm(before[before.index("void main()"):]) ==
              norm(after[after.index("void main()"):]), filename + " original main logic")
    path = "assets/the_sift/textures/misc/sift_portal_shaderpack.png"
    check(official.read(path) == (ASSETS / path).read_bytes(), "Original shaderpack PNG unchanged")

fsh = (ASSETS / "assets/the_sift/shaders/core/sift_portal.fsh").read_text(encoding="utf-8")
renderer = (ROOT / "src/client/java/mielon/thesift/client/render/SiftPortalRenderer.java").read_text(encoding="utf-8")
check("#moj_import <minecraft:projection.glsl>" in fsh and
      "#moj_import <minecraft:globals.glsl>" in fsh and
      "RenderTypes.beaconBeam(SHADERPACK_TEXTURE, false)" in renderer,
      "Version-specific shader imports and unchanged Beacon fallback")

if len(sys.argv) > 1:
    base_name, expected = BASELINES[VERSION]
    baseline = SIFT / "artifacts/release-candidates" / base_name
    check(sha(baseline) == expected, "Prior accepted JAR SHA unchanged")
    with ZipFile(baseline) as old, ZipFile(sys.argv[1]) as new:
        check(new.testzip() is None, "Built JAR CRC")
        names = set(new.namelist())
        check(names == set(old.namelist()), "JAR inventory unchanged")
        changes = {name for name in names if old.read(name) != new.read(name)}
        check(not any(name.endswith(".class") for name in changes),
              "No Java class changes (gameplay/rendering preserved)")
        allowed = {"META-INF/neoforge.mods.toml",
                   "assets/the_sift/shaders/include/portal_fields.glsl",
                   "assets/the_sift/shaders/core/sift_portal.fsh",
                   "assets/the_sift/shaders/core/sift_portal.vsh"}
        unexpected = changes - allowed
        check(not unexpected, "Only 3 shaders and version metadata changed: " + repr(unexpected))
        check(allowed == changes, "All three intended shaders and metadata packaged")
        metadata = tomllib.loads(new.read("META-INF/neoforge.mods.toml").decode("utf-8-sig"))
        dep = {d["modId"]: d for d in metadata["dependencies"]["the_sift"]}
        check(metadata["mods"][0]["version"] == "1.1.2-neoforge.2-alpha.1" and
              dep["minecraft"]["versionRange"] == "[" + VERSION + "]" and
              dep["neoforge"]["versionRange"].endswith(",)") and
              metadata["license"] == "MIT",
              "MC target, open NeoForge minimum and MIT preserved")

print("ORIGINAL_PORTAL_PARITY=PASS")
