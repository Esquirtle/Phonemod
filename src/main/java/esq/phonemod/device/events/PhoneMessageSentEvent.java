package esq.phonemod.device.events;

import javax.annotation.Nonnull;

/**
 * Fired when a text message is sent from a device.
 */
public final class PhoneMessageSentEvent implements DeviceEvent {

    private final String fromDeviceId;
    private final String toDeviceId;
    private final String body;

    public PhoneMessageSentEvent(@Nonnull String fromDeviceId,
            @Nonnull String toDeviceId,
            @Nonnull String body) {
        this.fromDeviceId = fromDeviceId;
        this.toDeviceId = toDeviceId;
        this.body = body;
    }

    @Nonnull
    public String getFromDeviceId() {
        return fromDeviceId;
    }

    @Nonnull
    public String getToDeviceId() {
        return toDeviceId;
    }

    @Nonnull
    public String getBody() {
        return body;
    }
}
