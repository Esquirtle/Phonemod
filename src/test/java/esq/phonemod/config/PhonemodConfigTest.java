package esq.phonemod.config;

import esq.phonemod.device.assets.CallAudioPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PhonemodConfigTest {

    @Test
    void developmentControlsPreserveCompatibilityDefaults() {
        PhonemodConfig config = new PhonemodConfig();
        assertEquals(AddressBindingMode.DEVICE, config.addressBindingMode());
        assertTrue(config.callsEnabled());
        assertEquals(CallAudioMode.ASSET, config.callAudioMode());
        assertTrue(config.allowCrossWorldCalls());
        assertFalse(config.requireOpenDeviceForIncomingCalls());
        assertEquals(30, config.callRingTimeoutSeconds());
        assertEquals(8, config.speakerRadiusBlocks());
        assertTrue(config.offlineMessagingEnabled());
        assertEquals(512, config.maxMessageCharacters());
    }

    @Test
    void developmentControlsRoundTripThroughCodec() {
        PhonemodConfig config = new PhonemodConfig();
        config.setAddressBindingMode(AddressBindingMode.PLAYER);
        config.setCallsEnabled(false);
        config.setCallAudioMode(CallAudioMode.TOGGLEABLE);
        config.setAllowCrossWorldCalls(false);
        config.setRequireOpenDeviceForIncomingCalls(true);
        config.setCallRingTimeoutSeconds(60);
        config.setSpeakerRadiusBlocks(16);
        config.setOfflineMessagingEnabled(false);
        config.setMaxMessageCharacters(1024);

        PhonemodConfig decoded = PhonemodConfig.CODEC.decode(PhonemodConfig.CODEC.encode(config));
        assertEquals(AddressBindingMode.PLAYER, decoded.addressBindingMode());
        assertFalse(decoded.callsEnabled());
        assertEquals(CallAudioMode.TOGGLEABLE, decoded.callAudioMode());
        assertFalse(decoded.allowCrossWorldCalls());
        assertTrue(decoded.requireOpenDeviceForIncomingCalls());
        assertEquals(60, decoded.callRingTimeoutSeconds());
        assertEquals(16, decoded.speakerRadiusBlocks());
        assertFalse(decoded.offlineMessagingEnabled());
        assertEquals(1024, decoded.maxMessageCharacters());
    }

    @Test
    void runtimeSettersRejectForgedOutOfRangeValues() {
        PhonemodConfig config = new PhonemodConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setCallRingTimeoutSeconds(4));
        assertThrows(IllegalArgumentException.class, () -> config.setSpeakerRadiusBlocks(65));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxMessageCharacters(4097));
    }

    @Test
    void callAudioOverrideMapsEveryPolicy() {
        assertEquals(CallAudioPolicy.SPEAKER,
                CallAudioMode.ASSET.resolve(CallAudioPolicy.SPEAKER));
        assertEquals(CallAudioPolicy.PRIVATE,
                CallAudioMode.PRIVATE.resolve(CallAudioPolicy.SPEAKER));
        assertEquals(CallAudioPolicy.SPEAKER,
                CallAudioMode.SPEAKER.resolve(CallAudioPolicy.PRIVATE));
        assertEquals(CallAudioPolicy.TOGGLEABLE,
                CallAudioMode.TOGGLEABLE.resolve(CallAudioPolicy.PRIVATE));
    }
}
