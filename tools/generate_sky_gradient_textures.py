#!/usr/bin/env python3
"""Pre-bake The Sift's existing six sky color layers into filtered RGBA maps.

Used ONLY by the independent MC26.2 Iris-compat ALPHA2 experiment. The vanilla
POSITION_COLOR mesh and existing color functions remain untouched.

Source: SiftProceduralSkyRenderer.baseColor/ribbonColor/addVertex in the
accepted 26.2 ALPHA1 source. Textures are static: the six animated mesh
transforms, day/night blending, rain dimming and GPU alpha compositing remain
dynamic and are not recomputed on the CPU per frame.

Workshop prerequisites already installed: Python 3, NumPy, Pillow. Never
downloads dependencies, runs Minecraft, or changes third-party shaderpacks.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/the_sift/textures/sky/alpha2"
W, H = 2048, 768
NAMES = (
    "base_day", "base_night",
    "ribbons_far_day", "ribbons_far_night",
    "ribbons_near_day", "ribbons_near_night",
)
DAY = np.asarray([
    [1.0, 0.61, 0.76], [0.3, 0.94, 0.97], [0.52, 1.0, 0.8],
    [0.91, 0.73, 1.0], [1.0, 0.82, 0.64],
], dtype=np.float32)
NIGHT = np.asarray([
    [0.22, 0.76, 1.0], [0.13, 0.96, 1.0], [0.36, 0.47, 1.0],
    [0.72, 0.35, 1.0], [0.92, 0.38, 0.88],
], dtype=np.float32)
TAU = np.float32(2 * np.pi)


def clamp(a):
    return np.clip(a, 0, 1)


def smooth(a):
    x = clamp(a)
    return x * x * (3 - 2 * x)


def lerp(t, a, b):
    return a + t * (b - a)


def grid():
    # Identical first/last azimuth columns; GPU clamp+linear sampling removes
    # UV seams without repeating any shaderpack-specific coordinate tricks.
    u = np.linspace(0, 1, W, dtype=np.float32)[None, :]
    v = np.linspace(0, 1, H, dtype=np.float32)[:, None]
    elevation = -90.0 + 180.0 * v
    art_v = clamp((elevation + 14.0) / 104.0)
    lower_convergence = smooth(clamp((-14.0 - elevation) / 68.0))
    ribbon_fade = 1.0 - smooth(clamp((-14.0 - elevation) / 16.0))
    return u, art_v, lower_convergence, ribbon_fade


def base_color(u, v, lower, night):
    horizon = 1 - smooth(clamp((v - 0.05) / 0.82))
    wave = 0.5 + 0.5 * np.sin(TAU * (u * 2 + 0.08))
    zenith = smooth((v - 0.68) / 0.27)
    if night:
        r = lerp(horizon, 0.025, 0.055) + wave * 0.015
        g = lerp(horizon, 0.145, 0.43) + wave * 0.035
        b = lerp(horizon, 0.31, 0.64) + wave * 0.055
        channels = (
            lerp(lower, lerp(zenith, r, 0.035), 0.055),
            lerp(lower, lerp(zenith, g, 0.19), 0.36),
            lerp(lower, lerp(zenith, b, 0.43), 0.58),
        )
    else:
        r = lerp(horizon, 0.47, 0.98)
        g = lerp(horizon, 0.82, 0.76)
        b = lerp(horizon, 0.93, 0.88)
        mint = 0.5 + 0.5 * np.sin(TAU * (u * 3 - 0.21))
        r = r - mint * 0.07 * (1 - horizon * 0.35)
        g = g + mint * 0.05 * (1 - horizon * 0.35)
        channels = (
            lerp(lower, lerp(zenith, r, 0.43), 0.94),
            lerp(lower, lerp(zenith, g, 0.87), 0.78),
            lerp(lower, lerp(zenith, b, 0.94), 0.88),
        )
    return (*channels, np.ones((H, W), dtype=np.float32))


def ribbon_color(u, v, lower_fade, night, variant):
    palette = NIGHT if night else DAY
    r = np.zeros((H, W), dtype=np.float32)
    g = r.copy()
    b = r.copy()
    total = r.copy()
    combined = r.copy()
    for i, (cr, cg, cb) in enumerate(palette):
        freq = 1.0 + i % 3
        phase = i * 0.173 + variant * 0.271
        center = (
            0.13 + i * 0.17
            + (0.115 - variant * 0.018) * np.sin(TAU * (u * freq + phase))
            + 0.038 * np.sin(TAU * (u * (freq + 2.0) - phase))
        )
        width = (0.135 if variant == 0 else 0.085) + (i % 2) * 0.018
        band = smooth(clamp(1 - np.abs(v - center) / width))
        pulse = np.sin(TAU * (u * 2 + phase))
        band *= 0.78 + 0.22 * pulse * pulse
        band *= 1 - smooth((v - 0.7) / 0.22)
        r += cr * band
        g += cg * band
        b += cb * band
        total += band
        combined = 1 - (1 - combined) * (1 - band * 0.58)
    occupied = total > 1.0e-4
    safe = np.maximum(total, 1.0e-4)
    glow = (0.48 if night else 0.42) if variant == 0 else (0.37 if night else 0.3)
    # Exact transparent pixels are white just as in the previous Java branch.
    # The texture sampler's alpha interpolation now produces smooth edges.
    return (
        np.where(occupied, r / safe, 1.0),
        np.where(occupied, g / safe, 1.0),
        np.where(occupied, b / safe, 1.0),
        clamp(combined * glow * lower_fade),
    )


def layer(name: str) -> np.ndarray:
    u, v, convergence, fade = grid()
    if name.startswith("base_"):
        channels = base_color(u, v, convergence, night=name.endswith("_night"))
    else:
        channels = ribbon_color(
            u, v, fade,
            night=name.endswith("_night"),
            variant=0 if "_far_" in name else 1,
        )
    # Java's Math.round(float * 255): positive numbers rounded half-up.
    data = np.stack(np.broadcast_arrays(*channels), axis=-1)
    assert np.isfinite(data).all()
    return np.floor(clamp(data) * 255.0 + 0.5).astype(np.uint8)


def main() -> None:
    cli = argparse.ArgumentParser()
    mode = cli.add_mutually_exclusive_group(required=True)
    mode.add_argument("--write", action="store_true")
    mode.add_argument("--verify", action="store_true")
    options = cli.parse_args()
    if options.write:
        OUTPUT.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for name in NAMES:
        rgba = layer(name)
        path = OUTPUT / f"{name}.png"
        assert np.array_equal(rgba[:, 0, :], rgba[:, -1, :]), f"Azimuth seam: {name}"
        if name.startswith("base"):
            assert rgba[:, :, 3].min() == 255
        else:
            assert rgba[:, :, 3].max() > 45
            assert rgba[-1, :, 3].max() == 0
        if options.write:
            # Fail closed when an existing generated image has changed.
            if path.exists():
                with Image.open(path) as previous:
                    assert np.array_equal(np.asarray(previous.convert("RGBA")), rgba), path
            else:
                Image.fromarray(rgba, "RGBA").save(path, format="PNG", compress_level=9)
        assert path.is_file()
        with Image.open(path) as png:
            assert png.size == (W, H) and png.mode == "RGBA"
            assert np.array_equal(np.asarray(png), rgba), f"Image content changed: {path}"
        hashes[path.name] = {
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "bytes": path.stat().st_size,
        }
        print(f"PASS {path.name} {path.stat().st_size:,} B {hashes[path.name]['sha256'][:16]}")
    report = {
        "version": "experimental-sky-textures-alpha2",
        "dimensions": [W, H],
        "format": "RGBA8",
        "images": hashes,
        "concept": "Six existing static sky RGBA layers, sampled at interpolated UV, shader pipeline TEXTURED",
        "alpha_modulation": "Unchanged per-frame six layer DynamicTransforms; no per-frame texture generation",
        "image_reproducibility": "pixel-exact under pinned NumPy/Pillow environment",
    }
    if options.write:
        manifest = ROOT / "evidence/alpha2-baked-sky-textures.json"
        manifest.parent.mkdir(parents=True, exist_ok=True)
        manifest.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("SKY_TEXTURES_" + ("GENERATED" if options.write else "VERIFIED") + "=6/6")


if __name__ == "__main__":
    main()
