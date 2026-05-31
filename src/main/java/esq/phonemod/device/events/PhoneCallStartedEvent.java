package esq.phonemod.device.events;

import javax.annotation.Nonnull;

/**
 * Fired when a phone call is initiated (outgoing).
 */
public final class PhoneCallStartedEvent implements DeviceEvent {

    private final String callerDeviceId;
    private final String calleeDeviceId;

    public PhoneCallStartedEvent(@Nonnull String callerDeviceId, @Nonnull String calleeDeviceId) {
        this.callerDeviceId = callerDeviceId;
        this.calleeDeviceId = calleeDeviceId;
    }

    @Nonnull
    public String getCallerDeviceId() {
        return callerDeviceId;
    }

    @Nonnull
    public String getCalleeDeviceId() {
        return calleeDeviceId;
    }
}
