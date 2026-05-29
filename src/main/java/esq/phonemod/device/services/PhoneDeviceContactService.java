package esq.phonemod.device.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.components.PhoneOwnerComponent;

import javax.annotation.Nonnull;
import java.util.Map;

public final class PhoneDeviceContactService implements DeviceContactService {

    @Nonnull
    @Override
    public Map<String, String> getContacts(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String deviceId) {
        PhoneOwnerComponent component = store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
        return component.getContacts(deviceId);
    }

    @Override
    public void addContact(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String deviceId,
            @Nonnull String contactDeviceId,
            @Nonnull String displayName) {
        PhoneOwnerComponent component = store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
        component.addContact(deviceId, contactDeviceId, displayName);
        store.putComponent(ref, PhoneOwnerComponent.getComponentType(), component);
    }

    @Override
    public void removeContact(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull String deviceId,
            @Nonnull String contactDeviceId) {
        PhoneOwnerComponent component = store.ensureAndGetComponent(ref, PhoneOwnerComponent.getComponentType());
        component.removeContact(deviceId, contactDeviceId);
        store.putComponent(ref, PhoneOwnerComponent.getComponentType(), component);
    }
}

