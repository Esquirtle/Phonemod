package esq.phonemod.device.events;

import javax.annotation.Nonnull;

/**
 * Fired once per participant when a pending call becomes active.
 */
public final class PhoneCallAnsweredEvent implements DeviceEvent {

    private final String localDeviceId;
    private final String partnerDeviceId;

    public PhoneCallAnsweredEvent(@Nonnull String localDeviceId, @Nonnull String partnerDeviceId) {
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
