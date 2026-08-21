package esq.phonemod.security;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure, side-effect-free decision function for authorizing an app-open request.
 *
 * <p>Mirrors the check sequence historically inlined in {@code DevicePage.openApp}:
 * {@link Decision#UNKNOWN_APP} &rarr; {@link Decision#INCOMPATIBLE_DEVICE} &rarr;
 * {@link Decision#NOT_OWNER} &rarr; {@link Decision#NOT_INSTALLED} &rarr;
 * {@link Decision#MISSING_PERMISSION} &rarr; {@link Decision#ALLOW}. Fail-closed:
 * the first failing rule wins.
 *
 * <p>No server, Store, PhoneApp, or singleton dependencies — every input is a
 * plain value supplied by the caller, so this is fully unit-testable.
 */
public final class AppOpenAuthorizer {

    private AppOpenAuthorizer() {
    }

    public enum Decision {
        ALLOW,
        UNKNOWN_APP,
        INCOMPATIBLE_DEVICE,
        NOT_OWNER,
        NOT_INSTALLED,
        MISSING_PERMISSION
    }

    /**
     * @param appKnown              whether the app id resolved to a registered app
     * @param supportedDeviceTypes  device types the app supports; EMPTY means all
     *                              devices (mirrors
     *                              {@code DevicePage.supportsCurrentDevice})
     * @param deviceType            the current session's device type
     * @param sessionOwnedByActor   whether the acting player owns the session
     * @param installed             whether the app is installed on the device
     * @param requiredPermissions   permission nodes required to open; EMPTY passes
     * @param hasPermission         predicate resolving whether a node is granted
     */
    public static Decision authorize(boolean appKnown,
            Set<String> supportedDeviceTypes,
            String deviceType,
            boolean sessionOwnedByActor,
            boolean installed,
            Set<String> requiredPermissions,
            Predicate<String> hasPermission) {
        if (!appKnown) {
            return Decision.UNKNOWN_APP;
        }
        if (!supportsDevice(supportedDeviceTypes, deviceType)) {
            return Decision.INCOMPATIBLE_DEVICE;
        }
        if (!sessionOwnedByActor) {
            return Decision.NOT_OWNER;
        }
        if (!installed) {
            return Decision.NOT_INSTALLED;
        }
        if (!hasAllPermissions(requiredPermissions, hasPermission)) {
            return Decision.MISSING_PERMISSION;
        }
        return Decision.ALLOW;
    }

    /**
     * Empty supported set means the app runs on any device; otherwise the current
     * device type must be an exact ({@code equals}) member. Replicates
     * {@code DevicePage.supportsCurrentDevice} semantics.
     */
    private static boolean supportsDevice(Set<String> supportedDeviceTypes, String deviceType) {
        if (supportedDeviceTypes == null || supportedDeviceTypes.isEmpty()) {
            return true;
        }
        return supportedDeviceTypes.contains(deviceType);
    }

    /** All required nodes must be granted; an empty set passes. */
    private static boolean hasAllPermissions(Set<String> requiredPermissions,
            Predicate<String> hasPermission) {
        if (requiredPermissions == null || requiredPermissions.isEmpty()) {
            return true;
        }
        for (String node : requiredPermissions) {
            if (!hasPermission.test(node)) {
                return false;
            }
        }
        return true;
    }
}
