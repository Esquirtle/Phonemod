package esq.phonemod.device.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceAssetDefaultsTest {
    @Test
    void defaultCallAudioPolicyIsPrivate() {
        assertEquals(CallAudioPolicy.PRIVATE, new DeviceAsset().getCallAudioPolicy());
    }
}
