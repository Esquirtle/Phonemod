package esq.phonemod.api;

import javax.annotation.Nonnull;

public interface CallingService {
    boolean call(@Nonnull DeviceIdentity from, @Nonnull String toAddress);
    void hangUp(@Nonnull DeviceIdentity endpoint);
}
