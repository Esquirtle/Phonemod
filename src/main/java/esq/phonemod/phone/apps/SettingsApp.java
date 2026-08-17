package esq.phonemod.phone.apps;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.Value;
import esq.phonemod.device.components.DeviceSettings;
import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.device.events.DeviceSettingsChangedEvent;
import esq.phonemod.device.events.PhoneEvents;
import esq.phonemod.device.core.DeviceSession;
import esq.phonemod.PhoneMod;
import esq.phonemod.config.AddressBindingMode;
import esq.phonemod.config.CallAudioMode;
import esq.phonemod.config.PhonemodConfig;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Separate player-facing device preferences and permission-gated plugin controls.
 *
 * <p>Device choices fire {@code settings_set} and persist through
 * {@link DeviceSettingsComponent}. Permission-gated framework choices fire
 * {@code framework_config_set}, mutate validated server configuration, and queue an
 * ordered save. Both paths re-render the page from authoritative state.
 */
public final class SettingsApp extends StatefulPhoneApp<SettingsApp.State> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FRAMEWORK_CONFIG_PERMISSION = "phonemod.admin.deviceframework";

    // ── Generic set action ──────────────────────────────────────────────────────

    private static final String ACTION_SET    = "settings_set";
    private static final String ACTION_VIEW = "settings_view";
    private static final String ACTION_FRAMEWORK_SET = "framework_config_set";
    private static final String PARAM_VIEW = "View";
    private static final String PARAM_SETTING  = "Setting";
    private static final String PARAM_VALUE    = "Value";

    // ── Setting keys (the PARAM_SETTING values) ─────────────────────────────────

    private static final String SETTING_THEME     = "theme";
    private static final String SETTING_WALLPAPER = "wallpaper";
    private static final String SETTING_LAYOUT    = "layout";
    private static final String SETTING_SOUND     = "sound";
    private static final String SETTING_NOTIFICATIONS = "notifications";
    private static final String SETTING_MESSAGE_PREVIEW = "message_preview";
    private static final String SETTING_INCOMING_CALLS = "incoming_calls";
    private static final String SETTING_RESET = "reset";

    private static final String CONFIG_ADDRESS_BINDING = "address_binding";
    private static final String CONFIG_CALLS_ENABLED = "calls_enabled";
    private static final String CONFIG_CALL_AUDIO = "call_audio";
    private static final String CONFIG_CROSS_WORLD = "cross_world";
    private static final String CONFIG_INCOMING_OPEN = "incoming_open";
    private static final String CONFIG_RING_TIMEOUT = "ring_timeout";
    private static final String CONFIG_SPEAKER_RADIUS = "speaker_radius";
    private static final String CONFIG_OFFLINE_MESSAGES = "offline_messages";
    private static final String CONFIG_MESSAGE_LENGTH = "message_length";

    // ── Selectors (one source of truth for DustSettings.ui) ─────────────────────

    private static final String SEL_HEADER             = "#SettingsHeader";   // themeable: appHeader
    private static final String SEL_PANEL              = "#SettingsPanel";    // themeable: appPanel
    private static final String SEL_PHONE_NUMBER_LABEL = "#PhoneNumberLabel.Text";
    private static final String SEL_THEME_LABEL        = "#ThemeLabel.Text";
    private static final String SEL_WALLPAPER_LABEL    = "#WallpaperLabel.Text";
    private static final String SEL_LAYOUT_LABEL       = "#LayoutLabel.Text";
    private static final String SEL_SOUND_LABEL        = "#SoundLabel.Text";
    private static final String SEL_NOTIFICATIONS_LABEL = "#NotificationsLabel.Text";
    private static final String SEL_MESSAGE_PREVIEW_LABEL = "#MessagePreviewLabel.Text";
    private static final String SEL_PLAYER_CALLS_LABEL = "#PlayerCallsLabel.Text";
    private static final String SEL_PLAYER_TAB         = "#PlayerSettingsTab";
    private static final String SEL_PLUGIN_TAB         = "#PluginSettingsTab";
    private static final String SEL_PLAYER_VIEW        = "#PlayerSettingsView";
    private static final String SEL_PLUGIN_VIEW        = "#PluginSettingsView";
    private static final String SEL_ADDRESS_LABEL      = "#AddressBindingLabel.Text";
    private static final String SEL_CALLS_LABEL        = "#CallsEnabledLabel.Text";
    private static final String SEL_CALL_AUDIO_LABEL   = "#CallAudioLabel.Text";
    private static final String SEL_CROSS_WORLD_LABEL  = "#CrossWorldLabel.Text";
    private static final String SEL_INCOMING_LABEL     = "#IncomingCallsLabel.Text";
    private static final String SEL_RING_TIMEOUT_LABEL = "#RingTimeoutLabel.Text";
    private static final String SEL_SPEAKER_RADIUS_LABEL = "#SpeakerRadiusLabel.Text";
    private static final String SEL_OFFLINE_LABEL      = "#OfflineMessagingLabel.Text";
    private static final String SEL_MESSAGE_LENGTH_LABEL = "#MessageLengthLabel.Text";
    private static final String DUSTLIB_DOCUMENT = "Pages/DustLib.ui";
    private static final String STYLE_TAB_ACTIVE = "DustShellTabStyleActive";
    private static final String STYLE_TAB_INACTIVE = "DustShellTabStyleInactive";

    // ── State ─────────────────────────────────────────────────────────────────

    public enum State {
        PLAYER,
        PLUGIN
    }

    public SettingsApp() {
        super(State.PLAYER);
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
    public boolean isSystemApp() {
        return true;
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
        boolean canConfigureFramework = canConfigureFramework(ctx);
        State state = getState(ctx);
        if (state == State.PLUGIN && !canConfigureFramework) {
            state = State.PLAYER;
            setState(ctx, state);
        }

        bindView(evb, SEL_PLAYER_TAB, State.PLAYER);
        cmd.set(SEL_PLUGIN_TAB + ".Visible", canConfigureFramework);
        if (canConfigureFramework) bindView(evb, SEL_PLUGIN_TAB, State.PLUGIN);

        cmd.set(SEL_PLAYER_TAB + ".Style", tabStyle(state == State.PLAYER));
        cmd.set(SEL_PLUGIN_TAB + ".Style", tabStyle(state == State.PLUGIN));
        cmd.set(SEL_PLAYER_VIEW + ".Visible", state == State.PLAYER);
        cmd.set(SEL_PLUGIN_VIEW + ".Visible", state == State.PLUGIN);

        if (state == State.PLAYER) {
            buildPlayerControls(ctx, cmd, evb);
        } else {
            buildFrameworkControls(cmd, evb, PhoneMod.config());
        }
    }

    private void buildPlayerControls(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        cmd.set(SEL_PHONE_NUMBER_LABEL, ctx.getPhoneNumber());
        DeviceSettings s = readSettings(ctx);

        cmd.set(SEL_THEME_LABEL, "Theme: " + valueOr(s.getThemeId(), "Default"));
        bindChoice(evb, "#ThemeDefault", SETTING_THEME, "Default");
        bindChoice(evb, "#ThemeDark", SETTING_THEME, "Dark");
        bindChoice(evb, "#ThemeCompact", SETTING_THEME, "Compact");

        cmd.set(SEL_WALLPAPER_LABEL, "Wallpaper: " + valueOr(s.getWallpaper(), "Logo"));
        bindChoice(evb, "#WallpaperLogo", SETTING_WALLPAPER, "Logo");
        bindChoice(evb, "#WallpaperDark", SETTING_WALLPAPER, "Dark");
        bindChoice(evb, "#WallpaperNone", SETTING_WALLPAPER, "None");

        cmd.set(SEL_LAYOUT_LABEL, "App Layout: " + valueOr(s.getAppLayout(), "Grid"));
        bindChoice(evb, "#LayoutGrid", SETTING_LAYOUT, "Grid");
        bindChoice(evb, "#LayoutList", SETTING_LAYOUT, "List");

        cmd.set(SEL_SOUND_LABEL, "Sound Profile: " + valueOr(s.getSoundProfileId(), "Nonkia"));
        bindChoice(evb, "#SoundNonkia", SETTING_SOUND, "Nonkia");
        bindChoice(evb, "#SoundSilent", SETTING_SOUND, "Silent");

        cmd.set(SEL_NOTIFICATIONS_LABEL, "Message alerts: "
                + enabled(s.isNotificationsEnabled()));
        bindChoice(evb, "#NotificationsEnabled", SETTING_NOTIFICATIONS, "true");
        bindChoice(evb, "#NotificationsDisabled", SETTING_NOTIFICATIONS, "false");

        cmd.set(SEL_MESSAGE_PREVIEW_LABEL, "Message preview: "
                + (s.isMessagePreviewEnabled() ? "SHOWN" : "HIDDEN"));
        bindChoice(evb, "#PreviewEnabled", SETTING_MESSAGE_PREVIEW, "true");
        bindChoice(evb, "#PreviewDisabled", SETTING_MESSAGE_PREVIEW, "false");

        cmd.set(SEL_PLAYER_CALLS_LABEL, "Incoming calls: "
                + (s.isIncomingCallsEnabled() ? "ALLOWED" : "DO NOT DISTURB"));
        bindChoice(evb, "#PlayerCallsEnabled", SETTING_INCOMING_CALLS, "true");
        bindChoice(evb, "#PlayerCallsDisabled", SETTING_INCOMING_CALLS, "false");

        bindChoice(evb, "#ResetPlayerSettings", SETTING_RESET, "defaults");
    }

    private void buildFrameworkControls(@Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb, @Nonnull PhonemodConfig config) {
        cmd.set(SEL_ADDRESS_LABEL, "Number binding: " + config.addressBindingMode());
        bindFrameworkChoice(evb, "#AddressDevice", CONFIG_ADDRESS_BINDING, "DEVICE");
        bindFrameworkChoice(evb, "#AddressPlayer", CONFIG_ADDRESS_BINDING, "PLAYER");

        cmd.set(SEL_CALLS_LABEL, "New calls: " + enabled(config.callsEnabled()));
        bindFrameworkChoice(evb, "#CallsEnabled", CONFIG_CALLS_ENABLED, "true");
        bindFrameworkChoice(evb, "#CallsDisabled", CONFIG_CALLS_ENABLED, "false");

        cmd.set(SEL_CALL_AUDIO_LABEL, "Call privacy: " + config.callAudioMode());
        bindFrameworkChoice(evb, "#CallAudioAsset", CONFIG_CALL_AUDIO, "ASSET");
        bindFrameworkChoice(evb, "#CallAudioPrivate", CONFIG_CALL_AUDIO, "PRIVATE");
        bindFrameworkChoice(evb, "#CallAudioSpeaker", CONFIG_CALL_AUDIO, "SPEAKER");
        bindFrameworkChoice(evb, "#CallAudioToggle", CONFIG_CALL_AUDIO, "TOGGLEABLE");

        cmd.set(SEL_CROSS_WORLD_LABEL, "Cross-world calls: "
                + enabled(config.allowCrossWorldCalls()));
        bindFrameworkChoice(evb, "#CrossWorldEnabled", CONFIG_CROSS_WORLD, "true");
        bindFrameworkChoice(evb, "#CrossWorldDisabled", CONFIG_CROSS_WORLD, "false");

        cmd.set(SEL_INCOMING_LABEL, "Incoming calls: "
                + (config.requireOpenDeviceForIncomingCalls() ? "DEVICE OPEN" : "CONNECTED"));
        bindFrameworkChoice(evb, "#IncomingConnected", CONFIG_INCOMING_OPEN, "false");
        bindFrameworkChoice(evb, "#IncomingOpenOnly", CONFIG_INCOMING_OPEN, "true");

        cmd.set(SEL_RING_TIMEOUT_LABEL, "Ring timeout: "
                + config.callRingTimeoutSeconds() + " seconds");
        bindFrameworkChoice(evb, "#Ring15", CONFIG_RING_TIMEOUT, "15");
        bindFrameworkChoice(evb, "#Ring30", CONFIG_RING_TIMEOUT, "30");
        bindFrameworkChoice(evb, "#Ring60", CONFIG_RING_TIMEOUT, "60");

        cmd.set(SEL_SPEAKER_RADIUS_LABEL, "Speaker radius: "
                + config.speakerRadiusBlocks() + " blocks");
        bindFrameworkChoice(evb, "#Speaker4", CONFIG_SPEAKER_RADIUS, "4");
        bindFrameworkChoice(evb, "#Speaker8", CONFIG_SPEAKER_RADIUS, "8");
        bindFrameworkChoice(evb, "#Speaker16", CONFIG_SPEAKER_RADIUS, "16");

        cmd.set(SEL_OFFLINE_LABEL, "Offline mail: " + enabled(config.offlineMessagingEnabled()));
        bindFrameworkChoice(evb, "#OfflineEnabled", CONFIG_OFFLINE_MESSAGES, "true");
        bindFrameworkChoice(evb, "#OfflineDisabled", CONFIG_OFFLINE_MESSAGES, "false");

        cmd.set(SEL_MESSAGE_LENGTH_LABEL, "Message limit: "
                + config.maxMessageCharacters() + " characters");
        bindFrameworkChoice(evb, "#Message128", CONFIG_MESSAGE_LENGTH, "128");
        bindFrameworkChoice(evb, "#Message512", CONFIG_MESSAGE_LENGTH, "512");
        bindFrameworkChoice(evb, "#Message1024", CONFIG_MESSAGE_LENGTH, "1024");
    }

    private void bindChoice(@Nonnull UIEventBuilder evb, @Nonnull String selector,
            @Nonnull String setting, @Nonnull String value) {
        PhoneUi.bindAction(evb, selector, ACTION_SET,
                PhoneUi.params(PARAM_SETTING, setting).append(PARAM_VALUE, value));
    }

    private void bindFrameworkChoice(@Nonnull UIEventBuilder evb, @Nonnull String selector,
            @Nonnull String setting, @Nonnull String value) {
        PhoneUi.bindAction(evb, selector, ACTION_FRAMEWORK_SET,
                PhoneUi.params(PARAM_SETTING, setting).append(PARAM_VALUE, value));
    }

    private void bindView(@Nonnull UIEventBuilder evb, @Nonnull String selector,
            @Nonnull State state) {
        PhoneUi.bindAction(evb, selector, ACTION_VIEW,
                PhoneUi.params(PARAM_VIEW, state.name()));
    }

    @Nonnull
    private Value<?> tabStyle(boolean active) {
        return Value.ref(DUSTLIB_DOCUMENT,
                active ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
    }

    @Nonnull
    private static String enabled(boolean value) {
        return value ? "ENABLED" : "DISABLED";
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
        if (ACTION_SET.equals(event.getAction()) && getState(ctx) == State.PLAYER) {
            String setting = event.getParam(PARAM_SETTING);
            String value = event.getParam(PARAM_VALUE);
            if (setting != null && value != null && !value.isBlank() && applySetting(ctx, setting, value)) {
                build(ctx, cmd, evb);
                return true;
            }
        }
        if (ACTION_VIEW.equals(event.getAction())) {
            String requested = event.getParam(PARAM_VIEW);
            if (State.PLAYER.name().equals(requested)) {
                setState(ctx, State.PLAYER);
            } else if (State.PLUGIN.name().equals(requested) && canConfigureFramework(ctx)) {
                setState(ctx, State.PLUGIN);
            } else {
                return false;
            }
            build(ctx, cmd, evb);
            return true;
        }
        if (ACTION_FRAMEWORK_SET.equals(event.getAction())
                && getState(ctx) == State.PLUGIN && canConfigureFramework(ctx)) {
            String setting = event.getParam(PARAM_SETTING);
            String value = event.getParam(PARAM_VALUE);
            if (setting != null && value != null && applyFrameworkSetting(ctx, setting, value)) {
                build(ctx, cmd, evb);
                return true;
            }
        }
        return false;
    }

    private boolean applyFrameworkSetting(@Nonnull PhoneAppContext ctx,
            @Nonnull String setting, @Nonnull String value) {
        if (!canConfigureFramework(ctx)) return false;
        PhonemodConfig config = PhoneMod.config();
        try {
            switch (setting) {
                case CONFIG_ADDRESS_BINDING ->
                        config.setAddressBindingMode(AddressBindingMode.valueOf(value));
                case CONFIG_CALLS_ENABLED -> config.setCallsEnabled(parseBoolean(value));
                case CONFIG_CALL_AUDIO -> config.setCallAudioMode(CallAudioMode.valueOf(value));
                case CONFIG_CROSS_WORLD -> config.setAllowCrossWorldCalls(parseBoolean(value));
                case CONFIG_INCOMING_OPEN ->
                        config.setRequireOpenDeviceForIncomingCalls(parseBoolean(value));
                case CONFIG_RING_TIMEOUT ->
                        config.setCallRingTimeoutSeconds(Integer.parseInt(value));
                case CONFIG_SPEAKER_RADIUS ->
                        config.setSpeakerRadiusBlocks(Integer.parseInt(value));
                case CONFIG_OFFLINE_MESSAGES ->
                        config.setOfflineMessagingEnabled(parseBoolean(value));
                case CONFIG_MESSAGE_LENGTH ->
                        config.setMaxMessageCharacters(Integer.parseInt(value));
                default -> { return false; }
            }
        } catch (IllegalArgumentException error) {
            LOGGER.atWarning().log("Rejected framework config setting=%s value=%s",
                    setting, value);
            return false;
        }

        PhoneMod.getInstance().saveConfig().exceptionally(error -> {
            LOGGER.atSevere().withCause(error).log("Could not save Phonemod configuration");
            return null;
        });
        ctx.sendNotification("Framework configuration", "Saved " + setting + ".");
        return true;
    }

    private static boolean parseBoolean(@Nonnull String value) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IllegalArgumentException("Expected boolean");
    }

    private static boolean canConfigureFramework(@Nonnull PhoneAppContext ctx) {
        return ctx.getPlayerRef().hasPermission(FRAMEWORK_CONFIG_PERMISSION);
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
        boolean reset = SETTING_RESET.equals(setting);
        if (reset && !"defaults".equals(value)) return false;
        DeviceSettings updated = reset
                ? new DeviceSettings()
                : (previous != null ? new DeviceSettings(previous) : new DeviceSettings());

        if (!reset) switch (setting) {
            case SETTING_THEME -> {
                if (!java.util.Set.of("Default", "Dark", "Compact").contains(value)) return false;
                updated.setThemeId(value);
            }
            case SETTING_WALLPAPER -> {
                if (!java.util.Set.of("Logo", "Dark", "None").contains(value)) return false;
                updated.setWallpaper(value);
            }
            case SETTING_LAYOUT -> {
                if (!java.util.Set.of("Grid", "List").contains(value)) return false;
                updated.setAppLayout(value);
            }
            case SETTING_SOUND -> {
                if (!java.util.Set.of("Nonkia", "Silent").contains(value)) return false;
                updated.setSoundProfileId(value);
            }
            case SETTING_NOTIFICATIONS -> updated.setNotificationsEnabled(parseBoolean(value));
            case SETTING_MESSAGE_PREVIEW -> updated.setMessagePreviewEnabled(parseBoolean(value));
            case SETTING_INCOMING_CALLS -> updated.setIncomingCallsEnabled(parseBoolean(value));
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
