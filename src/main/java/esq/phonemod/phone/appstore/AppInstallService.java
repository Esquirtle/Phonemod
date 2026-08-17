package esq.phonemod.phone.appstore;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.HytaleServer;
import esq.phonemod.api.event.AppInstallationEvent;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.core.PhoneService;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Manages installation and uninstallation of apps on player-owned devices.
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>System apps ({@link PhoneApp#isSystemApp()}) cannot be uninstalled.</li>
 *   <li>An app with a required permission can only be installed if the player
 *       holds that permission (checked via
 *       Hytale permission node.</li>
 *   <li>{@link #seedDefaults} is idempotent: system apps are always present;
 *       non-system default apps are added only once on first open.</li>
 * </ul>
 *
 * <p>All state mutations go through {@link DeviceInstalledAppsComponent} and
 * are persisted with {@link Store#putComponent}.
 */
public final class AppInstallService {

    private static AppInstallService instance;

    private AppInstallService() {
    }

    public static void initialize() {
        if (instance == null) {
            instance = new AppInstallService();
        }
    }

    @Nonnull
    public static AppInstallService get() {
        if (instance == null) {
            throw new IllegalStateException("AppInstallService must be initialized before use");
        }
        return instance;
    }

    // ── Install ───────────────────────────────────────────────────────────────

    /**
     * Installs {@code appId} on {@code deviceId} for the player entity at
     * {@code ref}.
     *
     * @param playerUuid UUID used for permission check
     * @return {@code true} if the install succeeded; {@code false} if the player
     *         lacks the required permission or the app is unknown
     */
    public boolean install(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID playerUuid,
            @Nonnull String deviceId,
            @Nonnull String appId) {
        PhoneApp<?> app = PhoneService.get().getApp(appId);
        if (app == null) {
            return false;
        }
        // Permission gate
        String requiredNode = app.getRequiredPermission();
        if (requiredNode != null) {
            if (!PermissionsModule.get().hasPermission(playerUuid, requiredNode)) {
                return false;
            }
        }
        AppInstallationEvent.Pre pre = HytaleServer.get().getEventBus()
                .dispatchFor(AppInstallationEvent.Pre.class)
                .dispatch(new AppInstallationEvent.Pre(playerUuid, deviceId, appId, true));
        if (pre.isCancelled()) return false;
        DeviceInstalledAppsComponent comp = store.ensureAndGetComponent(
                ref, DeviceInstalledAppsComponent.getComponentType());
        comp.install(deviceId, appId);
        store.putComponent(ref, DeviceInstalledAppsComponent.getComponentType(), comp);
        HytaleServer.get().getEventBus().dispatchFor(AppInstallationEvent.Post.class)
                .dispatch(new AppInstallationEvent.Post(playerUuid, deviceId, appId, true));
        return true;
    }

    // ── Uninstall ─────────────────────────────────────────────────────────────

    /**
     * Uninstalls {@code appId} from {@code deviceId}.
     *
     * @return {@code true} if the uninstall succeeded; {@code false} if the app is
     *         a system app (protected) or is not currently installed
     */
    public boolean uninstall(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String deviceId,
            @Nonnull String appId) {
        PhoneApp<?> app = PhoneService.get().getApp(appId);
        if (app != null && app.isSystemApp()) {
            return false;
        }
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) return false;
        AppInstallationEvent.Pre pre = HytaleServer.get().getEventBus()
                .dispatchFor(AppInstallationEvent.Pre.class)
                .dispatch(new AppInstallationEvent.Pre(player.getUuid(), deviceId, appId, false));
        if (pre.isCancelled()) return false;
        DeviceInstalledAppsComponent comp = store.ensureAndGetComponent(
                ref, DeviceInstalledAppsComponent.getComponentType());
        if (!comp.isInstalled(deviceId, appId)) {
            return false;
        }
        comp.uninstall(deviceId, appId);
        store.putComponent(ref, DeviceInstalledAppsComponent.getComponentType(), comp);
        HytaleServer.get().getEventBus().dispatchFor(AppInstallationEvent.Post.class)
                .dispatch(new AppInstallationEvent.Post(player.getUuid(), deviceId, appId, false));
        return true;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if {@code appId} is currently installed on
     * {@code deviceId} for this player entity.
     */
    public boolean isInstalled(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String deviceId,
            @Nonnull String appId) {
        DeviceInstalledAppsComponent comp = store.getComponent(
                ref, DeviceInstalledAppsComponent.getComponentType());
        return comp != null && comp.isInstalled(deviceId, appId);
    }

    // ── Seeding ───────────────────────────────────────────────────────────────

    /**
     * Seeds the installed-apps list for {@code deviceId} based on the set of
     * {@code defaultAppIds} declared in the device asset JSON.
     *
     * <p>Seeding logic (Decision D):
     * <ul>
     *   <li><b>System apps</b> — force-present on every call (idempotent,
     *       self-healing). They are installed regardless of prior state.</li>
     *   <li><b>Non-system default apps</b> — seeded only once. If the app is
     *       already in the installed set (even if the player later uninstalled
     *       it), it will NOT be re-added. This relies on a one-time seeded flag
     *       stored as the presence of the non-system app in the component at first
     *       open (or its absence meaning it was explicitly uninstalled).</li>
     * </ul>
     *
     * <p>The "seeded-once" semantic is implemented simply: non-system default apps
     * are installed only if they have never been in the player's installed-set for
     * this device. Once the player uninstalls such an app, it is permanently gone
     * from their device. The flag is the absence: if the app was never seeded
     * (brand new device), install it once; if it appears in the component
     * (previously seeded), leave it alone; if it was uninstalled, the component
     * won't list it so we would seed again — to prevent re-seeding after explicit
     * uninstall we track whether the device has been seeded before.
     *
     * <p>Implementation: we use a sentinel entry {@code "__seeded__"} in the
     * installed set to record that at least one seeding has happened.
     *
     * @param playerUuid   used for permission checks on default apps that require one
     * @param defaultAppIds app IDs from the device asset's {@code DefaultApps} array
     */
    public void seedDefaults(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UUID playerUuid,
            @Nonnull String deviceId,
            @Nonnull String[] defaultAppIds) {
        DeviceInstalledAppsComponent comp = store.ensureAndGetComponent(
                ref, DeviceInstalledAppsComponent.getComponentType());
        java.util.Set<String> installed = comp.getOrCreate(deviceId);
        boolean firstSeed = !installed.contains("__seeded__");

        for (String appId : defaultAppIds) {
            PhoneApp<?> app = PhoneService.get().getApp(appId);
            if (app == null) {
                continue;
            }
            if (app.isSystemApp()) {
                // Always force-present (idempotent, self-healing).
                installed.add(appId);
            } else if (firstSeed) {
                // Non-system defaults: seed only once, but respect permissions.
                String requiredNode = app.getRequiredPermission();
                if (requiredNode == null || PermissionsModule.get().hasPermission(playerUuid, requiredNode)) {
                    installed.add(appId);
                }
            }
        }

        if (firstSeed) {
            installed.add("__seeded__");
        }

        store.putComponent(ref, DeviceInstalledAppsComponent.getComponentType(), comp);
    }
}
