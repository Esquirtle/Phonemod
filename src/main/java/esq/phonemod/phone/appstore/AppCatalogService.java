package esq.phonemod.phone.appstore;

import esq.permission.api.AppPermissionRequirement;
import esq.permission.api.AppPermissionService;
import esq.permission.api.PermissionResult;
import esq.permission.api.PermissionVisibility;
import esq.phonemod.phone.api.PhoneApp;
import esq.phonemod.phone.core.PhoneService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Computes bucketed views of the app catalog for a specific player and device.
 *
 * <p>Buckets (Decision A/B):
 * <ul>
 *   <li><b>SYSTEM</b> — system apps that are always present</li>
 *   <li><b>INSTALLED</b> — non-system apps currently installed on this device</li>
 *   <li><b>AVAILABLE</b> — apps the player can install (permission check passes,
 *       not yet installed)</li>
 *   <li><b>RESTRICTED</b> — apps the player lacks permission for and whose
 *       visibility is {@link PermissionVisibility#LOCKED} (visible but gated);
 *       apps whose visibility is {@link PermissionVisibility#HIDDEN} are excluded
 *       from all buckets</li>
 * </ul>
 */
public final class AppCatalogService {

    private static AppCatalogService instance;

    private AppCatalogService() {
    }

    public static void initialize() {
        if (instance == null) {
            instance = new AppCatalogService();
        }
    }

    @Nonnull
    public static AppCatalogService get() {
        if (instance == null) {
            throw new IllegalStateException("AppCatalogService must be initialized before use");
        }
        return instance;
    }

    // ── Catalog buckets ───────────────────────────────────────────────────────

    /**
     * Returns all system apps filtered for {@code deviceType}.
     */
    @Nonnull
    public List<PhoneApp<?>> getSystemApps(@Nonnull String deviceType) {
        List<PhoneApp<?>> result = new ArrayList<>();
        for (PhoneApp<?> app : PhoneService.get().getApps()) {
            if (app.isSystemApp() && supportsDevice(app, deviceType)) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * Returns non-system apps that are installed on {@code deviceId} for this
     * player, filtered by {@code deviceType}.
     *
     * <p>The installed set comes from {@link DeviceInstalledAppsComponent}, which
     * the caller provides via {@code installedAppIds}.
     */
    @Nonnull
    public List<PhoneApp<?>> getInstalledApps(
            @Nonnull java.util.Set<String> installedAppIds,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> result = new ArrayList<>();
        for (String appId : installedAppIds) {
            if ("__seeded__".equals(appId)) {
                continue;
            }
            PhoneApp<?> app = PhoneService.get().getApp(appId);
            if (app != null && !app.isSystemApp() && supportsDevice(app, deviceType)) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * Returns apps the player can install but has not yet — permission allowed,
     * not currently installed, not a system app, compatible device type.
     */
    @Nonnull
    public List<PhoneApp<?>> getAvailableApps(
            @Nonnull UUID playerUuid,
            @Nonnull java.util.Set<String> installedAppIds,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> result = new ArrayList<>();
        for (PhoneApp<?> app : PhoneService.get().getApps()) {
            if (app.isSystemApp()) {
                continue;
            }
            if (!supportsDevice(app, deviceType)) {
                continue;
            }
            if (installedAppIds.contains(app.getId())) {
                continue;
            }
            String node = app.getRequiredPermission();
            if (node == null || AppPermissionService.get().hasPermission(playerUuid, node)) {
                result.add(app);
            }
        }
        return result;
    }

    /**
     * Returns apps the player cannot install because they lack the required
     * permission and the visibility is {@link PermissionVisibility#LOCKED}
     * (hidden apps are excluded entirely).
     */
    @Nonnull
    public List<PhoneApp<?>> getRestrictedApps(
            @Nonnull UUID playerUuid,
            @Nonnull String deviceType) {
        List<PhoneApp<?>> result = new ArrayList<>();
        for (PhoneApp<?> app : PhoneService.get().getApps()) {
            if (app.isSystemApp()) {
                continue;
            }
            if (!supportsDevice(app, deviceType)) {
                continue;
            }
            String node = app.getRequiredPermission();
            if (node == null) {
                continue;
            }
            AppPermissionRequirement req = AppPermissionRequirement.of(
                    node, app.getPermissionVisibility(), null);
            String result2 = AppPermissionService.get().evaluate(playerUuid, req);
            if (PermissionResult.LOCKED.equals(result2)) {
                result.add(app);
            }
            // HIDDEN apps are not added to any list
        }
        return result;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the app supports the given device type.
     * Apps with an empty {@code getSupportedDeviceTypes()} array run on all types.
     */
    private boolean supportsDevice(@Nonnull PhoneApp<?> app, @Nonnull String deviceType) {
        String[] supported = app.getSupportedDeviceTypes();
        if (supported == null || supported.length == 0) {
            return true;
        }
        for (String type : supported) {
            if (deviceType.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
