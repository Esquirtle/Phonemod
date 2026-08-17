package esq.phonemod.migration;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.components.PhoneAppSessionState;
import esq.phonemod.phone.components.PhoneOwnerComponent;

import javax.annotation.Nonnull;

/** Idempotently moves one player's address-keyed v1 data after an identity conflict. */
public final class PlayerDeviceDataMigrator {
    private PlayerDeviceDataMigrator() {}

    public static void remap(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                             @Nonnull String oldAddress, @Nonnull String newAddress) {
        if (oldAddress.equals(newAddress)) return;
        DeviceInstalledAppsComponent apps = store.getComponent(ref, DeviceInstalledAppsComponent.getComponentType());
        if (apps != null) { apps.remapDevice(oldAddress, newAddress); store.putComponent(ref, DeviceInstalledAppsComponent.getComponentType(), apps); }
        DeviceSettingsComponent settings = store.getComponent(ref, DeviceSettingsComponent.getComponentType());
        if (settings != null) { settings.remapDevice(oldAddress, newAddress); store.putComponent(ref, DeviceSettingsComponent.getComponentType(), settings); }
        ConversationHistoryComponent messages = store.getComponent(ref, ConversationHistoryComponent.getComponentType());
        if (messages != null) { messages.remapDevice(oldAddress, newAddress); store.putComponent(ref, ConversationHistoryComponent.getComponentType(), messages); }
        CallHistoryComponent calls = store.getComponent(ref, CallHistoryComponent.getComponentType());
        if (calls != null) { calls.remapDevice(oldAddress, newAddress); store.putComponent(ref, CallHistoryComponent.getComponentType(), calls); }
        PhoneOwnerComponent contacts = store.getComponent(ref, PhoneOwnerComponent.getComponentType());
        if (contacts != null) { contacts.remapDevice(oldAddress, newAddress); store.putComponent(ref, PhoneOwnerComponent.getComponentType(), contacts); }
        PhoneAppSessionState state = store.getComponent(ref, PhoneAppSessionState.getComponentType());
        if (state != null) { state.remapDevice(oldAddress, newAddress); store.putComponent(ref, PhoneAppSessionState.getComponentType(), state); }
    }
}
