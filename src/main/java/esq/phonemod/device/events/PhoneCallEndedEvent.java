package esq.phonemod.device.events;

import javax.annotation.Nonnull;

/**
 * Fired once per participant when an active call ends.
 */
public final class PhoneCallEndedEvent implements DeviceEvent {

    private final String localDeviceId;
    private final String partnerDeviceId;

    public PhoneCallEndedEvent(@Nonnull String localDeviceId, @Nonnull String partnerDeviceId) {
        this.localDeviceId = localDeviceId;
        this.partnerDeviceId = partnerDeviceId;
    }

    @Nonnull
    public String getLocalDeviceId() {
        return localDeviceId;
    }

    @Nonnull
    public String getPartnerDeviceId() {
        return partnerDeviceId;
    }
}
