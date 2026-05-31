package esq.phonemod.device.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mutable settings for one physical device.
 */
public final class DeviceSettings {

    private static final String[] EMPTY_STRINGS = new String[0];

    public static final BuilderCodec<DeviceSettings> CODEC = BuilderCodec
            .builder(DeviceSettings.class, DeviceSettings::new)
            .append(new KeyedCodec<>("ThemeId", Codec.STRING),
                    (settings, value) -> settings.themeId = value,
                    settings -> settings.themeId)
            .add()
            .append(new KeyedCodec<>("Wallpaper", Codec.STRING),
                    (settings, value) -> settings.wallpaper = value,
                    settings -> settings.wallpaper)
            .add()
            .append(new KeyedCodec<>("IconPack", Codec.STRING),
                    (settings, value) -> settings.iconPack = value,
                    settings -> settings.iconPack)
            .add()
            .append(new KeyedCodec<>("AppLayout", Codec.STRING),
                    (settings, value) -> settings.appLayout = value,
                    settings -> settings.appLayout)
            .add()
            .append(new KeyedCodec<>("SoundProfileId", Codec.STRING),
                    (settings, value) -> settings.soundProfileId = value,
                    settings -> settings.soundProfileId)
            .add()
            .append(new KeyedCodec<>("ShellPreset", Codec.STRING),
                    (settings, value) -> settings.shellPreset = value,
                    settings -> settings.shellPreset)
            .add()
            .append(new KeyedCodec<>("StatusModules", Codec.STRING_ARRAY),
                    (settings, value) -> settings.statusModules = value,
                    settings -> settings.statusModules)
            .add()
            .build();

    private String themeId;
    private String wallpaper;
    private String iconPack;
    private String appLayout;
    private String soundProfileId;
    private String shellPreset;
    private String[] statusModules = EMPTY_STRINGS;

    public DeviceSettings() {
    }

    public DeviceSettings(@Nonnull DeviceSettings source) {
        this.themeId = source.themeId;
        this.wallpaper = source.wallpaper;
        this.iconPack = source.iconPack;
        this.appLayout = source.appLayout;
        this.soundProfileId = source.soundProfileId;
        this.shellPreset = source.shellPreset;
        this.statusModules = source.statusModules != null ? source.statusModules.clone() : EMPTY_STRINGS;
    }

    @Nullable
    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(@Nullable String themeId) {
        this.themeId = themeId;
    }

    @Nullable
    public String getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(@Nullable String wallpaper) {
        this.wallpaper = wallpaper;
    }

    @Nullable
    public String getIconPack() {
        return iconPack;
    }

    public void setIconPack(@Nullable String iconPack) {
        this.iconPack = iconPack;
    }

    @Nullable
    public String getAppLayout() {
        return appLayout;
    }

    public void setAppLayout(@Nullable String appLayout) {
        this.appLayout = appLayout;
    }

    @Nullable
    public String getSoundProfileId() {
        return soundProfileId;
    }

    public void setSoundProfileId(@Nullable String soundProfileId) {
        this.soundProfileId = soundProfileId;
    }

    @Nullable
    public String getShellPreset() {
        return shellPreset;
    }

    public void setShellPreset(@Nullable String shellPreset) {
        this.shellPreset = shellPreset;
    }

    @Nonnull
    public String[] getStatusModules() {
        return statusModules != null ? statusModules.clone() : EMPTY_STRINGS;
    }

    public void setStatusModules(@Nullable String[] statusModules) {
        this.statusModules = statusModules != null ? statusModules.clone() : EMPTY_STRINGS;
    }
}

