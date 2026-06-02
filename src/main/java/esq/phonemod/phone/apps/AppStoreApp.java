package esq.phonemod.phone.apps;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.api.PhoneAppContext;
import esq.phonemod.phone.api.PhoneAssetPaths;
import esq.phonemod.phone.api.PhoneEvent;
import esq.phonemod.phone.api.PhoneUi;
import esq.phonemod.phone.api.StatefulPhoneApp;
import esq.phonemod.phone.appstore.AppCatalogService;
import esq.phonemod.phone.appstore.AppInstallService;
import esq.phonemod.phone.appstore.DeviceInstalledAppsComponent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * App Store — the device's application center.
 *
 * <p>Four tabs (Decision B):
 * <ul>
 *   <li><b>Installed</b> — non-system apps currently installed on this device</li>
 *   <li><b>Available</b> — apps the player can install (permission check passes)</li>
 *   <li><b>Restricted</b> — apps visible but locked (player lacks required permission)</li>
 *   <li><b>System</b> — built-in apps that cannot be uninstalled</li>
 * </ul>
 *
 * <p>Install/uninstall actions are handled here; app launch (Open) is handled by
 * the shell-level {@code open_app} action on {@code DevicePage}.
 */
public final class AppStoreApp extends StatefulPhoneApp<AppStoreApp.Tab> {

    public enum Tab { INSTALLED, AVAILABLE, RESTRICTED, SYSTEM }

    // ── Action constants (private to this app) ────────────────────────────────
    static final String ACTION_TAB    = "appstore_tab";
    static final String ACTION_INSTALL   = "appstore_install";
    static final String ACTION_UNINSTALL = "appstore_uninstall";
    static final String PARAM_TAB     = "Tab";
    static final String PARAM_APP_ID  = "AppId";

    // ── Selectors ─────────────────────────────────────────────────────────────
    private static final String SEL_HEADER  = "#AppStoreHeader";
    private static final String SEL_PANEL   = "#AppStorePanel";
    private static final String SEL_LIST    = "#AppStoreList";

    // Tab bar selectors
    private static final String SEL_TAB_INSTALLED   = "#TabInstalled";
    private static final String SEL_TAB_AVAILABLE   = "#TabAvailable";
    private static final String SEL_TAB_RESTRICTED  = "#TabRestricted";
    private static final String SEL_TAB_SYSTEM      = "#TabSystem";

    // Tab highlight styles (defined in DustLib.ui), swapped at runtime via .Style
    private static final String DUSTLIB_DOCUMENT    = "Pages/DustLib.ui";
    private static final String STYLE_TAB_ACTIVE    = "DustShellTabStyleActive";
    private static final String STYLE_TAB_INACTIVE  = "DustShellTabStyleInactive";

    // Row-relative selectors (relative to the appended row UI)
    private static final String SEL_ROW_ICON         = "#StoreIcon";
    private static final String SEL_ROW_NAME_TEXT    = "#StoreName.Text";
    private static final String SEL_ROW_ID_TEXT      = "#StoreId.Text";
    private static final String SEL_ROW_ACTION_BTN   = "#ActionButton";
    private static final String SEL_ROW_ACTION_TEXT  = "#ActionButton.Text";
    private static final String SEL_ROW_LOCKED_BADGE = "#LockedBadge";

    private static final String ROW_UI = "Pages/Phone/Components/DustAppStoreEntry.ui";

    public AppStoreApp() {
        super(Tab.INSTALLED);
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
        return 100;
    }

    @Override
    public boolean isSystemApp() {
        return true;
    }

    @Override
    public java.util.Map<String, String> getThemeableSelectors() {
        return java.util.Map.of(
                "appHeader", SEL_HEADER,
                "appPanel",  SEL_PANEL,
                "appContent", SEL_LIST);
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    @Override
    public void build(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        appendMainUI(ctx, cmd);

        Tab tab = getState(ctx);

        // Bind tab buttons
        bindTabButton(evb, SEL_TAB_INSTALLED,  Tab.INSTALLED);
        bindTabButton(evb, SEL_TAB_AVAILABLE,  Tab.AVAILABLE);
        bindTabButton(evb, SEL_TAB_RESTRICTED, Tab.RESTRICTED);
        bindTabButton(evb, SEL_TAB_SYSTEM,     Tab.SYSTEM);

        // Highlight the active tab by swapping each button's Style between the
        // active/inactive named styles defined in DustLib.ui. (.Style is a real
        // settable markup property; .Active is not.)
        cmd.set(SEL_TAB_INSTALLED  + ".Style", tabStyle(Tab.INSTALLED.equals(tab)));
        cmd.set(SEL_TAB_AVAILABLE  + ".Style", tabStyle(Tab.AVAILABLE.equals(tab)));
        cmd.set(SEL_TAB_RESTRICTED + ".Style", tabStyle(Tab.RESTRICTED.equals(tab)));
        cmd.set(SEL_TAB_SYSTEM     + ".Style", tabStyle(Tab.SYSTEM.equals(tab)));

        PhoneUi.safeClear(cmd, SEL_LIST);

        UUID playerUuid  = resolveUuid(ctx);
        String deviceId  = ctx.getPhoneNumber();
        String deviceType = resolveDeviceType(ctx);
        Set<String> installedIds = getInstalledIds(ctx);

        switch (tab) {
            case INSTALLED   -> renderInstalled(ctx, cmd, evb, installedIds, deviceType);
            case AVAILABLE   -> renderAvailable(ctx, cmd, evb, playerUuid, installedIds, deviceType);
            case RESTRICTED  -> renderRestricted(ctx, cmd, evb, playerUuid, deviceType);
            case SYSTEM      -> renderSystem(cmd, evb, deviceType);
        }
    }

    // ── Event handling ─────────────────────────────────────────────────────────

    @Override
    public boolean handleEvent(@Nonnull PhoneAppContext ctx,
            @Nonnull PhoneEvent event,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb) {
        String action = event.getAction();

        if (ACTION_TAB.equals(action)) {
            String tabName = event.getParam(PARAM_TAB);
            if (tabName != null) {
                try {
                    setState(ctx, Tab.valueOf(tabName));
                } catch (IllegalArgumentException ignored) {
                    // Unknown tab name; stay on current tab.
                }
            }
            build(ctx, cmd, evb);
            return true;
        }

        if (ACTION_INSTALL.equals(action)) {
            String appId = event.getParam(PARAM_APP_ID);
            if (appId != null) {
                UUID uuid = resolveUuid(ctx);
                boolean ok = AppInstallService.get().install(
                        ctx.getRef(), ctx.getStore(), uuid, ctx.getPhoneNumber(), appId);
                if (!ok) {
                    ctx.getPlayerRef().sendMessage(
                            Message.translation("phonemod.appstore.install.denied"));
                }
            }
            build(ctx, cmd, evb);
            return true;
        }

        if (ACTION_UNINSTALL.equals(action)) {
            String appId = event.getParam(PARAM_APP_ID);
            if (appId != null) {
                boolean ok = AppInstallService.get().uninstall(
                        ctx.getRef(), ctx.getStore(), ctx.getPhoneNumber(), appId);
                if (!ok) {
                    ctx.getPlayerRef().sendMessage(
                            Message.translation("phonemod.appstore.uninstall.denied"));
                }
            }
            build(ctx, cmd, evb);
            return true;
        }

        return false;
    }

    // ── Rendering helpers ──────────────────────────────────────────────────────

    private void renderInstalled(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull Set<String> installedIds,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> apps = AppCatalogService.get()
                .getInstalledApps(installedIds, deviceType);
        int i = 0;
        for (PhoneApp<?> app : apps) {
            String row = PhoneUi.appendListItem(cmd, SEL_LIST, ROW_UI, i);
            setRowBasic(cmd, row, app);
            // Uninstall button
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_ACTION_TEXT),
                    Message.translation("phonemod.appstore.action.uninstall"));
            PhoneUi.bindAction(evb, PhoneUi.child(row, SEL_ROW_ACTION_BTN),
                    ACTION_UNINSTALL, PhoneUi.params(PARAM_APP_ID, app.getId()));
            PhoneUi.setVisible(cmd, PhoneUi.child(row, SEL_ROW_LOCKED_BADGE), false);
            i++;
        }
        if (i == 0) {
            appendEmptyRow(cmd, "phonemod.appstore.empty.installed");
        }
    }

    private void renderAvailable(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull UUID playerUuid,
            @Nonnull Set<String> installedIds,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> apps = AppCatalogService.get()
                .getAvailableApps(playerUuid, installedIds, deviceType);
        int i = 0;
        for (PhoneApp<?> app : apps) {
            String row = PhoneUi.appendListItem(cmd, SEL_LIST, ROW_UI, i);
            setRowBasic(cmd, row, app);
            // Install button
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_ACTION_TEXT),
                    Message.translation("phonemod.appstore.action.install"));
            PhoneUi.bindAction(evb, PhoneUi.child(row, SEL_ROW_ACTION_BTN),
                    ACTION_INSTALL, PhoneUi.params(PARAM_APP_ID, app.getId()));
            PhoneUi.setVisible(cmd, PhoneUi.child(row, SEL_ROW_LOCKED_BADGE), false);
            i++;
        }
        if (i == 0) {
            appendEmptyRow(cmd, "phonemod.appstore.empty.available");
        }
    }

    private void renderRestricted(@Nonnull PhoneAppContext ctx,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull UUID playerUuid,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> apps = AppCatalogService.get()
                .getRestrictedApps(playerUuid, deviceType);
        int i = 0;
        for (PhoneApp<?> app : apps) {
            String row = PhoneUi.appendListItem(cmd, SEL_LIST, ROW_UI, i);
            setRowBasic(cmd, row, app);
            // Locked — no action button, show locked badge
            PhoneUi.setVisible(cmd, PhoneUi.child(row, SEL_ROW_ACTION_BTN), false);
            PhoneUi.setVisible(cmd, PhoneUi.child(row, SEL_ROW_LOCKED_BADGE), true);
            i++;
        }
        if (i == 0) {
            appendEmptyRow(cmd, "phonemod.appstore.empty.restricted");
        }
    }

    private void renderSystem(@Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evb,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> apps = AppCatalogService.get().getSystemApps(deviceType);
        int i = 0;
        for (PhoneApp<?> app : apps) {
            if (getId().equals(app.getId())) {
                continue; // Don't list the store itself in the system tab.
            }
            String row = PhoneUi.appendListItem(cmd, SEL_LIST, ROW_UI, i);
            setRowBasic(cmd, row, app);
            // System apps: show an Open button via shell open_app
            PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_ACTION_TEXT),
                    Message.translation("phonemod.appstore.action.open"));
            PhoneUi.bindOpenApp(evb, PhoneUi.child(row, SEL_ROW_ACTION_BTN), app.getId());
            PhoneUi.setVisible(cmd, PhoneUi.child(row, SEL_ROW_LOCKED_BADGE), false);
            i++;
        }
    }

    // ── Row utilities ──────────────────────────────────────────────────────────

    private void setRowBasic(@Nonnull UICommandBuilder cmd,
            @Nonnull String row,
            @Nonnull PhoneApp<?> app) {
        PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_NAME_TEXT), app.getDisplayName());
        PhoneUi.setText(cmd, PhoneUi.child(row, SEL_ROW_ID_TEXT), app.getId());
        String icon = app.getIconPath();
        if (icon != null && !icon.isBlank()) {
            cmd.set(PhoneUi.child(row, SEL_ROW_ICON) + ".Background", icon);
        }
    }

    private void appendEmptyRow(@Nonnull UICommandBuilder cmd, @Nonnull String key) {
        // Single non-interactive empty-state row. The text is embedded inline via
        // the %key translation reference (matching the built-in WarpListPage /
        // ItemRepairPage pattern) — appending an empty Label and then setting an
        // indexed selector separately throws a null-element error on the client.
        cmd.appendInline(SEL_LIST,
                "Label { Text: %" + key + "; Style: (Alignment: Center); FlexWeight: 1; Padding: (Full: 8); }");
    }

    private void bindTabButton(@Nonnull UIEventBuilder evb,
            @Nonnull String selector,
            @Nonnull Tab tab) {
        PhoneUi.bindAction(evb, selector, ACTION_TAB,
                PhoneUi.params(PARAM_TAB, tab.name()));
    }

    /**
     * Resolves the tab button style reference for the active/inactive state.
     * Both styles are defined in {@code DustLib.ui}.
     */
    @Nonnull
    private Value<?> tabStyle(boolean active) {
        return Value.ref(DUSTLIB_DOCUMENT,
                active ? STYLE_TAB_ACTIVE : STYLE_TAB_INACTIVE);
    }

    // ── Context helpers ────────────────────────────────────────────────────────

    @Nonnull
    private UUID resolveUuid(@Nonnull PhoneAppContext ctx) {
        PlayerRef pr = ctx.getStore().getComponent(ctx.getRef(), PlayerRef.getComponentType());
        if (pr == null) {
            throw new IllegalStateException("[AppStoreApp] No PlayerRef on entity");
        }
        return pr.getUuid();
    }

    /**
     * Returns the device type for this context. {@code DevicePage.openApp} injects it
     * as a state key so apps can filter the catalog by device type without needing a
     * session reference.
     */
    @Nonnull
    private String resolveDeviceType(@Nonnull PhoneAppContext ctx) {
        // Injected by DevicePage.openApp as "__deviceType__" state key.
        String injected = ctx.getState(DEVICE_TYPE_STATE_KEY);
        return (injected != null && !injected.isBlank()) ? injected : "phone";
    }

    /** State key injected by DevicePage into every app context to carry the device type. */
    public static final String DEVICE_TYPE_STATE_KEY = "__deviceType__";

    @Nonnull
    private Set<String> getInstalledIds(@Nonnull PhoneAppContext ctx) {
        DeviceInstalledAppsComponent comp = ctx.getStore().getComponent(
                ctx.getRef(), DeviceInstalledAppsComponent.getComponentType());
        if (comp == null) {
            return java.util.Collections.emptySet();
        }
        return comp.getInstalled(ctx.getPhoneNumber());
    }
}
