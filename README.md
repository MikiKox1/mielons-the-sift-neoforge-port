# Mielon's The Sift 2.0.3 — NeoForge (Unofficial Port)

Explore **The Sift**, a mysterious dimension of Ichor, ancient life, resonant souls, strange creatures and shifting skies. This community-maintained NeoForge port brings **Mielon's The Sift 2.0.3** to Minecraft **26.1.2, 26.2 and 26.3**, with the original `the_sift` mod ID, gameplay and content.

**Original creator:** [Mielon](https://modrinth.com/mod/mielons-the-sift) · **Unofficial NeoForge port:** MikiKox1 · **License:** [MIT](LICENSE)

## Downloads and requirements

| Minecraft | Current build | Minimum NeoForge | Required GeckoLib | Optional Easy Magic | Download |
| --- | --- | --- | --- | --- | --- |
| **26.1.2** | **2.0.3** | 26.1.2.100 | 5.5.2 (compatible 5.x) | 26.1.0 | [Port 2.0.3 — MC 26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v2.0.3-mc26.1.2-neoforge) |
| **26.2** | **2.0.3** | 26.2.0.90 | 5.5.6 (compatible 5.x) | 26.2.0 | [Port 2.0.3 — MC 26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v2.0.3-mc26.2-neoforge) |
| **26.3** | **2.0.3** | 26.3.0.64-beta | 5.5.7 (compatible 5.x) | 26.3.0 | [Port 2.0.3 — MC 26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v2.0.3-mc26.3-neoforge) |

**Java 25** and a version-matched **NeoForge + GeckoLib** installation are required. NeoForge minimums have no artificial upper version limit within their respective Minecraft lines. **Official 2.0.3 releases:** use the JAR for your exact Minecraft version, and keep a backup of existing worlds before upgrading from 1.x. Never install two versions of The Sift together or combine this port with the original Fabric build.

All three Minecraft versions have independent [source branches for 26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/tree/minecraft/26.1.2), [26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/tree/minecraft/26.2) and [26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/tree/minecraft/26.3), plus dedicated [changelogs for 26.1.2](CHANGELOG-26.1.2.md), [26.2](CHANGELOG-26.2.md) and [26.3](CHANGELOG-26.3.md).

## What's new in The Sift 2.0?

This is a major content update compared with the earlier public **Port 1.1.2**.

- **A transformed dimension:** Explore **11 biomes** across the surface and underground, including Coral Meadow, Coral Forest, Organic Desert, Corrupted Sift and three distinct underground biomes. New cave entrances, Ichor terraces, canyons and giant rib fossils reshape exploration.
- **Three tides and an evolving sky:** The **FLOW, ENDURE and THRIVE** phases change The Sift throughout the day. Its sky uses eight animated layers. Sleep is possible during Endure Tide or a thunderstorm; beware of hostile Stalkers and Scavengers during Endure.
- **More places to discover:** Find **Abandoned Camps, Ichor Domes, Soulshale Shelters, Soulshale Towers**, biome-specific Abandoned Portals and the underground **Sculkers' Lair**. Explorer maps lead to camps and the lair.
- **The Monarch:** A new boss summoned at a special totem with a Sculkflower. Discover Royal Sculk and the Monarch Upgrade Smithing Template, then earn a **Monarch Membrane** and craft **Monarch Elytra** that can gain altitude while gliding.
- **New Sifters and wildlife:** Meet the **Seedling, Nester and Pollinator** Sifters, each with its own behavior. Craft a **Sifter Cap** to avoid provoking them. Breed **Slabbers and Nuzzles**; Nuzzles can be dyed and sheared. Blubs have new variants, eat Blubby Berries and can stack into towers.
- **Reworked Ichor:** Unprotected exposure now **drains health**. A chestplate with **Soul Absorption I** prevents the damage; **Soul Absorption II** converts it into Regeneration. The enchantment also affects Ichor Bottles and Ichor Snowballs.
- **More building materials:** Soulshale and its polished, brick, chiseled, stair, slab, wall and ore variants; the complete **Ash Willow** wood family; and new coral and sculk vegetation.
- **Equipment and rewards:** Storage blocks for Charoite and Siftite, Siftite-compatible Beacon bases and fuel, new loot, a **3×3 Monarch painting**, plus new progression and exploration advancements.
- **Atmosphere and localization:** Ten additional ambient music tracks by Lealeanz, new creature sound effects, and updated Polish, Russian and Brazilian Portuguese localization.
- **Portal and gameplay polish:** Animated feedback for an incorrect Ancient City melody, updated portal visuals, expanded Singer bartering and the `spawn_rifts` gamerule.

## Entering The Sift

The Ancient City portal is activated through a melody played on eight Note Blocks:

1. Play a **Goat Horn** in the center of an Ancient City. A **Singer** awakens eight Sonorous Deepslate blocks.
2. Place a **Note Block** on each awakened block and right-click to tune its eight possible notes.
3. Left-click the Note Blocks in this sequence, using notes numbered **0–7**:

   **0 → 2 → 6 → 5 → 4 → 1 → 3 → 7**

The blocks replay the correct melody and open the Reinforced Deepslate portal. An incorrect sequence triggers musical and red-beam feedback.

The Sift retains **1:1 Overworld coordinates**. Temporary **Rifts** stay open for five minutes and can transport players, mobs, mounts, boats, minecarts, items and projectiles. In this NeoForge port, main Ancient City portals and paired Rifts keep their own return routes, including underground Ancient City arrival where a safe destination exists.

## Creatures, the Sculkers' Lair and the Monarch

**Singers** awaken Ancient City portals and trade loot for Soul Blocks. **Echo Golems** can be befriended with Echo Shards or built from Polished Soulshale and a Carved Pumpkin. **Dark Sniffers**, Stalkers and Scavengers threaten corrupted areas; the Dark Sniffer's Sonic Sniff can pull victims closer.

**Seedling, Nester and Pollinator Sifters** have distinct attacks. Sifter Hide can be crafted into a Sifter Cap, allowing peaceful passage unless the player initiates combat. **Blubs** eat and can be tamed with Blubby Berries. **Slabbers and Nuzzles** are breedable, with Nuzzles supporting wool dyeing and shearing.

The **Sculkers' Lair** contains hostile guards, Royal Sculk and the Monarch Upgrade Smithing Template. A Monarch summoning totem uses Ichor, Soul Blocks, a Soulshale Totem and Royal Sculk; placing the final **Sculkflower** summons the boss. Its membrane can upgrade Elytra to **Monarch Elytra**. Press jump while gliding to rise.

## Ichor, Siftite and Charoite

- **Ichor** flows through the dimension, can be bottled or collected with buckets, and is used in snow, snowballs and cauldron interactions. **Soul Absorption** on a chestplate determines whether contact damages, protects or regenerates the wearer.
- **Siftite** upgrades Netherite tools, weapons and armor at a Smithing Table. Its equipment has its own performance and durability, while a **Mini Rift** can return lost Siftite gear after death.
- **Charoite** can power eligible enchantments beyond their ordinary levels for additional experience. The matching optional **Easy Magic** mod supports Charoite alongside Lapis Lazuli while retaining Easy Magic's own reroll behavior.
- **Siftite Blocks** work as Beacon pyramid material, and a **Siftite Ingot** can be used as Beacon payment.

## NeoForge features and optional shaders

The port adapts the original registries, entities, structures, world generation, fluids, crafting, advancement conditions, networking and client rendering to each target Minecraft version. Its version-specific implementations also include:

- Ancient City portal-to-portal returns and paired Rift-to-Rift travel with safer underground return handling.
- Sculkers' Lair structure data adapted to each target version's NBT format, including the older Minecraft 26.1.2 and 26.2 formats.
- Eight-layer Sift sky and tide colors, Sonorous beams, Ichor weather, portal effects and shader-aware rendering for **Photon** and **Complementary** through compatible Iris/Sodium versions.
- Target-specific NeoForge resource, sign, advancement and rendering adapters that preserve the original mechanics.

**Optional visual mods:** Iris and Sodium for your exact Minecraft/NeoForge version; Photon v1.4 and Complementary Unbound/Reimagined r5.9.3 are supported integration targets. On **MC 26.3**, the earlier compatible combination was **Iris 1.11.7 + Sodium 0.9.3-beta.1**. Optional shader mods, packs and renderer replacements are **not bundled** in the JAR. Native rendering works without them.

**Optional gameplay mod:** Easy Magic for the matching Minecraft version; Easy Magic's own Puzzles Lib dependency is required when Easy Magic is installed, but neither is necessary to run The Sift. Optional dynamic lighting may use a compatible LambDynamicLights release.

## Credits and source

**Mielon** created The Sift and its original design. This **unofficial, AI-assisted NeoForge port** is maintained by **MikiKox1**. Its 2.0.3 implementation follows Mielon's published 2.0.3 binaries and preserves the original mod's authorship and applicable attribution.

Original contributor and asset credits include **ImpSteve** (Dark Sniffer texture), **Fuzja Jądrowa** (Rift music disc), **Echo_Dev** (Wellspring music), **StrawberrySushi_** (Korean localization), **Lealeanz** (ambient music), **Frumptle** (Russian localization) and **Grelinho** (Brazilian Portuguese localization). The original mod also credits creature sounds from Minecraft Dungeons II and certain Soulshale sounds/textures from a Minecraft 26.4 snapshot.

Original mod: [Modrinth](https://modrinth.com/mod/mielons-the-sift) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/mielons-the-sift). Public earlier source snapshot: [Arta48/Mielons-The-Sift](https://github.com/Arta48/Mielons-The-Sift).

See [LICENSE](LICENSE), [SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md), [AI_DISCLOSURE.md](AI_DISCLOSURE.md), and the individual Minecraft changelogs for detailed attribution and release history.
