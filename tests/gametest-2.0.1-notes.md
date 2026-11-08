# NeoForge MC 26.2 — official The Sift 2.0.1 server regression GameTests

## Purpose and scope

The separate development-only mod `the_sift_tests` now registers **8 additional server cases** from `src/test/java/mielon/thesift/gametest/Sift201RegressionGameTests.java` in `SiftGameTestsMod.java`. Previous 15 cases are unchanged (vanilla contributes its `always_pass` control, so the expected runner total is 24). The entire 2.0.1 test code can compile against the former 2.0.0 production API: game-rule references use the **native registry** `BuiltInRegistries.GAME_RULE`, and the native `GameRules` getters/setters/commands; the private `NEXT_SPAWN` map is read via **test-only** `Field#setAccessible`, with no new production hooks or dependencies.

Official source evidence, preserved by Prime:

- `../../reports/research/upstream-2.0.1-20261107/source-diffs/RiftManager.java.diff`: default-true gamerule `the_sift:spawn_rifts`, SPAWNING category, `tickNaturalSpawns` gate, timer clear on false, existing Rift processing still active.
- `.../source-diffs/SonorousConsoles.java.diff`: buffers remove only when `matches(attempt, sequence)` is true; the original still returns `Optional.of` even when the eight notes are wrong. Thus **assert only that a shifted 9th note yields the exact expected valid sequence**, not that an invalid eighth returns `Optional.empty`.
- `.../source-diffs/IchorBlockFluidStateMixin.java.diff`: constructor hook must skip Ichor property access when either arrays are null, set fluid-kind cache to non-Ichor, and retain luminous Ichorlogged states for normal non-null properties.
- Target runtime interfaces were inspected from the exact 26.2.0.93 mapped MC patched JAR, not copied from 26.3: `GameRules.registerBoolean(String, GameRuleCategory, boolean)`, `GameRules.get(GameRule<T>)`, `net.minecraft.commands.Commands.performPrefixedCommand(CommandSourceStack,String)`, `MinecraftServer.createCommandSourceStack()`, and `BlockState(Block, Property<?>[], Comparable<?>[])`.

## Added cases

| ID (`the_sift_tests:`) | Check |
| --- | --- |
| `spawn_rifts_rule_registration` | Data-registry ID, Boolean type, SPAWNING category, `defaultValue=true`, world default true |
| `spawn_rifts_toggle_timer` | Actual `gamerule the_sift:spawn_rifts false` and `true` server commands; false clears a seeded pending `NEXT_SPAWN` timer when `RiftManager.tick(server)` runs; true no longer clears the timer |
| `spawn_rifts_preserves_manual` | Real `ModEntities.RIFT` creates/configures/adds to server, then survives `RiftManager.tick` with gamerule disabled |
| `sonorous_invalid_opening_shift` | `SonorousDeepslateBlock.Mode.NOTE` under a placed vanilla note block; `[1,1,3,7,6,5,2,4]` followed by `8` must produce exact `[1,3,7,6,5,2,4,8]` OPENING match |
| `sonorous_invalid_closing_shift` | Symmetric `[8,8,4,2,5,6,7,3]` followed by `1` must produce exact `[8,4,2,5,6,7,3,1]` CLOSING match |
| `sonorous_valid_match_clears` | Genuine opening match is recognized, then its buffer is cleared so the next seven suffix notes do not produce a spurious event |
| `ichor_null_blockstate_constructor` | Constructs actual native `new BlockState(Blocks.AIR, null, null)` and expects no mixin crash, unchanged empty fluid and zero light |
| `ichorlogged_stairs_light` | Native waterloggable oak stair Ichor state preserves Ichor fluid and emitted light level ≥12 |

Test scenes use only disposable server Level objects. Rule-mutating tests save/restore the actual server game-rule value in `finally`. Sonorous buffers are reset in `finally`. The manual Rift is discarded in `finally`. Timer probes seed a synthetic **UUID map entry only** so that the test can verify clearing and retaining `NEXT_SPAWN` without inventing a real player; scheduling an actual player-specific random Rift remains a separate live-server test with a real player in The Sift dimension. A fresh world is required to assert the initial gamerule is true.

## Compiler and run responsibilities

- Java 25.0.4.1 `javac` against the current built W/main classes + all exact `build/port-compile-classpath.txt` dependencies: **PASS, exit 0** (all three files: old server tests, new 2.0.1 class, and test-mod registration class).
- No Gradle or server started by worker-8. Prime owns `compileTestJava`, `runGameTestServer`, original-2.0.0 RED / patched-2.0.1 GREEN experiments and keeping `src/test` out of production JARs.
- The native `BlockState` parent `StateHolder` asserts array-length equality only when Java assertions are enabled; if a test VM enables `-ea`, verify the injected partial-constructor path directly before classifying the null-array test as a port failure. No alternative constructor was invented.
- New cases cover deliberate 2.0.1 behavior and no more. The gamerule is only allowed to gate **natural Rift generation**; blocked automation does not imply manual Rift deletion, teleports disabled, or a complete save/reload test.

## Actual integration results (Prime, 2026-11-07)

The prime agent executed the native disposable GameTest server and supplied these logs, located at the W project root:

| Runtime | Log | Result |
| --- | --- | --- |
| **Original 2.0.0** production JAR with new 2.0.1 regression test mod | `GAMETEST-2.0.1-RED-ON-2.0.0.log` | Expected **RED**: 24 tests complete, 6 fail, build fails |
| **Patched 2.0.1**, NeoForge 26.2.0.93 | `GAMETEST-2.0.1-GREEN-26.2.0.93.log` | **GREEN**: all 24 required GameTests pass; build successful |
| **Same 2.0.1** production JAR, NeoForge minimum 26.2.0.90 | `GAMETEST-2.0.1-GREEN-26.2.0.90.log` | **GREEN**: all 24 required GameTests pass; build successful |

The six original-2.0.0 failures identified the intended missing implementation: three `spawn_rifts_*` cases reported no registered gamerule; both `sonorous_invalid_*_shift` cases reported the cleared rolling window; `ichor_null_blockstate_constructor` threw on `this.propertyKeys == null`. The other 18 cases, including the old 15 plus the vanilla test and two additional 2.0.1 invariants, passed under that original baseline. The same eight new tests then all passed after the 2.0.1 patch. Results come from actual runtime logs, not source inspection or a compile-only claim.

The prime additionally verified ordinary dedicated-server persistence of `spawn_rifts=false` across a full restart with prior world content intact. That is a separate gate from the GameTests and is recorded in Prime's own logs. No live user save or public release is implied by these results.
