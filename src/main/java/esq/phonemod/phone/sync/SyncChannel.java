package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One live instance of a {@link SyncChannelType}: its shared {@code state} plus the
 * set of {@link Viewer}s currently rendering it.
 *
 * <p>State mutations are serialized under a per-channel lock so concurrent
 * publishers cannot interleave. Reads return the live state reference for rendering;
 * by convention callers treat it as read-only and only change it through
 * {@link SyncService#publish}.
 *
 * @param <S> the shared state type
 */
public final class SyncChannel<S> {

    private final SyncChannelType<S> type;
    private final String id;
    private final Object lock = new Object();
    private S state;
    private final Set<Viewer> subscribers = ConcurrentHashMap.newKeySet();
    /** Content hash of the last persisted JSON, for dirty-gated writes. */
    @Nullable
    private volatile String savedHash;

    SyncChannel(@Nonnull SyncChannelType<S> type, @Nonnull String id, @Nonnull S state) {
        this.type = type;
        this.id = id;
        this.state = state;
    }

    @Nonnull
    public SyncChannelType<S> type() {
        return type;
    }

    @Nonnull
    public String id() {
        return id;
    }

    /** Returns the live state for rendering. Treat as read-only. */
    @Nonnull
    public S read() {
        synchronized (lock) {
            return state;
        }
    }

    /** Applies {@code mutator} to the state under the channel lock. */
    void mutate(@Nonnull Consumer<S> mutator) {
        synchronized (lock) {
            mutator.accept(state);
        }
    }

    @Nonnull
    Set<Viewer> subscribers() {
        return subscribers;
    }

    void addSubscriber(@Nonnull Viewer viewer) {
        subscribers.add(viewer);
    }

    boolean removeSubscriber(@Nonnull Viewer viewer) {
        return subscribers.remove(viewer);
    }

    /** Removes every viewer belonging to {@code phoneNumber} (any app). */
    boolean removeViewersForPhone(@Nonnull String phoneNumber) {
        return subscribers.removeIf(v -> v.phoneNumber().equals(phoneNumber));
    }

    @Nullable
    String savedHash() {
        return savedHash;
    }

    void setSavedHash(@Nullable String hash) {
        this.savedHash = hash;
    }
}
