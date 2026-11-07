# Changelog — Minecraft 26.3 (NeoForge)

Released versions are listed **newest first**.

## Port 1.1.2.1 — Minecraft 26.3

- Fixed Singer rendering artifacts with Sodium 0.9.3-beta.1 and Iris 1.11.7; the issue was not present with Sodium 0.9.3-alpha.1.
- Fixed the Sift portal rendering.

[Port 1.1.2.1 — Minecraft 26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2.1-mc26.3-neoforge)

## Port 1.1.2 — Minecraft 26.3

The NeoForge 26.3 edition now includes Mielon's upstream 1.1.2 gameplay changes and 26.3-specific sky compatibility updates.

- Updated **Ichor Regeneration I** to detect contact with flowing fluid, cauldrons and fluid-containing blocks and refresh the effect without overriding stronger independent effects.
- Updated **Dark Sniffer Sonic Sniff** with UUID-based per-wave target tracking and corrected saved player motion.
- Corrected the **Sift precipitation renderer** for Ichor rain and snow in their respective biomes.
- Reworked the **six-layer Sift sky** using smooth RGBA gradient textures and interpolated texture coordinates.
- Improved **Complementary Shaders** rendering by removing prominent sky bands, rings and triangular facets.
- Added **Photon Shaders** final-pass depth-masked sky composition that preserves terrain, moonlight and clouds.
- The **Photon rendering path** prepares sky textures and GPU buffers before the rendering pass.
- The original animated sky also renders without shaders.
- Preserved portal/Rift, Singer lighting, Sonorous beams, Ichor buckets, Charoite and optional Easy Magic integration from Port 1.1.1.

[Port 1.1.2 — Minecraft 26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2-mc26.3-neoforge)

## Port 1.1.1 — Minecraft 26.3

The first public NeoForge edition of Mielon's The Sift 1.1.1 for Minecraft 26.3.

- Adapted block and entity registration, world generation, dimension access, networking, fluid behavior and client rendering to NeoForge.
- Preserved the original `the_sift` ID, 10 biomes, world structures, creatures, items, Siftite and Charoite progression, Rifts, sounds and advancements.
- **Ichor:** Fluid flow, bucket collection, pickup and waterlogging interactions.
- **Ancient City portals and Rifts:** Portal activation and return travel.
- **Singer and Sonorous beams:** Dynamic lighting and beam effects.
- **Vegetation and sounds:** Sift plants respond to fluid contact; Echo Golem sounds and the Sift sky are included.
- Added optional **Easy Magic 26.3.0** Charoite integration while preserving vanilla enchanting; LambDynamicLights support remains optional.
- Set the minimum NeoForge loader to **26.3.0.64-beta**, with no upper loader bound.

[Port 1.1.1 — Minecraft 26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.1-neoforge.2-community.3-fix.1)

**Original mod:** Mielon (MIT). **Unofficial NeoForge port:** MikiKox1. See [README.md](README.md) for the current, version-specific dependencies and [SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md) for credits.
