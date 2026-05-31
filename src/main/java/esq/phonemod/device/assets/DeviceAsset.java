package esq.phonemod.device.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Immutable/default blueprint for a device shell.
 *
 * <p>File-backed assets infer their ID from the file name. The Java {@code id}
 * field is still populated by {@link AssetBuilderCodec} from asset extra data.
 */
public final class DeviceAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, DeviceAsset>> {

    private static final String[] EMPTY_STRINGS = new String[0];

    public static final AssetBuilderCodec<String, DeviceAsset> CODEC = AssetBuilderCodec
            .builder(DeviceAsset.class, DeviceAsset::new, Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data)
            .append(new KeyedCodec<>("DisplayNameKey", Codec.STRING),
                    (asset, value) -> asset.displayNameKey = value,
                    asset -> asset.displayNameKey)
            .add()
            .append(new KeyedCodec<>("DeviceType", Codec.STRING),
                    (asset, value) -> asset.deviceType = value,
                    asset -> asset.deviceType)
            .add()
            .append(new KeyedCodec<>("ShellUiPath", Codec.STRING),
                    (asset, value) -> asset.shellUiPath = value,
                    asset -> asset.shellUiPath)
            .add()
            .append(new KeyedCodec<>("ContentSelector", Codec.STRING),
                    (asset, value) -> asset.contentSelector = value,
                    asset -> asset.contentSelector)
            .add()
            .append(new KeyedCodec<>("HomeHolderSelector", Codec.STRING),
                    (asset, value) -> asset.homeHolderSelector = value,
                    asset -> asset.homeHolderSelector)
            .add()
            .append(new KeyedCodec<>("HomeButtonSelector", Codec.STRING),
                    (asset, value) -> asset.homeButtonSelector = value,
                    asset -> asset.homeButtonSelector)
            .add()
            .append(new KeyedCodec<>("MetadataKey", Codec.STRING),
                    (asset, value) -> asset.metadataKey = value,
                    asset -> asset.metadataKey)
            .add()
            .append(new KeyedCodec<>("DefaultApps", Codec.STRING_ARRAY),
                    (asset, value) -> asset.defaultApps = value,
                    asset -> asset.defaultApps)
            .add()
            .append(new KeyedCodec<>("Capabilities", Codec.STRING_ARRAY),
                    (asset, value) -> asset.capabilities = value,
                    asset -> asset.capabilities)
            .add()
            .append(new KeyedCodec<>("DefaultThemeId", Codec.STRING),
                    (asset, value) -> asset.defaultThemeId = value,
                    asset -> asset.defaultThemeId)
            .add()
            .append(new KeyedCodec<>("DefaultWallpaper", Codec.STRING),
                    (asset, value) -> asset.defaultWallpaper = value,
                    asset -> asset.defaultWallpaper)
            .add()
            .append(new KeyedCodec<>("DefaultIconPack", Codec.STRING),
                    (asset, value) -> asset.defaultIconPack = value,
                    asset -> asset.defaultIconPack)
            .add()
            .append(new KeyedCodec<>("SoundProfile", DeviceSoundProfile.CODEC),
                    (asset, value) -> asset.soundProfile = value,
                    asset -> asset.soundProfile)
            .add()
            .append(new KeyedCodec<>("Selectors",
                            new MapCodec<>(Codec.STRING, HashMap::new, false)),
                    (asset, value) -> asset.selectors = value,
                    asset -> asset.selectors)
            .add()
            .build();

    private static AssetStore<String, DeviceAsset, DefaultAssetMap<String, DeviceAsset>> assetStore;

    private String id;
    private AssetExtraInfo.Data data;
    private String displayNameKey;
    private String deviceType;
    private String shellUiPath;
    private String contentSelector;
    private String homeHolderSelector;
    private String homeButtonSelector;
    private String metadataKey;
    private String[] defaultApps = EMPTY_STRINGS;
    private String[] capabilities = EMPTY_STRINGS;
    private String defaultThemeId;
    private String defaultWallpaper;
    private String defaultIconPack;
    private DeviceSoundProfile soundProfile;
    private Map<String, String> selectors = Collections.emptyMap();

    @Nonnull
    public static AssetStore<String, DeviceAsset, DefaultAssetMap<String, DeviceAsset>> getAssetStore() {
        if (assetStore == null) {
            assetStore = AssetRegistry.getAssetStore(DeviceAsset.class);
        }
        return assetStore;
    }

    @Nonnull
    public static DefaultAssetMap<String, DeviceAsset> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public AssetExtraInfo.Data getData() {
        return data;
    }

    @Nullable
    public String getDisplayNameKey() {
        return displayNameKey;
    }

    @Nullable
    public String getDeviceType() {
        return deviceType;
    }

    @Nullable
    public String getShellUiPath() {
        return shellUiPath;
    }

    @Nullable
    public String getContentSelector() {
        return contentSelector;
    }

    @Nullable
    public String getHomeHolderSelector() {
        return homeHolderSelector;
    }

    @Nullable
    public String getHomeButtonSelector() {
        return homeButtonSelector;
    }

    @Nullable
    public String getMetadataKey() {
        return metadataKey;
    }

    @Nonnull
    public String[] getDefaultApps() {
        return defaultApps != null ? defaultApps.clone() : EMPTY_STRINGS;
    }

    @Nonnull
    public String[] getCapabilities() {
        return capabilities != null ? capabilities.clone() : EMPTY_STRINGS;
    }

    @Nonnull
    public Set<String> getCapabilitySet() {
        return Arrays.stream(getCapabilities()).collect(Collectors.toUnmodifiableSet());
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
    public Map<String, String> getSelectors() {
        return selectors != null ? Collections.unmodifiableMap(selectors) : Collections.emptyMap();
    }
}
