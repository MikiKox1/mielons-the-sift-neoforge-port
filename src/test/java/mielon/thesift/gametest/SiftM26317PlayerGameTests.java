package mielon.thesift.gametest;

import java.util.Optional;
import java.lang.reflect.Method;
import mielon.thesift.TheSiftMod;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import mielon.thesift.world.SafePortalArrival;
import mielon.thesift.world.SiftPortalStructure;
import mielon.thesift.world.SiftTeleportManager;
import mielon.thesift.world.SiftWorldStorage;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Regression for M263-17: the old full-server tests returned cows directly.
 * Here a registered server player actually touches portals on both sides.
 * Only enabled on an isolated full server with the real Sift dimension.
 */
final class SiftM26317PlayerGameTests {
    private SiftM26317PlayerGameTests() {}

    /**
     * Disposable server imports only the relevant OVERWORLD region from an
     * offline diagnostic copy. No actual player world, UUID or credentials
     * are used by the test. The saved fractional portal arrival is the
     * reproduction condition absent from earlier integer-Y fixtures.
     */
    @SuppressWarnings("removal")
    static void fractionalUndergroundPortalLanding(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "Sift dimension is needed for portal return");
        BlockPos saved = new BlockPos(-176, -34, -576);
        overworld.getChunk(saved);
        helper.assertTrue(SiftPortalBlock.isActivePortal(overworld.getBlockState(saved)),
            "Disposable region must retain the actual active Ancient City portal");
        Vec3 preferred = new Vec3(-175.036047, -33.833891, -575.965272);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.snapTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            BlockPos source = new BlockPos(82, Math.min(170, sift.getMaxY() - 10), 82);
            sift.getChunk(source);
            sift.setBlock(source.below(), Blocks.STONE.defaultBlockState(), 3);
            for (int i = 0; i < 4; i++) sift.setBlock(source.above(i), Blocks.AIR.defaultBlockState(), 3);
            player.teleport(new TeleportTransition(sift, Vec3.atBottomCenterOf(source), Vec3.ZERO,
                0.0F, 0.0F, TeleportTransition.DO_NOTHING));
            helper.assertTrue(player.level() == sift, "Player must start the return in Sift");
            SiftWorldStorage.saveReturnPoint(server, player.getUUID(),
                new SiftWorldStorage.ReturnPoint(preferred.x, preferred.y, preferred.z, 0, 0, saved));
            Optional<Vec3> landing = SafePortalArrival.findNearRecordedPortal(overworld, player, preferred);
            helper.assertTrue(landing.isPresent(),
                "M263-17: saved fractional player position cannot find a collision-safe landing near existing underground portal");
            helper.assertTrue(Math.abs(landing.orElseThrow().y - saved.getY()) <= 8.0,
                "Never redirect an underground saved portal to the surface");
            helper.assertTrue(landing.orElseThrow().distanceToSqr(
                saved.getX() + 0.5, landing.orElseThrow().y, saved.getZ() + 0.5) <= 16.0 * 16.0 + 1,
                "No arbitrary distant Overworld fallback is allowed");
            helper.assertTrue(SafePortalArrival.safe(overworld, player, landing.orElseThrow()),
                "Selected destination was not independently collision-safe");
            try {
                Method method = SiftTeleportManager.class.getDeclaredMethod(
                    "teleportBackToOverworld", MinecraftServer.class, net.minecraft.world.entity.Entity.class);
                method.setAccessible(true);
                helper.assertTrue(Boolean.TRUE.equals(method.invoke(null, server, player)),
                    "Real main-portal return rejected a safe, saved underground entrance");
            } catch (ReflectiveOperationException problem) {
                throw helper.assertionException("Unable to execute real main-portal return: " + problem);
            }
            helper.assertTrue(player.level() == overworld,
                "Player failed actual cross-dimension travel to saved portal");
            TheSiftMod.LOGGER.info("PRIME2_M26317_FRACTIONAL_RETURN_PASS target={} landed={} safe=true",
                saved, player.position());
            helper.succeed();
        } finally {
            player.connection.disconnect(Component.literal("Prime2 copied-geometry safe-landing QA completed"));
        }
    }

    @SuppressWarnings("removal")
    static void playerUsesNaturallyGeneratedMainPortal(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "The real Sift level is required");
        BlockPos mainAnchor = SiftPortalStructure.ensurePortal(server, sift).orElseThrow(
            () -> helper.assertionException("Naturally generated Sift main portal unavailable"));
        helper.assertTrue(SiftPortalBlock.isActivePortal(sift.getBlockState(mainAnchor)),
            "The actual stored Sift player anchor is not an active portal block");
        BlockPos overworldEntrance = helper.absolutePos(new BlockPos(5, 55, 5));
        prepareEntrance(overworld, overworldEntrance);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.snapTo(overworldEntrance.getX() + 3.5, overworldEntrance.getY(),
                overworldEntrance.getZ() + 0.5);
            SiftTeleportManager.tick(server);
            player.snapTo(overworldEntrance.getX() + 0.5, overworldEntrance.getY(),
                overworldEntrance.getZ() + 0.5);
            SiftTeleportManager.tick(server);
            helper.assertTrue(player.level() == sift, "Entered Overworld portal but was not in Sift");
            helper.assertTrue(SiftWorldStorage.getReturnPoint(server, player.getUUID()).isPresent(),
                "Overworld main entrance was not captured by player transfer");

            TheSiftMod.LOGGER.info("PRIME2_M26317_NATURAL_DIAG mainAnchor={} actualArrival={} touching={} saved={}",
                mainAnchor, player.position(), SiftTeleportManager.isTouchingPortal(player),
                SiftWorldStorage.getReturnPoint(server, player.getUUID()));
            // An arrival on a naturally generated portal is guarded. Even
            // with an animation it must NOT bounce back until contact ends.
            SiftTeleportManager.tick(server);
            helper.assertTrue(player.level() == sift,
                "Portal guard failed: natural arrival immediately bounced back");

            // Make only a tiny disposable exit platform just outside the
            // 120-block natural portal's X-plane. Do not replace the actual
            // portal block, template, or origin ticket.
            BlockPos exit = mainAnchor.east(3);
            for (int dx = 1; dx <= 4; dx++) {
                sift.setBlock(mainAnchor.east(dx).below(), Blocks.STONE.defaultBlockState(), 3);
                for (int dy = 0; dy < 4; dy++) {
                    sift.setBlock(mainAnchor.east(dx).above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            player.snapTo(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5);
            helper.assertTrue(!SiftTeleportManager.isTouchingPortal(player),
                "Natural portal exit fixture is still touching active portal blocks");
            for (int i = 0; i < 5; i++) SiftTeleportManager.tick(server);
            helper.assertTrue(player.level() == sift, "Exiting a portal must not itself teleport");
            player.snapTo(mainAnchor.getX() + 0.5, mainAnchor.getY(),
                mainAnchor.getZ() + 0.5);
            helper.assertTrue(SiftTeleportManager.isTouchingPortal(player),
                "Natural Sift portal does not detect returning player contact");
            SiftTeleportManager.tick(server);

            helper.assertTrue(player.level() == overworld,
                "M263-17: natural main Sift portal reentry failed to return registered player to Overworld");
            helper.assertTrue(Math.abs(player.getY() - overworldEntrance.getY()) <= 3,
                "Natural main portal returned the player to the wrong vertical level");
            helper.assertTrue(player.position().distanceToSqr(
                overworldEntrance.getX() + 0.5, player.getY(),
                overworldEntrance.getZ() + 0.5) <= 256,
                "Natural main portal returned the player away from recorded Overworld frame");
            TheSiftMod.LOGGER.info("PRIME2_M26317_NATURAL_RETURN_PASS anchor={} original={} selected={}",
                mainAnchor, overworldEntrance, player.position());
            helper.succeed();
        } finally {
            player.connection.disconnect(Component.literal("Prime2 natural main-portal QA finished"));
        }
    }

    private static void prepareEntrance(ServerLevel level, BlockPos entrance) {
        level.getChunk(entrance);
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            level.setBlock(entrance.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
            for (int dy = 0; dy < 4; dy++) {
                level.setBlock(entrance.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.setBlock(entrance, ModBlocks.SIFT_PORTAL.defaultBlockState()
                .setValue(SiftPortalBlock.ACTIVE, true), 3);
    }

    @SuppressWarnings("removal")
    static void playerMustReturnToOverworld(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "Full-server Sift level is required");

        // Place a genuinely safe two-block-high passage rather than a cow
        // fixture; the player's taller collision volume matters here.
        BlockPos entrance = helper.absolutePos(new BlockPos(4, 55, 4));
        overworld.getChunk(entrance);
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            overworld.setBlock(entrance.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
            for (int dy = 0; dy < 4; dy++) {
                overworld.setBlock(entrance.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        overworld.setBlock(entrance, ModBlocks.SIFT_PORTAL.defaultBlockState()
                .setValue(SiftPortalBlock.ACTIVE, true), 3);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(!player.isSpectator(), "Non-spectator fixture required");
            player.snapTo(entrance.getX() + 3.5, entrance.getY(), entrance.getZ() + 0.5);
            SiftTeleportManager.tick(server); // initialize the actual player tick lifecycle while outside the portal
            player.snapTo(entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5);
            SiftTeleportManager.tick(server); // real OVERWORLD -> SIFT contact
            helper.assertTrue(player.level() == sift,
                "Real player did not enter the Sift from the active Overworld portal");
            SiftWorldStorage.ReturnPoint point = SiftWorldStorage.getReturnPoint(server, player.getUUID()).orElseThrow(
                () -> helper.assertionException("Main entry did not save the player's Overworld return ticket"));
            helper.assertTrue(point.portalBlock().equals(entrance),
                "Player portal ticket saved the wrong original entrance");

            BlockPos siftPortal = new BlockPos(72, Math.min(150, sift.getMaxY() - 12), 72);
            sift.getChunk(siftPortal);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                sift.setBlock(siftPortal.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
                for (int dy = 0; dy < 4; dy++) {
                    sift.setBlock(siftPortal.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
            sift.setBlock(siftPortal, ModBlocks.SIFT_PORTAL.defaultBlockState()
                .setValue(SiftPortalBlock.ACTIVE, true), 3);
            player.snapTo(siftPortal.getX() + 2.5, siftPortal.getY(), siftPortal.getZ() + 0.5);
            // Stepping fully outside the arrival portal must clear the guard.
            for (int i = 0; i < 3; i++) SiftTeleportManager.tick(server);
            player.snapTo(siftPortal.getX() + 0.5, siftPortal.getY(), siftPortal.getZ() + 0.5);
            SiftTeleportManager.tick(server); // real SIFT -> OVERWORLD contact

            helper.assertTrue(player.level() == overworld,
                "M263-17: player entered Sift portal but remained in Sift");
            helper.assertTrue(player.position().distanceToSqr(
                entrance.getX() + 0.5, player.getY(), entrance.getZ() + 0.5) < 256,
                "Player returned far away from the recorded Overworld entrance");
            helper.assertTrue(Math.abs(player.getY() - entrance.getY()) <= 3,
                "Player returned at the surface instead of the recorded portal height");
            helper.assertTrue(SiftWorldStorage.getReturnPoint(server, player.getUUID()).isEmpty(),
                "One-shot portal ticket was not consumed");
            helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, player.getUUID())
                .map(p -> p.portalBlock().equals(entrance)).orElse(false),
                "Durable original portal entrance was lost");
            TheSiftMod.LOGGER.info("PRIME2_M26317_REAL_PLAYER_PASS origin={} dimension={} destination={}",
                entrance, player.level().dimension(), player.position());
            helper.succeed();
        } finally {
            player.connection.disconnect(Component.literal("Prime2 isolated player portal regression finished"));
        }
    }
}
