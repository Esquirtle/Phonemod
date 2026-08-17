package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/** Runtime implementation behind the deprecated v1 sync handle. */
public final class RuntimeLegacySyncBridge implements LegacySyncBridge {
    public <S> S state(@Nonnull SyncChannelType<S> type,@Nonnull String id){return SyncService.getOrCreate(type,id).read();}
    public <S> void update(@Nonnull SyncChannelType<S> type,@Nonnull String id,@Nonnull Consumer<S> mutator){SyncService.publish(type,id,mutator);}
    public <S> void subscribe(@Nonnull SyncChannelType<S> type,@Nonnull String id,@Nonnull Viewer viewer){SyncService.subscribe(type,id,viewer);}
    public <S> void unsubscribe(@Nonnull SyncChannelType<S> type,@Nonnull String id,@Nonnull Viewer viewer){SyncService.unsubscribe(type,id,viewer);}
    public <S> void registerType(@Nonnull SyncChannelType<S> type){SyncService.registerType(type);}
    public <S> String createRoom(@Nonnull SyncChannelType<S> type,@Nonnull String host){return esq.phonemod.phone.sync.room.RoomService.createRoom(type,host);}
    public <S> boolean joinRoom(@Nonnull SyncChannelType<S> type,@Nonnull String id,@Nonnull String member){return esq.phonemod.phone.sync.room.RoomService.join(type,id,member);}
    public <S> void leaveRoom(@Nonnull SyncChannelType<S> type,@Nonnull String id,@Nonnull String member){esq.phonemod.phone.sync.room.RoomService.leave(type,id,member);}
}
