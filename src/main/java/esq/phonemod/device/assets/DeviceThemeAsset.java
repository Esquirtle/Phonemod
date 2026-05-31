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

/**
 * Data-driven theme palette for device shells.
 *
 * <p>Themes are <em>data</em>, not separate {@code .ui} files: Hytale named
 * expressions are file-scoped and cannot be overridden across documents, so the
 * Dust component library ({@code DustLib.ui}) bakes in only the default palette.
 * Alternate themes are described here as a {@code Colors} map of
 * <em>role &rarr; hex color</em>, and applied at runtime by
 * {@code ThemeService}, which sets the background of the selectors a device
 * declares in its {@code Selectors} map.
 *
 * <p>The role keys are arbitrary strings shared between a device asset's
 * {@code Selectors} map and a theme's {@code Colors} map; the intersection is
 * what gets themed. Adding a new themeable element therefore needs no code —
 * only a new entry in both JSON maps.
 *
 * <p>File-backed assets infer their ID (the theme id, e.g. {@code "Dark"}) from
 * the file name.
 */
public final class DeviceThemeAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, DeviceThemeAsset>> {

    public static final AssetBuilderCodec<String, DeviceThemeAsset> CODEC = AssetBuilderCodec
            .builder(DeviceThemeAsset.class, DeviceThemeAsset::new, Codec.STRING,
                    (asset, id) -> asset.id = id,
                    asset -> asset.id,
                    (asset, data) -> asset.data = data,
                    asset -> asset.data)
            .append(new KeyedCodec<>("DisplayNameKey", Codec.STRING),
                    (asset, value) -> asset.displayNameKey = value,
                    asset -> asset.displayNameKey)
            .add()
            .append(new KeyedCodec<>("Colors",
                            new MapCodec<>(Codec.STRING, HashMap::new, false)),
                    (asset, value) -> asset.colors = value,
                    asset -> asset.colors)
            .add()
            .build();

    private static AssetStore<String, DeviceThemeAsset, DefaultAssetMap<String, DeviceThemeAsset>> assetStore;

    private String id;
    private AssetExtraInfo.Data data;
    private String displayNameKey;
    private Map<String, String> colors = Collections.emptyMap();

    @Nonnull
    public static AssetStore<String, DeviceThemeAsset, DefaultAssetMap<String, DeviceThemeAsset>> getAssetStore() {
        if (assetStore == null) {
            assetStore = AssetRegistry.getAssetStore(DeviceThemeAsset.class);
        }
        return assetStore;
    }

    @Nonnull
    public static DefaultAssetMap<String, DeviceThemeAsset> getAssetMap() {
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

    /** Role &rarr; hex color string (e.g. {@code "topbar" -> "#202c3a"}). */
    @Nonnull
    public Map<String, String> getColors() {
        return colors != null ? Collections.unmodifiableMap(colors) : Collections.emptyMap();
    }

    /** Returns the color for {@code role}, or {@code null} if this theme omits it. */
    @Nullable
    public String getColor(@Nonnull String role) {
        return colors != null ? colors.get(role) : null;
    }
}
