package esq.phonemod.config;

import esq.phonemod.device.assets.CallAudioPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallAudioModeTest {
    @Test
    void assetModeReturnsTheAssetPolicyUnchanged() {
        for (CallAudioPolicy policy : CallAudioPolicy.values()) {
            assertEquals(policy, CallAudioMode.ASSET.resolve(policy));
        }
    }

    @Test
    void overrideModesIgnoreTheAssetPolicy() {
        for (CallAudioPolicy policy : CallAudioPolicy.values()) {
            assertEquals(CallAudioPolicy.PRIVATE, CallAudioMode.PRIVATE.resolve(policy));
            assertEquals(CallAudioPolicy.SPEAKER, CallAudioMode.SPEAKER.resolve(policy));
            assertEquals(CallAudioPolicy.TOGGLEABLE, CallAudioMode.TOGGLEABLE.resolve(policy));
        }
    }
}
