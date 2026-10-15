package esq.phonemod.api.v3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Persisted session-scoped key/value state. World-thread only. */
public interface AppState {

    @Nullable
    String get(@Nonnull String key);

    void put(@Nonnull String key, @Nonnull String value);

    void remove(@Nonnull String key);
}
