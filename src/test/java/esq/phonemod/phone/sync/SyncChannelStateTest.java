package esq.phonemod.phone.sync;

import esq.phonemod.phone.apps.groupchat.GroupChatState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * State-machine invariants of {@link SyncChannel} independent of disk: the monotonic
 * {@code markSaved} guard, snapshot sequence/hash semantics, and thread-safe snapshot
 * sequencing under the per-channel lock. Uses a real persistent {@link SyncChannelType}
 * backed by {@link GroupChatState}'s codec — no Universe, no Mockito.
 */
final class SyncChannelStateTest {

    private static final SyncChannelType<GroupChatState> TYPE = SyncChannelType.persistent(
            "test_chat", GroupChatState.CODEC, GroupChatState::new, s -> s.id);

    private static SyncChannel<GroupChatState> newChannel(String id) {
        return new SyncChannel<>(TYPE, id, new GroupChatState(id));
    }

    @Test
    void markSavedIgnoresAnOlderSequenceSoTheLatestSavedHashSurvives() {
        SyncChannel<GroupChatState> channel = newChannel("g1");

        channel.markSaved(2, "h2");
        // A late-arriving lower-sequence save (guard at SyncChannel.java:103) must not
        // clobber the newer recorded hash.
        channel.markSaved(1, "h1");

        assertEquals("h2", channel.savedHash());
    }

    @Test
    void snapshotSequencesStrictlyIncreaseWhileIdenticalStateHashesAreEqual() {
        SyncChannel<GroupChatState> channel = newChannel("g1");

        SyncChannel.Snapshot first = channel.snapshot();
        SyncChannel.Snapshot second = channel.snapshot();

        assertTrue(second.sequence() > first.sequence(),
                "each snapshot must mint a strictly higher sequence");
        assertEquals(first.hash(), second.hash(),
                "unchanged state must produce an identical content hash");
    }

    @Test
    void mutatingStateChangesTheSnapshotHash() {
        SyncChannel<GroupChatState> channel = newChannel("g1");
        String before = channel.snapshot().hash();

        channel.mutate(state -> state.name = "renamed");

        assertNotEquals(before, channel.snapshot().hash());
    }

    @Test
    void concurrentSnapshotsMintUniqueContiguousSequencesUnderTheLock() throws Exception {
        SyncChannel<GroupChatState> channel = newChannel("g1");
        int threads = 16;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Long> sequences = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    barrier.await(5, TimeUnit.SECONDS);
                    sequences.add(channel.snapshot().sequence());
                    return null;
                });
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "all snapshot tasks finished");
        } finally {
            pool.shutdownNow();
        }

        Set<Long> unique = Set.copyOf(sequences);
        assertEquals(threads, unique.size(), "every snapshot sequence is unique (no torn increment)");
        assertEquals(
                IntStream.rangeClosed(1, threads).mapToObj(i -> (long) i).collect(Collectors.toSet()),
                unique,
                "sequences are contiguous 1..N");
    }
}
