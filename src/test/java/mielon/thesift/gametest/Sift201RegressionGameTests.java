package mielon.thesift.gametest;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SonorousDeepslateBlock;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.fluid.IchorWaterlogging;
import mielon.thesift.fluid.ModFluids;
import mielon.thesift.portal.SonorousConsoles;
import mielon.thesift.world.RiftManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/** Regression probes for the official upstream 2.0.1 delta; the server owns every fixture. */
final class Sift201RegressionGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 4, 4);
    private static final Identifier SPAWN_RIFTS = Identifier.fromNamespaceAndPath("the_sift", "spawn_rifts");

    private Sift201RegressionGameTests() {
    }

    static void spawnRiftsRegistered(GameTestHelper helper) {
        GameRule<Boolean> rule = spawnRiftsRule(helper);
        helper.assertTrue(rule.category().equals(GameRuleCategory.SPAWNING), "spawn_rifts is not in SPAWNING category");
        helper.assertValueEqual(rule.defaultValue(), Boolean.TRUE, "spawn_rifts should default true");
        helper.assertValueEqual(helper.getLevel().getServer().getGameRules().get(rule), Boolean.TRUE,
                "a new GameTest world should start with spawn_rifts enabled");
        helper.succeed();
    }

    static void spawnRiftsToggleAndTimer(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        GameRule<Boolean> rule = spawnRiftsRule(helper);
        boolean before = server.getGameRules().get(rule);
        Map<UUID, Long> nextSpawn = nextSpawnTimer(helper);
        UUID scheduled = UUID.randomUUID();
        try {
            nextSpawn.put(scheduled, server.overworld().getGameTime() + 7000L);
            command(server, false);
            helper.assertValueEqual(server.getGameRules().get(rule), Boolean.FALSE, "false gamerule command did not apply");
            RiftManager.tick(server);
            helper.assertTrue(nextSpawn.isEmpty(), "disabled natural Rift scheduling did not clear its pending timer");

            command(server, true);
            helper.assertValueEqual(server.getGameRules().get(rule), Boolean.TRUE, "true gamerule command did not apply");
            long marker = server.overworld().getGameTime() + 8000L;
            nextSpawn.put(scheduled, marker);
            RiftManager.tick(server);
            helper.assertValueEqual(nextSpawn.get(scheduled), marker,
                    "reenabling the gamerule still clears natural Rift schedule entries");
            helper.succeed();
        } finally {
            nextSpawn.remove(scheduled);
            command(server, before);
        }
    }

    static void disabledRiftsPreservesManualEntity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        GameRule<Boolean> rule = spawnRiftsRule(helper);
        boolean before = server.getGameRules().get(rule);
        RiftEntity rift = ModEntities.RIFT.create(level, EntitySpawnReason.COMMAND);
        helper.assertTrue(rift != null, "failed to construct real RiftEntity");
        BlockPos center = helper.absolutePos(CENTER);
        rift.configure(false, true, level.getGameTime() + 6000L, UUID.randomUUID());
        rift.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        helper.assertTrue(level.addFreshEntity(rift), "manual Rift entity failed to spawn");
        try {
            command(server, false);
            RiftManager.tick(server);
            helper.assertTrue(!rift.isRemoved() && level.getEntity(rift.getUUID()) == rift,
                    "disabling natural Rift spawning removed an already spawned Rift");
            helper.succeed();
        } finally {
            rift.discard();
            command(server, before);
        }
    }

    private static GameRule<Boolean> spawnRiftsRule(GameTestHelper helper) {
        GameRule<?> raw = BuiltInRegistries.GAME_RULE.getValue(SPAWN_RIFTS);
        helper.assertTrue(raw != null, "missing registered the_sift:spawn_rifts gamerule");
        helper.assertTrue(raw.valueClass() == Boolean.class && raw.getIdentifier().equals(SPAWN_RIFTS),
                "spawn_rifts must be a namespaced Boolean gamerule");
        @SuppressWarnings("unchecked")
        GameRule<Boolean> result = (GameRule<Boolean>) raw;
        return result;
    }

    private static void command(MinecraftServer server, boolean enabled) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                "gamerule the_sift:spawn_rifts " + enabled);
    }

    /** Reflection remains entirely within the dev-only tests; no new production accessor. */
    private static Map<UUID, Long> nextSpawnTimer(GameTestHelper helper) {
        try {
            Field field = RiftManager.class.getDeclaredField("NEXT_SPAWN");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, Long> timers = (Map<UUID, Long>) field.get(null);
            return timers;
        } catch (ReflectiveOperationException exception) {
            throw helper.assertionException("Cannot inspect native RiftManager.NEXT_SPAWN: " + exception);
        }
    }

    static void sonorousOpeningShiftAfterInvalidPrefix(GameTestHelper helper) {
        expectShiftedMatch(helper, new int[] {1, 1, 3, 7, 6, 5, 2, 4}, 8,
                SonorousConsoles.TARGET_SEQUENCE, SonorousConsoles.SequenceType.OPENING);
        helper.succeed();
    }

    static void sonorousClosingShiftAfterInvalidPrefix(GameTestHelper helper) {
        expectShiftedMatch(helper, new int[] {8, 8, 4, 2, 5, 6, 7, 3}, 1,
                SonorousConsoles.CLOSING_SEQUENCE, SonorousConsoles.SequenceType.CLOSING);
        helper.succeed();
    }

    private static void expectShiftedMatch(GameTestHelper helper, int[] invalid, int last,
            int[] expected, SonorousConsoles.SequenceType expectedType) {
        ServerLevel level = helper.getLevel();
        BlockPos note = helper.absolutePos(CENTER.above());
        level.setBlock(note.below(), ModBlocks.SONOROUS_DEEPSLATE.defaultBlockState()
                .setValue(SonorousDeepslateBlock.MODE, SonorousDeepslateBlock.Mode.NOTE), 3);
        level.setBlock(note, Blocks.NOTE_BLOCK.defaultBlockState(), 3);
        helper.assertValueEqual(SonorousConsoles.findAnchor(level, note), note.below(),
                "native loaded-block search did not resolve NOTE mode below the note block");

        SonorousConsoles.reset(level, note);
        try {
            for (int sound : invalid) {
                SonorousConsoles.onSoundPlayed(level, note, sound);
            }
            // Upstream 2.0.1 still returns an attempt on invalid eight notes; it must keep the buffer.
            Optional<SonorousConsoles.MatchedSequence> shifted = SonorousConsoles.onSoundPlayed(level, note, last);
            helper.assertTrue(shifted.isPresent(), "invalid first eight notes erased the rolling note window");
            helper.assertValueEqual(shifted.orElseThrow().type(), expectedType, "wrong shifted sequence direction");
            int[] actual = shifted.orElseThrow().notes().stream()
                    .mapToInt(SonorousConsoles.PlayedNote::soundIndex1to8).toArray();
            helper.assertTrue(Arrays.equals(actual, expected), "shifted console notes differ from the complete intended sequence: "
                    + Arrays.toString(actual));
            helper.assertTrue(shifted.orElseThrow().notes().stream().allMatch(n -> n.notePos().equals(note)),
                    "matched notes lost native note-block positions");
        } finally {
            SonorousConsoles.reset(level, note);
        }
    }

    static void sonorousValidWindowClears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos note = helper.absolutePos(CENTER.above());
        level.setBlock(note.below(), ModBlocks.SONOROUS_DEEPSLATE.defaultBlockState()
                .setValue(SonorousDeepslateBlock.MODE, SonorousDeepslateBlock.Mode.NOTE), 3);
        level.setBlock(note, Blocks.NOTE_BLOCK.defaultBlockState(), 3);
        SonorousConsoles.reset(level, note);
        try {
            Optional<SonorousConsoles.MatchedSequence> match = Optional.empty();
            for (int value : SonorousConsoles.TARGET_SEQUENCE) match = SonorousConsoles.onSoundPlayed(level, note, value);
            helper.assertTrue(match.isPresent(), "valid opening sequence was not recognized");
            helper.assertValueEqual(match.orElseThrow().type(), SonorousConsoles.SequenceType.OPENING,
                    "valid opening sequence was misclassified");
            for (int i = 1; i < SonorousConsoles.TARGET_SEQUENCE.length; i++) {
                helper.assertTrue(SonorousConsoles.onSoundPlayed(level, note, SonorousConsoles.TARGET_SEQUENCE[i]).isEmpty(),
                        "matched sequence was left buffered after a successful opening");
            }
        } finally {
            SonorousConsoles.reset(level, note);
        }
        helper.succeed();
    }

    static void ichorNullPropertyConstructor(GameTestHelper helper) {
        // Actual vanilla constructor: the 2.0.1 mixin must skip property lookup during incomplete state setup.
        BlockState incomplete = new BlockState(Blocks.AIR, null, null);
        helper.assertTrue(incomplete.is(Blocks.AIR), "null-array BlockState has wrong owner");
        helper.assertTrue(incomplete.getFluidState().isEmpty(), "null-array vanilla AIR state gained fluid");
        helper.assertValueEqual(incomplete.getLightEmission(), 0, "null-array vanilla AIR gained light");
        helper.succeed();
    }

    static void ichorloggedStairsRemainLuminous(GameTestHelper helper) {
        BlockState vanilla = Blocks.OAK_STAIRS.defaultBlockState();
        helper.assertTrue(vanilla.getFluidState().isEmpty(), "dry vanilla stairs should contain no fluid");
        BlockState ichorlogged = IchorWaterlogging.fillState(vanilla);
        helper.assertTrue(IchorWaterlogging.isIchorlogged(ichorlogged), "stair Ichorlogging flag not retained");
        helper.assertTrue(ichorlogged.getFluidState().getType().isSame(ModFluids.ICHOR),
                "Ichorlogged stair no longer exposes native Ichor fluid");
        helper.assertTrue(ichorlogged.getLightEmission() >= 12, "Ichorlogged stair no longer emits light level 12");
        helper.succeed();
    }
}
