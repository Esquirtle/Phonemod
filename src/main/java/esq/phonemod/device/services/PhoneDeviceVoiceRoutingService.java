package esq.phonemod.device.services;

import com.hypixel.hytale.server.core.modules.voice.VoiceModule;

public final class PhoneDeviceVoiceRoutingService implements DeviceVoiceRoutingService {

    @Override
    public boolean isEnabled() {
        return VoiceModule.get().isVoiceEnabled();
    }
}

