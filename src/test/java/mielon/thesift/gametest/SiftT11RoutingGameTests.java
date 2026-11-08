package mielon.thesift.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mielon.thesift.TheSiftMod;
import mielon.thesift.block.ModBlocks;
import mielon.thesift.block.SiftPortalBlock;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.world.RiftDirectory;
import mielon.thesift.world.RiftManager;
import mielon.thesift.world.SafePortalArrival;
import mielon.thesift.world.PortalTransitGuard;
import mielon.thesift.world.SiftTeleportManager;
import mielon.thesift.world.SiftWorldStorage;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Actual server-side regression probes for the user-observed wrong T11 Rift route. */
final class SiftT11RoutingGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 4, 4);
    private static boolean rejectNextRiftSpawn;

    private SiftT11RoutingGameTests() {
    }

    static void mainEntranceSurvivesRiftTicketClear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos block = helper.absolutePos(CENTER);
        level.setBlock(block.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(block, ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        Cow traveler = traveler(helper, block);
        SiftWorldStorage.saveReturnPoint(traveler);
        UUID id = traveler.getUUID();
        Optional<SiftWorldStorage.ReturnPoint> immediate = SiftWorldStorage.getReturnPoint(server, id);
        helper.assertTrue(immediate.isPresent() && immediate.orElseThrow().portalBlock().equals(block), "original portal cell not saved");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, id).isPresent(), "durable portal missing at entry");

        SiftWorldStorage.markRiftEntry(traveler);
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, id).isEmpty(), "Rift entry must clear one-shot ticket");
        Optional<SiftWorldStorage.ReturnPoint> durable = SiftWorldStorage.getMainPortalEntrance(server, id);
        helper.assertTrue(durable.isPresent() && durable.orElseThrow().portalBlock().equals(block),
                "Rift entry incorrectly cleared the last real main portal");
        helper.succeed();
    }

    static void activePortalWithStaleRecordedCellNeverQueuesRift(GameTestHelper helper) {
        BlockPos landing = helper.absolutePos(CENTER);
        ServerLevel level = helper.getLevel();
        level.setBlock(landing.below(), Blocks.STONE.defaultBlockState(), 3);
        Cow traveler = traveler(helper, landing);
        SiftWorldStorage.saveReturnPoint(traveler); // simulates a stale saved AIR cell
        BlockPos realPortal = landing.east();
        level.setBlock(realPortal, ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        helper.assertTrue(!SiftPortalBlock.isActivePortal(level.getBlockState(landing)), "fixture must start from a stale saved cell");
        helper.assertTrue(SiftPortalBlock.isActivePortal(level.getBlockState(realPortal)), "fixture has no nearby active portal");

        boolean handled = attemptMainReturn(level.getServer(), traveler, helper);
        helper.assertTrue(handled, "main return should find a safe Overworld destination near the original entrance");
        helper.assertTrue(!riftTransferQueued(traveler.getUUID(), helper), "main return was converted to a spawn Rift");
        helper.succeed();
    }

    static void trulyClosedPortalDoesNotCreateRift(GameTestHelper helper) {
        BlockPos landing = helper.absolutePos(CENTER);
        ServerLevel level = helper.getLevel();
        level.setBlock(landing.below(), Blocks.STONE.defaultBlockState(), 3);
        Cow traveler = traveler(helper, landing);
        SiftWorldStorage.saveReturnPoint(traveler);
        helper.assertTrue(!SiftPortalBlock.isActivePortal(level.getBlockState(landing)), "fixture must have no active entrance");
        boolean handled = attemptMainReturn(level.getServer(), traveler, helper);
        helper.assertTrue(handled, "closed main portal should return directly near saved coordinates or safe spawn");
        helper.assertTrue(!riftTransferQueued(traveler.getUUID(), helper), "closed main portal spawned a Rift instead of direct return");
        helper.succeed();
    }

    static void activePortalWithoutSafeStandingRoomNeverQueuesRift(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos airPortal = helper.absolutePos(CENTER).above(80);
        level.setBlock(airPortal, ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        Cow traveler = traveler(helper, airPortal);
        helper.assertTrue(SiftPortalBlock.isActivePortal(level.getBlockState(airPortal)), "portal must remain open");
        SiftWorldStorage.saveReturnPoint(traveler);
        helper.assertTrue(SafePortalArrival.find(level, traveler, traveler.position()).isEmpty(),
                "fixture must reproduce active portal with no floor/safe landing");
        helper.assertTrue(SafePortalArrival.findNearRecordedPortal(level, traveler, traveler.position()).isEmpty(),
                "fixture must have no valid landing within portal-local radius");
        helper.assertTrue(!attemptMainReturn(level.getServer(), traveler, helper),
                "No underground landing should leave the traveler in Sift, not send them to surface spawn");
        helper.assertTrue(traveler.level() == level && !traveler.isRemoved(),
                "Missing landing should not move or destroy traveler");
        helper.assertTrue(PortalTransitGuard.isBlocked(traveler),
                "Avoid retrying expensive failed landing on every tick while still touching portal");
        helper.assertTrue(!riftTransferQueued(traveler.getUUID(), helper),
                "unsafe landing beside ACTIVE portal must not fabricate a spawn Rift or report it closed");
        helper.succeed();
    }

    static void undergroundMainPortalDoesNotSendTravelerToSurface(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        BlockPos undergroundPortal = helper.absolutePos(CENTER).above(44);
        // Simulate an underground vault: the original portal is open below
        // a solid roof, but the nearest safe floor is 10-12 blocks east.
        // The original 8-block search fails. A generic spawn adjustment
        // jumps to world spawn or a surface heightmap instead of that room.
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                overworld.setBlock(undergroundPortal.offset(dx, 17, dz),
                    Blocks.STONE.defaultBlockState(), 3);
            }
        }
        for (int dx = 10; dx <= 13; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                overworld.setBlock(undergroundPortal.offset(dx, -1, dz),
                    Blocks.STONE.defaultBlockState(), 3);
            }
        }
        overworld.setBlock(undergroundPortal,
            ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        Cow traveler = traveler(helper, undergroundPortal);
        UUID id = traveler.getUUID();
        SiftWorldStorage.saveReturnPoint(server, id,
            new SiftWorldStorage.ReturnPoint(
                undergroundPortal.getX() + 0.5, undergroundPortal.getY(),
                undergroundPortal.getZ() + 0.5, 0.0F, 0.0F, undergroundPortal));
        helper.assertTrue(SafePortalArrival.find(overworld, traveler, traveler.position()).isEmpty(),
            "Fixture must reproduce the short search failing beside an active enclosed portal");
        Vec3 selected = SafePortalArrival.findNearRecordedPortal(overworld, traveler, traveler.position())
            .orElseThrow(() -> helper.assertionException("No safe landing found near recorded underground portal"));
        helper.assertTrue(Math.abs(selected.y - undergroundPortal.getY()) <= 3.0,
            "Underground arrival selected surface height instead of recorded portal level");
        helper.assertTrue(selected.distanceToSqr(
                undergroundPortal.getX() + 0.5, selected.y,
                undergroundPortal.getZ() + 0.5) <= 256.0,
            "Landing position is too far from the original Ancient City portal");
        helper.assertTrue(SafePortalArrival.safe(overworld, traveler, selected),
            "Selected underground landing is not collision-safe");
        helper.assertTrue(!riftTransferQueued(id, helper),
            "Underground portal accidentally started a Rift transfer");
        helper.succeed();
    }

    static void realUndergroundMainPortalReturn(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "Real Sift dimension missing");
        BlockPos seed = helper.absolutePos(CENTER);
        int y = Math.min(overworld.getMaxY() - 25,
            overworld.getHeight(Types.MOTION_BLOCKING, seed.getX(), seed.getZ()) + 23);
        BlockPos entrance = new BlockPos(seed.getX(), y, seed.getZ());
        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                overworld.setBlock(entrance.offset(dx, 17, dz),
                    Blocks.STONE.defaultBlockState(), 3);
            }
        }
        for (int dx = 10; dx <= 13; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                overworld.setBlock(entrance.offset(dx, -1, dz),
                    Blocks.STONE.defaultBlockState(), 3);
            }
        }
        overworld.setBlock(entrance,
            ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        BlockPos source = new BlockPos(32, Math.min(sift.getMaxY() - 8, 160), 32);
        sift.getChunk(source);
        sift.setBlock(source.below(), Blocks.STONE.defaultBlockState(), 3);
        for (int dy = 0; dy < 3; dy++) sift.setBlock(source.above(dy), Blocks.AIR.defaultBlockState(), 3);
        Cow cow = EntityType.COW.create(sift, EntitySpawnReason.COMMAND);
        helper.assertTrue(cow != null, "Cannot construct traveler in Sift");
        cow.setPos(source.getX() + 0.5, source.getY(), source.getZ() + 0.5);
        helper.assertTrue(sift.addFreshEntity(cow), "Cannot add traveler to Sift");
        SiftWorldStorage.saveReturnPoint(server, cow.getUUID(), new SiftWorldStorage.ReturnPoint(
            entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5, 0.0F, 0.0F, entrance
        ));
        helper.assertTrue(attemptMainReturn(server, cow, helper),
            "Main portal could not return traveler to a nearby safe underground landing");
        Entity returned = overworld.getEntityInAnyDimension(cow.getUUID());
        helper.assertTrue(returned != null && returned.level() == overworld,
            "Cross-dimensional underground traveler never appeared in Overworld");
        helper.assertTrue(Math.abs(returned.getY() - entrance.getY()) <= 3.0,
            "Underground return landed on the surface: portalY="
                + entrance.getY() + " resultY=" + returned.getY());
        helper.assertTrue(returned.position().distanceToSqr(
                entrance.getX() + 0.5, returned.getY(), entrance.getZ() + 0.5) <= 256.0,
            "Traveler landed far outside the recorded portal's underground room");
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, cow.getUUID()).isEmpty(),
            "One-shot return ticket remained after successful underground return");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, cow.getUUID()).orElseThrow()
                .portalBlock().equals(entrance),
            "Durable underground portal address was lost after successful return");
        TheSiftMod.LOGGER.info(
            "PRIME2_T11_UNDERGROUND_PASS portal={} selected={} verticalOffset={} samePortal=true",
            entrance, returned.position(), returned.getY() - entrance.getY());
        returned.discard();
        helper.succeed();
    }

    static void separateMainEntrancesBelongToSeparateTravelers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos first = helper.absolutePos(CENTER);
        BlockPos second = helper.absolutePos(CENTER.east(3));
        level.setBlock(first, ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        level.setBlock(second, ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        Cow a = traveler(helper, first);
        Cow b = traveler(helper, second);
        SiftWorldStorage.saveReturnPoint(a);
        SiftWorldStorage.saveReturnPoint(b);
        SiftWorldStorage.markRiftEntry(a);
        SiftWorldStorage.clearReturnPoint(server, b.getUUID());
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, a.getUUID()).isEmpty(), "Rift ticket not cleared for A");
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, b.getUUID()).isEmpty(), "Portal ticket not cleared for B");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, a.getUUID()).orElseThrow().portalBlock().equals(first),
                "A lost or swapped its historic portal");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, b.getUUID()).orElseThrow().portalBlock().equals(second),
                "B lost or swapped its historic portal");
        helper.succeed();
    }

    static void staleCellRecoversOnlyWithinItsOwnActiveFrame(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(CENTER);
        BlockPos stale = base.offset(1, 2, 0);
        buildReinforcedPortalFrame(level, base, stale);
        helper.assertTrue(!SiftPortalBlock.isActivePortal(level.getBlockState(stale)),
            "Saved original interior cell must be missing");
        Optional<BlockPos> recovered = adjacentMainPortal(level, stale, helper);
        helper.assertTrue(recovered.isPresent()
            && SiftPortalBlock.isActivePortal(level.getBlockState(recovered.orElseThrow())),
            "Failed to recover an adjacent active cell of the SAME intact portal frame");
        helper.succeed();
    }

    static void adjacentOtherPortalMustNotHijackRecordedFrame(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos saved = helper.absolutePos(CENTER).offset(1, 2, 0);
        // This entire valid portal is only one Z away from the old recorded
        // cell, but is a DIFFERENT reinforced-deepslate frame.
        buildReinforcedPortalFrame(level, helper.absolutePos(CENTER).south(), null);
        helper.assertTrue(SiftPortalBlock.isActivePortal(level.getBlockState(saved.south())),
            "Other-frame portal fixture is missing");
        helper.assertTrue(adjacentMainPortal(level, saved, helper).isEmpty(),
            "Nearby but unrelated frame was incorrectly selected as the recorded entrance");
        helper.succeed();
    }

    static void rejectedRiftSpawnIsNotRecorded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(CENTER);
        for (int a = -4; a <= 4; a++) {
            level.setBlock(base.offset(a, -1, 0), Blocks.STONE.defaultBlockState(), 3);
            for (int y = 0; y < 5; y++) {
                level.setBlock(base.offset(a, y, 0), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        rejectNextRiftSpawn = true;
        try {
            helper.assertTrue(!RiftManager.tryPlaceCreativeRift(level, base, true),
                    "a canceled native entity spawn must not be reported as a successful Rift");
            helper.assertTrue(!rejectNextRiftSpawn, "the fixture did not reach the real EntityJoinLevelEvent");
            helper.assertTrue(RiftDirectory.loaded().stream().noneMatch(
                rift -> rift.level() == level && rift.getAnchorPos().distSqr(base) < 9.0),
                "a canceled native entity was still added to the Rift directory");
        } finally {
            rejectNextRiftSpawn = false;
        }
        helper.succeed();
    }

    static void expiredSourceRiftCannotCompletePendingTransfer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CENTER);
        RiftEntity origin = new RiftEntity(ModEntities.RIFT, level);
        origin.configure(true, true, level.getGameTime() - 1, UUID.randomUUID());
        origin.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        Cow traveler = traveler(helper, pos);
        queueRift(traveler, origin, helper);
        helper.assertTrue(riftTransferQueued(traveler.getUUID(), helper), "pending fixture was not queued");
        RiftManager.tick(level.getServer());
        helper.assertTrue(!riftTransferQueued(traveler.getUUID(), helper),
                "an expired Rift kept the traveler queued for a delayed teleport");
        helper.assertTrue(traveler.level() == level && !traveler.isRemoved(),
                "an expired Rift must not move or delete its traveler");
        helper.succeed();
    }

    static void walkingAwayFromQueuedRiftCancelsTransfer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CENTER);
        RiftEntity origin = new RiftEntity(ModEntities.RIFT, level);
        origin.configure(true, true, level.getGameTime() + 6000, UUID.randomUUID());
        origin.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        Cow traveler = traveler(helper, pos);
        queueRift(traveler, origin, helper);
        traveler.setPos(pos.getX() + 25.5, pos.getY(), pos.getZ() + 0.5);
        RiftManager.tick(level.getServer());
        helper.assertTrue(!riftTransferQueued(traveler.getUUID(), helper),
                "a traveler that walked away must not teleport when the search finishes");
        helper.assertTrue(traveler.level() == level && !traveler.isRemoved(),
                "walking away from an open Rift must not teleport the traveler");
        helper.succeed();
    }

    static void riftIntoSiftCreatesAndReusesAnActualPairedRift(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "Sift dimension missing in the test server");
        BlockPos originPos = new BlockPos(8, 96, 8);
        RiftEntity origin = new RiftEntity(ModEntities.RIFT, helper.getLevel());
        origin.configure(true, true, helper.getLevel().getGameTime() + 6000L, UUID.randomUUID());
        origin.setPos(originPos.getX() + 0.5, originPos.getY(), originPos.getZ() + 0.5);

        sift.getChunk(0, 0);
        int platformY = Math.min(sift.getMaxY() - 8,
            Math.max(sift.getHeight(Types.MOTION_BLOCKING, 8, 8) + 8, sift.getSeaLevel() + 8));
        for (int x = 4; x <= 12; x++) {
            for (int z = 4; z <= 12; z++) {
                sift.setBlock(new BlockPos(x, platformY, z), Blocks.STONE.defaultBlockState(), 3);
                for (int height = 1; height <= 6; height++) {
                    sift.setBlock(new BlockPos(x, platformY + height, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        Cow traveler = traveler(helper, originPos);
        BlockPos first = destinationOfIntoSift(server, traveler, origin, helper);
        helper.assertTrue(first != null, "A new Overworld Rift did not create its Sift counterpart");
        RiftEntity partner = RiftDirectory.loaded().stream().filter(rift -> rift.level() == sift
            && rift.getPairId().equals(origin.getPairId()) && rift.getAnchorPos().equals(first))
            .findFirst().orElse(null);
        helper.assertTrue(partner != null && !partner.targetsSift()
            && originPos.equals(partner.getLinkedPos()), "Created Sift counterpart not linked back to source Rift");
        helper.assertTrue(origin.getLinkedPos().equals(first), "Overworld Rift did not link to Sift counterpart");
        int before = (int)RiftDirectory.loaded().stream().filter(rift ->
            rift.level() == sift && rift.getPairId().equals(origin.getPairId())).count();
        BlockPos second = destinationOfIntoSift(server, traveler, origin, helper);
        helper.assertTrue(first.equals(second),
            "Re-entry did not reuse paired Rift: sourceTime=" + helper.getLevel().getGameTime()
                + " siftTime=" + sift.getGameTime() + " sourceExpiry=" + origin.getExpiresAt()
                + " partnerExpiry=" + partner.getExpiresAt() + " partnerBacklink=" + partner.getLinkedPos()
                + " sectionReady=" + sift.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(
                    first.getX() >> 4, first.getZ() >> 4).pack())
                + " expected=" + first + " actual=" + second);
        helper.assertValueEqual((int)RiftDirectory.loaded().stream().filter(rift ->
            rift.level() == sift && rift.getPairId().equals(origin.getPairId())).count(), before,
            "Re-entry spawned a duplicate Rift instead of reusing its linked counterpart");
        TheSiftMod.LOGGER.info(
            "PRIME2_T11_CROSSDIM_PASS sourceRift={} pairedSiftRift={} pairId={} destinationReused={} count={}",
            originPos, first, origin.getPairId(), first.equals(second), before
        );
        partner.discard();
        helper.succeed();
    }

    static void realSiftDimensionMainPortalReturn(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "The real Sift dimension is not loaded");
        BlockPos entrance = helper.absolutePos(CENTER);
        overworld.setBlock(entrance.below(), Blocks.STONE.defaultBlockState(), 3);
        overworld.setBlock(entrance,
            ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);

        BlockPos source = new BlockPos(32, Math.min(sift.getMaxY() - 8, 160), 32);
        sift.getChunk(source);
        sift.setBlock(source.below(), Blocks.STONE.defaultBlockState(), 3);
        for (int y = 0; y < 3; y++) sift.setBlock(source.above(y), Blocks.AIR.defaultBlockState(), 3);
        Cow traveler = EntityType.COW.create(sift, EntitySpawnReason.COMMAND);
        helper.assertTrue(traveler != null, "Cannot create traveler in the real Sift dimension");
        traveler.setPos(source.getX() + 0.5, source.getY(), source.getZ() + 0.5);
        helper.assertTrue(sift.addFreshEntity(traveler), "Cannot place traveler in Sift");
        Cow passenger = EntityType.COW.create(sift, EntitySpawnReason.COMMAND);
        helper.assertTrue(passenger != null, "Cannot create a passenger in the Sift dimension");
        passenger.setPos(traveler.getX(), traveler.getY(), traveler.getZ());
        helper.assertTrue(sift.addFreshEntity(passenger), "Cannot add passenger to Sift");
        helper.assertTrue(passenger.startRiding(traveler, true, false), "Passenger could not mount the test vehicle");
        UUID id = traveler.getUUID();
        UUID passengerId = passenger.getUUID();
        SiftWorldStorage.ReturnPoint recorded = new SiftWorldStorage.ReturnPoint(
                entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5,
                30.0F, 0.0F, entrance
        );
        SiftWorldStorage.saveReturnPoint(server, id, recorded);
        SiftWorldStorage.saveReturnPoint(server, passengerId, recorded);
        long before = RiftDirectory.loaded().stream().filter(rift -> rift.level() == overworld).count();
        helper.assertTrue(attemptMainReturn(server, traveler, helper), "Main-portal return refused the cross-dimensional transfer");
        Entity returned = overworld.getEntityInAnyDimension(id);
        helper.assertTrue(returned != null && returned.level() == overworld, "Main-portal traveler stayed in Sift");
        helper.assertTrue(returned.position().distanceToSqr(
            entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5) <= 80.0,
            "Main portal routed traveler away from the recorded entrance");
        helper.assertTrue(!riftTransferQueued(id, helper), "Main portal scheduled an unrelated Rift");
        helper.assertTrue(RiftDirectory.loaded().stream().filter(rift -> rift.level() == overworld).count() == before,
            "Main return created an unwanted Rift");
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, id).isEmpty(), "One-shot ticket not cleared after successful return");
        Entity arrivedPassenger = overworld.getEntityInAnyDimension(passengerId);
        helper.assertTrue(arrivedPassenger != null && arrivedPassenger.level() == overworld,
            "Attached passenger did not travel with the portal vehicle");
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, passengerId).isEmpty(),
            "Passenger one-shot return ticket remained active after successful transfer");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, id).orElseThrow().portalBlock().equals(entrance),
            "Successful return erased the durable main-portal entrance");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, passengerId).orElseThrow().portalBlock().equals(entrance),
            "Successful transfer erased the passenger's durable main-portal record");
        TheSiftMod.LOGGER.info(
            "PRIME2_T11_MAIN_RETURN_PASS entity={} passenger={} recordedPortal={} realDestination={} durable=true riftCountUnchanged=true",
            id, passengerId, entrance, returned.position()
        );
        arrivedPassenger.discard();
        returned.discard();
        helper.succeed();
    }

    static void vehicleUsesPassengerMainEntranceWhenRootHasNoTicket(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "The real Sift dimension is not loaded");
        BlockPos entrance = helper.absolutePos(CENTER.east(2));
        overworld.setBlock(entrance.below(), Blocks.STONE.defaultBlockState(), 3);
        overworld.setBlock(entrance,
            ModBlocks.SIFT_PORTAL.defaultBlockState().setValue(SiftPortalBlock.ACTIVE, true), 3);
        BlockPos site = new BlockPos(48, Math.min(160, sift.getMaxY() - 8), 48);
        sift.getChunk(site);
        sift.setBlock(site.below(), Blocks.STONE.defaultBlockState(), 3);
        for (int y = 0; y < 3; y++) sift.setBlock(site.above(y), Blocks.AIR.defaultBlockState(), 3);
        Cow vehicle = EntityType.COW.create(sift, EntitySpawnReason.COMMAND);
        Cow rider = EntityType.COW.create(sift, EntitySpawnReason.COMMAND);
        helper.assertTrue(vehicle != null && rider != null, "Cannot construct test vehicle and passenger");
        vehicle.setPos(site.getX() + 0.5, site.getY(), site.getZ() + 0.5);
        rider.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        helper.assertTrue(sift.addFreshEntity(vehicle) && sift.addFreshEntity(rider), "Cannot add travelers to Sift");
        helper.assertTrue(rider.startRiding(vehicle, true, false), "Rider failed to mount vehicle");
        UUID vehicleId = vehicle.getUUID(), riderId = rider.getUUID();
        SiftWorldStorage.saveReturnPoint(server, riderId, new SiftWorldStorage.ReturnPoint(
            entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5, 90.0F, 0.0F, entrance
        ));
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, vehicleId).isEmpty(),
            "Vehicle fixture must have no stored main entrance");
        helper.assertTrue(attemptMainReturn(server, vehicle, helper),
            "Vehicle failed to return using the passenger's recorded entrance");
        Entity movedVehicle = overworld.getEntityInAnyDimension(vehicleId);
        Entity movedRider = overworld.getEntityInAnyDimension(riderId);
        helper.assertTrue(movedVehicle != null && movedRider != null
            && movedVehicle.level() == overworld && movedRider.level() == overworld,
            "Vehicle return separated or lost a passenger");
        helper.assertTrue(movedVehicle.position().distanceToSqr(
            entrance.getX() + 0.5, entrance.getY(), entrance.getZ() + 0.5) <= 80.0,
            "Vehicle ignored the passenger's recorded portal and used world spawn");
        helper.assertTrue(SiftWorldStorage.getReturnPoint(server, riderId).isEmpty(),
            "Rider's one-shot ticket was not consumed after successful travel");
        helper.assertTrue(SiftWorldStorage.getMainPortalEntrance(server, riderId).orElseThrow()
            .portalBlock().equals(entrance), "Rider's durable main-portal entrance disappeared");
        TheSiftMod.LOGGER.info("PRIME2_T11_RIDER_RETURN_PASS vehicle={} rider={} entrance={} arrived={}",
            vehicleId, riderId, entrance, movedVehicle.position());
        movedRider.discard();
        movedVehicle.discard();
        helper.succeed();
    }

    static void realSiftRiftReturnMakesMatchedOverworldCounterpart(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "The real Sift dimension is missing");
        int x = 8, z = 8;
        BlockPos site = new BlockPos(x, 0, z);
        for (ServerLevel level : new ServerLevel[] {overworld, sift}) {
            level.getChunk(site);
            int y = Math.min(level.getMaxY() - 8,
                Math.max(level.getHeight(Types.MOTION_BLOCKING, x, z) + 8, level.getSeaLevel() + 8));
            for (int a = x - 4; a <= x + 4; a++) {
                for (int b = z - 4; b <= z + 4; b++) {
                    level.setBlock(new BlockPos(a, y, b), Blocks.STONE.defaultBlockState(), 3);
                    for (int delta = 1; delta <= 6; delta++) {
                        level.setBlock(new BlockPos(a, y + delta, b), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            if (level == sift) site = new BlockPos(x, y + 1, z);
        }
        int overworldY = overworld.getHeight(Types.MOTION_BLOCKING, x, z);
        RiftEntity siftRift = new RiftEntity(ModEntities.RIFT, sift);
        siftRift.configure(false, true, sift.getGameTime() + 6000L, UUID.randomUUID());
        siftRift.setPos(x + 0.5, site.getY(), z + 0.5);
        siftRift.setLinkedPos(new BlockPos(x, overworldY, z), overworld.getGameTime() + 6000L);
        helper.assertTrue(sift.addFreshEntity(siftRift), "Sift source Rift could not be added");
        Cow traveler = EntityType.COW.create(sift, EntitySpawnReason.COMMAND);
        helper.assertTrue(traveler != null, "Cannot construct Sift traveler");
        traveler.setPos(siftRift.getX(), siftRift.getY(), siftRift.getZ());
        helper.assertTrue(sift.addFreshEntity(traveler), "Cannot add Sift traveler");
        UUID id = traveler.getUUID();
        queueRift(traveler, siftRift, helper);
        RiftManager.tick(server);
        Entity returned = overworld.getEntityInAnyDimension(id);
        helper.assertTrue(returned != null && returned.level() == overworld,
            "Sift Rift did not teleport its traveler into the Overworld");
        RiftEntity counterpart = RiftDirectory.loaded().stream().filter(rift ->
            rift.level() == overworld && rift.getPairId().equals(siftRift.getPairId())
                && rift.targetsSift() && !rift.isRemoved()).findFirst().orElse(null);
        helper.assertTrue(counterpart != null, "No actual Overworld Rift paired to Sift source");
        helper.assertTrue(siftRift.getAnchorPos().equals(counterpart.getLinkedPos())
            && counterpart.getAnchorPos().equals(siftRift.getLinkedPos()),
            "Cross-dimension Rift links were not made reciprocal");
        helper.assertTrue(SiftWorldStorage.getRiftRoute(server, id, counterpart.getUUID(), overworld.getGameTime())
            .orElseThrow().equals(siftRift.getAnchorPos()), "Missing durable Rift-route ticket for successful traveler");
        TheSiftMod.LOGGER.info(
            "PRIME2_T11_BIDIR_PASS source={} overworldExit={} pairId={} traveler={} routesPersisted=true",
            siftRift.getAnchorPos(), counterpart.getAnchorPos(), siftRift.getPairId(), id
        );
        returned.discard();
        siftRift.discard();
        counterpart.discard();
        helper.succeed();
    }

    private static BlockPos destinationOfIntoSift(
            MinecraftServer server, Entity traveler, RiftEntity origin, GameTestHelper helper) {
        try {
            Method method = RiftManager.class.getDeclaredMethod("intoSift", MinecraftServer.class, Entity.class, RiftEntity.class);
            method.setAccessible(true);
            Object destination = method.invoke(null, server, traveler, origin);
            if (destination == null) {
                return null;
            }
            Method pos = destination.getClass().getDeclaredMethod("pos");
            pos.setAccessible(true);
            net.minecraft.world.phys.Vec3 center = (net.minecraft.world.phys.Vec3)pos.invoke(destination);
            return BlockPos.containing(center);
        } catch (ReflectiveOperationException error) {
            throw helper.assertionException("Could not invoke actual Rift-to-Sift routing: " + error);
        }
    }

    static void cancelRiftSpawn(EntityJoinLevelEvent event) {
        if (rejectNextRiftSpawn && event.getEntity() instanceof RiftEntity) {
            rejectNextRiftSpawn = false;
            event.setCanceled(true);
        }
    }

    private static void queueRift(Entity traveler, RiftEntity origin, GameTestHelper helper) {
        try {
            Method queue = RiftManager.class.getDeclaredMethod("queue", Entity.class, RiftEntity.class, boolean.class, boolean.class);
            queue.setAccessible(true);
            queue.invoke(null, traveler, origin, false, true);
        } catch (ReflectiveOperationException error) {
            throw helper.assertionException("Cannot call native Rift queue: " + error);
        }
    }

    private static Cow traveler(GameTestHelper helper, BlockPos pos) {
        Cow cow = EntityType.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(cow != null, "vanilla Cow test fixture could not be constructed");
        cow.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // The tests must not add the Cow to a chunk: this prevents automatic
        // portal contact processing from masking the return routing decision.
        return cow;
    }

    private static void buildReinforcedPortalFrame(ServerLevel level, BlockPos base, BlockPos hole) {
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 5; y++) {
                BlockPos cell = base.offset(x, y, 0);
                boolean border = x == 0 || x == 3 || y == 0 || y == 4;
                level.setBlock(cell, (border ? Blocks.REINFORCED_DEEPSLATE : cell.equals(hole)
                    ? Blocks.AIR : ModBlocks.SIFT_PORTAL).defaultBlockState(), 3);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<BlockPos> adjacentMainPortal(ServerLevel level, BlockPos saved, GameTestHelper helper) {
        try {
            Method method = SiftTeleportManager.class.getDeclaredMethod("findActiveEntranceNearby", ServerLevel.class, BlockPos.class);
            method.setAccessible(true);
            return (Optional<BlockPos>) method.invoke(null, level, saved);
        } catch (ReflectiveOperationException error) {
            throw helper.assertionException("Cannot inspect frame-verified saved entrance recovery: " + error);
        }
    }

    private static boolean attemptMainReturn(MinecraftServer server, Entity entity, GameTestHelper helper) {
        try {
            Method method = SiftTeleportManager.class.getDeclaredMethod("teleportBackToOverworld", MinecraftServer.class, Entity.class);
            method.setAccessible(true);
            return Boolean.TRUE.equals(method.invoke(null, server, entity));
        } catch (ReflectiveOperationException error) {
            throw helper.assertionException("Cannot exercise actual main-portal return: " + error);
        }
    }

    private static boolean riftTransferQueued(UUID entityId, GameTestHelper helper) {
        try {
            Field field = RiftManager.class.getDeclaredField("PENDING");
            field.setAccessible(true);
            return ((Map<?, ?>)field.get(null)).containsKey(entityId);
        } catch (ReflectiveOperationException error) {
            throw helper.assertionException("Cannot inspect pending Rift transfers: " + error);
        }
    }
}
