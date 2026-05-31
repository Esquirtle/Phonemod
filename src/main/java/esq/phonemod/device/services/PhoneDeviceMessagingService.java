package esq.phonemod.device.services;

import esq.phonemod.phone.messaging.PhoneRegistry;
import esq.phonemod.phone.messaging.TextMessage;

import javax.annotation.Nonnull;
import java.util.List;

public final class PhoneDeviceMessagingService implements DeviceMessagingService {

    @Override
    public void deliver(@Nonnull String toDeviceId, @Nonnull TextMessage message) {
        PhoneRegistry.deliver(toDeviceId, message);
    }

    @Nonnull
    @Override
    public List<TextMessage> getInbox(@Nonnull String deviceId) {
        return PhoneRegistry.getInbox(deviceId);
    }
}

