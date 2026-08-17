package esq.phonemod.phone.sync;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Internal host bridge retained so v1 channel handles remain binary/source compatible. */
public interface LegacySyncBridge {
    AtomicReference<LegacySyncBridge> LOCATOR = new AtomicReference<>();
    <S> S state(@Nonnull SyncChannelType<S> type, @Nonnull String id);
    <S> void update(@Nonnull SyncChannelType<S> type, @Nonnull String id, @Nonnull Consumer<S> mutator);
    <S> void subscribe(@Nonnull SyncChannelType<S> type, @Nonnull String id, @Nonnull Viewer viewer);
    <S> void unsubscribe(@Nonnull SyncChannelType<S> type, @Nonnull String id, @Nonnull Viewer viewer);
    <S> void registerType(@Nonnull SyncChannelType<S> type);
    <S> String createRoom(@Nonnull SyncChannelType<S> type, @Nonnull String host);
    <S> boolean joinRoom(@Nonnull SyncChannelType<S> type, @Nonnull String id, @Nonnull String member);
    <S> void leaveRoom(@Nonnull SyncChannelType<S> type, @Nonnull String id, @Nonnull String member);
    static LegacySyncBridge require(){
        LegacySyncBridge bridge=LOCATOR.get();
        if(bridge==null) throw new IllegalStateException("Phonemod sync runtime is not enabled");
        return bridge;
    }
    static void install(LegacySyncBridge bridge){if(!LOCATOR.compareAndSet(null,bridge))throw new IllegalStateException("Sync bridge already installed");}
    static void uninstall(LegacySyncBridge bridge){LOCATOR.compareAndSet(bridge,null);}
}
