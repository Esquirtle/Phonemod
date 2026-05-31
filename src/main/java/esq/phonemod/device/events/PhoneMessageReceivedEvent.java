package esq.phonemod.device.events;

import javax.annotation.Nonnull;

/**
 * Fired when a text message is received by a device.
 */
public final class PhoneMessageReceivedEvent implements DeviceEvent {

    private final String toDeviceId;
    private final String fromDeviceId;
    private final String fromName;
    private final String body;

    public PhoneMessageReceivedEvent(@Nonnull String toDeviceId,
            @Nonnull String fromDeviceId,
            @Nonnull String fromName,
            @Nonnull String body) {
        this.toDeviceId = toDeviceId;
        this.fromDeviceId = fromDeviceId;
        this.fromName = fromName;
        this.body = body;
    }

    @Nonnull
    public String getToDeviceId() {
        return toDeviceId;
    }

    @Nonnull
    public String getFromDeviceId() {
        return fromDeviceId;
    }

    @Nonnull
    public String getFromName() {
        return fromName;
    }

    @Nonnull
    public String getBody() {
        return body;
    }
}
