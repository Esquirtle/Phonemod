package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Typed, app-facing handle to one channel, scoped to the requesting {@link Viewer}.
 * Obtained via {@code PhoneAppContext.channel(type, id)}.
 *
 * <p>Typical use inside an app:
 * <pre>{@code
 * var ch = ctx.channel(MyApp.ROOM, roomId);
 * ch.subscribe();              // in build(): this view re-renders on change
 * render(ch.state(), cmd, evb);
 * // in a route handler:
 * ch.update(s -> s.apply(move));   // mutate -> persist -> broadcast to all viewers
 * }</pre>
 *
 * @param <S> the channel's shared state type
 */
public final class SyncChannelHandle<S> {

    private final SyncChannelType<S> type;
    private final String id;
    private final Viewer viewer;

    public SyncChannelHandle(@Nonnull SyncChannelType<S> type,
            @Nonnull String id,
            @Nonnull Viewer viewer) {
        this.type = type;
        this.id = id;
        this.viewer = viewer;
    }

    /** Current shared state (creating the channel if needed). Treat as read-only. */
    @Nonnull
    public S state() {
        return SyncService.getOrCreate(type, id).read();
    }

    /** Mutates the shared state, then persists and re-renders every subscriber. */
    public void update(@Nonnull Consumer<S> mutator) {
        SyncService.publish(type, id, mutator);
    }

    /** Registers this view as a subscriber; it will re-render on every change. */
    public void subscribe() {
        SyncService.subscribe(type, id, viewer);
    }

    /** Stops this view receiving updates for the channel. */
    public void unsubscribe() {
        SyncService.unsubscribe(type, id, viewer);
    }

    @Nonnull
    public String id() {
        return id;
    }
}
