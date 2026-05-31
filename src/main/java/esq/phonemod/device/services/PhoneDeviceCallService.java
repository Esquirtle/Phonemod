package esq.phonemod.device.services;

import esq.phonemod.phone.messaging.CallRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PhoneDeviceCallService implements DeviceCallService {

    @Override
    public void initiateCall(@Nonnull String callerDeviceId,
            @Nonnull String calleeDeviceId,
            @Nonnull String calleeName) {
        CallRegistry.initiateCall(callerDeviceId, calleeDeviceId, calleeName);
    }

    @Override
    public void answerCall(@Nonnull String calleeDeviceId) {
        CallRegistry.answerCall(calleeDeviceId);
    }

    @Override
    public void hangUp(@Nonnull String deviceId) {
        CallRegistry.hangUp(deviceId);
    }

    @Override
    public boolean isInCall(@Nonnull String deviceId) {
        return CallRegistry.isInCall(deviceId);
    }

    @Nullable
    @Override
    public String getActivePartner(@Nonnull String deviceId) {
        return CallRegistry.getActivePartner(deviceId);
    }

    @Nullable
    @Override
    public String getPendingCallee(@Nonnull String deviceId) {
        return CallRegistry.getPendingCallee(deviceId);
    }

    @Nullable
    @Override
    public String getPendingCaller(@Nonnull String deviceId) {
        return CallRegistry.getPendingCaller(deviceId);
    }
}

