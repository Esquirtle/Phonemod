package esq.phonemod.setup;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.PhoneMod;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.phone.components.CallHistoryComponent;
import esq.phonemod.phone.components.ConversationHistoryComponent;
import esq.phonemod.phone.components.PhoneAppSessionState;
import esq.phonemod.phone.components.PhoneOwnerComponent;

public class ComponentRegistryManager {
    private final PhoneMod plugin;

    public ComponentRegistryManager(PhoneMod plugin) {
        this.plugin = plugin;
    }

    public void register() {
        ComponentType<EntityStore, DeviceSettingsComponent> deviceSettingsType =
                plugin.getEntityStoreRegistry().registerComponent(
                        DeviceSettingsComponent.class,
                        "DeviceSettings",
                        DeviceSettingsComponent.CODEC);
        DeviceSettingsComponent.setComponentType(deviceSettingsType);

        ComponentType<EntityStore, PhoneOwnerComponent> phoneOwnerType =
                plugin.getEntityStoreRegistry().registerComponent(
                        PhoneOwnerComponent.class,
                        "PhoneOwnerComponent",
                        PhoneOwnerComponent.CODEC);
        PhoneOwnerComponent.setComponentType(phoneOwnerType);

        ComponentType<EntityStore, ConversationHistoryComponent> conversationHistoryType =
                plugin.getEntityStoreRegistry().registerComponent(
                        ConversationHistoryComponent.class,
                        "PhoneConversationHistory",
                        ConversationHistoryComponent.CODEC);
        ConversationHistoryComponent.setComponentType(conversationHistoryType);

        ComponentType<EntityStore, CallHistoryComponent> callHistoryType =
                plugin.getEntityStoreRegistry().registerComponent(
                        CallHistoryComponent.class,
                        "PhoneCallHistory",
                        CallHistoryComponent.CODEC);
        CallHistoryComponent.setComponentType(callHistoryType);

        ComponentType<EntityStore, PhoneAppSessionState> sessionStateType =
                plugin.getEntityStoreRegistry().registerComponent(
                        PhoneAppSessionState.class,
                        "PhoneAppSessionState",
                        PhoneAppSessionState.CODEC);
        PhoneAppSessionState.setComponentType(sessionStateType);
    }

    public void registerSystems() {

    }
}
