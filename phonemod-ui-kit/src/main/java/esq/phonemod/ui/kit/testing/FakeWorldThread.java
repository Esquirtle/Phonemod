package esq.phonemod.ui.kit.testing;

import esq.phonemod.ui.kit.host.WorldThread;

import javax.annotation.Nonnull;

/** A real background thread named {@code "fake-world"}, standing in for a {@code World}. */
public final class FakeWorldThread implements WorldThread, AutoCloseable {

    public FakeWorldThread() {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public boolean isInWorldThread() {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public void execute(@Nonnull Runnable task) {
        throw new UnsupportedOperationException("F4");
    }

    /** Runs {@code task} on the fake world thread and blocks until it finishes. */
    public void runAndWait(@Nonnull Runnable task) {
        throw new UnsupportedOperationException("F4");
    }

    /** Blocks until every queued task has run. */
    public void drain() {
        throw new UnsupportedOperationException("F4");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("F4");
    }
}
