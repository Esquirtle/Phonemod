package esq.phonemod.phone.apps.helpers;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import javax.annotation.Nonnull;

/** Static helpers that build the Settings app UI state. */
public final class Settings {

    static final String SETTINGS_UI = "Pages/Phone/Settings.ui";
    private static final String CONTENT = "#AppContent";

    private Settings() {}

    /**
     * Clears {@code #AppContent} and renders the settings screen,
     * displaying the phone number and a styled plugin title.
     */
    public static void loadSettingsState(@Nonnull String phoneNumber,
                                          @Nonnull UICommandBuilder cmd,
                                          @Nonnull UIEventBuilder evb) {
        cmd.clear(CONTENT);
        cmd.append(CONTENT, SETTINGS_UI);
        cmd.set("#PhoneNumberLabel.Text", phoneNumber);
    }
}
