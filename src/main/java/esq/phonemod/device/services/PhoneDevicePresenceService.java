package esq.phonemod.device.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.messaging.PhoneRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public final class PhoneDevicePresenceService implements DevicePresenceService {

    @Nullable
    @Override
    public PhoneRegistry.OnlineEntry getOnlineEntry(@Nonnull String deviceId) {
        return PhoneRegistry.getOnlineEntry(deviceId);
    }

    @Nonnull
    @Override
    public Collection<PhoneRegistry.OnlineEntry> getOnlineEntries() {
        return PhoneRegistry.getOnlineEntries();
    }

    @Nonnull
    @Override
    public List<String> getDeviceIdsByRef(@Nonnull Ref<EntityStore> ref) {
        return PhoneRegistry.getPhoneNumbersByRef(ref);
    }
}

