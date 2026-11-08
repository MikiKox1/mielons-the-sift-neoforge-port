# Changelog — Minecraft 26.1.2 (NeoForge)

Released versions are listed **newest first**.

## Port 2.0.1 — Minecraft 26.1.2

The Sift 2.0.1 is a major content update to the existing unofficial NeoForge port. This build brings the original 2.0.1 gameplay to Minecraft 26.1.2, together with version-specific NeoForge compatibility work.

### World, structures and exploration

- Reworked world generation with **11 biomes**, including Coral Meadow, Coral Forest, Organic Desert, Corrupted Sift and three underground environments.
- New terrain details, large rib fossils, cave entrances, Ichor terraces, canyons and revamped biome-specific Abandoned Portals.
- Added **Abandoned Camps, Ichor Domes, Soulshale Shelters, Soulshale Towers** and the underground **Sculkers' Lair** dungeon.
- Explorer maps point towards camps and the Sculkers' Lair, which contains Royal Sculk and the Monarch Upgrade Smithing Template.
- Three daily tides — **FLOW, ENDURE and THRIVE** — alter The Sift's sky and atmosphere; beds work during Endure Tide or thunderstorms. Stalkers and Scavengers become an important threat during Endure.

### New boss, creatures and equipment

- **The Monarch** boss is summoned by completing a Royal Sculk totem with a Sculkflower. A Monarch Membrane and a smithing template can upgrade Elytra to **Monarch Elytra**, which gains altitude when the player presses jump while gliding.
- The former Sifter is replaced by **Seedling, Nester and Pollinator** variants with distinct combat behavior, including explosive Pollinator projectiles. **Sifter Hide** makes a **Sifter Cap** that prevents unprovoked attacks.
- Added **Slabbers and Nuzzles**, breedable using overgrown plants. Nuzzles support dyeing and shearing.
- Expanded **Blubs** with variants, **Blubby Berries** for taming and feeding, and the ability to climb on one another.
- Echo Golems can now be constructed with Polished Soulshale and a Carved Pumpkin.
- Added Soulshale decorative families, **Ash Willow** wood and vegetation, storage blocks for **Charoite and Siftite**, and Siftite Blocks/Ingot as Beacon components.

### Ichor, progression and ambience

- **Ichor now harms unprotected players**. **Soul Absorption I** on a chestplate prevents damage, while **Soul Absorption II** converts exposure into Regeneration; Ichor Bottles and Snowballs respond to the enchantment.
- Added **16 advancements** covering new structures, exploration, the Monarch, Sifter disguises, Ichor and Blub behavior.
- Added a **3×3 Monarch painting**, ten ambient tracks credited to Lealeanz, new creature audio, and expanded loot including Singer bartering.
- Updated Polish localization, with Russian and Brazilian Portuguese translations; the original Korean contributor credit remains.
- The Ancient City portal has updated visuals and audible/red-beam feedback for an incorrect note sequence. The `spawn_rifts` gamerule controls Rift spawning.

### NeoForge port features

- Preserves **main Ancient City portal ↔ main portal** and paired **Rift ↔ Rift** travel, with safer returns to underground portal locations.
- Adapts Sculkers' Lair structure templates to Minecraft 26.1.2 and retains all 11-biome progression conditions.
- Preserves target-correct advancement/loot predicates, including Sifter Cap equipment checks, and native Ash Willow sign materials and text editing.
- Supports an eight-layer animated Sift sky, tide transitions, portal and Sonorous effects, and optional Photon/Complementary integrations alongside native no-shader rendering.
- Keeps optional **Easy Magic 26.1.0 + Puzzles Lib** support for Charoite enchanting and Easy Magic rerolls.

**Requirements:** Minecraft **26.1.2**; **Java 25**; **NeoForge 26.1.2.100 or newer** (same Minecraft line; no upper bound); **GeckoLib 5.5.2 or compatible 5.x**. Easy Magic, Puzzles Lib, Iris, Sodium, Photon and Complementary are optional and must match the selected Minecraft version.

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
