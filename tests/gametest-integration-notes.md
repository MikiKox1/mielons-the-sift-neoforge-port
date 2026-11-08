# The Sift 2.0.0 – NeoForge 26.2 server GameTests

Dev-only test sources: `src/test/java/mielon/thesift/gametest/SiftGameTestsMod.java` and `SiftServerGameTests.java`. The test mod ID is `the_sift_tests` (`@Mod`). Nothing in these sources belongs in the production mod JAR. There are **15 registered GameTests**. Registration and test code were compiled directly with Java 25 against the exact local MC 26.2.0.93/NeoForge 26.2.0.93 mapped JARs and the current port's compiled classes. **No GameTest server result was claimed by this worker.**

## Exact registration contract

Verified from `build/port-compile-classpath.txt` first and fourth JARs, using Oracle JDK 25 `javap`:

- `RegisterGameTestsEvent.registerEnvironment(Identifier, TestEnvironmentDefinition<?>) -> Holder<TestEnvironmentDefinition<?>>`
- `RegisterGameTestsEvent.registerTest(Identifier, GameTestInstance) -> void`
- `FunctionGameTestInstance(ResourceKey<Consumer<GameTestHelper>>, TestData<Holder<TestEnvironmentDefinition<?>>>)`
- `TestData<EnvironmentType>(EnvironmentType, Identifier structure, int maxTicks, int setupTicks, boolean required, Rotation, boolean manualOnly, int maxAttempts, int requiredSuccesses, boolean skyAccess, int padding)`
- `TestEnvironmentDefinition.SetDifficulty(Difficulty)` for NORMAL and PEACEFUL batches
- `GameTestHelper.getLevel()`, `absolutePos(BlockPos)`, `spawn(EntityType<E>, BlockPos, EntitySpawnReason)`, `assertTrue(boolean, String)`, `succeed()`

The mod registers `Consumer<GameTestHelper>` methods through `DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, "the_sift_tests")` and links their `Registries.TEST_FUNCTION` keys into `FunctionGameTestInstance`. It listens for `RegisterGameTestsEvent` on its own mod bus. Each `TestData` uses `minecraft:empty`, max 100 ticks, setup 0, required once, no rotation and padding 20. `minecraft:empty.nbt` is **present** in the target patched MC26.2 JAR (its template itself measures 1×1×1); tests build their isolated scenes in loaded disposable chunks at relative `(4,4,4)`, outside that tiny template. The padding keeps scenes separated, though test-world cleanup after assertions outside the template requires runtime verification.

Prime's separate test source set and run configuration should expose the dev mod only on `runGameTestServer`:

```groovy
neoForge {
    runs {
        gameTestServer {
            gameTestServer()
            sourceSet = sourceSets.test
            loadedMods = [mods.the_sift, mods.the_sift_tests]
            systemProperty 'neoforge.enabledGameTestNamespaces', 'the_sift_tests'
            gameDirectory = project.file('run/gametest-26.2.0.93-port-2.0.0')
        }
    }
    mods {
        the_sift_tests { sourceSet(sourceSets.test) }
    }
}
```

This is a wiring outline; Prime already owns the actual ModDevGradle configuration and test-only `src/test/resources/META-INF/neoforge.mods.toml`, which must contain `modId="the_sift_tests"`. Production client/server configurations load only `the_sift`; packaging remains bound to `sourceSets.main`. No JUnit or extra test dependency is required. The normal Gradle `test` task does not execute server GameTests. Prime runs `compileTestJava` and the disposable `runGameTestServer` after the production compile/server gate.

## Coverage and expected observed invariants

| Test name | Server assertions |
| --- | --- |
| `mob_registration_attributes` | 13 custom Mob registrations instantiate; max-health attributes exist; native Sniffer mixin sets infection ticks 37 then 0 and reports corresponding infection state |
| `utility_entities` | 11 projectile/rift/boat entity types register and construct as their own types |
| `monarch_echo` | Real Monarch Echo spawns with `MONARCH_ECHO` type, `isEcho=true`, phase 0 |
| `ritual_sources_consume` | Eight source Ichor blocks around four-high soul pedestal activate on flower placement; exactly one Monarch; structure and source fluids consumed; no second summon |
| `ritual_missing_source` | A missing source rejects ritual, preserves flower and produces no Monarch |
| `ritual_flowing_ichor` | A flowing Ichor block is rejected as a source |
| `ritual_peaceful` | Valid arrangement does not summon or consume blocks on PEACEFUL |
| `echo_golem_rejects_incorrect_base` | Pumpkin on stone does not create Echo Golem or consume pumpkin |
| `echo_golem_single_summon` | Carved pumpkin on polished soulshale triggers native mixin; both blocks consumed and exactly one actual Echo Golem created |
| `blubby_berries_bonemeal_harvest` | Double bush growth on bonemeal; interaction resets GROWN and drops 1–4 real berry items |
| `sculkflower_crop_bonemeal` | Crop AGE 1 on farmland grows into sculkflower after bonemeal |
| `ichor_soul_absorption` | Cow touches source Ichor and receives damage from drinking at Soul Absorption 0; chest-armor Soul Absorption II on a living Pillager confers regeneration (using undead would yield a false failure) |
| `willow_native_strippables` | All four ash/overgrown willow log/wood mappings work via native `AxeItem.getAxeStrippingState`, preserving axis Z |
| `native_compostables` | Every one of the 21 exact upstream items has its expected probability (0.3/0.5/0.65) after datapack load via `NeoForgeDataMaps.COMPOSTABLES` |
| `furnace_fuel_events` | All 38 mod wood items return the corresponding vanilla fuel duration from a real `FurnaceFuelBurnTimeEvent` dispatched through `NeoForge.EVENT_BUS` |

All tests use real mod registries, blocks, entities and vanilla server objects. A failure must be reported by case name; it does not establish general mod failure until logs identify whether production or test arrangement caused the assertion. Server GameTests are bounded regression cases, not completion of the 673-case manual checklist. A passing test runner is also not a save/reload, player-driven ritual, dedicated multiplayer or visual test. Persistence, client rendering and migration gates remain separate.

## Verification record

- API: `javap` of target mapped `minecraft-patched-26.2.0.93.jar`, NeoForge `26.2.0.93-universal.jar` and FML 11.0.16 loader, including `TestData`, `RegisterGameTestsEvent`, `GameTestHelper`, `AxeItem`, `NeoForgeDataMaps`, `FurnaceFuelBurnTimeEvent`.
- Java sources: `javac -cp <build/classes/java/main plus 90 lines from build/port-compile-classpath.txt> -sourcepath '' .../SiftGameTestsMod.java .../SiftServerGameTests.java` -> **exit 0** on Java 25.0.4.1. Bytecode output went to the allocated worker worktree outside production artifacts. `-Xlint:deprecation` identifies two target-native deprecations (`Item.builtInRegistryHolder()` and `FuelValues.burnDuration(ItemStack)`), both exercised for parity.
- Prime's first Gradle `compileTestJava` **PASS**. First `runGameTestServer` **blocked before test discovery** with `ResolutionException: Modules the_sift_tests and the_sift export package mielon.thesift.worldgen` because the separately owned pure-unit `src/test/java/mielon/thesift/worldgen/PortCoreContractTest.java` is loaded in the test module. Prime is separating that pure test from the runtime GameTest source set. No GameTest scenario has yet passed, and NBT save/reload is untested.
