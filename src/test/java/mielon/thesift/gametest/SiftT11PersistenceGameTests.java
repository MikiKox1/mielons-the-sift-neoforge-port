package mielon.thesift.gametest;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import mielon.thesift.TheSiftMod;
import mielon.thesift.entity.ModEntities;
import mielon.thesift.entity.RiftEntity;
import mielon.thesift.world.RiftManager;
import mielon.thesift.world.TheSiftDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;

/** Cross-process test of persisted Rift links and delayed entity-section loading. */
final class SiftT11PersistenceGameTests {
    private static final UUID PAIR = UUID.nameUUIDFromBytes(
        "prime2-t11-rift-pair-after-restart".getBytes(StandardCharsets.UTF_8));
    private static final int ORIGIN_X = 320;
    private static final int ORIGIN_Z = 320;
    private static final int DEST_X = 512;
    private static final int DEST_Z = 512;

    private SiftT11PersistenceGameTests() {
    }

    static void preparePersistedPair(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "Missing genuine Sift dimension");
        BlockPos ow = preparePlatform(overworld, ORIGIN_X, ORIGIN_Z);
        BlockPos sf = preparePlatform(sift, DEST_X, DEST_Z);

        for (ServerLevel level : new ServerLevel[] {overworld, sift}) {
            BlockPos anchor = level == overworld ? ow : sf;
            helper.assertTrue(level.getEntitiesOfClass(RiftEntity.class,
                new AABB(anchor).inflate(4.0), rift -> PAIR.equals(rift.getPairId())).isEmpty(),
                "Persistent pair was already present; do not silently overwrite test evidence");
        }
        RiftEntity source = new RiftEntity(ModEntities.RIFT, overworld);
        RiftEntity target = new RiftEntity(ModEntities.RIFT, sift);
        source.configure(true, true, overworld.getGameTime() + 60000L, PAIR);
        target.configure(false, true, sift.getGameTime() + 60000L, PAIR);
        source.setPos(ow.getX() + 0.5, ow.getY(), ow.getZ() + 0.5);
        target.setPos(sf.getX() + 0.5, sf.getY(), sf.getZ() + 0.5);
        source.setLinkedPos(sf, source.getExpiresAt());
        target.setLinkedPos(ow, target.getExpiresAt());
        source.addTag("prime2_t11_saved_pair");
        target.addTag("prime2_t11_saved_pair");
        helper.assertTrue(overworld.addFreshEntity(source) && sift.addFreshEntity(target),
            "Could not add the complete Rift pair for the save/restart fixture");
        TheSiftMod.LOGGER.info("PRIME2_T11_PERSIST_PREP_PASS pair={} source={} target={}", PAIR, ow, sf);
        helper.succeed();
    }

    static void verifyPairAfterFullServerRestart(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel overworld = server.overworld();
        ServerLevel sift = server.getLevel(TheSiftDimension.LEVEL_KEY);
        helper.assertTrue(sift != null, "Sift dimension missing after restart");
        AtomicBoolean queued = new AtomicBoolean();
        UUID[] travelerId = new UUID[1];
        BlockPos sourcePos = new BlockPos(ORIGIN_X, 100, ORIGIN_Z);
        // GameTest runs far from the persisted Rift. Loading only block
        // chunks does not make the source entity section simulation-ready.
        overworld.getChunkSource().addTicketWithRadius(
            TicketType.PORTAL, new ChunkPos(ORIGIN_X >> 4, ORIGIN_Z >> 4), 3);
        overworld.getChunk(sourcePos);
        helper.succeedWhen(() -> {
            if (!overworld.areEntitiesLoaded(new ChunkPos(ORIGIN_X >> 4, ORIGIN_Z >> 4).pack())) {
                throw helper.assertionException("Waiting for saved Overworld Rift entity section");
            }
            RiftEntity source = overworld.getEntitiesOfClass(RiftEntity.class, new AABB(
                ORIGIN_X - 4, overworld.getMinY(), ORIGIN_Z - 4,
                ORIGIN_X + 5, overworld.getMaxY(), ORIGIN_Z + 5),
                rift -> PAIR.equals(rift.getPairId()) && rift.targetsSift()).stream().findFirst().orElse(null);
            helper.assertTrue(source != null, "The original Rift was lost during the full server restart");

            if (queued.compareAndSet(false, true)) {
                Cow cow = EntityType.COW.create(overworld, EntitySpawnReason.COMMAND);
                helper.assertTrue(cow != null, "Cannot construct the restarted pair's traveler");
                cow.setPos(source.getX(), source.getY(), source.getZ());
                cow.setNoAi(true);
                helper.assertTrue(overworld.addFreshEntity(cow), "Cannot add the restarted pair's traveler");
                travelerId[0] = cow.getUUID();
                queue(cow, source, helper);
            }

            // The target Sift chunk was not preloaded by this test. The actual
            // RiftManager must keep the request pending until its entity
            // section is available, then use the already-saved counterpart.
            Entity moved = server.overworld().getEntityInAnyDimension(travelerId[0]);
            helper.assertTrue(moved != null && moved.level() == sift,
                "Rift transfer has not completed through the reloaded pair");
            BlockPos targetPos = source.getLinkedPos();
            helper.assertTrue(targetPos != null, "Source Rift lost the saved linked position");
            helper.assertTrue(sift.areEntitiesLoaded(new ChunkPos(DEST_X >> 4, DEST_Z >> 4).pack()),
                "Transfer completed before target Sift entity sections loaded");
            RiftEntity partner = sift.getEntitiesOfClass(RiftEntity.class, new AABB(
                DEST_X - 4, sift.getMinY(), DEST_Z - 4,
                DEST_X + 5, sift.getMaxY(), DEST_Z + 5),
                rift -> PAIR.equals(rift.getPairId()) && !rift.targetsSift()).stream().findFirst().orElse(null);
            helper.assertTrue(partner != null && partner.getAnchorPos().equals(targetPos),
                "Original saved counterpart was not found at its persisted anchor");
            helper.assertTrue(partner.getAnchorPos().equals(source.getLinkedPos())
                    && source.getAnchorPos().equals(partner.getLinkedPos()),
                "Saved Rift pair no longer has reciprocal links after restart");
            long count = sift.getEntitiesOfClass(RiftEntity.class, new AABB(
                DEST_X - 4, sift.getMinY(), DEST_Z - 4,
                DEST_X + 5, sift.getMaxY(), DEST_Z + 5),
                rift -> PAIR.equals(rift.getPairId())).size();
            helper.assertValueEqual((int)count, 1, "Async entity load duplicated the linked Rift");
            TheSiftMod.LOGGER.info(
                "PRIME2_T11_PERSIST_VERIFY_PASS pair={} loadedTarget={} count={} movedEntity={} source={}",
                PAIR, partner.getAnchorPos(), count, travelerId[0], source.getAnchorPos());
            moved.discard();
        });
    }

    private static BlockPos preparePlatform(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = Math.min(level.getMaxY() - 8,
            Math.max(level.getMinY() + 8, level.getHeight(Types.MOTION_BLOCKING, x, z) + 8));
        for (int a = x - 4; a <= x + 4; a++) {
            for (int b = z - 4; b <= z + 4; b++) {
                level.setBlock(new BlockPos(a, y, b), Blocks.STONE.defaultBlockState(), 3);
                for (int delta = 1; delta <= 6; delta++) {
                    level.setBlock(new BlockPos(a, y + delta, b), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        return new BlockPos(x, y + 1, z);
    }

    private static void queue(Entity traveler, RiftEntity source, GameTestHelper helper) {
        try {
            Method method = RiftManager.class.getDeclaredMethod(
                "queue", Entity.class, RiftEntity.class, boolean.class, boolean.class);
            method.setAccessible(true);
            method.invoke(null, traveler, source, false, true);
        } catch (ReflectiveOperationException error) {
            throw helper.assertionException("Cannot queue transfer via persisted Rift: " + error);
        }
    }
}
