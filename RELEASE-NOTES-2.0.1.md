# Port 2.0.1 — Minecraft 26.1.2

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


## Installation

Install the matching Minecraft/NeoForge version, Java 25 and required GeckoLib 5.x. Place only the JAR for your exact Minecraft version in the mods folder; remove earlier The Sift JARs. Optional Easy Magic, Puzzles Lib and shader integrations must match the same Minecraft version. Back up existing worlds before upgrading from 1.x.

## Credits and license

Original mod: **Mielon** (© 2026, MIT License). Unofficial AI-assisted NeoForge port: **MikiKox1**. The original mod and contributor credits are retained, including Lealeanz, Frumptle, Grelinho, ImpSteve, Fuzja Jądrowa, StrawberrySushi_ and Echo_Dev. See README.md, SOURCE_PROVENANCE.md, AI_DISCLOSURE.md and LICENSE in the source repository.