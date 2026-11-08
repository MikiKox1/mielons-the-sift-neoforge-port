# Changelog — Minecraft 26.1.2 (NeoForge)

Released versions are listed **newest first**.

## Port 2.0.1 — Minecraft 26.1.2

A major update to the unofficial NeoForge port of Mielon's The Sift, bringing the new 2.0.1 content and gameplay to Minecraft 26.1.2.

### Changes

- **World and biomes:** Expanded The Sift to **11 biomes**, including Coral Meadow, Coral Forest, Organic Desert, Corrupted Sift and three underground cave biomes, with reworked caves, Ichor terraces, canyons and giant rib fossils.
- **Tides and sky:** Added the **FLOW, ENDURE and THRIVE** phases with an animated eight-layer sky, changing weather and sleeping during Endure Tide or thunderstorms.
- **Exploration:** Added **Abandoned Camps, Ichor Domes, Soulshale Shelters, Soulshale Towers**, biome-specific Abandoned Portals and the underground **Sculkers' Lair**, plus four exploration maps.
- **The Monarch:** Introduced the Monarch boss and its summoning ritual, Royal Sculk, Monarch Membrane and the Monarch Upgrade Smithing Template. Upgrade Elytra into **Monarch Elytra**, with upward movement while gliding.
- **Sifters:** Introduced **Seedling, Nester and Pollinator** with distinct attacks, plus **Sifter Hide** and a **Sifter Cap** that prevents unprovoked Sifter attacks.
- **Creatures:** Added Stalkers, Scavengers, breedable **Slabbers and Nuzzles**, dyeable and shearable Nuzzle wool, and new creature sounds.
- **Blubs and Echo Golems:** Added Blub variants, **Blubby Berries**, stackable Blub towers and player-built Echo Golems made from Polished Soulshale and a Carved Pumpkin.
- **Ichor and enchantments:** Reworked Ichor to damage unprotected players. **Soul Absorption I** prevents damage; **Soul Absorption II** grants Regeneration. Ichor Bottles and Ichor Snowballs also interact with the enchantment.
- **Blocks and equipment:** Added the Soulshale block family and ores, the full **Ash Willow** wood set, coral and sculk vegetation, Charoite/Siftite storage blocks and **Siftite Beacon** support.
- **Progression and atmosphere:** Added **16 advancements**, a 3×3 Monarch painting, ten ambient tracks by **Lealeanz**, updated loot and Singer bartering, and Polish, Russian and Brazilian Portuguese localization.
- **Ancient City portal:** Updated portal visuals and wrong-note melody feedback; improved overlapping note attempts and prevented unloaded-chunk access during portal checks.
- **Rifts and upstream fixes:** Added the enabled-by-default **the_sift:spawn_rifts** gamerule to control natural Rift spawning, and corrected Ichorlogged initialization and an Ichor Dome structure palette.
- **NeoForge portal travel:** Preserved separate routes for main Ancient City portals and paired Rifts, including safer underground return locations.
- **World and data compatibility:** Adapted the full Sculkers' Lair and original biome, loot, advancement, entity and item behavior to the target Minecraft/NeoForge APIs.
- **Visual compatibility:** Preserved the eight-layer Sift sky, Sonorous beams, portals and Ichor weather, with optional **Photon / Complementary** integrations via Iris/Sodium and native rendering without shaders.
- **Optional integrations:** Retained Charoite enchanting and rerolls with matching **Easy Magic + Puzzles Lib**, plus optional LambDynamicLights and compatible shader mods.

### Requirements

- **Minecraft:** 26.1.2 (exact version)
- **NeoForge:** matching Minecraft 26.1.2 loader; see [README.md](README.md) for the verified minimum.
- **Java:** 25
- **GeckoLib:** matching Minecraft 26.1.2 version, compatible 5.x (required).
- **Optional:** Easy Magic (with Puzzles Lib), LambDynamicLights, Iris/Sodium, Photon and Complementary; use releases for the same Minecraft version.

### Credits

**Original mod:** Mielon (MIT License). **Unofficial NeoForge port:** MikiKox1 (AI-assisted). Original contributor and asset credits are preserved in [SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md).

[Port 2.0.1 — Minecraft 26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v2.0.1-mc26.1.2-neoforge)

## Port 1.1.2.1 — Minecraft 26.1.2

- Fixed the Sift portal rendering.

[Port 1.1.2.1 — Minecraft 26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2.1-mc26.1.2-neoforge)

## Port 1.1.2 — Minecraft 26.1.2

The first unofficial NeoForge edition of Mielon's The Sift 1.1.2 for Minecraft 26.1.2.

- **Dimension and world:** Ten Sift biomes, terrain, caves, original structures, blocks, items, creatures and progression, adapted to Minecraft 26.1.2.
- **Portals and Rifts:** Ancient City portal activation, Rift travel and return routes, networking, menus and GeckoLib models and animations.
- **Ichor:** Regeneration I on contact with flowing fluid, fluid-containing blocks and full Ichor cauldrons, without replacing stronger effects.
- **Creatures:** Dark Sniffer Sonic Sniff affects each target once per wave, with player movement retained across save/rejoin. Singer, Echo Golem, Blub and Sifter are included.
- **Weather and sky:** Ichor rain and snow, including Ichor-Snowy Peaks, and an animated six-layer sky using smooth RGBA gradients.
- **Shaders and visuals:** Photon depth-masked sky rendering after Iris compositing, Complementary compatibility, Sonorous beams, portals, Rifts and fluid effects.
- **Optional integrations:** Easy Magic 26.1.0 with Puzzles Lib support for Charoite enchanting, LambDynamicLights 4.11.2 and compatible Iris/Sodium rendering.

[Port 1.1.2 — Minecraft 26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2-neoforge.1-mc26.1.2)

**Original mod:** Mielon (MIT). **Unofficial NeoForge port:** MikiKox1. See [README.md](README.md) for dependencies and [SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md) for credits.
