# Changelog — Minecraft 26.2 (NeoForge)

Released versions are listed **newest first**.

## Port 2.0.1 — Minecraft 26.2

The Sift 2.0.1 substantially expands the dimension, creatures, equipment and progression in this unofficial Minecraft 26.2 NeoForge port.

### World and exploration

- Rebuilt the Sift with **11 biomes**: new Coral Meadow, Coral Forest, Organic Desert, Corrupted Sift and three distinct underground biomes, alongside the returning landscapes.
- New caves, entrances, Ichor terraces and canyons, huge rib fossils and biome-specific abandoned portal designs and loot.
- New **Abandoned Camps, Ichor Domes, Soulshale Shelters and Towers**, plus the buried **Sculkers' Lair**, with Stalkers, Scavengers, Royal Sculk and the Monarch Upgrade Smithing Template.
- Added explorer maps leading to remote camps and the lair.
- Introduced **FLOW, ENDURE and THRIVE** tide phases, a changing eight-layer sky, and sleeping during Endure Tide or thunderstorms.

### The Monarch, mobs and items

- **The Monarch** boss can be summoned with a Sculkflower at a Royal Sculk totem. Its **Monarch Membrane** enables a smithing upgrade to **Monarch Elytra**, with jump-controlled upward flight while gliding.
- Three **Sifter** types — Seedling, Nester and Pollinator — have different attacks. The **Sifter Cap** uses their hides to prevent attacks until provoked.
- Added breedable **Slabbers and Nuzzles**; Nuzzles may be dyed and sheared. Added Blub variants, **Blubby Berries** for taming and feeding, and stacking Blub towers.
- Added buildable **Echo Golems** using Polished Soulshale and a Carved Pumpkin.
- New **Soulshale** block families, full **Ash Willow** wood set, coral/sculk plants, Charoite/Siftite storage blocks and Siftite Beacon compatibility.
- Added the 3×3 **Monarch** painting, new barter/structure loot and other equipment resources.

### Ichor, atmosphere and progression

- Rebalanced **Ichor**: it drains health on contact unless protected by **Soul Absorption I**; **Soul Absorption II** grants Regeneration instead. Ichor Bottles and Snowballs also respond to Soul Absorption.
- Added **16 new advancements** for the expanded world, Monarch, Ichor, Sifter equipment and creature interactions.
- Added ten ambient tracks by **Lealeanz** and new creature audio, together with Polish updates and Russian/Brazilian Portuguese localizations.
- Updated Ancient City portal effects, wrong-note melody playback and red-beam feedback. Added the `spawn_rifts` gamerule.

### NeoForge features

- Keeps main portal return travel separate from paired Rift connections; protects the underground return height where a safe destination exists.
- Adapts all **Sculkers' Lair** structure pieces to Minecraft 26.2's NBT palette format for complete dungeon generation.
- Carries the new eight-layer tides through native rendering, with optional **Photon and Complementary** sky and portal integration, Sonorous beams and shader-aware cloud composition.
- Supports the matching optional **Easy Magic 26.2.0** integration, including Charoite enchantment costs, item use and rerolls.
- Preserves original worldgen, creatures, networking, fluids, advancements, loot and target-specific NeoForge block interactions.

**Requirements:** Minecraft **26.2**; **Java 25**; **NeoForge 26.2.0.90 or newer** (same Minecraft line; no upper bound); **GeckoLib 5.5.6 or compatible 5.x**. Optional Easy Magic, Puzzles Lib and shader dependencies must match Minecraft 26.2.

[Port 2.0.1 — Minecraft 26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v2.0.1-mc26.2-neoforge)

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
