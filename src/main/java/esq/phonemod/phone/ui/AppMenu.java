package esq.phonemod.phone.ui;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.core.PhoneService;

import javax.annotation.Nonnull;

public final class AppMenu {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String APP_HOLDER = "#APPHolder";

    private AppMenu() {}

    public static void build(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evb) {
        cmd.clear(APP_HOLDER);

        int i = 0;
        for (PhoneApp<?> app : PhoneService.get().getApps()) {
            String buttonUI = app.getAppButtonUI();
            LOGGER.atInfo().log("[AppMenu] app=%s buttonUI=%s", app.getId(), buttonUI);

            if (buttonUI == null || buttonUI.isBlank()) {
                LOGGER.atWarning().log("[AppMenu] app=%s has no getAppButtonUI() — skipped", app.getId());
                continue;
            }
            // Each app owns a .ui file with its icon hardcoded in the TextButton style —
            // the only reliable way to set TextButton backgrounds since style properties
            // are immutable after parse time.
            cmd.append(APP_HOLDER, buttonUI);

            String entrySelector = APP_HOLDER + "[" + i + "]";
            // Label text IS dynamically settable (it's a Text property, not a style).
            cmd.set(entrySelector + " #APPNAME.Text", app.getDisplayName());

            evb.addEventBinding(CustomUIEventBindingType.Activating,
                    entrySelector + " #APPBUTTON",
                    EventData.of("Action", "open_app").append("App", app.getId()),
                    false);
            i++;
        }
    }
}
