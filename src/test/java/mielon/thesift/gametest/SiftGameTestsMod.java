package mielon.thesift.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Loaded only by the disposable NeoForge gameTestServer run, never the production mod. */
@Mod(SiftGameTestsMod.MOD_ID)
public final class SiftGameTestsMod {
    public static final String MOD_ID = "the_sift_tests";
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MOD_ID);

    private record TestCase(String name, Consumer<GameTestHelper> function, boolean peaceful) {
        private TestCase(String name, Consumer<GameTestHelper> function) {
            this(name, function, false);
        }
    }

    private static final List<TestCase> CASES = createCases();

    private static List<TestCase> createCases() {
        List<TestCase> cases = new ArrayList<>(List.of(
            new TestCase("mob_registration_attributes", SiftServerGameTests::mobRegistrationAttributes),
            new TestCase("utility_entities", SiftServerGameTests::utilityEntities),
            new TestCase("monarch_echo", SiftServerGameTests::monarchEcho),
            new TestCase("ritual_sources_consume", SiftServerGameTests::ritualSourcesConsume),
            new TestCase("ritual_missing_source", SiftServerGameTests::ritualMissingSource),
            new TestCase("ritual_flowing_ichor", SiftServerGameTests::ritualFlowingIchor),
            new TestCase("ritual_peaceful", SiftServerGameTests::ritualPeaceful, true),
            new TestCase("echo_golem_rejects_incorrect_base", SiftServerGameTests::echoGolemRejectsIncorrectBase),
            new TestCase("echo_golem_single_summon", SiftServerGameTests::echoGolemSingleSummon),
            new TestCase("blubby_berries_bonemeal_harvest", SiftServerGameTests::blubbyBerriesBonemealHarvest),
            new TestCase("sculkflower_crop_bonemeal", SiftServerGameTests::sculkflowerCropBonemeal),
            new TestCase("ichor_soul_absorption", SiftServerGameTests::ichorSoulAbsorption),
            new TestCase("willow_native_strippables", SiftServerGameTests::willowNativeStrippables),
            new TestCase("native_compostables", SiftServerGameTests::nativeCompostables),
            new TestCase("furnace_fuel_events", SiftServerGameTests::furnaceFuelEvents),
            new TestCase("easymagic_actual_menu_contract", SiftEasyMagicRuntimeGameTests::actualMenuContract),
            new TestCase("target263_nbt_legacy4903", Sift263StructureGameTests::legacy4903PortalTemplate),
            new TestCase("target263_nbt_schema5023", Sift263StructureGameTests::target5023StructurePalettes),
            new TestCase("mc263_all_sift_biomes_preserve_natural_spawns",
                    Sift263BiomeSpawnGameTests::allSiftBiomesPreserveNaturalSpawns),
            new TestCase("spawn_rifts_rule_registration", Sift201RegressionGameTests::spawnRiftsRegistered),
            new TestCase("spawn_rifts_toggle_timer", Sift201RegressionGameTests::spawnRiftsToggleAndTimer),
            new TestCase("spawn_rifts_preserves_manual", Sift201RegressionGameTests::disabledRiftsPreservesManualEntity),
            new TestCase("sonorous_invalid_opening_shift", Sift201RegressionGameTests::sonorousOpeningShiftAfterInvalidPrefix),
            new TestCase("sonorous_invalid_closing_shift", Sift201RegressionGameTests::sonorousClosingShiftAfterInvalidPrefix),
            new TestCase("sonorous_valid_match_clears", Sift201RegressionGameTests::sonorousValidWindowClears),
            new TestCase("ichor_null_blockstate_constructor", Sift201RegressionGameTests::ichorNullPropertyConstructor),
            new TestCase("ichorlogged_stairs_light", Sift201RegressionGameTests::ichorloggedStairsRemainLuminous),
            new TestCase("t11_main_durable_after_rift", SiftT11RoutingGameTests::mainEntranceSurvivesRiftTicketClear),
            new TestCase("t11_stale_main_cell_no_rift", SiftT11RoutingGameTests::activePortalWithStaleRecordedCellNeverQueuesRift),
            new TestCase("t11_closed_main_cell_no_rift", SiftT11RoutingGameTests::trulyClosedPortalDoesNotCreateRift),
            new TestCase("t11_active_no_safe_floor_no_rift", SiftT11RoutingGameTests::activePortalWithoutSafeStandingRoomNeverQueuesRift),
            new TestCase("t11_underground_no_surface_fallback", SiftT11RoutingGameTests::undergroundMainPortalDoesNotSendTravelerToSurface),
            new TestCase("t11_two_main_entrances_isolated", SiftT11RoutingGameTests::separateMainEntrancesBelongToSeparateTravelers),
            new TestCase("t11_same_frame_adjacent_recovery", SiftT11RoutingGameTests::staleCellRecoversOnlyWithinItsOwnActiveFrame),
            new TestCase("t11_different_adjacent_frame_rejected", SiftT11RoutingGameTests::adjacentOtherPortalMustNotHijackRecordedFrame),
            new TestCase("t11_rejected_rift_spawn", SiftT11RoutingGameTests::rejectedRiftSpawnIsNotRecorded),
            new TestCase("t11_expired_rift_no_late_transfer", SiftT11RoutingGameTests::expiredSourceRiftCannotCompletePendingTransfer),
            new TestCase("t11_walked_away_rift_no_transfer", SiftT11RoutingGameTests::walkingAwayFromQueuedRiftCancelsTransfer)
        ));
        // Never enable this flag in GameTestServer: real Sift dimension routing needs Prime's full server harness.
        if (Boolean.getBoolean("the_sift.tests.crossdimension")) {
            if (Boolean.getBoolean("the_sift.tests.m26317copied")) {
                cases.add(new TestCase("m26317_fractional_underground_return",
                        SiftM26317PlayerGameTests::fractionalUndergroundPortalLanding));
            }
            cases.add(new TestCase("m26317_natural_sift_main_roundtrip",
                    SiftM26317PlayerGameTests::playerUsesNaturallyGeneratedMainPortal));
            cases.add(new TestCase("m26317_real_player_main_roundtrip",
                    SiftM26317PlayerGameTests::playerMustReturnToOverworld));
            cases.add(new TestCase("t11_actual_rift_pair_sift", SiftT11RoutingGameTests::riftIntoSiftCreatesAndReusesAnActualPairedRift));
            cases.add(new TestCase("t11_real_main_return", SiftT11RoutingGameTests::realSiftDimensionMainPortalReturn));
            cases.add(new TestCase("t11_vehicle_rider_portal_return", SiftT11RoutingGameTests::vehicleUsesPassengerMainEntranceWhenRootHasNoTicket));
            cases.add(new TestCase("t11_real_sift_rift_out", SiftT11RoutingGameTests::realSiftRiftReturnMakesMatchedOverworldCounterpart));
            cases.add(new TestCase("t11_real_underground_portal", SiftT11RoutingGameTests::realUndergroundMainPortalReturn));
            cases.add(new TestCase("t11_pair_restart_prepare", SiftT11PersistenceGameTests::preparePersistedPair));
            cases.add(new TestCase("t11_pair_restart_verify", SiftT11PersistenceGameTests::verifyPairAfterFullServerRestart));
        }
        return List.copyOf(cases);
    }

    static {
        for (TestCase test : CASES) {
            FUNCTIONS.register(test.name(), () -> test.function());
        }
    }

    public SiftGameTestsMod(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(SiftGameTestsMod::registerGameTests);
        NeoForge.EVENT_BUS.addListener(SiftT11RoutingGameTests::cancelRiftSpawn);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> normal = event.registerEnvironment(
                id("normal"), new TestEnvironmentDefinition.SetDifficulty(Difficulty.NORMAL));
        Holder<TestEnvironmentDefinition<?>> peaceful = event.registerEnvironment(
                id("peaceful"), new TestEnvironmentDefinition.SetDifficulty(Difficulty.PEACEFUL));

        for (TestCase test : CASES) {
            ResourceKey<Consumer<GameTestHelper>> functionKey = ResourceKey.create(Registries.TEST_FUNCTION, id(test.name()));
            // Vanilla ships minecraft:empty.nbt; padding isolates expanded scenes during parallel runs.
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    test.peaceful() ? peaceful : normal, Level.OVERWORLD, EMPTY_STRUCTURE,
                    test.name().startsWith("m26317_") ? 400
                        : test.name().startsWith("t11_pair_restart") ? 240 : 100, 0, true,
                    Rotation.NONE, false, 1, 1, false, 20);
            event.registerTest(id(test.name()), new FunctionGameTestInstance(functionKey, data));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
