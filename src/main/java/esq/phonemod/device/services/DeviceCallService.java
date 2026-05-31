package esq.phonemod.device.services;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DeviceCallService {

    void initiateCall(@Nonnull String callerDeviceId,
            @Nonnull String calleeDeviceId,
            @Nonnull String calleeName);

    void answerCall(@Nonnull String calleeDeviceId);

    void hangUp(@Nonnull String deviceId);

    boolean isInCall(@Nonnull String deviceId);

    @Nullable
    String getActivePartner(@Nonnull String deviceId);

    @Nullable
    String getPendingCallee(@Nonnull String deviceId);

    @Nullable
    String getPendingCaller(@Nonnull String deviceId);
}
