package esq.phonemod.device.core;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.device.api.DevicePageHandle;
import esq.phonemod.phone.api.PhoneApp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Runtime-only state for one opened physical device.
 */
public final class DeviceSession {

    private final String deviceId;
    private final String deviceAssetId;
    private final DeviceShell shell;
    private final PlayerRef playerRef;
    private Ref<EntityStore> ref;
    private Store<EntityStore> store;
    private PhoneApp<?> currentApp;
    private String currentAppId;
    private DevicePageState currentState = DevicePageState.HOME;
    private DevicePageHandle pageHandle;

    public DeviceSession(@Nonnull String deviceId,
            @Nonnull String deviceAssetId,
            @Nonnull DeviceShell shell,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store) {
        this.deviceId = deviceId;
        this.deviceAssetId = deviceAssetId;
        this.shell = shell;
        this.playerRef = playerRef;
        this.ref = ref;
        this.store = store;
    }

    @Nonnull
    public String getDeviceId() {
        return deviceId;
    }

    @Nonnull
    public String getDeviceAssetId() {
        return deviceAssetId;
    }

    @Nonnull
    public DeviceShell getShell() {
        return shell;
    }

    @Nonnull
    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Nonnull
    public Ref<EntityStore> getRef() {
        return ref;
    }

    @Nonnull
    public Store<EntityStore> getStore() {
        return store;
    }

    public void updateStore(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.ref = ref;
        this.store = store;
    }

    @Nullable
    public PhoneApp<?> getCurrentApp() {
        return currentApp;
    }

    public void setCurrentApp(@Nullable PhoneApp<?> currentApp) {
        this.currentApp = currentApp;
    }

    @Nullable
    public String getCurrentAppId() {
        return currentAppId;
    }

    public void setCurrentAppId(@Nullable String currentAppId) {
        this.currentAppId = currentAppId;
    }

    @Nonnull
    public DevicePageState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(@Nonnull DevicePageState currentState) {
        this.currentState = currentState;
    }

    @Nullable
    public DevicePageHandle getPageHandle() {
        return pageHandle;
    }

    public void setPageHandle(@Nonnull DevicePageHandle pageHandle) {
        this.pageHandle = pageHandle;
    }
}

