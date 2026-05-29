package esq.phonemod.device.services;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public interface DeviceNotificationService {

    void send(@Nonnull PlayerRef playerRef, @Nonnull String title, @Nonnull String body);
}
