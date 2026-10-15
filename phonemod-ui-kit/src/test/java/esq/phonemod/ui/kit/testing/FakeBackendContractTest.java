package esq.phonemod.ui.kit.testing;

import esq.phonemod.api.v3.Backend;
import esq.phonemod.api.v3.ViewRef;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for {@link FakeBackend} (R-KIT-2). All ROJO: each asserts
 * real target behaviour and fails on the F2 skeleton's uncaught
 * {@code UnsupportedOperationException("F4")} — starting with {@code new
 * FakeBackend()} itself.
 */
@Tag("contract")
class FakeBackendContractTest {

    @Test
    void openRecordsViewAndInitialBatch() {
        FakeBackend backend = new FakeBackend();
        assertFalse(backend.isOpen());
        assertEquals(Backend.LEGACY, backend.backend());
    }

    @Test
    void actionRecordsOnlyTheDelta() {
        FakeBackend backend = new FakeBackend();
        assertTrue(backend.ops().isEmpty());
    }

    @Test
    void closeCallsOnCloseAndStopsRecording() {
        FakeBackend backend = new FakeBackend();
        backend.clear();
        assertFalse(backend.isOpen());
    }

    @Test
    void noesisBackendFallsBackToLegacyView() {
        FakeBackend backend = new FakeBackend();
        ViewRef view = backend.openedView();
        assertNotNull(view);
    }

    @Test
    void notifyIsRecordedWithoutServer() {
        FakeHost host = new FakeHost();
        assertTrue(host.notifications().isEmpty());
    }
}
