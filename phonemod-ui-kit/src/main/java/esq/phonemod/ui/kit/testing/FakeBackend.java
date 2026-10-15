package esq.phonemod.ui.kit.testing;

import esq.phonemod.api.v3.Backend;
import esq.phonemod.api.v3.ViewRef;
import esq.phonemod.ui.kit.diff.DiffOp;
import esq.phonemod.ui.kit.host.DeviceSessionInfo;
import esq.phonemod.ui.kit.host.RenderBackend;

import javax.annotation.Nonnull;
import java.util.List;

/** In-memory {@link RenderBackend} for testing apps without a server (R-KIT-2). */
public final class FakeBackend implements RenderBackend {

    public FakeBackend() {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    @Nonnull
    public Backend backend() {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public void open(@Nonnull DeviceSessionInfo session, @Nonnull Object app, @Nonnull ViewRef view) {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public void apply(@Nonnull DeviceSessionInfo session, @Nonnull List<DiffOp> ops) {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public void close(@Nonnull DeviceSessionInfo session) {
        throw new UnsupportedOperationException("F4");
    }

    /** Every batch applied so far, oldest first. */
    @Nonnull
    public List<List<DiffOp>> batches() {
        throw new UnsupportedOperationException("F4");
    }

    /** All ops across all batches, flattened. */
    @Nonnull
    public List<DiffOp> ops() {
        throw new UnsupportedOperationException("F4");
    }

    @Nonnull
    public ViewRef openedView() {
        throw new UnsupportedOperationException("F4");
    }

    public boolean isOpen() {
        throw new UnsupportedOperationException("F4");
    }

    /** Names of the threads {@link #apply} was called from, in call order. */
    @Nonnull
    public List<String> applyThreads() {
        throw new UnsupportedOperationException("F4");
    }

    public void clear() {
        throw new UnsupportedOperationException("F4");
    }
}
