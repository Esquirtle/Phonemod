package esq.phonemod.phone.messaging;

import esq.phonemod.device.components.DeviceSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PhoneRegistryPreferencesTest {

    @Test
    void snapshotCarriesOnlyImmutableRoutingAndNotificationChoices() {
        DeviceSettings settings = new DeviceSettings();
        settings.setNotificationsEnabled(false);
        settings.setMessagePreviewEnabled(false);
        settings.setIncomingCallsEnabled(false);
        settings.setSoundProfileId("Silent");

        PhoneRegistry.PreferenceSnapshot snapshot =
                PhoneRegistry.PreferenceSnapshot.from(settings);
        assertFalse(snapshot.notificationsEnabled());
        assertFalse(snapshot.messagePreviewEnabled());
        assertFalse(snapshot.incomingCallsEnabled());
        assertFalse(snapshot.soundEnabled());

        PhoneRegistry.PreferenceSnapshot defaults =
                PhoneRegistry.PreferenceSnapshot.from(null);
        assertTrue(defaults.notificationsEnabled());
        assertTrue(defaults.messagePreviewEnabled());
        assertTrue(defaults.incomingCallsEnabled());
        assertTrue(defaults.soundEnabled());
    }
}
