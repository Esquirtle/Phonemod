package esq.phonemod.device.events;

import esq.phonemod.device.components.DeviceSettings;
import esq.phonemod.device.core.DeviceSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fired when device settings are changed for a physical device.
 *
 * <p>The {@link #getPreviousSettings()} value is null when settings are
 * written for the first time.
 */
public final class DeviceSettingsChangedEvent implements DeviceEvent {

    private final DeviceSession session;
    private final DeviceSettings newSettings;
    private final DeviceSettings previousSettings;

    public DeviceSettingsChangedEvent(@Nonnull DeviceSession session,
            @Nonnull DeviceSettings newSettings,
            @Nullable DeviceSettings previousSettings) {
        this.session = session;
        this.newSettings = newSettings;
        this.previousSettings = previousSettings;
    }

    @Nonnull
    public DeviceSession getSession() {
        return session;
    }

    @Nonnull
    public DeviceSettings getNewSettings() {
        return newSettings;
    }

    @Nullable
    public DeviceSettings getPreviousSettings() {
        return previousSettings;
    }
}
