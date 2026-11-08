# Port 2.0.1 — Minecraft 26.3

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


## Installation

Install the matching Minecraft/NeoForge version, Java 25 and required GeckoLib 5.x. Place only the JAR for your exact Minecraft version in the mods folder; remove earlier The Sift JARs. Optional Easy Magic, Puzzles Lib and shader integrations must match the same Minecraft version. Back up existing worlds before upgrading from 1.x.

## Credits and license

Original mod: **Mielon** (© 2026, MIT License). Unofficial AI-assisted NeoForge port: **MikiKox1**. The original mod and contributor credits are retained, including Lealeanz, Frumptle, Grelinho, ImpSteve, Fuzja Jądrowa, StrawberrySushi_ and Echo_Dev. See README.md, SOURCE_PROVENANCE.md, AI_DISCLOSURE.md and LICENSE in the source repository.