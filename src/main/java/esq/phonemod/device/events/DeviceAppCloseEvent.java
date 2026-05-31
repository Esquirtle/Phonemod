package esq.phonemod.device.events;

import esq.phonemod.device.core.DeviceSession;

import javax.annotation.Nonnull;

/**
 * Fired when an app is closed on a device (player navigated away or device closed).
 */
public final class DeviceAppCloseEvent implements DeviceEvent {

    private final DeviceSession session;
    private final String appId;

    public DeviceAppCloseEvent(@Nonnull DeviceSession session, @Nonnull String appId) {
        this.session = session;
        this.appId = appId;
    }

    @Nonnull
    public DeviceSession getSession() {
        return session;
    }

    @Nonnull
    public String getAppId() {
        return appId;
    }
}
