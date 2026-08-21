package esq.phonemod.phone.sync;

import com.hypixel.hytale.server.core.universe.StorageManager;
import com.hypixel.hytale.server.core.util.BsonUtil;
import esq.phonemod.operations.OperationalMetrics;
import esq.phonemod.phone.apps.groupchat.GroupChatState;
import org.bson.BsonDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * On-disk behavior of {@link ChannelStore} and the bounded shutdown flush, driven with a
 * real {@code new StorageManager(() -> false)} + {@code @TempDir} and no Universe — the
 * exact seam precedent of {@code IdentityRegistryPersistenceTest}. Failure and hang cases
 * use an injected {@link ChannelStore.BsonWriter}; ordering cases gate the writer so the
 * assertions stay deterministic (no sleeps).
 */
final class ChannelStorePersistenceTest {

    private static final SyncChannelType<GroupChatState> TYPE = SyncChannelType.persistent(
            "test_chat", GroupChatState.CODEC, GroupChatState::new, s -> s.id);

    private static StorageManager unlocked() {
        return new StorageManager(() -> false);
    }

    private static ChannelStore realStore(Path dir) {
        return new ChannelStore(dir, unlocked(), BsonUtil::writeDocument);
    }

    private static SyncChannel<GroupChatState> channel(String id) {
        return new SyncChannel<>(TYPE, id, new GroupChatState(id));
    }

    @AfterEach
    void resetSyncServiceStatics() throws Exception {
        setSyncStore(null);
        clearStaticMap("TYPES");
        clearStaticMap("CHANNELS");
    }

    // ── dirty gate & round-trip ────────────────────────────────────────────────

    @Test
    void anUnchangedStateSavesNothingAfterTheFirstSave(@TempDir Path dir) throws Exception {
        ChannelStore store = realStore(dir);
        SyncChannel<GroupChatState> ch = channel("g1");
        ch.mutate(s -> s.name = "hi");

        CompletableFuture<ChannelStore.SaveResult> first = store.save(ch);
        assertNotNull(first, "first save of a dirty channel writes");
        ChannelStore.SaveResult result = first.get(5, TimeUnit.SECONDS);
        ch.markSaved(result.sequence(), result.hash());

        assertNull(store.save(ch), "a second save of unchanged state is gated out");
    }

    @Test
    void saveThenLoadRoundTripsIdenticalStateToTheHashedPath(@TempDir Path dir) throws Exception {
        ChannelStore store = realStore(dir);
        SyncChannel<GroupChatState> ch = channel("g1");
        ch.mutate(s -> s.name = "hello");

        ChannelStore.SaveResult result = store.save(ch).get(5, TimeUnit.SECONDS);
        ch.markSaved(result.sequence(), result.hash());

        Path file = dir.resolve("channels").resolve("test_chat")
                .resolve(Hashing.sha256Hex("g1") + ".json");
        assertTrue(Files.exists(file), "file lands under channels/<typeId>/<sha>.json");

        GroupChatState loaded = store.loadOne(TYPE, "g1");
        assertNotNull(loaded);
        assertEquals("g1", loaded.id);
        assertEquals("hello", loaded.name);
    }

    // ── serialization ──────────────────────────────────────────────────────────

    @Test
    void concurrentSameIdSavesSerializeSoTheHigherSequenceSnapshotWinsOnDisk(@TempDir Path dir)
            throws Exception {
        GatedWriter writer = new GatedWriter();
        ChannelStore store = new ChannelStore(dir, unlocked(), writer);
        SyncChannel<GroupChatState> ch = channel("g1");

        ch.mutate(s -> s.name = "A");
        CompletableFuture<ChannelStore.SaveResult> first = store.save(ch);   // seq 1, writes "A"
        assertNotNull(first);
        ch.mutate(s -> s.name = "B");
        CompletableFuture<ChannelStore.SaveResult> second = store.save(ch);  // seq 2, queued behind first
        assertNotNull(second);

        // Only "A" has been written; the store serializes "B" behind the in-flight "A".
        CompletableFuture<Void> gateA = writer.gates.poll(5, TimeUnit.SECONDS);
        assertNotNull(gateA);
        gateA.complete(null);                                                // release "A" → "B" runs
        CompletableFuture<Void> gateB = writer.gates.poll(5, TimeUnit.SECONDS);
        assertNotNull(gateB);
        gateB.complete(null);

        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);

        GroupChatState onDisk = store.loadOne(TYPE, "g1");
        assertNotNull(onDisk);
        assertEquals("B", onDisk.name, "the last (higher-sequence) write is the on-disk state");
    }

    @Test
    void concurrentDifferentIdSavesBothLand(@TempDir Path dir) throws Exception {
        ChannelStore store = realStore(dir);
        SyncChannel<GroupChatState> a = channel("a");
        a.mutate(s -> s.name = "AA");
        SyncChannel<GroupChatState> b = channel("b");
        b.mutate(s -> s.name = "BB");

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> fa = pool.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return store.save(a).get(5, TimeUnit.SECONDS);
            });
            Future<?> fb = pool.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return store.save(b).get(5, TimeUnit.SECONDS);
            });
            fa.get(10, TimeUnit.SECONDS);
            fb.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals("AA", store.loadOne(TYPE, "a").name);
        assertEquals("BB", store.loadOne(TYPE, "b").name);
    }

    // ── write failure / passive retry ──────────────────────────────────────────

    @Test
    void aFailedWriteCompletesExceptionallyLeavesTheChannelDirtyForRetry(@TempDir Path dir)
            throws Exception {
        ChannelStore store = new ChannelStore(dir, unlocked(),
                (path, document) -> CompletableFuture.failedFuture(new IOException("boom")));
        SyncChannel<GroupChatState> ch = channel("g1");
        ch.mutate(s -> s.name = "x");

        CompletableFuture<ChannelStore.SaveResult> future = store.save(ch);
        assertNotNull(future);
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

        assertNull(ch.savedHash(), "savedHash must not advance when the write fails");
        // Passive retry (decision 2): the same unchanged state is still dirty and re-saves.
        assertNotNull(store.save(ch), "unsaved state stays dirty and retries on the next save");
    }

    @Test
    void aFailedSyncSavePublishIncrementsTheSaveFailureMetric(@TempDir Path dir) throws Exception {
        // The saveFailure counter for channel saves is incremented by SyncService.persist
        // (SyncService.java:172), not ChannelStore — drive the failure through the facade with
        // an injected failing store (same-package reflection into the private static field).
        ChannelStore failing = new ChannelStore(dir, unlocked(),
                (path, document) -> CompletableFuture.failedFuture(new IOException("boom")));
        setSyncStore(failing);
        SyncService.registerType(TYPE);

        long before = OperationalMetrics.get().snapshot().saveFailures();
        SyncService.publish(TYPE, "gm", s -> s.name = "x");
        awaitUntil(() -> SyncService.pendingSaves() == 0, 5000);

        assertEquals(before + 1, OperationalMetrics.get().snapshot().saveFailures());
    }

    // ── THE FIX: bounded shutdown flush must not hang ───────────────────────────

    @Test
    void saveBlockingReturnsWithinTheDeadlineAndDoesNotMarkSavedWhenTheWriteNeverCompletes(
            @TempDir Path dir) {
        ChannelStore store = new ChannelStore(dir, unlocked(),
                (path, document) -> new CompletableFuture<>()); // never completes
        SyncChannel<GroupChatState> ch = channel("g1");
        ch.mutate(s -> s.name = "x");

        long deadline = System.nanoTime() + Duration.ofMillis(300).toNanos();
        assertTimeoutPreemptively(Duration.ofSeconds(3),
                () -> store.saveBlocking(ch, deadline),
                "saveBlocking must return once its deadline elapses, not hang on the write");

        assertNull(ch.savedHash(), "a timed-out flush must not mark the channel saved");
    }

    @Test
    void flushAllBoundsTheTotalShutdownDelayAcrossManyStuckChannels(@TempDir Path dir)
            throws Exception {
        ChannelStore never = new ChannelStore(dir, unlocked(),
                (path, document) -> new CompletableFuture<>()); // never completes
        setSyncStore(never);
        SyncService.registerType(TYPE);
        for (String id : new String[] {"c1", "c2", "c3"}) {
            SyncChannel<GroupChatState> ch = SyncService.getOrCreate(TYPE, id);
            ch.mutate(s -> s.name = "dirty-" + id);
        }

        // A shared deadline bounds the whole sweep to ~SHUTDOWN_FLUSH_TIMEOUT (5s). A naive
        // per-channel timeout would be 5s × 3 = 15s and blow this 10s preemptive bound.
        assertTimeoutPreemptively(Duration.ofSeconds(10), SyncService::flushAll,
                "flushAll must bound the total shutdown delay, not multiply it per channel");

        for (String id : new String[] {"c1", "c2", "c3"}) {
            assertNull(SyncService.get(TYPE, id).savedHash(),
                    "no stuck channel may be marked saved");
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** Writes the real bytes synchronously, then hands back a gate the test releases. */
    private static final class GatedWriter implements ChannelStore.BsonWriter {
        final BlockingQueue<CompletableFuture<Void>> gates = new LinkedBlockingQueue<>();

        @Override
        public CompletableFuture<Void> write(Path path, BsonDocument document) {
            BsonUtil.writeDocument(path, document).join();
            CompletableFuture<Void> gate = new CompletableFuture<>();
            gates.add(gate);
            return gate;
        }
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

    private static void setSyncStore(ChannelStore store) throws Exception {
        Field field = SyncService.class.getDeclaredField("store");
        field.setAccessible(true);
        field.set(null, store);
    }

    private static void clearStaticMap(String name) throws Exception {
        Field field = SyncService.class.getDeclaredField(name);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
    }
}
