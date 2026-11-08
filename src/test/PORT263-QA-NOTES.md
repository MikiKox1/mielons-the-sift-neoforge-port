# The Sift 2.0.1 — native MC 26.3 server QA handoff

**Scope:** test sources/resources under `src/test/**` and `src/coreTest/**` only. Java 25; mapped Minecraft 26.3/NeoForge 26.3.0.101. Prime owns Gradle, game/server startup, the full-world routing probes and all production code.

## Test inventory and target adaptations

- The 26.2 accepted baseline has **34 regular custom GameTests** plus the vanilla `minecraft:always_pass` control, yielding the previously recorded **35 GameTests**. Their 15 base gameplay, 8 official 2.0.1 regressions, and 11 T11 safe-routing cases remain registered with their existing test IDs and strict assertions.
- The accepted `src/coreTest/java/mielon/thesift/worldgen/PortCoreContractTest.java` remains unchanged. It still exercises the **180,481 accepted assertions** for tides, transitions, maps, Soulshale IDs and position-set behavior. Java 25 `javac` against the target Minecraft 26.3.0.101 JAR plus the frozen 26.2 main classes exited 0. Prime must execute the actual core tests against newly compiled 26.3 main classes before claiming 180,481 PASS.
- `SiftServerGameTests` now checks four real 26.3 axe-on-block transformations, preserving each log/wood axis. MC26.3 removed `AxeItem.getAxeStrippingState` and uses the target `BLOCK_TRANSFORMER` mechanism; the test performs an actual `ItemStack.useOn(UseOnContext)` with a mock server player.
- The compost regression checks **all 22 official target registrations**, including the added `ash_willow_sapling`, using `Item.components().get(DataComponents.COMPOSTABLE)` and the exact `ResolvableInt.Constant` layer values **30, 50 or 65**. NeoForge's old `COMPOSTABLES` data map does not exist in 26.3. Values are integers in the target, so the test checks exact equality rather than widening tolerances.
- The legacy `furnace_fuel_events` case retains its registration name but now checks that **all 38** Sift wood items have the **same entire native `DataComponents.COOKING_FUEL`** as their vanilla counterparts. Target 26.3 no longer has `FurnaceFuelBurnTimeEvent` or `FuelValues`.
- The berry and sculkflower bonemeal regression calls the target five-parameter methods with `BonemealSource.INTERACTION`. Common production code must implement the new MC26.3 `BonemealableBlock` signature; the test doesn't weaken either growth/harvest assertion.

## Optional Easy Magic — actual server menu

The test-only `SiftEasyMagicRuntimeGameTests.java` is adapted from the accepted, unchanged 26.2 QA reference:

`../neo-26.2-2.0.1-easymagic-runtime-qa/src/test/java/mielon/thesift/gametest/SiftEasyMagicRuntimeGameTests.java`

Registered **unconditionally** as `the_sift_tests:easymagic_actual_menu_contract`. At execution it uses `ModList.get().isLoaded("easymagic")`: without Easy Magic it constructs vanilla `EnchantmentMenu`; with Easy Magic it uses test-only reflection to construct the **actual** `fuzs.easymagic.common.world.inventory.ModEnchantmentMenu`. The direct class constructor signature `(int, Inventory, Container, ContainerLevelAccess)` and public `clues` list were verified with `javap` against the pinned **Easy Magic v26.3.0** NeoForge JAR:

`../../reference/optional-mc26.3-20261108/easymagic-v26.3.0-mc26.3.x+neoforge.jar`

Neither the production nor test compilation takes a static class dependency on Easy Magic. The test places a real enchanting table with bookshelves, accepts Lapis and Charoite, requires every nonzero Charoite offer to be **baseline +20**, checks actual reroll + two recalculations for **no cumulative +20**, verifies the three Easy Magic `clues` entries, checks a single reroll catalyst is consumed, and performs an actual enchant requiring **exactly one catalyst and one XP level** for button 0 while marking the enchanted output via `CharoiteEnchanting.wasUsed`. Prime should execute the same test with no optional mod and with Easy Magic v26.3.0 plus its exact Puzzles Lib v26.3.17 dependency. A compile PASS never substitutes for both runtime modes.

## New target NBT parser gates

Two native test IDs were added, always registered:

- `the_sift_tests:target263_nbt_legacy4903`: load the official `the_sift:abandoned_portal_coral_forest_1` (source DataVersion **4903**, old `Name` palette keys) through `ServerLevel.getStructureTemplateManager().get`, save the decoded in-memory template and require the `the_sift:soulshale` block ID, positive dimensions and nonempty block placements.
- `the_sift_tests:target263_nbt_schema5023`: load the official `the_sift:ichor_dome/organic_desert_2` (source DataVersion **5023**, new `id` keys; includes `the_sift:chiseled_soulshale`) and a new 5023 Lair piece `the_sift:sculkers_lair/parts/first_connector_1` (includes `the_sift:soulshale_stairs`) through that same **real target-version template parser**. Assert all named blocks survive registry resolution. Structure templates are decoded but never placed into test worlds.

The official 26.3 resource set intentionally includes mixed 4903/5023 files; do **not** normalize old NBT or assume all templates must be 5023. NeoForge/Minecraft MC26.3 `NbtUtils.readBlockState` consumes the lowercase `id` after any required data-fix, so checking the re-serialized native template catches silent fallback to AIR. Prime's separate resource verification covers all 79 Lair palette IDs; these three real-loader cases are bounded smoke gates, not a substitute for the 79-file scan.

The default GameTest registration is now **38 custom + 1 vanilla control = 39 tests** (38/38 was the already accepted prior run; the new biome gate awaits Prime's separate run). Seven pre-existing optional T11 scenarios that require a real Sift dimension and/or server restart remain in the source for a separate full-server harness. **Never set `the_sift.tests.crossdimension=true` in `GameTestServer`**; Prime runs actual cross-dimension and persistence QA on its standalone full server.

## Additional 26.3 natural-spawn warning gate (39th test)

- Native ID: `the_sift_tests:mc263_all_sift_biomes_preserve_natural_spawns`; class: `Sift263BiomeSpawnGameTests`. One required test, registered in the existing separate test mod.
- Immutable expectations: `src/test/resources/the_sift_tests/fixtures/official-263-natural-mob-spawns.json`. Copied **only from the 11 `data/the_sift/worldgen/biome/*.json` entries within the pinned official Fabric26.3 JAR** `../../reference/upstream-2.0.1-20261107/mielon's-the-sift-2.0.1-mc26.3-fabric.jar`, SHA-256 `B1DD0FAAE623797418C42197C4B66FFE2E96E08CABD1D9329EC5661C7C6195ED`. For each original biome, the frozen fixture contains the unmodified `attributes["minecraft:gameplay/natural_mob_spawns"].spawns_by_category` and `.spawn_costs`; never generated from the port's current data. A read-only second extraction compared every frozen JSON subtree against the JAR and confirmed exact equality for 11/11 biomes.
- The native test resolves **all 11** biomes using the running level's **actual biome registry**, then reads `Biome.getModifiedEnvironmentAttributes()`, requires the original `AttributeModifier.override()` entry, and evaluates `map.applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY)`. It checks each native weighted spawn in source order against the frozen entity registry ID, weight, uniform provider type, and exact min/max group size, including all empty categories. Every post-modifier spawn cost is compared against the frozen entity ID, exact charge and energy budget. Totals are mandatory: **53 creature + 23 monster = 76 spawns, plus 28 spawn costs**; corrupted_sift explicitly retains **0 creature + 3 monster**.
- Context: `../../reports/research/port-26.3-2.0.1-20261108/REVIEW-BIOME-SPAWN-WARNING.md`. The 11 NeoForge warnings arise because its legacy mutable spawn-settings builder cannot overwrite the original `OverrideModifier[]` attribute; this test verifies that the source attribute **actually survives** the post-modifier registry. A PASS does not prove mob placement predicates, natural spawn probabilities, or external add_spawns compatibility (the latter still has a distinct limitation).
- Worker-8 made no Gradle, game, client/server or production changes for this follow-up. Java API signatures were checked via exact MC26.3.0.101 bytecode only. **Prime must run `compileTestJava` and the 39-test native suite** in the isolated gameTestServer; do not claim this new case PASS based solely on the already accepted 38/38.

## Local verification and remaining gates

- Exact 26.3 mapped JAR source and bytecode inspected: `GameTestHelper`, `RegisterGameTestsEvent`, `TestData`, structure template manager, `Compostable`, `CookingFuel`, bonemeal method signatures and `EnchantmentMenu`. Registration constructor signatures are unchanged from 26.2.
- `javac --release 25` **PASS** for the independent structure parser GameTest using the mapped 26.3 Minecraft JAR, NeoForge 26.3.0.101 and required libraries (isolated output in worker-8's allocated worktree).
- `javac --release 25` **PASS** for the unchanged core contract using the frozen 26.2 main bytecode with target MC26.3 APIs, and separately **PASS** for the existing 2.0.1/T11 routing/persistence GameTests, the copied Easy Magic real-menu GameTest and the new NBT GameTests. These are target API checks only, not full 26.3 port builds.
- The port's production `src/main/java/**` is concurrently being adapted by other workers. Prime will run `compileTestJava` against the final native 26.3 production classes, then `runGameTestServer` with and without optional Easy Magic. The berry methods must be updated to the five-argument `BonemealSource` API in main before that compile can succeed.
- No Gradle, GameTest server, full Minecraft server/client, profile or player world was started by worker-8. No source outside this worker's ownership was changed.
