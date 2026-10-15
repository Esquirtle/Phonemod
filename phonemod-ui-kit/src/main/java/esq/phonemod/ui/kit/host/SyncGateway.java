package esq.phonemod.ui.kit.host;

import esq.phonemod.api.v3.SyncChannels;
import esq.phonemod.phone.sync.SyncChannelType;

import javax.annotation.Nonnull;

/** Port for {@code DeviceAppContext.sync}. Not wired until core exists (F3). */
public interface SyncGateway {
    @Nonnull
    <S> SyncChannels<S> channels(@Nonnull SyncChannelType<S> type);
}
