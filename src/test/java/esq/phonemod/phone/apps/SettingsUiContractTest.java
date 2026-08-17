package esq.phonemod.phone.apps;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsUiContractTest {

    @Test
    void playerAndPluginSettingsAreSeparateViews() throws IOException {
        String path = "/Common/UI/Custom/Pages/Phone/DustSettings.ui";
        try (InputStream input = SettingsUiContractTest.class.getResourceAsStream(path)) {
            assertNotNull(input, "Settings UI must be packaged as a Common asset");
            String document = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            int playerView = document.indexOf("Group #PlayerSettingsView");
            int pluginView = document.indexOf("Group #PluginSettingsView");
            int personalControl = document.indexOf("#ThemeDefault");
            int pluginControl = document.indexOf("#AddressDevice");

            assertTrue(document.contains("#PlayerSettingsTab"));
            assertTrue(document.contains("#PluginSettingsTab"));
            assertTrue(document.contains("#PluginSettingsTab { Text: \"Plugin\"; FlexWeight: 1; Visible: false; }"));
            assertTrue(document.contains("#NotificationsEnabled"));
            assertTrue(document.contains("#PreviewDisabled"));
            assertTrue(document.contains("#PlayerCallsDisabled"));
            assertTrue(document.contains("#ResetPlayerSettings"));
            assertFalse(document.contains("#IconPackMono"),
                    "Unavailable icon packs must not be presented as working preferences");
            assertTrue(playerView >= 0 && personalControl > playerView && personalControl < pluginView,
                    "Player controls must remain inside the player view");
            assertTrue(pluginView >= 0 && pluginControl > pluginView,
                    "Server-wide controls must remain inside the plugin view");
        }
    }
}
