package esq.phonemod.device.events;

import esq.phonemod.device.core.DeviceSession;

import javax.annotation.Nonnull;

/**
 * Fired when an app is opened on a device.
 */
public final class DeviceAppOpenEvent implements DeviceEvent {

    private final DeviceSession session;
    private final String appId;

    public DeviceAppOpenEvent(@Nonnull DeviceSession session, @Nonnull String appId) {
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
