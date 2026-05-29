package esq.phonemod.device.services;

import esq.phonemod.phone.messaging.TextMessage;

import javax.annotation.Nonnull;
import java.util.List;

public interface DeviceMessagingService {

    void deliver(@Nonnull String toDeviceId, @Nonnull TextMessage message);

    @Nonnull
    List<TextMessage> getInbox(@Nonnull String deviceId);
}
