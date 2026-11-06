# Mielon's The Sift — NeoForge (Unofficial Port)

An unofficial NeoForge port of **Mielon's The Sift** for **Minecraft 26.1.2, 26.2 and 26.3**. The original dimension, mobs, blocks, items, world generation, progression, music and `the_sift` mod ID are preserved.

**Original mod:** Mielon (MIT) · **NeoForge port:** MikiKox1 · **Source snapshot:** [Arta48/Mielons-The-Sift](https://github.com/Arta48/Mielons-The-Sift)

## Downloads and requirements

| Minecraft | Current port | NeoForge minimum | Required GeckoLib | Optional Easy Magic | Download |
| --- | --- | --- | --- | --- | --- |
| **26.1.2** | **1.1.2.1** | 26.1.2.100 | 5.5.2 (compatible 5.x) | 26.1.0 | [Port 1.1.2.1 — MC 26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2.1-mc26.1.2-neoforge) |
| **26.2** | **1.1.2.1** | 26.2.0.90 | 5.5.6 (compatible 5.x) | 26.2.0 | [Port 1.1.2.1 — MC 26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2.1-mc26.2-neoforge) |
| **26.3** | **1.1.2.1** | 26.3.0.64-beta | 5.5.7 (compatible 5.x) | 26.3.0 | [Port 1.1.2.1 — MC 26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/releases/tag/v1.1.2.1-mc26.3-neoforge) |

**Java:** 25. **Loader:** NeoForge. **Required:** GeckoLib for the matching Minecraft version. Each Minecraft version has its own source branch ([26.1.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/tree/minecraft/26.1.2) · [26.2](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/tree/minecraft/26.2) · [26.3](https://github.com/MikiKox1/mielons-the-sift-neoforge-port/tree/minecraft/26.3)) and a separate JAR. Do not install both builds or combine this port with the original Fabric mod.

**Release history:** [Minecraft 26.1.2](CHANGELOG-26.1.2.md) · [Minecraft 26.2](CHANGELOG-26.2.md) · [Minecraft 26.3](CHANGELOG-26.3.md). Each changelog shows the newest version first.

## The Sift dimension

The Sift contains **10 biomes**, including overgrown forests, Soul Canyons, Sniffer Caves and Ichor-Snowy Peaks. The dimension features Siftslate terrain, sculk formations, Ichor lakes and springs, monoliths, cliff arches, abandoned portals and an animated day/night sky.

- Overworld and Sift coordinates use a **1:1 scale**.
- Beds are usable inside the dimension.
- Rain includes Ichor-colored particles, while snowy peaks accumulate Ichor Snow.
- Temporary **Rifts** connect The Sift to the Overworld. Rifts remain open for five minutes and transport players, creatures, vehicles, projectiles and items.

## Opening the Ancient City portal

An Ancient City portal uses eight Sonorous Deepslate blocks and Note Blocks:

1. Play a **Goat Horn** in the center of an Ancient City.
2. A **Singer** awakens Sonorous Deepslate with its song.
3. Place Note Blocks on the awakened Sonorous Deepslate.
4. Use right-click to select notes **1–8** and left-click to play them in the exact order:

**1 → 3 → 7 → 6 → 5 → 2 → 4 → 8**

The correct melody activates the beams and opens the Reinforced Deepslate portal.

## Creatures

- **Singer:** Connected to the Ancient City portal and Soul Blocks; optional dynamic lighting with LambDynamicLights.
- **Echo Golem:** Collects Soul Blocks in Soul Canyons. An Echo Shard bonds the golem to a player, changing the delivery destination.
- **Blub:** A small neutral creature found in overgrown biomes. Can be tamed with a Soul Block, follow, sit and defend its owner.
- **Sifter:** A fast predator, found alone or in packs, that drops Raw Sifter Meat.
- **Dark Sniffer:** A hostile sculk-infected Sniffer. Its **Sonic Sniff** attack pulls targets closer; the attack uses one hit per target per wave.

## Ichor and equipment

**Ichor** is a restorative fluid that grants Regeneration I on contact and slightly slows movement. It can be collected in buckets and bottles, poured into cauldrons and transferred into cauldrons through pointed dripstone. Ichor Bottles, Ichor Snow, Ichor Snow Blocks and Ichor Snowballs are included.

**Siftite** upgrades Netherite equipment using a Siftite Ingot and Siftite Upgrade Smithing Template. It provides weapons, tools and a full armor set, with increased durability and mining speed. Lost Siftite equipment can return after respawning through a Mini Rift.

**Charoite** is an alternative enchanting material. It raises applicable enchantment levels and adds 20 experience levels to non-zero enchantment costs. Charoite-enchanted items keep the original anvil combination restrictions.

## Blocks, structures and progression

- A complete **Overgrown Willow** wood family, including logs, planks, slabs, stairs, doors, fences, signs, boats and foliage.
- Sift plants and sculk variants, including Sculkflower, Whisperbloom, Sunburst Plant and Overgrown Lotus.
- Siftslate variants of Coal, Diamond, Emerald, Charoite and Siftite ores.
- Abandoned portals with loot such as rare Siftite Nuggets, upgrade templates and the **Rift music disc**.
- Dedicated advancement progression, ambient sounds, music and eight Sonorous Deepslate notes.

## NeoForge gameplay and visual compatibility

The port includes adapted block and entity registration, world generation, structures, networking, fluids and client rendering.

- **Ichor:** Correct fluid flow, pickup, buckets, waterlogging, Regeneration I refresh and cauldron interactions.
- **Portals and Rifts:** Ancient City portal activation and safe return travel.
- **Dark Sniffer:** UUID-based Sonic Sniff target tracking and saved player movement handling.
- **Weather:** Correct Ichor rain and snow rendering in their respective biomes.
- **Visuals and sounds:** Singer lighting, Sonorous beams, water-sensitive plants, Echo Golem audio and the six-layer animated Sift sky.
- **Photon and Complementary Shaders:** Smooth sky gradients and depth-masked composition. The native sky remains available without shaders.

## Optional integrations

**Easy Magic:** Charoite works with the matching Easy Magic version while keeping ordinary Lapis Lazuli enchanting available. Easy Magic's reroll functionality remains supported. **Puzzles Lib is required by Easy Magic itself**, not directly by The Sift.

**LambDynamicLights:** Optional Singer and related entity lighting. **Iris / compatible Sodium:** Optional shader rendering. The mod does not bundle shaderpacks or renderer replacements.

## License and credits

**Original creator:** Mielon — © 2026, [MIT License](LICENSE). **NeoForge port maintainer:** MikiKox1.

**Additional original credits:** ImpSteve (Dark Sniffer texture), Fuzja Jądrowa (Rift music), StrawberrySushi_ (Korean localization), Echo_Dev (Wellspring remix).

See [SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md) for original work and asset credits, and [AI_DISCLOSURE.md](AI_DISCLOSURE.md) for the port's AI-assistance disclosure.
