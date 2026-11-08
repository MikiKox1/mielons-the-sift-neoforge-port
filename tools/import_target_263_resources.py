"""Import only pinned upstream target assets/data into this new MC26.3 worktree.

Keep NeoForge-only overlays. Retire only resource names that belong to the
previous exact upstream and no longer exist in MC26.3. Never change a reference.
"""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
SIFT = ROOT.parents[1]
REF = SIFT / "reference/upstream-2.0.1-20261107"
OLD = REF / "mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
NEW = REF / "mielon's-the-sift-2.0.1-mc26.3-fabric.jar"
PINS = {OLD: "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72",
        NEW: "b1dd0faae623797418c42197c4b66ffe2e96e08cabd1d9329ec5661c7c6195ed"}


def resources(archive):
    return {n for n in archive.namelist()
            if n.startswith(("assets/", "data/")) and not n.endswith("/")}


def main():
    assert ROOT.name == "neo-26.3-2.0.1-port-alpha1", "Wrong destination"
    assert "minecraft_version=26.3" in (ROOT / "gradle.properties").read_text(encoding="utf8")
    for file, pin in PINS.items():
        with file.open("rb") as stream:
            assert hashlib.file_digest(stream, "sha256").hexdigest() == pin, file
    roots = [ROOT / p for p in ("src/main/resources", "src/client/resources", "src/generated/resources")]
    with ZipFile(OLD) as old, ZipFile(NEW) as new:
        before, after = resources(old), resources(new)
        assert len(before) == 2555 and len(after) == 2573
        extra_before = {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                        for root in roots if root.exists() for p in root.rglob("*")
                        if p.is_file() and p.relative_to(root).as_posix().startswith(("assets/", "data/"))
                        and p.relative_to(root).as_posix() not in before | after}
        retired = []
        for name in sorted(before - after):
            for root in roots:
                path = root / name
                if path.is_file():
                    assert path.resolve().is_relative_to(ROOT.resolve())
                    path.unlink()
                    retired.append(str(path.relative_to(ROOT)))
        for name in sorted(after):
            found = [root / name for root in roots if (root / name).is_file()]
            assert len(found) <= 1, f"Ambiguous resource {name}"
            path = found[0] if found else roots[0] / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(new.read(name))
        for name in after:
            found = [root / name for root in roots if (root / name).is_file()]
            assert len(found) == 1 and found[0].read_bytes() == new.read(name)
        for name, digest in extra_before.items():
            found = [root / name for root in roots if (root / name).is_file()]
            assert len(found) == 1 and hashlib.sha256(found[0].read_bytes()).hexdigest() == digest
        manifest = {
            "source_jar_sha256": PINS[NEW], "target_minecraft": "26.3",
            "source_version": "2.0.1", "source_resource_count": len(after),
            "resource_sha256": {n: hashlib.sha256(new.read(n)).hexdigest() for n in sorted(after)},
            "retired_mc262_paths": retired,
            "preserved_neoforge_overlay_sha256": extra_before,
        }
        (ROOT / "UPSTREAM-26.3-RESOURCE-MANIFEST.json").write_text(
            json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf8")
        print("TARGET_RESOURCES_BYTE_MATCH", len(after))
        print("RETIRED_262_RESOURCES", len(retired))
        print("PRESERVED_NEOFORGE_OVERLAYS", len(extra_before))
        print("OVERLAYS", sorted(extra_before))


if __name__ == "__main__":
    main()
