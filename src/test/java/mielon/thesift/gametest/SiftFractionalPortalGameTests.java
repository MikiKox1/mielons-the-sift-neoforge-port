package mielon.thesift.gametest;

import java.util.Optional;
import mielon.thesift.world.SafePortalArrival;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** M263-17 backport: real player collision in a disposable underground-sized cell. */
final class SiftFractionalPortalGameTests {
    private SiftFractionalPortalGameTests() {}

    @SuppressWarnings("removal")
    static void fractionalUndergroundReturn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos local = helper.absolutePos(new BlockPos(5, 0, 5));
        BlockPos pocket = new BlockPos(local.getX(), 180, local.getZ());
        level.getChunk(pocket);

        // Solid shell: only a block-centered, exact-height player fits inside.
        // Outside the shell there is no floor at this altitude.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 9; dy++) {
                    level.setBlock(pocket.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(pocket, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pocket.above(), Blocks.AIR.defaultBlockState(), 3);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.snapTo(pocket.getX() + 0.5, pocket.getY() + 12, pocket.getZ() + 0.5);
            Vec3 exact = Vec3.atBottomCenterOf(pocket);
            Vec3 preferred = new Vec3(pocket.getX() + 0.96, pocket.getY() - 0.167, pocket.getZ() + 0.96);
            helper.assertTrue(SafePortalArrival.safe(level, player, exact),
                    "Bad fixture: the centered one-block pocket must admit a player");
            helper.assertTrue(!SafePortalArrival.safe(level, player, preferred),
                    "Bad fixture: fractional original position must intersect the shell");
            Optional<Vec3> target = SafePortalArrival.findNearRecordedPortal(level, player, preferred);
            helper.assertTrue(target.isPresent(),
                    "M263-17 backport regression: integer offsets from fractional coordinates miss the only valid pocket");
            helper.assertTrue(SafePortalArrival.safe(level, player, target.orElseThrow()),
                    "Selected underground return is not safe for the actual player");
            helper.assertTrue(target.orElseThrow().distanceToSqr(exact) < 0.01,
                    "Must land at the recorded underground cell, not on a roof or at the surface");
            helper.succeed();
        } finally {
            player.connection.disconnect(Component.literal("Prime2 fractional landing regression completed"));
        }
    }
}
