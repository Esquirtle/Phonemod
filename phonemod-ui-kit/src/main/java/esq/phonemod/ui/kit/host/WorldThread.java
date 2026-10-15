package esq.phonemod.ui.kit.host;

import javax.annotation.Nonnull;

/**
 * Neutral world-thread port. The legacy backend implements this over
 * {@code World.isInThread()} ({@code TickingThread.java:314}) and
 * {@code World.execute} ({@code World.java:849}; {@code World implements
 * Executor} at {@code World.java:81}).
 */
public interface WorldThread {

    boolean isInWorldThread();

    void execute(@Nonnull Runnable task);
}
