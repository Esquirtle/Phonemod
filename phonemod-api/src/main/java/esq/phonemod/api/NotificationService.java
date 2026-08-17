package esq.phonemod.api;

import javax.annotation.Nonnull;

public interface NotificationService {
    void send(@Nonnull DeviceIdentity target, @Nonnull String title, @Nonnull String body);
}
