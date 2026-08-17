package esq.phonemod.migration;

import esq.phonemod.device.components.DeviceSettingsComponent;
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;
import esq.phonemod.phone.components.PhoneAppSessionState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MigrationComponentTest {
    @Test void installedAppsRemapIsIdempotentAndNeverOverwritesDestination() {
        DeviceInstalledAppsComponent component = new DeviceInstalledAppsComponent();
        component.install("old", "legacy-app");
        component.install("new", "existing-app");
        component.remapDevice("old", "new");
        component.remapDevice("old", "new");
        assertEquals(java.util.Set.of("existing-app"), component.getInstalled("new"));
        assertTrue(component.getInstalled("old").isEmpty());
    }

    @Test void settingsAndSessionStateRemapTwiceToSameData() {
        DeviceSettingsComponent settings = new DeviceSettingsComponent();
        var oldSettings = settings.getOrCreate("old");
        settings.remapDevice("old", "new");
        settings.remapDevice("old", "new");
        assertSame(oldSettings, settings.get("new"));

        PhoneAppSessionState state = new PhoneAppSessionState();
        state.set("old|legacy", "view", "detail");
        state.remapDevice("old", "new");
        state.remapDevice("old", "new");
        assertEquals("detail", state.get("new|legacy", "view"));
        assertNull(state.get("old|legacy", "view"));
    }
}
