package esq.phonemod.api.v3;

import esq.phonemod.phone.sync.SyncChannelType;

import javax.annotation.Nonnull;

/**
 * App-facing entry point for one {@link SyncChannelType}, obtained via
 * {@code DeviceAppContext.sync(type)}. Replaces v2's {@code LegacyRooms}
 * ({@code LegacyRooms.java:32-47}); rooms are keyed by {@link DeviceSessionId},
 * not address (D-28).
 */
public interface SyncChannels<S> {

    @Nonnull
    SyncChannelType<S> type();

    @Nonnull
    SyncHandle<S> channel(@Nonnull String id);

    @Nonnull
    SyncHandle<S> createRoom(@Nonnull DeviceSessionId owner);

    void joinRoom(@Nonnull String roomId, @Nonnull DeviceSessionId session);

    void leaveRoom(@Nonnull String roomId, @Nonnull DeviceSessionId session);
}
