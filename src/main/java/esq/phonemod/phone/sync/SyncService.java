package esq.phonemod.phone.sync;

import com.hypixel.hytale.logger.HytaleLogger;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import esq.phonemod.operations.OperationalMetrics;
import java.util.function.Consumer;

/**
 * Central registry and coordinator for synchronized channels.
 *
 * <p>Responsibilities: hold the registered {@link SyncChannelType}s and live
 * {@link SyncChannel}s; create/load/dispose channels; apply mutations
 * (mutate &rarr; persist &rarr; broadcast); and fan a change out to every
 * subscriber by resolving their live {@code OnlineEntry} from {@link PhoneRegistry}
 * and re-rendering on each player's own world thread.
 *
 * <p>Static facade in the spirit of {@code CallRegistry}. Channel maps are
 * concurrent, per-channel mutation is serialized inside {@link SyncChannel}, and
 * subscriber sets are concurrent — safe across many open phones in different worlds.
 */
public final class SyncService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final Map<String, SyncChannelType<?>> TYPES = new ConcurrentHashMap<>();
    private static final Map<String, SyncChannel<?>> CHANNELS = new ConcurrentHashMap<>();
    private static final AtomicInteger PENDING_SAVES = new AtomicInteger();

    @Nullable
    private static volatile ChannelStore store;

    private SyncService() {
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    /** Binds the JSON store to the plugin data directory. Call before {@link #loadAll}. */
    public static void init(@Nonnull Path dataDirectory) {
        store = new ChannelStore(dataDirectory);
    }

    public static void registerType(@Nonnull SyncChannelType<?> type) {
        TYPES.put(type.typeId(), type);
    }

    /**
     * Rehydrates persistent channels from disk and clears stale ephemeral files.
     * Call once at runtime start, after all types are registered.
     */
    public static void loadAll() {
        for (SyncChannelType<?> type : TYPES.values()) {
            loadType(type);
        }
    }

    private static <S> void loadType(@Nonnull SyncChannelType<S> type) {
        ChannelStore s = store;
        if (s == null) {
            return;
        }
        if (type.persistence() == ChannelPersistence.EPHEMERAL) {
            s.deleteAllFor(type);
            return;
        }
        for (S state : s.loadAll(type)) {
            String id = type.idOf(state);
            CHANNELS.put(key(type.typeId(), id), new SyncChannel<>(type, id, state));
        }
        LOGGER.atFine().log("[SyncService] Loaded persistent channels for type %s", type.typeId());
    }

    /**
     * Synchronously flushes all persistent channels. Call on shutdown.
     *
     * <p>Uses a single shared deadline for the whole sweep so the total shutdown delay is
     * bounded to {@link ChannelStore#SHUTDOWN_FLUSH_TIMEOUT}, not that timeout multiplied by
     * the number of channels — critical when the Universe saving lock is held and every
     * per-channel {@code get} would otherwise wait the full budget.
     */
    public static void flushAll() {
        ChannelStore s = store;
        if (s == null) {
            return;
        }
        long deadlineNanos = System.nanoTime() + ChannelStore.SHUTDOWN_FLUSH_TIMEOUT.toNanos();
        for (SyncChannel<?> channel : CHANNELS.values()) {
            if (channel.type().persistence() == ChannelPersistence.PERSISTENT) {
                s.saveBlocking(channel, deadlineNanos);
            }
        }
    }

    // ── Channel access ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <S> SyncChannel<S> getOrCreate(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        return (SyncChannel<S>) CHANNELS.computeIfAbsent(key(type.typeId(), id),
                k -> create(type, id));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <S> SyncChannel<S> get(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        return (SyncChannel<S>) CHANNELS.get(key(type.typeId(), id));
    }

    /** Snapshot of every live channel (used by the room layer for cross-type cleanup). */
    @Nonnull
    public static List<SyncChannel<?>> allChannels() {
        return new ArrayList<>(CHANNELS.values());
    }

    /** Snapshot of every live channel of the given type. */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <S> List<SyncChannel<S>> channelsOfType(@Nonnull SyncChannelType<S> type) {
        List<SyncChannel<S>> result = new ArrayList<>();
        for (SyncChannel<?> channel : CHANNELS.values()) {
            if (channel.type().typeId().equals(type.typeId())) {
                result.add((SyncChannel<S>) channel);
            }
        }
        return result;
    }

    private static <S> SyncChannel<S> create(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        ChannelStore s = store;
        S state = (s != null && type.persistence() == ChannelPersistence.PERSISTENT)
                ? s.loadOne(type, id)
                : null;
        if (state == null) {
            state = type.newState(id);
        }
        return new SyncChannel<>(type, id, state);
    }

    /** Removes a channel from memory and deletes its JSON. */
    public static <S> void dispose(@Nonnull SyncChannelType<S> type, @Nonnull String id) {
        CHANNELS.remove(key(type.typeId(), id));
        ChannelStore s = store;
        if (s != null) {
            s.delete(type, id);
        }
        LOGGER.atFine().log("[SyncService] Disposed channel %s/%s", type.typeId(), id);
    }

    // ── Mutation + broadcast ─────────────────────────────────────────────────

    /** Mutates a channel's state, persists it, then re-renders every subscriber. */
    public static <S> void publish(@Nonnull SyncChannelType<S> type,
            @Nonnull String id,
            @Nonnull Consumer<S> mutator) {
        SyncChannel<S> channel = getOrCreate(type, id);
        channel.mutate(mutator);
        persist(channel);
        broadcast(channel);
    }

    private static <S> void persist(@Nonnull SyncChannel<S> channel) {
        ChannelStore s = store;
        if (s == null) {
            return;
        }
        var future = s.save(channel);
        if (future != null) {
            PENDING_SAVES.incrementAndGet();
            future.whenComplete((result, error) -> {
                PENDING_SAVES.decrementAndGet();
                if (error != null) {
                    OperationalMetrics.get().saveFailure();
                    LOGGER.atWarning().withCause(error).log(
                            "[SyncService] Save failed channel=%s/%s",
                            channel.type().typeId(), channel.id());
                    return;
                }
                channel.markSaved(result.sequence(), result.hash());
            });
        }
    }

    public static int pendingSaves() { return PENDING_SAVES.get(); }

    /** Re-renders the bound app for every subscriber whose phone is currently open. */
    public static void broadcast(@Nonnull SyncChannel<?> channel) {
        for (Viewer viewer : channel.subscribers()) {
            PhoneRegistry.OnlineEntry entry = PhoneRegistry.getOnlineEntry(viewer.phoneNumber());
            if (entry == null) {
                continue; // phone closed: state is persisted; it renders on reopen
            }
            String appId = viewer.appId();
            entry.world().execute(() -> entry.page().refreshApp(appId));
        }
    }

    // ── Subscriptions ─────────────────────────────────────────────────────────

    public static <S> void subscribe(@Nonnull SyncChannelType<S> type,
            @Nonnull String id,
            @Nonnull Viewer viewer) {
        getOrCreate(type, id).addSubscriber(viewer);
    }

    public static <S> void unsubscribe(@Nonnull SyncChannelType<S> type,
            @Nonnull String id,
            @Nonnull Viewer viewer) {
        SyncChannel<?> channel = CHANNELS.get(key(type.typeId(), id));
        if (channel != null) {
            channel.removeSubscriber(viewer);
        }
    }

    /** Drops one exact (phone, app) viewer from every channel — used on app close. */
    public static void unsubscribeViewer(@Nonnull Viewer viewer) {
        for (SyncChannel<?> channel : CHANNELS.values()) {
            channel.removeSubscriber(viewer);
        }
    }

    /** Drops all of a phone's viewers from every channel — used on device close/disconnect. */
    public static void unsubscribePhone(@Nonnull String phoneNumber) {
        for (SyncChannel<?> channel : CHANNELS.values()) {
            channel.removeViewersForPhone(phoneNumber);
        }
    }

    @Nonnull
    private static String key(@Nonnull String typeId, @Nonnull String id) {
        return typeId + "|" + id;
    }
}
