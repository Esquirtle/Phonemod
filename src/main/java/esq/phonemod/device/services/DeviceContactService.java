package esq.phonemod.device.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;

public interface DeviceContactService {

    @Nonnull
    Map<String, String> getContacts(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String deviceId);

    void addContact(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String deviceId,
            @Nonnull String contactDeviceId,
            @Nonnull String displayName);

    void removeContact(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String deviceId,
            @Nonnull String contactDeviceId);
}
