# Changelog — Minecraft 26.2 (NeoForge)

Released versions are listed **newest first**.

## Port 1.1.2.1 — Minecraft 26.2

- Fixed the Sift portal rendering.

[Port 1.1.2.1 — Minecraft 26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2.1-mc26.2-neoforge)

## Port 1.1.2 — Minecraft 26.2

A distinct NeoForge implementation of Mielon's The Sift 1.1.2 for Minecraft 26.2.

- Adapted the original 26.2 dimension, **biomes, world generation, features, carvers, structure templates, entities, fluids, networking, menus and registries** to NeoForge.
- Preserved the original `the_sift` content, advancement progression, blocks, mobs, structures and loot.
- Adapted portal, Rift, Sonorous beam, Singer and animated-sky rendering to the Minecraft 26.2 graphics APIs.
- **Ichor** provides contact-based Regeneration I for fluid, Ichor-containing blocks and cauldrons.
- **Dark Sniffer Sonic Sniff** affects each target once per wave and retains player movement across save/rejoin.
- **Ichor rain and snow** appear in their respective biomes, including Ichor-Snowy Peaks.
- Preserved **Ancient City portals, Rift return travel, Charoite enchanting and anvil restrictions**; adapted willow growth, bonemeal, stripping and composting.
- The **six-layer animated Sift sky** uses smooth RGBA gradient textures and interpolated UVs, including **Complementary Shaders** compatibility.
- **Photon Shaders** depth-masked sky composition runs after Iris's final pass and keeps sky layers behind terrain.
- Preserved the native shaderless sky, and added optional **Easy Magic 26.2.0** integration.

[Port 1.1.2 — Minecraft 26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2-mc26.2-neoforge)

**Original mod:** Mielon (MIT). **Unofficial NeoForge port:** MikiKox1. See [README.md](README.md) for the current, version-specific dependencies and [SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md) for credits.
