import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.LongConsumer;

/** Executes original binary classes in isolated loaders, without game startup or mocks. */
public final class PureReferenceParity {
    private static long checks;

    private static void same(Object first, Object second, String label) {
        checks++;
        if (!Objects.deepEquals(first, second)) throw new AssertionError(label + ": " + first + " != " + second);
    }

    private static Method method(Class<?> type, String name, Class<?>... params) throws Exception {
        Method result = type.getDeclaredMethod(name, params);
        result.setAccessible(true);
        return result;
    }

    private static Object create(Class<?> type, Class<?>[] params, Object... args) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor(params);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    private static Class<?> load(URLClassLoader loader, String name) throws Exception {
        return Class.forName("mielon.thesift." + name, true, loader);
    }

    private static void tides(URLClassLoader a, URLClassLoader b) throws Exception {
        Class<?> x = load(a, "world.SiftTides"), y = load(b, "world.SiftTides");
        for (String name : new String[]{"TIDE_TICKS", "PERIOD_TICKS", "TRANSITION_TICKS"}) {
            same(x.getField(name).get(null), y.getField(name).get(null), "tide constant " + name);
        }
        for (String name : new String[]{"tide", "thriveBlend", "flowBlend", "nightBlend", "shaderTime"}) {
            Method ma = method(x, name, long.class), mb = method(y, name, long.class);
            for (long tick = -25200; tick <= 50400; tick++) {
                Object va = ma.invoke(null, tick), vb = mb.invoke(null, tick);
                same(va instanceof Enum<?> ? va.toString() : va,
                     vb instanceof Enum<?> ? vb.toString() : vb, name + " at " + tick);
            }
            for (long tick : new long[]{Long.MIN_VALUE, Long.MAX_VALUE, -1, 0}) {
                Object va = ma.invoke(null, tick), vb = mb.invoke(null, tick);
                same(va instanceof Enum<?> ? va.toString() : va,
                     vb instanceof Enum<?> ? vb.toString() : vb, name + " boundary " + tick);
            }
        }
        Method sleepA = method(x, "canSleep", long.class, boolean.class);
        Method sleepB = method(y, "canSleep", long.class, boolean.class);
        for (long tick : new long[]{-25201, -1, 0, 8400, 16800, 25200, Long.MAX_VALUE}) {
            for (boolean storm : new boolean[]{false, true}) {
                same(sleepA.invoke(null, tick, storm), sleepB.invoke(null, tick, storm), "sleep");
            }
        }
    }

    private static void transitions(URLClassLoader a, URLClassLoader b) throws Exception {
        Class<?> x = load(a, "world.SiftTideTransition"), y = load(b, "world.SiftTideTransition");
        Method xa = method(x, "advance"), ya = method(y, "advance");
        Method xd = method(x, "done"), yd = method(y, "done");
        long[] values = {-25201, -1, 0, 8339, 8340, 8399, 8400, 16800, 25199, 25200, 1000000};
        for (long from : values) for (long to : values) {
            Object first = create(x, new Class<?>[]{long.class, long.class}, from, to);
            Object second = create(y, new Class<?>[]{long.class, long.class}, from, to);
            for (int i = 0; i <= 91; i++) {
                same(xd.invoke(first), yd.invoke(second), "transition completion");
                same(xa.invoke(first), ya.invoke(second), "transition intermediate tick");
            }
            same(xd.invoke(first), true, "original transition terminates");
            same(yd.invoke(second), true, "port transition terminates");
        }
    }

    private static void maps(URLClassLoader a, URLClassLoader b) throws Exception {
        Class<?> x = load(a, "map.SiftMapEncoding"), y = load(b, "map.SiftMapEncoding");
        Method xe = method(x, "encode", int[].class), ye = method(y, "encode", int[].class);
        Method xd = method(x, "decode", byte[].class), yd = method(y, "decode", byte[].class);
        Random random = new Random(26122001);
        for (int mode = 0; mode < 4; mode++) {
            int[] values = new int[mode == 0 ? 0 : 16384];
            for (int i = 0; i < values.length; i++) values[i] = mode == 1 ? 0 : mode == 2 ? i : random.nextInt();
            byte[] first = (byte[]) xe.invoke(null, (Object) values);
            byte[] second = (byte[]) ye.invoke(null, (Object) values);
            same(first, second, "lossless encoding bytes");
            same(xd.invoke(null, (Object) first), yd.invoke(null, (Object) second), "decoded map colors");
        }
        for (byte[] invalid : new byte[][]{{1,2,3,4}, new byte[300000]}) {
            same(failure(xd, invalid), failure(yd, invalid), "map rejection semantics");
        }
    }

    private static String failure(Method decode, byte[] input) throws Exception {
        try { decode.invoke(null, (Object) input); return "accepted"; }
        catch (InvocationTargetException ex) { return ex.getCause().getClass().getName(); }
    }

    private static void positionSet(URLClassLoader a, URLClassLoader b) throws Exception {
        Class<?> x = load(a, "worldgen.SiftPackedLongSet"), y = load(b, "worldgen.SiftPackedLongSet");
        Object first = create(x, new Class<?>[]{int.class}, 2), second = create(y, new Class<?>[]{int.class}, 2);
        Method[] am = {method(x, "add", long.class), method(x, "remove", long.class), method(x, "contains", long.class)};
        Method[] bm = {method(y, "add", long.class), method(y, "remove", long.class), method(y, "contains", long.class)};
        Random random = new Random(26122001);
        for (int i = 0; i < 24000; i++) {
            long key = i % 5 == 0 ? random.nextLong() : random.nextInt(1000) - 500;
            int operation = random.nextInt(3);
            same(am[operation].invoke(first, key), bm[operation].invoke(second, key), "packed set operation");
        }
        Set<Long> va = new HashSet<>(), vb = new HashSet<>();
        method(x, "forEach", LongConsumer.class).invoke(first, (LongConsumer) va::add);
        method(y, "forEach", LongConsumer.class).invoke(second, (LongConsumer) vb::add);
        same(va, vb, "packed set final contents");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("original JAR, port classes/JAR, target compile classpath file required");
        ArrayList<URL> dependencies = new ArrayList<>();
        for (String path : Files.readAllLines(Path.of(args[2]))) {
            if (!path.isBlank()) dependencies.add(Path.of(path.replace("\uFEFF", "")).toUri().toURL());
        }
        ArrayList<URL> originalUrls = new ArrayList<>(), portUrls = new ArrayList<>();
        originalUrls.add(Path.of(args[0]).toUri().toURL()); originalUrls.addAll(dependencies);
        portUrls.add(Path.of(args[1]).toUri().toURL()); portUrls.addAll(dependencies);
        try (URLClassLoader original = new URLClassLoader(originalUrls.toArray(URL[]::new), null);
             URLClassLoader port = new URLClassLoader(portUrls.toArray(URL[]::new), null)) {
            tides(original, port); transitions(original, port); maps(original, port); positionSet(original, port);
        }
        System.out.println("PURE_ORIGINAL_BINARY_PARITY=PASS;CLASSES=4;CHECKS=" + checks);
    }
}
