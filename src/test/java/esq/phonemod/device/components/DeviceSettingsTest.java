package esq.phonemod.device.components;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DeviceSettingsTest {

    @Test
    void legacyDocumentReceivesPrivacySafeDefaults() {
        DeviceSettings decoded = DeviceSettings.CODEC.decode(new BsonDocument());

        assertTrue(decoded.isNotificationsEnabled());
        assertTrue(decoded.isMessagePreviewEnabled());
        assertTrue(decoded.isIncomingCallsEnabled());
    }

    @Test
    void functionalPlayerPreferencesRoundTrip() {
        DeviceSettings settings = new DeviceSettings();
        settings.setThemeId("Dark");
        settings.setWallpaper("None");
        settings.setAppLayout("List");
        settings.setSoundProfileId("Silent");
        settings.setNotificationsEnabled(false);
        settings.setMessagePreviewEnabled(false);
        settings.setIncomingCallsEnabled(false);

        DeviceSettings decoded = DeviceSettings.CODEC.decode(DeviceSettings.CODEC.encode(settings));
        assertEquals("Dark", decoded.getThemeId());
        assertEquals("None", decoded.getWallpaper());
        assertEquals("List", decoded.getAppLayout());
        assertEquals("Silent", decoded.getSoundProfileId());
        assertFalse(decoded.isNotificationsEnabled());
        assertFalse(decoded.isMessagePreviewEnabled());
        assertFalse(decoded.isIncomingCallsEnabled());
    }
}
