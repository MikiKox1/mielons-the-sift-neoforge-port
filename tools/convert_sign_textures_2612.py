#!/usr/bin/env python3
"""Deterministic source-UV remap: original 2.0.1 32x32 blocks -> MC26.1.2 64x32 sign entity sheets.

This is a pixel rearrangement, not a resized or fake Oak texture. The canonical
board faces and wood colors come directly from the pinned Fabric MC26.2 2.0.1 PNGs.
Run without flags to verify existing generated files, or --write to create them.
"""

from __future__ import annotations

import argparse
import hashlib
import io
from pathlib import Path
from zipfile import ZipFile

from PIL import Image


WORK = Path(__file__).resolve().parents[1]
ASSETS = WORK / "src/main/resources/assets/the_sift"
SOURCE_JAR = WORK.parents[1] / "reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.2-fabric.jar"
SOURCE_JAR_SHA256 = "ae43039aef36b773a063b28080f446f7a57adbcd9cf8009b0a8c33746e725a72"
WOODS = ("overgrown_willow", "ash_willow")


def paste(sheet: Image.Image, source: Image.Image, dest: tuple[int, int], area: tuple[int, int, int, int]) -> None:
    part = source.crop(area)
    assert dest[0] >= 0 and dest[1] >= 0
    assert dest[0] + part.width <= 64 and dest[1] + part.height <= 32
    sheet.paste(part, dest)


def standing(source: Image.Image) -> Image.Image:
    """Preserve original 26.2 block template board faces at the target entity UVs.

    Original 26.2 block model template_sign_rot_0 uses UV units in 32x32:
      south (0,1)-(12,7), north (0,8)-(12,14), cap (0,0)-(12,1),
      underside (0,14)-(12,15), sides (12,1)-(13,7)/(12,8)-(13,14).
    The target 26.1.2 StandingSignRenderer builds a (24,12,2) body at texOffs(0,0)
    and a (2,14,2) post at texOffs(0,14), both with 64x32 layer UV dimensions.
    """
    dst = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    mapping = [
        ((2, 2), (0, 16, 24, 28)),     # north 24x12 original pixels
        ((28, 2), (0, 2, 24, 14)),     # south 24x12 original pixels
        ((0, 2), (24, 16, 26, 28)),   # west edge
        ((26, 2), (24, 2, 26, 14)),   # east edge
        ((2, 0), (0, 0, 24, 2)),      # top board
        ((26, 0), (0, 28, 24, 30)),   # bottom board
        ((0, 16), (28, 16, 30, 30)),  # post sides retain old 2x14 pixels
        ((2, 16), (28, 0, 30, 14)),
        ((4, 16), (30, 16, 32, 30)),
        ((6, 16), (30, 0, 32, 14)),
        ((2, 14), (28, 30, 30, 32)),
        ((4, 14), (28, 30, 30, 32)),
    ]
    for dest, rect in mapping:
        paste(dst, source, dest, rect)
    return dst


def hanging(source: Image.Image) -> Image.Image:
    """Reuse original 26.2 hanging board UV exactly and map it to target cuboid UV.

    MC26.2 template_hanging_sign_rot_0 uses board at x=[0..32], y=[14..28];
    MC26.1.2 HangingSignRenderer uses texOffs(0,12) for its (14,10,2) board.
    Wall plank and chains are sampled from their real 2.0.1 template regions.
    """
    dst = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    mapping = [
        ((0, 14), (0, 16, 2, 26)),       # board west
        ((2, 14), (18, 16, 32, 26)),     # board north
        ((16, 14), (16, 16, 18, 26)),    # board east
        ((18, 14), (2, 16, 16, 26)),     # board south
        ((2, 12), (2, 14, 16, 16)),      # board top
        ((16, 12), (2, 26, 16, 28)),     # board bottom
        ((4, 0), (0, 0, 16, 4)),         # wall attachment plank top
        ((20, 0), (0, 0, 16, 4)),        # wall attachment plank bottom
        ((4, 4), (0, 4, 16, 6)),         # wall plank north
        ((24, 4), (0, 4, 16, 6)),        # wall plank south
        ((0, 4), (16, 4, 20, 6)),        # wall plank west
        ((20, 4), (16, 4, 20, 6)),       # wall plank east
        ((0, 6), (22, 7, 25, 13)),      # diagonal L/R chains
        ((6, 6), (28, 7, 31, 13)),
        ((14, 6), (20, 0, 32, 6)),      # ceiling middle vertical chains
    ]
    for dest, rect in mapping:
        paste(dst, source, dest, rect)
    return dst


def png_bytes(image: Image.Image) -> bytes:
    output = io.BytesIO()
    image.save(output, format="PNG", optimize=False, compress_level=9)
    return output.getvalue()


def generate(write: bool) -> None:
    with SOURCE_JAR.open("rb") as file:
        assert hashlib.file_digest(file, "sha256").hexdigest() == SOURCE_JAR_SHA256

    outputs = []
    with ZipFile(SOURCE_JAR) as jar:
        for wood in WOODS:
            for kind, converter, subpath in (
                ("sign", standing, f"{wood}.png"),
                ("hanging_sign", hanging, f"hanging/{wood}.png"),
            ):
                source_name = f"assets/the_sift/textures/block/{wood}_{kind}.png"
                source_data = (ASSETS / "textures/block" / f"{wood}_{kind}.png").read_bytes()
                assert jar.read(source_name) == source_data, f"Non-original source art: {source_name}"
                source = Image.open(io.BytesIO(source_data)).convert("RGBA")
                assert source.size == (32, 32), source_name
                sheet = converter(source)
                assert sheet.size == (64, 32)
                # The two important faces must be genuinely opaque and use only
                # pixels from the 2.0.1 source (no generated Oak/placeholder art).
                face_y = 8 if kind == "sign" else 18
                assert sheet.getpixel((9, face_y))[3] > 0 and sheet.getpixel((21, face_y))[3] > 0
                target = ASSETS / "textures/entity/signs" / subpath
                payload = png_bytes(sheet)
                if write:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_bytes(payload)
                else:
                    assert target.is_file() and target.read_bytes() == payload, f"Entity sign PNG changed: {target}"
                outputs.append((target.relative_to(WORK).as_posix(), hashlib.sha256(payload).hexdigest()))
    print(f"{'WRITTEN' if write else 'VERIFIED'} {len(outputs)} original-art entity sprites (64x32)")
    for path, digest in outputs:
        print(f"{path} SHA256={digest}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="Create only the four owned entity sign PNGs")
    generate(parser.parse_args().write)
