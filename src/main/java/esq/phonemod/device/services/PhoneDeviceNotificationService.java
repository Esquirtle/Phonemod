package esq.phonemod.device.services;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;

public final class PhoneDeviceNotificationService implements DeviceNotificationService {

    @Override
    public void send(@Nonnull PlayerRef playerRef, @Nonnull String title, @Nonnull String body) {
        NotificationUtil.sendNotification(playerRef.getPacketHandler(), Message.raw(title), Message.raw(body));
    }
}

