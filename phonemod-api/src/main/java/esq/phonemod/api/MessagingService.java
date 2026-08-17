package esq.phonemod.api;

import javax.annotation.Nonnull;

public interface MessagingService {
    boolean send(@Nonnull DeviceIdentity from, @Nonnull String toAddress, @Nonnull String body);
}
