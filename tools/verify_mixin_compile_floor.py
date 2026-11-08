"""Verify actual compiled Redirect annotations match the minimum loader's ABI.

NeoForge26.1.2.100 bundles MixinExtras0.5.4, which expects Redirect.at as one
AnnotationNode. FabricMixin0.17.4 changed the annotation API to At[], so even a
single source @At compiles to an array. Use the minimum-compatible compile API,
not a gameplay edit, runtime downgrade or another bundled library.
"""
from pathlib import Path
import argparse
import re
import subprocess
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
JAVAP = Path(r"C:\Program Files\Java\jdk-25.0.4.1\bin\javap.exe")
REDIRECT = b"Lorg/spongepowered/asm/mixin/injection/Redirect;"


def verify(jar):
    jar = Path(jar).resolve()
    with ZipFile(jar) as archive:
        classes = [n[:-6].replace("/", ".") for n in archive.namelist()
                   if n.endswith(".class") and n.startswith("mielon/")
                   and REDIRECT in archive.read(n)]
    assert classes, "No compiled Redirect found; test cannot be vacuously satisfied"
    result = subprocess.run([str(JAVAP), "-v", "-p", "-classpath", str(jar), *classes],
                            capture_output=True, encoding="utf8", errors="replace", check=True)
    sections = result.stdout.split("org.spongepowered.asm.mixin.injection.Redirect(")[1:]
    assert sections, "javap did not expose annotations"
    bad = []
    for i, section in enumerate(sections, 1):
        match = re.search(r"\bat=(\[|@)", section)
        if not match or match.group(1) != "@":
            bad.append(i)
    assert not bad, f"Minimum MixinExtras0.5.4 cannot read array Redirect.at: hooks {bad}"
    return {"redirect_classes": len(classes), "single_annotation_redirects": len(sections),
            "all_minimum_compatible": True, "production_bytecode_mutated": False}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", type=Path, default=ROOT / "build/libs/the_sift-2.0.1-neoforge.1-mc26.1.2-alpha.1.jar")
    args = parser.parse_args()
    print("MINIMUM_MIXIN_ANNOTATION_ABI_PASS", verify(args.jar))
