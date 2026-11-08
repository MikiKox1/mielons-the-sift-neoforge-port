package mielon.thesift.worldgen;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import mielon.thesift.compat.SoulshaleIds;
import mielon.thesift.map.SiftMapEncoding;
import mielon.thesift.world.SiftTideTransition;
import mielon.thesift.world.SiftTides;
import net.minecraft.resources.Identifier;

/** Runs on a plain classpath, separate from the GameTest mod's Java module. */
public final class PortCoreContractTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        tides();
        transitions();
        mapColors();
        idMigration();
        terrainPositionSet();
        System.out.println("PASS: core contracts; assertions=" + checks);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void tides() {
        require(SiftTides.TIDE_TICKS == 8400, "tide length");
        require(SiftTides.PERIOD_TICKS == 25200, "cycle length");
        require(SiftTides.TRANSITION_TICKS == 60, "blend duration");
        long[] samples = {Long.MIN_VALUE, -25201, -25200, -16800, -1, 0, 1, 8339, 8340,
                8399, 8400, 16739, 16740, 16799, 16800, 25139, 25140, 25199, 25200, Long.MAX_VALUE};
        for (long tick : samples) {
            int phase = (int) (Math.floorMod(tick, 25200L) / 8400);
            require(SiftTides.tide(tick).ordinal() == phase, "phase at " + tick);
            require(SiftTides.canSleep(tick, false) == (phase == 2), "sleep without storm at " + tick);
            require(SiftTides.canSleep(tick, true), "storm sleep at " + tick);
        }
        for (int tick = -25200; tick <= 50400; tick++) {
            float thrive = SiftTides.thriveBlend(tick);
            float flow = SiftTides.flowBlend(tick);
            float endure = SiftTides.nightBlend(tick);
            require(thrive >= 0 && thrive <= 1 && flow >= 0 && flow <= 1 && endure >= 0 && endure <= 1,
                    "blend range at " + tick);
            require(Math.abs(thrive + flow + endure - 1.0f) < 0.00001f, "blend conservation at " + tick);
        }
        require(SiftTides.thriveBlend(8340) == 1.0f, "transition start");
        require(Math.abs(SiftTides.thriveBlend(8370) - 0.5f) < 0.00001f, "transition midpoint");
        require(SiftTides.flowBlend(8400) == 1.0f, "transition complete");
        System.out.println("PASS: tides, sleep, negative/large timestamps, full-cycle blends");
    }

    private static void transitions() {
        long[] points = {-25201, -1, 0, 4000, 8339, 8340, 8370, 8399, 8400,
                16740, 16800, 25140, 25199, 25200, 1000000};
        for (long from : points) {
            for (long to : points) {
                SiftTideTransition transition = new SiftTideTransition(from, to);
                int advances = 0;
                while (!transition.done() && advances++ < 91) transition.advance();
                require(transition.done(), "transition termination " + from + " -> " + to);
                require(advances <= 90, "shortest transition path");
                require(transition.time() == to, "exact target timestamp");
                require(transition.advance() == to && transition.done(), "finished transition stable");
            }
        }
        System.out.println("PASS: command-time transitions, termination and exact targets");
    }

    private static void mapColors() throws IOException {
        require(SiftMapEncoding.encode(new int[0]).length == 0, "empty encoding");
        require(SiftMapEncoding.decode(new byte[0]).length == 0, "empty decoding");
        Random random = new Random(26022000L);
        for (int type = 0; type < 3; type++) {
            int[] colors = new int[128 * 128];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = type == 0 ? 0 : type == 1 ? i * 0x10203 : random.nextInt();
            }
            byte[] packet = SiftMapEncoding.encode(colors);
            require(packet.length <= SiftMapEncoding.MAX_PACKET_BYTES, "legal encoded packet size");
            require(Arrays.equals(colors, SiftMapEncoding.decode(packet)), "lossless color round trip " + type);
        }
        try {
            SiftMapEncoding.encode(new int[16383]);
            throw new AssertionError("short layer accepted");
        } catch (IllegalArgumentException expected) { checks++; }
        expectDecodeFailure(new byte[SiftMapEncoding.MAX_PACKET_BYTES + 1], "oversized packet");
        expectDecodeFailure(new byte[] {1, 2, 3, 4}, "malformed compressed packet");
        for (int count : new int[] {1, 16383, 16385}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(new DeflaterOutputStream(bytes))) {
                for (int i = 0; i < count; i++) output.writeInt(i);
            }
            expectDecodeFailure(bytes.toByteArray(), "decompressed layer length " + count);
        }
        System.out.println("PASS: map payload round trips, size limits and malformed/truncated data");
    }

    private static void expectDecodeFailure(byte[] packet, String label) {
        try {
            SiftMapEncoding.decode(packet);
            throw new AssertionError("Invalid map data accepted: " + label);
        } catch (IOException expected) { checks++; }
    }

    private static void idMigration() {
        require(SoulshaleIds.upgrade(null) == null, "null identifier");
        Identifier old = Identifier.fromNamespaceAndPath("the_sift", "polished_grimstone_slab");
        Identifier migrated = SoulshaleIds.upgrade(old);
        require(migrated.toString().equals("the_sift:polished_soulshale_slab"), "old ID migration");
        require(SoulshaleIds.upgrade(migrated) == migrated, "idempotent migration");
        Identifier foreign = Identifier.fromNamespaceAndPath("other_mod", "grimstone");
        require(SoulshaleIds.upgrade(foreign) == foreign, "foreign namespace untouched");
        Identifier vanilla = Identifier.withDefaultNamespace("stone");
        require(SoulshaleIds.upgrade(vanilla) == vanilla, "vanilla ID untouched");
        System.out.println("PASS: Soulshale aliases, namespace isolation and idempotence");
    }

    private static void terrainPositionSet() {
        SiftPackedLongSet actual = new SiftPackedLongSet(2);
        Set<Long> expected = new HashSet<>();
        long[] boundaries = {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE};
        for (long key : boundaries) {
            require(actual.add(key) == expected.add(key), "boundary insertion");
            require(!actual.add(key), "duplicate boundary insertion");
        }
        Random random = new Random(1122000L);
        for (int i = 0; i < 24000; i++) {
            long key = i % 5 == 0 ? random.nextLong() : random.nextInt(1000) - 500;
            switch (random.nextInt(3)) {
                case 0 -> require(actual.add(key) == expected.add(key), "set insertion " + i);
                case 1 -> require(actual.remove(key) == expected.remove(key), "set deletion " + i);
                default -> require(actual.contains(key) == expected.contains(key), "set lookup " + i);
            }
        }
        Set<Long> visited = new HashSet<>();
        actual.forEach(value -> require(visited.add(value), "duplicate during iteration"));
        require(visited.equals(expected), "set contents after rehash/deletions");
        for (long key : expected) require(actual.remove(key), "remove remaining key");
        actual.forEach(value -> { throw new AssertionError("nonempty cleared set"); });
        require(!actual.contains(0) && !actual.remove(0), "zero key removed");
        System.out.println("PASS: terrain position set versus HashSet, collisions, rehash, deletion, zero");
    }
}
