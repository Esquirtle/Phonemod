package esq.phonemod.phone.apps;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;
import esq.phonemod.phone.core.PhoneService;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * App Store — the device's application center. Lists every registered
 * {@link PhoneApp} (including third-party apps that are not in the device's
 * default home grid) and launches them.
 *
 * <p>Each row shows the app's icon ({@link PhoneApp#getIconPath()}, with a Dust
 * placeholder tile when absent), its name + id, and an Open button that fires the
 * shell-level {@code open_app} action — so launching is handled by
 * {@code DevicePage}, not this app.
 *
 * <p>Composed entirely from DustLib components; ships no UI images of its own.
 */
public final class AppStoreApp extends StatefulPhoneApp<AppStoreApp.State> {

    public enum State { MAIN }

    private static final String SEL_HEADER = "#AppStoreHeader";
    private static final String SEL_PANEL  = "#AppStorePanel";
    private static final String SEL_LIST   = "#AppStoreList";

    private static final String ROW_UI = "Pages/Phone/Components/DustAppStoreEntry.ui";

    // Row-relative selectors.
    private static final String SEL_ROW_ICON      = "#StoreIcon";
    private static final String SEL_ROW_NAME_TEXT = "#StoreName.Text";
    private static final String SEL_ROW_ID_TEXT   = "#StoreId.Text";
    private static final String SEL_ROW_OPEN      = "#OpenButton";

    public AppStoreApp() {
        super(State.MAIN);
    }

    @Override
    public String getId() {
        return "appstore";
    }

    @Override
    public String getDisplayName() {
        return "App Store";
    }

    @Override
    public String getAppButtonUI() {
        return PhoneAssetPaths.APPSTORE_BUTTON_UI;
    }

    @Override
    public String getUIPath() {
        return PhoneAssetPaths.DUST_APPSTORE_UI;
    }

    @Override
    public int getSortOrder() {
        // Sit at the end of the default home grid.
        return 100;
    }

    @Override
    public Map<String, String> getThemeableSelectors() {
        return Map.of(
                "appHeader", SEL_HEADER,
                "appPanel", SEL_PANEL,
                "appContent", SEL_LIST);
    }

    @Override
    public void build(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        appendMainUI(ctx, cmd);
        PhoneUi.safeClear(cmd, SEL_LIST);

        int i = 0;
        for (PhoneApp<?> app : PhoneService.get().getApps()) {
            if (getId().equals(app.getId())) {
                continue; // don't list the store itself
            }
            String row = PhoneUi.appendListItem(cmd, SEL_LIST, ROW_UI, i);
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_NAME_TEXT), app.getDisplayName());
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_ID_TEXT), app.getId());

            String icon = app.getIconPath();
            if (icon != null && !icon.isBlank()) {
                // Overlays the placeholder tile baked into the row.
                cmd.set(PhoneUi.child(row, SEL_ROW_ICON) + ".Background", icon);
            }

            // open_app is a shell action — DevicePage opens the target app.
            PhoneUi.bindOpenApp(evb, PhoneUi.child(row, SEL_ROW_OPEN), app.getId());
            i++;
        }
    }

    @Override
    public boolean handleEvent(@Nonnull PhoneAppContext ctx,
            @Nonnull PhoneEvent event,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        // Nothing app-owned: the Open buttons fire the shell-level open_app action.
        return false;
    }
}
