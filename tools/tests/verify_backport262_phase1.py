#!/usr/bin/env python3
"""Static phase acceptance for independent The Sift 1.1.2 NeoForge 26.2.

Does NOT assert a successful compile/client/server/gameplay gate.
"""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
PREP = SIFT / "backports/26.2-1.1.2-prep"
FABRIC = PREP / "reference/upstream-mielon-1.1.2-fabric-26.2.jar"
RELEASE = PREP / "reference/our-port-1.1.2-neoforge-26.3.jar"
WORLDGEN = ROOT / "src/main/resources/data/the_sift/worldgen"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def check(ok: bool, message: str) -> None:
    assert ok, message
    print("PASS " + message)


def main() -> None:
    check(sha(FABRIC.read_bytes()) ==
          "1013807c2dea8f366f97347ab74cc895518799caf0e9513ff19a660567d33886",
          "official Fabric 26.2 1.1.2 pinned")
    check(sha(RELEASE.read_bytes()) ==
          "d3bd9a6bf718e755e1da42190ff726a48d3fe6708245ed879a00883434e27cee",
          "our accepted NeoForge 26.3 1.1.2 pinned")
    wg = json.loads((ROOT / "evidence/phase2-fabric-worldgen-262.json").read_text(encoding="utf-8"))
    pack = json.loads((ROOT / "evidence/phase2-pack-262.json").read_text(encoding="utf-8"))
    features = json.loads((ROOT / "evidence/phase2-feature-api-262.json").read_text(encoding="utf-8"))
    check(wg["state"] == "APPLIED" and pack["mode"] == "APPLIED"
          and features["state"] == "APPLIED",
          "all three isolated migrations have receipts")
    paths = {
        str(p.relative_to(WORLDGEN)).replace("\\", "/"): sha(p.read_bytes())
        for p in WORLDGEN.rglob("*.json")
    }
    check(len(paths) == 158 and paths == wg["target_paths"],
          "158 exactly matched original 26.2 worldgen JSON files")
    with ZipFile(FABRIC) as f, ZipFile(RELEASE) as r:
        check(f.testzip() is None and r.testzip() is None, "pinned original ZIP CRCs")
        nbt = [x for x in f.namelist() if x.endswith(".nbt")]
        check(len(nbt) == 16 and all(f.read(x) == r.read(x) for x in nbt),
              "all 16 original NBT structures preserve exact bytes")
    check(pack["restored_files"] == 147 and len(pack["resources"]) == 147,
          "147 additional format-sensitive resource files")
    for item in pack["resources"]:
        name = item["name"]
        candidates = [ROOT / "src/main/resources" / name, ROOT / "src/client/resources" / name]
        found = [p for p in candidates if p.is_file()]
        assert len(found) == 1 and sha(found[0].read_bytes()) == item["target_sha256"], name
    print("PASS all 147 target resource hashes after restore")
    d = ROOT / "src/main/java/mielon/thesift/worldgen"
    classes = [p for p in d.glob("*Feature.java") if p.name != "SiftIchorCaveSpringFeature.java"]
    check(len(classes) == 15, "15 normal feature implementations")
    for p in classes:
        src = p.read_text(encoding="utf-8")
        assert "extends Feature<NoneFeatureConfiguration>" in src, p.name
        assert "FeaturePlaceContext<NoneFeatureConfiguration>" in src, p.name
        assert "super(NoneFeatureConfiguration.CODEC)" in src, p.name
        assert "MapCodec" not in src, p.name
    check(len(features["changes"]) == 15, "15 old-API Feature wrappers migrated")
    spring = (d / "SiftIchorCaveSpringFeature.java").read_text(encoding="utf-8")
    check("Feature<SpringConfiguration>" in spring
          and "FeaturePlaceContext<SpringConfiguration>" in spring,
          "spring feature uses old 26.2 SpringConfiguration")
    registrar = (d / "ModWorldgen.java").read_text(encoding="utf-8")
    keys = re.findall(r'\bregister\("([^"]+)"', registrar)
    check(len(keys) == len(set(keys)) == 18
          and "BuiltInRegistries.FEATURE" in registrar
          and "ModRegistration.register" in registrar,
          "18 feature registries and delayed NeoForge registration preserved")
    props = dict(
        s.split("=", 1) for s in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines()
        if "=" in s and not s.startswith("#")
    )
    check(props["minecraft_version"] == "26.2"
          and props["neo_version"] == "26.2.0.90"
          and props["neo_min_version"] == "26.2.0.90"
          and props["geckolib_version"] == "5.5.6",
          "target MC26.2 / latest NeoForge .90 / GeckoLib 5.5.6")
    template = (ROOT / "src/main/templates/META-INF/neoforge.mods.toml").read_text(encoding="utf-8")
    expected = 'versionRange="[' + chr(36) + '{neo_min_version},)"'
    check(expected in template, "NeoForge range stays open-ended")
    print("STATIC_PHASE_262=PASS; COMPILATION_AND_RUNTIME=NOT_YET_PASS")


if __name__ == "__main__":
    main()
