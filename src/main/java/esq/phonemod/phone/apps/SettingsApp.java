package esq.phonemod.phone.apps;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.StatefulPhoneApp;

public final class SettingsApp extends StatefulPhoneApp<SettingsApp.State> {

    public enum State {
        MAIN
    }

    public SettingsApp() {
        super(State.MAIN);
    }

    @Override
    public String getId() {
        return "settings";
    }

    @Override
    public String getDisplayName() {
        return "Settings";
    }

    @Override
    public String getAppButtonUI() {
        return "Pages/Phone/Components/SettingsButton.ui";
    }

    @Override
    public String getUIPath() {
        return "Pages/Phone/Settings.ui";
    }

    @Override
    public void build(PhoneAppContext ctx, UICommandBuilder cmd, UIEventBuilder evb) {
        appendMainUI(cmd);
        renderSettingsState(ctx.getPhoneNumber(), cmd, evb);
    }

    private void renderSettingsState(String phoneNumber, UICommandBuilder cmd, UIEventBuilder evb) {
        cmd.set("#PhoneNumberLabel.Text", phoneNumber);
    }

    @Override
    public boolean handleEvent(PhoneAppContext ctx, PhoneEvent event, UICommandBuilder cmd, UIEventBuilder evb) {
        return false;
    }
}
