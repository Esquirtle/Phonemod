package esq.phonemod.device.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public interface DevicePresenceService {

    @Nullable
    PhoneRegistry.OnlineEntry getOnlineEntry(@Nonnull String deviceId);

    @Nonnull
    Collection<PhoneRegistry.OnlineEntry> getOnlineEntries();

    @Nonnull
    List<String> getDeviceIdsByRef(@Nonnull Ref<EntityStore> ref);
}

