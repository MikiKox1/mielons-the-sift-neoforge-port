# Changelog — Minecraft 26.3 (NeoForge)

Released versions are listed **newest first**.

## Port 2.0.1 — Minecraft 26.3

A major content update for the unofficial NeoForge port of The Sift, bringing Mielon's 2.0.1 world, progression and creatures to Minecraft 26.3.

### New dimension content

- **11 biomes**, including Coral Meadow, Coral Forest, Organic Desert, Corrupted Sift and three underground biomes, with redesigned terrain, Ichor formations, cave entrances and giant Organic Desert fossils.
- New **Abandoned Camps, Ichor Domes, Soulshale Shelters, Soulshale Towers**, biome-specific Abandoned Portals and the **Sculkers' Lair** dungeon.
- Explorer maps lead to camps and the buried lair; the lair holds Royal Sculk and the Monarch Upgrade Smithing Template.
- Three tide phases — **FLOW, ENDURE and THRIVE** — alter the eight-layer sky. Beds work during Endure Tide or thunderstorms; Stalkers and Scavengers threaten those conditions.

### The Monarch and new inhabitants

- New boss **The Monarch**, summoned by completing a Royal Sculk totem with a Sculkflower. Defeating it grants a **Monarch Membrane** for upgrading Elytra into **Monarch Elytra**, which can gain altitude while gliding.
- **Seedling, Nester and Pollinator** replace the former single Sifter type. **Sifter Hide** and the **Sifter Cap** enable a non-aggressive disguise unless the player attacks.
- New **Slabbers, Nuzzles and Blub variants**, plant-based breeding, Nuzzle dyeing/shearing, and **Blubby Berries** for taming, feeding and stacking Blubs.
- Player-built **Echo Golems** use Polished Soulshale and a Carved Pumpkin.
- Added the full **Ash Willow** wood set, **Soulshale** polished/brick/chiseled families and building shapes, coral/sculk plants and storage blocks for Charoite and Siftite.
- Siftite can power a **Beacon** using Siftite Blocks and Ingots. Added a 3×3 Monarch painting and expanded exploration/barter loot.

### Combat, Ichor and atmosphere

- **Ichor** now drains health unless the player equips a **Soul Absorption I** chestplate. **Soul Absorption II** changes exposure into Regeneration and also affects Ichor Bottles/Snowballs.
- Added **16 advancements** for 2.0 content, including the Monarch, new structures, disguises, Blub towers and Ichor.
- Added ten ambient tracks by **Lealeanz**, new creature sounds, and Polish, Russian and Brazilian Portuguese localization updates.
- Updated Ancient City portal visuals, incorrect-melody feedback and expanded Singer bartering. The `spawn_rifts` gamerule controls natural Rift spawning.

### NeoForge 26.3 implementation

- Uses the **native Minecraft 26.3 RenderPearl rendering pipeline** for portals, Rifts, Sonorous effects, Ichor weather and the eight-layer Sift sky.
- Preserves safe **Ancient City portal return** and dedicated **Rift pair** routing, including returns to underground portal locations.
- Supports optional **Photon and Complementary** composition, tide colors and sky, with Iris/Sodium combinations matching Minecraft 26.3. Native no-shader rendering remains available.
- Keeps 26.3-specific worldgen, structure data, advancement conditions, item components, recipes, entities, menu and fuel behavior.
- Includes optional **Easy Magic 26.3.0** support for Charoite enchanting and rerolls without bundling Easy Magic or Puzzles Lib.

**Requirements:** Minecraft **26.3**; **Java 25**; **NeoForge 26.3.0.64-beta or newer** (same Minecraft line; no upper bound); **GeckoLib 5.5.7 or compatible 5.x**. Optional Easy Magic, Puzzles Lib and shader dependencies must match Minecraft 26.3.

[Port 2.0.1 — Minecraft 26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v2.0.1-mc26.3-neoforge)

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
