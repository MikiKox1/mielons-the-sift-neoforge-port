#!/usr/bin/env python3
"""F4a deterministic client RenderPearl -> original MC26.2 Blaze3D API.

This only touches 8 API-adapter/pipeline/mixin classes for which the exact
original Mielon's The Sift 1.1.2 Fabric26.2 class was pinned and decompiled.
It does NOT copy Fabric mod bootstrap or remove independent NeoForge features.
All changes are atomic only after a complete successful preflight.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SIFT = ROOT.parent.parent
PREP = SIFT / "backports/26.2-1.1.2-prep"
FAB = PREP / "reference/upstream-mielon-1.1.2-fabric-26.2.jar"
ORIGINAL = PREP / "research/original26.2-client-render-decompiled"
JAVA_ROOT = ROOT / "src/client/java"
MANIFEST = ROOT / "evidence/phase4a-blaze3d-262.json"

FILES = (
    "compat/SiftShaderCompat.java",
    "loading/SiftLoadingBackgroundRenderer.java",
    "mixin/SiftCloudRendererMixin.java",
    "mixin/SiftSkyRendererMixin.java",
    "render/RiftRenderTypes.java",
    "render/SiftCloudRenderPipelines.java",
    "render/SiftPortalRenderTypes.java",
    "render/SonorousBeamRenderTypes.java",
)
PACKAGE = Path("mielon/thesift/client")


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def build(rel: str, old: str, current: str) -> str:
    assert "package mielon.thesift.client." in old, rel
    assert current.startswith("package mielon.thesift.client."), rel
    if rel == "compat/SiftShaderCompat.java":
        assert old.count("FabricLoader.getInstance().isModLoaded") == 1
        old = old.replace("import net.fabricmc.loader.api.FabricLoader;",
                          "import net.neoforged.fml.ModList;")
        old = old.replace('FabricLoader.getInstance().isModLoaded("iris")',
                          'ModList.get().isLoaded("iris")')
        assert "FabricLoader" not in old and "ModList.get()" in old
    if rel == "mixin/SiftCloudRendererMixin.java":
        # Preserve 26.3's safer passthrough for other mods' cloud pipelines.
        original = ("return original == RenderPipelines.FLAT_CLOUDS ? "
                    "SiftCloudRenderPipelines.FLAT : SiftCloudRenderPipelines.FANCY;")
        replacement = ("return original == RenderPipelines.FLAT_CLOUDS ? "
                       "SiftCloudRenderPipelines.FLAT : "
                       "(original == RenderPipelines.CLOUDS ? SiftCloudRenderPipelines.FANCY : original);")
        assert old.count(original) == 1
        old = old.replace(original, replacement)
        assert "RenderPass;setPipeline" in old
        assert "renderOit" not in old
    if rel == "mixin/SiftSkyRendererMixin.java":
        assert "renderSunMoonAndStars" in old
        assert "RenderTarget" in old
    if rel.endswith(("RiftRenderTypes.java", "SiftPortalRenderTypes.java", "SonorousBeamRenderTypes.java")):
        assert "com.mojang.blaze3d.pipeline.RenderPipeline" in old
        assert "MATRICES_PROJECTION" in old
        assert "com.mojang.renderpearl" not in old
    if rel == "render/SiftCloudRenderPipelines.java":
        assert "OIT_" not in old and "FLAT" in old and "FANCY" in old
        assert "com.mojang.blaze3d.pipeline.RenderPipeline" in old
    if rel == "loading/SiftLoadingBackgroundRenderer.java":
        assert "RenderPipeline" in old
    # No source from Fabric may retain a direct Fabric-only reference.
    assert "net.fabricmc" not in old
    assert "com.mojang.renderpearl" not in old
    return old.rstrip() + "\n"


def main() -> None:
    args = argparse.ArgumentParser()
    args.add_argument("--apply", action="store_true")
    options = args.parse_args()
    assert FAB.is_file() and sha(FAB.read_bytes()) == (
        "1013807c2dea8f366f97347ab74cc895518799caf0e9513ff19a660567d33886"
    )
    changes = []
    for rel in FILES:
        source = JAVA_ROOT / PACKAGE / rel
        original = ORIGINAL / PACKAGE / rel
        assert source.is_file() and original.is_file() and not source.is_symlink()
        assert source.resolve().is_relative_to(ROOT.resolve())
        current = source.read_bytes()
        target = build(rel, original.read_text(encoding="utf-8"),
                       current.decode("utf-8").replace("\r\n", "\n")).encode()
        assert sha(current) != sha(target), rel
        changes.append((source, current, target))
    manifest = {
        "stage": "F4a exact original 26.2 Blaze3D pipelines and mixins",
        "mode": "APPLIED" if options.apply else "DRY_RUN",
        "source_jar_sha256": sha(FAB.read_bytes()),
        "changes": [{
            "path": str(source.relative_to(ROOT)).replace("\\", "/"),
            "old_sha256": sha(current),
            "new_sha256": sha(target),
        } for source, current, target in changes],
        "preserved": [
            "NeoForge ModList detection of Iris",
            "Cloud pipeline passthrough for unknown custom pipelines",
            "Port1.1.2 sky shader tessellation in separate SiftProceduralSkyRenderer",
            "NeoForge-specific renderer/state/portal/beam improvements",
        ],
    }
    if options.apply:
        for source, current, target in changes:
            assert source.read_bytes() == current
            source.write_bytes(target)
            assert sha(source.read_bytes()) == sha(target)
        MANIFEST.parent.mkdir(parents=True, exist_ok=True)
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
    print("RENDER_API_262_MODE=" + manifest["mode"])
    print("CHANGED_RENDER_CLASSES=" + str(len(changes)))
    print("SHADERPACK_AND_NEOFORGE_FIXES_PRESERVED=YES")


if __name__ == "__main__":
    main()
