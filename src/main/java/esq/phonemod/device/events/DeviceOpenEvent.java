package esq.phonemod.device.events;

import esq.phonemod.device.core.DeviceSession;

import javax.annotation.Nonnull;

/**
 * Fired when a device page is opened for a player.
 */
public final class DeviceOpenEvent implements DeviceEvent {

    private final DeviceSession session;

    public DeviceOpenEvent(@Nonnull DeviceSession session) {
        this.session = session;
    }

    @Nonnull
    public DeviceSession getSession() {
        return session;
    }
}
