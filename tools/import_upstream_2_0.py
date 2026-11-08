"""One-time import into the NEW 26.2 port worktree; never changes originals.

The accepted 1.1.2.1 commit remains the Git parent. Keep only its NeoForge-only
platform helpers; import behavior from the exact 26.2 original reconstruction.
"""
from pathlib import Path, PurePosixPath
from zipfile import ZipFile
import hashlib
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]
THE_SIFT = ROOT.parents[1]
UPSTREAM = THE_SIFT / "reference/upstream-2.0.0-20261107/mielon's-the-sift-2.0.0-mc26.2-fabric.jar"
SOURCES = THE_SIFT / "reports/research/upstream-2.0.0-20261107/decompiled/mc26.2-fabric-vineflower-port"
MARKER = ROOT / "UPSTREAM-IMPORT-2.0.0.json"
EXPECTED_SHA = "ac6809cc012e52f727ca6d6342c27528d57e4d59c3527e3d164e1b43ca42e240"

branch = subprocess.check_output(["git", "-C", str(ROOT), "branch", "--show-current"], text=True).strip()
if ROOT.name != "neo-26.2-2.0.0-port" or branch != "port/26.2-upstream-2.0.0":
    raise RuntimeError("Refusing to import outside the explicitly isolated 2.0 worktree")
if MARKER.exists():
    raise RuntimeError("Import already ran; refusing to overwrite ongoing port adaptations")
with UPSTREAM.open("rb") as stream:
    if hashlib.file_digest(stream, "sha256").hexdigest() != EXPECTED_SHA:
        raise RuntimeError("Wrong or modified upstream original")

java_roots = (ROOT / "src/main/java", ROOT / "src/client/java")
resource_roots = (ROOT / "src/main/resources", ROOT / "src/client/resources")
keep_platform = {
    "mielon/thesift/platform/ModRegistration.java",
    "mielon/thesift/platform/NeoEvents.java",
    "mielon/thesift/entity/ModAttachments.java",
    "mielon/thesift/client/ClientModEvents.java",
    "mielon/thesift/client/mixin/SiftServerDetectionMixin.java",
    "mielon/thesift/mixin/SiftAfterBlockBreakMixin.java",
    "mielon/thesift/mixin/SiftAfterDeathMixin.java",
    "mielon/thesift/mixin/SiftAfterPlayerDeathMixin.java",
}
sources = {path.relative_to(SOURCES).as_posix(): path for path in SOURCES.rglob("*.java")}
if len(sources) != 440:
    raise RuntimeError(f"Incomplete upstream reconstruction: {len(sources)} Java files")
for rel, path in sources.items():
    if not rel.startswith("mielon/thesift/"):
        raise RuntimeError(f"Unexpected source namespace: {rel}")
    body = path.read_text(encoding="utf-8")
    if any(marker in body for marker in ("Couldn't be decompiled", "Decompilation failed", "This method has failed to decompile")):
        raise RuntimeError(f"Do not import decompiler failure bodies: {rel}")

with ZipFile(UPSTREAM) as jar:
    resources = [name for name in jar.namelist() if name.startswith(("assets/", "data/")) and not name.endswith("/")]
    for name in resources:
        path = PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts or "\\" in name:
            raise RuntimeError(f"Unsafe archive path: {name}")
    # Validate all contents before changing the isolated worktree.
    if jar.testzip() is not None:
        raise RuntimeError("Original JAR has a corrupt entry")
    removed_java = []
    for base in java_roots:
        for path in (base / "mielon/thesift").rglob("*.java"):
            rel = path.relative_to(base).as_posix()
            if rel not in sources and rel not in keep_platform:
                path.unlink()
                removed_java.append(rel)
    for rel, path in sources.items():
        destination_base = java_roots[1] if rel.startswith("mielon/thesift/client/") else java_roots[0]
        dest = destination_base / rel
        for other in java_roots:
            duplicate = other / rel
            if other != destination_base and duplicate.exists():
                duplicate.unlink()
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(path.read_bytes())
    removed_resources = []
    resource_set = set(resources)
    for base in resource_roots:
        for folder in ("assets", "data"):
            for path in (base / folder).rglob("*"):
                if path.is_file():
                    rel = path.relative_to(base).as_posix()
                    # All resource ownership is centralized in main/resources.
                    if rel not in resource_set or base != resource_roots[0]:
                        path.unlink()
                        removed_resources.append(str(path.relative_to(ROOT)).replace("\\", "/"))
    for name in resources:
        dest = resource_roots[0] / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(jar.read(name))
    for name in ("the_sift.mixins.json", "the_sift.client.mixins.json"):
        dest = resource_roots[0] / name
        dest.write_bytes(jar.read(name))
        for other in resource_roots[1:]:
            duplicate = other / name
            if duplicate.exists():
                duplicate.unlink()
    embedded_licenses = [name for name in jar.namelist() if PurePosixPath(name).name.startswith("LICENSE") and not name.endswith("/")]
    for name in embedded_licenses:
        dest = resource_roots[0] / PurePosixPath(name).name
        dest.write_bytes(jar.read(name))

MARKER.write_text(json.dumps({
    "upstream": UPSTREAM.name,
    "sha256": EXPECTED_SHA,
    "decompiler": "Vineflower 1.12.0 with exact target Minecraft 26.2/NeoForge .93 compile libraries",
    "source_count": len(sources),
    "data_asset_count": len(resources),
    "preserved_platform_only": sorted(keep_platform),
    "removed_obsolete_java": sorted(removed_java),
    "removed_obsolete_resources": sorted(removed_resources),
    "status": "IMPORTED; platform adaptation and runtime gates are not complete"
}, indent=2) + "\n", encoding="utf-8")
print(f"IMPORTED {len(sources)} Java files and {len(resources)} original 26.2 assets/data entries")
print(f"Retained {len(keep_platform)} NeoForge-only helpers; removed {len(removed_java)} obsolete source files only in NEW worktree")
