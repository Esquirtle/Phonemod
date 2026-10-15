package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Typed, session-scoped handle to one live channel. Same semantics as v2's
 * {@code SyncChannelHandle} ({@code SyncChannelHandle.java:35-54}), but indexed by
 * {@link DeviceSessionId} rather than phone number.
 */
public interface SyncHandle<S> {

    @Nonnull
    String id();

    @Nonnull
    S state();

    void update(@Nonnull Consumer<S> mutator);

    void subscribe();

    void unsubscribe();
}
