package esq.phonemod.device.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.assets.DeviceAsset;
import esq.phonemod.device.services.DeviceCallService;
import esq.phonemod.device.services.DeviceContactService;
import esq.phonemod.device.services.DeviceMessagingService;
import esq.phonemod.device.services.DeviceNotificationService;
import esq.phonemod.device.services.DevicePresenceService;
import esq.phonemod.device.services.DeviceVoiceRoutingService;
import esq.phonemod.device.services.PhoneDeviceCallService;
import esq.phonemod.device.services.PhoneDeviceContactService;
import esq.phonemod.device.services.PhoneDeviceMessagingService;
import esq.phonemod.device.services.PhoneDeviceNotificationService;
import esq.phonemod.device.services.PhoneDevicePresenceService;
import esq.phonemod.device.services.PhoneDeviceVoiceRoutingService;
import esq.phonemod.device.ui.DevicePage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Root singleton for device framework runtime helpers.
 */
public final class DeviceService {

    public static final String DEFAULT_PHONE_ASSET_ID = "Phone";

    private static DeviceService instance;

    private final DeviceRegistry registry;
    private final DeviceMessagingService messagingService;
    private final DeviceCallService callService;
    private final DeviceNotificationService notificationService;
    private final DeviceVoiceRoutingService voiceRoutingService;
    private final DeviceContactService contactService;
    private final DevicePresenceService presenceService;

    private DeviceService() {
        this.registry = new DeviceRegistry();
        this.messagingService = new PhoneDeviceMessagingService();
        this.callService = new PhoneDeviceCallService();
        this.notificationService = new PhoneDeviceNotificationService();
        this.voiceRoutingService = new PhoneDeviceVoiceRoutingService();
        this.contactService = new PhoneDeviceContactService();
        this.presenceService = new PhoneDevicePresenceService();
    }

    public static void initialize() {
        if (instance == null) {
            instance = new DeviceService();
        }
    }

    @Nonnull
    public static DeviceService get() {
        if (instance == null) {
            throw new IllegalStateException("DeviceService must be initialized before use");
        }
        return instance;
    }

    @Nullable
    public DeviceAsset getDeviceAsset(@Nonnull String assetId) {
        return registry.getAsset(assetId);
    }

    @Nonnull
    public DeviceShell createShell(@Nonnull String assetId) {
        return registry.getShell(assetId);
    }

    public boolean hasDeviceAsset(@Nonnull String assetId) {
        return registry.hasAsset(assetId);
    }

    @Nonnull
    public DeviceSession createSession(@Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String deviceAssetId,
            @Nonnull String deviceId) {
        DeviceShell shell = createShell(deviceAssetId);
        return new DeviceSession(deviceId, deviceAssetId, shell, playerRef, ref, store);
    }

    @Nonnull
    public DevicePage createDevicePage(@Nonnull DeviceSession session) {
        return new DevicePage(session);
    }

    @Nonnull
    public DeviceMessagingService getMessagingService() {
        return messagingService;
    }

    @Nonnull
    public DeviceCallService getCallService() {
        return callService;
    }

    @Nonnull
    public DeviceNotificationService getNotificationService() {
        return notificationService;
    }

    @Nonnull
    public DeviceVoiceRoutingService getVoiceRoutingService() {
        return voiceRoutingService;
    }

    @Nonnull
    public DeviceContactService getContactService() {
        return contactService;
    }

    @Nonnull
    public DevicePresenceService getPresenceService() {
        return presenceService;
    }
}
