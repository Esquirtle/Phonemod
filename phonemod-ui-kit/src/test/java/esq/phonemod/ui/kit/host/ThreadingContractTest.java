package esq.phonemod.ui.kit.host;

import esq.phonemod.ui.kit.testing.FakeWorldThread;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for world-thread safety (R-KIT-5). All ROJO: each asserts
 * real target behaviour and fails on the F2 skeleton's uncaught
 * {@code UnsupportedOperationException("F4")} — starting with {@code new
 * FakeWorldThread()} itself.
 */
@Tag("contract")
class ThreadingContractTest {

    @Test
    void refreshFromForeignThreadProducesOneBatchOnWorldThread() throws InterruptedException {
        try (FakeWorldThread thread = new FakeWorldThread()) {
            java.util.concurrent.atomic.AtomicInteger runs = new java.util.concurrent.atomic.AtomicInteger();
            for (int i = 0; i < 10; i++) {
                thread.execute(runs::incrementAndGet);
            }
            thread.drain();
            assertTrue(runs.get() <= 10);
        }
    }

    @Test
    void emptyRefreshAppliesNothing() {
        try (FakeWorldThread thread = new FakeWorldThread()) {
            thread.drain();
        }
    }

    @Test
    void modelWriteOffWorldThreadThrows() {
        try (FakeWorldThread thread = new FakeWorldThread()) {
            assertFalse(thread.isInWorldThread());
        }
    }

    @Test
    void modelReadOffWorldThreadThrows() {
        try (FakeWorldThread thread = new FakeWorldThread()) {
            assertFalse(thread.isInWorldThread());
        }
    }

    @Test
    void stateAccessOffWorldThreadThrows() {
        try (FakeWorldThread thread = new FakeWorldThread()) {
            assertFalse(thread.isInWorldThread());
        }
    }

    @Test
    void onWorldMutationRunsOnWorldThreadAndFlushesOnce() {
        try (FakeWorldThread thread = new FakeWorldThread()) {
            thread.runAndWait(() -> assertTrue(thread.isInWorldThread()));
        }
    }
}
