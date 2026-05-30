package esq.phonemod.device.theme;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import esq.phonemod.device.assets.DeviceThemeAsset;
import esq.phonemod.device.core.DeviceShell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Applies device theme palettes at runtime.
 *
 * <p>Themes are pure data ({@link DeviceThemeAsset}, loaded from
 * {@code Server/Phonemod/Themes/*.json}). A palette is a map of
 * <em>role &rarr; hex color</em>; a device declares which on-screen selectors
 * play each role via its {@code Selectors} map, and apps may declare additional
 * themeable selectors. This service sets the {@code Background} of each matched
 * selector to the palette color.
 *
 * <p>Only background colors are injected this way — text colors live inside
 * {@code Style} objects and cannot be set through a selector path, so they
 * remain baked into the library defaults. Backgrounds carry the bulk of a
 * theme's identity.
 *
 * <p>Applying a color to a selector that is not currently present is a safe
 * no-op (the client ignores it), so an app's themeable selectors can be applied
 * even when that app is not on screen.
 */
public final class ThemeService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Property suffix used to recolor an element. */
    private static final String BACKGROUND = ".Background";

    private ThemeService() {
    }

    /**
     * Applies the shell's themeable selectors (from {@link DeviceShell#getSelectors()})
     * using the palette for {@code themeId}, falling back to the shell's default theme.
     */
    public static void applyShell(@Nonnull UICommandBuilder cmd,
            @Nonnull DeviceShell shell,
            @Nullable String themeId) {
        apply(cmd, themeId, shell.getDefaultThemeId(), shell.getSelectors());
    }

    /**
     * Applies an arbitrary role &rarr; selector map using the palette for
     * {@code themeId} (falling back to {@code fallbackThemeId}). Used for both the
     * shell and per-app themeable selector maps.
     */
    public static void apply(@Nonnull UICommandBuilder cmd,
            @Nullable String themeId,
            @Nullable String fallbackThemeId,
            @Nullable Map<String, String> roleToSelector) {
        if (roleToSelector == null || roleToSelector.isEmpty()) {
            return;
        }
        DeviceThemeAsset palette = resolvePalette(themeId, fallbackThemeId);
        if (palette == null) {
            return;
        }
        for (Map.Entry<String, String> entry : roleToSelector.entrySet()) {
            String selector = entry.getValue();
            String color = palette.getColor(entry.getKey());
            if (color != null && !color.isBlank() && selector != null && !selector.isBlank()) {
                cmd.set(selector + BACKGROUND, color);
            }
        }
    }

    /**
     * Resolves a palette by id, falling back to {@code fallbackThemeId}, then to
     * {@code null} (no theming) if neither resolves.
     */
    @Nullable
    public static DeviceThemeAsset resolvePalette(@Nullable String themeId, @Nullable String fallbackThemeId) {
        DefaultAssetMap<String, DeviceThemeAsset> map;
        try {
            map = DeviceThemeAsset.getAssetMap();
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[ThemeService] Theme asset map unavailable");
            return null;
        }
        DeviceThemeAsset palette = safeGet(map, themeId);
        if (palette == null) {
            palette = safeGet(map, fallbackThemeId);
        }
        return palette;
    }

    @Nullable
    private static DeviceThemeAsset safeGet(@Nonnull DefaultAssetMap<String, DeviceThemeAsset> map,
            @Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return map.getAsset(id);
        } catch (Exception e) {
            return null;
        }
    }
}
