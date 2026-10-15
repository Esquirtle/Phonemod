package esq.phonemod.api.v3;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * F2 contract tests for {@link AppDescriptor} (R-API-7, R-API-8). All ROJO:
 * each asserts the real target behaviour, failing on the F2 skeleton's uncaught
 * {@code UnsupportedOperationException("F4")} (the compact constructor and
 * {@code Builder.build()} both throw).
 */
@Tag("contract")
class AppDescriptorContractTest {

    @Test
    void builderRequiresNamespacedId() {
        assertThrows(IllegalArgumentException.class, () -> AppDescriptor.builder("not-namespaced").build());
    }

    @Test
    void builderKeepsAllPermissions() {
        AppDescriptor d = AppDescriptor.builder("esq.example:app").permissions(Set.of("a", "b")).build();
        assertEquals(Set.of("a", "b"), d.permissions());
    }

    @Test
    void builderDefensivelyCopiesCollections() {
        var aliases = new java.util.HashSet<>(Set.of("alias"));
        AppDescriptor d = AppDescriptor.builder("esq.example:app").aliases(aliases).build();
        aliases.add("mutated");
        assertEquals(Set.of("alias"), d.aliases());
    }

    @Test
    void defaultsAreLockedNonSystemAllDevices() {
        AppDescriptor d = AppDescriptor.builder("esq.example:app").build();
        assertEquals(AppVisibility.LOCKED, d.visibility());
        assertFalse(d.system());
        assertTrue(d.supports(DeviceType.PHONE));
    }

    @Test
    void legacyOnlyViewResolvesForEveryBackend() {
        ViewRef legacy = ViewRef.of("Common/App.ui");
        AppDescriptor d = AppDescriptor.builder("esq.example:app").view(Backend.LEGACY, legacy).build();
        assertEquals(legacy, d.viewFor(Backend.NOESIS).orElseThrow());
    }

    @Test
    void backendSpecificViewWinsOverLegacyFallback() {
        ViewRef legacy = ViewRef.of("Common/App.ui");
        ViewRef noesis = ViewRef.of("App.xaml");
        AppDescriptor d = AppDescriptor.builder("esq.example:app")
                .view(Backend.LEGACY, legacy).view(Backend.NOESIS, noesis).build();
        assertEquals(noesis, d.viewFor(Backend.NOESIS).orElseThrow());
    }

    @Test
    void noViewsResolvesEmpty() {
        AppDescriptor d = AppDescriptor.builder("esq.example:app").build();
        assertTrue(d.viewFor(Backend.LEGACY).isEmpty());
    }

    @Test
    void supportsUsesDeviceTypeEquality() {
        AppDescriptor d = AppDescriptor.builder("esq.example:app")
                .deviceTypes(Set.of(DeviceType.of("phone"))).build();
        assertTrue(d.supports(DeviceType.PHONE));
    }
}
