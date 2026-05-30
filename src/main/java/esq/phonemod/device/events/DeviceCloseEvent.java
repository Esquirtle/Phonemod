package esq.phonemod.device.events;

import esq.phonemod.device.core.DeviceSession;

import javax.annotation.Nonnull;

/**
 * Fired when a device page is closed (dismissed) for a player.
 */
public final class DeviceCloseEvent implements DeviceEvent {

    private final DeviceSession session;

    public DeviceCloseEvent(@Nonnull DeviceSession session) {
        this.session = session;
    }

    @Nonnull
    public DeviceSession getSession() {
        return session;
    }
}
