package esq.phonemod.device.core;

import esq.phonemod.device.assets.DeviceAsset;
import esq.phonemod.device.assets.DeviceSoundProfile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable runtime shell descriptor produced from a {@link DeviceAsset}.
 */
public final class DeviceShell {

    private final String assetId;
    private final String deviceType;
    private final String shellUiPath;
    private final String contentSelector;
    private final String homeHolderSelector;
    private final String homeButtonSelector;
    private final String metadataKey;
    private final String[] defaultApps;
    private final Set<String> capabilities;
    private final String defaultThemeId;
    private final String defaultWallpaper;
    private final String defaultIconPack;
    private final DeviceSoundProfile soundProfile;
    private final Map<String, String> selectors;

    public DeviceShell(@Nonnull DeviceAsset asset) {
        this.assetId = required(asset.getId(), "asset id");
        this.deviceType = required(asset.getDeviceType(), "DeviceType");
        this.shellUiPath = required(asset.getShellUiPath(), "ShellUiPath");
        this.contentSelector = required(asset.getContentSelector(), "ContentSelector");
        this.homeHolderSelector = required(asset.getHomeHolderSelector(), "HomeHolderSelector");
        this.homeButtonSelector = asset.getHomeButtonSelector();
        this.metadataKey = required(asset.getMetadataKey(), "MetadataKey");
        this.defaultApps = asset.getDefaultApps();
        this.capabilities = Collections.unmodifiableSet(Arrays.stream(asset.getCapabilities())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet()));
        this.defaultThemeId = asset.getDefaultThemeId();
        this.defaultWallpaper = asset.getDefaultWallpaper();
        this.defaultIconPack = asset.getDefaultIconPack();
        this.soundProfile = asset.getSoundProfile();
        this.selectors = asset.getSelectors();
    }

    @Nonnull
    private static String required(@Nullable String value, @Nonnull String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Device asset missing required field: " + field);
        }
        return value;
    }

    @Nonnull
    public String getAssetId() {
        return assetId;
    }

    @Nonnull
    public String getDeviceType() {
        return deviceType;
    }

    @Nonnull
    public String getShellUiPath() {
        return shellUiPath;
    }

    @Nonnull
    public String getContentSelector() {
        return contentSelector;
    }

    @Nonnull
    public String getHomeHolderSelector() {
        return homeHolderSelector;
    }

    @Nullable
    public String getHomeButtonSelector() {
        return homeButtonSelector;
    }

    @Nonnull
    public String getMetadataKey() {
        return metadataKey;
    }

    @Nonnull
    public String[] getDefaultApps() {
        return defaultApps.clone();
    }

    public boolean hasCapability(@Nonnull String capability) {
        return capabilities.contains(capability);
    }

    @Nonnull
    public Set<String> getCapabilities() {
        return capabilities;
    }

    @Nullable
    public String getDefaultThemeId() {
        return defaultThemeId;
    }

    @Nullable
    public String getDefaultWallpaper() {
        return defaultWallpaper;
    }

    @Nullable
    public String getDefaultIconPack() {
        return defaultIconPack;
    }

    @Nullable
    public DeviceSoundProfile getSoundProfile() {
        return soundProfile;
    }

    @Nonnull
    public String selector(@Nonnull String key, @Nonnull String fallback) {
        String selector = selectors.get(key);
        return selector != null && !selector.isBlank() ? selector : fallback;
    }

    /**
     * All declared selectors for this device as a map of <em>role &rarr; selector</em>.
     * Used by the theming system to map palette roles onto on-screen elements.
     */
    @Nonnull
    public Map<String, String> getSelectors() {
        return selectors != null ? Collections.unmodifiableMap(selectors) : Collections.emptyMap();
    }
}

