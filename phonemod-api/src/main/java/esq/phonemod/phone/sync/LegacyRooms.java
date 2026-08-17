package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;

/** Deprecated 2.x source-compatibility facade for typed room channels. */
@Deprecated(since = "2.0", forRemoval = false)
public final class LegacyRooms {
    private LegacyRooms() {}
    public static <S> void registerType(@Nonnull SyncChannelType<S> type) {
        LegacySyncBridge.require().registerType(type);
    }
    public static <S> String create(@Nonnull SyncChannelType<S> type, @Nonnull String host) {
        return LegacySyncBridge.require().createRoom(type, host);
    }
    public static <S> boolean join(@Nonnull SyncChannelType<S> type, @Nonnull String id,
                                   @Nonnull String member) {
        return LegacySyncBridge.require().joinRoom(type, id, member);
    }
    public static <S> void leave(@Nonnull SyncChannelType<S> type, @Nonnull String id,
                                 @Nonnull String member) {
        LegacySyncBridge.require().leaveRoom(type, id, member);
    }
}
