package esq.phonemod.api;

import javax.annotation.Nonnull;

public interface SyncService {
    void publish(@Nonnull String channel, @Nonnull String payload);
    void subscribe(@Nonnull String channel);
}
