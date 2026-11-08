#!/usr/bin/env python3
"""26.3 Feature interface -> 26.2 Feature<NoneFeatureConfiguration>.

Safe scripted migration of fifteen self-contained custom worldgen features.
Uses the exact API signatures found in the pinned original Fabric 1.1.2
Minecraft 26.2 worldgen implementation. Does not replace feature algorithms.
Special SpringFeature and ModWorldgen registry are intentionally separate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "src/main/java/mielon/thesift/worldgen"
EVIDENCE = ROOT / "evidence/phase2-feature-api-262.json"
SKIP = {"SiftIchorCaveSpringFeature.java"}


def sha(v: bytes) -> str:
    return hashlib.sha256(v).hexdigest()


def port(text: str, name: str) -> str:
    cls = name.removesuffix(".java")
    declaration = f"public final class {cls} implements Feature {{"
    assert text.count(declaration) == 1, f"Unrecognised Feature declaration {cls}"
    text = text.replace(
        declaration, f"public final class {cls} extends Feature<NoneFeatureConfiguration> {{", 1
    )
    codec_lines = re.findall(
        r"(?m)^   public static final MapCodec<[^;\r\n]+;\r?\n", text
    )
    assert len(codec_lines) in (1, 2), f"Unexpected codec setup {cls}: {len(codec_lines)}"
    for line in codec_lines:
        text = text.replace(line, "", 1)
    method = re.compile(
        r"\n   public MapCodec<" + re.escape(cls) +
        r"> codec\(\) \{\r?\n(?:[^\r\n]*\r?\n){1,4}?   \}\r?\n"
    )
    text, removed = method.subn("\n", text, count=1)
    assert removed == 1, f"Unmatched codec method {cls}"

    ctor = re.compile(r"(?m)^(   public " + re.escape(cls) + r"\([^\r\n]*\) \{)\r?$")
    if ctor.search(text):
        text, n = ctor.subn(r"\1\n      super(NoneFeatureConfiguration.CODEC);", text, count=1)
        assert n == 1
    else:
        declaration_new = f"public final class {cls} extends Feature<NoneFeatureConfiguration> {{"
        text = text.replace(
            declaration_new,
            declaration_new + "\n   public " + cls +
            "() {\n      super(NoneFeatureConfiguration.CODEC);\n   }\n", 1
        )
    signature = re.compile(
        r"public boolean place\(WorldGenLevel level, ChunkGenerator generator, "
        r"RandomSource random, BlockPos (\w+)\) \{"
    )
    mat = signature.search(text)
    assert mat is not None, f"Old feature place entry point not found: {cls}"
    position = mat.group(1)
    text = signature.sub(
        "public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {\n"
        "      WorldGenLevel level = context.level();\n"
        "      ChunkGenerator generator = context.chunkGenerator();\n"
        "      RandomSource random = context.random();\n"
        "      BlockPos " + position + " = context.origin();",
        text, count=1,
    )
    text = text.replace("import com.mojang.serialization.MapCodec;\n", "", 1)
    text = text.replace(
        "import net.minecraft.world.level.levelgen.feature.Feature;\n",
        "import net.minecraft.world.level.levelgen.feature.Feature;\n"
        "import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;\n"
        "import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;\n",
        1,
    )
    assert "MapCodec" not in text and "implements Feature" not in text
    return text


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    options = ap.parse_args()
    files = sorted(p for p in BASE.glob("*Feature.java") if p.name not in SKIP)
    assert len(files) == 15, f"Expected 15 ordinary feature classes, got {len(files)}"
    transformed = []
    for p in files:
        old = p.read_bytes()
        new_text = port(old.decode("utf-8").replace("\r\n", "\n"), p.name)
        new = new_text.encode("utf-8")
        assert old != new
        transformed.append((p, old, new))
    manifest = {
        "stage": "Minecraft 26.2 Feature API migration",
        "state": "APPLIED" if options.apply else "DRY_RUN",
        "classes": len(transformed),
        "source_behavior": "Existing NeoForge26.3 algorithms, only version-specific Feature API wrappers migrated",
        "outside_scope": ["SiftIchorCaveSpringFeature", "ModWorldgen", "renderer", "entity/UI"],
        "changes": [{
            "path": str(p.relative_to(ROOT)).replace("\\", "/"),
            "before_sha": sha(old), "after_sha": sha(new),
        } for p, old, new in transformed],
    }
    if options.apply:
        for p, old, new in transformed:
            assert p.resolve().is_relative_to(ROOT.resolve()) and p.read_bytes() == old
            p.write_bytes(new)
        EVIDENCE.parent.mkdir(parents=True, exist_ok=True)
        EVIDENCE.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print("FEATURE_WRAPPERS=" + str(len(transformed)))
    print("MODE=" + manifest["state"])


if __name__ == "__main__":
    main()
