package esq.phonemod.phone.apps;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.device.components.DeviceSettings;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.device.events.DeviceSettingsChangedEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.device.core.DeviceSession;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * MOCKED/BOILERPLATE: Settings app that demonstrates reading and writing every
 * {@link DeviceSettings} dimension. It is a framework reference, not a finished
 * settings product — the choices are hardcoded mock strings and only `theme`
 * has a visible effect (re-skin via {@link DeviceSettingsChangedEvent}).
 *
 * <p>Each dimension shows its current value and offers 2–3 mock choices. Every
 * choice fires one generic action ({@code settings_set}) carrying the target
 * setting + value; {@link #applySetting} persists it via
 * {@link DeviceSettingsComponent} and posts {@link DeviceSettingsChangedEvent}.
 */
public final class SettingsApp extends StatefulPhoneApp<SettingsApp.State> {

    // ── Generic set action ──────────────────────────────────────────────────────

    private static final String ACTION_SET    = "settings_set";
    private static final String PARAM_SETTING  = "Setting";
    private static final String PARAM_VALUE    = "Value";

    // ── Setting keys (the PARAM_SETTING values) ─────────────────────────────────

    private static final String SETTING_THEME     = "theme";
    private static final String SETTING_WALLPAPER = "wallpaper";
    private static final String SETTING_ICONPACK  = "iconpack";
    private static final String SETTING_LAYOUT    = "layout";
    private static final String SETTING_SOUND     = "sound";

    // ── Selectors (one source of truth for DustSettings.ui) ─────────────────────

    private static final String SEL_HEADER             = "#SettingsHeader";   // themeable: appHeader
    private static final String SEL_PANEL              = "#SettingsPanel";    // themeable: appPanel
    private static final String SEL_PHONE_NUMBER_LABEL = "#PhoneNumberLabel.Text";
    private static final String SEL_THEME_LABEL        = "#ThemeLabel.Text";
    private static final String SEL_WALLPAPER_LABEL    = "#WallpaperLabel.Text";
    private static final String SEL_ICONPACK_LABEL     = "#IconPackLabel.Text";
    private static final String SEL_LAYOUT_LABEL       = "#LayoutLabel.Text";
    private static final String SEL_SOUND_LABEL        = "#SoundLabel.Text";

    // ── State ─────────────────────────────────────────────────────────────────

    public enum State {
        MAIN
    }

    public SettingsApp() {
        super(State.MAIN);
    }

    // ── Identity ───────────────────────────────────────────────────────────────

    @Override
    public String getId() {
        return "settings";
    }

    @Override
    public String getDisplayName() {
        return "Settings";
    }

    @Override
    public String getAppButtonUI() {
        return PhoneAssetPaths.SETTINGS_BUTTON_UI;
    }

    @Override
    public String getUIPath() {
        return PhoneAssetPaths.DUST_SETTINGS_UI;
    }

    @Override
    public String getIconPath() {
        return "Pages/Phone/Settings.png";
    }

    @Override
    public Map<String, String> getThemeableSelectors() {
        return Map.of(
                "appHeader", SEL_HEADER,
                "appPanel", SEL_PANEL);
    }

    // ── Build ──────────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        appendMainUI(ctx, cmd);
        cmd.set(SEL_PHONE_NUMBER_LABEL, ctx.getPhoneNumber());

        DeviceSettings s = readSettings(ctx);

        cmd.set(SEL_THEME_LABEL, "Theme: " + valueOr(s.getThemeId(), "Default"));
        bindChoice(evb, "#ThemeDefault", SETTING_THEME, "Default");
        bindChoice(evb, "#ThemeDark",    SETTING_THEME, "Dark");
        bindChoice(evb, "#ThemeCompact", SETTING_THEME, "Compact");

        cmd.set(SEL_WALLPAPER_LABEL, "Wallpaper: " + valueOr(s.getWallpaper(), "Logo"));
        bindChoice(evb, "#WallpaperLogo", SETTING_WALLPAPER, "Logo");
        bindChoice(evb, "#WallpaperDark", SETTING_WALLPAPER, "Dark");
        bindChoice(evb, "#WallpaperNone", SETTING_WALLPAPER, "None");

        cmd.set(SEL_ICONPACK_LABEL, "Icon Pack: " + valueOr(s.getIconPack(), "Default"));
        bindChoice(evb, "#IconPackDefault", SETTING_ICONPACK, "Default");
        bindChoice(evb, "#IconPackMono",    SETTING_ICONPACK, "Mono");

        cmd.set(SEL_LAYOUT_LABEL, "App Layout: " + valueOr(s.getAppLayout(), "Grid"));
        bindChoice(evb, "#LayoutGrid", SETTING_LAYOUT, "Grid");
        bindChoice(evb, "#LayoutList", SETTING_LAYOUT, "List");

        cmd.set(SEL_SOUND_LABEL, "Sound Profile: " + valueOr(s.getSoundProfileId(), "Nonkia"));
        bindChoice(evb, "#SoundNonkia", SETTING_SOUND, "Nonkia");
        bindChoice(evb, "#SoundSilent", SETTING_SOUND, "Silent");
    }

    private void bindChoice(@Nonnull UIEventBuilder evb, @Nonnull String selector,
            @Nonnull String setting, @Nonnull String value) {
        PhoneUi.bindAction(evb, selector, ACTION_SET,
                PhoneUi.params(PARAM_SETTING, setting).append(PARAM_VALUE, value));
    }

    @Nonnull
    private static String valueOr(String value, @Nonnull String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    // ── Event handling ─────────────────────────────────────────────────────────

    @Override
    public boolean handleEvent(@Nonnull PhoneAppContext ctx,
            @Nonnull PhoneEvent event,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        if (ACTION_SET.equals(event.getAction())) {
            String setting = event.getParam(PARAM_SETTING);
            String value = event.getParam(PARAM_VALUE);
            if (setting != null && value != null && !value.isBlank() && applySetting(ctx, setting, value)) {
                build(ctx, cmd, evb);
                return true;
            }
        }
        return false;
    }

    // ── Settings helpers ───────────────────────────────────────────────────────

    /**
     * Reads the current {@link DeviceSettings} for the device that opened this app.
     * Returns a new (empty) {@link DeviceSettings} if none have been written yet.
     */
    @Nonnull
    private DeviceSettings readSettings(@Nonnull PhoneAppContext ctx) {
        ComponentType<EntityStore, DeviceSettingsComponent> type =
                DeviceSettingsComponent.getComponentType();
        if (type == null) {
            return new DeviceSettings();
        }
        DeviceSettingsComponent component = ctx.getStore().getComponent(ctx.getRef(), type);
        if (component == null) {
            return new DeviceSettings();
        }
        DeviceSettings existing = component.get(ctx.getPhoneNumber());
        return existing != null ? existing : new DeviceSettings();
    }

    /**
     * Applies one setting value into {@link DeviceSettingsComponent} and posts a
     * {@link DeviceSettingsChangedEvent}. Direct write — we are on the world thread
     * when called from {@link #handleEvent}. Returns {@code false} for an unknown
     * setting key (no write, no event).
     */
    private boolean applySetting(@Nonnull PhoneAppContext ctx,
            @Nonnull String setting, @Nonnull String value) {
        ComponentType<EntityStore, DeviceSettingsComponent> type =
                DeviceSettingsComponent.getComponentType();
        if (type == null) {
            return false;
        }

        DeviceSettingsComponent component =
                ctx.getStore().ensureAndGetComponent(ctx.getRef(), type);
        DeviceSettings previous = component.get(ctx.getPhoneNumber());
        DeviceSettings updated = previous != null ? new DeviceSettings(previous) : new DeviceSettings();

        switch (setting) {
            case SETTING_THEME     -> updated.setThemeId(value);
            case SETTING_WALLPAPER -> updated.setWallpaper(value);
            case SETTING_ICONPACK  -> updated.setIconPack(value);
            case SETTING_LAYOUT    -> updated.setAppLayout(value);
            case SETTING_SOUND     -> updated.setSoundProfileId(value);
            default -> {
                return false;
            }
        }

        component.put(ctx.getPhoneNumber(), updated);
        ctx.getStore().putComponent(ctx.getRef(), type, component);

        // Best-effort notification; skipped if no open session (e.g. unit tests).
        DeviceSession session = resolveSession(ctx);
        if (session != null) {
            PhoneEvents.post(new DeviceSettingsChangedEvent(session, updated, previous));
        }
        return true;
    }

    /**
     * Attempts to find the open {@link DeviceSession} for the current player.
     * Returns null if no session is found (e.g. during unit tests or if the
     * registry has not been populated yet).
     */
    private static DeviceSession resolveSession(@Nonnull PhoneAppContext ctx) {
        try {
            var entry = esq.phonemod.phone.messaging.PhoneRegistry
                    .getOnlineEntry(ctx.getPhoneNumber());
            if (entry == null) {
                return null;
            }
            if (entry.page() instanceof esq.phonemod.device.ui.DevicePage devicePage) {
                return devicePage.getSession();
            }
        } catch (Exception ignored) {
            // Not critical — event posting is best-effort.
        }
        return null;
    }
}
