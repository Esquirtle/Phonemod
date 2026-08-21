package esq.phonemod.security;

import esq.phonemod.security.AppOpenAuthorizer.Decision;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AppOpenAuthorizerTest {

    /** Grants exactly the nodes in {@code granted}. */
    private static Predicate<String> grants(Set<String> granted) {
        return granted::contains;
    }

    // (1a) Forged open of a hidden-UI app: known, compatible, owned, installed,
    // requires {"apps.x"} but nothing granted => MISSING_PERMISSION (fail-closed).
    @Test
    void hiddenUiForgedOpenWithoutGrantIsDenied() {
        assertEquals(Decision.MISSING_PERMISSION, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of("apps.x"), grants(Set.of())));
    }

    // (1b) Same, but the node is granted => ALLOW.
    @Test
    void hiddenUiOpenWithGrantIsAllowed() {
        assertEquals(Decision.ALLOW, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of("apps.x"), grants(Set.of("apps.x"))));
    }

    // (2) Unknown app => UNKNOWN_APP.
    @Test
    void unknownApp() {
        assertEquals(Decision.UNKNOWN_APP, AppOpenAuthorizer.authorize(
                false, Set.of(), "phone", true, true,
                Set.of(), grants(Set.of())));
    }

    // (3) Supported {"tablet"} but device "phone" => INCOMPATIBLE_DEVICE;
    // empty supported set with any device => not that reason.
    @Test
    void deviceCompatibility() {
        assertEquals(Decision.INCOMPATIBLE_DEVICE, AppOpenAuthorizer.authorize(
                true, Set.of("tablet"), "phone", true, true,
                Set.of(), grants(Set.of())));
        assertNotEquals(Decision.INCOMPATIBLE_DEVICE, AppOpenAuthorizer.authorize(
                true, Set.of(), "anything", true, true,
                Set.of(), grants(Set.of())));
    }

    // (4) Session not owned by actor, everything else valid => NOT_OWNER.
    @Test
    void notOwner() {
        assertEquals(Decision.NOT_OWNER, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", false, true,
                Set.of(), grants(Set.of())));
    }

    // (5) Installed, requires {"apps.x"}, nothing granted => MISSING_PERMISSION.
    @Test
    void installedButMissingPermission() {
        assertEquals(Decision.MISSING_PERMISSION, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of("apps.x"), grants(Set.of())));
    }

    // (6) Installed, requires {"apps.x"}, predicate always false (revoked)
    // => MISSING_PERMISSION.
    @Test
    void revokedPermission() {
        assertEquals(Decision.MISSING_PERMISSION, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of("apps.x"), node -> false));
    }

    // (7) Requires {"a","b"}: only "a" granted => MISSING_PERMISSION;
    // both granted => ALLOW.
    @Test
    void allRequiredPermissionsMustBeGranted() {
        assertEquals(Decision.MISSING_PERMISSION, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of("a", "b"), grants(Set.of("a"))));
        assertEquals(Decision.ALLOW, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of("a", "b"), grants(Set.of("a", "b"))));
    }

    // (8) All satisfied and no permissions required => ALLOW.
    @Test
    void noPermissionsRequiredAllows() {
        assertEquals(Decision.ALLOW, AppOpenAuthorizer.authorize(
                true, Set.of(), "phone", true, true,
                Set.of(), grants(Set.of())));
    }

    // ORDER: unknown app with every other input also invalid => UNKNOWN_APP wins.
    @Test
    void firstFailureWinsUnknownApp() {
        assertEquals(Decision.UNKNOWN_APP, AppOpenAuthorizer.authorize(
                false, Set.of("tablet"), "phone", false, false,
                Set.of("apps.x"), grants(Set.of())));
    }
}
