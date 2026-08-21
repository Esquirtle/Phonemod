package esq.phonemod.phone.messaging;

import com.hypixel.hytale.server.core.universe.StorageManager;
import com.hypixel.hytale.server.core.util.BsonUtil;
import com.hypixel.hytale.server.core.util.Config;
import esq.phonemod.PhoneMod;
import esq.phonemod.config.PhonemodConfig;
import esq.phonemod.operations.OperationalMetrics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ordered-save and durability behavior of {@link OfflineMailboxStore}, driven with a real
 * {@code new StorageManager(() -> false)} + {@code @TempDir} through the injected
 * {@link OfflineMailboxStore.BsonWriter} seam.
 *
 * <p>{@code enqueue}/{@code prune} read {@link PhonemodConfig} through the live
 * {@link PhoneMod} instance, which — as {@code MigrationComponentTest} notes at its lines
 * 43-45 — is normally unavailable in a unit test. Rather than boot a plugin, install a
 * config-only {@link PhoneMod} double once via the cited {@link Config#preloadedConfig}.
 */
final class OfflineMailboxOrderedSaveTest {

    private static final String ADDRESS = "555-2000";

    @BeforeAll
    static void installPhoneModConfig() throws Exception {
        if (PhoneMod.getInstance() != null) {
            return;
        }
        sun.misc.Unsafe unsafe = unsafe();
        PhoneMod plugin = (PhoneMod) unsafe.allocateInstance(PhoneMod.class);
        Config<PhonemodConfig> config = Config.preloadedConfig(
                Path.of("."), "phonemod", PhonemodConfig.CODEC, new PhonemodConfig());
        Field configField = PhoneMod.class.getDeclaredField("config");
        unsafe.putObject(plugin, unsafe.objectFieldOffset(configField), config);
        Field instanceField = PhoneMod.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, plugin);
    }

    private static StorageManager unlocked() {
        return new StorageManager(() -> false);
    }

    private static TextMessage message(String body) {
        return new TextMessage("555-1000", "Sender", body);
    }

    @Test
    void concurrentEnqueuesToOneAddressPersistTheFinalOrderedListIntact(@TempDir Path dir)
            throws Exception {
        OfflineMailboxStore store = new OfflineMailboxStore();
        store.init(dir, unlocked(), BsonUtil::writeDocument);

        int count = 6;
        long now = System.currentTimeMillis();
        CyclicBarrier barrier = new CyclicBarrier(count);
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            for (int i = 0; i < count; i++) {
                String body = "m" + i;
                pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    store.enqueue(ADDRESS, message(body), now);
                    return null;
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // flush() joins the per-address pending chain (:107) so all writes have landed.
        store.flush();

        List<String> inMemory = bodies(store.peek(ADDRESS));
        assertEquals(count, inMemory.size(), "every concurrent enqueue is retained");

        // Reload from disk: the persisted list must match the in-memory order exactly,
        // proving the self-chaining save wrote the final ordered list without interleaving.
        OfflineMailboxStore reloaded = new OfflineMailboxStore();
        reloaded.init(dir, unlocked(), BsonUtil::writeDocument);
        assertEquals(inMemory, bodies(reloaded.peek(ADDRESS)),
                "on-disk order equals the final in-memory order");
    }

    @Test
    void aFailingWriterCountsASaveFailureWithoutCorruptingTheInMemoryBox(@TempDir Path dir) {
        OfflineMailboxStore store = new OfflineMailboxStore();
        store.init(dir, unlocked(),
                (path, document) -> CompletableFuture.failedFuture(new IOException("boom")));

        long before = OperationalMetrics.get().snapshot().saveFailures();
        store.enqueue(ADDRESS, message("hi"), System.currentTimeMillis());

        awaitUntil(() -> OperationalMetrics.get().snapshot().saveFailures() > before, 5000);
        assertEquals(before + 1, OperationalMetrics.get().snapshot().saveFailures());

        // The failed persist must not drop the message from memory.
        assertEquals(List.of("hi"), bodies(store.peek(ADDRESS)));
    }

    @Test
    void flushJoinsEveryPendingWrite(@TempDir Path dir) throws Exception {
        OfflineMailboxStore store = new OfflineMailboxStore();
        store.init(dir, unlocked(), BsonUtil::writeDocument);

        long now = System.currentTimeMillis();
        store.enqueue(ADDRESS, message("one"), now);
        store.enqueue(ADDRESS, message("two"), now);

        store.flush();

        OfflineMailboxStore reloaded = new OfflineMailboxStore();
        reloaded.init(dir, unlocked(), BsonUtil::writeDocument);
        assertEquals(List.of("one", "two"), bodies(reloaded.peek(ADDRESS)),
                "flush() must not return before pending writes have persisted");
    }

    private static List<String> bodies(List<TextMessage> messages) {
        List<String> result = new ArrayList<>();
        for (TextMessage message : messages) {
            result.add(message.body());
        }
        return result;
    }

    private static void awaitUntil(BooleanSupplier condition, long millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition not met within " + millis + "ms");
            }
            Thread.yield();
        }
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }
}
