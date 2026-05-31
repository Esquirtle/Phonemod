package esq.phonemod.device.events;

import javax.annotation.Nonnull;

/**
 * Fired when an incoming call goes unanswered and is cleared.
 */
public final class PhoneCallMissedEvent implements DeviceEvent {

    private final String calleeDeviceId;
    private final String callerDeviceId;

    public PhoneCallMissedEvent(@Nonnull String calleeDeviceId, @Nonnull String callerDeviceId) {
        this.calleeDeviceId = calleeDeviceId;
        this.callerDeviceId = callerDeviceId;
    }

    @Nonnull
    public String getCalleeDeviceId() {
        return calleeDeviceId;
    }

    @Nonnull
    public String getCallerDeviceId() {
        return callerDeviceId;
    }
}
